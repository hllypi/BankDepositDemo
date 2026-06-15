package com.dcits.bank.demo.backend.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务交易流水表 — business_transaction
 */
@Data
public class BusinessTransaction {

    /** 内部交易流水号（自增主键，仅内部关联用） */
    private Long transId;

    /** 交易流水号（业务编号） */
    private String transNo;


    /** 关联流水号（转账双边账互绑的纽带） */
    private Long relatedTransId;

    /** 发生账户ID（关联 account.account_id） */
    private Long accountId;

    /** 对方对外账号（转账时记录对手方卡号） */
    private String counterPartyAccount;

    /** 资金方向标识（D-借方/扣减，C-贷方/增加） */
    private String dcFlag;

    /** 交易类型（00-开户，01-存款，02-取款，03-转账，04-结息，05-销户） */
    private String transType;
    private String currency;

    /** 交易金额绝对值（CHECK约束 trans_amount >= 0） */
    private BigDecimal transAmount;

    /** 交易后总余额 */
    private BigDecimal balanceAfter;
    private BigDecimal frozenAmountAfter;

    /** 交易渠道（APP / COUNTER / ATM / SYSTEM） */
    private String channel;

    /** 经办人/系统标识 */
    private String operatorId;

    /** 交易发生时间 */
    private LocalDateTime transTime;

    /** 状态机（0-处理中，1-成功，2-失败，3-冲正） */
    private Integer status;

    /** 交易摘要 */
    private String remark;

    /** 落库时间 */
    private LocalDateTime createdTime;

    /** 状态更新时间 */
    private LocalDateTime updateTime;
}
