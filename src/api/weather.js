const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export async function fetchWeatherForecast(options = {}) {
  const params = new URLSearchParams({
    latitude: String(options.latitude),
    longitude: String(options.longitude),
    timezone: options.timezone ?? 'auto',
    target: options.target ?? 'today-sunset',
    forecastDays: String(options.forecastDays ?? 2),
  })
  const response = await fetch(`${API_BASE_URL}/forecast?${params.toString()}`, {
    signal: options.signal,
  })

  if (!response.ok) {
    let payload

    try {
      payload = await response.json()
    } catch {
      throw new Error('暂时无法获取晚霞预测数据。')
    }

    const detail = payload?.detail
    throw new Error(typeof detail === 'string' ? detail : '暂时无法获取晚霞预测数据。')
  }

  return response.json()
}
