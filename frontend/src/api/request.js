import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截 — 添加认证 Token
request.interceptors.request.use((config) => {
  config.headers['X-Auth-Token'] = '9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08'
  return config
})

// 响应拦截
request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.result_code !== 0) {
      ElMessage.error(data.result_msg || '请求失败')
      return Promise.reject(new Error(data.result_msg))
    }
    return data
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
