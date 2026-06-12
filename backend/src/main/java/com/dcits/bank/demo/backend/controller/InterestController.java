package com.dcits.bank.demo.backend.controller;

import com.dcits.bank.demo.backend.common.ApiResult;
import com.dcits.bank.demo.backend.dto.InterestSettlementDTO;
import com.dcits.bank.demo.backend.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "结息管理", description = "日终积数生成、单账户结息、批量结息")
@RestController
@RequestMapping("/api/interest")
public class InterestController {

    private final InterestService interestService;

    public InterestController(InterestService interestService) {
        this.interestService = interestService;
    }

    @Operation(summary = "日终积数生成", description = "扫描所有正常账户，为每个账户插入当日余额快照，幂等防重。返回处理账户数量。")
    @PostMapping("/daily-balance")
    public ApiResult<Integer> generateDailyBalances() {
        return ApiResult.success(interestService.generateDailyBalances());
    }

    @Operation(summary = "单账户结息", description = "根据日积数计算利息并更新账户余额，生成会计分录和结息审计记录。")
    @PostMapping("/settle/{accountId}")
    public ApiResult<InterestSettlementDTO> settleInterest(@PathVariable Long accountId) {
        return ApiResult.success(interestService.settleInterest(accountId));
    }

    @Operation(summary = "全部结息", description = "对所有正常账户逐笔执行结息，返回每账户执行结果。")
    @PostMapping("/settle/all")
    public ApiResult<Map<Long, String>> settleInterestAll() {
        return ApiResult.success(interestService.settleInterestAll());
    }
}
