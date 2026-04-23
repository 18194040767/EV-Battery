<template>
  <router-view />
</template>

<style>
:root {
  --app-primary: #1d5c57;
  --app-primary-dark: #174844;
  --app-accent: #bc8d4b;
  --app-bg: #edf1f0;
  --app-surface: rgba(255, 255, 255, 0.92);
  --app-surface-strong: #ffffff;
  --app-border: rgba(29, 92, 87, 0.1);
  --app-text: #1a2b2a;
  --app-muted: #667a79;
  --app-shadow: 0 24px 60px rgba(18, 28, 28, 0.08);
  --app-shadow-soft: 0 10px 24px rgba(18, 28, 28, 0.06);
  --app-radius-lg: 28px;
  --app-radius-md: 20px;
  --app-radius-sm: 14px;
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
  background:
    radial-gradient(circle at top left, rgba(188, 141, 75, 0.12), transparent 24%),
    radial-gradient(circle at top right, rgba(29, 92, 87, 0.1), transparent 28%),
    linear-gradient(180deg, #f5f7f6 0%, #edf1f0 100%);
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
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(246, 249, 248, 0.86));
  box-shadow: var(--app-shadow-soft);
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  letter-spacing: -0.02em;
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
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 249, 0.9));
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
  border-radius: 14px;
}

:where(.el-table) {
  --el-table-header-bg-color: #f7faf9;
  --el-table-row-hover-bg-color: #f4f8f6;
}

@media (max-width: 768px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
