<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <p class="kicker">Access</p>
      <h2>账号登录</h2>

      <el-form :model="form" label-position="top" class="auth-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="handleLogin">登录</el-button>
        <el-button link @click="$router.push('/register')">注册账号</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCurrentUser, login } from '../../api/user'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号')
    return
  }

  try {
    const res = await login(form)
    const token = res?.data?.token || ''
    if (!token) {
      ElMessage.error(res?.message || '登录失败')
      return
    }
    userStore.setToken(token)
    const current = await getCurrentUser()
    userStore.setUserInfo(current?.data || null)
    ElMessage.success('登录成功')
    router.push(userStore.isAdmin ? '/admin/dashboard' : '/home')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-card {
  width: min(460px, 100%);
  border-radius: 28px;
  padding: 12px;
}

.kicker {
  margin: 0 0 10px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.auth-card h2 {
  margin: 0;
}

.auth-form {
  margin-top: 24px;
}

.submit-btn {
  width: 100%;
  margin-bottom: 10px;
}
</style>
