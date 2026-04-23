import request from '../utils/request'

export const chatWithAssistant = (data) => request.post('/assistant/chat', data)

export const streamAssistantReply = async ({ question, history = [], onChunk, onDone, onError }) => {
  const token = localStorage.getItem('token')
  const response = await fetch('/api/assistant/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ question, history })
  })

  if (!response.ok || !response.body) {
    throw new Error(`assistant stream failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''

    for (const event of events) {
      const lines = event.split('\n')
      const eventName = lines.find((line) => line.startsWith('event:'))?.replace('event:', '').trim() || 'message'
      const data = lines
        .filter((line) => line.startsWith('data:'))
        .map((line) => line.replace('data:', '').trim())
        .join('\n')

      if (eventName === 'chunk') {
        onChunk?.(data)
      } else if (eventName === 'done') {
        onDone?.()
      } else if (eventName === 'error') {
        onError?.(data || 'AI 小助手暂时不可用')
      }
    }
  }
}
