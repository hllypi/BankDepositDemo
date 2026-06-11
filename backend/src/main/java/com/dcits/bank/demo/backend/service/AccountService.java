package com.dcits.bank.demo.backend.service;

import com.dcits.bank.demo.backend.dto.*;
import com.dcits.bank.demo.backend.entity.*;
import com.dcits.bank.demo.backend.enums.*;
import com.dcits.bank.demo.backend.exception.BusinessException;
import com.dcits.bank.demo.backend.mapper.*;
import com.dcits.bank.demo.backend.util.AccountNoGenerator;
import com.dcits.bank.demo.backend.util.IdCardUtil;
import com.dcits.bank.demo.backend.util.LuhnUtil;
import com.dcits.bank.demo.backend.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private static final String CARD_BIN = "621700";
    /** 乐观锁最大重试次数 */
    private static final int MAX_RETRY = 3;

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final BusinessTransactionMapper transactionMapper;
    private final CashTransactionMapper cashTransactionMapper;
    private final AccountingService accountingService;

    public AccountService(CustomerMapper customerMapper,
                          AccountMapper accountMapper,
                          BusinessTransactionMapper transactionMapper,
                          CashTransactionMapper cashTransactionMapper,
                          AccountingService accountingService) {
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.cashTransactionMapper = cashTransactionMapper;
        this.accountingService = accountingService;
    }

    // ==================== 功能1：客户开户 ====================

    @Transactional(rollbackFor = Exception.class)
    public OpenAccountResponse openAccount(OpenAccountRequest req) {
        validateOpenAccountRequest(req);

        Long customerId = getOrCreateCustomer(req);

        String accountNo = AccountNoGenerator.generate(req.getBranchCode());
        String cardNo = generateUniqueCardNo();
        String passwordHash = PasswordUtil.encode(req.getPassword());
        String currency = req.getCurrency() != null ? req.getCurrency() : "CNY";

        Account account = new Account();
        account.setAccountNo(accountNo);
        account.setCardNo(cardNo);
        account.setPasswordHash(passwordHash);
        account.setCustomerId(customerId);
        account.setAccountType(AccountType.DEMAND_DEPOSIT.getCode());
        account.setAccountLevel(req.getAccountLevel() != null ? req.getAccountLevel() : 1);
        account.setCurrency(currency);
        account.setBranchCode(req.getBranchCode());
        account.setBalance(BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setStatus(AccountStatus.NORMAL.getCode());
        account.setVersion(0);
        account.setOpenDate(LocalDate.now());
        accountMapper.insert(account);

        String outTradeNo = "OPEN_" + cardNo + "_" + System.currentTimeMillis();
        BusinessTransaction trans = buildTransaction(account.getAccountId(), outTradeNo,
                DcFlag.CREDIT.getCode(), TransType.OPEN_ACCOUNT.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, req.getChannel(), "SYSTEM");
        transactionMapper.insert(trans);

        accountingService.generateEntries(trans);

        return new OpenAccountResponse(customerId, account.getAccountId(), cardNo, accountNo);
    }

    // ==================== 功能2：存款交易 ====================

    /**
     * 存款交易 — 对应基线文档 功能2。
     * 幂等防重 + 密码鉴权 + 乐观锁更新余额 + 复式记账。
     * 柜面渠道额外记录现金入库明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public DepositResponse deposit(DepositRequest req) {
        // 1. 幂等校验
        BusinessTransaction existing = transactionMapper.selectByOutTradeNo(req.getOutTradeNo());
        if (existing != null && existing.getStatus().equals(TransStatus.SUCCESS.getCode())) {
            return new DepositResponse(existing.getTransId(), existing.getBalanceAfter(), existing.getStatus());
        }

        // 2. 账户定位 + 验密 + 状态检查
        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());

        // 3. 乐观锁更新余额（余额 + 存款金额），失败重试
        BigDecimal balanceAfter = updateBalanceWithRetry(account.getAccountId(),
                account.getBalance().add(req.getTransAmount()), account.getVersion());

        // 4. 记录交易流水
        BusinessTransaction trans = buildTransaction(account.getAccountId(), req.getOutTradeNo(),
                DcFlag.CREDIT.getCode(), TransType.DEPOSIT.getCode(),
                req.getTransAmount(), balanceAfter, req.getChannel(), req.getOperatorId());
        trans.setRemark(req.getRemark());
        transactionMapper.insert(trans);

        // 5. 会计分录（借1002库存现金 / 贷1001活期存款）
        accountingService.generateEntries(trans);

        // 6. 柜面渠道：记录现金入库明细
        if (Channel.COUNTER.getCode().equals(req.getChannel())) {
            CashTransaction cash = new CashTransaction();
            cash.setTransId(trans.getTransId());
            cash.setTellerId(req.getOperatorId());
            cash.setCashType(CashType.IN.getCode());
            cash.setAmount(req.getTransAmount());
            cashTransactionMapper.insert(cash);
        }

        return new DepositResponse(trans.getTransId(), balanceAfter, trans.getStatus());
    }

    // ==================== 功能3：取款交易 ====================

    /**
     * 取款交易 — 对应基线文档 功能3。
     * 与存款镜像：可用余额校验 + 账户等级限额 + 乐观锁扣减 + 复式记账（借1001贷1002）。
     * 柜面渠道额外记录现金出库明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdraw(WithdrawRequest req) {
        // 1. 幂等校验
        BusinessTransaction existing = transactionMapper.selectByOutTradeNo(req.getOutTradeNo());
        if (existing != null && existing.getStatus().equals(TransStatus.SUCCESS.getCode())) {
            return new WithdrawResponse(existing.getTransId(), existing.getBalanceAfter(), existing.getStatus());
        }

        // 2. 账户定位 + 验密 + 状态检查
        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());

        // 3. 可用余额校验（冻结金额不可动用）
        BigDecimal available = account.getBalance().subtract(account.getFrozenAmount());
        if (available.compareTo(req.getTransAmount()) < 0) {
            throw new BusinessException(ResultCode.BALANCE_INSUFFICIENT);
        }

        // 4. 账户等级限额校验
        checkLevelLimit(account.getAccountLevel(), req.getTransAmount());

        // 5. 乐观锁扣减余额
        BigDecimal balanceAfter = updateBalanceWithRetry(account.getAccountId(),
                account.getBalance().subtract(req.getTransAmount()), account.getVersion());

        // 6. 记录交易流水（借方）
        BusinessTransaction trans = buildTransaction(account.getAccountId(), req.getOutTradeNo(),
                DcFlag.DEBIT.getCode(), TransType.WITHDRAW.getCode(),
                req.getTransAmount(), balanceAfter, req.getChannel(), req.getOperatorId());
        trans.setRemark(req.getRemark());
        transactionMapper.insert(trans);

        // 7. 会计分录（借1001活期存款 / 贷1002库存现金）
        accountingService.generateEntries(trans);

        // 8. 柜面渠道：记录现金出库明细
        if (Channel.COUNTER.getCode().equals(req.getChannel())) {
            CashTransaction cash = new CashTransaction();
            cash.setTransId(trans.getTransId());
            cash.setTellerId(req.getOperatorId());
            cash.setCashType(CashType.OUT.getCode());
            cash.setAmount(req.getTransAmount());
            cashTransactionMapper.insert(cash);
        }

        return new WithdrawResponse(trans.getTransId(), balanceAfter, trans.getStatus());
    }

    /** 账户等级限额：Ⅱ类单笔≤1万，Ⅲ类单笔≤1000。 */
    private void checkLevelLimit(int accountLevel, BigDecimal amount) {
        if (accountLevel == AccountLevel.LEVEL_II.getCode()
                && amount.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LEVEL_LIMIT, "Ⅱ类账户单笔交易不得超过10000元");
        }
        if (accountLevel == AccountLevel.LEVEL_III.getCode()
                && amount.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LEVEL_LIMIT, "Ⅲ类账户单笔交易不得超过1000元");
        }
    }

    // ==================== 公共辅助方法 ====================

    /**
     * 乐观锁更新余额，并发冲突时重试（最多 {@link #MAX_RETRY} 次）。
     * 每次重试重新读取最新的 account 版本号。
     */
    private BigDecimal updateBalanceWithRetry(Long accountId, BigDecimal newBalance, int currentVersion) {
        for (int i = 0; i < MAX_RETRY; i++) {
            int affected = accountMapper.updateBalanceWithVersion(accountId, newBalance, currentVersion);
            if (affected > 0) {
                return newBalance;
            }
            // 并发冲突：重新读取最新版本号再试
            Account latest = accountMapper.selectById(accountId);
            currentVersion = latest.getVersion();
        }
        throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
    }

    /**
     * 根据卡号定位账户，校验密码及账户状态。
     */
    private Account locateAndAuthAccount(String cardNo, String rawPassword) {
        Account account = accountMapper.selectByCardNo(cardNo);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() == AccountStatus.FROZEN.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        if (account.getStatus() == AccountStatus.CLOSED.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_CLOSED);
        }
        if (!PasswordUtil.matches(rawPassword, account.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        return account;
    }

    /** 构建交易流水对象（公共字段填充）。 */
    private BusinessTransaction buildTransaction(Long accountId, String outTradeNo,
                                                  String dcFlag, String transType,
                                                  BigDecimal amount, BigDecimal balanceAfter,
                                                  String channel, String operatorId) {
        BusinessTransaction trans = new BusinessTransaction();
        trans.setAccountId(accountId);
        trans.setOutTradeNo(outTradeNo);
        trans.setDcFlag(dcFlag);
        trans.setTransType(transType);
        trans.setTransAmount(amount);
        trans.setBalanceAfter(balanceAfter);
        trans.setChannel(channel);
        trans.setOperatorId(operatorId);
        trans.setTransTime(LocalDateTime.now());
        trans.setStatus(TransStatus.SUCCESS.getCode());
        return trans;
    }

    // ==================== 开户辅助方法 ====================

    private Long getOrCreateCustomer(OpenAccountRequest req) {
        Customer existing = customerMapper.selectByIdTypeAndIdNumber(req.getIdType(), req.getIdNumber());
        if (existing != null) {
            return existing.getCustomerId();
        }
        Customer customer = new Customer();
        customer.setCustomerName(req.getCustomerName());
        customer.setType(CustomerType.PERSONAL.getCode());
        customer.setIdType(req.getIdType());
        customer.setIdNumber(req.getIdNumber());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());

        if ("01".equals(req.getIdType())) {
            if (!IdCardUtil.isValid(req.getIdNumber())) {
                throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "身份证号不合法");
            }
            customer.setDateOfBirth(IdCardUtil.birthday(req.getIdNumber()));
            customer.setGender("MALE".equals(IdCardUtil.gender(req.getIdNumber())) ? "M" : "F");
        }

        customer.setBranch(req.getBranchCode());
        customer.setStatus(CustomerStatus.NORMAL.getCode());
        customerMapper.insert(customer);
        return customer.getCustomerId();
    }

    private String generateUniqueCardNo() {
        String prefix = CARD_BIN + LuhnUtil.randomDigits(12);
        return LuhnUtil.generateCardNo(prefix);
    }

    private void validateOpenAccountRequest(OpenAccountRequest req) {
        if (isEmpty(req.getCustomerName())) throw new BusinessException(ResultCode.PARAM_MISSING, "客户姓名不能为空");
        if (isEmpty(req.getIdType())) throw new BusinessException(ResultCode.PARAM_MISSING, "证件类型不能为空");
        if (isEmpty(req.getIdNumber())) throw new BusinessException(ResultCode.PARAM_MISSING, "证件号码不能为空");
        if (isEmpty(req.getPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "账户密码不能为空");
        if (isEmpty(req.getPhone())) throw new BusinessException(ResultCode.PARAM_MISSING, "联系电话不能为空");
        if (isEmpty(req.getAddress())) throw new BusinessException(ResultCode.PARAM_MISSING, "通讯地址不能为空");
        if (isEmpty(req.getBranchCode())) throw new BusinessException(ResultCode.PARAM_MISSING, "开户行代码不能为空");
        if (isEmpty(req.getChannel())) throw new BusinessException(ResultCode.PARAM_MISSING, "开户渠道不能为空");
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
