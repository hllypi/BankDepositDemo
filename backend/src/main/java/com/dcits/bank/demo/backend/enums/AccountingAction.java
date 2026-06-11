package com.dcits.bank.demo.backend.enums;

public enum AccountingAction {
    DEBIT(1, "借"),
    CREDIT(2, "贷");

    private final int code;
    private final String desc;

    AccountingAction(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
