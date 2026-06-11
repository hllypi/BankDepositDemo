package com.dcits.bank.demo.backend.util;

import java.security.SecureRandom;

/**
 * Luhn 算法工具，用于生成银行卡号的校验位。
 * 银行卡号规则：前 N-1 位为标识码 + 序列号，最后一位为 Luhn 校验位。
 */
public final class LuhnUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private LuhnUtil() {}

    /**
     * 给定不含校验位的前缀（BIN + 序列号），计算并返回完整的 Luhn 合规卡号。
     */
    public static String generateCardNo(String prefixWithoutCheckDigit) {
        int checkDigit = calculateLuhnCheckDigit(prefixWithoutCheckDigit);
        return prefixWithoutCheckDigit + checkDigit;
    }

    /**
     * 生成一个指定总长度的随机数字串（不含校验位），用于卡号中间序列号部分。
     */
    public static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 验证完整卡号的 Luhn 校验位是否合法。
     */
    public static boolean isValid(String cardNo) {
        return calculateLuhnCheckDigit(cardNo.substring(0, cardNo.length() - 1))
                == Character.getNumericValue(cardNo.charAt(cardNo.length() - 1));
    }

    /**
     * Luhn 算法：从右向左，偶数位乘2，累加各位数字之和，返回 (10 - sum%10) % 10。
     */
    private static int calculateLuhnCheckDigit(String digits) {
        int sum = 0;
        boolean doubleIt = true; // 从最右边开始算（不含校验位），所以第一位要加倍
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = Character.getNumericValue(digits.charAt(i));
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return (10 - (sum % 10)) % 10;
    }
}
