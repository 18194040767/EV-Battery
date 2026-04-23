<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <p class="kicker">Enroll</p>
      <h2>用户注册</h2>

      <el-form :model="form" label-position="top" class="auth-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="确认">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" @click="submit">注册</el-button>
        <el-button link @click="$router.push('/login')">返回登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../../api/user'

const router = useRouter()
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写账号')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次不一致')
    return
  }

  try {
    const res = await register(form)
    if (res?.code !== 200) {
      ElMessage.error(res?.message || '注册失败')
      return
    }
    ElMessage.success('注册成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '注册失败')
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
  width: min(500px, 100%);
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
