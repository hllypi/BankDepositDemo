package com.dcits.bank.demo.backend.service;

import com.dcits.bank.demo.backend.dto.OpenAccountRequest;
import com.dcits.bank.demo.backend.dto.OpenAccountResponse;
import com.dcits.bank.demo.backend.entity.*;
import com.dcits.bank.demo.backend.enums.*;
import com.dcits.bank.demo.backend.exception.BusinessException;
import com.dcits.bank.demo.backend.mapper.*;
import com.dcits.bank.demo.backend.util.AccountNoGenerator;
import com.dcits.bank.demo.backend.util.LuhnUtil;
import com.dcits.bank.demo.backend.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AccountService {

    /** 模拟发卡行标识码 BIN，6位 */
    private static final String CARD_BIN = "621700";

    private final CustomerMapper customerMapper;
    private final AccountMapper accountMapper;
    private final BusinessTransactionMapper transactionMapper;
    private final AccountingEntryMapper entryMapper;

    public AccountService(CustomerMapper customerMapper,
                          AccountMapper accountMapper,
                          BusinessTransactionMapper transactionMapper,
                          AccountingEntryMapper entryMapper) {
        this.customerMapper = customerMapper;
        this.accountMapper = accountMapper;
        this.transactionMapper = transactionMapper;
        this.entryMapper = entryMapper;
    }

    /**
     * 客户开户 — 对应基线文档 功能1。
     * 整个开户流程在一个本地事务中完成，保证客户/账户/流水/分录的一致性。
     */
    @Transactional(rollbackFor = Exception.class)
    public OpenAccountResponse openAccount(OpenAccountRequest req) {
        // 1. 参数校验
        validateRequest(req);

        // 2. 客户查询：证件类型+证件号码唯一确定客户，存在则复用，不存在则新建
        Long customerId = getOrCreateCustomer(req);

        // 3. 生成账号 & 卡号，密码哈希
        String accountNo = AccountNoGenerator.generate(req.getBranchCode());
        String cardNo = generateUniqueCardNo();
        String passwordHash = PasswordUtil.encode(req.getPassword());
        String currency = req.getCurrency() != null ? req.getCurrency() : "CNY";

        // 4. 创建账户记录
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

        // 5. 记录开户交易流水（贷方，金额为零）
        String outTradeNo = "OPEN_" + cardNo + "_" + System.currentTimeMillis();
        BusinessTransaction trans = new BusinessTransaction();
        trans.setOutTradeNo(outTradeNo);
        trans.setAccountId(account.getAccountId());
        trans.setDcFlag(DcFlag.CREDIT.getCode());
        trans.setTransType(TransType.OPEN_ACCOUNT.getCode());
        trans.setTransAmount(BigDecimal.ZERO);
        trans.setBalanceAfter(BigDecimal.ZERO);
        trans.setChannel(req.getChannel());
        trans.setOperatorId("SYSTEM");
        trans.setTransTime(LocalDateTime.now());
        trans.setStatus(TransStatus.SUCCESS.getCode());
        transactionMapper.insert(trans);

        // 6. 生成会计分录（零金额，仅做开户记账标记）
        AccountingEntry entry = new AccountingEntry();
        entry.setVoucherId("VCH" + account.getAccountNo());
        entry.setTransId(trans.getTransId());
        entry.setAccountCode("1001"); // 活期存款科目代码，后续由枚举统一管理
        entry.setAction(AccountingAction.CREDIT.getCode());
        entry.setAmount(BigDecimal.ZERO);
        entry.setSummary("个人活期存款开户");
        entryMapper.insert(entry);

        return new OpenAccountResponse(customerId, account.getAccountId(), cardNo, accountNo);
    }

    /**
     * 根据证件类型+证件号码查询客户，已存在则复用，否则新建。
     */
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
        customer.setDateOfBirth(req.getDateOfBirth() != null ? LocalDate.parse(req.getDateOfBirth()) : null);
        customer.setGender(req.getGender());
        customer.setAge(req.getAge());
        customer.setBranch(req.getBranchCode());
        customer.setStatus(CustomerStatus.NORMAL.getCode());
        customerMapper.insert(customer);
        return customer.getCustomerId();
    }

    /**
     * 生成 Luhn 合规的唯一卡号：BIN(6位) + 随机序列(12位) + 校验位(1位) = 19位。
     */
    private String generateUniqueCardNo() {
        // 简单实现：BIN + 12位随机 + 校验位。实际生产需保证序列号唯一性。
        String prefix = CARD_BIN + LuhnUtil.randomDigits(12);
        return LuhnUtil.generateCardNo(prefix);
    }

    private void validateRequest(OpenAccountRequest req) {
        if (req.getCustomerName() == null || req.getCustomerName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "客户姓名不能为空");
        }
        if (req.getIdType() == null || req.getIdType().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "证件类型不能为空");
        }
        if (req.getIdNumber() == null || req.getIdNumber().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "证件号码不能为空");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "账户密码不能为空");
        }
        if (req.getPhone() == null || req.getPhone().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "联系电话不能为空");
        }
        if (req.getAddress() == null || req.getAddress().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "通讯地址不能为空");
        }
        if (req.getBranchCode() == null || req.getBranchCode().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "开户行代码不能为空");
        }
        if (req.getChannel() == null || req.getChannel().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "开户渠道不能为空");
        }
    }
}
