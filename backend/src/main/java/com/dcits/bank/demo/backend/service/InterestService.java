package com.dcits.bank.demo.backend.service;

import com.dcits.bank.demo.backend.dto.DepositRequest;
import com.dcits.bank.demo.backend.dto.DepositResponse;
import com.dcits.bank.demo.backend.dto.InterestRequest;
import com.dcits.bank.demo.backend.entity.*;
import com.dcits.bank.demo.backend.enums.AccountEnums;
import com.dcits.bank.demo.backend.enums.ResultCode;
import com.dcits.bank.demo.backend.enums.TransType;
import com.dcits.bank.demo.backend.enums.TransactionEnums;
import com.dcits.bank.demo.backend.exception.BusinessException;
import com.dcits.bank.demo.backend.mapper.*;
import com.dcits.bank.demo.backend.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InterestService {
    private final DailyBalanceMapper dailyBalanceMapper;
    private final AccountMapper accountMapper;
    private final BusinessTransactionMapper businessTransactionMapper;
    private final AccountingService accountingService;
    private final CashTransactionMapper cashTransactionMapper;
    private final IdempotencyService idempotencyService;
    private final InterestRateConfigMapper interestRateConfigMapper;
    @Autowired
    public InterestService(
            DailyBalanceMapper dailyBalanceMapper,
            AccountMapper accountMapper,
            BusinessTransactionMapper businessTransactionMapper,
            AccountingService accountingService,
            CashTransactionMapper cashTransactionMapper,
            IdempotencyService idempotencyService,
            InterestRateConfigMapper interestRateConfigMapper
    ) {
        this.dailyBalanceMapper = dailyBalanceMapper;
        this.accountMapper = accountMapper;
        this.businessTransactionMapper = businessTransactionMapper;
        this.accountingService = accountingService;
        this.cashTransactionMapper = cashTransactionMapper;
        this.idempotencyService = idempotencyService;
        this.interestRateConfigMapper = interestRateConfigMapper;
    }


    /**
     * 定时任务, 每天01:30执行, 每次执行记录上一天的余额
     *
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void execute() {
        dailyBalanceSchedule(LocalDate.now());
    }

    @Transactional(rollbackFor = Exception.class)
    public Object dailyBalanceSchedule(LocalDate excDate) {
        LocalDate targetDate = excDate.plusDays( -1 );

        // 获取所有正常且未处理过的账户
        List<Long> allAcc = dailyBalanceMapper.selectAllNormalAccount(targetDate);

        // 处理这些账户
        for(Long acc : allAcc) {
            // 获取前一天的金额
            BigDecimal curBalance = dailyBalanceMapper.selectLastDayBalance(
                    acc,
                    targetDate.plusDays( -1 )
            );
            // 如果前一天就没有余额, 就搁置
            if(curBalance == null) {
                // 开户当天
                if(accountMapper.selectById(acc).getOpenDate().equals(targetDate)) {
                    curBalance = BigDecimal.valueOf(0L);
                }
                else {
                    runDailyBalanceSchedule(acc, targetDate);
                    continue;
                }
            }
            // 获取当天的流水和
            BigDecimal transSum = businessTransactionMapper.selectSumByAccAndTime(
                    acc,
                    targetDate
            );
            // 没有流水就记0
            if(transSum == null) transSum = BigDecimal.valueOf(0L);

            DailyBalance dailyBalanceDTO = new DailyBalance();
//            dailyBalanceDTO.setDailyBalanceId(0L); 自增
//            dailyBalanceDTO.setCreatedTime(null); 默认
//            dailyBalanceDTO.setUpdateTime(null); 默认
            dailyBalanceDTO.setAccountId(acc);
            dailyBalanceDTO.setCurrency("CNY");
            dailyBalanceDTO.setBalanceDate(targetDate);
            dailyBalanceDTO.setEndBalance(curBalance.add(transSum));

            dailyBalanceMapper.insert(dailyBalanceDTO);
        }

        return null;
    }

    @Transactional(rollbackFor = Exception.class)
    public void runDailyBalanceSchedule(Long accountId, LocalDate targetDate) {
        DailyBalance lastEntry = dailyBalanceMapper.selectLastEntry(accountId);
        if(lastEntry == null) {
            Account account = accountMapper.selectById(accountId);
            if(account == null) {
                throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
            }
            lastEntry = new DailyBalance();
            lastEntry.setBalanceDate(account.getOpenDate().plusDays(-1));
            lastEntry.setEndBalance(BigDecimal.valueOf(0L));
        }
        BigDecimal curBalance = lastEntry.getEndBalance();
        for(
                LocalDate curDate = lastEntry.getBalanceDate().plusDays(1);
                !curDate.isAfter(targetDate);
                curDate = curDate.plusDays(1)
        ) {
            BigDecimal transSum = businessTransactionMapper.selectSumByAccAndTime(
                    accountId,
                    curDate
            );
            if(transSum == null) transSum = BigDecimal.valueOf(0L);

            curBalance = curBalance.add(transSum);
            DailyBalance dailyBalanceDTO = new DailyBalance();
//            dailyBalanceDTO.setDailyBalanceId(0L); 自增
//            dailyBalanceDTO.setCreatedTime(null); 默认
//            dailyBalanceDTO.setUpdateTime(null); 默认
            dailyBalanceDTO.setAccountId(accountId);
            dailyBalanceDTO.setCurrency("CNY");
            dailyBalanceDTO.setBalanceDate(curDate);
            dailyBalanceDTO.setEndBalance(curBalance);

            dailyBalanceMapper.insert(dailyBalanceDTO);
        }

    }

    @Transactional(rollbackFor = Exception.class)
    public DepositResponse deposit(DepositRequest req) {

        Account account = accountMapper.selectByCardNo(req.getCardNo());


        BigDecimal newBalance = account.getBalance().add(req.getTransAmount());
        int affected = accountMapper.updateBalanceWithVersion(account.getAccountId(), newBalance, account.getVersion());
        if (affected <= 0) {
            throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
        }

        BusinessTransaction trans = new BusinessTransaction();
        {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String timePart = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String nanoPart = String.format("%04d", System.nanoTime() % 10000);
            String seq = String.format("%02d", ThreadLocalRandom.current().nextInt(100));

            trans.setTransNo(account.getBranchCode() + datePart + TransType.DEPOSIT.getCode() + timePart + nanoPart + seq);
        }
        trans.setAccountId(account.getAccountId());
        trans.setDcFlag(TransactionEnums.DcFlag.CREDIT.getCode());
        trans.setTransType(TransType.DEPOSIT.getCode());
        trans.setCurrency("CNY");
        trans.setTransAmount(req.getTransAmount());
        trans.setBalanceAfter(newBalance);
        trans.setFrozenAmountAfter(BigDecimal.ZERO);
        trans.setChannel(req.getChannel());
        trans.setOperatorId(req.getOperatorId());
        trans.setTransTime(req.getTransTime());
        trans.setStatus(TransactionEnums.Status.SUCCESS.getCode());

        businessTransactionMapper.insert(trans);

        // 5. 会计分录（借1002库存现金 / 贷1001活期存款）
        accountingService.generateEntries(trans);

        // 6. 记录现金入库
        CashTransaction cashIn = new CashTransaction();
        cashIn.setTransId(trans.getTransId());
        cashIn.setTellerId(req.getOperatorId());
        cashIn.setCashType(TransactionEnums.CashType.IN.getCode());
        cashIn.setAmount(req.getTransAmount());
        cashIn.setBranchCode(account.getBranchCode());
        cashIn.setBoxId(account.getBranchCode());
        cashIn.setCurrency(account.getCurrency());
        cashIn.setBoxBalanceAfter(BigDecimal.ZERO);
        cashIn.setStatus(1);
        cashTransactionMapper.insert(cashIn);

        DepositResponse resp = new DepositResponse(
                trans.getTransNo(),
                newBalance,
                trans.getStatus()
        );
        idempotencyService.save(req.getOutTradeNo(), resp);
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public BigDecimal calInterest(Long accountId, LocalDate curDate) {
        InterestRateConfig interestRateConfig = interestRateConfigMapper.selectByRateId(2L);
        BigDecimal activeRate = interestRateConfig.getRateValue();

        BigDecimal interestBase = dailyBalanceMapper.selectBalanceSum(accountId, curDate);
        if(activeRate == null || interestBase == null) {
            throw new BusinessException(ResultCode.RATE_NOT_FOUND);
        }
        BigDecimal dayRate =
                BigDecimal.valueOf(
                        activeRate.doubleValue() / 360
                ).setScale(2, RoundingMode.HALF_UP);
        return interestBase.multiply(dayRate);
    }

    @Transactional(rollbackFor = Exception.class)
    public Object interest(InterestRequest req) {
        if(
            req.getOutTradeNo() == null ||
            req.getCardNo() == null ||
            req.getPassword() == null ||
            req.getInterestDate() == null
        ) {
            throw new BusinessException(ResultCode.PARAM_MISSING);
        }
        // 1. 幂等校验
        Object cached = idempotencyService.check(req.getOutTradeNo(), DepositResponse.class);
        if (cached != null) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
        }

        // 2. 账户定位 + 验密 + 状态检查
        Account account = accountMapper.selectByCardNo(req.getCardNo());
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (account.getStatus() == AccountEnums.Status.FROZEN.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        if (account.getStatus() == AccountEnums.Status.CLOSED.getCode()) {
            throw new BusinessException(ResultCode.ACCOUNT_CLOSED);
        }
        if (!PasswordUtil.matches(req.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        BigDecimal interestAmount = calInterest(account.getAccountId(), LocalDate.now());

        BusinessTransaction trans = new BusinessTransaction();
        {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String timePart = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String nanoPart = String.format("%04d", System.nanoTime() % 10000);
            String seq = String.format("%02d", ThreadLocalRandom.current().nextInt(100));

            trans.setTransNo(account.getBranchCode() + datePart + TransType.DEPOSIT.getCode() + timePart + nanoPart + seq);
        }
        trans.setAccountId(account.getAccountId());
        trans.setDcFlag(TransactionEnums.DcFlag.CREDIT.getCode());
        trans.setTransType(TransType.INTEREST.getCode());
        trans.setCurrency("CNY");
        trans.setTransAmount(interestAmount);
        trans.setBalanceAfter(account.getBalance().add(interestAmount));
        trans.setFrozenAmountAfter(BigDecimal.ZERO);
        trans.setChannel("COUNTER");
        trans.setOperatorId("TELLER001437");
        trans.setTransTime(req.getInterestDate().atStartOfDay());
        trans.setStatus(TransactionEnums.Status.SUCCESS.getCode());
        businessTransactionMapper.insert(trans);

        accountingService.generateEntries(trans);

        BigDecimal newBalance = account.getBalance().add(interestAmount);
        int affected = accountMapper.updateBalanceWithVersion(account.getAccountId(), newBalance, account.getVersion());
        if (affected <= 0) {
            throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
        }

        DepositResponse resp = new DepositResponse(trans.getTransNo(), newBalance, trans.getStatus());
        idempotencyService.save(req.getOutTradeNo(), resp);


        return null;
    }

}
