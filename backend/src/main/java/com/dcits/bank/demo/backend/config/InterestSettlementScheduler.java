package com.dcits.bank.demo.backend.config;

import com.dcits.bank.demo.backend.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 季度结息定时任务 — 每季度末月 21 号 02:00 执行
 */
@Component
public class InterestSettlementScheduler {

    private static final Logger log = LoggerFactory.getLogger(InterestSettlementScheduler.class);

    private final AccountService accountService;

    public InterestSettlementScheduler(AccountService accountService) {
        this.accountService = accountService;
    }

    @Scheduled(cron = "0 0 2 21 3,6,9,12 ?")
    public void run() {
        log.info("季度结息开始...");
        Map<Long, String> result = accountService.settleInterestAll();
        long success = result.values().stream().filter(v -> v.startsWith("SUCCESS")).count();
        long fail = result.values().stream().filter(v -> v.startsWith("FAILED")).count();
        long skip = result.values().stream().filter(v -> v.equals("无需结息")).count();
        log.info("季度结息完成: total={}, success={}, fail={}, skip={}", result.size(), success, fail, skip);
    }
}
