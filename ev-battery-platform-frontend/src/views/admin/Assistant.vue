<template>
  <div class="assistant-page">
    <section class="assistant-panel">
      <div class="assistant-head">
        <div>
          <p>AI Assistant</p>
          <h2>后台 AI 助手</h2>
          <span>复用用户端 AI 接口，可咨询订单、审核、合同、物流与运营数据问题。</span>
        </div>
        <el-button @click="clearThread">清空会话</el-button>
      </div>

      <div class="quick-prompts">
        <button v-for="prompt in quickPrompts" :key="prompt" type="button" @click="usePrompt(prompt)">
          {{ prompt }}
        </button>
      </div>

      <div class="thread">
        <article v-for="(item, index) in messages" :key="`${item.role}-${index}`" class="bubble" :class="item.role">
          <p>{{ item.content }}</p>
        </article>
        <article v-if="loading" class="bubble assistant">
          <p>正在思考...</p>
        </article>
      </div>

      <div class="composer">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="输入你的后台管理问题，Ctrl + Enter 发送"
          @keyup.ctrl.enter="submit"
        />
        <el-button type="primary" :loading="loading" @click="submit">发送</el-button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { streamAssistantReply } from '../../api/assistant'

const loading = ref(false)
const input = ref('')
const messages = ref([
  { role: 'assistant', content: '你好，我是后台 AI 助手。你可以问我订单处理、审核流程、合同存证、物流追踪或运营统计相关问题。' }
])

const quickPrompts = ['今天有哪些订单需要处理？', '商品审核要注意什么？', '如何查看合同存证状态？', '帮我解释健康分布数据']

const clearThread = () => {
  messages.value = [{ role: 'assistant', content: '会话已清空。继续告诉我你想处理的后台问题。' }]
}

const usePrompt = (prompt) => {
  input.value = prompt
  submit()
}

const submit = async () => {
  const question = input.value.trim()
  if (!question || loading.value) return

  messages.value.push({ role: 'user', content: question })
  const assistantMessage = { role: 'assistant', content: '' }
  messages.value.push(assistantMessage)
  input.value = ''
  loading.value = true

  try {
    await streamAssistantReply({
      question,
      history: messages.value.slice(-8).map((item) => ({ role: item.role, content: item.content })),
      onChunk: (chunk) => {
        assistantMessage.content += chunk
      },
      onDone: () => {
        if (!assistantMessage.content) assistantMessage.content = '暂时没有拿到回复，请稍后再试。'
      },
      onError: (message) => {
        assistantMessage.content = message || 'AI 助手暂时不可用'
      }
    })
  } catch {
    assistantMessage.content = 'AI 助手暂时不可用，请稍后再试。'
    ElMessage.error('AI 助手暂时不可用')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.assistant-page {
  display: grid;
}

.assistant-panel {
  display: grid;
  grid-template-rows: auto auto minmax(300px, 1fr) auto;
  min-height: calc(100vh - 112px);
  border: 1px solid rgba(54, 94, 150, 0.1);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(50, 86, 150, 0.08);
  overflow: hidden;
}

.assistant-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px;
  border-bottom: 1px solid #edf2f8;
}

.assistant-head p {
  margin: 0 0 8px;
  color: #126cff;
  font-size: 12px;
  font-weight: 800;
}

.assistant-head h2 {
  margin: 0;
  color: #071331;
}

.assistant-head span {
  display: block;
  margin-top: 8px;
  color: #66758f;
}

.quick-prompts {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  padding: 16px 24px 0;
}

.quick-prompts button {
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(18, 108, 255, 0.18);
  border-radius: 999px;
  background: #f3f7ff;
  color: #1b56aa;
  font-weight: 700;
  cursor: pointer;
}

.thread {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 24px;
  overflow: auto;
}

.bubble {
  max-width: 76%;
  padding: 13px 15px;
  border-radius: 16px;
}

.bubble p {
  margin: 0;
  line-height: 1.7;
  white-space: pre-wrap;
}

.bubble.assistant {
  align-self: flex-start;
  background: #f3f7ff;
  color: #23314f;
}

.bubble.user {
  align-self: flex-end;
  background: #126cff;
  color: #ffffff;
}

.composer {
  display: grid;
  grid-template-columns: 1fr 110px;
  gap: 14px;
  padding: 18px 24px 22px;
  border-top: 1px solid #edf2f8;
}

@media (max-width: 780px) {
  .composer {
    grid-template-columns: 1fr;
  }

  .bubble {
    max-width: 92%;
  }
}
</style>
