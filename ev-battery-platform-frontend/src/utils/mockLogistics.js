const STORAGE_KEY = 'mock_logistics_progress'
const DEFAULT_STATUS_FLOW = ['已揽收', '干线运输', '到达分拨中心', '派送中', '已签收']

const loadStateMap = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch (error) {
    return {}
  }
}

const saveStateMap = (value) => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(value))
}

const normalizeIndex = (index, total) => {
  if (!total) return 0
  return Math.max(0, Math.min(Number(index || 0), total - 1))
}

export const ensureMockLogisticsState = (orderId, total, defaultIndex = 0) => {
  const stateMap = loadStateMap()
  const key = String(orderId)
  const current = stateMap[key] || {
    currentIndex: normalizeIndex(defaultIndex, total),
    updatedAt: Date.now()
  }
  current.currentIndex = normalizeIndex(current.currentIndex, total)
  stateMap[key] = current
  saveStateMap(stateMap)
  return current
}

export const advanceMockLogistics = (orderId, total) => {
  const stateMap = loadStateMap()
  const key = String(orderId)
  const current = ensureMockLogisticsState(orderId, total, 0)
  current.currentIndex = normalizeIndex(current.currentIndex + 1, total)
  current.updatedAt = Date.now()
  stateMap[key] = current
  saveStateMap(stateMap)
  return current
}

export const resetMockLogistics = (orderId, total, defaultIndex = 0) => {
  const stateMap = loadStateMap()
  stateMap[String(orderId)] = {
    currentIndex: normalizeIndex(defaultIndex, total),
    updatedAt: Date.now()
  }
  saveStateMap(stateMap)
  return stateMap[String(orderId)]
}

export const mergeMockLogisticsState = (orderId, data = {}) => {
  const nodes = normalizeNodes(data.nodes || [])
  const defaultIndex = resolveDefaultIndex(data, nodes.length)
  const current = ensureMockLogisticsState(orderId, nodes.length, defaultIndex)
  const currentIndex = normalizeIndex(current.currentIndex, nodes.length)
  const nextNodes = nodes.map((node, index) => ({
    ...node,
    isCurrent: index === currentIndex
  }))
  const routePoints = data.route?.checkpoints || []
  const checkpointIndex = routePoints.length <= 1
    ? 0
    : Math.round((currentIndex / Math.max(nodes.length - 1, 1)) * (routePoints.length - 1))
  const checkpoint = routePoints[Math.min(checkpointIndex, Math.max(routePoints.length - 1, 0))] || routePoints[0] || null
  return {
    ...data,
    currentIndex,
    nodes: nextNodes,
    currentCheckpoint: checkpoint,
    etaDays: Math.max(0, nodes.length - currentIndex - 1),
    progressPercent: nodes.length <= 1 ? Number(data.progressPercent || 100) : Math.round((currentIndex / (nodes.length - 1)) * 100),
    status: nextNodes[currentIndex]?.status || data.status
  }
}

const resolveDefaultIndex = (data, total) => {
  if (!total) return 0
  const percent = Number(data?.progressPercent || 0)
  if (!Number.isFinite(percent) || percent <= 0) return 0
  return Math.min(total - 1, Math.floor((percent / 100) * (total - 1)))
}

const normalizeNodes = (rawNodes) => {
  const nodes = (rawNodes || []).map((node, index) => ({
    ...node,
    status: node.status || DEFAULT_STATUS_FLOW[index] || `运输节点 ${index + 1}`
  }))
  if (nodes.length >= DEFAULT_STATUS_FLOW.length) return nodes
  const lastTime = nodes[nodes.length - 1]?.time || new Date().toISOString()
  for (let i = nodes.length; i < DEFAULT_STATUS_FLOW.length; i += 1) {
    nodes.push({
      status: DEFAULT_STATUS_FLOW[i],
      time: lastTime,
      description: `${DEFAULT_STATUS_FLOW[i]}模拟节点`,
      isCurrent: false
    })
  }
  return nodes
}
