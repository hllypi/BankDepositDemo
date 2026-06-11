package com.dcits.bank.demo.backend.enums;

public enum AccountType {
    DEMAND_DEPOSIT("C01", "活期存款");

    private final String code;
    private final String desc;

    AccountType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
