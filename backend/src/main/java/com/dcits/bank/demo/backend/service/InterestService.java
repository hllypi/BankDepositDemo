package com.dcits.bank.demo.backend.service;

import com.dcits.bank.demo.backend.dto.InterestSettlementDTO;
import com.dcits.bank.demo.backend.entity.*;
import com.dcits.bank.demo.backend.enums.*;
import com.dcits.bank.demo.backend.exception.BusinessException;
import com.dcits.bank.demo.backend.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InterestService {

    private static final int MAX_RETRY = 3;

    private final AccountMapper accountMapper;
    private final DailyBalanceMapper dailyBalanceMapper;
    private final InterestRateConfigMapper interestRateConfigMapper;
    private final InterestSettlementMapper interestSettlementMapper;
    private final BusinessTransactionMapper transactionMapper;
    private final AccountingService accountingService;

    public InterestService(AccountMapper accountMapper,
                           DailyBalanceMapper dailyBalanceMapper,
                           InterestRateConfigMapper interestRateConfigMapper,
                           InterestSettlementMapper interestSettlementMapper,
                           BusinessTransactionMapper transactionMapper,
                           AccountingService accountingService) {
        this.accountMapper = accountMapper;
        this.dailyBalanceMapper = dailyBalanceMapper;
        this.interestRateConfigMapper = interestRateConfigMapper;
        this.interestSettlementMapper = interestSettlementMapper;
        this.transactionMapper = transactionMapper;
        this.accountingService = accountingService;
    }

    /**
     * 日终积数生成：扫描所有正常账户，为每个账户插入当日余额快照。幂等。
     * @return 处理账户数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int generateDailyBalances() {
        List<Account> accounts = accountMapper.selectAllNormal();
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Account account : accounts) {
            DailyBalance existing = dailyBalanceMapper.selectByAccountAndDate(account.getAccountId(), today);
            if (existing != null) {
                continue;
            }
            DailyBalance db = new DailyBalance();
            db.setAccountId(account.getAccountId());
            db.setBalanceDate(today);
            db.setEndBalance(account.getBalance());
            dailyBalanceMapper.insert(db);
            count++;
        }
        return count;
    }

    /**
     * 单账户结息：计算积数和 → 取利率 → 算利息 → 乐观锁加余额 → 分录 → 记录结息审计
     * @param accountId 账户ID
     * @return 结息结果 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public InterestSettlementDTO settleInterest(Long accountId) {
        // 1. 校验账户存在且正常
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() != AccountEnums.Status.NORMAL.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN, "账户状态异常，无法结息");
        }

        // 2. 确定计息区间：lastSettlementDate 之后（若为null则从openDate之后）到昨天
        LocalDate startDate = account.getLastSettlementDate() != null
                ? account.getLastSettlementDate().plusDays(1)
                : account.getOpenDate().plusDays(1);
        LocalDate endDate = LocalDate.now().minusDays(1);

        if (startDate.isAfter(endDate)) {
            return null; // 无需结息，前端判断 settlementId 为 null
        }

        // 3. 从 daily_balance 查积数和
        BigDecimal accumulatedAmount = dailyBalanceMapper.sumBalanceByAccountAndDateRange(
                account.getAccountId(), startDate, endDate);
        if (accumulatedAmount == null || accumulatedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // 4. 取有效日利率
        InterestRateConfig rateConfig = interestRateConfigMapper.selectActiveRate(
                account.getAccountType(), account.getCurrency(), LocalDate.now());
        if (rateConfig == null) {
            throw new BusinessException(ResultCode.RATE_NOT_FOUND);
        }
        BigDecimal dailyRate = rateConfig.getRateValue();
        int interestDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // 5. 计算利息 = 积数和 × 日利率，保留2位小数向下取整
        BigDecimal interestAmount = accumulatedAmount.multiply(dailyRate)
                .setScale(2, RoundingMode.FLOOR);

        if (interestAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // 6. 乐观锁给余额加利息，更新 lastSettlementDate
        BigDecimal newBalance = updateBalanceAndSettlementWithRetry(account.getAccountId(), interestAmount);

        // 7. 记录交易流水 transType='04' INTEREST, dcFlag='C'
        String transNo = generateTransNo(account.getBranchCode(), TransType.INTEREST.getCode());
        BusinessTransaction trans = new BusinessTransaction();
        trans.setTransNo(transNo);
        trans.setAccountId(account.getAccountId());
        trans.setOutTradeNo("INT_" + account.getAccountId() + "_" + System.currentTimeMillis());
        trans.setDcFlag(TransactionEnums.DcFlag.CREDIT.getCode());
        trans.setTransType(TransType.INTEREST.getCode());
        trans.setTransAmount(interestAmount);
        trans.setBalanceAfter(newBalance);
        trans.setChannel("SYSTEM");
        trans.setOperatorId("SYSTEM");
        trans.setTransTime(LocalDateTime.now());
        trans.setStatus(TransactionEnums.Status.SUCCESS.getCode());
        trans.setRemark("活期存款结息");
        transactionMapper.insert(trans);

        // 8. 会计分录
        accountingService.generateEntries(trans);

        // 9. 写入结息审计记录
        InterestSettlement settlement = new InterestSettlement();
        settlement.setAccountId(account.getAccountId());
        settlement.setSettlementDate(LocalDate.now());
        settlement.setAccumulatedAmount(accumulatedAmount);
        settlement.setAppliedRate(dailyRate);
        settlement.setInterestDays(interestDays);
        settlement.setInterestAmount(interestAmount);
        settlement.setTransId(trans.getTransId());
        interestSettlementMapper.insert(settlement);

        return new InterestSettlementDTO(
                settlement.getSettlementId(), settlement.getSettlementDate(),
                accumulatedAmount, dailyRate, interestDays, interestAmount, trans.getTransId());
    }

    /**
     * 全部结息：查所有正常账户，逐个结息，try-catch包裹
     * @return Map<Long, String> (accountId → "SUCCESS: X元" / "FAILED: 原因" / "无需结息")
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<Long, String> settleInterestAll() {
        List<Account> accounts = accountMapper.selectAllNormal();
        Map<Long, String> result = new LinkedHashMap<>();
        for (Account account : accounts) {
            try {
                InterestSettlementDTO dto = settleInterest(account.getAccountId());
                if (dto == null) {
                    result.put(account.getAccountId(), "无需结息");
                } else {
                    result.put(account.getAccountId(), "SUCCESS: " + dto.getInterestAmount() + "元");
                }
            } catch (Exception e) {
                String msg = e instanceof BusinessException ? e.getMessage() : "系统错误";
                result.put(account.getAccountId(), "FAILED: " + msg);
            }
        }
        return result;
    }

    /**
     * 乐观锁更新余额 + 结息日期，失败重试
     */
    private BigDecimal updateBalanceAndSettlementWithRetry(Long accountId, BigDecimal delta) {
        for (int i = 0; i < MAX_RETRY; i++) {
            Account latest = accountMapper.selectById(accountId);
            BigDecimal newBalance = latest.getBalance().add(delta);
            latest.setBalance(newBalance);
            latest.setLastSettlementDate(LocalDate.now());
            int affected = accountMapper.updateBalanceAndSettlementDateWithVersion(latest);
            if (affected > 0) {
                return newBalance;
            }
        }
        throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
    }

    private String generateTransNo(String branchCode, String transType) {
        LocalDate today = LocalDate.now();
        String datePart = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String timePart = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String nanoPart = String.format("%04d", System.nanoTime() % 10000);
        String seq = String.format("%02d", ThreadLocalRandom.current().nextInt(100));
        return branchCode + datePart + transType + timePart + nanoPart + seq;
    }
}
