package com.dcits.bank.demo.backend.controller;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.dto.OpenAccountRequest;
import com.dcits.bank.demo.backend.dto.OpenAccountResponse;
import com.dcits.bank.demo.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "账户管理", description = "开户、销户等账户全生命周期管理")
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "客户开户", description = "创建客户信息并开立个人活期存款账户，返回银行卡号和核心内部账号。支持证件类型+证件号码唯一识别客户，已有客户可复用。")
    @PostMapping("/open")
    public ApiResult<OpenAccountResponse> openAccount(@RequestBody OpenAccountRequest request) {
        OpenAccountResponse result = accountService.openAccount(request);
        return ApiResult.success(result);
    }
}
