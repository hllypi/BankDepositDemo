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
    @Insert("INSERT INTO daily_balance (account_id, balance_date, end_balance) VALUES (#{accountId}, #{balanceDate}, #{endBalance})")
    @Options(useGeneratedKeys = true, keyProperty = "dailyBalanceId")
    int insert(DailyBalance dailyBalance);

    /** 汇总结息周期内每日余额之和（日积数），作为利息计算基数 */
    @Select("SELECT IFNULL(SUM(end_balance), 0) FROM daily_balance WHERE account_id = #{accountId} " +
            "AND balance_date >= #{startDate} AND balance_date <= #{endDate}")
    BigDecimal sumBalanceByAccountAndDateRange(@Param("accountId") Long accountId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /** 查询指定账户指定日期的日积数记录（幂等校验用） */
    @Select("SELECT * FROM daily_balance WHERE account_id = #{accountId} AND balance_date = #{balanceDate}")
    DailyBalance selectByAccountAndDate(@Param("accountId") Long accountId,
                                         @Param("balanceDate") LocalDate balanceDate);
}
