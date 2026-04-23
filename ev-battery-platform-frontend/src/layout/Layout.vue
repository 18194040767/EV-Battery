<template>
  <el-container class="layout-shell">
    <el-drawer v-model="mobileMenuVisible" :with-header="false" direction="ltr" size="256px" class="mobile-nav">
      <aside class="drawer-aside">
        <div class="brand-block">
          <p class="brand-kicker">EV Battery Platform</p>
          <h1>梯次利用交易平台</h1>
        </div>
        <el-menu :default-active="$route.path" router class="nav-menu mobile-menu">
          <el-menu-item v-for="item in frontMenus" :key="item.path" :index="item.path">
            <AppNavIcon :name="item.icon" />
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </el-menu>
      </aside>
    </el-drawer>

    <el-aside :width="collapsed ? '76px' : '236px'" class="layout-aside">
      <div class="brand-block" :class="{ compact: collapsed }">
        <p class="brand-kicker">{{ collapsed ? 'EV' : 'EV Battery Platform' }}</p>
        <h1>{{ collapsed ? '平台' : '梯次利用交易平台' }}</h1>
      </div>
      <el-menu :default-active="$route.path" router :collapse="collapsed" :collapse-transition="false" class="nav-menu">
        <el-menu-item v-for="item in frontMenus" :key="item.path" :index="item.path">
          <AppNavIcon :name="item.icon" />
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-button text class="header-button" @click="handleMenuAction">
            {{ isMobile ? '菜单' : collapsed ? '展开菜单' : '收起菜单' }}
          </el-button>
          <div>
            <p class="header-label">{{ pageLabel }}</p>
            <strong class="header-title">电池流转与交易</strong>
          </div>
        </div>
        <div class="header-right">
          <MessageCenter />
          <UserDropdown mode="front" />
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import AppNavIcon from '../components/AppNavIcon.vue'
import MessageCenter from '../components/MessageCenter.vue'
import UserDropdown from '../components/UserDropdown.vue'

const route = useRoute()
const userStore = useUserStore()
const mobileMenuVisible = ref(false)
const isMobile = ref(false)

const frontMenus = [
  { path: '/home', label: '首页', icon: 'home' },
  { path: '/battery/list', label: '电池档案', icon: 'battery' },
  { path: '/assessment', label: '健康评估', icon: 'assessment' },
  { path: '/trade/product-list', label: '商品市场', icon: 'market' },
  { path: '/logistics/list', label: '物流追踪', icon: 'logistics' }
]

const collapsed = computed(() => userStore.sidebarCollapsed)
const pageLabel = computed(() => route.meta?.title || '平台总览')

const syncScreen = () => {
  isMobile.value = window.innerWidth <= 992
}

const handleMenuAction = () => {
  if (isMobile.value) {
    mobileMenuVisible.value = true
  } else {
    userStore.toggleSidebar()
  }
}

onMounted(() => {
  syncScreen()
  window.addEventListener('resize', syncScreen)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncScreen)
})
</script>

<style scoped>
.layout-shell {
  min-height: 100vh;
}

.layout-aside,
.drawer-aside {
  background: linear-gradient(180deg, #123c39 0%, #0d2c2c 100%);
  color: #f4fffe;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.layout-aside {
  overflow: hidden;
}

.brand-block {
  padding: 28px 18px 18px;
}

.brand-block.compact {
  padding-inline: 14px;
}

.brand-kicker {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.brand-block h1 {
  margin: 0;
  font-size: 20px;
  line-height: 1.35;
}

.nav-menu {
  border-right: none;
  background: transparent;
}

:deep(.nav-menu.el-menu--collapse) {
  width: 76px;
}

:deep(.nav-menu .el-menu-item) {
  color: rgba(255, 255, 255, 0.86);
  border-radius: 14px;
  margin: 6px 10px;
}

:deep(.nav-menu .el-menu-item .el-tooltip__trigger) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

:deep(.nav-menu .el-menu-item.is-active) {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.14);
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 28px;
  border-bottom: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(18px);
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
  color: var(--app-muted);
}

.header-title {
  font-size: 18px;
  color: var(--app-text);
}

.header-button {
  color: var(--app-primary);
}

.layout-main {
  padding: 24px;
}

@media (max-width: 992px) {
  .layout-aside {
    display: none;
  }

  .layout-header {
    padding-inline: 18px;
  }

  .layout-main {
    padding: 16px;
  }
}
</style>

