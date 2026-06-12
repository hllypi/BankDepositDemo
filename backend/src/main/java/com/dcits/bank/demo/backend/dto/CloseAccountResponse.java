package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * 销户响应 DTO
 */
@Data
@AllArgsConstructor
@Schema(description = "销户响应")
public class CloseAccountResponse {

    @Schema(description = "核心内部账号")
    private String accountNo;

    @Schema(description = "银行卡号")
    private String cardNo;

    @Schema(description = "销户日期")
    private LocalDate closedDate;

    @Schema(description = "账户状态")
    private Integer status;
}
