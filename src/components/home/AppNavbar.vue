<script setup>
import { RouterLink } from 'vue-router'
import { computed, ref } from 'vue'

const props = defineProps({
  brand: {
    type: String,
    default: '',
  },
  links: {
    type: Array,
    default: () => [],
  },
})

const leftLinks = computed(() => props.links.filter((link) => link.align === 'left'))
const rightLinks = computed(() => props.links.filter((link) => link.align !== 'left'))
const allLinks = computed(() => [...leftLinks.value, ...rightLinks.value])
const activeEmail = ref('')

function toggleEmail(email) {
  activeEmail.value = activeEmail.value === email ? '' : email
}
</script>

<template>
  <header class="navbar">
    <nav class="nav-links nav-links-left desktop-links" aria-label="Primary navigation">
      <template v-for="link in leftLinks" :key="link.label">
        <RouterLink v-if="link.to" :to="link.to">
          {{ link.label }}
        </RouterLink>
        <a v-else :href="link.href">
          {{ link.label }}
        </a>
      </template>
    </nav>

    <nav class="nav-links nav-links-right desktop-links" aria-label="Secondary navigation">
      <template v-for="link in rightLinks" :key="link.label">
        <RouterLink v-if="link.to" :to="link.to">
          {{ link.label }}
        </RouterLink>
        <button v-else-if="link.email" class="nav-button" type="button" @click="toggleEmail(link.email)">
          {{ link.label }}
        </button>
        <a v-else :href="link.href">
          {{ link.label }}
        </a>
      </template>

      <div v-if="activeEmail" class="email-popover" role="status">
        <span>{{ activeEmail }}</span>
      </div>
    </nav>

    <nav class="nav-links nav-links-mobile" aria-label="Mobile navigation">
      <template v-for="link in allLinks" :key="link.label">
        <RouterLink v-if="link.to" :to="link.to">
          {{ link.label }}
        </RouterLink>
        <button v-else-if="link.email" class="nav-button" type="button" @click="toggleEmail(link.email)">
          {{ link.label }}
        </button>
        <a v-else :href="link.href">
          {{ link.label }}
        </a>
      </template>

      <div v-if="activeEmail" class="email-popover email-popover-mobile" role="status">
        <span>{{ activeEmail }}</span>
      </div>
    </nav>
  </header>
</template>

<style scoped>
.navbar {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 22px;
  background: rgba(6, 11, 23, 0.55);
  backdrop-filter: blur(14px);
}

.nav-links {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.nav-links-left {
  justify-content: flex-start;
}

.nav-links-right {
  justify-content: flex-end;
  margin-left: auto;
}

.nav-links-mobile {
  display: none;
}

.nav-links a,
.nav-button {
  padding: 10px 14px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: rgba(246, 247, 251, 0.72);
  font: inherit;
  text-decoration: none;
  cursor: pointer;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    transform 0.2s ease;
}

.nav-links :deep(.router-link-active),
.nav-links a:hover,
.nav-button:hover {
  color: #ffffff;
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-1px);
}

.email-popover {
  position: absolute;
  top: calc(100% + 10px);
  right: 18px;
  padding: 12px 15px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  background: rgba(8, 13, 27, 0.88);
  color: #ffffff;
  box-shadow: 0 18px 52px rgba(0, 0, 0, 0.32);
  backdrop-filter: blur(18px);
  white-space: nowrap;
}

.email-popover::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 24px;
  width: 12px;
  height: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-left: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(8, 13, 27, 0.88);
  transform: rotate(45deg);
}

@media (max-width: 720px) {
  .navbar {
    display: block;
    padding: 10px 12px;
    overflow: visible;
  }

  .desktop-links {
    display: none;
  }

  .nav-links-mobile {
    display: flex;
    width: 100%;
    flex-wrap: nowrap;
    gap: 8px;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .nav-links-mobile::-webkit-scrollbar {
    display: none;
  }

  .nav-links a,
  .nav-button {
    padding: 9px 12px;
    white-space: nowrap;
  }

  .email-popover-mobile {
    position: fixed;
    top: 78px;
    right: 12px;
    z-index: 10;
    max-width: calc(100vw - 48px);
    overflow-wrap: anywhere;
    white-space: normal;
  }
}
</style>
