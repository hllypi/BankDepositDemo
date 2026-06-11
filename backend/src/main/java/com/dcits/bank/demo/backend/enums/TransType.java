package com.dcits.bank.demo.backend.enums;

public enum TransType {
    OPEN_ACCOUNT("00", "开户"),
    DEPOSIT("01", "存款"),
    WITHDRAW("02", "取款"),
    TRANSFER("03", "转账"),
    INTEREST("04", "结息"),
    CLOSE_ACCOUNT("05", "销户");

    private final String code;
    private final String desc;

    TransType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
