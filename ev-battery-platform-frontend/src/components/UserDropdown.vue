<template>
  <el-dropdown trigger="click">
    <div class="dropdown-trigger">
      <el-avatar :size="38" class="avatar">{{ initials }}</el-avatar>
      <div class="user-copy">
        <strong>{{ userStore.displayName }}</strong>
        <span>{{ mode === 'admin' ? '管理员账号' : '平台用户' }}</span>
      </div>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <template v-if="mode === 'front'">
          <el-dropdown-item @click="go('/trade/user')">我的主页</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/cart')">我的购物车</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/order-list')">我的订单</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/favorites')">我的收藏</el-dropdown-item>
          <el-dropdown-item @click="go('/contract/list')">我的合同</el-dropdown-item>
          <el-dropdown-item @click="go('/trade/user')">账号设置</el-dropdown-item>
        </template>
        <template v-else>
          <el-dropdown-item @click="go('/admin/dashboard')">数据驾驶舱</el-dropdown-item>
          <el-dropdown-item @click="go('/admin/users')">账号管理</el-dropdown-item>
        </template>
        <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

const props = defineProps({
  mode: {
    type: String,
    default: 'front'
  }
})

const router = useRouter()
const userStore = useUserStore()

const initials = computed(() => (userStore.displayName || 'U').slice(0, 1).toUpperCase())

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
  gap: 12px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
}

.avatar {
  background: linear-gradient(135deg, var(--app-primary), var(--app-accent));
  color: #fff;
}

.user-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-copy strong {
  font-size: 14px;
}

.user-copy span {
  font-size: 12px;
  color: var(--app-muted);
}

@media (max-width: 768px) {
  .user-copy {
    display: none;
  }
}
</style>
