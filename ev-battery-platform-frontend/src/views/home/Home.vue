<template>
  <div class="page-shell home-page">
    <HomeBanner :slides="bannerSlides" />

    <section class="quick-route-grid" aria-label="快捷路由通道">
      <button
        v-for="item in quickRoutes"
        :key="item.path + item.title"
        type="button"
        class="quick-route-card"
        @click="go(item.path)"
      >
        <span class="route-icon" :class="item.tone">
          <AppNavIcon :name="item.icon" />
        </span>
        <span class="route-copy">
          <strong>{{ item.title }}</strong>
          <em>{{ item.description }}</em>
        </span>
        <span class="route-arrow">→</span>
      </button>
    </section>

    <section class="overview-card">
      <div class="overview-title">平台概览</div>
      <div class="overview-metrics">
        <article v-for="item in overviewMetrics" :key="item.label" class="overview-metric">
          <span class="metric-icon" :class="item.tone">
            <AppNavIcon :name="item.icon" />
          </span>
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
          <em>{{ item.delta }}</em>
        </article>
      </div>
    </section>

    <HotProducts :products="hotProducts" />

    <AIAssistantPet />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import HomeBanner from '../../components/HomeBanner.vue'
import HotProducts from '../../components/HotProducts.vue'
import AIAssistantPet from '../../components/AIAssistantPet.vue'
import AppNavIcon from '../../components/AppNavIcon.vue'
import { getTradeProducts, getTradeOrders } from '../../api/trade'
import { getTradeTrend } from '../../api/statistics'

const router = useRouter()
const hotProducts = ref([])
const productTotal = ref(0)
const orderTotal = ref(0)
const thirtyDayAmount = ref(0)

const bannerSlides = [
  { title: '协同平台', src: '/home-banner-1.png' },
  { title: 'SOH健康评估与状态洞察', src: '/home-banner-2.png' },
  { title: '二手交易与商品市场', src: '/home-banner-3.png' },
  { title: '物流追踪与履约管理', src: '/home-banner-4.png' },
  { title: '电子合同与存证校验', src: '/home-banner-5.png' }
]

const quickRoutes = [
  { title: '电池档案', description: '档案管理与评估', icon: 'battery', tone: 'blue', path: '/battery/list' },
  { title: '健康评估', description: 'SOH评估与报告', icon: 'assessment', tone: 'teal', path: '/assessment' },
  { title: '商品市场', description: '商品浏览与交易', icon: 'market', tone: 'violet', path: '/trade/product-list' },
  { title: '物流追踪', description: '运单查询与轨迹', icon: 'logistics', tone: 'sky', path: '/logistics/list' },
  { title: '合同存证', description: '电子合同与存证', icon: 'contracts', tone: 'orange', path: '/contract/list' },
  { title: '二手交易', description: '履约服务管理', icon: 'market', tone: 'cyan', path: '/trade/product-list' }
]

const overviewMetrics = computed(() => [
  { label: '用户总数', value: 0, delta: '较昨日 +0', icon: 'users', tone: 'blue' },
  { label: '商品总数', value: productTotal.value, delta: '较昨日 +0', icon: 'products', tone: 'teal' },
  { label: '订单总数', value: orderTotal.value, delta: '较昨日 +0', icon: 'orders', tone: 'violet' },
  { label: '近30日额', value: `¥${thirtyDayAmount.value.toFixed(2)}`, delta: '较昨日 0.00%', icon: 'statistics', tone: 'orange' }
])

const go = (path) => {
  router.push(path)
}

const getRecords = (payload) => payload?.data?.records || payload?.records || []
const getTotal = (payload, records = []) => Number(payload?.data?.total ?? payload?.total ?? records.length ?? 0)

onMounted(async () => {
  const [productsRes, ordersRes, trendRes] = await Promise.allSettled([
    getTradeProducts({ page: 1, size: 4, sortBy: 'latest' }),
    getTradeOrders({ page: 1, size: 1 }),
    getTradeTrend({ days: 30 })
  ])

  if (productsRes.status === 'fulfilled') {
    const records = getRecords(productsRes.value)
    hotProducts.value = records
    productTotal.value = getTotal(productsRes.value, records)
  }

  if (ordersRes.status === 'fulfilled') {
    const records = getRecords(ordersRes.value)
    orderTotal.value = getTotal(ordersRes.value, records)
  }

  if (trendRes.status === 'fulfilled') {
    thirtyDayAmount.value = (trendRes.value?.data?.amountSeries || []).reduce((sum, item) => sum + Number(item || 0), 0)
    if (!orderTotal.value) {
      orderTotal.value = (trendRes.value?.data?.orderSeries || []).reduce((sum, item) => sum + Number(item || 0), 0)
    }
  }
})
</script>

<style scoped>
.home-page {
  gap: 18px;
}

.quick-route-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.quick-route-card {
  display: grid;
  min-height: 120px;
  grid-template-columns: 52px 1fr;
  grid-template-rows: 1fr auto;
  align-items: start;
  gap: 12px 14px;
  padding: 20px 18px 16px;
  border: 1px solid rgba(210, 222, 242, 0.78);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 14px 32px rgba(37, 88, 170, 0.07);
  color: #101a33;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.quick-route-card:hover {
  transform: translateY(-2px);
  border-color: rgba(47, 124, 255, 0.34);
  box-shadow: 0 18px 40px rgba(37, 88, 170, 0.12);
}

.route-icon,
.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  box-shadow: 0 10px 20px rgba(37, 88, 170, 0.16);
}

.route-icon {
  width: 50px;
  height: 50px;
  border-radius: 13px;
  font-size: 26px;
}

.route-copy {
  min-width: 0;
}

.route-copy strong {
  display: block;
  margin-top: 2px;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.25;
}

.route-copy em {
  display: block;
  margin-top: 8px;
  color: #66758d;
  font-size: 13px;
  font-style: normal;
  line-height: 1.35;
}

.route-arrow {
  grid-column: 1 / -1;
  justify-self: center;
  color: #5b80a9;
  font-size: 20px;
  line-height: 1;
}

.blue {
  background: linear-gradient(180deg, #5aa0ff 0%, #1f75ff 100%);
}

.teal {
  background: linear-gradient(180deg, #4bd8c4 0%, #22b8a6 100%);
}

.violet {
  background: linear-gradient(180deg, #9069ff 0%, #6c4ee6 100%);
}

.sky {
  background: linear-gradient(180deg, #63a7ff 0%, #2d8cff 100%);
}

.orange {
  background: linear-gradient(180deg, #ffb057 0%, #f59e3b 100%);
}

.cyan {
  background: linear-gradient(180deg, #41cfe0 0%, #18a9c5 100%);
}

.overview-card {
  padding: 24px 24px 28px;
  border: 1px solid rgba(210, 222, 242, 0.78);
  border-radius: 16px;
  background: #ffffff;
  box-shadow: 0 14px 32px rgba(37, 88, 170, 0.06);
}

.overview-title {
  margin-bottom: 24px;
  color: #101a33;
  font-size: 18px;
  font-weight: 800;
}

.overview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.overview-metric {
  text-align: center;
}

.metric-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  font-size: 23px;
}

.overview-metric p {
  margin: 14px 0 8px;
  color: #5f6f8d;
  font-size: 14px;
}

.overview-metric strong {
  display: block;
  color: #101a33;
  font-size: 26px;
  line-height: 1.15;
}

.overview-metric em {
  display: block;
  margin-top: 12px;
  color: #7b8799;
  font-size: 13px;
  font-style: normal;
}

@media (max-width: 1280px) {
  .quick-route-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .quick-route-grid,
  .overview-metrics {
    grid-template-columns: 1fr;
  }

  .quick-route-card {
    min-height: 104px;
  }
}
</style>
