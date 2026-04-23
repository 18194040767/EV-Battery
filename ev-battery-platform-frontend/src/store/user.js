import { defineStore } from 'pinia'

const normalizeUser = (userInfo) => {
  if (!userInfo) return null
  return {
    ...userInfo,
    id: userInfo.id ?? userInfo.userId ?? null,
    roles: Array.isArray(userInfo.roles) ? userInfo.roles : []
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: normalizeUser(JSON.parse(localStorage.getItem('userInfo') || 'null')),
    sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === '1'
  }),
  getters: {
    isAdmin: (state) => state.userInfo?.roles?.includes('ROLE_ADMIN'),
    displayName: (state) => state.userInfo?.realName || state.userInfo?.username || '用户',
    userId: (state) => state.userInfo?.id || null
  },
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token || '')
    },
    setUserInfo(userInfo) {
      this.userInfo = normalizeUser(userInfo)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo || null))
    },
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed ? '1' : '0')
    },
    setSidebarCollapsed(value) {
      this.sidebarCollapsed = !!value
      localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed ? '1' : '0')
    },
    clearAuth() {
      this.token = ''
      this.userInfo = null
      this.sidebarCollapsed = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('sidebarCollapsed')
    }
  }
})
