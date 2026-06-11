package com.dcits.bank.demo.backend.enums;

public enum Channel {
    APP("APP", "手机银行"),
    COUNTER("COUNTER", "柜面"),
    ATM("ATM", "自动柜员机"),
    SYSTEM("SYSTEM", "系统");

    private final String code;
    private final String desc;

    Channel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
