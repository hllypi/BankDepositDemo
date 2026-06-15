package com.dcits.bank.demo.backend.mapper;

import com.dcits.bank.demo.backend.entity.Account;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * 账户信息表 Mapper
 */
@Mapper
public interface AccountMapper {

    /** 根据银行卡号查询账户（唯一索引 uk_card_no） */
    @Select("SELECT * FROM account WHERE card_no = #{cardNo}")
    Account selectByCardNo(@Param("cardNo") String cardNo);

    /** 根据核心内部账号查询账户（唯一索引 uk_account_no） */
    @Select("SELECT * FROM account WHERE account_no = #{accountNo}")
    Account selectByAccountNo(@Param("accountNo") String accountNo);

    /** 根据主键查询账户 */
    @Select("SELECT * FROM account WHERE account_id = #{accountId}")
    Account selectById(@Param("accountId") Long accountId);

    /** 新增账户，返回自增主键 accountId */
    @Insert("INSERT INTO account (account_no, card_no, password_hash, customer_id, account_type, account_level, " +
            "currency, branch_code, balance, frozen_amount, rate_id, last_settlement_date, status, version, " +
            "open_date, remark) VALUES (#{accountNo}, #{cardNo}, #{passwordHash}, #{customerId}, #{accountType}, " +
            "#{accountLevel}, #{currency}, #{branchCode}, #{balance}, #{frozenAmount}, #{rateId}, " +
            "#{lastSettlementDate}, #{status}, #{version}, #{openDate}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "accountId")
    int insert(Account account);

    /** 乐观锁更新余额：WHERE version 校验，version + 1。返回受影响行数，0 表示并发冲突需重试 */
    @Update("UPDATE account SET balance = #{balance}, version = version + 1 " +
            "WHERE account_id = #{accountId} AND version = #{version}")
    int updateBalanceWithVersion(@Param("accountId") Long accountId,
                                  @Param("balance") BigDecimal balance,
                                  @Param("version") int version);

    int debitAvailableBalanceForTransfer(@Param("accountId") Long accountId,
                                          @Param("amount") BigDecimal amount);

    int creditBalanceForTransfer(@Param("accountId") Long accountId,
                                  @Param("amount") BigDecimal amount);

    /** 乐观锁更新余额 + 结息日期，用于结息派发 */
    @Update("UPDATE account SET balance = #{balance}, last_settlement_date = #{lastSettlementDate}, " +
            "version = version + 1 WHERE account_id = #{accountId} AND version = #{version}")
    int updateBalanceAndSettlementDateWithVersion(Account account);

    /** 乐观锁更新账户状态，用于销户 */
    @Update("UPDATE account SET status = #{status}, close_date = #{closeDate}, version = version + 1 " +
            "WHERE account_id = #{accountId} AND version = #{version}")
    int updateStatusWithVersion(Account account);

    /** 查询所有正常状态账户（status=0） */
    @Select("SELECT * FROM account WHERE status = 0")
    List<Account> selectAllNormal();

    /** 更新账户密码 */
    @Update("UPDATE account SET password_hash = #{passwordHash} WHERE account_id = #{accountId}")
    int updatePassword(@Param("accountId") Long accountId, @Param("passwordHash") String passwordHash);
}
