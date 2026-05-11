const GEOCODING_ENDPOINT = 'https://geocoding-api.open-meteo.com/v1/search'

export function buildGeocodingQuery(cityName, { count = 1, language = 'en', format = 'json' } = {}) {
  return new URLSearchParams({
    name: cityName,
    count: String(count),
    language,
    format,
  }).toString()
}

export function getGeocodingEndpoint(cityName, options) {
  return `${GEOCODING_ENDPOINT}?${buildGeocodingQuery(cityName, options)}`
}

export async function searchCityLocation(cityName, options = {}) {
  const response = await fetch(getGeocodingEndpoint(cityName, options), {
    signal: options.signal,
  })

  if (!response.ok) {
    throw new Error('暂时无法解析这个城市。')
  }

  const data = await response.json()
  const [result] = data.results ?? []

  if (!result) {
    return null
  }

  return {
    name: result.name,
    latitude: result.latitude,
    longitude: result.longitude,
    timezone: result.timezone,
    country: result.country ?? '',
    admin1: result.admin1 ?? '',
    countryCode: result.country_code ?? '',
  }
}
