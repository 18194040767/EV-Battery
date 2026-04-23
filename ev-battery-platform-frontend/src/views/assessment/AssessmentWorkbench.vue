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
          <span>已选单体</span>
          <strong>{{ selectedBatteryId || '-' }}</strong>
        </div>
        <div class="metric">
          <span>批量数量</span>
          <strong>{{ batchBatteryIds.length }}</strong>
        </div>
        <div class="metric">
          <span>机器学习</span>
          <strong>{{ useML ? 'ON' : 'OFF' }}</strong>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
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

    <el-row :gutter="16">
      <el-col :lg="16" :xs="24">
        <el-card shadow="hover">
          <template #header>当前评估结果</template>
          <el-empty v-if="!currentAssessment" description="暂无评估数据，请先执行评估或从档案页进入" />
          <div v-else class="result-grid">
            <div class="score-panel">
              <el-progress type="dashboard" :percentage="Number(currentAssessment.healthScore || 0)" />
              <strong>{{ currentAssessment.healthLevel || '-' }}</strong>
              <span>评分 {{ currentAssessment.healthScore || 0 }}</span>
            </div>
            <div class="result-cards">
              <div class="result-card">
                <span>规则评分</span>
                <strong>{{ currentAssessment.ruleScore ?? '-' }}</strong>
              </div>
              <div class="result-card">
                <span>机器学习评分</span>
                <strong>{{ currentAssessment.mlScore ?? '-' }}</strong>
              </div>
              <div class="result-card">
                <span>建议场景</span>
                <strong>{{ currentAssessment.suggestedScene || '-' }}</strong>
              </div>
              <div class="result-card">
                <span>评估时间</span>
                <strong>{{ formatDate(currentAssessment.assessmentTime) }}</strong>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :lg="8" :xs="24">
        <el-card shadow="hover">
          <template #header>档案联动快照</template>
          <el-empty v-if="!currentAssessment?.batteryRecord" description="暂无档案信息" />
          <el-descriptions v-else :column="1" border>
            <el-descriptions-item label="电池编码">{{ currentAssessment.batteryCode }}</el-descriptions-item>
            <el-descriptions-item label="档案状态">{{ formatBatteryStatus(currentAssessment.batteryRecord.status) }}</el-descriptions-item>
            <el-descriptions-item label="容量保持率">{{ currentAssessment.batteryRecord.capacityRetentionRate }}</el-descriptions-item>
            <el-descriptions-item label="内阻比">{{ currentAssessment.batteryRecord.internalResistanceRatio }}</el-descriptions-item>
            <el-descriptions-item label="循环次数">{{ currentAssessment.batteryRecord.cycleCount }}</el-descriptions-item>
            <el-descriptions-item label="平均温度">{{ currentAssessment.batteryRecord.avgTemperature }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :lg="14" :xs="24">
        <el-card shadow="hover">
          <template #header>趋势可视化</template>
          <el-empty v-if="!currentAssessment?.trendData?.length" description="暂无趋势数据" />
          <div v-else ref="trendChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :lg="10" :xs="24">
        <el-card shadow="hover">
          <template #header>文字评估报告</template>
          <el-empty v-if="!currentAssessment?.reportContent" description="暂无文字报告" />
          <div v-else class="report-panel">
            <div class="report-title">
              <strong>{{ currentAssessment.reportSummary || 'Assessment report' }}</strong>
              <span>ID {{ currentAssessment.reportId || '-' }}</span>
            </div>
            <pre class="report-text">{{ currentAssessment.reportContent }}</pre>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
    grid: { left: 32, right: 18, top: 24, bottom: 24, containLabel: true },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: currentAssessment.value.trendData.map((item) => item.month)
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [
      {
        type: 'line',
        smooth: true,
        data: currentAssessment.value.trendData.map((item) => item.retention),
        lineStyle: { width: 3, color: '#2f7cf6' },
        areaStyle: { color: 'rgba(47,124,246,0.16)' },
        itemStyle: { color: '#2f7cf6' }
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
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  border-radius: 24px;
  border: 1px solid #dbe8f4;
  background: linear-gradient(135deg, #f7fbff, #eef5ff 58%, #f9fcff);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-copy h2,
.hero-copy p {
  margin: 0;
}

.eyebrow {
  margin-bottom: 8px;
  color: #6482a8;
  font-size: 13px;
}

.desc {
  margin-top: 10px;
  color: #5d7086;
}

.hero-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.metric,
.result-card {
  min-width: 120px;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid #e5eef8;
}

.metric span,
.result-card span {
  display: block;
  color: #70839b;
  font-size: 13px;
}

.metric strong,
.result-card strong {
  display: block;
  margin-top: 8px;
  color: #1e2f45;
  font-size: 20px;
}

.toolbar,
.toolbar-fields,
.toolbar-actions,
.result-grid,
.batch-layout {
  display: flex;
  gap: 12px;
}

.toolbar {
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-fields {
  flex: 1;
  flex-wrap: wrap;
}

.field {
  width: 260px;
}

.field-wide {
  width: 420px;
}

.result-grid {
  align-items: center;
}

.score-panel {
  min-width: 240px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.result-cards {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 12px;
}

.chart {
  height: 320px;
}

.batch-chart {
  flex: 1;
  min-width: 320px;
}

.report-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #4a617d;
}

.report-text {
  margin: 0;
  padding: 16px;
  border-radius: 14px;
  background: #f6f8fb;
  color: #233549;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: Consolas, Monaco, monospace;
}

@media (max-width: 960px) {
  .result-grid,
  .batch-layout {
    flex-direction: column;
  }

  .field,
  .field-wide {
    width: 100%;
  }
}
</style>


