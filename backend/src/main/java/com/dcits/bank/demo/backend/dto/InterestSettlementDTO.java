package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结息响应 DTO
 */
@Data
@AllArgsConstructor
@Schema(description = "结息响应")
public class InterestSettlementDTO {

    @Schema(description = "结息记录ID")
    private Long settlementId;

    @Schema(description = "结息日期")
    private LocalDate settlementDate;

    @Schema(description = "积数总和")
    private BigDecimal accumulatedAmount;

    @Schema(description = "日利率")
    private BigDecimal appliedRate;

    @Schema(description = "计息天数")
    private Integer interestDays;

    @Schema(description = "利息金额")
    private BigDecimal interestAmount;

    @Schema(description = "关联流水ID")
    private Long transId;
}
