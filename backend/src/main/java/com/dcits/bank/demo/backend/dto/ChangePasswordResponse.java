package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 修改密码响应 DTO
 */
@Data
@AllArgsConstructor
@Schema(description = "修改密码响应")
public class ChangePasswordResponse {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "提示消息")
    private String message;
}
