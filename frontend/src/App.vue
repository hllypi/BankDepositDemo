<template>
  <router-view />
</template>

<script setup>
</script>

<style>
*, *::before, *::after { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: #0f1428;
  color: #e2e8f0;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
::-webkit-scrollbar { width: 5px; }
::-webkit-scrollbar-track { background: rgba(255,255,255,0.02); }
::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.08); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.15); }

/* ===== Element Plus 暗色主题变量 ===== */
:root {
  --el-color-primary: #818cf8;
  --el-color-primary-light-3: #a5b4fc;
  --el-color-primary-light-5: #c7d2fe;
  --el-color-primary-light-7: #e0e7ff;
  --el-color-primary-light-9: #f0f5ff;
  --el-color-primary-dark-2: #6366f1;
  --el-bg-color: #11152a;
  --el-bg-color-overlay: #1a1f3a;
  --el-border-color: rgba(255,255,255,0.08);
  --el-border-color-light: rgba(255,255,255,0.06);
  --el-border-color-lighter: rgba(255,255,255,0.04);
  --el-text-color-primary: #e2e8f0;
  --el-text-color-regular: #cbd5e1;
  --el-text-color-secondary: #94a3b8;
  --el-text-color-placeholder: #64748b;
  --el-fill-color: rgba(255,255,255,0.02);
  --el-fill-color-light: rgba(255,255,255,0.04);
  --el-fill-color-blank: rgba(255,255,255,0.015);
  --el-border-radius-base: 10px;
  --el-border-radius-round: 20px;
  --el-input-bg-color: rgba(255,255,255,0.03);
  --el-input-border-color: rgba(255,255,255,0.08);
  --el-input-hover-border-color: rgba(255,255,255,0.15);
  --el-input-focus-border-color: #818cf8;
  --el-input-placeholder-color: #64748b;
  --el-disabled-bg-color: rgba(255,255,255,0.02);
  --el-disabled-border-color: rgba(255,255,255,0.06);
  --el-disabled-text-color: #64748b;
}

/* 全局 Element Plus 暗色微调 */
.el-input__wrapper {
  background: rgba(255,255,255,0.03) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  box-shadow: none !important;
  border-radius: 10px !important;
  transition: all 0.3s !important;
}
.el-input__wrapper:hover { border-color: rgba(255,255,255,0.15) !important; }
.el-input__wrapper.is-focus { border-color: #818cf8 !important; box-shadow: 0 0 0 3px rgba(129,140,248,0.15) !important; }
.el-input__inner { color: #e2e8f0 !important; }
.el-input__inner::placeholder { color: #64748b !important; }

.el-select .el-input__wrapper { background: rgba(255,255,255,0.03) !important; }
.el-select-dropdown {
  background: #1a1f3a !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
}
.el-select-dropdown__item { color: #e2e8f0 !important; }
.el-select-dropdown__item:hover { background: rgba(255,255,255,0.04) !important; }
.el-select-dropdown__item.selected { color: #818cf8 !important; }

.el-button--primary {
  background: linear-gradient(135deg, #6366f1, #8b5cf6) !important;
  border: none !important;
}
.el-button--primary:hover { background: linear-gradient(135deg, #7775f6, #9b6ff7) !important; }
.el-button--primary:active { background: linear-gradient(135deg, #5552e8, #7c51eb) !important; }
.el-button--default {
  background: rgba(255,255,255,0.04) !important;
  border: 1px solid rgba(255,255,255,0.08) !important;
  color: #e2e8f0 !important;
}
.el-button--default:hover { background: rgba(255,255,255,0.08) !important; border-color: rgba(255,255,255,0.12) !important; }

.el-table {
  --el-table-bg-color: rgba(255,255,255,0.015);
  --el-table-tr-bg-color: rgba(255,255,255,0.015);
  --el-table-header-bg-color: rgba(255,255,255,0.03);
  --el-table-row-hover-bg-color: rgba(255,255,255,0.04);
  --el-table-border-color: rgba(255,255,255,0.06);
  --el-table-text-color: #e2e8f0;
  --el-table-header-text-color: #94a3b8;
}
.el-table th.el-table__cell { background: rgba(255,255,255,0.03); border-bottom-color: rgba(255,255,255,0.06); color: #94a3b8; }
.el-table td.el-table__cell { border-bottom-color: rgba(255,255,255,0.04); color: #e2e8f0; }
.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell { background: rgba(255,255,255,0.02); }

.el-pagination {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: rgba(255,255,255,0.04);
  --el-pagination-text-color: #94a3b8;
  --el-pagination-hover-color: #818cf8;
}
.el-pagination button { background: rgba(255,255,255,0.04) !important; color: #94a3b8 !important; }
.el-pagination button:hover { color: #818cf8 !important; }
.el-pagination .el-pager li { background: rgba(255,255,255,0.04) !important; color: #94a3b8 !important; }
.el-pagination .el-pager li:hover { color: #818cf8 !important; }
.el-pagination .el-pager li.is-active { background: #818cf8 !important; color: #fff !important; }

.el-dialog {
  --el-dialog-bg-color: #1a1f3a;
  --el-dialog-border-radius: 16px;
}
.el-dialog { background: #1a1f3a !important; border: 1px solid rgba(255,255,255,0.08) !important; }
.el-dialog__header { color: #e2e8f0 !important; }
.el-dialog__title { color: #e2e8f0 !important; }
.el-message-box { --el-messagebox-bg-color: #1a1f3a; background: #1a1f3a !important; }
.el-message { --el-message-bg-color: #1a1f3a; background: #1a1f3a !important; border: 1px solid rgba(255,255,255,0.08) !important; }

.el-form-item__label { color: #94a3b8 !important; font-weight: 500; }

.el-card { background: rgba(255,255,255,0.015) !important; border: 1px solid rgba(255,255,255,0.06) !important; }
.el-card__header { color: #e2e8f0 !important; border-bottom-color: rgba(255,255,255,0.06) !important; }

.el-radio-group { display: flex !important; flex-wrap: nowrap !important; gap: 0 !important; }
.el-radio-group .el-radio-button { display: inline-flex !important; }

.el-checkbox__label { color: #e2e8f0 !important; }
.el-checkbox__inner { background: rgba(255,255,255,0.04) !important; border-color: rgba(255,255,255,0.08) !important; }
.el-checkbox__input.is-checked .el-checkbox__inner { background: #818cf8 !important; border-color: #818cf8 !important; }

.el-date-editor .el-input__wrapper { background: rgba(255,255,255,0.03) !important; }
.el-picker-panel { background: #0f1428 !important; border: 1px solid rgba(255,255,255,0.1) !important; }
.el-date-picker__header { background: rgba(255,255,255,0.02) !important; }
.el-date-picker__header-label { color: #e2e8f0 !important; font-weight: 600 !important; }
.el-date-picker__time-header { color: #94a3b8 !important; }
.el-date-table th { color: #64748b !important; font-weight: 500 !important; }
.el-date-table td.available .el-date-table-cell__text { color: #cbd5e1 !important; font-weight: 500 !important; }
.el-date-table td.available:hover .el-date-table-cell__text { color: #fff !important; background: rgba(129,140,248,0.2) !important; }
.el-date-table td.today .el-date-table-cell__text { color: #818cf8 !important; font-weight: 700 !important; }
.el-date-table td.current .el-date-table-cell__text { background: linear-gradient(135deg, #6366f1, #8b5cf6) !important; color: #fff !important; font-weight: 600 !important; }
.el-date-table td.disabled .el-date-table-cell__text { color: #475569 !important; }
.el-picker-panel__icon-btn { color: #94a3b8 !important; }
.el-picker-panel__icon-btn:hover { color: #e2e8f0 !important; }
.el-picker-panel__footer { background: rgba(255,255,255,0.02) !important; border-top: 1px solid rgba(255,255,255,0.06) !important; }
.el-picker-panel__shortcut { color: #94a3b8 !important; }
.el-picker-panel__shortcut:hover { color: #e2e8f0 !important; background: rgba(255,255,255,0.06) !important; }

.el-input-number { background: rgba(255,255,255,0.03) !important; }
.el-input-number .el-input__wrapper { background: transparent !important; }
.el-input-number__decrease, .el-input-number__increase { background: rgba(255,255,255,0.04) !important; border-color: rgba(255,255,255,0.08) !important; color: #94a3b8 !important; }

.el-divider { border-color: rgba(255,255,255,0.06) !important; }

.el-descriptions { background: rgba(255,255,255,0.02) !important; }
.el-descriptions__label { color: #94a3b8 !important; background: rgba(255,255,255,0.04) !important; }
.el-descriptions__content { color: #e2e8f0 !important; }

.el-empty__description { color: #94a3b8 !important; }

.el-loading-mask { background: rgba(17,21,42,0.8) !important; }
.el-loading-spinner .el-loading-text { color: #94a3b8 !important; }
</style>
