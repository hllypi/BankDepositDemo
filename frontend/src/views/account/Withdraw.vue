<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon orange">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg>
        </div>
        <h3>取款交易</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="银行卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="账户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入账户密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="取款金额" prop="transAmount">
              <el-input-number v-model="form.transAmount" :min="0.01" :precision="2" :step="100" style="width:100%" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="交易渠道" prop="channel">
              <el-select v-model="form.channel" style="width:100%">
                <el-option label="APP" value="APP" />
                <el-option label="柜面" value="COUNTER" />
                <el-option label="ATM" value="ATM" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="6">
            <el-form-item label="经办人">
              <el-input v-model="form.operatorId" placeholder="经办人或系统标识" />
            </el-form-item>
          </el-col>
          <el-col :span="18">
            <el-form-item label="交易摘要">
              <el-input v-model="form.remark" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24" class="btn-row">
            <el-button type="warning" @click="onSubmit" :loading="loading">确认取款</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <Teleport to="body">
      <div class="overlay" v-if="resultVisible" @click.self="resultVisible = false">
        <div class="result-dialog">
          <div class="rd-check">
            <svg viewBox="0 0 52 52" width="64" height="64"><circle cx="26" cy="26" r="25" fill="#22c55e"/><path d="M14 27l7 7 17-17" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <h2>取款成功</h2>
          <div class="rd-amount debit">{{ result.balanceAfter }}</div>
          <p class="rd-meta">交易流水号 {{ result.transNo }}</p>
          <button class="rd-btn" @click="resultVisible = false">确 定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { withdraw } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)
const resultVisible = ref(false)
const result = reactive({ transNo: '', balanceAfter: '' })

const form = reactive({
  cardNo: '', password: '', transAmount: 0,
  channel: 'APP', operatorId: '', remark: '', outTradeNo: ''
})

const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  transAmount: [{ required: true, message: '请输入取款金额', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择交易渠道' }]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const payload = { ...form, outTradeNo: 'WTH_' + Date.now() }
    const res = await withdraw(payload)
    result.transNo = res.data.transNo
    result.balanceAfter = res.data.balanceAfter
    resultVisible.value = true
  } catch (e) {}
  finally { loading.value = false }
}
</script>

<style scoped>
.page { width: 100%; max-width: 1100px; }
.card { background: rgba(255,255,255,0.015); border: 1px solid rgba(255,255,255,0.04); border-radius: 18px; padding: 32px 36px; }
.card-hd { display: flex; align-items: center; gap: 14px; margin-bottom: 28px; }
.card-hd-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.card-hd-icon.orange { background: rgba(251,146,60,0.1); color: #fb923c; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 24px; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }
.submit-btn.warn { background: #f97316; border-color: #f97316; }
.submit-btn.warn:hover { background: #ea580c; }

.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.result-dialog {
  background: #11152a; border-radius: 20px; padding: 40px 36px 32px;
  text-align: center; width: 400px; box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.rd-check { margin-bottom: 12px; }
.result-dialog h2 { font-size: 20px; color: #e2e8f0; margin-bottom: 16px; }
.rd-amount { font-size: 36px; font-weight: 700; margin-bottom: 6px; color: #f97316; }
.rd-meta { font-size: 13px; color: #94a3b8; margin-bottom: 20px; }
.rd-btn {
  width: 100%; height: 42px; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none;
  border-radius: 10px; font-size: 15px; cursor: pointer; font-weight: 600;
}
.rd-btn:hover { background: linear-gradient(135deg, #7775f6, #9b6ff7); }
</style>
