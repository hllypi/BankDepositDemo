package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "修改密码响应")
public class ChangePasswordResponse {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "结果消息")
    private String message;
}
