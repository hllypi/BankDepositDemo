package com.dcits.bank.demo.backend.enums;

public enum CustomerType {
    PERSONAL("01", "个人"),
    CORPORATE("02", "企业");

    private final String code;
    private final String desc;

    CustomerType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
