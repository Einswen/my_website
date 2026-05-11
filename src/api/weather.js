const FORECAST_ENDPOINT = 'https://api.open-meteo.com/v1/forecast'
const AIR_QUALITY_ENDPOINT = 'https://air-quality-api.open-meteo.com/v1/air-quality'
const CURRENT_FIELDS = ['temperature_2m']
const HOURLY_FIELDS = [
  'temperature_2m',
  'relative_humidity_2m',
  'cloud_cover',
  'cloud_cover_low',
  'cloud_cover_mid',
  'cloud_cover_high',
  'wind_speed_10m',
  'visibility',
]
const DAILY_FIELDS = ['sunrise', 'sunset']
const AIR_QUALITY_FIELDS = ['aerosol_optical_depth']
const BOUNDARY_SAMPLE_DISTANCES_KM = [50, 100, 150, 200, 250, 300, 350, 400, 450, 500]
const EARTH_RADIUS_KM = 6371
const HIGH_CLOUD_ZERO_THRESHOLD = 10

function parseApiDateParts(isoString) {
  const [datePart = '', timePart = '00:00'] = isoString.split('T')
  const [year, month, day] = datePart.split('-').map(Number)
  const [hour = 0, minute = 0] = timePart.slice(0, 5).split(':').map(Number)

  return { year, month, day, hour, minute }
}

function parseIsoMinutes(isoString) {
  const { year, month, day, hour, minute } = parseApiDateParts(isoString)

  return Date.UTC(year, month - 1, day, hour, minute) / 60000
}

function parseApiTimeToUtcDate(isoString, utcOffsetSeconds) {
  const { year, month, day, hour, minute } = parseApiDateParts(isoString)
  const utcTimestamp = Date.UTC(year, month - 1, day, hour, minute) - utcOffsetSeconds * 1000

  return new Date(utcTimestamp)
}

function normalizeDegrees(value) {
  return ((value % 360) + 360) % 360
}

function degreesToRadians(value) {
  return (value * Math.PI) / 180
}

function radiansToDegrees(value) {
  return (value * 180) / Math.PI
}

function calculateSolarAzimuth(latitude, longitude, localIsoTime, utcOffsetSeconds) {
  const utcDate = parseApiTimeToUtcDate(localIsoTime, utcOffsetSeconds)
  const julianDay = utcDate.getTime() / 86400000 + 2440587.5
  const julianCentury = (julianDay - 2451545) / 36525

  const geomMeanLongSun = normalizeDegrees(
    280.46646 + julianCentury * (36000.76983 + julianCentury * 0.0003032),
  )
  const geomMeanAnomalySun = normalizeDegrees(
    357.52911 + julianCentury * (35999.05029 - 0.0001537 * julianCentury),
  )
  const eccentricityEarthOrbit =
    0.016708634 - julianCentury * (0.000042037 + 0.0000001267 * julianCentury)
  const sunEqOfCenter =
    Math.sin(degreesToRadians(geomMeanAnomalySun)) *
      (1.914602 - julianCentury * (0.004817 + 0.000014 * julianCentury)) +
    Math.sin(degreesToRadians(2 * geomMeanAnomalySun)) * (0.019993 - 0.000101 * julianCentury) +
    Math.sin(degreesToRadians(3 * geomMeanAnomalySun)) * 0.000289
  const sunTrueLong = geomMeanLongSun + sunEqOfCenter
  const omega = 125.04 - 1934.136 * julianCentury
  const sunAppLong = sunTrueLong - 0.00569 - 0.00478 * Math.sin(degreesToRadians(omega))
  const meanObliqEcliptic =
    23 +
    (26 +
      (21.448 -
        julianCentury * (46.815 + julianCentury * (0.00059 - julianCentury * 0.001813))) /
        60) /
      60
  const obliqCorr = meanObliqEcliptic + 0.00256 * Math.cos(degreesToRadians(omega))
  const sunDeclination = radiansToDegrees(
    Math.asin(
      Math.sin(degreesToRadians(obliqCorr)) * Math.sin(degreesToRadians(sunAppLong)),
    ),
  )
  const y = Math.tan(degreesToRadians(obliqCorr / 2)) ** 2
  const equationOfTime =
    4 *
    radiansToDegrees(
      y * Math.sin(2 * degreesToRadians(geomMeanLongSun)) -
        2 * eccentricityEarthOrbit * Math.sin(degreesToRadians(geomMeanAnomalySun)) +
        4 *
          eccentricityEarthOrbit *
          y *
          Math.sin(degreesToRadians(geomMeanAnomalySun)) *
          Math.cos(2 * degreesToRadians(geomMeanLongSun)) -
        0.5 * y * y * Math.sin(4 * degreesToRadians(geomMeanLongSun)) -
        1.25 *
          eccentricityEarthOrbit *
          eccentricityEarthOrbit *
          Math.sin(2 * degreesToRadians(geomMeanAnomalySun)),
    )
  const utcMinutes =
    utcDate.getUTCHours() * 60 + utcDate.getUTCMinutes() + utcDate.getUTCSeconds() / 60
  const trueSolarTime = ((utcMinutes + equationOfTime + 4 * longitude) % 1440 + 1440) % 1440
  const hourAngle = trueSolarTime / 4 < 0 ? trueSolarTime / 4 + 180 : trueSolarTime / 4 - 180
  const hourAngleRad = degreesToRadians(hourAngle)
  const latitudeRad = degreesToRadians(latitude)
  const declinationRad = degreesToRadians(sunDeclination)
  const azimuth = radiansToDegrees(
    Math.atan2(
      Math.sin(hourAngleRad),
      Math.cos(hourAngleRad) * Math.sin(latitudeRad) -
        Math.tan(declinationRad) * Math.cos(latitudeRad),
    ),
  )

  return normalizeDegrees(azimuth + 180)
}

function destinationPoint(latitude, longitude, bearingDegrees, distanceKm) {
  const bearingRad = degreesToRadians(bearingDegrees)
  const latRad = degreesToRadians(latitude)
  const lonRad = degreesToRadians(longitude)
  const angularDistance = distanceKm / EARTH_RADIUS_KM
  const destinationLat = Math.asin(
    Math.sin(latRad) * Math.cos(angularDistance) +
      Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(bearingRad),
  )
  const destinationLon =
    lonRad +
    Math.atan2(
      Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(latRad),
      Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(destinationLat),
    )

  return {
    latitude: radiansToDegrees(destinationLat),
    longitude: radiansToDegrees(destinationLon),
  }
}

function getTargetEventTime(daily, target = 'today-sunset') {
  const [dayValue, eventValue] = target.split('-')
  const dayIndex = dayValue === 'tomorrow' ? 1 : 0
  const eventKey = eventValue === 'sunrise' ? 'sunrise' : 'sunset'
  const eventTime = daily?.[eventKey]?.[dayIndex]

  if (!eventTime) {
    return null
  }

  return {
    dayIndex,
    eventKey,
    eventTime,
    target,
  }
}

function findNearestWeatherIndex(hourlyTimes, targetTime) {
  if (!hourlyTimes.length || !targetTime) {
    return -1
  }

  const targetMinutes = parseIsoMinutes(targetTime)

  return hourlyTimes.reduce((closestIndex, time, index) => {
    const closestDifference = Math.abs(parseIsoMinutes(hourlyTimes[closestIndex]) - targetMinutes)
    const currentDifference = Math.abs(parseIsoMinutes(time) - targetMinutes)

    return currentDifference < closestDifference ? index : closestIndex
  }, 0)
}

function buildWeatherQuery({ latitude, longitude, timezone = 'auto', forecastDays = 2, hourlyFields = HOURLY_FIELDS }) {
  return new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    current: CURRENT_FIELDS.join(','),
    hourly: hourlyFields.join(','),
    daily: DAILY_FIELDS.join(','),
    forecast_days: String(forecastDays),
    timezone,
  }).toString()
}

function buildAirQualityQuery({ latitude, longitude, timezone = 'auto', forecastDays = 2 }) {
  return new URLSearchParams({
    latitude: String(latitude),
    longitude: String(longitude),
    hourly: AIR_QUALITY_FIELDS.join(','),
    forecast_days: String(forecastDays),
    timezone,
  }).toString()
}

function getWeatherEndpoint(options) {
  return `${FORECAST_ENDPOINT}?${buildWeatherQuery(options)}`
}

function getAirQualityEndpoint(options) {
  return `${AIR_QUALITY_ENDPOINT}?${buildAirQualityQuery(options)}`
}

async function fetchJson(url, signal, errorMessage) {
  const response = await fetch(url, { signal })

  if (!response.ok) {
    throw new Error(errorMessage)
  }

  return response.json()
}

function getValueAtIndex(series, index) {
  return Array.isArray(series) ? series[index] : null
}

async function fetchHighCloudBoundaryDistance({
  latitude,
  longitude,
  timezone,
  forecastDays,
  signal,
  snapshotTime,
  solarAzimuth,
}) {
  const samplePoints = BOUNDARY_SAMPLE_DISTANCES_KM.map((distanceKm) => ({
    distanceKm,
    ...destinationPoint(latitude, longitude, solarAzimuth, distanceKm),
  }))

  const forecasts = await Promise.all(
    samplePoints.map((point) =>
      fetchJson(
        getWeatherEndpoint({
          latitude: point.latitude,
          longitude: point.longitude,
          timezone,
          forecastDays,
          hourlyFields: ['cloud_cover_high'],
        }),
        signal,
        '暂时无法采样高云边界数据。',
      ),
    ),
  )

  for (let index = 0; index < forecasts.length; index += 1) {
    const forecast = forecasts[index]
    const timeIndex = forecast.hourly?.time?.indexOf(snapshotTime) ?? -1

    if (timeIndex === -1) {
      continue
    }

    const highCloud = getValueAtIndex(forecast.hourly.cloud_cover_high, timeIndex)

    if (typeof highCloud === 'number' && highCloud < HIGH_CLOUD_ZERO_THRESHOLD) {
      return samplePoints[index].distanceKm
    }
  }

  return BOUNDARY_SAMPLE_DISTANCES_KM.at(-1) ?? 500
}

export async function fetchWeatherForecast(options = {}) {
  const target = options.target ?? 'today-sunset'
  const [forecastData, airQualityData] = await Promise.all([
    fetchJson(
      getWeatherEndpoint(options),
      options.signal,
      '暂时无法获取天气数据。',
    ),
    fetchJson(
      getAirQualityEndpoint(options),
      options.signal,
      '暂时无法获取空气质量数据。',
    ),
  ])

  const hourly = forecastData.hourly
  const daily = forecastData.daily

  if (!hourly?.time?.length || !daily?.sunrise?.length || !daily?.sunset?.length) {
    throw new Error('这个城市的预测数据不完整。')
  }

  const targetEvent = getTargetEventTime(daily, target)
  const targetIndex = findNearestWeatherIndex(hourly.time, targetEvent?.eventTime)

  if (targetIndex === -1 || !targetEvent) {
    throw new Error('没有找到接近目标时刻的小时级预测数据。')
  }

  const snapshotTime = hourly.time[targetIndex]
  const aerosolIndex = airQualityData.hourly?.time?.indexOf(snapshotTime) ?? -1
  const utcOffsetSeconds = forecastData.utc_offset_seconds ?? 0
  const solarAzimuth = calculateSolarAzimuth(
    forecastData.latitude,
    forecastData.longitude,
    targetEvent.eventTime,
    utcOffsetSeconds,
  )
  const boundaryDistanceKm = await fetchHighCloudBoundaryDistance({
    latitude: forecastData.latitude,
    longitude: forecastData.longitude,
    timezone: forecastData.timezone ?? options.timezone ?? 'auto',
    forecastDays: options.forecastDays ?? 2,
    signal: options.signal,
    snapshotTime,
    solarAzimuth,
  })

  return {
    latitude: forecastData.latitude,
    longitude: forecastData.longitude,
    timezone: forecastData.timezone ?? options.timezone ?? 'auto',
    utcOffsetSeconds,
    target: targetEvent.target,
    targetDayIndex: targetEvent.dayIndex,
    eventKey: targetEvent.eventKey,
    eventTime: targetEvent.eventTime,
    snapshotTime,
    sunsetTime: targetEvent.eventTime,
    temperature: getValueAtIndex(hourly.temperature_2m, targetIndex),
    humidity: getValueAtIndex(hourly.relative_humidity_2m, targetIndex),
    cloudCover: getValueAtIndex(hourly.cloud_cover, targetIndex),
    cloudCoverLow: getValueAtIndex(hourly.cloud_cover_low, targetIndex),
    cloudCoverMid: getValueAtIndex(hourly.cloud_cover_mid, targetIndex),
    cloudCoverHigh: getValueAtIndex(hourly.cloud_cover_high, targetIndex),
    windSpeed: getValueAtIndex(hourly.wind_speed_10m, targetIndex),
    visibility: getValueAtIndex(hourly.visibility, targetIndex),
    aerosolOpticalDepth: aerosolIndex >= 0
      ? getValueAtIndex(airQualityData.hourly.aerosol_optical_depth, aerosolIndex)
      : null,
    boundaryDistanceKm,
    solarAzimuth,
    minutesToTarget: Math.abs(parseIsoMinutes(targetEvent.eventTime) - parseIsoMinutes(snapshotTime)),
    minutesToSunset: Math.abs(parseIsoMinutes(targetEvent.eventTime) - parseIsoMinutes(snapshotTime)),
  }
}
