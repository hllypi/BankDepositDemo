package com.dcits.bank.demo.backend.service;

import com.dcits.bank.demo.backend.entity.AccountingEntry;
import com.dcits.bank.demo.backend.entity.BusinessTransaction;
import com.dcits.bank.demo.backend.enums.*;
import com.dcits.bank.demo.backend.mapper.AccountingEntryMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 会计分录服务，根据交易流水 + 交易类型枚举中的分录模板，
 * 统一生成一借一贷两条 accounting_entry，确保借贷平衡。
 */
@Service
public class AccountingService {

    private final AccountingEntryMapper entryMapper;

    public AccountingService(AccountingEntryMapper entryMapper) {
        this.entryMapper = entryMapper;
    }

    /**
     * 根据一笔交易流水生成两条会计分录（1借 + 1贷）。
     * 凭证号直接使用交易流水号 transNo，一张凭证对应一笔交易。
     */
    public void generateEntries(BusinessTransaction trans) {
        // 1. 从枚举模板取借贷科目及摘要
        TransType tt = TransType.fromCode(trans.getTransType());

        // 2. 凭证号 = 交易流水号，两号合一
        String voucherId = trans.getTransNo();

        AccountingEnums.AccountCode debitCode;
        AccountingEnums.AccountCode creditCode;
        String debitSummary;
        String creditSummary;
        BigDecimal amount = trans.getTransAmount();

        // 3. 转账取转入/转出方向：转出(D)用模板正向，转入(C)交换借贷科目
        if (tt == TransType.TRANSFER && TransactionEnums.DcFlag.CREDIT.getCode().equals(trans.getDcFlag())) {
            // 转入方：借1004行内清算 / 贷1001活期存款
            debitCode = tt.getCreditCode();
            creditCode = tt.getDebitCode();
            debitSummary = "行内清算";
            creditSummary = "活期存款入账";
        } else {
            // 通用：直接取模板配置
            debitCode = tt.getDebitCode();
            creditCode = tt.getCreditCode();
            debitSummary = tt.getDebitSummary();
            creditSummary = tt.getCreditSummary();
        }

        // 4. 写入借方分录
        entryMapper.insert(buildEntry(voucherId, trans.getTransId(),
                debitCode.getCode(), AccountingEnums.Action.DEBIT.getCode(), amount, debitSummary));

        // 5. 写入贷方分录
        entryMapper.insert(buildEntry(voucherId, trans.getTransId(),
                creditCode.getCode(), AccountingEnums.Action.CREDIT.getCode(), amount, creditSummary));
    }

    /** 组装一条会计分录实体 */
    private AccountingEntry buildEntry(String voucherId, Long transId,
                                       String accountCode, int action,
                                       BigDecimal amount, String summary) {
        AccountingEntry entry = new AccountingEntry();
        entry.setVoucherId(voucherId);
        entry.setTransId(transId);
        entry.setAccountCode(accountCode);
        entry.setAction(action);      // 1-借，2-贷
        entry.setAmount(amount);
        entry.setSummary(summary);
        return entry;
    }
}
