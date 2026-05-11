<script setup>
const emit = defineEmits(['search', 'update:searchValue', 'update:targetValue'])

defineProps({
  eyebrow: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  primaryLabel: {
    type: String,
    default: '',
  },
  primaryHref: {
    type: String,
    default: '#',
  },
  secondaryLabel: {
    type: String,
    default: '',
  },
  secondaryHref: {
    type: String,
    default: '#',
  },
  stats: {
    type: Array,
    default: () => [],
  },
  previewTitle: {
    type: String,
    default: '',
  },
  previewScore: {
    type: [Number, String],
    default: '--',
  },
  previewScoreSuffix: {
    type: String,
    default: '',
  },
  previewProgress: {
    type: Number,
    default: 0,
  },
  previewSummary: {
    type: String,
    default: '',
  },
  searchValue: {
    type: String,
    default: '',
  },
  targetValue: {
    type: String,
    default: '',
  },
  targetOptions: {
    type: Array,
    default: () => [],
  },
  targetLabel: {
    type: String,
    default: '预测类型',
  },
  searchPlaceholder: {
    type: String,
    default: '搜索城市',
  },
  searchButtonLabel: {
    type: String,
    default: '查询',
  },
  searchDisabled: {
    type: Boolean,
    default: false,
  },
  searchStatus: {
    type: String,
    default: '',
  },
})
</script>

<template>
  <section class="hero-section">
    <div class="hero-copy">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="description" class="description">{{ description }}</p>

      <form class="search-form" @submit.prevent="emit('search')">
        <label class="search-label" for="city-search">城市搜索</label>
        <div class="search-row">
          <select
            v-if="targetOptions.length"
            class="target-select"
            :aria-label="targetLabel"
            :value="targetValue"
            :disabled="searchDisabled"
            @change="emit('update:targetValue', $event.target.value)"
          >
            <option v-for="option in targetOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <input
            id="city-search"
            class="search-input"
            :value="searchValue"
            :placeholder="searchPlaceholder"
            :disabled="searchDisabled"
            @input="emit('update:searchValue', $event.target.value)"
          />
          <button class="search-button" type="submit" :disabled="searchDisabled">
            {{ searchButtonLabel }}
          </button>
        </div>
        <p v-if="searchStatus" class="search-status">{{ searchStatus }}</p>
      </form>

      <div v-if="primaryLabel || secondaryLabel" class="hero-actions">
        <a v-if="primaryLabel" class="primary-action" :href="primaryHref">{{ primaryLabel }}</a>
        <a v-if="secondaryLabel" class="secondary-action" :href="secondaryHref">{{ secondaryLabel }}</a>
      </div>

      <div v-if="stats.length" class="hero-stats">
        <article v-for="stat in stats" :key="stat.label" class="stat-item">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </article>
      </div>
    </div>

    <aside class="hero-preview">
      <p class="preview-label">{{ previewTitle }}</p>
      <div class="preview-score">
        <span>{{ previewScore }}</span>
        <small>{{ previewScoreSuffix }}</small>
      </div>
      <p class="preview-summary">{{ previewSummary }}</p>
      <div class="preview-track">
        <div class="preview-track-fill" :style="{ width: `${previewProgress}%` }"></div>
      </div>
    </aside>
  </section>
</template>

<style scoped>
.hero-section {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr;
  gap: 24px;
  margin-top: 24px;
  padding: 34px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 32px;
  background:
    linear-gradient(140deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.03)),
    radial-gradient(circle at top right, rgba(103, 105, 255, 0.18), transparent 28%);
  box-shadow: 0 28px 80px rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(22px);
}

.eyebrow,
.preview-label,
.stat-item span {
  color: #f6a56d;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 12px 0 0;
  max-width: 10ch;
  font-size: clamp(3rem, 7vw, 5.6rem);
  line-height: 0.96;
}

.description {
  max-width: 620px;
  margin: 22px 0 0;
  color: rgba(232, 236, 245, 0.78);
  font-size: 1.02rem;
  line-height: 1.85;
}

.search-form {
  margin-top: 28px;
}

.search-label {
  display: inline-block;
  color: #f6a56d;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.search-row {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

.target-select,
.search-input {
  flex: 1;
  min-height: 52px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.05);
  color: #f6f7fb;
  font: inherit;
  outline: none;
}

.target-select {
  flex: 0 0 160px;
  cursor: pointer;
}

.search-input::placeholder {
  color: rgba(246, 247, 251, 0.4);
}

.target-select:focus,
.search-input:focus {
  border-color: rgba(255, 191, 132, 0.45);
  box-shadow: 0 0 0 4px rgba(255, 156, 104, 0.12);
}

.search-button {
  min-width: 132px;
  min-height: 52px;
  padding: 0 18px;
  border: 0;
  border-radius: 18px;
  background: linear-gradient(135deg, #ff9c68, #ffd06f);
  color: #08101d;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
}

.search-button:disabled {
  cursor: wait;
  opacity: 0.72;
}

.search-status {
  margin: 12px 0 0;
  color: rgba(232, 236, 245, 0.74);
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 28px;
}

.hero-actions a {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 18px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 700;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.primary-action {
  color: #08101d;
  background: linear-gradient(135deg, #ff9c68, #ffd06f);
  box-shadow: 0 16px 34px rgba(255, 161, 87, 0.28);
}

.secondary-action {
  color: #f6f7fb;
  background: rgba(255, 255, 255, 0.08);
}

.hero-actions a:hover {
  transform: translateY(-2px);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.stat-item {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-item strong {
  display: block;
  margin-top: 10px;
  font-size: 1rem;
  color: #ffffff;
}

.hero-preview {
  position: relative;
  overflow: hidden;
  padding: 26px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(255, 165, 115, 0.24), transparent 28%),
    rgba(8, 12, 24, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.hero-preview::after {
  content: '';
  position: absolute;
  right: -32px;
  bottom: -32px;
  width: 140px;
  height: 140px;
  border-radius: 999px;
  background: rgba(98, 108, 255, 0.18);
  filter: blur(10px);
}

.preview-score {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  margin-top: 14px;
}

.preview-score span {
  font-size: clamp(4rem, 10vw, 6rem);
  font-weight: 800;
  line-height: 0.9;
}

.preview-score small {
  margin-bottom: 10px;
  color: rgba(246, 247, 251, 0.64);
  font-size: 1rem;
}

.preview-summary {
  position: relative;
  z-index: 1;
  margin: 18px 0 0;
  max-width: 28ch;
  color: rgba(232, 236, 245, 0.78);
  line-height: 1.7;
}

.preview-track {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 10px;
  margin-top: 24px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}

.preview-track-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #ff9966, #ffd16a 55%, #8e8dff);
}

@media (max-width: 960px) {
  .hero-section,
  .hero-stats {
    grid-template-columns: 1fr;
  }

  .hero-section {
    padding: 24px;
  }
}

@media (max-width: 640px) {
  .search-row {
    flex-direction: column;
  }

  .target-select {
    flex: auto;
    width: 100%;
  }

  .search-button {
    width: 100%;
  }
}
</style>
