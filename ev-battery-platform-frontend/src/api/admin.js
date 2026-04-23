import request from '../utils/request'

export const getAdminDashboard = () => request.get('/admin/dashboard')
export const getAdminUsers = () => request.get('/admin/users')
export const updateAdminUser = (data) => request.put('/admin/users', data)
export const resetAdminUserPassword = (id) => request.post(`/admin/users/${id}/reset-password`)

export const getAdminBatteries = () => request.get('/admin/batteries')
export const auditAdminBattery = (id, data) => request.post(`/admin/batteries/${id}/audit`, data)

export const getAdminProducts = () => request.get('/admin/products')
export const auditAdminProduct = (id, data) => request.post(`/admin/products/${id}/audit`, data)

export const getAdminOrders = () => request.get('/admin/orders')
export const cancelAdminOrder = (id) => request.post(`/admin/orders/${id}/cancel`)
export const shipAdminOrder = (id, data) => request.post(`/admin/orders/${id}/ship`, data)

export const getAdminContracts = () => request.get('/admin/contracts')
export const verifyAdminContract = (id) => request.post(`/admin/contracts/${id}/verify`)
