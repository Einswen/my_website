const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

export const pinnedMessage = {
  id: 'pinned-einswen',
  name: 'Einswen',
  content: '这个网站仍在努力开发...大家有什么好玩的建议可以写下来。',
  createdAt: '置顶',
  pinned: true,
}

class GuestbookApiError extends Error {
  constructor(message, options = {}) {
    super(message)
    this.name = 'GuestbookApiError'
    this.retryAfterSeconds = options.retryAfterSeconds ?? 0
  }
}

function normalizeMessage(message) {
  return {
    id: String(message.id),
    name: message.name,
    content: message.content,
    createdAt: message.createdAt,
    pinned: Boolean(message.pinned),
  }
}

async function parseApiError(response) {
  let payload

  try {
    payload = await response.json()
  } catch {
    throw new GuestbookApiError('留言服务暂时不可用。')
  }

  const detail = payload?.detail

  if (typeof detail === 'object' && detail !== null) {
    throw new GuestbookApiError(detail.message || '留言提交失败。', {
      retryAfterSeconds: detail.retryAfterSeconds,
    })
  }

  throw new GuestbookApiError(typeof detail === 'string' ? detail : '留言提交失败。')
}

export async function fetchGuestbookMessages() {
  const response = await fetch(`${API_BASE_URL}/messages`)

  if (!response.ok) {
    await parseApiError(response)
  }

  const messages = await response.json()
  return messages.map(normalizeMessage)
}

export async function submitGuestbookMessage({ name, content }) {
  const response = await fetch(`${API_BASE_URL}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      name: name.trim(),
      content: content.trim(),
    }),
  })

  if (!response.ok) {
    await parseApiError(response)
  }

  const payload = await response.json()

  return {
    message: normalizeMessage(payload.message),
    cooldownSeconds: payload.cooldownSeconds ?? 3600,
  }
}
