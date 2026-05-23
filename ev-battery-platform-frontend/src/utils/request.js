import axios from 'axios'
import { useUserStore } from '../store/user'
import router from '../router'

const request = axios.create({ baseURL: '/api', timeout: 10000 })
request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) config.headers.Authorization = `Bearer ${userStore.token}`
  return config
})
request.interceptors.response.use((res) => res.data, (error) => {
  if (error?.response?.status === 401) {
    const userStore = useUserStore()
    if (userStore.isGuest) return Promise.reject(error)
    userStore.clearAuth()
    router.push('/login')
  }
  return Promise.reject(error)
})
export default request
