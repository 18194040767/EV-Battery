<template>
  <div class="admin-feature-page">
    <section class="feature-head">
      <div>
        <p>Messages</p>
        <h2>消息通知</h2>
        <span>查看系统公告、订单提醒和安全通知，可单条或批量标记已读。</span>
      </div>
      <el-button type="primary" :disabled="!messages.length" @click="readAll">全部已读</el-button>
    </section>

    <section class="message-panel">
      <el-empty v-if="!messages.length && !loading" description="暂无消息" />
      <article v-for="item in messages" :key="item.id" class="message-row" :class="{ unread: !item.readFlag }">
        <span class="message-dot"></span>
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.content }}</p>
          <time>{{ formatTime(item.createdAt) }}</time>
        </div>
        <el-button link type="primary" :disabled="item.readFlag" @click="readOne(item)">标记已读</el-button>
      </article>
    </section>
  </div>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMessageList, markAllMessageRead, markMessageRead } from '../../api/message'

const loading = ref(false)
const messages = ref([])

const loadMessages = async () => {
  loading.value = true
  try {
    const res = await getMessageList({ page: 1, size: 50 })
    messages.value = res?.data?.records || []
  } catch {
    messages.value = []
    ElMessage.error('消息加载失败')
  } finally {
    loading.value = false
  }
}

const readOne = async (item) => {
  await markMessageRead(item.id)
  item.readFlag = true
}

const readAll = async () => {
  await markAllMessageRead()
  messages.value = messages.value.map((item) => ({ ...item, readFlag: true }))
  ElMessage.success('消息已全部标记为已读')
}

const formatTime = (value) => (value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-')

onMounted(loadMessages)
</script>

<style scoped>
.admin-feature-page {
  display: grid;
  gap: 18px;
}

.feature-head,
.message-panel {
  border: 1px solid rgba(54, 94, 150, 0.1);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(50, 86, 150, 0.08);
}

.feature-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
}

.feature-head p {
  margin: 0 0 8px;
  color: #126cff;
  font-size: 12px;
  font-weight: 800;
}

.feature-head h2 {
  margin: 0;
  color: #071331;
  font-size: 24px;
}

.feature-head span {
  display: block;
  margin-top: 8px;
  color: #66758f;
}

.message-panel {
  padding: 8px 22px;
}

.message-row {
  display: grid;
  grid-template-columns: 12px 1fr auto;
  gap: 16px;
  align-items: center;
  padding: 20px 0;
  border-bottom: 1px solid #edf2f8;
}

.message-row:last-child {
  border-bottom: 0;
}

.message-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c8d4e6;
}

.message-row.unread .message-dot {
  background: #126cff;
}

.message-row strong {
  color: #071331;
}

.message-row p {
  margin: 6px 0;
  color: #4f5f7a;
}

.message-row time {
  color: #8a97ad;
  font-size: 12px;
}
</style>
