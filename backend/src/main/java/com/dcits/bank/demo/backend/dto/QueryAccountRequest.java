package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "查询账户请求")
public class QueryAccountRequest {

    @Schema(description = "银行卡号")
    private String cardNo;

    @Schema(description = "账户密码")
    private String password;
}
