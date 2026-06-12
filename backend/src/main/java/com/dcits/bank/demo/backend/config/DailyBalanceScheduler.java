package com.dcits.bank.demo.backend.config;

import com.dcits.bank.demo.backend.dto.DailyBalanceResponse;
import com.dcits.bank.demo.backend.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日终余额快照定时任务 — 每日 01:00 执行
 */
@Component
public class DailyBalanceScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyBalanceScheduler.class);

    private final AccountService accountService;

    public DailyBalanceScheduler(AccountService accountService) {
        this.accountService = accountService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void run() {
        log.info("日终余额快照开始...");
        DailyBalanceResponse result = accountService.dailyBalance();
        log.info("日终余额快照完成: date={}, total={}, success={}, skip={}",
                result.getBalanceDate(), result.getTotalAccounts(),
                result.getSuccessCount(), result.getSkipCount());
    }
}
