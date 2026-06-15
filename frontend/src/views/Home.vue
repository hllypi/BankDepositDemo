<template>
  <div class="home">
    <div class="section-header">
      <div class="sh-left"><h3>功能导航</h3><span class="sh-desc">选择模块开始操作</span></div>
      <div class="sh-line"></div>
    </div>

    <div class="quick-grid">
      <div class="quick-card" v-for="item in cards" :key="item.path"
        @click="$router.push(item.path)" :style="{ '--accent': item.color }">
        <div class="qc-top">
          <div class="qc-icon" :style="{ background: item.bg, color: item.color }" v-html="item.icon"></div>
        </div>
        <span class="qc-label">{{ item.label }}</span>
        <span class="qc-desc">{{ item.desc }}</span>
        <div class="qc-glow"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
const cards = [
  { path: '/account/open', label: '客户开户', desc: '创建活期存款账户，分配银行卡号', color: '#818cf8', bg: 'rgba(99,102,241,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg>' },
  { path: '/account/deposit', label: '存款', desc: '柜台 / APP / ATM 多渠道入金', color: '#4ade80', bg: 'rgba(34,197,94,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg>' },
  { path: '/account/withdraw', label: '取款', desc: '账户等级限额 · 可用余额校验', color: '#fb923c', bg: 'rgba(251,146,60,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg>' },
  { path: '/account/transfer', label: '行内转账', desc: '双边账互绑 · 实时到账', color: '#f87171', bg: 'rgba(248,113,113,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>' },
  { path: '/account/transactions', label: '流水查询', desc: '90天内明细 · 分页检索', color: '#c084fc', bg: 'rgba(192,132,252,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>' },
  { path: '/interest/settle', label: '结息管理', desc: '日终积数扫描 · 批量派息', color: '#a5b4fc', bg: 'rgba(165,180,252,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>' },
  { path: '/account/close', label: '销户', desc: '自动结息后关闭账户', color: '#94a3b8', bg: 'rgba(148,163,184,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>' },
  { path: '/account/password', label: '修改密码', desc: 'BCrypt 加密存储', color: '#fda4af', bg: 'rgba(253,164,175,0.1)',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>' }
]

function particleStyle(i) {
  const x = 10 + (i * 17) % 90
  const y = 10 + (i * 23) % 80
  const size = 3 + (i % 4)
  const delay = (i * 1.2).toFixed(1)
  return {
    left: x + '%', top: y + '%',
    width: size + 'px', height: size + 'px',
    animationDelay: delay + 's',
    opacity: 0.15 + (i % 3) * 0.15
  }
}
</script>

<style scoped>
.home { width: 100%; max-width: 1280px; }

/* ===== Section ===== */
.section-header { display: flex; align-items: center; gap: 16px; margin-bottom: 24px; }
.sh-left { display: flex; align-items: baseline; gap: 12px; white-space: nowrap; }
.sh-left h3 { font-size: 18px; font-weight: 700; color: #e2e8f0; }
.sh-desc { font-size: 13px; color: rgba(255,255,255,0.2); }
.sh-line { flex: 1; height: 1px; background: rgba(255,255,255,0.04); }

/* ===== Quick Cards ===== */
.quick-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.quick-card {
  position: relative; overflow: hidden;
  background: rgba(255,255,255,0.015); border: 1px solid rgba(255,255,255,0.04);
  border-radius: 18px; padding: 24px 20px 20px;
  cursor: pointer; transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.qc-glow {
  position: absolute; inset: 0; opacity: 0;
  background: radial-gradient(circle at var(--mx, 50%) var(--my, 50%), var(--accent, #818cf8) 0%, transparent 70%);
  transition: opacity 0.4s; pointer-events: none; z-index: 0;
}
.quick-card:hover { transform: translateY(-2px); border-color: rgba(99,102,241,0.2); }
.quick-card:hover .qc-glow { opacity: 0.06; }
.qc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; z-index: 1; }
.qc-icon {
  width: 44px; height: 44px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.qc-label { font-size: 14px; font-weight: 600; color: #e2e8f0; margin-bottom: 4px; z-index: 1; }
.qc-desc { font-size: 12px; color: rgba(255,255,255,0.25); line-height: 1.5; z-index: 1; }
</style>
