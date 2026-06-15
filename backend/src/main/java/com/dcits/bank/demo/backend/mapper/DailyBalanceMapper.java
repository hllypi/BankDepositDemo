package com.dcits.bank.demo.backend.mapper;

import com.dcits.bank.demo.backend.entity.DailyBalance;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账户日积数底表 Mapper
 */
@Mapper
public interface DailyBalanceMapper {

    /** 新增每日余额快照（唯一约束 uk_account_date 保证每日每账户一条） */
    @Insert("INSERT INTO daily_balance (account_id, currency, balance_date, end_balance) VALUES (#{accountId}, #{currency}, #{balanceDate}, #{endBalance})")
    @Options(useGeneratedKeys = true, keyProperty = "dailyBalanceId")
    int insert(DailyBalance dailyBalance);

    /** 汇总结息周期内每日余额之和（日积数），作为利息计算基数 */
    @Select("SELECT IFNULL(SUM(end_balance), 0) FROM daily_balance WHERE account_id = #{accountId} " +
            "AND balance_date >= #{startDate} AND balance_date <= #{endDate}")
    BigDecimal sumBalanceByAccountAndDateRange(@Param("accountId") Long accountId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /** 查询指定账户指定日期的日积数记录 */
    @Select("SELECT * FROM daily_balance WHERE account_id = #{accountId} AND balance_date = #{balanceDate}")
    DailyBalance selectByAccountAndDate(@Param("accountId") Long accountId,
                                         @Param("balanceDate") LocalDate balanceDate);

    /** 查询指定账户在指定日期之前最近一条日积数记录（无当日交易时回溯） */
    @Select("SELECT * FROM daily_balance WHERE account_id = #{accountId} AND balance_date < #{before} " +
            "ORDER BY balance_date DESC LIMIT 1")
    DailyBalance selectLatestBefore(@Param("accountId") Long accountId,
                                     @Param("before") LocalDate before);
}
