const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

async function parsePetResponse(response, fallbackMessage) {
  if (response.ok) {
    return response.json()
  }

  let payload

  try {
    payload = await response.json()
  } catch {
    throw new Error(fallbackMessage)
  }

  const detail = payload?.detail
  throw new Error(typeof detail === 'string' ? detail : fallbackMessage)
}

export async function fetchPetState() {
  const response = await fetch(`${API_BASE_URL}/pet`)
  return parsePetResponse(response, '暂时无法加载电子宠物。')
}

export async function patPet() {
  const response = await fetch(`${API_BASE_URL}/pet/pat`, {
    method: 'POST',
  })

  return parsePetResponse(response, '我现在没有反应，再试一次吧。')
}

export async function feedPet(optionId) {
  const response = await fetch(`${API_BASE_URL}/pet/feed`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ optionId }),
  })

  return parsePetResponse(response, '喂食暂时失败了。')
}

export async function changePetOutfit(optionId) {
  const response = await fetch(`${API_BASE_URL}/pet/outfit`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ optionId }),
  })

  return parsePetResponse(response, '换装暂时失败了。')
}

export async function chatWithPet(message) {
  const response = await fetch(`${API_BASE_URL}/pet/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ message }),
  })

  return parsePetResponse(response, '我刚刚走神了。')
}
