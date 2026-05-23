<template>
  <router-view />
</template>

<style>
:root {
  --app-primary: #1f75ff;
  --app-primary-dark: #155bd9;
  --app-accent: #2f7cff;
  --app-bg: #f4f8ff;
  --app-surface: rgba(255, 255, 255, 0.92);
  --app-surface-strong: #ffffff;
  --app-border: rgba(73, 104, 150, 0.14);
  --app-text: #101a33;
  --app-muted: #687791;
  --app-shadow: 0 18px 46px rgba(37, 88, 170, 0.08);
  --app-shadow-soft: 0 10px 26px rgba(42, 91, 172, 0.07);
  --app-radius-lg: 18px;
  --app-radius-md: 14px;
  --app-radius-sm: 10px;
  color: var(--app-text);
  background: var(--app-bg);
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", "Noto Sans SC", sans-serif;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
  background: #ffffff;
}

a {
  color: inherit;
  text-decoration: none;
}

.page-shell {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  background: var(--app-surface);
  box-shadow: var(--app-shadow);
  backdrop-filter: blur(14px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  padding: 22px 24px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  background: #ffffff;
  box-shadow: var(--app-shadow-soft);
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  letter-spacing: 0;
}

.page-header p {
  margin: 6px 0 0;
  color: var(--app-muted);
}

.page-kicker {
  margin: 0 0 8px;
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.stack-card {
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-lg);
  background: var(--app-surface-strong);
  box-shadow: var(--app-shadow-soft);
}

.section-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.soft-grid {
  display: grid;
  gap: 14px;
}

.metric-card {
  padding: 18px 20px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-md);
  background: #ffffff;
}

.metric-card span {
  display: block;
  color: var(--app-muted);
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  color: var(--app-text);
}

:where(.el-card) {
  border-radius: var(--app-radius-lg);
  border-color: var(--app-border);
  box-shadow: var(--app-shadow-soft);
}

:where(.el-button--primary) {
  --el-button-bg-color: var(--app-primary);
  --el-button-border-color: var(--app-primary);
  --el-button-hover-bg-color: var(--app-primary-dark);
  --el-button-hover-border-color: var(--app-primary-dark);
}

:where(.el-input__wrapper, .el-select__wrapper, .el-textarea__inner, .el-input-number, .el-button) {
  border-radius: 8px;
}

:where(.el-table) {
  --el-table-header-bg-color: #ffffff;
  --el-table-row-hover-bg-color: #f6f9ff;
}

.el-empty__image {
  width: min(320px, 82%) !important;
  height: 200px !important;
  background: url("/empty-state.png") center / contain no-repeat;
}

.el-empty__image svg,
.el-empty__image img {
  display: none !important;
}

.el-empty__description p {
  color: #8d9bb1;
  font-size: 15px;
}

.page-header h2,
.page-head h2,
.hero h2,
.hero-copy h2,
.verify-head h2,
.hero-panel h2,
.order-head h2,
.profile-head h2,
.head h2,
.feature-head h2,
.assistant-head h2,
.info-card h2 {
  max-width: 100%;
  font-size: clamp(26px, 2.3vw, 30px) !important;
  line-height: 1.16 !important;
  letter-spacing: 0 !important;
}

@media (max-width: 768px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
