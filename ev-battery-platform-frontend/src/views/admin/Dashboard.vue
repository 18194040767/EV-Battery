<template>
  <div class="page-shell">
    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="panel-card metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="chart-grid">
      <div class="panel-card chart-card">
        <div class="section-title">
          <h3>交易趋势</h3>
        </div>
        <div ref="trendRef" class="chart-box" />
      </div>
      <div class="panel-card chart-card">
        <div class="section-title">
          <h3>健康分布</h3>
        </div>
        <div ref="healthRef" class="chart-box" />
      </div>
    </section>

    <section class="chart-grid">
      <div class="panel-card chart-card">
        <div class="section-title">
          <h3>分类占比</h3>
        </div>
        <div ref="categoryRef" class="chart-box" />
      </div>
      <div class="panel-card recent-card">
        <div class="section-title">
          <h3>订单动态</h3>
        </div>
        <div class="recent-list">
          <article v-for="item in recentOrders" :key="item.orderNo" class="recent-item">
            <strong>{{ item.orderNo }}</strong>
            <p>{{ item.title || '电池商品' }}</p>
            <span>{{ item.orderStatus }} · ¥{{ item.amount }}</span>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { getAdminDashboard } from '../../api/admin'

const dashboard = ref({})
const trendRef = ref(null)
const healthRef = ref(null)
const categoryRef = ref(null)
const instances = []

const metrics = computed(() => {
  const source = dashboard.value?.metrics || {}
  return [
    { label: '用户总数', value: source.totalUsers || 0 },
    { label: '商品总数', value: source.totalProducts || 0 },
    { label: '订单总数', value: source.totalOrders || 0 },
    { label: '近30日额', value: `¥${Number(source.totalAmount30d || 0).toFixed(2)}` }
  ]
})

const recentOrders = computed(() => dashboard.value?.recentOrders || [])

const renderCharts = async () => {
  await nextTick()
  instances.forEach((instance) => instance.dispose())
  instances.length = 0

  if (trendRef.value) {
    const chart = echarts.init(trendRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['交易额', '订单量'] },
      xAxis: { type: 'category', data: dashboard.value?.tradeTrend30d?.xAxis || [] },
      yAxis: [{ type: 'value' }, { type: 'value' }],
      series: [
        { name: '交易额', type: 'line', smooth: true, data: dashboard.value?.tradeTrend30d?.amountSeries || [], color: '#0f766e' },
        { name: '订单量', type: 'bar', yAxisIndex: 1, data: dashboard.value?.tradeTrend30d?.orderSeries || [], color: '#1d4ed8' }
      ]
    })
    instances.push(chart)
  }

  if (healthRef.value) {
    const chart = echarts.init(healthRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['42%', '72%'], data: dashboard.value?.healthDistribution || [], label: { formatter: '{b}\n{d}%' } }]
    })
    instances.push(chart)
  }

  if (categoryRef.value) {
    const chart = echarts.init(categoryRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: (dashboard.value?.productCategoryDistribution || []).map((item) => item.name) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (dashboard.value?.productCategoryDistribution || []).map((item) => item.value), color: '#0ea5e9', barWidth: 32 }]
    })
    instances.push(chart)
  }
}

onMounted(async () => {
  const res = await getAdminDashboard()
  dashboard.value = res?.data || {}
  renderCharts()
  window.addEventListener('resize', renderCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', renderCharts)
  instances.forEach((instance) => instance.dispose())
})
</script>

<style scoped>
.metric-grid,
.chart-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.chart-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.metric-card,
.chart-card,
.recent-card {
  padding: 24px;
}

.metric-card span {
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin-top: 16px;
  font-size: 30px;
  color: #10233d;
}

.section-title h3 {
  margin: 0;
}

.chart-box {
  height: 340px;
  margin-top: 14px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 18px;
}

.recent-item {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.recent-item strong,
.recent-item p {
  display: block;
  margin-bottom: 6px;
}

.recent-item span {
  color: #64748b;
}

@media (max-width: 1100px) {
  .metric-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
