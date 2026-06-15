<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon red">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        </div>
        <h3>账户销户</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="银行卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入要销户的银行卡号" />
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
            <el-button type="danger" @click="onSubmit" :loading="loading">确认销户</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 确认弹窗 -->
    <Teleport to="body">
      <div class="overlay" v-if="confirmVisible" @click.self="confirmVisible = false">
        <div class="confirm-dialog">
          <div class="cd-icon warn">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="#f97316" stroke-width="2">
              <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
              <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
            </svg>
          </div>
          <h2>确认销户</h2>
          <p class="cd-text">销户后将无法恢复，账户余额将被清退。<br/>确定要销户该账户吗？</p>
          <div class="cd-actions">
            <el-button @click="confirmVisible = false" size="large">取消</el-button>
            <el-button type="danger" @click="doClose" :loading="loading" size="large">确认销户</el-button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 结果弹窗 -->
    <Teleport to="body">
      <div class="overlay" v-if="resultVisible" @click.self="resultVisible = false">
        <div class="result-dialog">
          <div class="rd-check">
            <svg viewBox="0 0 52 52" width="64" height="64"><circle cx="26" cy="26" r="25" fill="#22c55e"/><path d="M14 27l7 7 17-17" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <h2>销户成功</h2>
          <div class="rd-table">
            <div class="rd-row"><span>核心内部账号</span><strong>{{ result.accountNo }}</strong></div>
            <div class="rd-row"><span>银行卡号</span><strong>{{ result.cardNo }}</strong></div>
            <div class="rd-row"><span>销户日期</span><strong>{{ result.closedDate }}</strong></div>
          </div>
          <button class="rd-btn" @click="resultVisible = false">确 定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { closeAccount } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)
const confirmVisible = ref(false)
const resultVisible = ref(false)
const result = reactive({ accountNo: '', cardNo: '', closedDate: '' })

const form = reactive({
  cardNo: '',
  password: ''
})

const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  confirmVisible.value = true
}

async function doClose() {
  loading.value = true
  try {
    const payload = { ...form, outTradeNo: 'CLS_' + Date.now() }
    const res = await closeAccount(payload)
    result.accountNo = res.data.accountNo
    result.cardNo = res.data.cardNo
    result.closedDate = res.data.closedDate
    confirmVisible.value = false
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
.card-hd-icon.gray { background: rgba(148,163,184,0.1); color: #94a3b8; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }
.submit-btn.danger { background: #ef4444; border-color: #ef4444; }
.submit-btn.danger:hover { background: #dc2626; }

.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.confirm-dialog {
  background: #11152a; border-radius: 20px; padding: 36px 32px 28px;
  text-align: center; width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.cd-icon { margin-bottom: 12px; }
.confirm-dialog h2 { font-size: 20px; color: #e2e8f0; margin-bottom: 12px; }
.cd-text { font-size: 14px; color: #94a3b8; line-height: 1.8; margin-bottom: 24px; }
.cd-actions { display: flex; gap: 12px; justify-content: center; }

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
.rd-row strong { color: #e2e8f0; font-weight: 600; }
.rd-btn {
  width: 100%; height: 42px; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none;
  border-radius: 10px; font-size: 15px; cursor: pointer; font-weight: 600;
}
.rd-btn:hover { background: linear-gradient(135deg, #7775f6, #9b6ff7); }
</style>
