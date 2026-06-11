package com.dcits.bank.demo.backend.enums;

public enum IdType {
    ID_CARD("01", "身份证"),
    PASSPORT("02", "护照"),
    MILITARY("03", "军官证");

    private final String code;
    private final String desc;

    IdType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
