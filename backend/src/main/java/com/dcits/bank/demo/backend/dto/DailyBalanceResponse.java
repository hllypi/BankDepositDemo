package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Schema(description = "日终余额快照响应")
public class DailyBalanceResponse {

    @Schema(description = "快照日期")
    private LocalDate balanceDate;

    @Schema(description = "处理账户总数")
    private int totalAccounts;

    @Schema(description = "快照成功数")
    private int successCount;

    @Schema(description = "跳过数（当日已有快照）")
    private int skipCount;
}
