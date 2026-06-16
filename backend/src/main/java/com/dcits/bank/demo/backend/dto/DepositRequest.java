package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 存款请求 — 对应基线文档 功能2 输入要素。
 */
@Data
@Schema(description = "存款请求")
public class DepositRequest {

    @Schema(description = "外部请求幂等号，最长64字符，防重复入账", example = "DEP20260611001")
    private String outTradeNo;

    @Schema(description = "银行卡号", example = "6217003588954801077")
    private String cardNo;

    @Schema(description = "账户密码", example = "123456")
    private String password;

    @Schema(description = "存款金额", example = "10000.00")
    private BigDecimal transAmount;

    @Schema(description = "交易渠道（APP/COUNTER/ATM）", example = "COUNTER")
    private String channel;

    @Schema(description = "经办人/系统标识，柜面渠道时传柜员编号", example = "TELLER001")
    private String operatorId;

    @Schema(description = "交易摘要", example = "现金存入")
    private String remark;

    @Schema(description = "测试用交易时间", example = "现金存入")
    private LocalDateTime transTime;
}
