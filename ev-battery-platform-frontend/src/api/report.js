import request from '../utils/request'
export const generateReport = (data) => request.post('/report/generate', data)
export const listReport = (params) => request.get('/report/list', { params })
export const compareReport = (id1, id2) => request.get('/report/compare', { params: { id1, id2 } })
export const reportDetail = (id) => request.get(`/report/${id}`)
