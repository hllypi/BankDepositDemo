package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 销户请求 DTO
 */
@Data
@Schema(description = "销户请求")
public class CloseAccountRequest {

    @Schema(description = "银行卡号", example = "6217003588954801077")
    private String cardNo;

    @Schema(description = "账户密码", example = "123456")
    private String password;

    @Schema(description = "外部请求幂等号", example = "CLS20260612001")
    private String outTradeNo;
}
