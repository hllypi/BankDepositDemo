/*
 Navicat Premium Dump SQL

 Source Server         : 实验室数据库
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46-0ubuntu0.24.04.2)
 Source Host           : 8.138.144.238:3306
 Source Schema         : BankDepositDemo

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46-0ubuntu0.24.04.2)
 File Encoding         : 65001

 Date: 16/06/2026 10:09:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for account
-- ----------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account`  (
  `account_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内部账户ID(主键)',
  `account_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '核心内部账号',
  `card_no` varchar(19) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '银行卡号(对外，Luhn校验位)',
  `password_hash` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码哈希值（SM3 / Bcrypt + 随机Salt，不可逆）',
  `customer_id` bigint NOT NULL COMMENT '所属客户ID',
  `account_type` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'C01' COMMENT '账户类型（C01-活期存款）',
  `account_level` tinyint NOT NULL DEFAULT 1 COMMENT '账户等级(1-Ⅰ类, 2-Ⅱ类, 3-Ⅲ类)',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `branch_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '开户行代码',
  `balance` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '当前总余额',
  `frozen_amount` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额(可用余额=balance-frozen_amount)',
  `rate_id` bigint NULL DEFAULT NULL COMMENT '挂载的特定利率ID(关联利率配置表)',
  `last_settlement_date` date NULL DEFAULT NULL COMMENT '上一次结息日期(跑批起点防重复)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '账户状态（0-正常，1-冻结，2-销户）',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号(防并发核心)',
  `open_date` date NOT NULL COMMENT '开户日期',
  `close_date` date NULL DEFAULT NULL COMMENT '销户日期',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`account_id`) USING BTREE,
  UNIQUE INDEX `uk_account_no`(`account_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_card_no`(`card_no` ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_branch`(`branch_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for accounting_entry
-- ----------------------------
DROP TABLE IF EXISTS `accounting_entry`;
CREATE TABLE `accounting_entry`  (
  `entry_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分录流水ID',
  `voucher_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属记账凭证号',
  `trans_id` bigint NOT NULL COMMENT '所属业务流水ID',
  `account_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会计科目代码(由应用层Enum统一定义)',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '记账币种',
  `action` tinyint NOT NULL COMMENT '借贷方向（1-借，2-贷）',
  `amount` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '发生金额绝对值',
  `summary` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分录摘要',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记账时间',
  PRIMARY KEY (`entry_id`) USING BTREE,
  INDEX `idx_trans_id`(`trans_id` ASC) USING BTREE,
  INDEX `idx_account_code_date`(`account_code` ASC, `voucher_id` ASC) USING BTREE,
  CONSTRAINT `chk_entry_amount_positive` CHECK (`amount` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 103 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会计分录明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for business_transaction
-- ----------------------------
DROP TABLE IF EXISTS `business_transaction`;
CREATE TABLE `business_transaction`  (
  `trans_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内部交易流水号',
  `trans_no` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易流水号(业务编号)',
  `related_trans_id` bigint NULL DEFAULT NULL COMMENT '关联流水号(转账双边账的纽带)',
  `account_id` bigint NOT NULL COMMENT '发生账户ID',
  `counter_party_account` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '对方对外账号',
  `dc_flag` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '资金方向标识(D-借方/扣减，C-贷方/增加)',
  `trans_type` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易类型（00-开户，01-存款，02-取款，03-转账，04-结息，05-销户）',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '交易币种',
  `trans_amount` decimal(18, 2) NOT NULL COMMENT '交易金额绝对值',
  `balance_after` decimal(18, 2) NOT NULL COMMENT '交易后总余额快照',
  `frozen_amount_after` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '交易后冻结金额快照(结合balance_after计算瞬时可用余额)',
  `channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '交易渠道(APP, COUNTER, ATM, SYSTEM)',
  `operator_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '经办人/系统标识',
  `trans_time` datetime NOT NULL COMMENT '发生时间',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态机（0-处理中, 1-成功, 2-失败, 3-冲正）',
  `remark` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '摘要',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '落库时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',
  PRIMARY KEY (`trans_id`) USING BTREE,
  INDEX `idx_acc_time`(`account_id` ASC, `trans_time` ASC) USING BTREE,
  INDEX `idx_related_trans`(`related_trans_id` ASC) USING BTREE,
  CONSTRAINT `chk_trans_amount_positive` CHECK (`trans_amount` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 53 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '业务交易流水表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cash_transaction
-- ----------------------------
DROP TABLE IF EXISTS `cash_transaction`;
CREATE TABLE `cash_transaction`  (
  `cash_id` bigint NOT NULL AUTO_INCREMENT,
  `trans_id` bigint NOT NULL COMMENT '关联交易流水ID',
  `teller_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '柜员编号',
  `branch_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属机构代码',
  `box_id` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '物理尾箱编号',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '交易币种',
  `cash_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '现金类型（I-入库，O-出库）',
  `amount` decimal(18, 2) NOT NULL COMMENT '发生金额绝对值',
  `box_balance_after` decimal(18, 2) NOT NULL COMMENT '交易后该尾箱该币种的总余额(存量快照)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-正常，2-冲正）',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`cash_id`) USING BTREE,
  INDEX `idx_trans_id`(`trans_id` ASC) USING BTREE,
  INDEX `idx_teller_box`(`teller_id` ASC, `box_id` ASC) USING BTREE,
  INDEX `idx_branch_currency`(`branch_code` ASC, `currency` ASC) USING BTREE,
  CONSTRAINT `chk_box_balance_positive` CHECK (`box_balance_after` >= 0),
  CONSTRAINT `chk_cash_amount_positive` CHECK (`amount` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '柜员现金尾箱交易明细表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for customer
-- ----------------------------
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer`  (
  `customer_id` bigint NOT NULL AUTO_INCREMENT COMMENT '内部客户ID',
  `customer_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户姓名',
  `type` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户类型',
  `id_type` char(2) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件类型（01-身份证，02-护照等）',
  `id_number` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通讯地址',
  `date_of_birth` date NULL DEFAULT NULL COMMENT '出生日期',
  `gender` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  `branch` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '开户机构',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0-正常，1-注销，2-冻结）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`customer_id`) USING BTREE,
  UNIQUE INDEX `uk_id_type_number`(`id_type` ASC, `id_number` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for daily_balance
-- ----------------------------
DROP TABLE IF EXISTS `daily_balance`;
CREATE TABLE `daily_balance`  (
  `daily_balance_id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '内部账户ID',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `balance_date` date NOT NULL COMMENT '余额所属日期',
  `end_balance` decimal(18, 2) NOT NULL COMMENT '当日最终余额(日积数)',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`daily_balance_id`) USING BTREE,
  UNIQUE INDEX `uk_acc_curr_date`(`account_id` ASC, `currency` ASC, `balance_date` ASC) USING BTREE,
  INDEX `idx_date`(`balance_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 103 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '账户日积数底表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for interest_rate_config
-- ----------------------------
DROP TABLE IF EXISTS `interest_rate_config`;
CREATE TABLE `interest_rate_config`  (
  `rate_id` bigint NOT NULL AUTO_INCREMENT,
  `rate_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '利率名称',
  `account_type` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'C01' COMMENT '适用账户类型',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '适用币种',
  `rate_value` decimal(12, 8) NOT NULL COMMENT '利率值（高精度日利率）',
  `effective_date` date NOT NULL COMMENT '生效日期',
  `expiry_date` date NULL DEFAULT NULL COMMENT '失效日期',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（1-有效，0-失效）',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`rate_id`) USING BTREE,
  INDEX `idx_effective`(`effective_date` ASC) USING BTREE,
  INDEX `idx_dimension`(`account_type` ASC, `currency` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '全局/差异化利率配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for interest_settlement
-- ----------------------------
DROP TABLE IF EXISTS `interest_settlement`;
CREATE TABLE `interest_settlement`  (
  `settlement_id` bigint NOT NULL AUTO_INCREMENT,
  `account_id` bigint NOT NULL COMMENT '内部账户ID',
  `currency` char(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CNY' COMMENT '结息币种',
  `settlement_date` date NOT NULL COMMENT '结息执行日期',
  `accumulated_amount` decimal(18, 2) NOT NULL COMMENT '该周期内总积数',
  `applied_rate` decimal(12, 8) NOT NULL COMMENT '执行时的日利率',
  `interest_days` int NOT NULL COMMENT '计息总天数',
  `interest_amount` decimal(18, 2) NOT NULL COMMENT '最终派发利息金额',
  `trans_id` bigint NULL DEFAULT NULL COMMENT '关联生成的利息派发流水ID',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  PRIMARY KEY (`settlement_id`) USING BTREE,
  INDEX `idx_account_id`(`account_id` ASC) USING BTREE,
  INDEX `idx_settlement_date`(`settlement_date` ASC) USING BTREE,
  INDEX `idx_trans_id`(`trans_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '结息审计记录明细表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
