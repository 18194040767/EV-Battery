import axios from 'axios'
import request from '../utils/request'
import { mergeMockLogisticsState, resetMockLogistics } from '../utils/mockLogistics'

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export const fillTracking = (data) => request.post('/logistics/fill-tracking', data)
export const queryLogisticsStatus = (orderId) => request.get(`/logistics/status/${orderId}`)
export const getTrackData = async (orderId) => {
  const res = await queryLogisticsStatus(orderId)
  const data = res?.data || {}
  const merged = mergeMockLogisticsState(orderId, data)
  const path = (merged.route?.checkpoints || []).map((item) => ({
    lng: Number(item.lng),
    lat: Number(item.lat)
  }))
  const nodes = (merged.nodes || []).map((item, index) => ({
    status: item.status,
    time: item.time ? String(item.time).replace('T', ' ') : '-',
    description: item.description || '',
    isCurrent: !!item.isCurrent,
    index
  }))
  return {
    waybillNo: merged.trackingNo || '',
    currentStatus: merged.status || '待发货',
    currentIndex: merged.currentIndex ?? resolveCurrentIndex(merged, nodes.length),
    nodes,
    path,
    route: merged.route || null,
    etaDays: merged.etaDays || 0,
    currentCheckpoint: merged.currentCheckpoint || null,
    raw: merged
  }
}

const resolveCurrentIndex = (data, total) => {
  if (!total) return 0
  const percent = Number(data?.progressPercent || 0)
  return Math.min(total - 1, Math.max(0, Math.floor((percent / 100) * total)))
}

export const downloadHazardousNotice = (orderId) =>
  axios.get(`/api/logistics/${orderId}/hazardous-notice`, {
    responseType: 'blob',
    headers: authHeaders()
  })

export const mockShipTradeOrder = async (orderId, data = {}) => {
  const res = await request.post(`/trade/orders/${orderId}/mock-ship`, data)
  resetMockLogistics(orderId, 5, 0)
  return res
}
