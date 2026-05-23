<template>
  <div class="admin-feature-page">
    <section class="feature-head">
      <div>
        <p>System</p>
        <h2>系统管理</h2>
        <span>集中管理后台显示、通知和安全偏好，设置会保存在当前浏览器。</span>
      </div>
      <el-button type="primary" @click="saveSettings">保存设置</el-button>
    </section>

    <section class="settings-grid">
      <article class="setting-card">
        <h3>后台偏好</h3>
        <div class="setting-row">
          <div>
            <strong>默认收起侧边栏</strong>
            <span>下次进入后台时使用紧凑导航。</span>
          </div>
          <el-switch v-model="settings.collapseSidebar" />
        </div>
        <div class="setting-row">
          <div>
            <strong>显示消息角标</strong>
            <span>侧栏和顶部消息入口展示未读数量。</span>
          </div>
          <el-switch v-model="settings.showBadges" />
        </div>
      </article>

      <article class="setting-card">
        <h3>安全策略</h3>
        <div class="setting-row">
          <div>
            <strong>登录设备提醒</strong>
            <span>检测到新设备时生成安全提醒。</span>
          </div>
          <el-switch v-model="settings.deviceAlert" />
        </div>
        <div class="setting-row">
          <div>
            <strong>敏感操作二次确认</strong>
            <span>审核、重置密码等操作前提示确认。</span>
          </div>
          <el-switch v-model="settings.confirmDanger" />
        </div>
      </article>

      <article class="setting-card">
        <h3>运行状态</h3>
        <div class="status-grid">
          <span v-for="item in statusItems" :key="item.label">
            <b>{{ item.value }}</b>
            {{ item.label }}
          </span>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const saved = JSON.parse(localStorage.getItem('adminSystemSettings') || 'null')
const settings = reactive(
  saved || {
    collapseSidebar: userStore.sidebarCollapsed,
    showBadges: true,
    deviceAlert: true,
    confirmDanger: true
  }
)

const statusItems = [
  { label: '服务运行', value: '正常' },
  { label: '数据库', value: '正常' },
  { label: '缓存服务', value: '正常' },
  { label: '存储空间', value: '62%' }
]

const saveSettings = () => {
  userStore.setSidebarCollapsed(settings.collapseSidebar)
  localStorage.setItem('adminSystemSettings', JSON.stringify(settings))
  ElMessage.success('系统设置已保存')
}
</script>

<style scoped>
.admin-feature-page {
  display: grid;
  gap: 18px;
}

.feature-head,
.setting-card {
  border: 1px solid rgba(54, 94, 150, 0.1);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(50, 86, 150, 0.08);
}

.feature-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
}

.feature-head p {
  margin: 0 0 8px;
  color: #126cff;
  font-size: 12px;
  font-weight: 800;
}

.feature-head h2 {
  margin: 0;
  color: #071331;
  font-size: 24px;
}

.feature-head span {
  display: block;
  margin-top: 8px;
  color: #66758f;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.setting-card {
  padding: 22px;
}

.setting-card h3 {
  margin: 0 0 18px;
  color: #071331;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0;
  border-top: 1px solid #edf2f8;
}

.setting-row strong {
  display: block;
  color: #17233c;
}

.setting-row span {
  display: block;
  margin-top: 5px;
  color: #66758f;
  font-size: 13px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.status-grid span {
  display: grid;
  place-items: center;
  min-height: 86px;
  border-radius: 12px;
  background: #f1f7ff;
  color: #66758f;
  font-size: 13px;
}

.status-grid b {
  color: #16a164;
  font-size: 20px;
}

@media (max-width: 1100px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
