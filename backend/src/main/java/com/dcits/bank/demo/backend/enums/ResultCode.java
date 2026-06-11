package com.dcits.bank.demo.backend.enums;

public enum ResultCode {

    SUCCESS(0, "成功"),

    // 参数校验 1xxx
    PARAM_MISSING(1001, "缺少必填参数"),
    PARAM_FORMAT_ERROR(1002, "参数格式不合法"),

    // 账户 2xxx
    ACCOUNT_NOT_FOUND(2001, "账户不存在"),
    ACCOUNT_FROZEN(2002, "账户已冻结"),
    ACCOUNT_CLOSED(2003, "账户已销户"),
    PASSWORD_ERROR(2004, "密码错误"),
    BALANCE_INSUFFICIENT(2005, "余额不足"),
    ACCOUNT_LEVEL_LIMIT(2006, "超出账户等级限额"),
    FROZEN_AMOUNT_EXISTS(2007, "存在冻结金额，不可销户"),

    // 幂等 3xxx
    DUPLICATE_REQUEST(3001, "重复请求"),

    // 查询 4xxx
    TIME_RANGE_TOO_LARGE(4001, "查询时间跨度过大，请缩短范围"),

    // 并发 5xxx
    CONCURRENT_CONFLICT(5001, "系统繁忙，请重试"),

    // 利率 6xxx
    RATE_NOT_FOUND(6001, "未找到适用利率"),

    // 系统 9xxx
    SYSTEM_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
