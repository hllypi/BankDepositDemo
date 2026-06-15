<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>
        </div>
        <h3>客户开户</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="客户姓名" prop="customerName">
              <el-input v-model="form.customerName" placeholder="请输入客户姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="证件类型" prop="idType">
              <el-select v-model="form.idType" placeholder="请选择证件类型" style="width:100%">
                <el-option label="身份证" value="01" />
                <el-option label="护照" value="02" />
                <el-option label="军官证" value="03" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="证件号码" prop="idNumber">
              <el-input v-model="form.idNumber" placeholder="请输入证件号码" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="6">
            <el-form-item label="账户密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="请输入账户密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="账户等级" prop="accountLevel">
              <el-select v-model="form.accountLevel" style="width:100%">
                <el-option label="Ⅰ类" :value="1" />
                <el-option label="Ⅱ类" :value="2" />
                <el-option label="Ⅲ类" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="开户行代码" prop="branchCode">
              <el-input v-model="form.branchCode" placeholder="如 010001" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="开户渠道" prop="channel">
              <el-select v-model="form.channel" style="width:100%">
                <el-option label="APP" value="APP" />
                <el-option label="柜面" value="COUNTER" />
                <el-option label="ATM" value="ATM" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24">
            <el-form-item label="通讯地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入通讯地址" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24" class="btn-row">
            <el-button type="primary" @click="onSubmit" :loading="loading">提交开户</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 结果弹窗 -->
    <Teleport to="body">
      <div class="overlay" v-if="resultVisible" @click.self="resultVisible = false">
        <div class="result-dialog">
          <div class="rd-check">
            <svg viewBox="0 0 52 52" width="64" height="64"><circle cx="26" cy="26" r="25" fill="#22c55e"/><path d="M14 27l7 7 17-17" stroke="#fff" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <h2>开户成功</h2>
          <div class="rd-table">
            <div class="rd-row"><span>银行卡号</span><strong>{{ result.cardNo }}</strong></div>
            <div class="rd-row"><span>核心账号</span><strong>{{ result.accountNo }}</strong></div>
          </div>
          <button class="rd-btn" @click="resultVisible = false">确 定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { openAccount } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)
const resultVisible = ref(false)
const result = reactive({ cardNo: '', accountNo: '' })

const form = reactive({
  customerName: '', idType: '01', idNumber: '', password: '',
  phone: '', address: '', accountLevel: 1, currency: 'CNY', branchCode: '010001', channel: 'APP'
})

const rules = {
  customerName: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  idType: [{ required: true, message: '请选择证件类型' }],
  idNumber: [{ required: true, message: '请输入证件号码', trigger: 'blur' }],
  password: [{ required: true, message: '请输入账户密码', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入通讯地址', trigger: 'blur' }],
  branchCode: [{ required: true, message: '请输入开户行代码', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择开户渠道' }]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    // 自动生成幂等号：OPEN + 年月日时分秒 + 随机数
    const now = new Date()
    const outTradeNo = 'OPEN' + 
      now.getFullYear().toString().padStart(4, '0') +
      (now.getMonth() + 1).toString().padStart(2, '0') +
      now.getDate().toString().padStart(2, '0') +
      now.getHours().toString().padStart(2, '0') +
      now.getMinutes().toString().padStart(2, '0') +
      now.getSeconds().toString().padStart(2, '0') +
      Math.random().toString(36).substring(2, 6).toUpperCase()
    const res = await openAccount({ ...form, outTradeNo })
    result.cardNo = res.data.cardNo
    result.accountNo = res.data.accountNo
    resultVisible.value = true
    formRef.value.resetFields()
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
.card-hd-icon.blue { background: rgba(99,102,241,0.1); color: #818cf8; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 24px; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }

/* 结果弹窗 */
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
