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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
     * !! 需要确保单笔交易时间跨度不超过90分钟!
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void execute() {
        dailyBalanceSchedule(LocalDate.now());
    }

    /**
     * 每日批处理任务, 根据前天的余额记录与昨天的流水记录推导出昨天的余额
     * 记录余额
     * @param excDate 执行批处理的时间, 会插入昨天的余额记录
     * @return 成功执行, 事务提交返回true
     */
    @Transactional(rollbackFor = Exception.class)
    public Object dailyBalanceSchedule(LocalDate excDate) {
        LocalDate targetDate = excDate.plusDays( -1 );

        // 获取所有正常且未处理过的账户
        List<Long> allAcc = dailyBalanceMapper.selectAllNormalAccount(targetDate);

        // 处理这些账户
        for(Long acc : allAcc) {
            // 获取前天的金额
            BigDecimal curBalance = dailyBalanceMapper.selectLastDayBalance(
                    acc,
                    targetDate
            );
            // 如果前天就没有余额, 选择处理策略
            if(curBalance == null) {
                // 昨天开户, 前天必不可能有余额记录, 默认为0
                if(accountMapper.selectById(acc).getOpenDate().equals(targetDate)) {
                    curBalance = BigDecimal.valueOf(0L);
                }
                else {
                    // 一般直接忽略, 为了测试使用, 在此会跑断档日期以来的所有余额记录
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

        return Boolean.TRUE;
    }

    /**
     * 余额记录追溯补全
     * @param accountId 追溯的账号
     * @param targetDate 要追溯到哪一天(包含)
     */
    @Transactional(rollbackFor = Exception.class)
    public void runDailyBalanceSchedule(Long accountId, LocalDate targetDate) {
        // 找到断档日期, 默认使用上次的余额记录日期
        DailyBalance lastEntry = dailyBalanceMapper.selectLastEntry(accountId);
        if(lastEntry == null) { // 如果一条都没有, 就使用开户日期
            Account account = accountMapper.selectById(accountId);
            if(account == null) {
                throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
            }
            lastEntry = new DailyBalance();
            lastEntry.setBalanceDate(account.getOpenDate().plusDays(-1));
            lastEntry.setEndBalance(BigDecimal.valueOf(0L));
        }

        BigDecimal curBalance = lastEntry.getEndBalance();
        // 根据查询到的余额与日期开始补全余额记录
        for(
                LocalDate curDate = lastEntry.getBalanceDate().plusDays(1);
                !curDate.isAfter(targetDate);
                curDate = curDate.plusDays(1)
        ) {
            // 查询每日流水表, 获取以日为单位的流水和
            BigDecimal transSum = businessTransactionMapper.selectSumByAccAndTime(
                    accountId,
                    curDate
            );
            // 当日没有流水记0
            if(transSum == null) transSum = BigDecimal.valueOf(0L);

            // 将其加到"模拟余额"中, 反复迭代
            curBalance = curBalance.add(transSum);

            // 传值与存储
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

    /**
     * 带有主动时间的存款功能, 用于模拟计息区间在余额上的差异
     * @param req 存款同款请求报文, 不过需要填入存储日期字段
     * @return 同存款
     */
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

    /**
     * 计息值核心计算方法, 此方法只会查询, 不涉及任何新增与修改
     * @param accountId 需要结息的账户
     * @param curDate 结息的日期, 会自动去找上次结息日, 算头不算尾
     * @return 这段时间的利息总额
     */
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal calInterest(Long accountId, LocalDate curDate) {
        // 获取利率
        InterestRateConfig interestRateConfig = interestRateConfigMapper.selectByRateId(2L);
        BigDecimal activeRate = interestRateConfig.getRateValue();

        // 获取余额算数积分
        BigDecimal interestBase = dailyBalanceMapper.selectBalanceSum(accountId, curDate);
        if(activeRate == null || interestBase == null) {
            throw new BusinessException(ResultCode.RATE_NOT_FOUND);
        }

        // 积分计算利息
        BigDecimal dayRate =
                BigDecimal.valueOf(
                        activeRate.doubleValue() / 360
                ).setScale(2, RoundingMode.HALF_UP);
        return interestBase.multiply(dayRate);
    }

    /**
     * 结息方法
     * @param req 结息请求报文
     * @return 成功返回 true
     */
    @Transactional(rollbackFor = Exception.class)
    public Object interest(InterestRequest req) {
        // 传入参数校验
        if(
            req.getOutTradeNo() == null ||
            req.getCardNo() == null ||
            req.getPassword() == null ||
            req.getInterestDate() == null
        ) {
            throw new BusinessException(ResultCode.PARAM_MISSING);
        }
        // 幂等校验
        Object cached = idempotencyService.check(req.getOutTradeNo(), DepositResponse.class);
        if (cached != null) {
            throw new BusinessException(ResultCode.DUPLICATE_REQUEST);
        }

        // 账户定位 + 验密 + 状态检查
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

        // 获取时段利息
        BigDecimal interestAmount = calInterest(account.getAccountId(), LocalDate.now());

        // 新增流水
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
        trans.setRemark(req.getRemark());
        businessTransactionMapper.insert(trans);

        // 复式记账：借贷平衡，生成一对会计分录
        accountingService.generateEntries(trans);

        // 乐观锁更新账户余额
        BigDecimal newBalance = account.getBalance().add(interestAmount);
        int affected = accountMapper.updateBalanceWithVersion(account.getAccountId(), newBalance, account.getVersion());
        if (affected <= 0) {
            throw new BusinessException(ResultCode.CONCURRENT_CONFLICT);
        }

        // 组装响应并缓存幂等结果
        DepositResponse resp = new DepositResponse(trans.getTransNo(), newBalance, trans.getStatus());
        idempotencyService.save(req.getOutTradeNo(), resp);

        // 更新上次计息时间
        account = accountMapper.selectById(account.getAccountId());
        account.setLastSettlementDate(req.getInterestDate());
        accountMapper.updateLastSettlementDate(account);


        return Boolean.TRUE;
    }

}
