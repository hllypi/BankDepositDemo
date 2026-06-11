package com.dcits.bank.demo.backend.enums;

public enum DcFlag {
    DEBIT("D", "借方/扣减"),
    CREDIT("C", "贷方/增加");

    private final String code;
    private final String desc;

    DcFlag(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
