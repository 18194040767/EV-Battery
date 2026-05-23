import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

const frontChildren = [
  { path: '', redirect: '/home' },
  { path: 'home', component: () => import('../views/home/Home.vue'), meta: { title: '首页' } },
  { path: 'battery/list', component: () => import('../views/battery/BatteryList.vue'), meta: { title: '电池档案' } },
  { path: 'battery/detail/:id', component: () => import('../views/battery/BatteryDetail.vue'), meta: { title: '档案详情' } },
  { path: 'battery/upload', component: () => import('../views/battery/BatteryUpload.vue'), meta: { title: '上传档案', requiresAuth: true } },
  { path: 'assessment', component: () => import('../views/assessment/AssessmentWorkbench.vue'), meta: { title: '健康评估' } },
  { path: 'trade/product-list', component: () => import('../views/trade/ProductList.vue'), meta: { title: '商品市场' } },
  { path: 'trade/product/:id', component: () => import('../views/trade/ProductDetail.vue'), meta: { title: '商品详情' } },
  { path: 'trade/cart', component: () => import('../views/trade/Cart.vue'), meta: { title: '购物车', requiresAuth: true } },
  { path: 'trade/favorites', component: () => import('../views/trade/Favorites.vue'), meta: { title: '我的收藏', requiresAuth: true } },
  { path: 'trade/user/:id?', component: () => import('../views/trade/UserProfile.vue'), meta: { title: '个人主页', requiresAuth: true } },
  { path: 'trade/demand-list', component: () => import('../views/trade/DemandList.vue'), meta: { title: '采购需求' } },
  { path: 'trade/order-list', component: () => import('../views/trade/OrderList.vue'), meta: { title: '订单中心', requiresAuth: true } },
  { path: 'contract/list', component: () => import('../views/contract/ContractList.vue'), meta: { title: '我的合同', requiresAuth: true } },
  { path: 'contract/verify', component: () => import('../views/contract/Verify.vue'), meta: { title: '合同查验' } },
  { path: 'logistics/list', component: () => import('../views/logistics/LogisticsList.vue'), meta: { title: '物流追踪' } },
  { path: 'report/list', component: () => import('../views/report/ReportList.vue'), meta: { title: '智能报告' } },
  { path: 'statistics/overview', component: () => import('../views/statistics/Overview.vue'), meta: { title: '运营概览' } }
]

const adminChildren = [
  { path: '', redirect: '/admin/dashboard' },
  { path: 'dashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { requiresAdmin: true, title: '数据驾驶舱' } },
  { path: 'users', component: () => import('../views/admin/Users.vue'), meta: { requiresAdmin: true, title: '用户管理' } },
  { path: 'batteries', component: () => import('../views/admin/Batteries.vue'), meta: { requiresAdmin: true, title: '档案审核' } },
  { path: 'products', component: () => import('../views/admin/Products.vue'), meta: { requiresAdmin: true, title: '商品审核' } },
  { path: 'orders', component: () => import('../views/admin/Orders.vue'), meta: { requiresAdmin: true, title: '订单管理' } },
  { path: 'contracts', component: () => import('../views/admin/Contracts.vue'), meta: { requiresAdmin: true, title: '合同存证' } },
  { path: 'statistics', component: () => import('../views/admin/Statistics.vue'), meta: { requiresAdmin: true, title: '运营统计' } },
  { path: 'system', component: () => import('../views/admin/SystemSettings.vue'), meta: { requiresAdmin: true, title: '系统管理' } },
  { path: 'messages', component: () => import('../views/admin/Messages.vue'), meta: { requiresAdmin: true, title: '消息通知' } },
  { path: 'assistant', component: () => import('../views/admin/Assistant.vue'), meta: { requiresAdmin: true, title: 'AI 助手' } }
]

const routes = [
  { path: '/login', component: () => import('../views/auth/Login.vue'), meta: { guestOnly: true } },
  { path: '/register', component: () => import('../views/auth/Register.vue'), meta: { guestOnly: true } },
  { path: '/', component: () => import('../layout/Layout.vue'), meta: { requiresFrontUser: true }, children: frontChildren },
  { path: '/admin', component: () => import('../layout/AdminLayout.vue'), meta: { requiresAdmin: true }, children: adminChildren }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isAuthPage = to.path === '/login' || to.path === '/register'

  if (!userStore.token && !userStore.isGuest && !isAuthPage) {
    next('/login')
    return
  }

  if (userStore.token && isAuthPage) {
    next(userStore.isAdmin ? '/admin/dashboard' : '/home')
    return
  }

  if (to.matched.some((record) => record.meta?.requiresAdmin)) {
    if (!userStore.isAdmin) {
      next('/home')
      return
    }
    next()
    return
  }

  if (to.matched.some((record) => record.meta?.requiresAuth) && !userStore.token) {
    next('/login')
    return
  }

  next()
})

export default router
