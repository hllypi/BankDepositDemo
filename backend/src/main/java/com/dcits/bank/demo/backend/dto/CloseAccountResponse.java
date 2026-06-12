package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Schema(description = "销户响应")
public class CloseAccountResponse {

    @Schema(description = "核心内部账号")
    private String accountNo;

    @Schema(description = "银行卡号")
    private String cardNo;

    @Schema(description = "销户日期")
    private LocalDate closeDate;

    @Schema(description = "账户状态（2-销户）")
    private Integer status;
}
