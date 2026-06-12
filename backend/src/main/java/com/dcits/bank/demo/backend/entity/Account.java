package com.dcits.bank.demo.backend.entity;

import lombok.Data;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账户信息表 — account
 */
@Data
public class Account {

    /** 内部账户ID（自增主键） */
    private Long accountId;

    /** 核心内部账号（唯一索引 uk_account_no） */
    private String accountNo;

    /** 银行卡号，对外标识（唯一索引 uk_card_no，Luhn校验位） */
    private String cardNo;

    /** 密码密文（BCrypt + 随机Salt，不可逆） */
    @ToString.Exclude
    private String passwordHash;

    /** 所属客户ID（关联 customer.customer_id） */
    private Long customerId;

    /** 账户类型（C01-活期存款） */
    private String accountType;

    /** 账户等级（1-Ⅰ类，2-Ⅱ类，3-Ⅲ类） */
    private Integer accountLevel;

    /** 币种（默认 CNY） */
    private String currency;

    /** 开户行代码 */
    private String branchCode;

    /** 当前总余额 */
    private BigDecimal balance;

    /** 冻结金额（可用余额 = balance - frozen_amount） */
    private BigDecimal frozenAmount;

    /** 挂载利率ID（关联 interest_rate_config.rate_id） */
    private Long rateId;

    /** 上次结息日期（跑批起点，防重复结息） */
    private LocalDate lastSettlementDate;

    /** 账户状态（0-正常，1-冻结，2-销户） */
    private Integer status;

    /** 乐观锁版本号（每次余额变动 +1，防并发覆盖） */
    private Integer version;

    /** 开户日期 */
    private LocalDate openDate;

    /** 销户日期 */
    private LocalDate closeDate;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;
}
