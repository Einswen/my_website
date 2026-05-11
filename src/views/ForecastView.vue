<script setup>
import { computed, ref } from 'vue'

import { searchCityLocation } from '../api/geocoding'
import { fetchWeatherForecast } from '../api/weather'
import AppNavbar from '../components/home/AppNavbar.vue'
import HeroSection from '../components/home/HeroSection.vue'
import SunsetCard from '../components/home/SunsetCard.vue'

const DEFAULT_CITY = 'Xiamen'
const DEFAULT_TARGET = 'today-sunset'

const navLinks = [
  { label: '首页', to: '/', align: 'left' },
  { label: '晚霞预测', to: '/forecast' },
  { label: '留言板', to: '/messages' },
  { label: '联系我', email: 'EinswenL@gmail.com' },
]

const searchQuery = ref(DEFAULT_CITY)
const selectedTarget = ref(DEFAULT_TARGET)
const location = ref(null)
const forecast = ref(null)
const loading = ref(false)
const error = ref('')
const emptyMessage = ref('')
const lastSearchedCity = ref(DEFAULT_CITY)
const lastTarget = ref(DEFAULT_TARGET)

const targetOptions = [
  { label: '今日朝霞', value: 'today-sunrise', eventLabel: '日出时间', eventName: '朝霞' },
  { label: '今日晚霞', value: 'today-sunset', eventLabel: '日落时间', eventName: '晚霞' },
  { label: '明日朝霞', value: 'tomorrow-sunrise', eventLabel: '日出时间', eventName: '朝霞' },
  { label: '明日晚霞', value: 'tomorrow-sunset', eventLabel: '日落时间', eventName: '晚霞' },
]

let activeRequestId = 0

function formatLocationName(value) {
  if (!value) {
    return ''
  }

  const parts = [value.name, value.admin1, value.country].filter(Boolean)

  return [...new Set(parts)].join(', ')
}

function formatApiDateTime(isoString) {
  return isoString ? isoString.replace('T', ' ') : ''
}

const selectedTargetOption = computed(
  () => targetOptions.find((option) => option.value === selectedTarget.value) ?? targetOptions[1],
)

const lastTargetOption = computed(
  () => targetOptions.find((option) => option.value === lastTarget.value) ?? selectedTargetOption.value,
)

const opticalState = computed(() => {
  if (!forecast.value) {
    return null
  }

  return forecast.value.opticalState ?? null
})

const heroStats = computed(() => [
  {
    label: '城市',
    value: location.value ? formatLocationName(location.value) : '等待查询',
  },
  {
    label: '时区',
    value: forecast.value?.timezone ?? location.value?.timezone ?? '待获取',
  },
  {
    label: '状态',
    value: loading.value ? '加载中' : forecast.value ? '已就绪' : error.value ? '错误' : '空闲',
  },
  {
    label: '类型',
    value: lastTargetOption.value.label,
  },
])

const heroPreviewScore = computed(() => (opticalState.value === null ? '--' : opticalState.value.score))
const heroPreviewProgress = computed(() => opticalState.value?.score ?? 0)

const heroPreviewSummary = computed(() => {
  if (loading.value) {
    return `正在加载 ${lastSearchedCity.value} 的${lastTargetOption.value.label}...`
  }

  if (error.value) {
    return error.value
  }

  if (emptyMessage.value) {
    return emptyMessage.value
  }

  if (!forecast.value) {
    return '准备好后点击查询，加载朝霞或晚霞预测。'
  }

  return opticalState.value?.label ?? '点击查询以获取预测。'
})

const heroSearchStatus = computed(() => {
  if (loading.value) {
    return `正在加载 ${lastSearchedCity.value} 的${lastTargetOption.value.label}...`
  }

  if (error.value) {
    return error.value
  }

  if (emptyMessage.value) {
    return emptyMessage.value
  }

  if (location.value) {
    return `正在显示 ${formatLocationName(location.value)} 的${lastTargetOption.value.label}数据。`
  }

  return '输入城市后点击查询。'
})

const sunsetDescription = computed(() => {
  if (!forecast.value || !opticalState.value) {
    return '等待数据中。'
  }

  return opticalState.value.suitable
    ? `适合观察${lastTargetOption.value.eventName}。`
    : `当前不太适合观察${lastTargetOption.value.eventName}。`
})

const sunsetMetrics = computed(() => {
  if (!forecast.value || !opticalState.value) {
    return []
  }

  return [
    { label: '高云', value: `${forecast.value.cloudCoverHigh}%` },
    { label: '中云', value: `${forecast.value.cloudCoverMid}%` },
    { label: '低云', value: `${forecast.value.cloudCoverLow}%` },
    { label: 'AOD', value: `${forecast.value.aerosolOpticalDepth ?? '--'}` },
    { label: '边界距离', value: `${forecast.value.boundaryDistanceKm} km` },
    { label: '能见度', value: `${Math.round((forecast.value.visibility ?? 0) / 1000)} km` },
  ]
})

const rawDataRows = computed(() => {
  if (!forecast.value) {
    return []
  }

  return [
    { label: lastTargetOption.value.eventLabel, value: formatApiDateTime(forecast.value.eventTime) },
    { label: '采样时间', value: formatApiDateTime(forecast.value.snapshotTime) },
    { label: '太阳方位', value: `${Math.round(forecast.value.solarAzimuth)}°` },
    { label: '边界距离', value: `${forecast.value.boundaryDistanceKm} km` },
    { label: 'AOD', value: `${forecast.value.aerosolOpticalDepth ?? '--'}` },
    { label: '坐标', value: `${forecast.value.latitude.toFixed(2)}, ${forecast.value.longitude.toFixed(2)}` },
  ]
})

async function runCitySearch() {
  const cityName = searchQuery.value.trim()

  if (!cityName) {
    error.value = '请输入城市名后再查询。'
    emptyMessage.value = ''
    return
  }

  const requestId = ++activeRequestId

  loading.value = true
  error.value = ''
  emptyMessage.value = ''
  lastSearchedCity.value = cityName
  lastTarget.value = selectedTarget.value

  try {
    const resolvedLocation = await searchCityLocation(cityName)

    if (requestId !== activeRequestId) {
      return
    }

    if (!resolvedLocation) {
      location.value = null
      forecast.value = null
      emptyMessage.value = `没有找到与“${cityName}”匹配的城市。`
      return
    }

    const liveForecast = await fetchWeatherForecast({
      latitude: resolvedLocation.latitude,
      longitude: resolvedLocation.longitude,
      timezone: resolvedLocation.timezone,
      target: selectedTarget.value,
    })

    if (requestId !== activeRequestId) {
      return
    }

    location.value = resolvedLocation
    forecast.value = liveForecast
  } catch (requestError) {
    if (requestId !== activeRequestId) {
      return
    }

    location.value = null
    forecast.value = null
    error.value = requestError instanceof Error ? requestError.message : '加载数据时出现问题。'
  } finally {
    if (requestId === activeRequestId) {
      loading.value = false
    }
  }
}
</script>

<template>
  <main id="top" class="forecast-view">
    <div class="background-orb background-orb-left"></div>
    <div class="background-orb background-orb-right"></div>

    <div class="page-shell">
      <AppNavbar :links="navLinks" />

      <HeroSection
        eyebrow="朝霞 / 晚霞预测"
        title="朝霞 / 晚霞预测"
        :stats="heroStats"
        preview-title="评分"
        preview-score-suffix="%"
        :preview-score="heroPreviewScore"
        :preview-progress="heroPreviewProgress"
        :preview-summary="heroPreviewSummary"
        :search-value="searchQuery"
        :target-value="selectedTarget"
        :target-options="targetOptions"
        search-placeholder="搜索城市，例如厦门或东京"
        :search-button-label="loading ? '查询中...' : '查询'"
        :search-disabled="loading"
        :search-status="heroSearchStatus"
        @search="runCitySearch"
        @update:search-value="searchQuery = $event"
        @update:target-value="selectedTarget = $event"
      />

      <section id="forecast" class="content-section">
        <div class="section-heading">
          <p>实时数据</p>
          <h2>预测结果</h2>
        </div>

        <div class="card-grid">
          <SunsetCard
            title="天空光学状态"
            :score="heroPreviewScore"
            :status="opticalState?.label ?? '等待实时采样'"
            :description="sunsetDescription"
            :metrics="sunsetMetrics"
            :location-name="formatLocationName(location)"
            :sunset-time="formatApiDateTime(forecast?.eventTime)"
            :event-time-label="lastTargetOption.eventLabel"
            :snapshot-time="formatApiDateTime(forecast?.snapshotTime)"
            :timezone="forecast?.timezone ?? ''"
            :loading="loading"
            :error="error"
            :empty="!loading && !error && !forecast"
          />

          <article class="insight-card">
            <p class="insight-label">原始数据</p>
            <h3>当前快照</h3>
            <div class="insight-list">
              <div v-for="row in rawDataRows" :key="row.label" class="insight-item">
                <span>{{ row.label }}</span>
                <strong>{{ row.value }}</strong>
              </div>
            </div>
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
    radial-gradient(circle at 15% 20%, rgba(255, 126, 95, 0.2), transparent 28%),
    radial-gradient(circle at 85% 15%, rgba(74, 108, 247, 0.22), transparent 32%),
    linear-gradient(180deg, #07111f 0%, #0b1220 46%, #130f21 100%);
  color: #f6f7fb;
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

.forecast-view {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
}

.page-shell {
  position: relative;
  z-index: 1;
  width: min(1180px, calc(100% - 32px));
  margin: 0 auto;
  padding: 24px 0 64px;
}

.background-orb {
  position: absolute;
  border-radius: 999px;
  filter: blur(30px);
  opacity: 0.8;
}

.background-orb-left {
  top: 120px;
  left: -120px;
  width: 280px;
  height: 280px;
  background: rgba(255, 114, 76, 0.2);
}

.background-orb-right {
  top: 80px;
  right: -80px;
  width: 240px;
  height: 240px;
  background: rgba(84, 118, 255, 0.18);
}

.content-section,
.workflow-panel,
.journal-panel {
  margin-top: 24px;
  padding: 28px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 28px;
  background: rgba(10, 14, 28, 0.56);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(18px);
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: end;
  margin-bottom: 24px;
}

.section-heading p,
.section-kicker,
.insight-label {
  margin: 0 0 10px;
  color: #f6a56d;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.section-heading h2,
.insight-card h3 {
  margin: 0;
}

.section-heading h2 {
  font-size: clamp(1.8rem, 3.4vw, 2.6rem);
  line-height: 1.08;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.insight-card {
  padding: 24px;
  border-radius: 26px;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.03)),
    rgba(8, 11, 22, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.insight-card h3 {
  font-size: 1.5rem;
}

.insight-list {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.insight-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.insight-item span {
  display: block;
  color: rgba(232, 236, 245, 0.62);
  font-size: 0.85rem;
}

.insight-item strong {
  display: block;
  margin-top: 8px;
  color: #ffffff;
  line-height: 1.5;
}

@media (max-width: 960px) {
  .page-shell {
    width: min(100% - 24px, 1180px);
    padding-bottom: 40px;
  }

  .card-grid {
    grid-template-columns: 1fr;
  }

  .section-heading {
    flex-direction: column;
    align-items: start;
  }

  .content-section,
  .insight-card {
    padding: 22px;
  }
}
</style>
