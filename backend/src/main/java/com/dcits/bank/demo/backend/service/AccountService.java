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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private static final String CARD_BIN = "621700";
    /** 乐观锁最大重试次数 */
    private static final int MAX_RETRY = 3;

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final BusinessTransactionMapper transactionMapper;
    private final CashTransactionMapper cashTransactionMapper;
    private final DailyBalanceMapper dailyBalanceMapper;
    private final AccountingService accountingService;
    private final InterestRateConfigMapper interestRateConfigMapper;
    private final InterestSettlementMapper interestSettlementMapper;

    public AccountService(CustomerMapper customerMapper,
                          AccountMapper accountMapper,
                          BusinessTransactionMapper transactionMapper,
                          CashTransactionMapper cashTransactionMapper,
                          AccountingService accountingService,
                          DailyBalanceMapper dailyBalanceMapper,
                          InterestRateConfigMapper interestRateConfigMapper,
                          InterestSettlementMapper interestSettlementMapper) {
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.cashTransactionMapper = cashTransactionMapper;
        this.accountingService = accountingService;
        this.dailyBalanceMapper = dailyBalanceMapper;
        this.interestRateConfigMapper = interestRateConfigMapper;
        this.interestSettlementMapper = interestSettlementMapper;
    }

    //  功能1：客户开户
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
        account.setAccountType(AccountEnums.Type.DEMAND_DEPOSIT.getCode());
        account.setAccountLevel(req.getAccountLevel() != null ? req.getAccountLevel() : 1);
        account.setCurrency(currency);
        account.setBranchCode(req.getBranchCode());
        account.setBalance(BigDecimal.ZERO);
        account.setFrozenAmount(BigDecimal.ZERO);
        account.setStatus(AccountEnums.Status.NORMAL.getCode());
        account.setVersion(0);
        account.setOpenDate(LocalDate.now());
        accountMapper.insert(account);

        BusinessTransaction trans = buildTransaction(account.getAccountId(), req.getOutTradeNo(),
                TransactionEnums.DcFlag.CREDIT.getCode(), TransType.OPEN_ACCOUNT.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, req.getChannel(), "SYSTEM", account.getBranchCode());
        transactionMapper.insert(trans);

        accountingService.generateEntries(trans);

        return new OpenAccountResponse(cardNo, accountNo);
    }

    // 功能2：存款交易

    /**
     * 存款交易 — 对应基线文档 功能2。
     * 幂等防重 + 密码鉴权 + 乐观锁更新余额 + 复式记账。
     * 柜面渠道额外记录现金入库明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public DepositResponse deposit(DepositRequest req) {
        validateTransactionRequest(req.getOutTradeNo(), req.getCardNo(), req.getPassword(),
                req.getTransAmount(), req.getChannel());
        // 1. 幂等校验
        BusinessTransaction existing = transactionMapper.selectByOutTradeNo(req.getOutTradeNo());
        if (existing != null && existing.getStatus().equals(TransactionEnums.Status.SUCCESS.getCode())) {
            return new DepositResponse(existing.getTransNo(), existing.getBalanceAfter(), existing.getStatus());
        }

        // 2. 账户定位 + 验密 + 状态检查
        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());

        // 3. 乐观锁更新余额（余额 + 存款金额），失败重试
        BigDecimal balanceAfter = updateBalanceWithRetry(account.getAccountId(), req.getTransAmount());

        // 4. 记录交易流水
        BusinessTransaction trans = buildTransaction(account.getAccountId(), req.getOutTradeNo(),
                TransactionEnums.DcFlag.CREDIT.getCode(), TransType.DEPOSIT.getCode(),
                req.getTransAmount(), balanceAfter, req.getChannel(), req.getOperatorId(), account.getBranchCode());
        trans.setRemark(req.getRemark());
        transactionMapper.insert(trans);

        // 5. 会计分录（借1002库存现金 / 贷1001活期存款）
        accountingService.generateEntries(trans);

        // 6. 记录现金入库
        CashTransaction cashIn = new CashTransaction();
        cashIn.setTransId(trans.getTransId());
        cashIn.setTellerId(req.getOperatorId());
        cashIn.setCashType(TransactionEnums.CashType.IN.getCode());
        cashIn.setAmount(req.getTransAmount());
        cashTransactionMapper.insert(cashIn);

        return new DepositResponse(trans.getTransNo(), balanceAfter, trans.getStatus());
    }

    //  功能3：取款交易

    /**
     * 取款交易 — 对应基线文档 功能3。
     * 与存款镜像：可用余额校验 + 账户等级限额 + 乐观锁扣减 + 复式记账（借1001贷1002）。
     * 柜面渠道额外记录现金出库明细。
     */
    @Transactional(rollbackFor = Exception.class)
    public WithdrawResponse withdraw(WithdrawRequest req) {
        validateTransactionRequest(req.getOutTradeNo(), req.getCardNo(), req.getPassword(),
                req.getTransAmount(), req.getChannel());
        // 1. 幂等校验
        BusinessTransaction existing = transactionMapper.selectByOutTradeNo(req.getOutTradeNo());
        if (existing != null && existing.getStatus().equals(TransactionEnums.Status.SUCCESS.getCode())) {
            return new WithdrawResponse(existing.getTransNo(), existing.getBalanceAfter(), existing.getStatus());
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
        BigDecimal balanceAfter = updateBalanceWithRetry(account.getAccountId(), req.getTransAmount().negate());

        // 6. 记录交易流水（借方）
        BusinessTransaction trans = buildTransaction(account.getAccountId(), req.getOutTradeNo(),
                TransactionEnums.DcFlag.DEBIT.getCode(), TransType.WITHDRAW.getCode(),
                req.getTransAmount(), balanceAfter, req.getChannel(), req.getOperatorId(), account.getBranchCode());
        trans.setRemark(req.getRemark());
        transactionMapper.insert(trans);

        // 7. 会计分录（借1001活期存款 / 贷1002库存现金）
        accountingService.generateEntries(trans);

        // 8. 记录现金出库
        CashTransaction cashOut = new CashTransaction();
        cashOut.setTransId(trans.getTransId());
        cashOut.setTellerId(req.getOperatorId());
        cashOut.setCashType(TransactionEnums.CashType.OUT.getCode());
        cashOut.setAmount(req.getTransAmount());
        cashTransactionMapper.insert(cashOut);

        return new WithdrawResponse(trans.getTransNo(), balanceAfter, trans.getStatus());
    }

    // 功能4：转账交易

    /**
     * 转账交易 — 对应基线文档 功能4。
     * 行内转账，转出方扣减 + 转入方增加，双流水通过 related_trans_id 互相关联，
     * 两套会计分录在同一事务中完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public TransferResponse transfer(TransferRequest req) {
        validateTransactionRequest(req.getOutTradeNo(), req.getFromCardNo(), req.getPassword(),
                req.getTransAmount(), req.getChannel());
        if (isEmpty(req.getToCardNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "转入方卡号不能为空");
        // 1. 幂等校验：同一outTradeNo可能已有转出流水，直接查
        BusinessTransaction existing = transactionMapper.selectByOutTradeNo(req.getOutTradeNo());
        if (existing != null && existing.getStatus().equals(TransactionEnums.Status.SUCCESS.getCode())) {
            // 查找关联的转入流水
            BusinessTransaction related = transactionMapper.selectById(existing.getRelatedTransId());
            return new TransferResponse(existing.getTransNo(), related != null ? related.getTransNo() : null,
                    existing.getBalanceAfter(), existing.getStatus());
        }

        // 2. 转出方账户定位 + 验密
        Account fromAccount = locateAndAuthAccount(req.getFromCardNo(), req.getPassword());

        // 3. 转入方账户校验（需为活期且状态正常）
        Account toAccount = validateToAccount(req.getToCardNo());

        if (fromAccount.getAccountId().equals(toAccount.getAccountId())) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "不能向自己转账");
        }

        // 4. 转出方可用余额 + 等级限额
        BigDecimal available = fromAccount.getBalance().subtract(fromAccount.getFrozenAmount());
        if (available.compareTo(req.getTransAmount()) < 0) {
            throw new BusinessException(ResultCode.BALANCE_INSUFFICIENT);
        }
        checkLevelLimit(fromAccount.getAccountLevel(), req.getTransAmount());

        // 5. 乐观锁：转出方扣减
        BigDecimal fromBalanceAfter = updateBalanceWithRetry(fromAccount.getAccountId(), req.getTransAmount().negate());

        // 6. 乐观锁：转入方增加
        BigDecimal toBalanceAfter = updateBalanceWithRetry(toAccount.getAccountId(), req.getTransAmount());

        // 7. 双流水记录 — 共用同一个交易流水号，通过 related_trans_id 关联
        String transferNo = generateTransNo(fromAccount.getBranchCode(), TransType.TRANSFER.getCode());

        BusinessTransaction fromTrans = buildTransaction(transferNo, fromAccount.getAccountId(), req.getOutTradeNo(),
                TransactionEnums.DcFlag.DEBIT.getCode(), TransType.TRANSFER.getCode(),
                req.getTransAmount(), fromBalanceAfter, req.getChannel(), req.getOperatorId());
        fromTrans.setCounterPartyAccount(req.getToCardNo());
        fromTrans.setRemark(req.getRemark());
        transactionMapper.insert(fromTrans);

        // 转入方幂等号加后缀避唯一约束，交易流水号与转出方相同
        BusinessTransaction toTrans = buildTransaction(transferNo, toAccount.getAccountId(), req.getOutTradeNo() + "_TO",
                TransactionEnums.DcFlag.CREDIT.getCode(), TransType.TRANSFER.getCode(),
                req.getTransAmount(), toBalanceAfter, req.getChannel(), req.getOperatorId());
        toTrans.setCounterPartyAccount(req.getFromCardNo());
        toTrans.setRelatedTransId(fromTrans.getTransId());
        toTrans.setRemark(req.getRemark());
        transactionMapper.insert(toTrans);

        // 互相绑定
        transactionMapper.updateRelatedTransId(fromTrans.getTransId(), toTrans.getTransId());

        // 8. 双套会计分录（dcFlag 在 buildTransaction 时已设置，无需再设）
        accountingService.generateEntries(fromTrans);
        accountingService.generateEntries(toTrans);

        return new TransferResponse(fromTrans.getTransNo(), toTrans.getTransNo(),
                fromBalanceAfter, fromTrans.getStatus());
    }

    /** 校验转入方账户存在、状态正常、且为活期账户。 */
    private Account validateToAccount(String cardNo) {
        Account account = accountMapper.selectByCardNo(cardNo);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND, "转入方账户不存在");
        }
        if (account.getStatus() != AccountEnums.Status.NORMAL.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN, "转入方账户状态异常");
        }
        if (!AccountEnums.Type.DEMAND_DEPOSIT.getCode().equals(account.getAccountType())) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "转入方不是活期存款账户");
        }
        return account;
    }

    /** 账户等级限额：Ⅱ类单笔≤1万，Ⅲ类单笔≤1000。 */
    private void checkLevelLimit(int accountLevel, BigDecimal amount) {
        if (accountLevel == AccountEnums.Level.LEVEL_II.getCode()
                && amount.compareTo(new BigDecimal("10000.00")) > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LEVEL_LIMIT, "Ⅱ类账户单笔交易不得超过10000元");
        }
        if (accountLevel == AccountEnums.Level.LEVEL_III.getCode()
                && amount.compareTo(new BigDecimal("1000.00")) > 0) {
            throw new BusinessException(ResultCode.ACCOUNT_LEVEL_LIMIT, "Ⅲ类账户单笔交易不得超过1000元");
        }
    }

    // 功能6：修改客户信息 

    /**
     * 修改客户信息 — 对应基线文档 功能6。
     * 通过卡号+密码鉴权，更新客户联系电话和通讯地址，证件信息不可修改。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long updateCustomer(UpdateCustomerRequest req) {
        if (isEmpty(req.getCardNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "卡号不能为空");
        if (isEmpty(req.getPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "密码不能为空");
        if (isEmpty(req.getPhone()) && isEmpty(req.getAddress()))
            throw new BusinessException(ResultCode.PARAM_MISSING, "至少需要修改一项（电话或地址）");
        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());
        Customer customer = new Customer();
        customer.setCustomerId(account.getCustomerId());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());
        customerMapper.updateContact(customer);
        return account.getCustomerId();
    }

    // 功能8：交易流水查询 

    /**
     * 交易流水查询 — 对应基线文档 功能8。
     * 密码鉴权 + 时间跨度风控（≤90天）+ 分页检索 + 对外脱敏。
     */
    public TransactionQueryResponse queryTransactions(TransactionQueryRequest req) {
        if (isEmpty(req.getCardNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "卡号不能为空");
        if (isEmpty(req.getPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "密码不能为空");
        if (isEmpty(req.getStartDate())) throw new BusinessException(ResultCode.PARAM_MISSING, "起始时间不能为空");
        if (isEmpty(req.getEndDate())) throw new BusinessException(ResultCode.PARAM_MISSING, "结束时间不能为空");

        // 1. 鉴权
        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());

        // 2. 时间跨度风控
        LocalDateTime start, end;
        try {
            start = LocalDateTime.parse(req.getStartDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            end = LocalDateTime.parse(req.getEndDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "时间格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
        if (ChronoUnit.DAYS.between(start, end) > 90) {
            throw new BusinessException(ResultCode.TIME_RANGE_TOO_LARGE);
        }
        if (end.isBefore(start)) {
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "结束时间不能早于开始时间");
        }

        // 3. 分页参数
        int pageNum = req.getPageNum() != null ? req.getPageNum() : 1;
        int pageSize = req.getPageSize() != null ? req.getPageSize() : 10;
        int offset = (pageNum - 1) * pageSize;

        // 4. 总数 + 分页数据
        long total = transactionMapper.countByAccountAndTime(account.getAccountId(), start, end, req.getTransType());
        List<BusinessTransaction> list = transactionMapper.selectByAccountAndTimePaged(
                account.getAccountId(), start, end, req.getTransType(), pageSize, offset);

        // 5. 组装并脱敏（不暴露 trans_id、对方账号完整明文等）
        List<TransactionItem> items = list.stream().map(t -> new TransactionItem(
                t.getTransTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                t.getTransType(),
                t.getDcFlag(),
                t.getCounterPartyAccount(),
                t.getTransAmount(),
                t.getBalanceAfter(),
                t.getRemark()
        )).toList();

        return new TransactionQueryResponse(total, items);
    }

    // 功能9：修改密码

    /**
     * 修改密码 — 通过卡号+旧密码鉴权，更新为新密码。
     */
    @Transactional(rollbackFor = Exception.class)
    public ChangePasswordResponse changePassword(ChangePasswordRequest req) {
        if (isEmpty(req.getCardNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "卡号不能为空");
        if (isEmpty(req.getOldPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "原密码不能为空");
        if (isEmpty(req.getNewPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "新密码不能为空");

        Account account = locateAndAuthAccount(req.getCardNo(), req.getOldPassword());

        String newPasswordHash = PasswordUtil.encode(req.getNewPassword());
        accountMapper.updatePassword(account.getAccountId(), newPasswordHash);
        return new ChangePasswordResponse(true, "密码修改成功");
    }

    // 功能10：查询账户信息

    /**
     * 查询账户信息 — 通过卡号+密码鉴权，返回账户完整信息（含脱敏字段）。
     */
    public AccountInfoResponse queryAccount(QueryAccountRequest req) {
        if (isEmpty(req.getCardNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "卡号不能为空");
        if (isEmpty(req.getPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "密码不能为空");

        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());
        Customer customer = customerMapper.selectById(account.getCustomerId());

        // 脱敏处理
        String maskedCardNo = maskCardNo(account.getCardNo());
        String maskedName = maskName(customer != null ? customer.getCustomerName() : "");

        BigDecimal availableBalance = account.getBalance().subtract(account.getFrozenAmount());

        return new AccountInfoResponse(
                maskedCardNo,
                account.getAccountNo(),
                maskedName,
                account.getAccountType(),
                account.getAccountLevel(),
                account.getCurrency(),
                account.getBranchCode(),
                account.getBalance(),
                account.getFrozenAmount(),
                availableBalance,
                account.getStatus(),
                account.getOpenDate()
        );
    }

    /** 卡号脱敏：只显示前6位和后4位 */
    private String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 10) return cardNo;
        return cardNo.substring(0, 6) + "****" + cardNo.substring(cardNo.length() - 4);
    }

    /** 姓名脱敏：保留首字，其余用 * */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    // 日终余额快照

    /**
     * 日终余额快照 — 对所有正常账户做每日余额快照，存入日积数底表。
     * 已存在当日快照的账户自动跳过（幂等），单账户失败不中断整体流程。
     */
    public DailyBalanceResponse dailyBalance() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<Account> accounts = accountMapper.selectAllNormal();
        int success = 0, skip = 0;
        for (Account acc : accounts) {
            try {
                if (dailyBalanceMapper.selectByAccountAndDate(acc.getAccountId(), yesterday) != null) {
                    skip++;
                    continue;
                }
                BigDecimal endBalance;
                BusinessTransaction lastTrans = transactionMapper
                        .selectLastByAccountAndDate(acc.getAccountId(), startOfYesterday, startOfToday);
                if (lastTrans != null) {
                    endBalance = lastTrans.getBalanceAfter();
                } else {
                    DailyBalance prev = dailyBalanceMapper.selectLatestBefore(acc.getAccountId(), yesterday);
                    endBalance = prev != null ? prev.getEndBalance() : BigDecimal.ZERO;
                }
                DailyBalance db = new DailyBalance();
                db.setAccountId(acc.getAccountId());
                db.setBalanceDate(yesterday);
                db.setEndBalance(endBalance);
                dailyBalanceMapper.insert(db);
                success++;
            } catch (Exception e) {
                // 单账户失败不中断
            }
        }
        return new DailyBalanceResponse(yesterday, accounts.size(), success, skip);
    }

    // 功能5：销户

    /**
     * 销户 — 验密、清空余额、乐观锁更新状态为 CLOSED。
     * 若有冻结金额则拒绝销户；若余额>0 则先强制取款全部余额再销户。
     */
    @Transactional(rollbackFor = Exception.class)
    public CloseAccountResponse closeAccount(CloseAccountRequest req) {
        if (isEmpty(req.getCardNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "卡号不能为空");
        if (isEmpty(req.getPassword())) throw new BusinessException(ResultCode.PARAM_MISSING, "密码不能为空");
        if (isEmpty(req.getOutTradeNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "幂等号不能为空");

        // 1. 幂等校验：同一 outTradeNo 代表已销户
        BusinessTransaction existing = transactionMapper.selectByOutTradeNo(req.getOutTradeNo());
        if (existing != null) {
            Account acct = accountMapper.selectById(existing.getAccountId());
            return new CloseAccountResponse(acct.getAccountNo(), acct.getCardNo(),
                    acct.getCloseDate(), acct.getStatus());
        }

        // 2. 账户定位 + 验密 + 状态检查
        Account account = locateAndAuthAccount(req.getCardNo(), req.getPassword());
        if (account.getStatus() == AccountEnums.Status.CLOSED.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_CLOSED);
        }

        // 3. 校验冻结金额
        if (account.getFrozenAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ResultCode.FROZEN_AMOUNT_EXISTS);
        }

        // 4. 销户前强制结息，将未结利息计入余额
        settleInterestForClose(account);
        account = accountMapper.selectById(account.getAccountId());

        // 5. 若余额>0，先强制取款全部余额（记录流水+分录），乐观锁扣到0
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balance = account.getBalance();
            BigDecimal balanceAfter = updateBalanceWithRetry(account.getAccountId(), balance.negate());

            BusinessTransaction withdrawTrans = buildTransaction(account.getAccountId(),
                    req.getOutTradeNo() + "_WTH", TransactionEnums.DcFlag.DEBIT.getCode(),
                    TransType.WITHDRAW.getCode(), balance, balanceAfter,
                    "SYSTEM", "SYSTEM", account.getBranchCode());
            withdrawTrans.setRemark("销户-余额清退");
            transactionMapper.insert(withdrawTrans);
            accountingService.generateEntries(withdrawTrans);

            // 重新获取最新版本号用于下一步状态更新
            account = accountMapper.selectById(account.getAccountId());
        }

        // 5. 乐观锁更新状态为 CLOSED(2)，closeDate=今天
        Account toClose = new Account();
        toClose.setAccountId(account.getAccountId());
        toClose.setStatus(AccountEnums.Status.CLOSED.getCode());
        toClose.setCloseDate(LocalDate.now());
        toClose.setVersion(account.getVersion());
        int affected = accountMapper.updateStatusWithVersion(toClose);
        if (affected == 0) {
            throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
        }

        // 6. 记录销户交易流水 + 分录
        BusinessTransaction closeTrans = buildTransaction(account.getAccountId(), req.getOutTradeNo(),
                TransactionEnums.DcFlag.DEBIT.getCode(), TransType.CLOSE_ACCOUNT.getCode(),
                BigDecimal.ZERO, BigDecimal.ZERO, "SYSTEM", "SYSTEM", account.getBranchCode());
        closeTrans.setRemark("账户销户");
        transactionMapper.insert(closeTrans);
        accountingService.generateEntries(closeTrans);

        return new CloseAccountResponse(account.getAccountNo(), account.getCardNo(),
                toClose.getCloseDate(), toClose.getStatus());
    }

    /**
     * 销户前结息：计算截止到昨天的未结利息并派发到余额，无积数则跳过。
     */
    private void settleInterestForClose(Account account) {
        // 计息区间
        LocalDate startDate = account.getLastSettlementDate() != null
                ? account.getLastSettlementDate().plusDays(1)
                : account.getOpenDate().plusDays(1);
        LocalDate endDate = LocalDate.now().minusDays(1);
        if (startDate.isAfter(endDate)) return;

        // 积数和
        BigDecimal accumulated = dailyBalanceMapper.sumBalanceByAccountAndDateRange(
                account.getAccountId(), startDate, endDate);
        if (accumulated == null || accumulated.compareTo(BigDecimal.ZERO) == 0) return;

        // 日利率
        InterestRateConfig rateConfig = interestRateConfigMapper.selectActiveRate(
                account.getAccountType(), account.getCurrency(), LocalDate.now());
        if (rateConfig == null) return;

        // 利息 = 积数和 × 日利率，向下取整2位
        BigDecimal interest = accumulated.multiply(rateConfig.getRateValue())
                .setScale(2, RoundingMode.FLOOR);
        if (interest.compareTo(BigDecimal.ZERO) == 0) return;

        // 乐观锁加利息 + 更新结算日
        updateBalanceAndSettlementWithRetry(account.getAccountId(), interest);

        // 重新读取最新余额
        Account latest = accountMapper.selectById(account.getAccountId());

        // 交易流水
        BusinessTransaction trans = buildTransaction(account.getAccountId(),
                "INT_CLOSE_" + account.getAccountId() + "_" + System.currentTimeMillis(),
                TransactionEnums.DcFlag.CREDIT.getCode(), TransType.INTEREST.getCode(),
                interest, latest.getBalance(), "SYSTEM", "SYSTEM", account.getBranchCode());
        trans.setRemark("销户前强制结息");
        transactionMapper.insert(trans);
        accountingService.generateEntries(trans);

        // 结息审计记录
        InterestSettlement settlement = new InterestSettlement();
        settlement.setAccountId(account.getAccountId());
        settlement.setSettlementDate(LocalDate.now());
        settlement.setAccumulatedAmount(accumulated);
        settlement.setAppliedRate(rateConfig.getRateValue());
        settlement.setInterestDays((int) ChronoUnit.DAYS.between(startDate, endDate) + 1);
        settlement.setInterestAmount(interest);
        settlement.setTransId(trans.getTransId());
        interestSettlementMapper.insert(settlement);
    }

    /** 乐观锁更新余额 + 结息日期（销户前结息专用） */
    private void updateBalanceAndSettlementWithRetry(Long accountId, BigDecimal delta) {
        for (int i = 0; i < MAX_RETRY; i++) {
            Account latest = accountMapper.selectById(accountId);
            latest.setBalance(latest.getBalance().add(delta));
            latest.setLastSettlementDate(LocalDate.now());
            int affected = accountMapper.updateBalanceAndSettlementDateWithVersion(latest);
            if (affected > 0) return;
        }
        throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
    }

    // 功能11：结息

    @Transactional(rollbackFor = Exception.class)
    public InterestSettlementDTO settleInterest(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        if (account.getStatus() != AccountEnums.Status.NORMAL.getCode())
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN, "账户状态异常，无法结息");

        LocalDate startDate = account.getLastSettlementDate() != null
                ? account.getLastSettlementDate().plusDays(1) : account.getOpenDate().plusDays(1);
        LocalDate endDate = LocalDate.now().minusDays(1);
        if (startDate.isAfter(endDate)) return null;

        BigDecimal accumulated = dailyBalanceMapper.sumBalanceByAccountAndDateRange(accountId, startDate, endDate);
        if (accumulated == null || accumulated.compareTo(BigDecimal.ZERO) == 0) return null;

        InterestRateConfig rateConfig = interestRateConfigMapper.selectActiveRate(
                account.getAccountType(), account.getCurrency(), LocalDate.now());
        if (rateConfig == null) throw new BusinessException(ResultCode.RATE_NOT_FOUND);

        int interestDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal interestAmount = accumulated.multiply(rateConfig.getRateValue())
                .setScale(2, RoundingMode.FLOOR);
        if (interestAmount.compareTo(BigDecimal.ZERO) == 0) return null;

        updateBalanceAndSettlementWithRetry(accountId, interestAmount);
        Account latest = accountMapper.selectById(accountId);

        BusinessTransaction trans = buildTransaction(accountId,
                "INT_" + accountId + "_" + System.currentTimeMillis(),
                TransactionEnums.DcFlag.CREDIT.getCode(), TransType.INTEREST.getCode(),
                interestAmount, latest.getBalance(), "SYSTEM", "SYSTEM", account.getBranchCode());
        trans.setRemark("活期存款结息");
        transactionMapper.insert(trans);
        accountingService.generateEntries(trans);

        InterestSettlement settlement = new InterestSettlement();
        settlement.setAccountId(accountId);
        settlement.setSettlementDate(LocalDate.now());
        settlement.setAccumulatedAmount(accumulated);
        settlement.setAppliedRate(rateConfig.getRateValue());
        settlement.setInterestDays(interestDays);
        settlement.setInterestAmount(interestAmount);
        settlement.setTransId(trans.getTransId());
        interestSettlementMapper.insert(settlement);

        return new InterestSettlementDTO(settlement.getSettlementId(), settlement.getSettlementDate(),
                accumulated, rateConfig.getRateValue(), interestDays, interestAmount, trans.getTransId());
    }

    /**
     * 批量结息 — 每账户通过代理走独立事务，单账户失败不中断整体。
     */
    public Map<Long, String> settleInterestAll() {
        List<Account> accounts = accountMapper.selectAllNormal();
        Map<Long, String> result = new LinkedHashMap<>();
        for (Account acc : accounts) {
            try {
                InterestSettlementDTO dto = settleInterestSelf(acc.getAccountId());
                result.put(acc.getAccountId(), dto != null ? "SUCCESS: " + dto.getInterestAmount() + "元" : "无需结息");
            } catch (Exception e) {
                result.put(acc.getAccountId(), "FAILED: " + (e instanceof BusinessException ? e.getMessage() : "系统错误"));
            }
        }
        return result;
    }

    /** 代理方法，让 settleInterestAll 的每账户调用走独立的 @Transactional */
    @Transactional(rollbackFor = Exception.class)
    public InterestSettlementDTO settleInterestSelf(Long accountId) {
        return settleInterest(accountId);
    }

    // ==================== 公共辅助方法 ====================

    /**
     * 乐观锁更新余额，并发冲突时重试（最多 {@link #MAX_RETRY} 次）。
     * 每次重试重新读取最新的 account 版本号。
     */
    /**
     * 乐观锁更新余额。
     * 每次重试都重新读取最新余额和版本号，避免基于过期余额计算导致"丢钱"。
     * @param delta 资金变动量：存款/转入为正，取款/转出为负
     */
    /**
     * 乐观锁更新余额。
     * 每次重试都重新读取最新余额和版本号，delta 为负时每轮校验可用余额。
     * @param delta 资金变动量：存款/转入为正，取款/转出为负
     */
    private BigDecimal updateBalanceWithRetry(Long accountId, BigDecimal delta) {
        for (int i = 0; i < MAX_RETRY; i++) {
            Account latest = accountMapper.selectById(accountId);
            // 取款/转出时每轮重算可用余额，防止 TOCTOU 导致余额变负
            if (delta.signum() < 0) {
                BigDecimal available = latest.getBalance().subtract(latest.getFrozenAmount());
                if (available.compareTo(delta.abs()) < 0) {
                    throw new BusinessException(ResultCode.BALANCE_INSUFFICIENT);
                }
            }
            BigDecimal newBalance = latest.getBalance().add(delta);
            int affected = accountMapper.updateBalanceWithVersion(accountId, newBalance, latest.getVersion());
            if (affected > 0) {
                return newBalance;
            }
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
        if (account.getStatus() == AccountEnums.Status.FROZEN.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        if (account.getStatus() == AccountEnums.Status.CLOSED.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_CLOSED);
        }
        if (!PasswordUtil.matches(rawPassword, account.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        return account;
    }

    /** 构建交易流水对象，自动生成业务交易流水号。 */
    private BusinessTransaction buildTransaction(Long accountId, String outTradeNo,
                                                  String dcFlag, String transType,
                                                  BigDecimal amount, BigDecimal balanceAfter,
                                                  String channel, String operatorId, String branchCode) {
        return buildTransaction(generateTransNo(branchCode, transType),
                accountId, outTradeNo, dcFlag, transType, amount, balanceAfter, channel, operatorId);
    }

    /** 构建交易流水对象，使用指定的 transNo（转账双方共用）。 */
    private BusinessTransaction buildTransaction(String transNo, Long accountId, String outTradeNo,
                                                  String dcFlag, String transType,
                                                  BigDecimal amount, BigDecimal balanceAfter,
                                                  String channel, String operatorId) {
        BusinessTransaction trans = new BusinessTransaction();
        trans.setTransNo(transNo);
        trans.setAccountId(accountId);
        trans.setOutTradeNo(outTradeNo);
        trans.setDcFlag(dcFlag);
        trans.setTransType(transType);
        trans.setTransAmount(amount);
        trans.setBalanceAfter(balanceAfter);
        trans.setChannel(channel);
        trans.setOperatorId(operatorId);
        trans.setTransTime(LocalDateTime.now());
        trans.setStatus(TransactionEnums.Status.SUCCESS.getCode());
        return trans;
    }

    /**
     * 生成业务交易流水号：机构号(6) + 日期(8) + 交易类型(2) + 时分秒(6) + 纳秒末4位 + 随机码(2)
     * 纳秒粒度保证同秒内几乎不可能碰撞。
     */
    private String generateTransNo(String branchCode, String transType) {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePart = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String nanoPart = String.format("%04d", System.nanoTime() % 10000);
        String seq = String.format("%02d", ThreadLocalRandom.current().nextInt(100));
        return branchCode + datePart + transType + timePart + nanoPart + seq;
    }

    // ==================== 开户辅助方法 ====================

    private Long getOrCreateCustomer(OpenAccountRequest req) {
        Customer existing = customerMapper.selectByIdTypeAndIdNumber(req.getIdType(), req.getIdNumber());
        if (existing != null) {
            return existing.getCustomerId();
        }
        Customer customer = new Customer();
        customer.setCustomerName(req.getCustomerName());
        customer.setType(CustomerEnums.Type.PERSONAL.getCode());
        customer.setIdType(req.getIdType());
        customer.setIdNumber(req.getIdNumber());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());

        if ("01".equals(req.getIdType())) {
            if (!IdCardUtil.isValid(req.getIdNumber())) {
                throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "身份证号不合法");
            }
            customer.setDateOfBirth(IdCardUtil.birthday(req.getIdNumber()));
            customer.setGender(IdCardUtil.gender(req.getIdNumber()));
        }

        customer.setBranch(req.getBranchCode());
        customer.setStatus(CustomerEnums.Status.NORMAL.getCode());
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
        if (isEmpty(req.getOutTradeNo())) throw new BusinessException(ResultCode.PARAM_MISSING, "幂等号不能为空");
        if (isEmpty(req.getChannel())) throw new BusinessException(ResultCode.PARAM_MISSING, "开户渠道不能为空");
    }

    /** 交易公共参数校验 */
    private void validateTransactionRequest(String outTradeNo, String cardNo, String password,
                                             BigDecimal amount, String channel) {
        if (isEmpty(outTradeNo)) throw new BusinessException(ResultCode.PARAM_MISSING, "幂等号不能为空");
        if (isEmpty(cardNo)) throw new BusinessException(ResultCode.PARAM_MISSING, "卡号不能为空");
        if (isEmpty(password)) throw new BusinessException(ResultCode.PARAM_MISSING, "密码不能为空");
        if (isEmpty(channel)) throw new BusinessException(ResultCode.PARAM_MISSING, "交易渠道不能为空");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException(ResultCode.PARAM_FORMAT_ERROR, "交易金额必须大于0");
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
