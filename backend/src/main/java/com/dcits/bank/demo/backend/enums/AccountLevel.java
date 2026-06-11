package com.dcits.bank.demo.backend.enums;

public enum AccountLevel {
    LEVEL_I(1, "Ⅰ类户"),
    LEVEL_II(2, "Ⅱ类户"),
    LEVEL_III(3, "Ⅲ类户");

    private final int code;
    private final String desc;

    AccountLevel(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
