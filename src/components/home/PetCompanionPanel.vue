<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const SPRITE_URL = `${API_BASE_URL}/pet/sprite`
const SPRITE_SIZE = 32
const DISPLAY_SCALE = 4
const FRAME_INTERVAL_MS = 120

const fallbackFoods = [
  { id: 'salmon-bites', label: '三文鱼粒' },
  { id: 'chicken-soup', label: '鸡肉浓汤' },
  { id: 'berry-snack', label: '莓果猫零食' },
]

const fallbackPet = {
  name: '我',
  color: 'pink-white',
  satiety: 72,
  hungerStage: 'content',
  mood: '安静趴着',
  reaction: '',
  foods: fallbackFoods,
}

const idleBubbleMessages = {
  'well-fed': ['呼噜呼噜。', '今天很舒服。', '我先趴一会儿。'],
  content: ['我在这里。', '尾巴轻轻晃一下。', '点我一下看看。'],
  hungry: ['有点想吃小鱼。', '饭碗在哪儿呢。', '我开始惦记零食了。'],
  starving: ['真的有点饿了。', '先喂我一口嘛。', '肚子在咕噜咕噜。'],
}

// Adapted from oneko.js sprite states and frame coordinates.
const spriteSets = {
  idle: [[-3, -3]],
  alert: [[-7, -3]],
  scratchSelf: [
    [-5, 0],
    [-6, 0],
    [-7, 0],
  ],
  tired: [[-3, -2]],
  sleeping: [
    [-2, 0],
    [-2, -1],
  ],
}

const props = defineProps({
  pet: {
    type: Object,
    default: null,
  },
  loading: {
    type: Boolean,
    default: false,
  },
  busyAction: {
    type: String,
    default: '',
  },
  error: {
    type: String,
    default: '',
  },
  chatMessages: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['pat', 'feed', 'chat', 'reload'])

const chatInput = ref('')
const panelOpen = ref(false)
const spriteFrame = ref(0)
const bubbleMessage = ref('')
const bubbleKind = ref('idle')

let spriteTimer = null
let bubbleHideTimer = null
let idleBubbleTimer = null
let hydratedReaction = false
let lastReaction = ''
let lastError = ''

const resolvedPet = computed(() => ({
  ...fallbackPet,
  ...props.pet,
  name: '我',
  foods: props.pet?.foods?.length ? props.pet.foods : fallbackFoods,
}))

const hasBackendState = computed(() => Boolean(props.pet))
const interactionDisabled = computed(() => props.loading || !hasBackendState.value)
const chatDisabled = computed(() => interactionDisabled.value || props.busyAction === 'chat')
const satietyLabel = computed(() => `${resolvedPet.value.satiety ?? 0}/100`)
const idleBubblePool = computed(() => idleBubbleMessages[resolvedPet.value.hungerStage] ?? idleBubbleMessages.content)

const spriteState = computed(() => {
  if (props.busyAction === 'pat') {
    return 'scratchSelf'
  }

  if (props.busyAction === 'feed' || panelOpen.value) {
    return 'alert'
  }

  if (resolvedPet.value.hungerStage === 'well-fed') {
    return 'sleeping'
  }

  if (resolvedPet.value.hungerStage === 'starving') {
    return 'tired'
  }

  return 'idle'
})

const currentSpritePosition = computed(() => {
  const frames = spriteSets[spriteState.value] ?? spriteSets.idle
  return frames[spriteFrame.value % frames.length]
})

const catSpriteStyle = computed(() => ({
  width: `${SPRITE_SIZE}px`,
  height: `${SPRITE_SIZE}px`,
  backgroundImage: `url(${SPRITE_URL})`,
  backgroundPosition: `${currentSpritePosition.value[0] * SPRITE_SIZE}px ${currentSpritePosition.value[1] * SPRITE_SIZE}px`,
  transform: `scale(${DISPLAY_SCALE})`,
}))

const connectionText = computed(() => {
  if (props.loading) {
    return '正在连接后端'
  }

  if (!hasBackendState.value) {
    return '后端暂时离线'
  }

  return `饱食 ${satietyLabel.value}`
})

function advanceSprite() {
  const frames = spriteSets[spriteState.value] ?? spriteSets.idle
  spriteFrame.value = (spriteFrame.value + 1) % frames.length
}

function clearBubbleHideTimer() {
  if (bubbleHideTimer) {
    window.clearTimeout(bubbleHideTimer)
    bubbleHideTimer = null
  }
}

function clearIdleBubbleTimer() {
  if (idleBubbleTimer) {
    window.clearTimeout(idleBubbleTimer)
    idleBubbleTimer = null
  }
}

function scheduleIdleBubble() {
  clearIdleBubbleTimer()

  if (!hasBackendState.value || props.loading || panelOpen.value || props.busyAction || bubbleMessage.value) {
    return
  }

  const delay = 32000 + Math.round(Math.random() * 22000)
  idleBubbleTimer = window.setTimeout(() => {
    const pool = idleBubblePool.value
    const text = pool[Math.floor(Math.random() * pool.length)]
    showBubble(text, { kind: 'idle', duration: 5000 })
  }, delay)
}

function hideBubble() {
  clearBubbleHideTimer()
  bubbleMessage.value = ''
  bubbleKind.value = 'idle'
  scheduleIdleBubble()
}

function showBubble(message, options = {}) {
  const trimmed = message?.trim()

  if (!trimmed) {
    return
  }

  const { kind = 'idle', duration = 5000 } = options

  clearIdleBubbleTimer()
  clearBubbleHideTimer()
  bubbleMessage.value = trimmed
  bubbleKind.value = kind
  bubbleHideTimer = window.setTimeout(hideBubble, duration)
}

function togglePanel() {
  panelOpen.value = !panelOpen.value
}

function handlePat() {
  if (interactionDisabled.value || props.busyAction === 'pat') {
    return
  }

  emit('pat')
}

function handleFeed(optionId = resolvedPet.value.foods[0]?.id ?? 'salmon-bites') {
  if (interactionDisabled.value || props.busyAction === 'feed') {
    return
  }

  emit('feed', optionId)
}

function submitChat() {
  const message = chatInput.value.trim()

  if (!message || chatDisabled.value) {
    return
  }

  emit('chat', message)
  chatInput.value = ''
}

watch(spriteState, () => {
  spriteFrame.value = 0
})

watch(
  () => resolvedPet.value.reaction,
  (reaction) => {
    const nextReaction = reaction?.trim() ?? ''

    if (!hydratedReaction) {
      hydratedReaction = true
      lastReaction = nextReaction
      scheduleIdleBubble()
      return
    }

    if (nextReaction && nextReaction !== lastReaction) {
      showBubble(nextReaction, { kind: 'reply', duration: 15000 })
    }

    lastReaction = nextReaction
  },
)

watch(
  () => props.error,
  (error) => {
    const nextError = error?.trim() ?? ''

    if (nextError && nextError !== lastError) {
      showBubble(nextError, { kind: 'error', duration: 15000 })
    }

    lastError = nextError
  },
)

watch(
  [panelOpen, () => props.loading, () => props.busyAction, hasBackendState],
  () => {
    if (!panelOpen.value && !bubbleMessage.value) {
      scheduleIdleBubble()
      return
    }

    if (panelOpen.value) {
      clearIdleBubbleTimer()
    }
  },
)

onMounted(() => {
  spriteTimer = window.setInterval(advanceSprite, FRAME_INTERVAL_MS)
  scheduleIdleBubble()
})

onBeforeUnmount(() => {
  if (spriteTimer) {
    window.clearInterval(spriteTimer)
  }

  clearBubbleHideTimer()
  clearIdleBubbleTimer()
})
</script>

<template>
  <Teleport to="body">
    <section class="pet-dock" :class="{ 'is-open': panelOpen }">
      <div class="pet-stage">
        <div v-if="bubbleMessage" class="cat-bubble" :class="`bubble-${bubbleKind}`" aria-live="polite">
          <span class="bubble-text">{{ bubbleMessage }}</span>
        </div>

        <div v-if="panelOpen" class="interaction-layer" role="group" aria-label="互动菜单">
          <button
            class="action-bubble action-feed"
            :class="{ busy: busyAction === 'feed' }"
            type="button"
            :disabled="interactionDisabled"
            aria-label="喂我吃东西"
            @click="handleFeed()"
          >
            <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M3.5 12c2.2-2.5 4.7-3.8 7.6-3.8 3.4 0 5.8 1.3 7.5 3.8-1.7 2.5-4.1 3.8-7.5 3.8-2.9 0-5.4-1.3-7.6-3.8Zm14.4-3.1 2.6-2.5v11.2l-2.6-2.5m-8.2-3.1h.1"
              />
            </svg>
            <span>喂食</span>
          </button>

          <button
            class="action-bubble action-pat"
            :class="{ busy: busyAction === 'pat' }"
            type="button"
            :disabled="interactionDisabled"
            aria-label="抚摸我"
            @click="handlePat"
          >
            <svg class="action-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M8.2 12.5V6.7c0-.8.6-1.4 1.3-1.4s1.3.6 1.3 1.4v3.1m0 2.7V5.4c0-.8.6-1.4 1.3-1.4s1.3.6 1.3 1.4v6.2m0 1.1V6.9c0-.8.6-1.4 1.3-1.4s1.3.6 1.3 1.4v7.4m0 .1v-4c0-.8.6-1.4 1.3-1.4s1.3.6 1.3 1.4v5.2c0 3-2.5 5.4-5.5 5.4h-1.1c-2 0-3.8-1.1-4.7-2.8l-1.8-3.5c-.3-.6-.1-1.3.5-1.6.5-.2 1.1 0 1.4.5l1.8 2.1Z"
              />
            </svg>
            <span>抚摸</span>
          </button>
        </div>

        <button
          class="cat-button"
          type="button"
          :aria-expanded="panelOpen"
          :aria-label="panelOpen ? '收起交互界面' : '展开交互界面'"
          @click="togglePanel"
        >
          <span class="cat-shadow"></span>
          <span class="cat-sprite" :style="catSpriteStyle" aria-hidden="true"></span>
        </button>
      </div>

      <aside v-if="panelOpen" class="pet-chat-window">
        <div class="chat-head">
          <div class="chat-title">
            <span class="chat-dot" :class="{ offline: !hasBackendState && !loading }"></span>
            <strong>{{ resolvedPet.name }}</strong>
          </div>
          <span class="chat-state">{{ loading ? '连接中' : resolvedPet.mood }}</span>
        </div>

        <div class="chat-log">
          <div v-for="message in chatMessages" :key="message.id" class="chat-line" :class="message.role">
            <p>{{ message.content }}</p>
          </div>
        </div>

        <form class="chat-form" @submit.prevent="submitChat">
          <input
            v-model="chatInput"
            class="chat-input"
            type="text"
            maxlength="240"
            placeholder="和我说句话"
            :disabled="chatDisabled"
          />
          <button class="chat-send" type="submit" :disabled="chatDisabled">
            {{ busyAction === 'chat' ? '...' : '发送' }}
          </button>
        </form>

        <div class="chat-foot">
          <span>{{ connectionText }}</span>
          <button v-if="!hasBackendState && !loading" class="chat-reload" type="button" @click="emit('reload')">
            重连
          </button>
        </div>
      </aside>
    </section>
  </Teleport>
</template>

<style scoped>
.pet-dock {
  position: fixed;
  inset: 0;
  z-index: 1400;
  pointer-events: none;
}

.pet-stage,
.pet-chat-window {
  pointer-events: none;
}

.pet-stage {
  position: fixed;
  left: 50%;
  bottom: 0;
  width: min(320px, calc(100vw - 16px));
  height: 280px;
  transform: translateX(-50%);
  z-index: 1405;
}

.cat-bubble {
  position: absolute;
  left: 50%;
  bottom: 150px;
  z-index: 1402;
  max-width: min(228px, calc(100vw - 40px));
  padding: 12px 16px;
  border: 4px solid #3e3345;
  background: #fff4f8;
  color: #302937;
  box-shadow:
    0 0 0 4px #fff8fb,
    8px 8px 0 rgba(17, 21, 36, 0.16);
  transform: translateX(-50%);
  transition: bottom 0.24s ease, transform 0.24s ease;
  pointer-events: none;
}

.pet-dock.is-open .cat-bubble {
  bottom: 218px;
}

.cat-bubble::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: -12px;
  width: 12px;
  height: 12px;
  border-right: 4px solid #3e3345;
  border-bottom: 4px solid #3e3345;
  background: inherit;
  box-shadow: 4px 4px 0 0 #fff8fb;
  transform: translateX(-50%) rotate(45deg);
}

.bubble-idle {
  background: #fff4f8;
  color: #302937;
}

.bubble-reply {
  background: #fff8ef;
  color: #473424;
}

.bubble-error {
  background: #ffe8df;
  color: #6b392c;
}

.bubble-text {
  display: block;
  line-height: 1.6;
  text-align: center;
}

.interaction-layer {
  position: absolute;
  left: 50%;
  bottom: 154px;
  z-index: 1406;
  display: flex;
  gap: 8px;
  transform: translateX(-50%);
  pointer-events: auto;
}

.action-bubble {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 72px;
  min-height: 36px;
  padding: 0 10px;
  border: 3px solid #3e3345;
  border-radius: 999px;
  background: #fff4f8;
  color: #302937;
  box-shadow:
    0 0 0 3px #fff8fb,
    5px 5px 0 rgba(17, 21, 36, 0.12);
  cursor: pointer;
  font: inherit;
  font-weight: 700;
  font-size: 0.82rem;
  transition:
    transform 0.18s ease,
    background-color 0.18s ease,
    color 0.18s ease;
}

.action-bubble:hover:not(:disabled) {
  transform: translateY(-2px);
}

.action-feed {
  background: #fff7ef;
  color: #523923;
}

.action-pat {
  background: #fff1f5;
  color: #4a2d3a;
}

.action-bubble:disabled,
.chat-input:disabled,
.chat-send:disabled,
.chat-reload:disabled {
  opacity: 0.58;
  cursor: not-allowed;
}

.action-icon {
  width: 14px;
  height: 14px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.cat-button {
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 180px;
  height: 150px;
  border: 0;
  padding: 0;
  background: transparent;
  transform: translateX(-50%);
  cursor: pointer;
  pointer-events: auto;
}

.cat-shadow {
  position: absolute;
  left: 50%;
  bottom: 12px;
  width: 112px;
  height: 18px;
  border-radius: 999px;
  background: rgba(6, 9, 17, 0.28);
  filter: blur(8px);
  transform: translateX(-50%);
}

.cat-sprite {
  position: absolute;
  left: 50%;
  bottom: 34px;
  background-repeat: no-repeat;
  image-rendering: pixelated;
  transform-origin: center bottom;
  pointer-events: none;
}

.pet-chat-window {
  position: fixed;
  right: 18px;
  bottom: 128px;
  z-index: 1401;
  width: min(304px, calc(100vw - 24px));
  padding: 15px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(7, 12, 22, 0.94), rgba(7, 12, 22, 0.8)),
    rgba(7, 12, 22, 0.86);
  box-shadow: 0 24px 66px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(16px);
  pointer-events: auto;
}

.chat-head,
.chat-foot,
.chat-title,
.chat-form {
  display: flex;
  align-items: center;
}

.chat-head,
.chat-foot {
  justify-content: space-between;
  gap: 12px;
}

.chat-head {
  margin-bottom: 12px;
}

.chat-title {
  gap: 8px;
}

.chat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #7ae6b1;
  box-shadow: 0 0 12px rgba(122, 230, 177, 0.5);
}

.chat-dot.offline {
  background: #f3a283;
  box-shadow: 0 0 12px rgba(243, 162, 131, 0.38);
}

.chat-state,
.chat-foot {
  color: rgba(233, 237, 246, 0.72);
  font-size: 0.82rem;
}

.chat-log {
  display: grid;
  gap: 10px;
  max-height: 208px;
  margin-bottom: 12px;
  overflow-y: auto;
}

.chat-line {
  max-width: 92%;
  padding: 10px 12px;
  border-radius: 16px;
  line-height: 1.6;
}

.chat-line p {
  margin: 0;
}

.chat-line.user {
  justify-self: end;
  background: rgba(118, 143, 255, 0.14);
}

.chat-line.assistant {
  justify-self: start;
  background: rgba(255, 196, 161, 0.12);
}

.chat-form {
  gap: 10px;
}

.chat-input {
  flex: 1;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  color: #f7f8fc;
  font: inherit;
}

.chat-send,
.chat-reload {
  border: 0;
  border-radius: 14px;
  font: inherit;
  cursor: pointer;
}

.chat-send {
  min-width: 70px;
  min-height: 40px;
  background: linear-gradient(135deg, #ffae8a, #ffd97b);
  color: #1b2230;
  font-weight: 700;
}

.chat-reload {
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.08);
  color: #f1f4fb;
}

@media (max-width: 720px) {
  .pet-stage {
    width: min(300px, calc(100vw - 12px));
    height: 268px;
  }

  .cat-bubble {
    bottom: 144px;
    max-width: min(212px, calc(100vw - 32px));
  }

  .pet-dock.is-open .cat-bubble {
    bottom: 208px;
  }

  .interaction-layer {
    bottom: 148px;
    gap: 6px;
  }

  .action-bubble {
    min-width: 66px;
    min-height: 34px;
    padding: 0 9px;
    font-size: 0.78rem;
  }

  .pet-chat-window {
    left: 10px;
    right: 10px;
    bottom: 164px;
    width: auto;
  }
}
</style>
