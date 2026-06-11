package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 开户响应 — 对应基线文档 功能1 输出要素。
 */
@Data
@AllArgsConstructor
@Schema(description = "开户响应")
public class OpenAccountResponse {

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "账户ID")
    private Long accountId;

    @Schema(description = "银行卡号（对外，Luhn校验位）", example = "6217001234567890123")
    private String cardNo;

    @Schema(description = "核心内部账号", example = "01000126061115302542")
    private String accountNo;
}
