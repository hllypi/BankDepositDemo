package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改密码请求 DTO
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordRequest {

    @Schema(description = "银行卡号", example = "6217003588954801077")
    private String cardNo;

    @Schema(description = "旧密码", example = "123456")
    private String oldPassword;

    @Schema(description = "新密码（6-20位）", example = "654321")
    private String newPassword;
}
