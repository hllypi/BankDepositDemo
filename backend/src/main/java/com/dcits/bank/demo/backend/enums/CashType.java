package com.dcits.bank.demo.backend.enums;

public enum CashType {
    IN("I", "入库"),
    OUT("O", "出库");

    private final String code;
    private final String desc;

    CashType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
