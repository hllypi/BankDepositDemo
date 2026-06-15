<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon blue">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
        </div>
        <h3>修改密码</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="银行卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="旧密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" placeholder="请输入旧密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="12">
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24" class="btn-row">
            <el-button type="primary" @click="onSubmit" :loading="loading">确认修改</el-button>
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
          <h2>密码修改成功</h2>
          <p class="rd-text">您的账户密码已成功更新</p>
          <button class="rd-btn" @click="resultVisible = false">确 定</button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { changePassword } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)
const resultVisible = ref(false)

const form = reactive({
  cardNo: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '新密码长度需在6-20位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await changePassword({
      cardNo: form.cardNo,
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    resultVisible.value = true
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
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
.card-hd-icon.purple { background: rgba(168,85,247,0.1); color: #c084fc; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }

.overlay {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(4px);
}
.result-dialog {
  background: #11152a; border-radius: 20px; padding: 40px 36px 32px;
  text-align: center; width: 380px; box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.rd-check { margin-bottom: 12px; }
.result-dialog h2 { font-size: 20px; color: #e2e8f0; margin-bottom: 12px; }
.rd-text { font-size: 14px; color: #94a3b8; margin-bottom: 24px; }
.rd-btn {
  width: 100%; height: 42px; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none;
  border-radius: 10px; font-size: 15px; cursor: pointer; font-weight: 600;
}
.rd-btn:hover { background: linear-gradient(135deg, #7775f6, #9b6ff7); }
</style>
