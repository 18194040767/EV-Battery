<template>
  <div class="dashboard-page">
    <section class="metric-row">
      <article v-for="item in metrics" :key="item.label" class="metric-tile">
        <span class="metric-icon">
          <component :is="item.icon" />
        </span>
        <div>
          <p>{{ item.label }}</p>
          <strong>{{ item.value }}</strong>
          <small>
            较昨日
            <b :class="{ flat: item.delta === '0%' }">{{ item.deltaPrefix }} {{ item.delta }}</b>
          </small>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <div class="left-stack">
        <section class="chart-row">
          <article class="dash-card chart-card">
            <PanelTitle v-model="trendDays" title="交易趋势" @change="loadTrend" />
            <div ref="trendRef" class="chart-box" />
          </article>
          <article class="dash-card chart-card">
            <PanelTitle v-model="healthDays" title="健康分布" @change="loadHealth" />
            <div ref="healthRef" class="chart-box" />
          </article>
        </section>

        <section class="chart-row bottom-row">
          <article class="dash-card chart-card">
            <PanelTitle v-model="categoryDays" title="分类占比" @change="loadCategory" />
            <div ref="categoryRef" class="chart-box" />
          </article>
          <article class="dash-card order-card">
            <PanelTitle title="订单动态" :show-filter="false" />
            <div class="recent-list">
              <button v-for="item in recentOrders" :key="item.orderNo" type="button" class="recent-item" @click="go('/admin/orders')">
                <div>
                  <strong>{{ item.orderNo }}</strong>
                  <p>{{ item.title || '电池商品' }}</p>
                  <span>
                    <b :class="statusClass(item.orderStatus)">{{ item.orderStatus || 'PENDING_PAYMENT' }}</b>
                    · ¥{{ formatAmount(item.amount) }}
                  </span>
                </div>
                <time>{{ formatOrderTime(item) }}</time>
              </button>
            </div>
            <button class="more-link" type="button" @click="go('/admin/orders')">
              查看更多订单 <el-icon><ArrowRight /></el-icon>
            </button>
          </article>
        </section>

        <section class="dash-card action-strip">
          <h3>快捷操作</h3>
          <div class="action-list">
            <button v-for="item in actionButtons" :key="item.label" type="button" @click="runAction(item)">
              <component :is="item.icon" />
              {{ item.label }}
            </button>
          </div>
        </section>
      </div>

      <aside class="right-stack">
        <section class="dash-card notice-card">
          <div class="side-title">
            <h3><el-icon><Bell /></el-icon> 消息通知</h3>
            <b v-if="unreadCount">{{ unreadCount }}</b>
          </div>
          <article v-for="item in notices" :key="item.title" class="notice-item">
            <span :class="item.tone">
              <component :is="item.icon" />
            </span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.text }}</p>
              <time>{{ item.time }}</time>
            </div>
            <i v-if="item.unread"></i>
          </article>
          <button class="side-link" type="button" @click="go('/admin/messages')">
            查看全部消息 <el-icon><ArrowRight /></el-icon>
          </button>
        </section>

        <section class="dash-card quick-card">
          <h3>快捷入口</h3>
          <div class="quick-grid">
            <button v-for="item in quickEntries" :key="item.label" type="button" @click="go(item.path)">
              <span><component :is="item.icon" /></span>
              {{ item.label }}
            </button>
          </div>
        </section>

        <section class="dash-card status-card">
          <h3>系统状态</h3>
          <div class="status-row">
            <article v-for="item in systemStatus" :key="item.label" :class="item.tone">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Bell,
  Box,
  Briefcase,
  DataAnalysis,
  Document,
  Download,
  Goods,
  Lock,
  Notebook,
  Operation,
  Setting,
  Tickets,
  User,
  Wallet
} from '@element-plus/icons-vue'
import { getAdminDashboard } from '../../api/admin'
import { getHealthDistribution, getProductCategoryDistribution, getTradeTrend } from '../../api/statistics'
import { getMessageList, getUnreadCount } from '../../api/message'

const rangeOptions = [
  { label: '近7天', value: 7 },
  { label: '近30天', value: 30 },
  { label: '近半年', value: 180 }
]

const PanelTitle = defineComponent({
  props: {
    title: { type: String, required: true },
    modelValue: { type: Number, default: 30 },
    showFilter: { type: Boolean, default: true }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const onChange = (event) => {
      const value = Number(event.target.value)
      emit('update:modelValue', value)
      emit('change', value)
    }
    return () =>
      h('div', { class: 'panel-title' }, [
        h('h3', props.title),
        props.showFilter
          ? h(
              'select',
              { class: 'range-select', value: props.modelValue, onChange },
              rangeOptions.map((item) => h('option', { value: item.value }, item.label))
            )
          : null
      ])
  }
})

const router = useRouter()
const dashboard = ref({})
const trendDays = ref(30)
const healthDays = ref(30)
const categoryDays = ref(30)
const trendData = ref({ xAxis: [], amountSeries: [], orderSeries: [] })
const healthData = ref([])
const categoryData = ref([])
const messageRows = ref([])
const unreadCount = ref(0)

const trendRef = ref(null)
const healthRef = ref(null)
const categoryRef = ref(null)
let trendChart
let healthChart
let categoryChart

const metricSource = computed(() => dashboard.value?.metrics || {})

const metrics = computed(() => [
  { label: '用户总数', value: metricSource.value.totalUsers ?? 0, delta: '12%', deltaPrefix: '↑', icon: User },
  { label: '商品总数', value: metricSource.value.totalProducts ?? 0, delta: '8%', deltaPrefix: '↑', icon: Box },
  { label: '订单总数', value: metricSource.value.totalOrders ?? 0, delta: '15%', deltaPrefix: '↑', icon: Tickets },
  { label: '近30日额', value: `¥${Number(metricSource.value.totalAmount30d ?? 0).toFixed(2)}`, delta: '0%', deltaPrefix: '—', icon: Wallet }
])

const recentOrders = computed(() => dashboard.value?.recentOrders?.slice(0, 4) || [])

const notices = computed(() => {
  if (messageRows.value.length) {
    return messageRows.value.slice(0, 3).map((item, index) => ({
      title: item.title || '系统消息',
      text: item.content || '暂无内容',
      time: formatRelativeTime(item.createdAt),
      icon: index === 0 ? Bell : index === 1 ? Notebook : Lock,
      tone: index === 0 ? 'blue' : index === 1 ? 'green' : 'orange',
      unread: !item.readFlag
    }))
  }
  return [
    { title: '系统公告', text: '暂无新的系统公告', time: '刚刚', icon: Bell, tone: 'blue', unread: false },
    { title: '订单提醒', text: '进入订单管理查看待处理订单', time: '刚刚', icon: Notebook, tone: 'green', unread: false },
    { title: '安全提醒', text: '系统运行正常', time: '刚刚', icon: Lock, tone: 'orange', unread: false }
  ]
})

const quickEntries = [
  { label: '用户管理', icon: User, path: '/admin/users' },
  { label: '订单管理', icon: Tickets, path: '/admin/orders' },
  { label: '商品审核', icon: Box, path: '/admin/products' },
  { label: '档案审核', icon: Operation, path: '/admin/batteries' },
  { label: '合同存证', icon: Document, path: '/admin/contracts' },
  { label: '数据报表', icon: DataAnalysis, path: '/admin/statistics' },
  { label: '消息通知', icon: Bell, path: '/admin/messages' },
  { label: '系统设置', icon: Setting, path: '/admin/system' }
]

const actionButtons = [
  { label: '新增用户', icon: User, type: 'route', path: '/admin/users' },
  { label: '发布商品', icon: Goods, type: 'route', path: '/trade/product-list' },
  { label: '创建合同', icon: Briefcase, type: 'route', path: '/admin/contracts' },
  { label: '导出报表', icon: Download, type: 'export' },
  { label: '批量审核', icon: DataAnalysis, type: 'route', path: '/admin/products' },
  { label: '系统设置', icon: Setting, type: 'route', path: '/admin/system' }
]

const systemStatus = [
  { label: '服务运行', value: '正常', tone: 'ok' },
  { label: '数据库', value: '正常', tone: 'ok' },
  { label: '缓存服务', value: '正常', tone: 'ok' },
  { label: '存储空间', value: '62%', tone: 'info' }
]

const go = (path) => {
  router.push(path)
}

const runAction = (item) => {
  if (item.type === 'export') {
    exportReport()
    return
  }
  go(item.path)
}

const exportReport = () => {
  const lines = [
    ['指标', '数值'],
    ['用户总数', metricSource.value.totalUsers ?? 0],
    ['商品总数', metricSource.value.totalProducts ?? 0],
    ['订单总数', metricSource.value.totalOrders ?? 0],
    ['近30日额', Number(metricSource.value.totalAmount30d ?? 0).toFixed(2)]
  ]
  const csv = lines.map((line) => line.join(',')).join('\n')
  const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `后台数据报表-${dayjs().format('YYYYMMDD-HHmm')}.csv`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('报表已导出')
}

const formatAmount = (value) => Number(value ?? 0).toFixed(0)
const statusClass = (status) => ({ success: status === 'SUCCESS' })
const formatOrderTime = (item) => item.time || formatRelativeTime(item.createdAt)

const formatRelativeTime = (value) => {
  if (!value) return '刚刚'
  const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60000))
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)} 小时前`
  return `${Math.floor(minutes / 1440)} 天前`
}

const normalizeChartRows = (rows, fallback) => (rows?.length ? rows : fallback)

const loadDashboard = async () => {
  const res = await getAdminDashboard()
  dashboard.value = res?.data || {}
}

const loadTrend = async () => {
  const res = await getTradeTrend({ days: trendDays.value })
  trendData.value = res?.data || { xAxis: [], amountSeries: [], orderSeries: [] }
  renderTrend()
}

const loadHealth = async () => {
  const res = await getHealthDistribution({ days: healthDays.value })
  healthData.value = normalizeChartRows(res?.data || [], [])
  renderHealth()
}

const loadCategory = async () => {
  const res = await getProductCategoryDistribution({ days: categoryDays.value })
  categoryData.value = normalizeChartRows(res?.data || [], [])
  renderCategory()
}

const loadMessages = async () => {
  try {
    const [listRes, countRes] = await Promise.all([getMessageList({ page: 1, size: 3 }), getUnreadCount()])
    messageRows.value = listRes?.data?.records || []
    unreadCount.value = countRes?.data?.unreadCount || 0
  } catch {
    messageRows.value = []
    unreadCount.value = 0
  }
}

const renderTrend = async () => {
  await nextTick()
  if (!trendRef.value) return
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  const data = trendData.value
  trendChart.setOption({
    color: ['#146cff', '#cfe0ff'],
    grid: { top: 54, right: 46, bottom: 46, left: 48, containLabel: true },
    tooltip: { trigger: 'axis' },
    legend: {
      top: 8,
      left: 'center',
      itemWidth: 24,
      itemHeight: 12,
      textStyle: { color: '#61708d', fontSize: 12 },
      data: ['交易额', '订单量']
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.xAxis || [],
      axisLine: { lineStyle: { color: '#d7e0ed' } },
      axisTick: { show: false },
      axisLabel: {
        color: '#556887',
        formatter: (value) => dayjs(value).isValid() ? dayjs(value).format('MM-DD') : value,
        hideOverlap: true
      }
    },
    yAxis: [
      { type: 'value', splitLine: { lineStyle: { color: '#e4eaf3' } }, axisLabel: { color: '#536682' } },
      { type: 'value', splitLine: { show: false }, axisLabel: { color: '#536682' } }
    ],
    series: [
      {
        name: '交易额',
        type: 'line',
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(20, 108, 255, 0.16)' },
            { offset: 1, color: 'rgba(20, 108, 255, 0)' }
          ])
        },
        data: data.amountSeries || []
      },
      { name: '订单量', type: 'bar', yAxisIndex: 1, barWidth: 14, itemStyle: { borderRadius: [2, 2, 0, 0] }, data: data.orderSeries || [] }
    ]
  }, true)
}

const renderHealth = async () => {
  await nextTick()
  if (!healthRef.value) return
  if (!healthChart) healthChart = echarts.init(healthRef.value)
  const data = healthData.value
  const compact = healthRef.value.clientWidth < 520
  healthChart.setOption({
    color: ['#176dff', '#8ed07f', '#ffc24a', '#f04e66', '#a8b5c9'],
    tooltip: { trigger: 'item' },
    legend: {
      orient: 'vertical',
      right: 4,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#5b6a86', fontSize: 12 },
      formatter: (name) => {
        const total = data.reduce((sum, row) => sum + Number(row.value || 0), 0)
        const item = data.find((row) => row.name === name)
        const percent = total ? ((Number(item?.value || 0) / total) * 100).toFixed(0) : 0
        return `${name}  ${percent}%`
      }
    },
    series: [
      {
        type: 'pie',
        radius: compact ? ['42%', '68%'] : ['38%', '66%'],
        center: compact ? ['34%', '54%'] : ['39%', '54%'],
        label: { show: !compact, formatter: '{b}\n{d}%', color: '#273755', fontSize: 12 },
        labelLine: { show: !compact, length: 13, length2: 8 },
        avoidLabelOverlap: true,
        data
      }
    ]
  }, true)
}

const renderCategory = async () => {
  await nextTick()
  if (!categoryRef.value) return
  if (!categoryChart) categoryChart = echarts.init(categoryRef.value)
  const rows = categoryData.value
  const names = rows.map((item) => item.name || '未知')
  const values = rows.map((item) => Number(item.value || 0))
  categoryChart.setOption({
    color: ['#176dff'],
    grid: { top: 32, right: 24, bottom: 46, left: 42, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: names,
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#b9c6d8' } },
      axisLabel: { color: '#60708a', interval: 0, hideOverlap: true }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#e2e9f3' } },
      axisLabel: { color: '#60708a' }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 32,
        data: values,
        itemStyle: { color: '#176dff', borderRadius: [2, 2, 0, 0] }
      }
    ]
  }, true)
}

const resizeCharts = () => {
  trendChart?.resize()
  healthChart?.resize()
  categoryChart?.resize()
}

onMounted(async () => {
  await Promise.allSettled([loadDashboard(), loadTrend(), loadHealth(), loadCategory(), loadMessages()])
  window.addEventListener('resize', resizeCharts)
})

watch([trendDays, healthDays, categoryDays], () => nextTick(resizeCharts))

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  healthChart?.dispose()
  categoryChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1480px;
  margin: 0 auto;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-tile,
.dash-card {
  border: 1px solid rgba(54, 94, 150, 0.1);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 34px rgba(50, 86, 150, 0.08);
}

.metric-tile {
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 128px;
  padding: 20px 22px;
}

.metric-icon,
.quick-grid span {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: #f0f5ff;
  color: #126cff;
}

.metric-icon svg {
  width: 29px;
  height: 29px;
}

.metric-tile p {
  margin: 0 0 9px;
  color: #445471;
  font-size: 14px;
  font-weight: 700;
}

.metric-tile strong {
  display: block;
  color: #071331;
  font-size: 27px;
  line-height: 1.1;
  font-weight: 900;
}

.metric-tile small {
  display: flex;
  gap: 16px;
  margin-top: 14px;
  color: #687894;
  font-size: 13px;
  font-weight: 700;
}

.metric-tile small b {
  color: #0bb96d;
  font-weight: 800;
}

.metric-tile small b.flat {
  color: #71809b;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 318px;
  gap: 16px;
  align-items: start;
}

.left-stack,
.right-stack {
  display: grid;
  gap: 16px;
}

.chart-row {
  display: grid;
  grid-template-columns: minmax(0, 1.16fr) minmax(0, 0.98fr);
  gap: 16px;
  align-items: stretch;
}

.dash-card {
  min-width: 0;
  padding: 18px 19px;
}

.chart-card,
.order-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 366px;
}

.order-card {
  grid-template-rows: auto auto auto;
  align-content: start;
  min-height: 366px;
}

.panel-title,
.side-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title h3,
.dash-card h3,
.side-title h3 {
  margin: 0;
  color: #071331;
  font-size: 18px;
  font-weight: 900;
}

.panel-title .range-select {
  height: 32px;
  min-width: 86px;
  padding: 0 30px 0 12px;
  border: 1px solid #dfe6f1;
  border-radius: 8px;
  appearance: none;
  background:
    linear-gradient(45deg, transparent 50%, #7b8aa3 50%) right 13px center / 6px 6px no-repeat,
    linear-gradient(135deg, #ffffff 0%, #f7faff 100%);
  color: #263653;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  line-height: 32px;
  cursor: pointer;
  box-shadow: 0 8px 18px rgba(31, 82, 160, 0.06);
}

.panel-title .range-select:focus {
  outline: none;
  border-color: #9dc2ff;
  box-shadow: 0 0 0 3px rgba(23, 109, 255, 0.12);
}

.chart-box {
  min-height: 294px;
  width: 100%;
}

.recent-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  min-height: 76px;
  padding: 13px 15px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(180deg, #f8fbff 0%, #f4f7fc 100%);
  text-align: left;
  cursor: pointer;
}

.recent-item strong {
  display: block;
  color: #071331;
  font-size: 13px;
  font-weight: 900;
  line-height: 1.2;
  word-break: break-all;
}

.recent-item p {
  margin: 6px 0 0;
  color: #34445f;
  font-size: 13px;
}

.recent-item span,
.recent-item time {
  color: #72809a;
  font-size: 12px;
  white-space: nowrap;
}

.recent-item span b {
  color: #6b7a94;
}

.recent-item span b.success {
  color: #0ab66a;
}

.more-link,
.side-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  border: 0;
  background: transparent;
  color: #126cff;
  font-weight: 800;
  cursor: pointer;
}

.more-link {
  width: 100%;
  margin-top: 14px;
  font-size: 13px;
}

.action-strip {
  display: grid;
  grid-template-columns: 94px 1fr;
  align-items: center;
  gap: 16px;
  min-height: 82px;
  padding: 16px 18px;
}

.action-list {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.action-strip button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  height: 40px;
  border: 1px solid #e0e8f4;
  border-radius: 8px;
  background: #fbfdff;
  color: #4f5f7a;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

.action-strip svg {
  width: 17px;
  height: 17px;
  color: #126cff;
}

.right-stack .dash-card {
  padding: 19px 17px;
}

.notice-card {
  min-height: 350px;
}

.side-title h3 {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
}

.side-title b {
  display: grid;
  place-items: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #126cff;
  color: #ffffff;
  font-size: 12px;
}

.notice-item {
  position: relative;
  display: grid;
  grid-template-columns: 43px 1fr 8px;
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid #edf2f8;
}

.side-title + .notice-item {
  margin-top: 12px;
}

.notice-item > span {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
}

.notice-item .blue {
  background: #edf4ff;
  color: #126cff;
}

.notice-item .green {
  background: #ffffff;
  color: #1f75ff;
}

.notice-item .orange {
  background: #fff1e4;
  color: #ff8a1f;
}

.notice-item strong {
  display: block;
  color: #071331;
  font-size: 14px;
  font-weight: 900;
}

.notice-item p {
  overflow: hidden;
  margin: 5px 0;
  color: #60708b;
  font-size: 12px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-item time {
  color: #6c7892;
  font-size: 12px;
}

.notice-item i {
  align-self: center;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #126cff;
}

.side-link {
  width: 100%;
  margin-top: 5px;
  font-size: 13px;
}

.quick-card {
  min-height: 205px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 17px 10px;
  margin-top: 18px;
}

.quick-grid button {
  display: grid;
  justify-items: center;
  gap: 8px;
  border: 0;
  background: transparent;
  color: #53627d;
  font-size: 12px;
  cursor: pointer;
}

.quick-grid span {
  width: 38px;
  height: 38px;
  border-radius: 11px;
}

.quick-grid svg {
  width: 21px;
  height: 21px;
}

.status-card {
  min-height: 142px;
}

.status-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 7px;
  margin-top: 17px;
}

.status-row article {
  display: grid;
  place-items: center;
  min-height: 58px;
  border-radius: 50%;
  background: #eef8f1;
  color: #16a164;
}

.status-row article.info {
  background: #eef3ff;
  color: #126cff;
}

.status-row span {
  color: #61708a;
  font-size: 11px;
  font-weight: 700;
}

.status-row strong {
  font-size: 14px;
  font-weight: 900;
}

@media (max-width: 1380px) {
  .action-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1320px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .right-stack {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    align-items: start;
  }
}

@media (max-width: 1120px) {
  .metric-row,
  .chart-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .chart-row,
  .metric-row,
  .right-stack {
    grid-template-columns: 1fr;
  }

  .metric-tile {
    min-height: auto;
  }

  .chart-card,
  .order-card {
    min-height: auto;
  }

  .chart-box {
    min-height: 250px;
  }

  .action-strip {
    grid-template-columns: 1fr;
  }

  .action-list {
    grid-template-columns: 1fr;
  }
}
</style>
