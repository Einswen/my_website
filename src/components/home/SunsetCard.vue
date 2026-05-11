<script setup>
defineProps({
  title: {
    type: String,
    default: '',
  },
  score: {
    type: [Number, String],
    default: '--',
  },
  status: {
    type: String,
    default: '',
  },
  description: {
    type: String,
    default: '',
  },
  metrics: {
    type: Array,
    default: () => [],
  },
  locationName: {
    type: String,
    default: '',
  },
  sunsetTime: {
    type: String,
    default: '',
  },
  eventTimeLabel: {
    type: String,
    default: '日落',
  },
  snapshotTime: {
    type: String,
    default: '',
  },
  timezone: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
  empty: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <article class="sunset-card">
    <div class="card-head">
      <div>
        <p class="title">{{ title }}</p>
        <h3>{{ status }}</h3>
      </div>
      <div class="score-badge">{{ score }}</div>
    </div>

    <div v-if="loading" class="state-panel">
      <p>正在获取实时定位和天气数据...</p>
    </div>
    <div v-else-if="error" class="state-panel state-error">
      <p>{{ error }}</p>
    </div>
    <div v-else-if="empty" class="state-panel">
      <p>输入城市并点击查询后，这里会显示实时朝霞或晚霞预测数据。</p>
    </div>
    <template v-else>
      <p class="description">{{ description }}</p>

      <div class="meta-list">
        <div v-if="locationName" class="meta-item">{{ locationName }}</div>
        <div v-if="sunsetTime" class="meta-item">{{ eventTimeLabel }} {{ sunsetTime }}</div>
        <div v-if="snapshotTime" class="meta-item">采样 {{ snapshotTime }}</div>
        <div v-if="timezone" class="meta-item">{{ timezone }}</div>
      </div>

      <div class="metric-list">
        <div v-for="metric in metrics" :key="metric.label" class="metric-item">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>
    </template>
  </article>
</template>

<style scoped>
.sunset-card {
  padding: 24px;
  border-radius: 26px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.03)),
    rgba(8, 11, 22, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: start;
}

.title,
.metric-item span {
  margin: 0;
  color: #f6a56d;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.card-head h3 {
  margin: 10px 0 0;
  font-size: 1.5rem;
  line-height: 1.15;
}

.score-badge {
  min-width: 76px;
  padding: 16px 14px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(255, 164, 116, 0.2), rgba(106, 110, 255, 0.18));
  color: #ffffff;
  font-size: 2rem;
  font-weight: 800;
  text-align: center;
}

.description {
  margin: 18px 0 0;
  color: rgba(232, 236, 245, 0.74);
  line-height: 1.75;
}

.state-panel {
  margin-top: 18px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.state-panel p {
  margin: 0;
  color: rgba(232, 236, 245, 0.74);
  line-height: 1.7;
}

.state-error {
  border-color: rgba(255, 120, 120, 0.24);
  background: rgba(255, 97, 97, 0.08);
}

.meta-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.meta-item {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  color: rgba(246, 247, 251, 0.78);
  font-size: 0.86rem;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.metric-item {
  padding: 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.metric-item strong {
  display: block;
  margin-top: 8px;
  color: #ffffff;
  font-size: 0.98rem;
}

@media (max-width: 720px) {
  .metric-list {
    grid-template-columns: 1fr;
  }
}
</style>
