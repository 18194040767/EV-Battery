<template>
  <main class="auth-showcase auth-showcase--login">
    <section class="auth-hero" aria-label="平台介绍">
      <h1><span>EV-</span>BatterySecondLife</h1>
      <h2>退役动力电池二手交易与健康评估平台</h2>
      <p>让每一块电池，延续价值 · 驱动绿色未来</p>

      <div class="feature-pills" aria-label="核心能力">
        <span v-for="item in features" :key="item.label">
          <component :is="item.icon" />
          {{ item.label }}
        </span>
      </div>
    </section>

    <section class="auth-panel" aria-label="登录表单">
      <div class="panel-heading">
        <h2>欢迎登录</h2>
        <p><span>EV-BatterySecondLife</span> 平台</p>
      </div>

      <el-form :model="form" class="auth-form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="手机号 / 账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            placeholder="密码"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <div class="form-row">
          <el-checkbox v-model="remember">记住我</el-checkbox>
          <el-button link type="primary" @click="openForgotPassword">忘记密码?</el-button>
        </div>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">登录</el-button>

        <div class="auth-divider"><span>或</span></div>

        <el-button class="secondary-btn" @click="$router.push('/register')">
          <el-icon><UserFilled /></el-icon>
          立即注册
        </el-button>
        <el-button link type="primary" class="visitor-btn" @click="handleVisitor">
          游客访问
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form>
    </section>
  </main>

  <el-dialog
    v-model="forgotVisible"
    class="forgot-password-dialog"
    title="找回密码"
    width="460px"
    align-center
    @closed="resetForgotForm"
  >
    <el-form :model="forgotForm" label-position="top" class="forgot-password-form" @keyup.enter="handleForgotEnter">
      <el-form-item label="账号">
        <el-input v-model.trim="forgotForm.username" placeholder="请输入账号" :prefix-icon="User" />
      </el-form-item>
      <el-form-item label="注册邮箱">
        <el-input v-model.trim="forgotForm.email" placeholder="请输入注册邮箱" :prefix-icon="Message" />
      </el-form-item>

      <el-alert
        v-if="mockCode"
        class="mock-code-alert"
        type="success"
        :closable="false"
        show-icon
      >
        <template #title>
          模拟验证码：<strong>{{ mockCode }}</strong>，请在 10 分钟内使用
        </template>
      </el-alert>

      <el-form-item label="验证码">
        <el-input v-model.trim="forgotForm.code" maxlength="6" placeholder="请输入 6 位验证码" :prefix-icon="Key" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input
          v-model="forgotForm.newPassword"
          type="password"
          show-password
          placeholder="至少 6 位"
          :prefix-icon="Lock"
        />
      </el-form-item>
      <el-form-item label="确认新密码">
        <el-input
          v-model="forgotForm.confirmPassword"
          type="password"
          show-password
          placeholder="请再次输入新密码"
          :prefix-icon="Lock"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="forgot-password-actions">
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button :loading="codeLoading" @click="handleRequestCode">获取验证码</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleResetPassword">重置密码</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Box,
  Document,
  Key,
  Lock,
  Message,
  TrendCharts,
  User,
  UserFilled,
  Van
} from '@element-plus/icons-vue'
import { getCurrentUser, login, requestForgotPasswordCode, resetForgotPassword } from '../../api/user'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const remember = ref(false)
const forgotVisible = ref(false)
const codeLoading = ref(false)
const resetLoading = ref(false)
const mockCode = ref('')
const form = reactive({ username: '', password: '' })
const forgotForm = reactive({
  username: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const features = [
  { label: 'SOH评估', icon: Box },
  { label: '二手交易', icon: TrendCharts },
  { label: '电子合同', icon: Document },
  { label: '物流追踪', icon: Van }
]

const handleVisitor = () => {
  userStore.enterGuestMode()
  router.push('/home')
}

const openForgotPassword = () => {
  forgotForm.username = form.username || ''
  forgotVisible.value = true
}

const resetForgotForm = () => {
  Object.assign(forgotForm, {
    username: '',
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
  })
  mockCode.value = ''
  codeLoading.value = false
  resetLoading.value = false
}

const validateAccountAndEmail = () => {
  if (!forgotForm.username || !forgotForm.email) {
    ElMessage.warning('请输入账号和注册邮箱')
    return false
  }
  return true
}

const handleRequestCode = async () => {
  if (!validateAccountAndEmail()) return
  codeLoading.value = true
  try {
    const res = await requestForgotPasswordCode({
      username: forgotForm.username,
      email: forgotForm.email
    })
    if (res?.code !== 200) {
      ElMessage.warning(res?.message || '验证码获取失败')
      return
    }
    mockCode.value = res?.data?.code || ''
    ElMessage.success('验证码已生成')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '验证码获取失败')
  } finally {
    codeLoading.value = false
  }
}

const handleResetPassword = async () => {
  if (!validateAccountAndEmail()) return
  if (!forgotForm.code) {
    ElMessage.warning('请输入验证码')
    return
  }
  if (!forgotForm.newPassword || forgotForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (forgotForm.newPassword !== forgotForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  resetLoading.value = true
  try {
    const res = await resetForgotPassword({
      username: forgotForm.username,
      email: forgotForm.email,
      code: forgotForm.code,
      newPassword: forgotForm.newPassword
    })
    if (res?.code !== 200) {
      ElMessage.warning(res?.message || '密码重置失败')
      return
    }
    form.username = forgotForm.username
    form.password = ''
    forgotVisible.value = false
    ElMessage.success('密码已重置，请使用新密码登录')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '密码重置失败')
  } finally {
    resetLoading.value = false
  }
}

const handleForgotEnter = () => {
  if (mockCode.value) {
    handleResetPassword()
  } else {
    handleRequestCode()
  }
}

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  loading.value = true
  try {
    const res = await login(form)
    const loginUser = res?.data || {}
    const token = loginUser.token || ''
    if (!token) {
      ElMessage.error(res?.message || '登录失败')
      return
    }
    userStore.setToken(token)
    userStore.setUserInfo({
      id: loginUser.userId,
      username: loginUser.username || form.username,
      realName: loginUser.username || form.username,
      roles: loginUser.roles || []
    })

    try {
      const current = await getCurrentUser()
      userStore.setUserInfo(current?.data || userStore.userInfo)
    } catch {
      // 登录接口已经返回路由所需的角色信息，current 失败时不打断登录。
    }

    ElMessage.success('登录成功')
    router.push(userStore.isAdmin ? '/admin/dashboard' : '/home')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style src="./auth-page.css"></style>

<style scoped>
.forgot-password-form {
  display: grid;
  gap: 2px;
}

.mock-code-alert {
  margin: 0 0 16px;
}

.forgot-password-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.forgot-password-actions .el-button {
  margin-left: 0;
}

@media (max-width: 560px) {
  .forgot-password-actions {
    justify-content: stretch;
  }

  .forgot-password-actions .el-button {
    flex: 1 1 100%;
  }
}
</style>
