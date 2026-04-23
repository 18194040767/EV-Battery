<template>
  <el-popover placement="bottom-end" :width="380" trigger="click" @show="loadMessages">
    <template #reference>
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="message-badge">
        <el-button text class="message-btn">消息</el-button>
      </el-badge>
    </template>
    <div class="message-header">
      <strong>消息中心</strong>
      <el-button link type="primary" @click="readAll">全部已读</el-button>
    </div>
    <el-empty v-if="!messages.length" description="暂无消息" :image-size="80" />
    <div v-else class="message-list">
      <article v-for="item in messages" :key="item.id" class="message-item" :class="{ unread: !item.readFlag }">
        <div class="title-row">
          <strong>{{ item.title }}</strong>
          <span>{{ formatTime(item.createdAt) }}</span>
        </div>
        <p>{{ item.content }}</p>
        <div class="actions">
          <el-button link type="primary" @click="openDetail(item)">查看</el-button>
          <el-button link :disabled="item.readFlag" @click="readOne(item)">标记已读</el-button>
        </div>
      </article>
    </div>
  </el-popover>
</template>

<script setup>
import dayjs from 'dayjs'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMessageList, getUnreadCount, markAllMessageRead, markMessageRead } from '../api/message'

const router = useRouter()
const unreadCount = ref(0)
const messages = ref([])

const refreshUnread = async () => {
  const res = await getUnreadCount()
  unreadCount.value = res?.data?.unreadCount || 0
}

const loadMessages = async () => {
  const res = await getMessageList({ page: 1, size: 10 })
  messages.value = res?.data?.records || []
  await refreshUnread()
}

const readOne = async (item) => {
  await markMessageRead(item.id)
  item.readFlag = true
  await refreshUnread()
}

const readAll = async () => {
  await markAllMessageRead()
  messages.value = messages.value.map((item) => ({ ...item, readFlag: true }))
  await refreshUnread()
}

const openDetail = async (item) => {
  if (!item.readFlag) {
    await readOne(item)
  }
  if (item.relatedType === 'ASSESSMENT' && item.relatedId) {
    await router.push(`/assessment?assessmentId=${item.relatedId}`)
    ElMessage.success('已打开对应评估记录')
  }
}

const formatTime = (value) => (value ? dayjs(value).format('MM-DD HH:mm') : '-')

onMounted(refreshUnread)
</script>

<style scoped>
.message-badge {
  margin-right: 8px;
}
.message-btn {
  color: var(--app-primary);
}
.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.message-list {
  max-height: 420px;
  overflow: auto;
}
.message-item {
  padding: 10px 4px;
  border-bottom: 1px solid var(--app-border);
}
.message-item.unread strong {
  color: #1d4ed8;
}
.title-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}
.title-row span {
  color: #94a3b8;
  font-size: 12px;
}
.message-item p {
  margin: 8px 0;
  color: #334155;
  line-height: 1.5;
}
.actions {
  display: flex;
  justify-content: flex-end;
}
</style>
