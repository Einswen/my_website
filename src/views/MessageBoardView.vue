<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { fetchGuestbookMessages, pinnedMessage, submitGuestbookMessage } from '../api/messages'
import AppNavbar from '../components/home/AppNavbar.vue'

const PREVIEW_WORD_LIMIT = 100

const navLinks = [
  { label: '首页', to: '/', align: 'left' },
  { label: '晚霞预测', to: '/forecast' },
  { label: '留言板', to: '/messages' },
  { label: '联系我', email: 'EinswenL@gmail.com' },
]

const name = ref('')
const content = ref('')
const messages = ref([])
const expandedMessageIds = ref(new Set())
const loadingMessages = ref(true)
const submitting = ref(false)
const error = ref('')
const successMessage = ref('')
const cooldownRemaining = ref(0)
const cooldownUntil = ref(0)

let cooldownTimer = null

const allMessages = computed(() => [pinnedMessage, ...messages.value])
const canSubmit = computed(
  () =>
    !submitting.value &&
    cooldownRemaining.value <= 0 &&
    name.value.trim() &&
    content.value.trim(),
)

function tokenizeContent(value) {
  return value.match(/[\u4e00-\u9fff]|[A-Za-z0-9]+(?:[-'][A-Za-z0-9]+)?/g) ?? []
}

function shouldCollapseMessage(message) {
  return tokenizeContent(message.content).length > PREVIEW_WORD_LIMIT
}

function getMessagePreview(message) {
  if (!shouldCollapseMessage(message) || expandedMessageIds.value.has(message.id)) {
    return message.content
  }

  const tokens = tokenizeContent(message.content).slice(0, PREVIEW_WORD_LIMIT)
  const usesMostlyChinese = tokens.filter((token) => /^[\u4e00-\u9fff]$/.test(token)).length > tokens.length / 2

  return `${usesMostlyChinese ? tokens.join('') : tokens.join(' ')}...`
}

function toggleMessage(messageId) {
  const nextExpandedIds = new Set(expandedMessageIds.value)

  if (nextExpandedIds.has(messageId)) {
    nextExpandedIds.delete(messageId)
  } else {
    nextExpandedIds.add(messageId)
  }

  expandedMessageIds.value = nextExpandedIds
}

function formatMessageTime(value) {
  if (value === '置顶') {
    return value
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function formatCooldown(value) {
  const totalSeconds = Math.ceil(value / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60

  return `${minutes}分${String(seconds).padStart(2, '0')}秒`
}

function refreshCooldown() {
  cooldownRemaining.value = Math.max(0, cooldownUntil.value - Date.now())
}

function startCooldownTimer() {
  window.clearInterval(cooldownTimer)
  refreshCooldown()
  cooldownTimer = window.setInterval(refreshCooldown, 1000)
}

function applyCooldown(seconds) {
  cooldownUntil.value = Date.now() + seconds * 1000
  startCooldownTimer()
}

async function submitMessage() {
  error.value = ''
  successMessage.value = ''
  refreshCooldown()

  if (!name.value.trim() || !content.value.trim()) {
    error.value = '请填写名字和留言。'
    return
  }

  if (cooldownRemaining.value > 0) {
    error.value = `一小时只能留言一次，请 ${formatCooldown(cooldownRemaining.value)} 后再试。`
    return
  }

  submitting.value = true

  try {
    const result = await submitGuestbookMessage({
      name: name.value,
      content: content.value,
    })

    messages.value = [result.message, ...messages.value]
    applyCooldown(result.cooldownSeconds)
    name.value = ''
    content.value = ''
    successMessage.value = '留言已保存。'
  } catch (requestError) {
    const retryAfterSeconds = requestError?.retryAfterSeconds ?? 0

    if (retryAfterSeconds > 0) {
      applyCooldown(retryAfterSeconds)
    }

    error.value = requestError instanceof Error ? requestError.message : '留言保存失败，请稍后再试。'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    messages.value = await fetchGuestbookMessages()
  } catch {
    error.value = '留言加载失败，请检查后端服务。'
  } finally {
    loadingMessages.value = false
  }
})

onBeforeUnmount(() => {
  window.clearInterval(cooldownTimer)
})
</script>

<template>
  <main class="message-view">
    <div class="background-glow background-glow-a"></div>
    <div class="background-glow background-glow-b"></div>

    <div class="page-shell">
      <AppNavbar :links="navLinks" />

      <section class="message-board" aria-labelledby="message-title">
        <div class="message-heading">
          <h1 id="message-title">留言板</h1>
        </div>

        <form class="message-form" @submit.prevent="submitMessage">
          <input v-model="name" type="text" placeholder="名字" autocomplete="name" />
          <textarea v-model="content" placeholder="写点什么..." rows="5"></textarea>

          <div class="form-actions">
            <p v-if="cooldownRemaining > 0" class="form-status">
              一小时只能留言一次，剩余 {{ formatCooldown(cooldownRemaining) }}
            </p>
            <p v-else-if="error" class="form-status form-status-error">{{ error }}</p>
            <p v-else-if="successMessage" class="form-status form-status-success">
              {{ successMessage }}
            </p>
            <p v-else class="form-status"> </p>

            <button type="submit" :disabled="!canSubmit">
              {{ submitting ? '发布中...' : '发布留言' }}
            </button>
          </div>
        </form>

        <div class="message-list">
          <article
            v-for="message in allMessages"
            :key="message.id"
            class="message-card"
            :class="{ 'message-card-pinned': message.pinned }"
          >
            <div class="message-meta">
              <strong>{{ message.name }}</strong>
              <span>{{ formatMessageTime(message.createdAt) }}</span>
            </div>

            <p class="message-content">{{ getMessagePreview(message) }}</p>

            <button
              v-if="shouldCollapseMessage(message)"
              class="expand-button"
              type="button"
              @click="toggleMessage(message.id)"
            >
              {{ expandedMessageIds.has(message.id) ? '收起' : '展开' }}
            </button>
          </article>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
:global(body) {
  margin: 0;
  min-width: 320px;
  background:
    radial-gradient(circle at 20% 12%, rgba(78, 112, 255, 0.18), transparent 30%),
    radial-gradient(circle at 82% 18%, rgba(255, 147, 83, 0.14), transparent 28%),
    linear-gradient(180deg, #050a16 0%, #080f1d 52%, #0b0d18 100%);
  color: #eff4ff;
  font-family:
    'Avenir Next',
    'SF Pro Display',
    'PingFang SC',
    'Hiragino Sans GB',
    sans-serif;
}

:global(*) {
  box-sizing: border-box;
}

.message-view {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.page-shell {
  position: relative;
  z-index: 1;
  width: min(980px, calc(100% - 32px));
  margin: 0 auto;
  padding: 24px 0 64px;
}

.background-glow {
  position: absolute;
  border-radius: 999px;
  filter: blur(34px);
  opacity: 0.72;
}

.background-glow-a {
  top: 160px;
  left: -120px;
  width: 260px;
  height: 260px;
  background: rgba(73, 112, 255, 0.18);
}

.background-glow-b {
  right: -110px;
  bottom: 100px;
  width: 280px;
  height: 280px;
  background: rgba(255, 130, 77, 0.13);
}

.message-board {
  margin-top: 28px;
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 28px;
  background: rgba(8, 13, 27, 0.62);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
}

.message-heading {
  margin-bottom: 22px;
}

.message-heading h1 {
  margin: 0;
  font-size: clamp(2rem, 5vw, 3.2rem);
  line-height: 1;
}

.message-form {
  display: grid;
  gap: 14px;
  margin-bottom: 22px;
}

.message-form input,
.message-form textarea {
  width: 100%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.055);
  color: #ffffff;
  font: inherit;
  outline: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.message-form input {
  padding: 14px 16px;
}

.message-form textarea {
  min-height: 132px;
  padding: 16px;
  resize: vertical;
}

.message-form input:focus,
.message-form textarea:focus {
  border-color: rgba(255, 179, 110, 0.54);
  box-shadow: 0 0 0 4px rgba(255, 179, 110, 0.1);
}

.message-form input::placeholder,
.message-form textarea::placeholder {
  color: rgba(239, 244, 255, 0.42);
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.form-status {
  margin: 0;
  color: rgba(239, 244, 255, 0.58);
  font-size: 0.9rem;
}

.form-status-error {
  color: #ff9b9b;
}

.form-status-success {
  color: #a5f0c5;
}

.form-actions button,
.expand-button {
  border: 0;
  border-radius: 999px;
  color: #08101e;
  font-weight: 700;
  cursor: pointer;
}

.form-actions button {
  flex: 0 0 auto;
  padding: 12px 18px;
  background: linear-gradient(135deg, #ffe29f, #ff8f70);
  box-shadow: 0 12px 34px rgba(255, 143, 112, 0.22);
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}

.form-actions button:not(:disabled):hover {
  transform: translateY(-1px);
}

.form-actions button:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.message-list {
  display: grid;
  gap: 14px;
}

.message-card {
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.045);
}

.message-card-pinned {
  border-color: rgba(255, 198, 122, 0.2);
  background:
    linear-gradient(135deg, rgba(255, 196, 120, 0.08), rgba(255, 255, 255, 0.04)),
    rgba(255, 255, 255, 0.045);
}

.message-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.message-meta strong {
  color: #ffffff;
}

.message-meta span {
  color: rgba(239, 244, 255, 0.48);
  font-size: 0.86rem;
}

.message-content {
  margin: 12px 0 0;
  color: rgba(239, 244, 255, 0.78);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.expand-button {
  margin-top: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.86);
}

@media (max-width: 720px) {
  .page-shell {
    width: min(100% - 24px, 980px);
    padding-bottom: 42px;
  }

  .message-board {
    padding: 22px;
  }

  .form-actions,
  .message-meta {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-actions button {
    width: 100%;
  }
}
</style>
