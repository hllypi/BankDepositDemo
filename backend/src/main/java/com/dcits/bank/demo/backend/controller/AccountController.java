package com.dcits.bank.demo.backend.controller;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.dto.*;
import com.dcits.bank.demo.backend.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @Operation(summary = "存款", description = "向活期存款账户存入资金。幂等防重，乐观锁保证并发安全，柜面渠道记录现金入库。")
    @PostMapping("/deposit")
    public ApiResult<DepositResponse> deposit(@RequestBody DepositRequest request) {
        return ApiResult.success(accountService.deposit(request));
    }

    @Operation(summary = "取款", description = "从活期存款账户支取资金。校验可用余额及账户等级限额，乐观锁保证并发安全，柜面渠道额外记录现金出库。")
    @PostMapping("/withdraw")
    public ApiResult<WithdrawResponse> withdraw(@RequestBody WithdrawRequest request) {
        return ApiResult.success(accountService.withdraw(request));
    }

    @Operation(summary = "转账", description = "行内转账，转出方扣减，转入方增加。双流水通过related_trans_id互相关联，两套会计分录在同一事务中完成。")
    @PostMapping("/transfer")
    public ApiResult<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ApiResult.success(accountService.transfer(request));
    }

    @Operation(summary = "修改客户信息", description = "通过卡号+密码鉴权后，更新客户的联系电话和通讯地址。证件信息不可修改。")
    @PutMapping("/customer")
    public ApiResult<Long> updateCustomer(@RequestBody UpdateCustomerRequest request) {
        return ApiResult.success(accountService.updateCustomer(request));
    }

    @Operation(summary = "交易流水查询", description = "查询账户动账明细。密码鉴权，时间跨度不超过90天，分页返回脱敏后的交易记录。")
    @PostMapping("/transactions")
    public ApiResult<TransactionQueryResponse> queryTransactions(@RequestBody TransactionQueryRequest request) {
        return ApiResult.success(accountService.queryTransactions(request));
    }

    @Operation(summary = "销户", description = "关闭账户。验密、清空余额、乐观锁更新状态，记录销户交易流水。")
    @PostMapping("/close")
    public ApiResult<CloseAccountResponse> closeAccount(@RequestBody CloseAccountRequest request) {
        return ApiResult.success(accountService.closeAccount(request));
    }

    @Operation(summary = "修改密码", description = "通过卡号+旧密码鉴权，更新为新密码。")
    @PutMapping("/password")
    public ApiResult<ChangePasswordResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        return ApiResult.success(accountService.changePassword(request));
    }

    @Operation(summary = "查询账户信息", description = "通过卡号+密码鉴权，返回账户完整信息（含脱敏字段）。")
    @PostMapping("/query")
    public ApiResult<AccountInfoResponse> queryAccount(@RequestBody QueryAccountRequest request) {
        return ApiResult.success(accountService.queryAccount(request));
    }

    @Operation(summary = "日终余额快照", description = "对所有正常状态账户做每日余额快照，存到日积数底表。已存在当日快照的账户自动跳过。")
    @PostMapping("/daily-balance")
    public ApiResult<DailyBalanceResponse> dailyBalance() {
        return ApiResult.success(accountService.dailyBalance());
    }

    @Operation(summary = "单账户结息", description = "根据日积数计算利息并更新账户余额，生成会计分录和结息审计记录。")
    @PostMapping("/settle/{accountId}")
    public ApiResult<InterestSettlementDTO> settleInterest(@PathVariable Long accountId) {
        return ApiResult.success(accountService.settleInterest(accountId));
    }

    @Operation(summary = "单账户结息(卡号+密码)", description = "通过卡号+密码鉴权后执行结息。")
    @PostMapping("/settle")
    public ApiResult<InterestSettlementDTO> settleInterestByCard(@RequestBody SettleInterestRequest request) {
        return ApiResult.success(accountService.settleInterestByCard(request.getCardNo(), request.getPassword()));
    }

    @Operation(summary = "全部结息", description = "对所有正常账户逐笔执行结息，每账户独立事务，返回每账户执行结果。")
    @PostMapping("/settle/all")
    public ApiResult<Map<Long, String>> settleInterestAll() {
        return ApiResult.success(accountService.settleInterestAll());
    }
}
