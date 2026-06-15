import request from './request'

// ---------- 账户相关 ----------

/** 客户开户 */
export function openAccount(data) {
  return request.post('/account/open', data)
}

/** 存款 */
export function deposit(data) {
  return request.post('/account/deposit', data)
}

/** 取款 */
export function withdraw(data) {
  return request.post('/account/withdraw', data)
}

/** 转账 */
export function transfer(data) {
  return request.post('/account/transfer', data)
}

/** 修改客户信息 */
export function updateCustomer(data) {
  return request.put('/account/customer', data)
}

/** 交易流水查询 */
export function queryTransactions(data) {
  return request.post('/account/transactions', data)
}

// ---------- 结息相关 ----------

/** 单账户结息 */
export function settleInterest(data) {
  return request.post('/account/settle', data)
}

/** 全部账户结息 */
export function settleInterestAll() {
  return request.post('/account/settle/all')
}

/** 日终积数生成 */
export function generateDailyBalances() {
  return request.post('/account/daily-balance')
}

// ---------- 销户 ----------

/** 销户 */
export function closeAccount(data) {
  return request.post('/account/close', data)
}

// ---------- 密码管理 ----------

/** 修改密码 */
export function changePassword(data) {
  return request.put('/account/password', data)
}

// ---------- 账户查询 ----------

/** 账户信息查询 */
export function queryAccount(data) {
  return request.post('/account/query', data)
}
