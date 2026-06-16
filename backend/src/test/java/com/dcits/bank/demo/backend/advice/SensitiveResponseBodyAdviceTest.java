package com.dcits.bank.demo.backend.advice;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.dto.AccountInfoResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitiveResponseBodyAdviceTest {

    @Test
    void beforeBodyWriteShouldDesensitizeAccountInfoResponse() {
        SensitiveResponseBodyAdvice advice = new SensitiveResponseBodyAdvice();
        AccountInfoResponse accountInfo = new AccountInfoResponse(
                "6217007133587136694",
                "ACCT202606160001",
                "罗超",
                "C01",
                1,
                "CNY",
                "010001",
                new BigDecimal("3000.00"),
                BigDecimal.ZERO,
                new BigDecimal("3000.00"),
                0,
                LocalDate.of(2026, 6, 16)
        );
        ApiResult<AccountInfoResponse> result = ApiResult.success(accountInfo);

        advice.beforeBodyWrite(result, null, null, null, null, null);

        AccountInfoResponse desensitized = result.getData();
        assertNotEquals("6217007133587136694", desensitized.getCardNo());
        assertTrue(desensitized.getCardNo().contains("*"));
        assertNotEquals("罗超", desensitized.getCustomerName());
        assertTrue(desensitized.getCustomerName().contains("*"));
        assertEquals("ACCT202606160001", desensitized.getAccountNo());
    }
}
