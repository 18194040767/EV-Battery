import request from '../utils/request'
export const login = (data) => request.post('/user/login', data)
export const register = (data) => request.post('/user/register', data)
export const getCurrentUser = () => request.get('/user/current')
export const changePassword = (data) => request.post('/user/change-password', data)
export const requestForgotPasswordCode = (data) => request.post('/user/forgot-password/code', data)
export const resetForgotPassword = (data) => request.post('/user/forgot-password/reset', data)
