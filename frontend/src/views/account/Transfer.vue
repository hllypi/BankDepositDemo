<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon red">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>
        </div>
        <h3>行内转账</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="转出银行卡号" prop="fromCardNo">
              <el-input v-model="form.fromCardNo" placeholder="请输入转出方银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="转入银行卡号" prop="toCardNo">
              <el-input v-model="form.toCardNo" placeholder="请输入转入方银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="转入客户姓名" prop="toCustomerName">
              <el-input v-model="form.toCustomerName" placeholder="请输入转入方客户姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="账户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="转出方账户密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="6">
            <el-form-item label="转账金额" prop="transAmount">
              <el-input-number v-model="form.transAmount" :min="0.01" :precision="2" :step="100" style="width:100%" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="交易渠道" prop="channel">
              <el-select v-model="form.channel" style="width:100%">
                <el-option label="APP" value="APP" />
                <el-option label="柜面" value="COUNTER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="经办人">
              <el-input v-model="form.operatorId" placeholder="经办人或系统标识" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="转账附言">
              <el-input v-model="form.remark" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24" class="btn-row">
            <el-button type="primary" @click="onSubmit" :loading="loading">确认转账</el-button>
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
          <h2>转账成功</h2>
          <div class="rd-table">
            <div class="rd-row"><span>交易流水号</span><strong>{{ result.transNo }}</strong></div>
            <div class="rd-row"><span>转出后余额</span><strong>{{ result.fromBalanceAfter }}</strong></div>
          </div>
          <button class="rd-btn" @click="resultVisible = false">确 定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { transfer } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)
const resultVisible = ref(false)
const result = reactive({ transNo: '', fromBalanceAfter: '' })

const form = reactive({
  fromCardNo: '',
  toCardNo: '',
  toCustomerName: '',
  password: '',
  transAmount: 0,
  channel: 'APP',
  operatorId: '',
  remark: '',
  outTradeNo: ''
})

const rules = {
  fromCardNo: [{ required: true, message: '请输入转出方银行卡号', trigger: 'blur' }],
  toCardNo: [{ required: true, message: '请输入转入方银行卡号', trigger: 'blur' }],
  toCustomerName: [{ required: true, message: '请输入转入方客户姓名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入账户密码', trigger: 'blur' }],
  transAmount: [{ required: true, message: '请输入转账金额', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择交易渠道', trigger: 'change' }]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const payload = { ...form }
    payload.outTradeNo = 'TRF_' + Date.now()
    const res = await transfer(payload)
    result.transNo = res.data.transNo
    result.fromBalanceAfter = res.data.fromBalanceAfter
    resultVisible.value = true
  } catch (e) { }
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
.card-hd-icon.red { background: rgba(248,113,113,0.1); color: #f87171; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 24px; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }
.submit-btn.danger { background: #ef4444; border-color: #ef4444; }
.submit-btn.danger:hover { background: #dc2626; }

.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.result-dialog {
  background: #11152a; border-radius: 20px; padding: 40px 36px 32px;
  text-align: center; width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.rd-check { margin-bottom: 12px; }
.result-dialog h2 { font-size: 20px; color: #e2e8f0; margin-bottom: 20px; }
.rd-table { text-align: left; margin-bottom: 24px; }
.rd-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid rgba(255,255,255,0.03);
}
.rd-row span { font-size: 13px; color: #94a3b8; }
.rd-row strong { color: #e2e8f0; font-weight: 600; font-size: 13px; }
.rd-btn {
  width: 100%; height: 42px; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none;
  border-radius: 10px; font-size: 15px; cursor: pointer; font-weight: 600;
}
.rd-btn:hover { background: linear-gradient(135deg, #7775f6, #9b6ff7); }
</style>
