package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 转账请求 — 对应基线文档 功能4 输入要素。
 */
@Data
@Schema(description = "转账请求")
public class TransferRequest {

    @Schema(description = "外部请求幂等号", example = "TRF20260611001")
    @NotBlank(message = "幂等号不能为空")
    @Size(max = 64, message = "幂等号长度不能超过64位")
    private String outTradeNo;

    @Schema(description = "转出方银行卡号", example = "6217003588954801077")
    @NotBlank(message = "转出方卡号不能为空")
    @Pattern(regexp = "^\\d{1,19}$", message = "转出方卡号必须为1-19位数字")
    private String fromCardNo;

    @Schema(description = "转入方银行卡号", example = "6217005771031977676")
    @NotBlank(message = "转入方卡号不能为空")
    @Pattern(regexp = "^\\d{1,19}$", message = "转入方卡号必须为1-19位数字")
    private String toCardNo;

    @Schema(description = "转入方客户姓名", example = "张三")
    @NotBlank(message = "转入方客户姓名不能为空")
    @Size(max = 50, message = "转入方客户姓名长度不能超过50位")
    private String toCustomerName;

    @Schema(description = "转出方账户密码", example = "123456")
    @NotBlank(message = "账户密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "账户密码必须为6位数字")
    private String password;

    @Schema(description = "转账金额", example = "2000.00")
    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0.01", message = "转账金额必须大于0")
    @Digits(integer = 16, fraction = 2, message = "转账金额最多16位整数和2位小数")
    private BigDecimal transAmount;

    @Schema(description = "交易渠道（APP/COUNTER/ATM）", example = "APP")
    @NotBlank(message = "交易渠道不能为空")
    @Pattern(regexp = "^(APP|COUNTER|ATM)$", message = "交易渠道只能为APP、COUNTER或ATM")
    private String channel;

    @Schema(description = "经办人/系统标识", example = "APP_USER")
    @Size(max = 50, message = "经办人长度不能超过50位")
    private String operatorId;

    @Schema(description = "交易摘要", example = "手机银行转账")
    @Size(max = 100, message = "交易摘要长度不能超过100位")
    private String remark;
}
