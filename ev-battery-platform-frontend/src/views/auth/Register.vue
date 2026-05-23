<template>
  <main class="auth-showcase auth-showcase--register">
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

    <section class="auth-panel auth-panel--register" aria-label="注册表单">
      <div class="panel-heading">
        <h2>欢迎注册</h2>
        <p><span>EV-BatterySecondLife</span> 平台</p>
      </div>

      <el-form :model="form" class="auth-form" @keyup.enter="submit">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="手机号 / 账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.email" size="large" placeholder="邮箱" :prefix-icon="Message" />
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
        <el-form-item>
          <el-input
            v-model="form.confirmPassword"
            size="large"
            type="password"
            show-password
            placeholder="确认密码"
            :prefix-icon="Lock"
          />
        </el-form-item>

        <el-button type="primary" class="submit-btn" :loading="loading" @click="submit">注册</el-button>

        <div class="auth-divider"><span>或</span></div>

        <el-button class="secondary-btn" @click="$router.push('/login')">
          <el-icon><ArrowLeft /></el-icon>
          返回登录
        </el-button>
        <el-button link type="primary" class="visitor-btn" @click="handleVisitor">
          游客访问
          <el-icon><ArrowRight /></el-icon>
        </el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight, Box, Document, Lock, Message, TrendCharts, User, Van } from '@element-plus/icons-vue'
import { register } from '../../api/user'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })

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

const submit = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }

  loading.value = true
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
  } finally {
    loading.value = false
  }
}
</script>

<style src="./auth-page.css"></style>
