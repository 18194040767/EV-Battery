<template>
  <div class="page">
    <el-card class="hero" shadow="never">
      <div class="hero-copy">
        <p class="eyebrow">评估中心</p>
        <h2>健康评估</h2>
        <p class="desc">单体与批量评估</p>
      </div>
      <div class="hero-meta">
        <div class="metric">
          <span class="metric-icon"><el-icon><Document /></el-icon></span>
          <div>
            <span>已选单体</span>
            <strong>{{ selectedBatteryId || '-' }}</strong>
          </div>
        </div>
        <div class="metric">
          <span class="metric-icon"><el-icon><Collection /></el-icon></span>
          <div>
            <span>批量数量</span>
            <strong>{{ batchBatteryIds.length }}</strong>
          </div>
        </div>
        <div class="metric">
          <span class="metric-icon"><el-icon><Cpu /></el-icon></span>
          <div>
            <span>机器学习</span>
            <strong>{{ useML ? 'ON' : 'OFF' }}</strong>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-fields">
          <el-select v-model="selectedBatteryId" filterable clearable placeholder="选择单个电池档案" class="field">
            <el-option
              v-for="item in batteryOptions"
              :key="item.id"
              :label="`${item.batteryCode} / ${item.sourceType || 'manual'}`"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="batchBatteryIds" multiple filterable collapse-tags clearable placeholder="选择批量评估档案" class="field field-wide">
            <el-option
              v-for="item in batteryOptions"
              :key="item.id"
              :label="`${item.batteryCode} / ${formatBatteryStatus(item.status)}`"
              :value="item.id"
            />
          </el-select>
          <el-switch v-model="useML" inline-prompt active-text="ML" inactive-text="Rule" />
        </div>
        <div class="toolbar-actions">
          <el-button :loading="singleLoading" type="primary" @click="runSingleAssessment">开始评估</el-button>
          <el-button :loading="batchLoading" @click="runBatchAssessment">一键批量评估</el-button>
          <el-button @click="loadLatestAssessment">读取最新结果</el-button>
        </div>
      </div>
    </el-card>

    <section class="assessment-result-board">
      <div class="result-panel result-panel--score">
        <div class="panel-title">
          <span class="panel-icon"><el-icon><Histogram /></el-icon></span>
          <strong>当前评估结果</strong>
        </div>
        <div v-if="!currentAssessment" class="empty-state">
          <img src="/empty-state.png" alt="" />
          <p>暂无评估数据，请先执行评估或从档案页进入</p>
        </div>
        <div v-else class="score-layout">
          <div class="score-panel">
            <svg class="score-gauge" viewBox="0 0 220 150" aria-label="健康评分">
              <defs>
                <linearGradient id="scoreGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stop-color="#8db0ff" />
                  <stop offset="52%" stop-color="#6f97f8" />
                  <stop offset="100%" stop-color="#5a86ef" />
                </linearGradient>
              </defs>
              <path class="gauge-track" d="M 25 125 A 85 85 0 0 1 195 125" pathLength="100" />
              <path
                class="gauge-value"
                d="M 25 125 A 85 85 0 0 1 195 125"
                pathLength="100"
                :stroke-dasharray="`${Number(currentAssessment.healthScore || 0)} 100`"
              />
            </svg>
            <div class="score-number">
              <strong>{{ currentAssessment.healthScore || 0 }}</strong><span>%</span>
            </div>
            <b>{{ currentAssessment.healthLevel || '-' }}</b>
            <p>评分 {{ currentAssessment.healthScore || 0 }}</p>
          </div>
          <div class="result-cards">
            <div class="result-card">
              <span class="card-icon"><el-icon><CircleCheckFilled /></el-icon></span>
              <div>
                <span>规则评分</span>
                <strong>{{ currentAssessment.ruleScore ?? '-' }}</strong>
              </div>
            </div>
            <div class="result-card">
              <span class="card-icon"><el-icon><Cpu /></el-icon></span>
              <div>
                <span>机器学习评分</span>
                <strong>{{ currentAssessment.mlScore ?? '-' }}</strong>
              </div>
            </div>
            <div class="result-card">
              <span class="card-icon"><el-icon><Van /></el-icon></span>
              <div>
                <span>建议场景</span>
                <strong>{{ currentAssessment.suggestedScene || '-' }}</strong>
              </div>
            </div>
            <div class="result-card">
              <span class="card-icon"><el-icon><Clock /></el-icon></span>
              <div>
                <span>评估时间</span>
                <strong>{{ formatDate(currentAssessment.assessmentTime) }}</strong>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="result-panel result-panel--snapshot">
        <div class="panel-title">
          <span class="panel-icon"><el-icon><Files /></el-icon></span>
          <strong>档案联动快照</strong>
        </div>
        <div v-if="!currentAssessment?.batteryRecord" class="empty-state">
          <img src="/empty-state.png" alt="" />
          <p>暂无档案信息</p>
        </div>
        <div v-else class="snapshot-table">
          <div class="snapshot-row">
            <span><el-icon><PriceTag /></el-icon>电池编码</span>
            <strong>{{ currentAssessment.batteryCode }}</strong>
          </div>
          <div class="snapshot-row">
            <span><el-icon><FolderChecked /></el-icon>档案状态</span>
            <strong>{{ formatBatteryStatus(currentAssessment.batteryRecord.status) }}</strong>
          </div>
          <div class="snapshot-row">
            <span><el-icon><Collection /></el-icon>容量保持率</span>
            <strong>{{ currentAssessment.batteryRecord.capacityRetentionRate }}</strong>
          </div>
          <div class="snapshot-row">
            <span><el-icon><Connection /></el-icon>内阻比</span>
            <strong>{{ currentAssessment.batteryRecord.internalResistanceRatio }}</strong>
          </div>
          <div class="snapshot-row">
            <span><el-icon><Refresh /></el-icon>循环次数</span>
            <strong>{{ currentAssessment.batteryRecord.cycleCount }}</strong>
          </div>
          <div class="snapshot-row">
            <span><el-icon><Odometer /></el-icon>平均温度</span>
            <strong>{{ currentAssessment.batteryRecord.avgTemperature }}</strong>
          </div>
        </div>
      </div>

      <div class="result-panel result-panel--trend">
        <div class="panel-title">
          <span class="panel-icon"><el-icon><TrendCharts /></el-icon></span>
          <strong>趋势可视化</strong>
        </div>
        <div v-if="!currentAssessment?.trendData?.length" class="empty-state">
          <img src="/empty-state.png" alt="" />
          <p>暂无趋势数据</p>
        </div>
        <div v-else ref="trendChartRef" class="chart"></div>
      </div>

      <div class="result-panel result-panel--report">
        <div class="panel-title">
          <span class="panel-icon"><el-icon><Document /></el-icon></span>
          <strong>文字评估报告</strong>
        </div>
        <div v-if="!currentAssessment?.reportContent" class="empty-state">
          <img src="/empty-state.png" alt="" />
          <p>暂无报告数据</p>
        </div>
        <div v-else class="report-panel">
          <div class="report-title">
            <strong>{{ currentAssessment.reportSummary || '评估报告' }}</strong>
            <span>ID {{ currentAssessment.reportId || '-' }}</span>
          </div>
          <pre class="report-text">{{ currentAssessment.reportContent }}</pre>
          <div class="report-watermark" aria-hidden="true">
            <el-icon><Document /></el-icon>
            <el-icon><CircleCheckFilled /></el-icon>
          </div>
        </div>
      </div>
    </section>

    <el-card shadow="hover">
      <template #header>评估历史</template>
      <el-empty v-if="!historyList.length" description="当前电池暂无历史记录" />
      <el-table v-else :data="historyList" @row-click="focusAssessment">
        <el-table-column prop="id" label="评估ID" width="100" />
        <el-table-column prop="batteryCode" label="电池编码" min-width="160" />
        <el-table-column prop="healthScore" label="健康分" width="100" />
        <el-table-column prop="healthLevel" label="等级" width="120" />
        <el-table-column prop="mlScore" label="ML分数" width="120" />
        <el-table-column prop="assessmentTime" label="评估时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.assessmentTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="hover">
      <template #header>批量评估结果</template>
      <el-empty v-if="!batchResults.length" description="暂无批量结果" />
      <div v-else class="batch-layout">
        <div ref="batchChartRef" class="chart batch-chart"></div>
        <el-table :data="batchResults" @row-click="focusAssessment">
          <el-table-column prop="batteryCode" label="电池编码" min-width="160" />
          <el-table-column prop="healthScore" label="健康分" width="100" />
          <el-table-column prop="healthLevel" label="等级" width="120" />
          <el-table-column prop="ruleScore" label="规则分" width="100" />
          <el-table-column prop="mlScore" label="ML分" width="100" />
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  CircleCheckFilled,
  Clock,
  Collection,
  Connection,
  Cpu,
  Document,
  Files,
  FolderChecked,
  Histogram,
  Odometer,
  PriceTag,
  Refresh,
  TrendCharts,
  Van
} from '@element-plus/icons-vue'
import { getBatteryList } from '../../api/battery'
import { getAssessmentHistory, getAssessmentReport, getBatchAssessmentTask, getLatest, triggerAssessment, triggerBatchAssessment } from '../../api/assessment'
import { normalizeId, normalizeIdList } from '../../utils/id'
import { formatBatteryStatus } from '../../utils/batteryStatus'
import { extractErrorMessage } from '../../utils/requestError'

const route = useRoute()
const router = useRouter()

const batteryOptions = ref([])
const selectedBatteryId = ref(null)
const batchBatteryIds = ref([])
const useML = ref(true)
const currentAssessment = ref(null)
const historyList = ref([])
const batchResults = ref([])
const singleLoading = ref(false)
const batchLoading = ref(false)
const trendChartRef = ref(null)
const batchChartRef = ref(null)

let trendChart = null
let batchChart = null
let batchTaskTimer = null

const formatDate = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}

const updateRoute = () => {
  router.replace({
    path: '/assessment',
    query: {
      batteryId: selectedBatteryId.value ? String(selectedBatteryId.value) : undefined,
      assessmentId: currentAssessment.value?.id ? String(currentAssessment.value.id) : undefined,
      batteryIds: batchBatteryIds.value.length ? batchBatteryIds.value.join(',') : undefined
    }
  })
}

const renderTrendChart = () => {
  if (!trendChartRef.value || !currentAssessment.value?.trendData?.length) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    grid: { left: 34, right: 24, top: 22, bottom: 30, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.94)',
      borderColor: '#e7eefb',
      borderWidth: 1,
      padding: [12, 16],
      textStyle: { color: '#17264b', fontSize: 14 },
      extraCssText: 'border-radius:12px;box-shadow:0 14px 36px rgba(38,83,160,.14);'
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: currentAssessment.value.trendData.map((item) => item.month),
      axisLine: { lineStyle: { color: '#dfe8f7' } },
      axisTick: { show: false },
      axisLabel: { color: '#566890', fontSize: 12, margin: 14 }
    },
    yAxis: {
      type: 'value',
      min: 80,
      max: 100,
      interval: 5,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#566890', fontSize: 13 },
      splitLine: { lineStyle: { color: '#dfe8f7', type: 'dashed' } }
    },
    series: [
      {
        type: 'line',
        smooth: true,
        data: currentAssessment.value.trendData.map((item) => item.retention),
        symbol: 'circle',
        symbolSize: 9,
        lineStyle: { width: 2, color: '#357dff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(58,126,255,0.18)' },
            { offset: 1, color: 'rgba(58,126,255,0.02)' }
          ])
        },
        itemStyle: { color: '#ffffff', borderColor: '#357dff', borderWidth: 2 },
        emphasis: {
          scale: 1.8,
          itemStyle: { color: '#357dff', borderColor: '#c7dcff', borderWidth: 4 }
        }
      }
    ]
  })
}

const renderBatchChart = () => {
  if (!batchChartRef.value || !batchResults.value.length) return
  if (!batchChart) batchChart = echarts.init(batchChartRef.value)
  batchChart.setOption({
    grid: { left: 32, right: 18, top: 24, bottom: 60, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: batchResults.value.map((item) => item.batteryCode),
      axisLabel: { interval: 0, rotate: 20 }
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [
      {
        type: 'bar',
        data: batchResults.value.map((item) => item.healthScore || 0),
        itemStyle: {
          color: '#22a06b',
          borderRadius: [8, 8, 0, 0]
        }
      }
    ]
  })
}

const loadBatteryOptions = async () => {
  const res = await getBatteryList({ page: 1, size: 200 })
  batteryOptions.value = (res?.data?.records || []).map((item) => ({
    ...item,
    id: normalizeId(item.id)
  }))
}

const loadHistory = async (batteryId) => {
  if (!batteryId) {
    historyList.value = []
    return
  }
  const res = await getAssessmentHistory({ batteryId: normalizeId(batteryId) })
  historyList.value = res?.data || []
}

const loadAssessmentById = async (assessmentId) => {
  if (!assessmentId) return
  const res = await getAssessmentReport(normalizeId(assessmentId))
  currentAssessment.value = res?.data || null
  if (currentAssessment.value?.batteryId) {
    selectedBatteryId.value = normalizeId(currentAssessment.value.batteryId)
    await loadHistory(selectedBatteryId.value)
  }
}

const loadLatestAssessment = async () => {
  try {
    if (!selectedBatteryId.value) {
      ElMessage.warning('请先选择电池档案')
      return
    }
    const res = await getLatest(selectedBatteryId.value)
    currentAssessment.value = res?.data || null
    await loadHistory(selectedBatteryId.value)
    updateRoute()
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '读取结果失败'))
  }
}

const loadBatchLatest = async (batteryIds) => {
  const results = await Promise.all(
    batteryIds.map(async (id) => {
      try {
        const res = await getLatest(id)
        return res?.data || null
      } catch (error) {
        return null
      }
    })
  )
  batchResults.value = results.filter(Boolean)
  if (batchResults.value.length) {
    currentAssessment.value = batchResults.value[0]
    selectedBatteryId.value = normalizeId(batchResults.value[0].batteryId)
    await loadHistory(selectedBatteryId.value)
  }
  updateRoute()
}

const runSingleAssessment = async () => {
  if (!selectedBatteryId.value) {
    ElMessage.warning('请先选择要评估的电池档案')
    return
  }
  singleLoading.value = true
  try {
    const res = await triggerAssessment(selectedBatteryId.value, useML.value)
    if (res?.code !== 200 || !res?.data) {
      ElMessage.error(res?.message || '评估失败')
      return
    }
    currentAssessment.value = res.data
    batchResults.value = []
    const option = batteryOptions.value.find((item) => Number(item.id) === Number(selectedBatteryId.value))
    if (option) option.status = 'ASSESSED'
    await loadHistory(selectedBatteryId.value)
    await loadBatteryOptions()
    updateRoute()
    ElMessage.success('评估完成，结果已刷新')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '评估失败'))
  } finally {
    singleLoading.value = false
  }
}

const runBatchAssessment = async () => {
  if (!batchBatteryIds.value.length) {
    ElMessage.warning('请先选择批量评估档案')
    return
  }
  batchLoading.value = true
  try {
    const res = await triggerBatchAssessment(batchBatteryIds.value, useML.value)
    const taskId = res?.data?.taskId
    if (!taskId) {
      ElMessage.error(res?.message || '批量评估启动失败')
      batchLoading.value = false
      return
    }
    ElMessage.success('批量评估已启动，正在等待结果落库')
    if (batchTaskTimer) clearInterval(batchTaskTimer)
    batchTaskTimer = setInterval(async () => {
      const task = await getBatchAssessmentTask(taskId)
      if (task?.data?.finished) {
        clearInterval(batchTaskTimer)
        batchTaskTimer = null
        batchLoading.value = false
        const taskResults = (task?.data?.results || []).filter(Boolean)
        if (taskResults.length) {
          batchResults.value = taskResults
          currentAssessment.value = taskResults[0]
          selectedBatteryId.value = normalizeId(taskResults[0].batteryId)
          await loadHistory(selectedBatteryId.value)
          updateRoute()
        } else {
          await loadBatchLatest([...batchBatteryIds.value])
        }
        await loadBatteryOptions()
        ElMessage.success(`批量评估完成：${task.data.completed}/${task.data.total}`)
      }
    }, 1500)
  } catch (error) {
    batchLoading.value = false
    ElMessage.error(extractErrorMessage(error, '批量评估失败'))
  }
}

const focusAssessment = async (row) => {
  if (!row?.id) return
  await loadAssessmentById(normalizeId(row.id))
  updateRoute()
}

const hydrateFromRoute = async () => {
  const queryBatteryId = normalizeId(route.query.batteryId)
  const queryAssessmentId = normalizeId(route.query.assessmentId)
  const queryBatteryIds = normalizeIdList(String(route.query.batteryIds || '').split(','))

  if (queryBatteryId) selectedBatteryId.value = queryBatteryId
  if (queryBatteryIds.length) batchBatteryIds.value = queryBatteryIds

  if (queryAssessmentId) {
    await loadAssessmentById(queryAssessmentId)
    return
  }
  if (queryBatteryId) {
    try {
      await loadLatestAssessment()
    } catch (error) {
      historyList.value = []
    }
  }
  if (queryBatteryIds.length) {
    await loadBatchLatest(queryBatteryIds)
  }
}

watch(() => currentAssessment.value?.id, async () => {
  await nextTick()
  renderTrendChart()
})

watch(() => batchResults.value.length, async () => {
  await nextTick()
  renderBatchChart()
})

onMounted(async () => {
  await loadBatteryOptions()
  await hydrateFromRoute()
  await nextTick()
  renderTrendChart()
  renderBatchChart()
  window.addEventListener('resize', renderTrendChart)
  window.addEventListener('resize', renderBatchChart)
})

onBeforeUnmount(() => {
  if (batchTaskTimer) clearInterval(batchTaskTimer)
  if (trendChart) trendChart.dispose()
  if (batchChart) batchChart.dispose()
  window.removeEventListener('resize', renderTrendChart)
  window.removeEventListener('resize', renderBatchChart)
})
</script>

<style scoped>
.page {
  min-height: calc(100vh - 112px);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 0 0 18px;
  color: #061a44;
  font-family: "Inter", "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.hero,
.toolbar-card,
.result-panel {
  border: 1px solid rgba(209, 224, 246, 0.92);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 36px rgba(27, 75, 150, 0.07);
}

.hero {
  min-height: 260px;
  position: relative;
  overflow: hidden;
  border-radius: 24px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 250, 255, 0.83) 42%, rgba(242, 248, 255, 0.12) 100%),
    url("/assessment-hero-bg.png") center right / cover no-repeat;
}

.hero :deep(.el-card__body) {
  min-height: 260px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 30px 38px 28px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
}

.hero-copy h2,
.hero-copy p {
  margin: 0;
}

.eyebrow {
  width: fit-content;
  padding: 4px 13px;
  border-radius: 13px;
  background: linear-gradient(180deg, #e9f2ff 0%, #dbeaff 100%);
  color: #1476ff;
  font-size: 15px;
  font-weight: 800;
}

.hero-copy h2 {
  color: #071d4c;
  font-size: 34px;
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: 0;
}

.desc {
  color: #587196;
  font-size: 15px;
  font-weight: 700;
}

.hero-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.metric {
  flex: 1 1 220px;
  max-width: 280px;
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid rgba(212, 226, 246, 0.98);
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 28px rgba(35, 82, 150, 0.04);
}

.metric:first-child {
  flex-basis: 270px;
  max-width: 320px;
}

.metric > div {
  min-width: 0;
  flex: 1 1 auto;
}

.metric-icon,
.panel-icon {
  display: inline-grid;
  place-items: center;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  border-radius: 10px;
  background: linear-gradient(180deg, #eff6ff 0%, #e2efff 100%);
  color: #2f80ff;
  box-shadow: inset 0 0 0 1px rgba(76, 137, 255, 0.08);
}

.metric-icon .el-icon,
.panel-icon .el-icon {
  font-size: 24px;
}

.metric span:not(.metric-icon) {
  display: block;
  color: #7b8eaf;
  font-size: 14px;
  font-weight: 700;
}

.metric strong {
  display: block;
  margin-top: 8px;
  color: #061a44;
  font-size: clamp(17px, 2.1vw, 22px);
  line-height: 1.12;
  font-weight: 900;
  overflow-wrap: anywhere;
  word-break: break-all;
}

.toolbar-card {
  border-radius: 20px;
}

.toolbar-card :deep(.el-card__body) {
  padding: 27px 38px;
}

.toolbar,
.toolbar-fields,
.toolbar-actions,
.batch-layout {
  display: flex;
  gap: 16px;
}

.toolbar {
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.toolbar-fields {
  flex: 1 1 660px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-actions {
  flex: 0 0 auto;
  align-items: center;
  gap: 18px;
}

.field {
  width: 292px;
}

.field-wide {
  width: 300px;
}

:deep(.toolbar .el-select__wrapper) {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 0 0 1px #d7e3f4 inset;
}

:deep(.toolbar .el-select__placeholder) {
  color: #8a9ab8;
  font-size: 15px;
  font-weight: 600;
}

:deep(.toolbar .el-switch) {
  --el-switch-on-color: #2f80ff;
  --el-switch-off-color: #b7c6df;
}

:deep(.toolbar .el-switch__label) {
  color: #2f80ff;
  font-weight: 800;
}

:deep(.toolbar-actions .el-button) {
  min-width: 132px;
  height: 43px;
  margin-left: 0;
  border-radius: 8px;
  border-color: #d6e2f2;
  background: rgba(255, 255, 255, 0.86);
  color: #193061;
  font-size: 16px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(34, 82, 155, 0.04);
}

:deep(.toolbar-actions .el-button--primary) {
  border-color: transparent;
  background: linear-gradient(180deg, #3d8cff 0%, #1f6eff 100%);
  color: #ffffff;
  box-shadow: 0 12px 22px rgba(33, 111, 255, 0.26);
}

.assessment-result-board {
  display: grid;
  grid-template-columns: minmax(520px, 1fr) minmax(420px, 1fr);
  gap: 22px;
}

.result-panel {
  min-height: 270px;
  overflow: hidden;
  border-radius: 15px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.9)),
    radial-gradient(circle at 50% 68%, rgba(83, 136, 226, 0.08), transparent 28%);
}

.result-panel--score {
  container-type: inline-size;
}

.result-panel--score,
.result-panel--snapshot,
.result-panel--trend,
.result-panel--report {
  min-height: 270px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 15px;
  min-height: 58px;
  padding: 0 24px;
  border-bottom: 1px solid #dce6f5;
  color: #081d4a;
  font-size: 22px;
  font-weight: 900;
}

.panel-title strong {
  font-weight: 900;
}

.empty-state {
  min-height: 210px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 18px 20px 24px;
  color: #9aa8bd;
  text-align: center;
}

.empty-state img {
  width: min(360px, 88%);
  aspect-ratio: 16 / 10;
  object-fit: contain;
}

.empty-state p {
  margin: 12px 0 0;
  color: #99a7bc;
  font-size: 16px;
  line-height: 1.6;
}

.score-layout {
  min-height: 210px;
  display: grid;
  grid-template-columns: minmax(220px, 0.9fr) minmax(0, 1.25fr);
  gap: clamp(16px, 2cqw, 24px);
  align-items: center;
  padding: clamp(18px, 2.8cqw, 26px);
  overflow: visible;
}

.score-panel {
  position: relative;
  min-width: 0;
  min-height: 212px;
  display: grid;
  justify-items: center;
  align-content: start;
  color: #17264b;
}

.score-gauge {
  width: min(236px, 100%);
  height: auto;
  aspect-ratio: 236 / 160;
  overflow: visible;
}

.score-gauge path {
  fill: none;
  stroke-linecap: round;
  stroke-width: 15;
}

.gauge-track {
  stroke: #dfe8ff;
}

.gauge-value {
  stroke: url("#scoreGradient");
  filter: drop-shadow(0 8px 12px rgba(72, 126, 241, 0.18));
}

.score-number {
  position: absolute;
  top: 72px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  color: #17264b;
}

.score-number strong {
  font-size: clamp(42px, 7cqw, 54px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: 0;
}

.score-number span {
  margin-left: 5px;
  font-size: 24px;
  font-weight: 800;
}

.score-panel b {
  margin-top: -4px;
  color: #17264b;
  font-size: clamp(18px, 3cqw, 22px);
  line-height: 1.2;
  text-align: center;
  overflow-wrap: anywhere;
}

.score-panel p {
  margin: 10px 0 0;
  color: #506189;
  font-size: 17px;
}

.result-cards {
  display: grid;
  min-width: 0;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 190px), 1fr));
  gap: 14px;
}

.result-card {
  min-width: 0;
  min-height: 96px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: clamp(14px, 2cqw, 20px);
  border: 1px solid #dfe8f6;
  border-radius: 13px;
  background: rgba(255, 255, 255, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.result-card > div {
  min-width: 0;
}

.result-card span:not(.card-icon) {
  display: block;
  color: #5c6b91;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.result-card strong {
  display: block;
  margin-top: 12px;
  color: #17264b;
  font-size: clamp(18px, 2.5cqw, 20px);
  line-height: 1.25;
  font-weight: 800;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.card-icon {
  flex: 0 0 auto;
  color: #5d8cff;
  font-size: 22px;
  line-height: 1;
}

@container (max-width: 720px) {
  .score-layout {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .score-panel {
    min-height: 190px;
  }

  .score-gauge {
    width: min(220px, 82%);
  }

  .score-number {
    top: 62px;
  }

  .result-cards {
    grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
  }
}

.snapshot-table {
  margin: 22px 26px 26px;
  overflow: hidden;
  border: 1px solid #dfe8f6;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.52);
}

.snapshot-row {
  display: grid;
  grid-template-columns: 38% 62%;
  min-height: 48px;
  border-bottom: 1px solid #e3ebf7;
}

.snapshot-row:last-child {
  border-bottom: 0;
}

.snapshot-row span {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  background: rgba(247, 250, 255, 0.78);
  color: #40527e;
  font-size: 14px;
  font-weight: 700;
}

.snapshot-row span .el-icon {
  color: #578bff;
  font-size: 19px;
}

.snapshot-row:nth-child(3) span .el-icon {
  color: #42c78a;
}

.snapshot-row:nth-child(4) span .el-icon {
  color: #8f74ff;
}

.snapshot-row:nth-child(5) span .el-icon {
  color: #ffc154;
}

.snapshot-row:nth-child(6) span .el-icon {
  color: #ff626b;
}

.snapshot-row strong {
  display: flex;
  align-items: center;
  padding: 0 20px;
  color: #17264b;
  font-size: 15px;
  font-weight: 600;
}

.result-panel .chart {
  height: 208px;
  margin: 8px 18px 18px;
}

.chart {
  height: 320px;
}

.batch-chart {
  flex: 1;
  min-width: 320px;
}

.report-panel {
  position: relative;
  min-height: 210px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px 26px 26px;
}

.report-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #2d74ff;
  font-size: 16px;
}

.report-title strong,
.report-title span {
  color: #2d74ff;
  font-weight: 800;
}

.report-text {
  min-height: 150px;
  margin: 0;
  padding: 20px 22px;
  border: 1px solid #e2ebf8;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.55);
  color: #203258;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  font-size: 16px;
  line-height: 1.9;
}

.report-watermark {
  position: absolute;
  right: 26px;
  bottom: 18px;
  color: rgba(101, 121, 239, 0.25);
  font-size: 54px;
}

.report-watermark .el-icon:last-child {
  position: absolute;
  right: -6px;
  bottom: -2px;
  color: rgba(101, 121, 239, 0.56);
  font-size: 28px;
}

.page > :deep(.el-card:not(.hero):not(.toolbar-card)) {
  border: 1px solid rgba(209, 224, 246, 0.92);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 36px rgba(27, 75, 150, 0.07);
}

.page > :deep(.el-card:not(.hero):not(.toolbar-card) .el-card__header) {
  min-height: 58px;
  display: flex;
  align-items: center;
  padding: 0 24px;
  color: #081d4a;
  font-size: 20px;
  font-weight: 900;
  border-bottom: 1px solid #dce6f5;
}

.page > :deep(.el-card:not(.hero):not(.toolbar-card) .el-card__body) {
  padding: 24px;
}

@media (max-width: 1080px) {
  .assessment-result-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .hero,
  .toolbar-card,
  .result-panel {
    border-radius: 16px;
  }

  .hero {
    min-height: 360px;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(246, 250, 255, 0.9) 48%, rgba(242, 248, 255, 0.26) 100%),
      url("/assessment-hero-bg.png") 58% bottom / auto 72% no-repeat;
  }

  .hero :deep(.el-card__body) {
    min-height: 360px;
    padding: 24px;
  }

  .hero-copy h2 {
    font-size: 30px;
  }

  .desc {
    font-size: 18px;
  }

  .hero-meta,
  .toolbar,
  .toolbar-fields,
  .toolbar-actions,
  .batch-layout {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
  }

  .metric,
  .field,
  .field-wide,
  :deep(.toolbar-actions .el-button) {
    width: 100%;
  }

  .score-layout {
    grid-template-columns: 1fr;
  }

  .result-cards {
    grid-template-columns: 1fr;
  }

  .snapshot-row {
    grid-template-columns: 1fr;
  }

  .snapshot-row span,
  .snapshot-row strong {
    min-height: 44px;
  }
}
</style>
