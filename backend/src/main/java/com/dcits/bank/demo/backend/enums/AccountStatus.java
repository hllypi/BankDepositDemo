package com.dcits.bank.demo.backend.enums;

public enum AccountStatus {
    NORMAL(0, "正常"),
    FROZEN(1, "冻结"),
    CLOSED(2, "销户");

    private final int code;
    private final String desc;

    AccountStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
