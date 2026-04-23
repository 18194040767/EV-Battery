<template>
  <div class="page-shell">
    <HomeBanner :slides="bannerSlides" />
    <FeatureSection :features="featureCards" />

    <section class="insight-grid">
      <div class="panel-card quick-metrics">
        <div class="section-copy">
          <p>概览</p>
          <h3>平台数据</h3>
        </div>
        <div class="metric-list">
          <article v-for="item in metrics" :key="item.label">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </article>
        </div>
      </div>

      <div class="panel-card article-panel">
        <div class="section-copy">
          <p>资讯</p>
          <h3>行业快讯</h3>
        </div>
        <article v-for="item in insights" :key="item.title" class="article-item">
          <strong>{{ item.title }}</strong>
        </article>
      </div>
    </section>

    <HotProducts :products="hotProducts" />

    <section class="panel-card partner-panel">
      <div class="section-copy">
        <p>合作</p>
        <h3>伙伴网络</h3>
      </div>
      <div class="partner-wall">
        <span v-for="item in partners" :key="item">{{ item }}</span>
      </div>
    </section>

    <AIAssistantPet />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import HomeBanner from '../../components/HomeBanner.vue'
import FeatureSection from '../../components/FeatureSection.vue'
import HotProducts from '../../components/HotProducts.vue'
import AIAssistantPet from '../../components/AIAssistantPet.vue'
import { getTradeProducts } from '../../api/trade'
import { getHealthDistribution, getProductCategoryDistribution, getTradeTrend } from '../../api/statistics'

const hotProducts = ref([])
const stats = ref({})

const bannerSlides = [
  { kicker: 'Platform', title: '电池流通', description: '可信交易', background: 'linear-gradient(135deg, #0f766e 0%, #164e63 100%)' },
  { kicker: 'Market', title: '快速选货', description: '健康可查', background: 'linear-gradient(135deg, #1d4ed8 0%, #0f766e 100%)' },
  { kicker: 'Service', title: '全程履约', description: '存证可验', background: 'linear-gradient(135deg, #14532d 0%, #0f766e 100%)' }
]

const featureCards = [
  { mark: '01', title: '健康评估', description: '状态可查' },
  { mark: '02', title: '安全交易', description: '履约清晰' },
  { mark: '03', title: '物流追踪', description: '轨迹可见' },
  { mark: '04', title: '合同存证', description: '摘要可验' }
]

const metrics = computed(() => {
  const totalOrders = (stats.value?.trend?.orderSeries || []).reduce((sum, item) => sum + Number(item || 0), 0)
  const totalAmount = (stats.value?.trend?.amountSeries || []).reduce((sum, item) => sum + Number(item || 0), 0)
  return [
    { label: '近7日单量', value: totalOrders },
    { label: '近7日交易', value: `¥${totalAmount.toFixed(0)}` },
    { label: '等级数', value: (stats.value?.health || []).length },
    { label: '分类数', value: (stats.value?.categories || []).length }
  ]
})

const insights = [{ title: '储能应用' }, { title: '履约存证' }, { title: '检测分级' }]
const partners = ['4S网络', '集成商', '回收企业', '储能项目', '检测机构', '物流服务']

onMounted(async () => {
  const [productsRes, trendRes, healthRes, categoryRes] = await Promise.allSettled([
    getTradeProducts({ page: 1, size: 4, sortBy: 'latest' }),
    getTradeTrend({ days: 7 }),
    getHealthDistribution(),
    getProductCategoryDistribution()
  ])

  if (productsRes.status === 'fulfilled') {
    hotProducts.value = productsRes.value?.data?.records || []
  }

  stats.value = {
    trend: trendRes.status === 'fulfilled' ? trendRes.value?.data || {} : {},
    health: healthRes.status === 'fulfilled' ? healthRes.value?.data || [] : [],
    categories: categoryRes.status === 'fulfilled' ? categoryRes.value?.data || [] : []
  }
})
</script>

<style scoped>
.insight-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 18px;
}

.quick-metrics,
.article-panel,
.partner-panel {
  padding: 24px;
}

.section-copy p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.section-copy h3 {
  margin: 0;
}

.metric-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.metric-list article {
  padding: 18px;
  border-radius: 18px;
  background: #f7fbfa;
  border: 1px solid rgba(15, 118, 110, 0.08);
}

.metric-list strong {
  display: block;
  font-size: 28px;
  color: var(--app-primary-dark);
}

.metric-list span {
  color: var(--app-muted);
}

.article-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--app-border);
}

.article-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.partner-wall {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.partner-wall span {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  min-height: 92px;
  border-radius: 18px;
  background: #f7fbfa;
  border: 1px solid rgba(15, 118, 110, 0.08);
  color: #21433d;
  font-weight: 600;
}

@media (max-width: 900px) {
  .insight-grid,
  .partner-wall {
    grid-template-columns: 1fr;
  }

  .metric-list {
    grid-template-columns: 1fr;
  }

}
</style>
