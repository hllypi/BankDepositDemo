package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 取款请求 — 对应基线文档 功能3 输入要素。
 */
@Data
@Schema(description = "取款请求")
public class WithdrawRequest {

    @Schema(description = "外部请求幂等号", example = "WTH20260611001")
    private String outTradeNo;

    @Schema(description = "银行卡号", example = "6217003588954801077")
    private String cardNo;

    @Schema(description = "账户密码", example = "123456")
    private String password;

    @Schema(description = "取款金额", example = "3000.00")
    private BigDecimal transAmount;

    @Schema(description = "交易渠道（APP/COUNTER/ATM）", example = "APP")
    private String channel;

    @Schema(description = "经办人/系统标识", example = "APP_USER")
    private String operatorId;

    @Schema(description = "交易摘要", example = "ATM取款")
    private String remark;
}
