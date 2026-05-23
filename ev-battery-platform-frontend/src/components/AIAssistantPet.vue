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

    <div v-if="bubbleVisible" class="assistant-bubble">
      <button class="robot-button" type="button" aria-label="打开 AI 小助手" @click="togglePanel">
        <span class="robot-figure" aria-hidden="true">
          <span class="robot-antenna left"></span>
          <span class="robot-antenna right"></span>
          <span class="robot-ear left"></span>
          <span class="robot-ear right"></span>
          <span class="robot-head">
            <span class="robot-face">
              <span class="blink-eye left"></span>
              <span class="blink-eye right"></span>
              <span class="robot-smile"></span>
            </span>
          </span>
          <span class="robot-body"></span>
        </span>
      </button>
      <button class="bubble-copy" type="button" @click="togglePanel">
        <strong>Hi，我是小E</strong>
        <span>有问题可以随时问我哦~</span>
      </button>
      <button class="bubble-close" type="button" aria-label="关闭助手提示" @click="bubbleVisible = false">×</button>
    </div>

    <button v-else class="mini-robot" type="button" aria-label="打开 AI 小助手" @click="togglePanel">
      <span class="robot-figure" aria-hidden="true">
        <span class="robot-antenna left"></span>
        <span class="robot-antenna right"></span>
        <span class="robot-ear left"></span>
        <span class="robot-ear right"></span>
        <span class="robot-head">
          <span class="robot-face">
            <span class="blink-eye left"></span>
            <span class="blink-eye right"></span>
            <span class="robot-smile"></span>
          </span>
        </span>
        <span class="robot-body"></span>
      </span>
    </button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { streamAssistantReply } from '../api/assistant'

const panelVisible = ref(false)
const bubbleVisible = ref(true)
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
  bubbleVisible.value = true
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
  border: 1px solid rgba(204, 217, 238, 0.88);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 28px 60px rgba(37, 88, 170, 0.18);
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
  padding: 6px 10px;
  border: 1px solid rgba(204, 217, 238, 0.95);
  border-radius: 999px;
  background: #ffffff;
  color: #36577e;
  font-size: 12px;
  cursor: pointer;
}

.pet-thread {
  display: flex;
  max-height: 240px;
  flex-direction: column;
  gap: 10px;
  padding: 14px 18px;
  overflow: auto;
}

.pet-bubble {
  max-width: 88%;
  padding: 12px 14px;
  border-radius: 18px;
}

.pet-bubble p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.pet-bubble.assistant {
  align-self: flex-start;
  background: #f6f9ff;
}

.pet-bubble.user {
  align-self: flex-end;
  background: rgba(47, 124, 255, 0.1);
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

.assistant-bubble {
  position: relative;
  display: flex;
  width: 330px;
  min-height: 94px;
  align-items: center;
  padding: 13px 42px 13px 118px;
  border: 1px solid rgba(214, 224, 244, 0.78);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 38px rgba(79, 126, 195, 0.2);
}

.robot-button,
.mini-robot {
  position: relative;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.robot-button {
  position: absolute;
  left: -1px;
  bottom: 1px;
  width: 116px;
  height: 116px;
}

.bubble-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #263853;
  cursor: pointer;
  text-align: left;
}

.bubble-copy strong {
  font-size: 14px;
  line-height: 1.2;
}

.bubble-copy span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.35;
}

.bubble-close {
  position: absolute;
  top: 22px;
  right: 18px;
  display: inline-flex;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: 0;
  background: transparent;
  color: #6b778c;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.mini-robot {
  width: 86px;
  height: 86px;
  border-radius: 26px;
}

.robot-figure {
  position: relative;
  display: block;
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 14px 22px rgba(72, 128, 214, 0.22));
}

.robot-head {
  position: absolute;
  left: 18%;
  top: 12%;
  z-index: 2;
  width: 64%;
  height: 52%;
  border-radius: 48% 48% 42% 42%;
  background: linear-gradient(145deg, #ffffff 0%, #f4f8ff 72%, #e6efff 100%);
  box-shadow: inset -5px -7px 16px rgba(65, 118, 205, 0.12), inset 5px 5px 16px rgba(255, 255, 255, 0.9);
}

.robot-face {
  position: absolute;
  left: 16%;
  top: 26%;
  width: 68%;
  height: 42%;
  border-radius: 16px;
  background: linear-gradient(145deg, #101a33 0%, #071326 100%);
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.08);
}

.robot-body {
  position: absolute;
  left: 27%;
  top: 57%;
  width: 46%;
  height: 31%;
  border-radius: 48% 48% 42% 42%;
  background: linear-gradient(145deg, #ffffff 0%, #edf4ff 100%);
  box-shadow: inset -4px -7px 14px rgba(65, 118, 205, 0.12);
}

.robot-body::before,
.robot-body::after {
  position: absolute;
  top: 20%;
  width: 24%;
  height: 42%;
  border-radius: 999px;
  background: linear-gradient(145deg, #ffffff 0%, #e8f0ff 100%);
  content: "";
}

.robot-body::before {
  left: -20%;
  transform: rotate(18deg);
}

.robot-body::after {
  right: -20%;
  transform: rotate(-18deg);
}

.robot-ear,
.robot-antenna {
  position: absolute;
  z-index: 1;
  background: linear-gradient(145deg, #79a9ff 0%, #2777ff 100%);
}

.robot-ear {
  top: 30%;
  width: 12%;
  height: 22%;
  border-radius: 999px;
}

.robot-ear.left {
  left: 9%;
}

.robot-ear.right {
  right: 9%;
}

.robot-antenna {
  top: 8%;
  width: 8%;
  height: 18%;
  border-radius: 999px;
}

.robot-antenna.left {
  left: 28%;
  transform: rotate(-20deg);
}

.robot-antenna.right {
  right: 28%;
  transform: rotate(20deg);
}

.blink-eye {
  position: absolute;
  top: 30%;
  width: 12%;
  height: 30%;
  border-radius: 999px;
  background: #bff9ff;
  box-shadow: 0 0 8px rgba(189, 249, 255, 0.85);
  transform-origin: center;
  animation: robot-blink 4.2s ease-in-out infinite;
}

.blink-eye.left {
  left: 27%;
}

.blink-eye.right {
  right: 27%;
}

.robot-smile {
  position: absolute;
  left: 50%;
  bottom: 18%;
  width: 28%;
  height: 22%;
  border: 3px solid #bff9ff;
  border-top: 0;
  border-left-color: transparent;
  border-right-color: transparent;
  border-bottom-left-radius: 999px;
  border-bottom-right-radius: 999px;
  transform: translateX(-50%);
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

@keyframes robot-blink {
  0%, 43%, 49%, 100% {
    transform: scaleY(1);
  }
  45%, 47% {
    transform: scaleY(0.12);
  }
}

@media (max-width: 768px) {
  .assistant-pet {
    right: 14px;
    bottom: 14px;
  }

  .assistant-bubble {
    width: 286px;
    min-height: 86px;
    padding-left: 98px;
  }

  .robot-button {
    width: 98px;
    height: 98px;
  }
}
</style>
