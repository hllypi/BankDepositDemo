package com.dcits.bank.demo.backend.mapper;

import com.dcits.bank.demo.backend.entity.Account;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;

@Mapper
public interface AccountMapper {

    @Select("SELECT * FROM account WHERE card_no = #{cardNo}")
    Account selectByCardNo(@Param("cardNo") String cardNo);

    @Select("SELECT * FROM account WHERE account_no = #{accountNo}")
    Account selectByAccountNo(@Param("accountNo") String accountNo);

    @Select("SELECT * FROM account WHERE account_id = #{accountId}")
    Account selectById(@Param("accountId") Long accountId);

    @Insert("INSERT INTO account (account_no, card_no, password_hash, customer_id, account_type, account_level, " +
            "currency, branch_code, balance, frozen_amount, rate_id, last_settlement_date, status, version, " +
            "open_date, remark) VALUES (#{accountNo}, #{cardNo}, #{passwordHash}, #{customerId}, #{accountType}, " +
            "#{accountLevel}, #{currency}, #{branchCode}, #{balance}, #{frozenAmount}, #{rateId}, " +
            "#{lastSettlementDate}, #{status}, #{version}, #{openDate}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "accountId")
    int insert(Account account);

    @Update("UPDATE account SET balance = #{balance}, version = version + 1 " +
            "WHERE account_id = #{accountId} AND version = #{version}")
    int updateBalanceWithVersion(@Param("accountId") Long accountId,
                                  @Param("balance") BigDecimal balance,
                                  @Param("version") int version);

    @Update("UPDATE account SET balance = #{balance}, last_settlement_date = #{lastSettlementDate}, " +
            "version = version + 1 WHERE account_id = #{accountId} AND version = #{version}")
    int updateBalanceAndSettlementDateWithVersion(Account account);

    @Update("UPDATE account SET status = #{status}, close_date = #{closeDate}, version = version + 1 " +
            "WHERE account_id = #{accountId} AND version = #{version}")
    int updateStatusWithVersion(Account account);
}
