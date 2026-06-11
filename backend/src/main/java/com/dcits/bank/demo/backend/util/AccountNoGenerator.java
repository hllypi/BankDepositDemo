package com.dcits.bank.demo.backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 核心内部账号生成器。
 * 格式：机构号(6位) + 日期时分秒(12位) + 随机序列(2位) + 校验位(1位)，共21位。
 */
public final class AccountNoGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private AccountNoGenerator() {}

    /**
     * 按行内规则生成唯一 account_no。
     * @param branchCode 开户机构号
     */
    public static String generate(String branchCode) {
        String datePart = LocalDateTime.now().format(FMT);
        String seqPart = LuhnUtil.randomDigits(2);
        String raw = branchCode + datePart + seqPart;
        return raw + checksum(raw);
    }

    /** 简单的 mod10 校验位 */
    private static int checksum(String s) {
        int sum = 0;
        for (char c : s.toCharArray()) {
            sum += Character.getNumericValue(c);
        }
        return sum % 10;
    }
}
