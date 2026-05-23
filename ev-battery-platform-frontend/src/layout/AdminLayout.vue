<template>
  <el-container class="admin-shell">
    <el-aside :width="collapsed ? '86px' : '260px'" class="admin-aside">
      <button v-if="!collapsed" class="admin-brand" type="button" @click="go('/home')" aria-label="返回首页">
        <span class="admin-brand-copy">
          <p>SYSTEM CONSOLE</p>
          <h1>后台管理中心</h1>
        </span>
      </button>

      <el-menu :default-active="$route.path" router :collapse="collapsed" :collapse-transition="false" class="admin-menu">
        <el-menu-item v-for="item in primaryMenus" :key="item.path" :index="item.path">
          <AppNavIcon :name="item.icon" />
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>

      <div class="admin-menu-extra">
        <button class="extra-item" :class="{ active: $route.path === '/admin/system' }" type="button" @click="go('/admin/system')">
          <AppNavIcon name="settings" />
          <span v-if="!collapsed">系统管理</span>
          <el-icon v-if="!collapsed"><ArrowDown /></el-icon>
        </button>
        <button class="extra-item has-badge" :class="{ active: $route.path === '/admin/messages' }" type="button" @click="go('/admin/messages')">
          <AppNavIcon name="bell" />
          <span v-if="!collapsed">消息通知</span>
          <b v-if="!collapsed && unreadCount">{{ unreadCount }}</b>
        </button>
        <button class="extra-item" :class="{ active: $route.path === '/admin/assistant' }" type="button" @click="go('/admin/assistant')">
          <AppNavIcon name="assistant" />
          <span v-if="!collapsed">AI 助手</span>
        </button>
      </div>
    </el-aside>

    <el-container class="admin-main-shell">
      <el-header class="admin-header">
        <div class="header-left">
          <el-button text class="header-button" @click="userStore.toggleSidebar()">
            {{ collapsed ? '展开菜单' : '收起菜单' }}
          </el-button>
          <span class="breadcrumb-sep">/</span>
          <span class="breadcrumb-muted">管理后台</span>
          <span class="breadcrumb-sep">/</span>
          <strong class="header-title">{{ $route.meta?.title || '系统管理' }}</strong>
        </div>
        <div class="header-right">
          <MessageCenter />
          <UserDropdown mode="admin" />
        </div>
      </el-header>

      <el-main class="admin-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import AppNavIcon from '../components/AppNavIcon.vue'
import MessageCenter from '../components/MessageCenter.vue'
import UserDropdown from '../components/UserDropdown.vue'
import { getUnreadCount } from '../api/message'

const router = useRouter()
const userStore = useUserStore()
const collapsed = computed(() => userStore.sidebarCollapsed)
const unreadCount = ref(0)

const primaryMenus = [
  { path: '/admin/dashboard', label: '数据驾驶舱', icon: 'dashboard' },
  { path: '/admin/users', label: '用户管理', icon: 'users' },
  { path: '/admin/batteries', label: '档案审核', icon: 'battery' },
  { path: '/admin/products', label: '商品审核', icon: 'products' },
  { path: '/admin/orders', label: '订单管理', icon: 'orders' },
  { path: '/admin/contracts', label: '合同存证', icon: 'contracts' },
  { path: '/admin/statistics', label: '运营统计', icon: 'statistics' }
]

const go = (path) => {
  router.push(path)
}

onMounted(async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res?.data?.unreadCount || 0
  } catch {
    unreadCount.value = 0
  }
})
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 79% 8%, rgba(67, 132, 255, 0.12), transparent 22%),
    linear-gradient(180deg, #f8fbff 0%, #eef5ff 100%);
  color: #071331;
}

.admin-aside {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow: hidden;
  background:
    radial-gradient(circle at 68% 12%, rgba(39, 115, 255, 0.2), transparent 24%),
    linear-gradient(180deg, #101b28 0%, #06111a 100%);
  color: #f8fbff;
  box-shadow: 16px 0 42px rgba(4, 15, 30, 0.08);
}

.admin-aside::after {
  position: absolute;
  inset: 0;
  pointer-events: none;
  content: "";
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.035), transparent 36%);
}

.admin-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 27px 24px 24px;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.admin-brand p {
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.74);
  font-size: 12px;
  letter-spacing: 0;
}

.admin-brand h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  color: #ffffff;
}

.admin-menu {
  position: relative;
  z-index: 1;
  border-right: 0;
  background: transparent;
}

:deep(.admin-menu.el-menu--collapse) {
  width: 86px;
}

:deep(.admin-menu .el-menu-item) {
  height: 54px;
  margin: 7px 12px;
  border-radius: 10px;
  color: rgba(255, 255, 255, 0.86);
  font-size: 15px;
  font-weight: 700;
}

:deep(.admin-menu .el-menu-item .nav-icon) {
  width: 30px;
  height: 30px;
  margin-right: 14px;
}

:deep(.admin-menu.el-menu--collapse .el-menu-item .nav-icon) {
  width: 34px !important;
  min-width: 34px;
  height: 34px !important;
  flex: 0 0 34px;
  margin-right: 0;
}

:deep(.admin-menu.el-menu--collapse .el-menu-item) {
  justify-content: center;
  padding: 0;
}

:deep(.admin-menu.el-menu--collapse .el-menu-item .nav-icon svg) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.admin-menu .el-menu-item.is-active) {
  color: #ffffff;
  background: linear-gradient(135deg, #1454b8 0%, #163f79 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08), 0 12px 28px rgba(0, 86, 255, 0.22);
}

.admin-menu-extra {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 8px;
  margin: 18px 24px 0;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
}

.extra-item {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  height: 44px;
  padding: 0 4px;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.extra-item :deep(.nav-icon) {
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
}

.extra-item.active {
  border-radius: 10px;
  background: rgba(20, 84, 184, 0.38);
  color: #ffffff;
}

.extra-item .el-icon {
  margin-left: auto;
  font-size: 14px;
}

.extra-item b {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  margin-left: auto;
  border-radius: 50%;
  background: #0f74ff;
  color: #ffffff;
  font-size: 12px;
}

.admin-main-shell {
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 76px;
  padding: 0 22px 0 38px;
  border-bottom: 0;
  background: rgba(248, 251, 255, 0.82);
  backdrop-filter: blur(16px);
}

.header-left,
.header-right {
  display: flex;
  align-items: center;
}

.header-left {
  gap: 13px;
  color: #62718d;
  font-size: 14px;
}

.header-right {
  gap: 12px;
}

.header-button {
  height: auto;
  padding: 0;
  color: #0f74ff;
  font-size: 14px;
  font-weight: 700;
}

.breadcrumb-sep {
  color: #b7c1d2;
}

.breadcrumb-muted {
  color: #7a879e;
}

.header-title {
  color: #071331;
  font-size: 17px;
  font-weight: 800;
}

.admin-content {
  min-width: 0;
  padding: 18px 22px 14px 26px;
}

@media (max-width: 1180px) {
  .admin-aside {
    display: none;
  }

  .admin-header {
    padding-inline: 18px;
  }

  .admin-content {
    padding-inline: 16px;
  }
}

@media (max-width: 720px) {
  .admin-header {
    height: auto;
    min-height: 70px;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
    padding-block: 14px;
  }

  .header-left {
    flex-wrap: wrap;
  }
}
</style>
