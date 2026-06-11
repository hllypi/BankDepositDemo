package com.dcits.bank.demo.backend.enums;

public enum RateStatus {
    VALID(1, "有效"),
    INVALID(0, "失效");

    private final int code;
    private final String desc;

    RateStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
