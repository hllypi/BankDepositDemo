package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 存款响应 — 对应基线文档 功能2 输出要素。
 */
@Data
@AllArgsConstructor
@Schema(description = "存款响应")
public class DepositResponse {

    @Schema(description = "内部交易流水号")
    private Long transId;

    @Schema(description = "存款后账户总余额")
    private BigDecimal balanceAfter;

    @Schema(description = "交易状态（1-成功）")
    private Integer status;
}
