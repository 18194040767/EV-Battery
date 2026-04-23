import request from '../utils/request'

export const uploadSingle = (data) => request.post('/battery/upload/single', data)
export const uploadBatch = (data) => request.post('/battery/upload/batch', data)

export const getBatteryList = (params) => request.get('/battery/list', { params })
export const getBatteryDetail = (id) => request.get(`/battery/${id}`)
export const createBatteryManual = (data) => request.post('/battery/manual', data)
export const updateBattery = (id, data) => request.put(`/battery/${id}`, data)
export const updateBatteryStatus = (id, status) => request.patch(`/battery/${id}/status`, { status })
export const deleteBattery = (id) => request.delete(`/battery/${id}`)
export const deleteBatteryBatch = (ids) => request.delete('/battery/batch', { data: { ids } })

export const saveBatteryDraft = (data) => request.post('/battery/draft', data)
export const getBatteryDraftList = () => request.get('/battery/draft/list')
export const deleteBatteryDraft = (id) => request.delete(`/battery/draft/${id}`)

export const getBatteryStatistics = () => request.get('/battery/statistics')
export const batchBatteryTag = (data) => request.put('/battery/batch/tag', data)
export const assignBatteryTags = (id, data) => request.post(`/battery/${id}/tags`, data)
export const removeBatteryTags = (id, data) => request.delete(`/battery/${id}/tags`, { data })

export const getTagList = () => request.get('/tag/list')
export const createTag = (data) => request.post('/tag', data)
