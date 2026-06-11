package com.dcits.bank.demo.backend.enums;

public enum CustomerStatus {
    NORMAL(0, "正常"),
    CANCELLED(1, "注销"),
    FROZEN(2, "冻结");

    private final int code;
    private final String desc;

    CustomerStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
