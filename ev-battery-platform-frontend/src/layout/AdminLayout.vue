<template>
  <el-container class="layout-shell admin-shell">
    <el-aside :width="collapsed ? '80px' : '248px'" class="admin-aside">
      <div class="admin-brand" :class="{ compact: collapsed }">
        <p>{{ collapsed ? 'ADM' : 'System Console' }}</p>
        <h1>{{ collapsed ? '后台' : '后台管理中心' }}</h1>
      </div>
      <el-menu :default-active="$route.path" router :collapse="collapsed" :collapse-transition="false" class="admin-menu">
        <el-menu-item v-for="item in adminMenus" :key="item.path" :index="item.path">
          <AppNavIcon :name="item.icon" />
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div class="header-left">
          <el-button text class="header-button" @click="userStore.toggleSidebar()">{{ collapsed ? '展开菜单' : '收起菜单' }}</el-button>
          <div>
            <p class="header-label">管理后台</p>
            <strong class="header-title">{{ $route.meta?.title || '系统管理' }}</strong>
          </div>
        </div>
        <div class="header-right">
          <MessageCenter />
          <UserDropdown mode="admin" />
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '../store/user'
import AppNavIcon from '../components/AppNavIcon.vue'
import MessageCenter from '../components/MessageCenter.vue'
import UserDropdown from '../components/UserDropdown.vue'

const userStore = useUserStore()
const collapsed = computed(() => userStore.sidebarCollapsed)

const adminMenus = [
  { path: '/admin/dashboard', label: '数据驾驶舱', icon: 'dashboard' },
  { path: '/admin/users', label: '用户管理', icon: 'users' },
  { path: '/admin/batteries', label: '档案审核', icon: 'battery' },
  { path: '/admin/products', label: '商品审核', icon: 'products' },
  { path: '/admin/orders', label: '订单管理', icon: 'orders' },
  { path: '/admin/contracts', label: '合同存证', icon: 'contracts' },
  { path: '/admin/statistics', label: '运营统计', icon: 'statistics' }
]
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
}

.admin-aside {
  background: linear-gradient(180deg, #0f172a 0%, #12243f 100%);
  color: #f8fbff;
  overflow: hidden;
}

.admin-brand {
  padding: 28px 18px 18px;
}

.admin-brand p {
  margin: 0 0 8px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.64);
}

.admin-brand h1 {
  margin: 0;
  font-size: 20px;
}

.admin-menu {
  border-right: none;
  background: transparent;
}

:deep(.admin-menu.el-menu--collapse) {
  width: 80px;
}

:deep(.admin-menu .el-menu-item) {
  color: rgba(255, 255, 255, 0.84);
  border-radius: 14px;
  margin: 6px 10px;
}

:deep(.admin-menu .el-menu-item.is-active) {
  color: #fff;
  background: rgba(59, 130, 246, 0.22);
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(14px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-label {
  margin: 0;
  font-size: 12px;
  color: #64748b;
}

.header-title {
  font-size: 18px;
  color: #10233d;
}

.header-button {
  color: var(--app-accent);
}

.layout-main {
  padding: 24px;
}
</style>
