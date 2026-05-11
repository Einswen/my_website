export function getPredictEndpoint(baseUrl = '/api') {
  return `${baseUrl}/predict`
}

export function createPredictPayload(snapshot) {
  return {
    cloudCoverLow: snapshot.cloudCoverLow,
    cloudCoverHigh: snapshot.cloudCoverHigh,
    humidity: snapshot.humidity,
    temperature: snapshot.temperature,
    windSpeed: snapshot.windSpeed,
    sunsetTime: snapshot.sunsetTime ?? null,
  }
}
