package com.dcits.bank.demo.backend.enums;

/**
 * 交易类型，每个枚举值自带复式记账分录模板（1借 + 1贷）。
 * 转账(03)受 DcFlag 影响，规则由 AccountingService 处理。
 */
public enum TransType {

    OPEN_ACCOUNT("00", "开户",
            AccountingEnums.AccountCode.CASH_IN_VAULT,  AccountingEnums.AccountCode.DEMAND_DEPOSIT,
            "库存现金",                                  "个人活期存款开户"),

    DEPOSIT("01", "存款",
            AccountingEnums.AccountCode.CASH_IN_VAULT,  AccountingEnums.AccountCode.DEMAND_DEPOSIT,
            "现金存入",                                  "活期存款"),

    WITHDRAW("02", "取款",
            AccountingEnums.AccountCode.DEMAND_DEPOSIT, AccountingEnums.AccountCode.CASH_IN_VAULT,
            "活期存款",                              "库存现金"),

    /** 转账分录由 DcFlag 决定：D(转出)→借1001贷1004，C(转入)→借1004贷1001 */
    TRANSFER("03", "转账",
            AccountingEnums.AccountCode.DEMAND_DEPOSIT, AccountingEnums.AccountCode.INTERNAL_CLEARING,
            "活期存款",                              "行内清算"),

    INTEREST("04", "结息",
            AccountingEnums.AccountCode.INTEREST_EXPENSE, AccountingEnums.AccountCode.DEMAND_DEPOSIT,
            "利息支出",                                   "活期存款结息"),

    CLOSE_ACCOUNT("05", "销户",
            AccountingEnums.AccountCode.DEMAND_DEPOSIT, AccountingEnums.AccountCode.CASH_IN_VAULT,
            "活期存款销户",                              "库存现金");

    private final String code;
    private final String desc;
    private final AccountingEnums.AccountCode debitCode;
    private final AccountingEnums.AccountCode creditCode;
    private final String debitSummary;
    private final String creditSummary;

    TransType(String code, String desc,
              AccountingEnums.AccountCode debitCode, AccountingEnums.AccountCode creditCode,
              String debitSummary, String creditSummary) {
        this.code = code;
        this.desc = desc;
        this.debitCode = debitCode;
        this.creditCode = creditCode;
        this.debitSummary = debitSummary;
        this.creditSummary = creditSummary;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
    public AccountingEnums.AccountCode getDebitCode() { return debitCode; }
    public AccountingEnums.AccountCode getCreditCode() { return creditCode; }
    public String getDebitSummary() { return debitSummary; }
    public String getCreditSummary() { return creditSummary; }

    public static TransType fromCode(String code) {
        for (TransType t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("未知交易类型: " + code);
    }
}
