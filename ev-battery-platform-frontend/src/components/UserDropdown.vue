<template>
  <el-dropdown trigger="click">
    <div class="dropdown-trigger">
      <el-avatar :size="46" :src="avatarSrc" class="avatar" />
      <div class="user-copy">
        <strong>{{ displayName }}</strong>
        <span>{{ roleLabel }}</span>
      </div>
      <el-icon class="chevron"><ArrowDown /></el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <template v-if="mode === 'front' && !userStore.isGuest">
          <el-dropdown-item @click="go('/trade/user')">我的主页</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/cart')">我的购物车</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/order-list')">我的订单</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/favorites')">我的收藏</el-dropdown-item>
          <el-dropdown-item @click="go('/contract/list')">我的合同</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/user')">账号设置</el-dropdown-item>
        </template>
        <template v-else-if="mode === 'front'">
          <el-dropdown-item @click="logout">登录 / 注册</el-dropdown-item>
        </template>
        <template v-else>
          <el-dropdown-item @click="go('/admin/dashboard')">数据驾驶舱</el-dropdown-item>
          <el-dropdown-item @click="go('/admin/users')">账号管理</el-dropdown-item>
        </template>
        <el-dropdown-item v-if="!userStore.isGuest" divided @click="logout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../store/user'

const props = defineProps({
  mode: {
    type: String,
    default: 'front'
  }
})

const router = useRouter()
const userStore = useUserStore()

const displayName = computed(() => {
  if (userStore.isGuest) return '游客'
  return props.mode === 'admin' ? userStore.displayName || '平台管理员' : userStore.displayName || '平台用户'
})
const roleLabel = computed(() => {
  if (userStore.isGuest) return '游客模式'
  return props.mode === 'admin' ? '管理员账号' : '平台用户'
})
const avatarSrc = computed(() => userStore.userInfo?.avatar || '/default-avatar.png')

const go = (path) => router.push(path)

const logout = () => {
  userStore.clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 11px;
  min-width: 176px;
  height: 56px;
  padding: 5px 14px 5px 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 8px 24px rgba(47, 91, 160, 0.09);
  cursor: pointer;
}

.avatar {
  border: 5px solid #e9f2ff;
  background: #eef4ff;
}

.avatar :deep(img) {
  object-fit: cover;
}

.user-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.25;
}

.user-copy strong {
  color: #071331;
  font-size: 14px;
  font-weight: 900;
}

.user-copy span {
  margin-top: 3px;
  color: #66758f;
  font-size: 12px;
  font-weight: 700;
}

.chevron {
  margin-left: auto;
  color: #6d7c96;
}

@media (max-width: 768px) {
  .dropdown-trigger {
    min-width: 0;
  }

  .user-copy,
  .chevron {
    display: none;
  }
}
</style>
