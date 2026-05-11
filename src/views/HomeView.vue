<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

import AppNavbar from '../components/home/AppNavbar.vue'

const navLinks = [
  { label: '首页', to: '/', align: 'left' },
  { label: '晚霞预测', to: '/forecast' },
  { label: '留言板', to: '/messages' },
  { label: '联系我', email: 'EinswenL@gmail.com' },
]

const welcomeWords = ['hihi', '你好～']
const typedWord = ref('')
const particleCanvas = ref(null)

let animationTimer = null
let particleFrame = null
let particles = []

const pointer = {
  x: 0,
  y: 0,
  active: false,
}

function startWelcomeTyping() {
  let wordIndex = 0
  let characterIndex = 0
  let isDeleting = false

  const tick = () => {
    const currentWord = welcomeWords[wordIndex]

    if (isDeleting) {
      characterIndex -= 1
    } else {
      characterIndex += 1
    }

    typedWord.value = currentWord.slice(0, characterIndex)

    let delay = isDeleting ? 85 : 140

    if (!isDeleting && characterIndex === currentWord.length) {
      delay = 1200
      isDeleting = true
    } else if (isDeleting && characterIndex === 0) {
      isDeleting = false
      wordIndex = (wordIndex + 1) % welcomeWords.length
      delay = 320
    }

    animationTimer = window.setTimeout(tick, delay)
  }

  tick()
}

function createParticle(width, height) {
  const depth = Math.random()

  return {
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * (0.18 + depth * 0.24),
    vy: (Math.random() - 0.5) * (0.18 + depth * 0.24),
    radius: 0.8 + depth * 1.8,
    alpha: 0.22 + depth * 0.46,
    hue: Math.random() > 0.72 ? 28 : 218,
  }
}

function startParticleField() {
  const canvas = particleCanvas.value

  if (!canvas) {
    return
  }

  const context = canvas.getContext('2d')
  const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')

  if (!context || motionQuery.matches) {
    return
  }

  const resizeCanvas = () => {
    const width = window.innerWidth
    const height = window.innerHeight
    const pixelRatio = Math.min(window.devicePixelRatio || 1, 2)

    canvas.width = Math.floor(width * pixelRatio)
    canvas.height = Math.floor(height * pixelRatio)
    canvas.style.width = `${width}px`
    canvas.style.height = `${height}px`
    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0)

    const particleCount = Math.min(128, Math.max(58, Math.floor((width * height) / 15500)))
    particles = Array.from({ length: particleCount }, () => createParticle(width, height))
  }

  const trackPointer = (event) => {
    pointer.x = event.clientX
    pointer.y = event.clientY
    pointer.active = true
  }

  const clearPointer = () => {
    pointer.active = false
  }

  const drawParticles = () => {
    const width = window.innerWidth
    const height = window.innerHeight

    context.clearRect(0, 0, width, height)

    const pulse = 0.5 + Math.sin(Date.now() * 0.0018) * 0.5
    const mouseRadius = 170
    const linkDistance = 132

    particles.forEach((particle, index) => {
      if (pointer.active) {
        const dx = particle.x - pointer.x
        const dy = particle.y - pointer.y
        const distance = Math.hypot(dx, dy)

        if (distance > 0 && distance < mouseRadius) {
          const force = (1 - distance / mouseRadius) * 0.045
          particle.vx += (dx / distance) * force
          particle.vy += (dy / distance) * force
        }
      }

      particle.x += particle.vx
      particle.y += particle.vy
      particle.vx *= 0.985
      particle.vy *= 0.985

      if (particle.x < -20) particle.x = width + 20
      if (particle.x > width + 20) particle.x = -20
      if (particle.y < -20) particle.y = height + 20
      if (particle.y > height + 20) particle.y = -20

      for (let nextIndex = index + 1; nextIndex < particles.length; nextIndex += 1) {
        const nextParticle = particles[nextIndex]
        const dx = particle.x - nextParticle.x
        const dy = particle.y - nextParticle.y
        const distance = Math.hypot(dx, dy)

        if (distance < linkDistance) {
          const opacity = (1 - distance / linkDistance) * 0.18
          const gradient = context.createLinearGradient(
            particle.x,
            particle.y,
            nextParticle.x,
            nextParticle.y,
          )

          gradient.addColorStop(0, `rgba(129, 164, 255, ${opacity})`)
          gradient.addColorStop(1, `rgba(255, 165, 104, ${opacity * 0.62})`)
          context.strokeStyle = gradient
          context.lineWidth = 0.8
          context.beginPath()
          context.moveTo(particle.x, particle.y)
          context.lineTo(nextParticle.x, nextParticle.y)
          context.stroke()
        }
      }

      context.beginPath()
      context.fillStyle = `hsla(${particle.hue}, 100%, 76%, ${particle.alpha + pulse * 0.08})`
      context.shadowColor = `hsla(${particle.hue}, 100%, 70%, 0.32)`
      context.shadowBlur = 12
      context.arc(particle.x, particle.y, particle.radius, 0, Math.PI * 2)
      context.fill()
      context.shadowBlur = 0
    })

    if (pointer.active) {
      const halo = context.createRadialGradient(pointer.x, pointer.y, 0, pointer.x, pointer.y, 220)
      halo.addColorStop(0, 'rgba(139, 177, 255, 0.16)')
      halo.addColorStop(0.42, 'rgba(255, 162, 102, 0.07)')
      halo.addColorStop(1, 'rgba(255, 162, 102, 0)')
      context.fillStyle = halo
      context.beginPath()
      context.arc(pointer.x, pointer.y, 220, 0, Math.PI * 2)
      context.fill()
    }

    particleFrame = window.requestAnimationFrame(drawParticles)
  }

  resizeCanvas()
  drawParticles()

  window.addEventListener('resize', resizeCanvas)
  window.addEventListener('pointermove', trackPointer)
  window.addEventListener('pointerleave', clearPointer)

  return () => {
    window.removeEventListener('resize', resizeCanvas)
    window.removeEventListener('pointermove', trackPointer)
    window.removeEventListener('pointerleave', clearPointer)
  }
}

let stopParticleField = null

onMounted(() => {
  startWelcomeTyping()
  stopParticleField = startParticleField()
})

onBeforeUnmount(() => {
  if (animationTimer) {
    window.clearTimeout(animationTimer)
  }

  if (particleFrame) {
    window.cancelAnimationFrame(particleFrame)
  }

  if (stopParticleField) {
    stopParticleField()
  }
})
</script>

<template>
  <main class="home-view">
    <canvas ref="particleCanvas" class="particle-canvas" aria-hidden="true"></canvas>
    <div class="noise"></div>

    <div class="page-shell">
      <AppNavbar :links="navLinks" />

      <section class="hero-stage">
        <div class="welcome-stage">
          <span class="welcome-ghost">{{ typedWord || 'hihi' }}</span>
          <span class="welcome-ghost welcome-ghost-alt">{{ typedWord || '你好～' }}</span>
          <h1 class="welcome-word">
            {{ typedWord }}<span class="cursor"></span>
          </h1>
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
    radial-gradient(circle at top, rgba(18, 36, 76, 0.48), transparent 36%),
    linear-gradient(180deg, #040816 0%, #08111f 48%, #0a0f1c 100%);
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

.home-view {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.86;
  pointer-events: none;
}

.noise {
  position: absolute;
  inset: 0;
  z-index: 1;
  opacity: 0.18;
  background-image:
    radial-gradient(rgba(255, 255, 255, 0.12) 0.7px, transparent 0.7px),
    radial-gradient(rgba(255, 255, 255, 0.08) 0.7px, transparent 0.7px);
  background-position: 0 0, 18px 18px;
  background-size: 36px 36px;
  pointer-events: none;
}

.page-shell {
  position: relative;
  z-index: 2;
  width: min(1200px, calc(100% - 32px));
  margin: 0 auto;
  padding: 24px 0 52px;
}

.hero-stage {
  display: flex;
  min-height: calc(100vh - 120px);
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0 12px;
  text-align: center;
  margin: 0;
}

.welcome-stage {
  position: relative;
  min-height: 196px;
  display: flex;
  align-items: center;
  justify-content: center;
  isolation: isolate;
}

.welcome-stage::before {
  content: '';
  position: absolute;
  width: min(72vw, 760px);
  height: min(24vw, 220px);
  border-radius: 999px;
  background: radial-gradient(circle, rgba(86, 125, 255, 0.2), transparent 70%);
  filter: blur(26px);
  opacity: 0.9;
}

.welcome-ghost {
  position: absolute;
  margin: 0;
  font-size: clamp(3.8rem, 11vw, 9rem);
  line-height: 0.9;
  letter-spacing: -0.06em;
  color: rgba(113, 154, 255, 0.18);
  transform: translate(-8px, 6px);
  filter: blur(2px);
  animation: driftA 5s ease-in-out infinite;
  pointer-events: none;
}

.welcome-ghost-alt {
  color: rgba(255, 160, 110, 0.14);
  transform: translate(10px, -6px);
  animation: driftB 4.4s ease-in-out infinite;
}

.welcome-word {
  position: relative;
  z-index: 1;
  margin: 0;
  min-width: 8ch;
  font-size: clamp(3.8rem, 11vw, 9rem);
  line-height: 0.9;
  letter-spacing: -0.06em;
  color: #eef5ff;
  text-shadow:
    0 0 12px rgba(94, 137, 255, 0.16),
    0 0 30px rgba(255, 182, 96, 0.08);
  animation: shimmer 3.6s linear infinite;
}

.cursor {
  display: inline-block;
  width: 0.65ch;
  height: 1.1em;
  margin-left: 4px;
  vertical-align: text-bottom;
  background: linear-gradient(180deg, #8cb0ff, #ffe29f);
  box-shadow: 0 0 14px rgba(140, 176, 255, 0.28);
  animation: blink 0.9s steps(1) infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

@keyframes driftA {
  0%,
  100% {
    transform: translate(-8px, 6px);
  }

  50% {
    transform: translate(-2px, -3px);
  }
}

@keyframes driftB {
  0%,
  100% {
    transform: translate(10px, -6px);
  }

  50% {
    transform: translate(4px, 4px);
  }
}

@keyframes shimmer {
  0% {
    filter: brightness(1) saturate(1);
  }

  45% {
    filter: brightness(1.04) saturate(1.08);
  }

  50% {
    filter: brightness(1.16) saturate(1.18);
  }

  55% {
    filter: brightness(1.03) saturate(1.06);
  }

  100% {
    filter: brightness(1) saturate(1);
  }
}

@media (max-width: 980px) {
  .hero-stage {
    min-height: calc(100vh - 104px);
    padding-top: 18px;
  }

  .welcome-stage {
    min-height: 148px;
  }
}

@media (max-width: 640px) {
  .page-shell {
    width: min(100% - 24px, 1200px);
    padding-top: 18px;
  }

  .hero-stage {
    min-height: calc(100vh - 92px);
    padding: 18px 0 8px;
  }

  .welcome-stage {
    min-height: 128px;
  }

  .welcome-word,
  .welcome-ghost {
    font-size: clamp(3rem, 16vw, 4.8rem);
  }
}
</style>
