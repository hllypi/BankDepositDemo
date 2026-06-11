package com.dcits.bank.demo.backend.controller;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.dto.*;
import com.dcits.bank.demo.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账户管理", description = "开户、存款、取款、转账、销户等账户全生命周期管理")
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "客户开户", description = "创建客户信息并开立个人活期存款账户，返回银行卡号和核心内部账号。同一证件可复用已有客户。")
    @PostMapping("/open")
    public ApiResult<OpenAccountResponse> openAccount(@RequestBody OpenAccountRequest request) {
        return ApiResult.success(accountService.openAccount(request));
    }

    @Operation(summary = "存款", description = "向活期存款账户存入资金。幂等防重，乐观锁保证并发安全，柜面渠道额外记录现金入库。")
    @PostMapping("/deposit")
    public ApiResult<DepositResponse> deposit(@RequestBody DepositRequest request) {
        return ApiResult.success(accountService.deposit(request));
    }

    @Operation(summary = "取款", description = "从活期存款账户支取资金。校验可用余额及账户等级限额，乐观锁保证并发安全，柜面渠道额外记录现金出库。")
    @PostMapping("/withdraw")
    public ApiResult<WithdrawResponse> withdraw(@RequestBody WithdrawRequest request) {
        return ApiResult.success(accountService.withdraw(request));
    }
}
