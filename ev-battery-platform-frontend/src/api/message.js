import request from '../utils/request'

export const getMessageList = (params) => request.get('/message/list', { params })
export const getUnreadCount = () => request.get('/message/unread-count')
export const markMessageRead = (id) => request.post(`/message/${id}/read`)
export const markAllMessageRead = () => request.post('/message/read-all')
