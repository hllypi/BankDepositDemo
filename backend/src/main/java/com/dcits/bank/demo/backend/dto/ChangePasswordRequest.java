package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "银行卡号")
    private String cardNo;

    @Schema(description = "原密码")
    private String oldPassword;

    @Schema(description = "新密码")
    private String newPassword;
}
