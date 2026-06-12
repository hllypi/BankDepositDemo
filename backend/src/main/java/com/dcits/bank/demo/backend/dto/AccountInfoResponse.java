package com.dcits.bank.demo.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@Schema(description = "账户信息响应")
public class AccountInfoResponse {

    @Schema(description = "银行卡号（脱敏）")
    private String cardNo;

    @Schema(description = "核心内部账号")
    private String accountNo;

    @Schema(description = "客户姓名（脱敏）", example = "张**")
    private String customerName;

    @Schema(description = "账户类型")
    private String accountType;

    @Schema(description = "账户等级")
    private Integer accountLevel;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "开户行代码")
    private String branchCode;

    @Schema(description = "当前总余额")
    private BigDecimal balance;

    @Schema(description = "冻结金额")
    private BigDecimal frozenAmount;

    @Schema(description = "可用余额")
    private BigDecimal availableBalance;

    @Schema(description = "账户状态（0-正常，1-冻结，2-销户）")
    private Integer status;

    @Schema(description = "开户日期")
    private LocalDate openDate;
}
