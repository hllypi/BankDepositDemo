<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
        </div>
        <h3>账户信息查询</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="银行卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入账户密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24" class="btn-row">
            <el-button type="primary" @click="onQuery" :loading="loading">查询</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="card result-card" v-if="accountInfo">
      <div class="card-hd">
        <div class="card-hd-icon green">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M16 8l-5.33 5.33L8 10.67"/></svg>
        </div>
        <h3>账户详情</h3>
      </div>
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">银行卡号</span>
          <span class="info-value">{{ accountInfo.cardNo }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">核心内部账号</span>
          <span class="info-value">{{ accountInfo.accountNo }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">客户姓名</span>
          <span class="info-value">{{ accountInfo.customerName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">账户类型</span>
          <span class="info-value">{{ accountInfo.accountType === 'C01' ? '活期存款' : accountInfo.accountType }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">账户等级</span>
          <span class="info-value">{{ levelText(accountInfo.accountLevel) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">币种</span>
          <span class="info-value">{{ accountInfo.currency }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">开户行代码</span>
          <span class="info-value">{{ accountInfo.branchCode }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">开户日期</span>
          <span class="info-value">{{ accountInfo.openDate }}</span>
        </div>
        <div class="info-item highlight">
          <span class="info-label">当前余额</span>
          <span class="info-value balance">{{ formatMoney(accountInfo.balance) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">冻结金额</span>
          <span class="info-value">{{ formatMoney(accountInfo.frozenAmount) }}</span>
        </div>
        <div class="info-item highlight-green">
          <span class="info-label">可用余额</span>
          <span class="info-value balance">{{ formatMoney(accountInfo.availableBalance) }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">账户状态</span>
          <span class="info-value" :class="{ 'status-closed': accountInfo.status === 2, 'status-frozen': accountInfo.status === 1 }">
            {{ statusText(accountInfo.status) }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { queryAccount } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)
const accountInfo = ref(null)

const form = reactive({
  cardNo: '',
  password: ''
})

const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function levelText(level) {
  const map = { 1: 'Ⅰ类户', 2: 'Ⅱ类户', 3: 'Ⅲ类户' }
  return map[level] || level
}

function statusText(status) {
  const map = { 0: '正常', 1: '冻结', 2: '销户' }
  return map[status] || status
}

function formatMoney(val) {
  if (val == null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function onQuery() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  accountInfo.value = null
  try {
    const res = await queryAccount(form)
    accountInfo.value = res.data
  } catch (e) {}
  finally { loading.value = false }
}
</script>

<style scoped>
.page { width: 100%; max-width: 1100px; }
.card { background: rgba(255,255,255,0.015); border: 1px solid rgba(255,255,255,0.04); border-radius: 18px; padding: 32px 36px; margin-bottom: 20px; }
.card-hd { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
.card-hd-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.card-hd-icon.blue { background: rgba(99,102,241,0.1); color: #818cf8; }
.card-hd-icon.green { background: rgba(34,197,94,0.1); color: #4ade80; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }

.result-card { border-color: rgba(34,197,94,0.15) !important; }
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.info-item {
  background: rgba(255,255,255,0.02);
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid rgba(255,255,255,0.03);
}
.info-item.highlight {
  background: linear-gradient(135deg, rgba(255,255,255,0.03) 0%, rgba(99,102,241,0.06) 100%);
}
.info-item.highlight-green {
  background: linear-gradient(135deg, rgba(255,255,255,0.03) 0%, rgba(34,197,94,0.06) 100%);
}
.info-item.highlight .info-value,
.info-item.highlight-green .info-value {
  color: #a5b4fc;
  font-weight: 700;
  font-size: 16px;
}
.info-label { font-size: 12px; color: rgba(255,255,255,0.25); display: block; }
.info-value { font-size: 14px; color: #e2e8f0; display: block; margin-top: 4px; font-weight: 500; }
.info-value.balance { font-size: 18px; }
.status-closed { color: #f87171; }
.status-frozen { color: #fb923c; }
</style>
