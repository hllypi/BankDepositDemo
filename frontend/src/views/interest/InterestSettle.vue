<template>
  <div class="tx-page">
    <!-- 单账户结息 -->
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon indigo">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
        </div>
        <h3>单账户结息</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="银行卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="账户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="账户密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12" class="btn-col">
            <el-button type="primary" @click="onSettleSingle" :loading="singleLoading">执行结息</el-button>
          </el-col>
        </el-row>
      </el-form>

      <template v-if="showResult">
        <div class="result-section">
          <template v-if="singleResult">
            <div class="result-header">
              <span class="result-title">结息成功</span>
              <span class="result-date">{{ singleResult.settlementDate }}</span>
            </div>
            <div class="result-grid">
              <div class="result-item">
                <span class="ri-label">计息区间</span>
                <span class="ri-value">{{ singleResult.startDate }} — {{ singleResult.settlementDate }}</span>
              </div>
              <div class="result-item">
                <span class="ri-label">计息天数</span>
                <span class="ri-value">{{ singleResult.interestDays }} 天</span>
              </div>
              <div class="result-item">
                <span class="ri-label">积数总和</span>
                <span class="ri-value">{{ singleResult.accumulatedAmount }}</span>
              </div>
              <div class="result-item">
                <span class="ri-label">日利率</span>
                <span class="ri-value">{{ singleResult.appliedRate }}</span>
              </div>
              <div class="result-item highlight">
                <span class="ri-label">利息金额</span>
                <span class="ri-value">¥ {{ singleResult.interestAmount }}</span>
              </div>
            </div>
          </template>
          <div v-else class="empty-info">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#94a3b8" stroke-width="1.5">
              <circle cx="12" cy="12" r="10"/><line x1="8" y1="15" x2="16" y2="15"/>
            </svg>
            <p>该账户无需结息</p>
            <p class="hint">计息区间为空或利息为0</p>
          </div>
        </div>
      </template>
    </div>

    <!-- 批量结息管理 -->
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon orange">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
        </div>
        <h3>批量结息管理</h3>
      </div>
      <el-row :gutter="16">
        <el-col :span="24">
          <el-form size="default">
            <el-row :gutter="16" style="margin-top: 20px;">
              <el-col :span="24" class="btn-row">
                <el-button type="warning" @click="onSettleAll" :loading="allLoading">执行全部结息</el-button>
                <el-button type="info" @click="onGenerateDaily" :loading="dailyLoading">日终积数生成</el-button>
              </el-col>
            </el-row>
          </el-form>
        </el-col>
      </el-row>

      <template v-if="allResults">
        <div class="result-section">
          <div class="result-header">
            <span class="result-title">执行结果</span>
          </div>
          <div class="results-container">
            <div
              v-for="(msg, id) in allResults" :key="id"
              class="result-line"
              :class="{ success: msg.startsWith('SUCCESS'), failed: msg.startsWith('FAILED'), info: !msg.startsWith('SUCCESS') && !msg.startsWith('FAILED') }"
            >
              <span class="result-id">[账户 {{ id }}]</span>
              <span class="result-msg">{{ msg }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { settleInterest, settleInterestAll, generateDailyBalances } from '../../api/index.js'

const formRef = ref()
const singleLoading = ref(false)
const allLoading = ref(false)
const dailyLoading = ref(false)
const singleResult = ref(null)
const allResults = ref(null)
const showResult = ref(false)

const form = reactive({ cardNo: '', password: '' })
const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入账户密码', trigger: 'blur' }]
}

async function onSettleSingle() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  singleLoading.value = true
  singleResult.value = null
  showResult.value = false
  try {
    const res = await settleInterest(form)
    singleResult.value = res.data
    showResult.value = true
  } catch (e) { /* 拦截器已弹窗 */ }
  finally { singleLoading.value = false }
}

async function onSettleAll() {
  allLoading.value = true
  allResults.value = null
  try {
    const res = await settleInterestAll()
    allResults.value = res.data
  } catch (e) { /* 拦截器已弹窗 */ }
  finally { allLoading.value = false }
}

async function onGenerateDaily() {
  dailyLoading.value = true
  try {
    const res = await generateDailyBalances()
    const data = res.data
    allResults.value = {
      '日终积数': `日终积数已生成，处理 ${data.totalAccounts} 个账户（成功：${data.successCount}，跳过：${data.skipCount}）`
    }
  } catch (e) { /* 拦截器已弹窗 */ }
  finally { dailyLoading.value = false }
}
</script>

<style scoped>
.tx-page { 
  width: 100%; 
  max-width: 1280px; 
  margin: 0 auto;
  padding: 0 20px;
}

/* 卡片样式 */
.card { 
  background: rgba(255,255,255,0.015); 
  border: 1px solid rgba(255,255,255,0.04); 
  border-radius: 18px; 
  padding: 32px; 
  margin-bottom: 20px; 
}
.card-hd { 
  display: flex; 
  align-items: center; 
  gap: 14px; 
  margin-bottom: 24px; 
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255,255,255,0.04);
}
.card-hd-icon {
  width: 44px; 
  height: 44px; 
  border-radius: 12px;
  display: flex; 
  align-items: center; 
  justify-content: center;
}
.card-hd-icon.indigo { 
  background: rgba(99,102,241,0.1); 
  color: #818cf8; 
}
.card-hd-icon.orange { 
  background: rgba(251,146,60,0.1); 
  color: #fb923c; 
}
.card-hd h3 { 
  font-size: 18px; 
  font-weight: 700; 
  color: #e2e8f0; 
  margin: 0;
}
.btn-row { 
  display: flex; 
  align-items: center; 
  gap: 12px; 
  justify-content: flex-start;
  padding-top: 8px;
}

/* 表单样式 */
:deep(.el-form-item) {
  margin-bottom: 0;
}
:deep(.el-form-item__label) {
  font-size: 14px;
  color: #94a3b8;
  font-weight: 500;
}
:deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.03);
}

/* 结果区域 */
.result-section { 
  margin-top: 24px; 
  padding-top: 24px; 
  border-top: 1px solid rgba(255,255,255,0.04); 
}
.result-header { 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  margin-bottom: 16px; 
}
.result-title { 
  font-size: 16px; 
  font-weight: 600; 
  color: #e2e8f0; 
}
.result-date { 
  font-size: 13px; 
  color: rgba(255,255,255,0.5); 
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.result-item {
  background: rgba(255,255,255,0.02);
  border-radius: 10px;
  padding: 14px 16px;
}
.result-item.highlight {
  grid-column: span 2;
  background: linear-gradient(135deg, rgba(34,197,94,0.08) 0%, rgba(34,197,94,0.04) 100%);
  border: 1px solid rgba(34,197,94,0.15);
}
.result-item.highlight .ri-label { color: rgba(34,197,94,0.6); }
.result-item.highlight .ri-value { color: #4ade80; font-weight: 700; font-size: 18px; }
.ri-label { font-size: 12px; color: rgba(255,255,255,0.4); }
.ri-value { display: block; font-size: 15px; color: #f1f5f9; margin-top: 6px; font-weight: 500; }

.empty-info {
  text-align: center;
  padding: 40px;
  color: #94a3b8;
}
.empty-info p { margin: 12px 0 0; font-size: 14px; }
.empty-info .hint { font-size: 13px; color: #64748b; margin-top: 6px; }

.results-container {
  max-height: 350px;
  overflow-y: auto;
  background: rgba(255,255,255,0.02);
  border-radius: 10px;
  padding: 8px;
}
.result-line {
  display: flex;
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 4px;
}
.result-line.success { background: rgba(34,197,94,0.08); }
.result-line.failed { background: rgba(239,68,68,0.08); }
.result-line.info { background: rgba(59,130,246,0.08); }
.result-id { color: rgba(255,255,255,0.5); font-size: 13px; }
.result-msg { color: #e2e8f0; font-size: 14px; margin-left: 8px; }
</style>