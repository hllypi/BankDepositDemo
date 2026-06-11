package com.dcits.bank.demo.backend.enums;

public enum TransStatus {
    PROCESSING(0, "处理中"),
    SUCCESS(1, "成功"),
    FAILED(2, "失败"),
    REVERSED(3, "冲正");

    private final int code;
    private final String desc;

    TransStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
