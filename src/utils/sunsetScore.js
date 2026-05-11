const CHROMASKY_CONFIG = {
  cloudThreshold: 0.1,
  maxSearchDistanceKm: 500,
  optimalDistanceKm: 400,
  weights: {
    boundary: 0.8,
    highCloud: 0.1,
    midCloud: 0.1,
  },
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function round(value, digits = 2) {
  const factor = 10 ** digits

  return Math.round(value * factor) / factor
}

function toFraction(percentageValue) {
  return clamp((percentageValue ?? 0) / 100, 0, 1)
}

function scoreBoundaryDistance(distanceKm) {
  if (distanceKm >= CHROMASKY_CONFIG.maxSearchDistanceKm) {
    return 0
  }

  if (distanceKm <= CHROMASKY_CONFIG.optimalDistanceKm) {
    return distanceKm / CHROMASKY_CONFIG.optimalDistanceKm
  }

  return clamp(
    1 - (distanceKm - CHROMASKY_CONFIG.optimalDistanceKm) /
      (CHROMASKY_CONFIG.maxSearchDistanceKm - CHROMASKY_CONFIG.optimalDistanceKm),
    0,
    1,
  )
}

function scoreHighCloud(highCloudFraction) {
  if (highCloudFraction >= 0.4 && highCloudFraction <= 0.8) {
    return 1
  }

  if (highCloudFraction > 0.8) {
    return 0.7
  }

  if (highCloudFraction >= 0.1) {
    return 0.6
  }

  return 0.1
}

function scoreMidCloud(midCloudFraction) {
  if (midCloudFraction >= 0.2 && midCloudFraction <= 0.5) {
    return 1
  }

  if (midCloudFraction > 0.5 && midCloudFraction <= 0.8) {
    return 0.7
  }

  if (midCloudFraction > 0.8) {
    return 0.3
  }

  return 0.2
}

function scoreLowCloud(lowCloudFraction) {
  if (lowCloudFraction <= 0.1) {
    return 1
  }

  if (lowCloudFraction <= 0.3) {
    return 0.6
  }

  if (lowCloudFraction <= 0.5) {
    return 0.1
  }

  return 0
}

function scoreAerosolOpticalDepth(aerosolOpticalDepth) {
  if (aerosolOpticalDepth < 0.3) {
    return 1
  }

  if (aerosolOpticalDepth < 0.6) {
    return 0.5
  }

  return 0
}

export function evaluateSkyOpticalState(snapshot) {
  const highCloudFraction = toFraction(snapshot.cloudCoverHigh)
  const midCloudFraction = toFraction(snapshot.cloudCoverMid)
  const lowCloudFraction = toFraction(snapshot.cloudCoverLow)
  const boundaryDistanceKm = snapshot.boundaryDistanceKm ?? CHROMASKY_CONFIG.maxSearchDistanceKm
  const aerosolOpticalDepth = snapshot.aerosolOpticalDepth ?? 0.3

  const factorScores = {
    boundary: scoreBoundaryDistance(boundaryDistanceKm),
    highCloud: scoreHighCloud(highCloudFraction),
    midCloud: scoreMidCloud(midCloudFraction),
    lowCloud: scoreLowCloud(lowCloudFraction),
    aerosol: scoreAerosolOpticalDepth(aerosolOpticalDepth),
  }

  if (highCloudFraction < CHROMASKY_CONFIG.cloudThreshold) {
    return {
      score: 0,
      suitable: false,
      label: '不适合',
      description: '观测点上方高云太少，缺少形成晚霞层次感的主要载体。',
      factorScores,
      normalizedFactors: {
        highCloudFraction: round(highCloudFraction),
        midCloudFraction: round(midCloudFraction),
        lowCloudFraction: round(lowCloudFraction),
        aerosolOpticalDepth: round(aerosolOpticalDepth),
        boundaryDistanceKm: round(boundaryDistanceKm),
      },
    }
  }

  const qualityScore =
    factorScores.boundary * CHROMASKY_CONFIG.weights.boundary +
    factorScores.highCloud * CHROMASKY_CONFIG.weights.highCloud +
    factorScores.midCloud * CHROMASKY_CONFIG.weights.midCloud

  const penaltyScore = factorScores.lowCloud * factorScores.aerosol
  const finalScore = Math.round(qualityScore * penaltyScore * 100)

  return {
    score: finalScore,
    suitable: finalScore >= 45,
    label: classifySunsetScore(finalScore).label,
    description: classifySunsetScore(finalScore).description,
    factorScores,
    normalizedFactors: {
      highCloudFraction: round(highCloudFraction),
      midCloudFraction: round(midCloudFraction),
      lowCloudFraction: round(lowCloudFraction),
      aerosolOpticalDepth: round(aerosolOpticalDepth),
      boundaryDistanceKm: round(boundaryDistanceKm),
      qualityScore: round(qualityScore),
      penaltyScore: round(penaltyScore),
    },
  }
}

export function calculateSunsetScore(snapshot) {
  return evaluateSkyOpticalState(snapshot).score
}

export function classifySunsetScore(score) {
  if (score >= 70) {
    return {
      label: '非常适合',
      description: '高云结构、云边界距离和透明度都比较理想，天空具备很强的晚霞潜力。',
    }
  }

  if (score >= 45) {
    return {
      label: '比较适合',
      description: '天空光学条件整体不错，傍晚有较大机会出现层次和颜色都在线的晚霞。',
    }
  }

  if (score >= 20) {
    return {
      label: '一般',
      description: '部分关键因子还可以，但低云、透明度或边界结构可能限制最终表现。',
    }
  }

  return {
    label: '不适合',
    description: '当前天空光学状态不够理想，出现高质量晚霞的概率偏低。',
  }
}
