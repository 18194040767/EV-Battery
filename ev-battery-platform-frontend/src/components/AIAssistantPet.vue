<template>
  <div class="assistant-pet">
    <transition name="pet-panel">
      <section v-if="panelVisible" class="pet-panel">
        <div class="pet-panel-head">
          <div>
            <strong>AI 小助手</strong>
            <p>订单、物流、合同、评估都可以问我</p>
          </div>
          <el-button text @click="panelVisible = false">收起</el-button>
        </div>

        <div class="pet-quick">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            class="quick-chip"
            @click="usePrompt(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <div class="pet-thread">
          <article
            v-for="(item, index) in messages"
            :key="`${item.role}-${index}`"
            class="pet-bubble"
            :class="item.role"
          >
            <p>{{ item.content }}</p>
          </article>
          <article v-if="loading" class="pet-bubble assistant loading">
            <p>正在思考...</p>
          </article>
        </div>

        <div class="pet-input">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="问我一个问题"
            @keyup.ctrl.enter="submit"
          />
          <div class="pet-actions">
            <span>Ctrl + Enter 发送</span>
            <el-button type="primary" size="small" :loading="loading" @click="submit">发送</el-button>
          </div>
        </div>
      </section>
    </transition>

    <button class="pet-trigger" type="button" @click="togglePanel">
      <span class="pet-body" :class="{ active: panelVisible }">
        <span class="pet-eye left"></span>
        <span class="pet-eye right"></span>
        <span class="pet-cheek left"></span>
        <span class="pet-cheek right"></span>
        <span class="pet-mouth"></span>
      </span>
      <span class="pet-label">{{ panelVisible ? '在线陪伴中' : '点我提问' }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { streamAssistantReply } from '../api/assistant'

const panelVisible = ref(false)
const loading = ref(false)
const input = ref('')
const messages = ref([
  {
    role: 'assistant',
    content: '你好，我在这儿。你可以直接问我支付、物流、合同或评估问题。'
  }
])

const quickPrompts = ['怎么支付', '怎么查物流', '怎么查看合同']

const togglePanel = () => {
  panelVisible.value = !panelVisible.value
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
      history: messages.value.slice(-8).map((item) => ({
        role: item.role,
        content: item.content
      })),
      onChunk: (chunk) => {
        assistantMessage.content += chunk
      },
      onDone: () => {
        if (!assistantMessage.content) {
          assistantMessage.content = '我暂时没有拿到回复，请稍后再试。'
        }
      },
      onError: (message) => {
        assistantMessage.content = message || 'AI 小助手暂时不可用'
      }
    })
    panelVisible.value = true
  } catch (error) {
    assistantMessage.content = 'AI 小助手暂时不可用'
    ElMessage.error('AI 小助手暂时不可用')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.assistant-pet {
  position: fixed;
  right: 22px;
  bottom: 24px;
  z-index: 60;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.pet-panel {
  width: min(340px, calc(100vw - 28px));
  max-height: 480px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(29, 92, 87, 0.14);
  box-shadow: 0 28px 60px rgba(15, 23, 42, 0.18);
  backdrop-filter: blur(16px);
  overflow: hidden;
}

.pet-panel-head,
.pet-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.pet-panel-head {
  padding: 16px 18px 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.pet-panel-head strong {
  display: block;
  font-size: 15px;
}

.pet-panel-head p {
  margin: 4px 0 0;
  color: var(--app-muted);
  font-size: 12px;
}

.pet-quick {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding: 12px 18px 0;
}

.quick-chip {
  border: 1px solid rgba(29, 92, 87, 0.14);
  border-radius: 999px;
  background: #f7fbfa;
  color: #24534f;
  font-size: 12px;
  padding: 6px 10px;
  cursor: pointer;
}

.pet-thread {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 18px;
  max-height: 240px;
  overflow: auto;
}

.pet-bubble {
  max-width: 88%;
  padding: 12px 14px;
  border-radius: 18px;
}

.pet-bubble p {
  margin: 0;
  line-height: 1.6;
  font-size: 13px;
  white-space: pre-wrap;
}

.pet-bubble.assistant {
  align-self: flex-start;
  background: #f7fbfa;
}

.pet-bubble.user {
  align-self: flex-end;
  background: rgba(29, 92, 87, 0.1);
}

.pet-bubble.loading {
  opacity: 0.76;
}

.pet-input {
  padding: 0 18px 18px;
}

.pet-actions {
  margin-top: 10px;
  color: var(--app-muted);
  font-size: 12px;
}

.pet-trigger {
  position: relative;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.pet-body {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 28px 28px 24px 24px;
  background: linear-gradient(135deg, #ffb7d5 0%, #ff8fb8 55%, #ff7aa8 100%);
  box-shadow: 0 18px 34px rgba(15, 118, 110, 0.28);
  animation: floaty 2.8s ease-in-out infinite;
}

.pet-body.active {
  animation-duration: 1.8s;
}

.pet-body::before,
.pet-body::after {
  content: '';
  position: absolute;
  top: -8px;
  width: 18px;
  height: 18px;
  background: #ff9bc1;
  border-radius: 8px 8px 2px 8px;
}

.pet-body::before {
  left: 10px;
  transform: rotate(-22deg);
}

.pet-body::after {
  right: 10px;
  transform: scaleX(-1) rotate(-22deg);
}

.pet-eye,
.pet-cheek,
.pet-mouth {
  position: absolute;
}

.pet-eye {
  top: 26px;
  width: 7px;
  height: 11px;
  border-radius: 999px;
  background: #ffffff;
  transform-origin: center;
  animation: blink 4.6s ease-in-out infinite;
}

.pet-eye.left {
  left: 21px;
}

.pet-eye.right {
  right: 21px;
}

.pet-cheek {
  top: 39px;
  width: 12px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.42);
}

.pet-cheek.left {
  left: 13px;
}

.pet-cheek.right {
  right: 13px;
}

.pet-mouth {
  left: 50%;
  bottom: 17px;
  width: 18px;
  height: 10px;
  border: 2px solid rgba(255, 255, 255, 0.96);
  border-top: 0;
  border-left-color: transparent;
  border-right-color: transparent;
  border-bottom-left-radius: 999px;
  border-bottom-right-radius: 999px;
  transform: translateX(-50%) translateY(1px);
}

.pet-trigger::before,
.pet-trigger::after {
  content: '';
  position: absolute;
  width: 10px;
  height: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.5);
  top: 42px;
}

.pet-trigger::before {
  right: 50px;
}

.pet-trigger::after {
  right: 12px;
}

.pet-label {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(29, 92, 87, 0.12);
  color: #244642;
  font-size: 12px;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.12);
}

.pet-panel-enter-active,
.pet-panel-leave-active {
  transition: all 0.2s ease;
}

.pet-panel-enter-from,
.pet-panel-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}

@keyframes floaty {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

@keyframes blink {
  0%, 44%, 100% {
    transform: scaleY(1);
  }
  46%, 48% {
    transform: scaleY(0.18);
  }
}

@media (max-width: 768px) {
  .assistant-pet {
    right: 14px;
    bottom: 14px;
  }

  .pet-label {
    display: none;
  }
}
</style>
