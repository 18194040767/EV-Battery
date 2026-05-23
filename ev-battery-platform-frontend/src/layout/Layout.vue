<template>
  <el-container class="layout-shell">
    <el-drawer v-model="mobileMenuVisible" :with-header="false" direction="ltr" size="256px" class="mobile-nav">
      <aside class="drawer-aside">
        <button class="brand-block" type="button" @click="goHome" aria-label="返回首页">
          <span class="css-logo" aria-hidden="true"><img src="/app-logo.png" alt="" /></span>
          <span class="brand-copy">
            <strong>EV-BatterySecondLife</strong>
            <small>退役动力电池二手交易与健康评估平台</small>
          </span>
        </button>
        <el-menu :default-active="$route.path" router class="nav-menu mobile-menu">
          <el-menu-item v-for="item in frontMenus" :key="item.path" :index="item.path">
            <AppNavIcon :name="item.icon" />
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </el-menu>
      </aside>
    </el-drawer>

    <el-aside :width="collapsed ? '76px' : '300px'" class="layout-aside">
      <button class="brand-block" :class="{ compact: collapsed }" type="button" @click="goHome" aria-label="返回首页">
        <span class="css-logo" aria-hidden="true"><img src="/app-logo.png" alt="" /></span>
        <span v-if="!collapsed" class="brand-copy">
          <strong>EV-BatterySecondLife</strong>
          <small>退役动力电池二手交易与健康评估平台</small>
        </span>
      </button>
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
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import AppNavIcon from '../components/AppNavIcon.vue'
import MessageCenter from '../components/MessageCenter.vue'
import UserDropdown from '../components/UserDropdown.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mobileMenuVisible = ref(false)
const isMobile = ref(false)

const frontMenus = [
  { path: '/home', label: '首页', icon: 'home' },
  { path: '/battery/list', label: '电池档案', icon: 'battery' },
  { path: '/assessment', label: '健康评估', icon: 'assessment' },
  { path: '/trade/product-list', label: '商品市场', icon: 'market' },
  { path: '/contract/verify', label: '合同查验', icon: 'contracts' },
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

const goHome = () => {
  mobileMenuVisible.value = false
  router.push('/home')
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
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  color: #101a33;
  border-right: 1px solid #e4ebf5;
  box-shadow: 8px 0 26px rgba(37, 88, 170, 0.04);
}

.layout-aside {
  overflow: hidden;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 26px 18px 28px;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.brand-block.compact {
  justify-content: center;
  padding-inline: 12px;
}

.css-logo {
  position: relative;
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  border-radius: 16px;
  background: transparent;
  overflow: hidden;
}

.css-logo img {
  width: 146%;
  height: 146%;
  display: block;
  object-fit: cover;
}

.brand-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.brand-copy strong {
  color: #071331;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.12;
}

.brand-copy small {
  color: #52627d;
  font-size: 11px;
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
  height: 56px;
  margin: 2px 14px 10px;
  border-radius: 12px;
  color: #1f2a44;
  font-size: 16px;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

:deep(.nav-menu .nav-icon) {
  width: 30px;
  height: 30px;
  margin-right: 16px;
  color: #64748b;
}

:deep(.nav-menu.el-menu--collapse .nav-icon) {
  width: 34px !important;
  min-width: 34px;
  height: 34px !important;
  flex: 0 0 34px;
  margin-right: 0;
}

:deep(.nav-menu.el-menu--collapse .nav-icon svg) {
  width: 100% !important;
  height: 100% !important;
}

:deep(.nav-menu.el-menu--collapse .el-menu-item) {
  justify-content: center;
  padding: 0;
}

:deep(.nav-menu .el-menu-item .el-tooltip__trigger) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
}

:deep(.nav-menu .el-menu-item.is-active) {
  color: #1677ff;
  background: linear-gradient(90deg, #eaf2ff 0%, #f1f6ff 100%);
  box-shadow: inset 0 0 0 1px rgba(31, 117, 255, 0.02);
}

:deep(.nav-menu .el-menu-item.is-active .nav-icon),
:deep(.nav-menu .el-menu-item:hover .nav-icon) {
  color: #1677ff;
}

:deep(.nav-menu .el-menu-item:hover) {
  color: #1677ff;
  background: #f3f7ff;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 70px;
  padding: 0 28px;
  border-bottom: 1px solid #e7edf6;
  background: rgba(255, 255, 255, 0.88);
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
  padding: 24px 24px 18px;
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

