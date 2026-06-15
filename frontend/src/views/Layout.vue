<template>
  <div class="app-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 40 40" width="40" height="40" fill="none">
            <defs>
              <linearGradient id="bgGrad" x1="0" y1="0" x2="40" y2="40">
                <stop offset="0%" stop-color="#6366f1"/>
                <stop offset="100%" stop-color="#a78bfa"/>
              </linearGradient>
            </defs>
            <rect width="40" height="40" rx="12" fill="url(#bgGrad)" opacity="0.95"/>
            <path d="M10 26L20 10L30 26H24V32H16V26H10Z" fill="#fff" opacity="0.95"/>
          </svg>
        </div>
        <div class="brand-text">
          <span class="brand-name">CoreBank</span>
          <span class="brand-sub">CORE BANKING SYSTEM</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/home" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg></span>
          <span>首页</span>
        </router-link>

        <div class="nav-label"><span>账户管理</span></div>

        <router-link to="/account/query" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg></span>
          <span>账户信息</span>
        </router-link>
        <router-link to="/account/open" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="8.5" cy="7" r="4"/><line x1="20" y1="8" x2="20" y2="14"/><line x1="23" y1="11" x2="17" y2="11"/></svg></span>
          <span>客户开户</span>
        </router-link>
        <router-link to="/account/deposit" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="16"/><line x1="8" y1="12" x2="16" y2="12"/></svg></span>
          <span>存款</span>
        </router-link>
        <router-link to="/account/withdraw" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="8" y1="12" x2="16" y2="12"/></svg></span>
          <span>取款</span>
        </router-link>
        <router-link to="/account/transfer" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg></span>
          <span>转账</span>
        </router-link>
        <router-link to="/account/customer" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></span>
          <span>客户信息</span>
        </router-link>
        <router-link to="/account/transactions" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg></span>
          <span>流水查询</span>
        </router-link>
        <router-link to="/account/password" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg></span>
          <span>修改密码</span>
        </router-link>
        <router-link to="/account/close" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg></span>
          <span>销户</span>
        </router-link>

        <div class="nav-label"><span>结息管理</span></div>

        <router-link to="/interest/settle" class="nav-item" active-class="active">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg></span>
          <span>结息管理</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="version-badge">v2.0</div>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="main-area">
      <header class="topbar">
        <div class="topbar-left">
          <h2 class="page-title">{{ route.meta.title }}</h2>
          <span class="page-divider">/</span>
          <span class="page-breadcrumb">dashboard</span>
        </div>
        <div class="topbar-right">
          <div class="topbar-time-box">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            <span>{{ now }}</span>
          </div>
          <div class="topbar-avatar">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </div>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { ref, onMounted, onUnmounted } from 'vue'

const route = useRoute()
const now = ref('')

function updateTime() {
  const d = new Date()
  now.value = d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0') + ' ' +
    String(d.getHours()).padStart(2, '0') + ':' +
    String(d.getMinutes()).padStart(2, '0') + ':' +
    String(d.getSeconds()).padStart(2, '0')
}
let timer
onMounted(() => { updateTime(); timer = setInterval(updateTime, 1000) })
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
.app-layout {
  display: flex; height: 100vh; width: 100vw; overflow: hidden;
  background: linear-gradient(135deg, #0f1428 0%, #11152a 50%, #1a1f3a 100%);
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 260px; background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  display: flex; flex-direction: column; flex-shrink: 0; position: relative;
  border-right: 1px solid rgba(0,0,0,0.05);
  box-shadow: 4px 0 24px rgba(0,0,0,0.06);
}
.sidebar-brand {
  display: flex; align-items: center; gap: 12px;
  padding: 28px 20px 22px; position: relative;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.brand-icon { flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; }
.brand-name {
  font-size: 20px; font-weight: 800; letter-spacing: -0.5px;
  background: linear-gradient(135deg, #a5b4fc 0%, #c4b5fd 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.brand-sub { color: rgba(255,255,255,0.3); font-size: 10px; font-weight: 600; letter-spacing: 3px; margin-top: 1px; }

.sidebar-nav { flex: 1; overflow-y: auto; padding: 20px 12px; }
.nav-label {
  padding: 24px 14px 10px;
  color: rgba(255,255,255,0.25); font-size: 10px;
  font-weight: 700; letter-spacing: 3px;
  display: flex; align-items: center; gap: 10px;
}
.nav-label::after {
  content: ''; flex: 1; height: 1px;
  background: rgba(255,255,255,0.06);
}
.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 14px; border-radius: 12px;
  color: rgba(255,255,255,0.5); text-decoration: none;
  font-size: 13.5px; margin-bottom: 2px; font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.nav-icon {
  display: flex; align-items: center; justify-content: center;
  width: 34px; height: 34px; border-radius: 10px;
  background: rgba(255,255,255,0.04); flex-shrink: 0;
  transition: all 0.3s;
}
.nav-item:hover {
  background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.85);
  transform: translateX(2px);
}
.nav-item:hover .nav-icon { background: rgba(99,102,241,0.15); color: #a5b4fc; }
.nav-item.active {
  background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(139,92,246,0.08));
  color: #c7d2fe; font-weight: 600;
}
.nav-item.active .nav-icon {
  background: linear-gradient(135deg, rgba(99,102,241,0.25), rgba(139,92,246,0.15));
  color: #a5b4fc;
}

.sidebar-footer { padding: 14px 20px; border-top: 1px solid rgba(255,255,255,0.06); }
.version-badge {
  display: inline-block; background: rgba(255,255,255,0.04);
  color: rgba(255,255,255,0.25); font-size: 11px; font-weight: 600;
  padding: 4px 10px; border-radius: 6px; letter-spacing: 1px;
}

/* ===== 主区域 ===== */
.main-area { flex: 1; display: flex; flex-direction: column; min-width: 0; background: transparent; }
.topbar {
  height: 64px; background: rgba(255,255,255,0.03);
  border-bottom: 1px solid rgba(255,255,255,0.06);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 40px; flex-shrink: 0;
}
.topbar-left { display: flex; align-items: baseline; gap: 10px; }
.page-title { font-size: 17px; font-weight: 700; color: #e2e8f0; }
.page-divider { color: rgba(255,255,255,0.2); font-size: 16px; }
.page-breadcrumb { font-size: 12px; color: #94a3b8; }
.topbar-right { display: flex; align-items: center; gap: 16px; }
.topbar-time-box {
  display: flex; align-items: center; gap: 8px;
  background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px; padding: 6px 14px;
  font-size: 12px; color: #94a3b8; font-variant-numeric: tabular-nums;
}
.topbar-avatar {
  width: 34px; height: 34px; border-radius: 10px;
  background: linear-gradient(135deg, rgba(99,102,241,0.15), rgba(139,92,246,0.1));
  display: flex; align-items: center; justify-content: center; color: #818cf8;
  border: 1px solid rgba(99,102,241,0.15);
}

.content {
  flex: 1; overflow-y: auto; padding: 36px 40px;
  display: flex; justify-content: center;
}
</style>
