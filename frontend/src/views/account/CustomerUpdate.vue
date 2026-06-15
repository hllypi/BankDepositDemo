<template>
  <div class="page">
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon purple">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
        </div>
        <h3>修改客户信息</h3>
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
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入新的联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通讯地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入新的通讯地址" maxlength="200" />
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
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateCustomer } from '../../api/index.js'

const formRef = ref()
const loading = ref(false)

const form = reactive({
  cardNo: '',
  password: '',
  phone: '',
  address: ''
})

const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入账户密码', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入通讯地址', trigger: 'blur' }]
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await updateCustomer({ ...form })
    ElMessage.success('客户信息修改成功')
    formRef.value.resetFields()
  } catch (e) { /* 拦截器已弹窗 */ }
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
.card-hd-icon.cyan { background: rgba(6,182,212,0.1); color: #22d3ee; }
.card-hd h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 24px; }
.submit-btn { width: 100%; margin-top: 12px; height: 46px; border-radius: 10px; font-size: 15px; font-weight: 600; }
</style>
