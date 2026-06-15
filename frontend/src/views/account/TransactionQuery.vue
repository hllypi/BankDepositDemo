<template>
  <div class="tx-page">
    <!-- 查询区 -->
    <div class="card">
      <div class="card-hd">
        <div class="card-hd-icon purple">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        </div>
        <h3>交易流水查询</h3>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="default" label-width="80px">
        <!-- 第一行：卡号 + 密码 + 时间范围 -->
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="银行卡号" prop="cardNo">
              <el-input v-model="form.cardNo" placeholder="请输入银行卡号" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="账户密码" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="dateRange" type="daterange"
                range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
                value-format="YYYY-MM-DD" :shortcuts="dateShortcuts"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二行：交易类型 + 收支类型 -->
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="6">
            <el-form-item label="交易类型">
              <el-select v-model="filterType" placeholder="全部类型" clearable style="width:100%"
                @change="curPage = 1">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="收支类型">
              <el-radio-group v-model="filterDcFlag" @change="onDcFlagChange">
                <el-radio-button value="">全部</el-radio-button>
                <el-radio-button value="C">收入</el-radio-button>
                <el-radio-button value="D">支出</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12"></el-col>
        </el-row>

        <!-- 第三行：金额筛选 -->
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24">
            <el-form-item label="金额筛选">
              <div class="amount-filter">
                <el-radio-group v-model="filterAmount" @change="onAmountPresetChange" size="small">
                  <el-radio-button value="">全部</el-radio-button>
                  <el-radio-button value="0-1000">&lt;1k</el-radio-button>
                  <el-radio-button value="1000-5000">1k~5k</el-radio-button>
                  <el-radio-button value="5000-10000">5k~1w</el-radio-button>
                  <el-radio-button value="10000-">&gt;1w</el-radio-button>
                  <el-radio-button value="custom">自定义</el-radio-button>
                </el-radio-group>
                <template v-if="filterAmount === 'custom'">
                  <el-input-number v-model="amountMin" :min="0" :precision="2" placeholder="最低" style="width:120px;margin-left:12px" controls-position="right" />
                  <span style="margin:0 8px;color:#94a3b8">~</span>
                  <el-input-number v-model="amountMax" :min="0" :precision="2" placeholder="最高" style="width:120px" controls-position="right" @change="onFilterChange" />
                </template>
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第四行：按钮 -->
        <el-row :gutter="16" style="margin-top: 20px;">
          <el-col :span="24" class="btn-row">
            <el-button type="primary" @click="onQuery" :loading="loading" :icon="Search">查询</el-button>
            <el-button @click="onReset">重置</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <!-- 统计区 -->
    <div class="stats-row" v-if="filteredList.length">
      <el-card shadow="hover" class="stat-card income">
        <div class="stat-label">收入合计</div>
        <div class="stat-value">+{{ incomeTotal }}</div>
        <div class="stat-count">{{ incomeCount }} 笔</div>
      </el-card>
      <el-card shadow="hover" class="stat-card expense">
        <div class="stat-label">支出合计</div>
        <div class="stat-value">-{{ expenseTotal }}</div>
        <div class="stat-count">{{ expenseCount }} 笔</div>
      </el-card>
      <el-card shadow="hover" class="stat-card net">
        <div class="stat-label">收支净额</div>
        <div class="stat-value" :style="{ color: net >= 0 ? '#22c55e' : '#ef4444' }">{{ netText }}</div>
        <div class="stat-count">共 {{ filteredList.length }} 笔</div>
      </el-card>
    </div>

    <!-- 流水列表 -->
    <div class="tx-list" v-loading="loading">
      <template v-if="filteredList.length">
        <div class="tx-list-header">
          <span class="tx-date-label">{{ dateLabel }}</span>
        </div>
        <div
          v-for="(item, idx) in pagedList" :key="idx"
          class="tx-item"
          @click="showDetail(item)"
        >
          <div class="tx-left">
            <div class="tx-icon" :style="{ background: iconBg(item) }">
              <el-icon :size="20"><component :is="icon(item)" /></el-icon>
            </div>
            <div class="tx-info">
              <div class="tx-type-label">{{ typeFullLabel(item) }}</div>
              <div class="tx-time">{{ item.transTime }}</div>
            </div>
          </div>
          <div class="tx-right">
            <div class="tx-amount" :class="item.dcFlag === 'C' ? 'credit' : 'debit'">
              {{ item.dcFlag === 'C' ? '+' : '-' }}{{ item.transAmount }}
            </div>
            <div class="tx-balance">余额 {{ item.balanceAfter }}</div>
          </div>
        </div>

        <div class="tx-pagination" v-if="totalPages > 1">
          <el-pagination
            v-model:current-page="curPage" :page-size="pageSize"
            :total="filteredList.length" layout="prev, pager, next"
            small background
          />
        </div>
      </template>
      <el-empty v-else-if="!loading" description="暂无交易记录" />
    </div>

    <!-- 交易详情弹窗 -->
    <el-dialog v-model="detailVisible" title="交易详情" width="480px" destroy-on-close>
      <template v-if="detail">
        <div class="detail-header">
          <div class="detail-amount" :class="detail.dcFlag === 'C' ? 'credit' : 'debit'">
            {{ detail.dcFlag === 'C' ? '+' : '-' }}{{ detail.transAmount }}
          </div>
          <div class="detail-type">{{ typeFullLabel(detail) }}</div>
        </div>
        <el-divider />
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="交易类型">{{ typeFullLabel(detail) }}</el-descriptions-item>
          <el-descriptions-item label="交易金额">
            <span :style="{ color: detail.dcFlag === 'C' ? '#22c55e' : '#ef4444', fontWeight: 'bold' }">
              {{ detail.dcFlag === 'C' ? '+' : '-' }}{{ detail.transAmount }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="交易账户">
            {{ form.cardNo }}
          </el-descriptions-item>
          <el-descriptions-item label="交易时间">{{ detail.transTime }}</el-descriptions-item>
          <el-descriptions-item label="交易摘要">{{ detail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="对方账户">{{ counterPartyText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="余额">{{ detail.balanceAfter }}</el-descriptions-item>
          <el-descriptions-item label="记账日">{{ detail.transTime?.split(' ')[0] || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { queryTransactions } from '../../api/index.js'

// ---- 表单 ----
const formRef = ref()
const loading = ref(false)

const initEnd = new Date()
const initStart = new Date()
initStart.setDate(initStart.getDate() - 30)

const dateRange = ref([
  formatDate(initStart),
  formatDate(initEnd)
])

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const dateShortcuts = [
  { text: '近一周', value: () => { const e = new Date(); const s = new Date(); s.setDate(s.getDate() - 7); return [formatDate(s), formatDate(e)] } },
  { text: '近一月', value: () => { const e = new Date(); const s = new Date(); s.setDate(s.getDate() - 30); return [formatDate(s), formatDate(e)] } },
  { text: '近三月', value: () => { const e = new Date(); const s = new Date(); s.setDate(s.getDate() - 90); return [formatDate(s), formatDate(e)] } }
]

const form = reactive({
  cardNo: '',
  password: '',
  transType: ''
})

const rules = {
  cardNo: [{ required: true, message: '请输入银行卡号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入账户密码', trigger: 'blur' }]
}

// ---- 筛选 ----
const filterType = ref('')
const filterDcFlag = ref('')
const filterAmount = ref('')
const amountMin = ref(null)
const amountMax = ref(null)

// ---- 数据 ----
const allData = ref([])
const curPage = ref(1)
const pageSize = 20

// 计算属性
const filteredList = computed(() => {
  let arr = allData.value

  // 交易类型筛选（transType+dcFlag 组合，客户端过滤）
  if (filterType.value) {
    arr = arr.filter(tx => typeCombo(tx) === filterType.value)
  }

  // 金额预设档位（客户端过滤，即时响应）
  if (filterAmount.value && filterAmount.value !== 'custom') {
    const [lo, hi] = filterAmount.value.split('-').map(Number)
    arr = arr.filter(tx => {
      const amt = Number(tx.transAmount)
      if (hi) return amt >= lo && amt < hi
      return amt >= lo
    })
  }

  return arr
})

const totalPages = computed(() => Math.ceil(filteredList.value.length / pageSize))
const pagedList = computed(() => {
  const s = (curPage.value - 1) * pageSize
  return filteredList.value.slice(s, s + pageSize)
})

const incomeTotal = computed(() => sumBy(filteredList.value, 'C'))
const expenseTotal = computed(() => sumBy(filteredList.value, 'D'))
const incomeCount = computed(() => countBy(filteredList.value, 'C'))
const expenseCount = computed(() => countBy(filteredList.value, 'D'))
const net = computed(() => {
  const ie = parseFloat(incomeTotal.value) || 0
  const ee = parseFloat(expenseTotal.value) || 0
  return (ie - ee).toFixed(2)
})
const netText = computed(() => {
  const n = parseFloat(net.value)
  return n >= 0 ? `+${n}` : `${n}`
})

const dateLabel = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return ''
  return `${dateRange.value[0]} ~ ${dateRange.value[1]}`
})

// ---- 详情弹窗 ----
const detailVisible = ref(false)
const detail = ref(null)

function showDetail(item) {
  detail.value = item
  detailVisible.value = true
}

// ---- 查询 ----
async function onQuery(dcFlagOverride) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  allData.value = []
  curPage.value = 1
  try {
    // 如果传入了覆盖值就使用它，否则使用当前筛选值
    let dcFlagVal = filterDcFlag.value || ''
    if (dcFlagOverride !== undefined && dcFlagOverride !== null) {
      // 处理事件对象
      const value = dcFlagOverride && dcFlagOverride.target ? dcFlagOverride.target.value : dcFlagOverride
      dcFlagVal = String(value || '')
    }
    const payload = {
      cardNo: String(form.cardNo || ''),
      password: String(form.password || ''),
      startDate: dateRange.value && dateRange.value[0] ? String(dateRange.value[0]) + ' 00:00:00' : '',
      endDate: dateRange.value && dateRange.value[1] ? String(dateRange.value[1]) + ' 23:59:59' : '',
      dcFlag: dcFlagVal || null,
      pageNum: 1,
      pageSize: 500
    }
    // 自定义金额范围发后端
    if (filterAmount.value === 'custom') {
      if (amountMin.value != null) payload.amountMin = Number(amountMin.value)
      if (amountMax.value != null) payload.amountMax = Number(amountMax.value)
    }
    const res = await queryTransactions(payload)
    allData.value = res.data.transList || []
  } catch (e) { 
    console.error('查询失败:', e)
    allData.value = [] 
  }
  finally { loading.value = false }
}

function onReset() {
  formRef.value?.resetFields()
  dateRange.value = [formatDate(initStart), formatDate(initEnd)]
  filterType.value = ''
  filterDcFlag.value = ''
  filterAmount.value = ''
  amountMin.value = null
  amountMax.value = null
  allData.value = []
  curPage.value = 1
}

function onDcFlagChange(event) {
  curPage.value = 1
  // 处理事件对象或直接值
  const value = event && event.target ? event.target.value : event
  onQuery(value)
}
function onAmountPresetChange(val) {
  if (val !== 'custom') { amountMin.value = null; amountMax.value = null }
  else { onQuery() }  // 切换到自定义时重查（不带金额限制，等用户输入后再触发）
  curPage.value = 1
}

// ---- 工具函数 ----
function sumBy(list, dc) {
  return list.filter(t => t.dcFlag === dc)
    .reduce((s, t) => s + parseFloat(t.transAmount || 0), 0).toFixed(2)
}
function countBy(list, dc) {
  return list.filter(t => t.dcFlag === dc).length
}

// transType + dcFlag 组合 → 业务类型
function typeCombo(tx) {
  const tp = tx.transType
  const dc = tx.dcFlag
  if (tp === '01') return 'deposit'        // 存款
  if (tp === '02') return 'withdraw'       // 取款
  if (tp === '03' && dc === 'D') return 'transfer_out'  // 转账支取
  if (tp === '03' && dc === 'C') return 'transfer_in'   // 转账存入
  if (tp === '04') return 'interest'       // 利息
  if (tp === '00') return 'open'           // 开户
  if (tp === '05') return 'close'          // 销户
  return tp
}

const typeOptions = [
  { label: '存款', value: 'deposit' },
  { label: '取款', value: 'withdraw' },
  { label: '转账存入', value: 'transfer_in' },
  { label: '转账支取', value: 'transfer_out' },
  { label: '利息', value: 'interest' },
  { label: '开户', value: 'open' },
  { label: '销户', value: 'close' }
]

const typeFullLabels = {
  deposit: '存款', withdraw: '取款',
  transfer_in: '转账存入', transfer_out: '转账支取',
  interest: '结息', open: '开户', close: '销户'
}
function typeFullLabel(tx) {
  return typeFullLabels[typeCombo(tx)] || tx.transType
}

const icons = {
  deposit: 'CirclePlusFilled', withdraw: 'RemoveFilled',
  transfer_in: 'Download', transfer_out: 'Upload',
  interest: 'Money', open: 'UserFilled', close: 'SwitchFilled'
}
const iconBgs = {
  deposit: 'rgba(34,197,94,0.08)', withdraw: 'rgba(251,146,60,0.08)',
  transfer_in: 'rgba(59,130,246,0.08)', transfer_out: 'rgba(248,113,113,0.08)',
  interest: 'rgba(168,85,247,0.08)', open: 'rgba(99,102,241,0.08)', close: 'rgba(148,163,184,0.08)'
}
function icon(tx) { return icons[typeCombo(tx)] || 'List' }
function iconBg(tx) { return iconBgs[typeCombo(tx)] || 'rgba(255,255,255,0.02)' }

function counterPartyText(tx) {
  if (typeCombo(tx) === 'transfer_in' || typeCombo(tx) === 'transfer_out') {
    return tx.counterPartyAccount || '-'
  }
  if (typeCombo(tx) === 'deposit') return '现金/他行转入'
  if (typeCombo(tx) === 'withdraw') return '现金/他行转出'
  if (typeCombo(tx) === 'interest') return '系统结息'
  return '-'
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
.card-hd-icon.purple { 
  background: rgba(168,85,247,0.1); 
  color: #c084fc; 
}
.card-hd h3 { 
  font-size: 18px; 
  font-weight: 700; 
  color: #e2e8f0; 
  margin: 0;
}
.btn-col { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
  justify-content: flex-end;
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
:deep(.el-select .el-input__wrapper) {
  background: rgba(255,255,255,0.03);
}

/* 统计区 */
.stats-row { 
  display: grid; 
  grid-template-columns: repeat(3, 1fr);
  gap: 20px; 
  margin-bottom: 20px; 
}
.stat-card {
  background: rgba(255,255,255,0.015); 
  border: 1px solid rgba(255,255,255,0.04); 
  border-radius: 16px; 
  padding: 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.stat-card.income::before { background: linear-gradient(90deg, #22c55e, #16a34a); }
.stat-card.expense::before { background: linear-gradient(90deg, #ef4444, #dc2626); }
.stat-card.net::before { background: linear-gradient(90deg, #6366f1, #4f46e5); }

.stat-label { 
  font-size: 13px; 
  margin-bottom: 10px; 
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.stat-card.income .stat-label { color: #22c55e; }
.stat-card.expense .stat-label { color: #ef4444; }
.stat-card.net .stat-label { color: #6366f1; }

.stat-value { 
  font-size: 28px; 
  font-weight: 700; 
  margin-bottom: 8px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}
.stat-card.income .stat-value { color: #22c55e; }
.stat-card.expense .stat-value { color: #ef4444; }
.stat-card.net .stat-value { color: #e2e8f0; }

.stat-count { 
  font-size: 12px; 
  color: #64748b; 
}

/* 流水列表 */
.tx-list { 
  background: rgba(255,255,255,0.01); 
  border-radius: 16px; 
  margin-bottom: 20px;
  border: 1px solid rgba(255,255,255,0.03);
}
.tx-list-header { 
  padding: 18px 24px; 
  font-size: 13px; 
  color: #64748b; 
  border-bottom: 1px solid rgba(255,255,255,0.04);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.tx-item { 
  display: flex; 
  align-items: center; 
  padding: 20px 24px; 
  border-bottom: 1px solid rgba(255,255,255,0.02); 
  cursor: pointer; 
  transition: all 0.15s ease; 
}
.tx-item:hover { 
  background: rgba(255,255,255,0.02);
  padding-left: 28px;
}
.tx-item:last-child { border-bottom: none; }
.tx-left { 
  display: flex; 
  align-items: center; 
  gap: 16px; 
  flex: 1;
}
.tx-icon {
  width: 48px; 
  height: 48px; 
  border-radius: 14px;
  display: flex; 
  align-items: center; 
  justify-content: center;
  flex-shrink: 0;
}
.tx-info { 
  flex: 1; 
  min-width: 0;
}
.tx-type-label { 
  font-size: 15px; 
  color: #e2e8f0; 
  font-weight: 500;
  margin-bottom: 4px;
}
.tx-time { 
  font-size: 13px; 
  color: #64748b; 
}
.tx-right { 
  text-align: right; 
  flex-shrink: 0;
}
.tx-amount { 
  font-size: 18px; 
  font-weight: 600; 
  margin-bottom: 4px;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}
.tx-amount.credit { color: #22c55e; }
.tx-amount.debit { color: #ef4444; }
.tx-balance { 
  font-size: 12px; 
  color: #64748b; 
}
.tx-pagination { 
  display: flex; 
  justify-content: center; 
  padding: 20px; 
  border-top: 1px solid rgba(255,255,255,0.03);
}

/* 详情弹窗 */
.detail-header { 
  text-align: center; 
  padding: 16px 0 24px; 
}
.detail-amount { 
  font-size: 36px; 
  font-weight: 700; 
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}
.detail-amount.credit { color: #22c55e; }
.detail-amount.debit { color: #ef4444; }
.detail-type { 
  font-size: 14px; 
  color: #94a3b8; 
  margin-top: 8px; 
}

.amount-filter { 
  display: flex; 
  align-items: center; 
  flex-wrap: wrap; 
  gap: 8px; 
}

/* 收支类型单选按钮水平排列 */
:deep(.el-radio-group) {
  display: flex !important;
  gap: 0 !important;
  flex-wrap: nowrap !important;
}
:deep(.el-radio-group .el-radio-button) {
  display: inline-flex !important;
}
:deep(.el-radio-button:first-child .el-radio-button__inner) { 
  border-radius: 8px 0 0 8px; 
}
:deep(.el-radio-button:last-child .el-radio-button__inner) { 
  border-radius: 0 8px 8px 0; 
}

/* 日期选择器样式 */
:deep(.el-date-editor .el-input__wrapper) { 
  background: rgba(255,255,255,0.03) !important; 
}
</style>
