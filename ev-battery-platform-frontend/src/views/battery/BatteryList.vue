<template>
  <div class="page">
    <el-card class="hero" shadow="never">
      <div class="hero-copy">
        <p class="eyebrow">档案中心 <span>›</span></p>
        <h2>电池档案管理</h2>
        <p class="desc">档案录入与评估</p>
       </div>
      <div class="hero-actions">
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="onSingleChange">
          <el-button type="primary" :icon="Upload">上传文件</el-button>
        </el-upload>
        <el-upload multiple :auto-upload="false" :show-file-list="false" :on-change="onBatchChange">
          <el-button :icon="UploadFilled">批量导入</el-button>
        </el-upload>
        <el-button :icon="CirclePlus" @click="openCreate()">手动添加</el-button>
      </div>
    </el-card>

    <div class="stats">
      <el-card class="stat-card stat-card--blue" shadow="never">
        <div class="stat-icon"><el-icon><FolderOpened /></el-icon></div>
        <div>
          <span>档案总数</span>
          <strong>{{ stats.total || 0 }}</strong>
        </div>
      </el-card>
      <el-card class="stat-card stat-card--green" shadow="never">
        <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
        <div>
          <span>已评估</span>
          <strong>{{ stats.statusCounts?.ASSESSED || 0 }}</strong>
        </div>
      </el-card>
      <el-card class="stat-card stat-card--orange" shadow="never">
        <div class="stat-icon"><el-icon><Clock /></el-icon></div>
        <div>
          <span>待评估</span>
          <strong>{{ stats.statusCounts?.PENDING_ASSESSMENT || 0 }}</strong>
        </div>
      </el-card>
      <el-card class="stat-card stat-card--purple" shadow="never">
        <div class="stat-icon"><el-icon><TrendCharts /></el-icon></div>
        <div>
          <span>平均健康分</span>
          <strong>{{ stats.averageHealthScore || 0 }}</strong>
        </div>
      </el-card>
    </div>

    <el-card class="toolbar-card" shadow="never">
      <div class="toolbar">
        <div class="toolbar-fields">
          <el-input v-model="filters.keyword" clearable placeholder="搜索电池编码" class="field" :suffix-icon="Search" @keyup.enter="loadList" />
          <el-select v-model="filters.status" clearable placeholder="状态" class="field" @change="loadList">
            <el-option label="待评估" value="PENDING_ASSESSMENT" />
            <el-option label="已评估" value="ASSESSED" />
            <el-option label="已交易" value="TRADED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
          <el-button type="primary" @click="loadList">查询</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button :disabled="!selectedIds.length" :loading="batchLoading" :icon="Histogram" @click="runBatchAssessment">批量评估</el-button>
          <el-button :disabled="!selectedIds.length" type="danger" :icon="Delete" @click="removeBatch">批量删除</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="records" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="batteryCode" label="电池编码" min-width="180" />
        <el-table-column prop="sourceType" label="来源" min-width="120" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <el-tag class="status-tag" type="success" effect="light">{{ formatBatteryStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="voltage" label="电压(V)" width="110" />
        <el-table-column prop="cycleCount" label="循环次数" width="120" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link class="blue-action" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button link class="assess-action" :icon="TrendCharts" @click="runSingleAssessment(row)">评估</el-button>
              <el-button link class="blue-action" :icon="EditPen" @click="openCreate(row)">编辑</el-button>
              <el-button link type="danger" :icon="Delete" @click="removeOne(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next, sizes"
        :current-page="page"
        :page-size="pageSize"
        :page-sizes="[10, 12, 20, 50]"
        :total="total"
        @current-change="changePage"
        @size-change="changePageSize"
      />
    </el-card>

    <el-drawer v-model="drawerVisible" :title="form.id ? '编辑电池档案' : '新建电池档案'" size="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="来源">
          <el-input v-model="form.sourceType" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="待评估" value="PENDING_ASSESSMENT" />
            <el-option label="已评估" value="ASSESSED" />
            <el-option label="已交易" value="TRADED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="电压(V)">
              <el-input-number v-model="form.voltage" :controls="false" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="循环次数">
              <el-input-number v-model="form.cycleCount" :controls="false" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容量保持率(%)">
              <el-input-number v-model="form.capacityRetentionRate" :controls="false" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="内阻比">
              <el-input-number v-model="form.internalResistanceRatio" :controls="false" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平均温度">
              <el-input-number v-model="form.avgTemperature" :controls="false" :precision="2" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="drawer-footer">
          <el-button @click="drawerVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">保存</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CirclePlus, Clock, Delete, EditPen, FolderOpened, Histogram, Search, TrendCharts, Upload, UploadFilled, View } from '@element-plus/icons-vue'
import { createBatteryManual, deleteBattery, deleteBatteryBatch, getBatteryList, getBatteryStatistics, updateBattery, uploadBatch, uploadSingle } from '../../api/battery'
import { getBatchAssessmentTask, getLatest, triggerAssessment, triggerBatchAssessment } from '../../api/assessment'
import { normalizeId } from '../../utils/id'
import { formatBatteryStatus } from '../../utils/batteryStatus'
import { extractErrorMessage } from '../../utils/requestError'

const router = useRouter()

const loading = ref(false)
const batchLoading = ref(false)
const drawerVisible = ref(false)
const records = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(12)
const selectedIds = ref([])
const stats = reactive({ total: 0, averageHealthScore: 0, statusCounts: {} })
const filters = reactive({ keyword: '', status: '' })
const form = reactive({
  id: null,
  sourceType: '手动录入',
  status: 'PENDING_ASSESSMENT',
  voltage: 365,
  capacityRetentionRate: 90,
  internalResistanceRatio: 0.12,
  cycleCount: 520,
  avgTemperature: 25,
  remark: ''
})

let batchTaskTimer = null

const buildQuery = () => ({
  page: page.value,
  size: pageSize.value,
  keyword: filters.keyword || undefined,
  statuses: filters.status ? [filters.status] : undefined
})

const loadStats = async () => {
  Object.assign(stats, (await getBatteryStatistics()).data || {})
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await getBatteryList(buildQuery())
    records.value = (res?.data?.records || []).map((item) => ({
      ...item,
      id: normalizeId(item.id)
    }))
    total.value = res?.data?.total || 0
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  await Promise.all([loadStats(), loadList()])
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map((item) => normalizeId(item.id))
}

const changePage = (value) => {
  page.value = value
  loadList()
}

const changePageSize = (value) => {
  pageSize.value = value
  page.value = 1
  loadList()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    sourceType: '手动录入',
    status: 'PENDING_ASSESSMENT',
    voltage: 365,
    capacityRetentionRate: 90,
    internalResistanceRatio: 0.12,
    cycleCount: 520,
    avgTemperature: 25,
    remark: ''
  })
}

const openCreate = (row) => {
  if (!row) {
    resetForm()
  } else {
    Object.assign(form, {
      id: row.id,
      sourceType: row.sourceType,
      status: row.status,
      voltage: row.voltage,
      capacityRetentionRate: row.capacityRetentionRate,
      internalResistanceRatio: row.internalResistanceRatio,
      cycleCount: row.cycleCount,
      avgTemperature: row.avgTemperature,
      remark: row.remark || ''
    })
  }
  drawerVisible.value = true
}

const submitForm = async () => {
  try {
    const payload = {
      sourceType: form.sourceType,
      status: form.status,
      voltage: form.voltage,
      capacityRetentionRate: form.capacityRetentionRate,
      internalResistanceRatio: form.internalResistanceRatio,
      cycleCount: form.cycleCount,
      avgTemperature: form.avgTemperature,
      remark: form.remark
    }
    if (form.id) {
      await updateBattery(form.id, payload)
    } else {
      await createBatteryManual(payload)
    }
    drawerVisible.value = false
    await refresh()
    ElMessage.success(form.id ? '档案已更新' : '档案已创建')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '保存失败'))
  }
}

const openDetail = (row) => {
  router.push(`/battery/detail/${row.id}`)
}

const runSingleAssessment = async (row) => {
  try {
    const res = await triggerAssessment(row.id, true)
    if (res?.code !== 200 || !res?.data) {
      ElMessage.error(res?.message || '评估失败')
      return
    }
    row.status = 'ASSESSED'
    row.latestHealthScore = res.data.healthScore
    row.latestHealthLevel = res.data.healthLevel
    await refresh()
    router.push({
      path: '/assessment',
      query: {
        batteryId: String(row.id),
        assessmentId: String(res.data.id)
      }
    })
    ElMessage.success('评估完成，已打开结果页')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '评估失败'))
  }
}

const runBatchAssessment = async () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请先选择要批量评估的档案')
    return
  }
  batchLoading.value = true
  try {
    const batteryIds = [...selectedIds.value]
    const res = await triggerBatchAssessment(batteryIds, true)
    const taskId = res?.data?.taskId
    if (!taskId) {
      ElMessage.error(res?.message || '批量评估启动失败')
      batchLoading.value = false
      return
    }
    if (batchTaskTimer) clearInterval(batchTaskTimer)
    batchTaskTimer = setInterval(async () => {
      const task = await getBatchAssessmentTask(taskId)
      if (task?.data?.finished) {
        clearInterval(batchTaskTimer)
        batchTaskTimer = null
        batchLoading.value = false
        const taskResults = (task?.data?.results || []).filter(Boolean)
        const latestResults = taskResults.length ? taskResults : await Promise.all(
          batteryIds.map(async (id) => {
            try {
              const latest = await getLatest(id)
              return latest?.data || null
            } catch (error) {
              return null
            }
          })
        )
        const firstResult = latestResults.find(Boolean)
        records.value = records.value.map((item) =>
          batteryIds.includes(normalizeId(item.id))
            ? {
                ...item,
                status: 'ASSESSED'
              }
            : item
        )
        await refresh()
        router.push({
          path: '/assessment',
          query: {
            batteryIds: batteryIds.join(','),
            batteryId: firstResult?.batteryId ? String(firstResult.batteryId) : batteryIds[0],
            assessmentId: firstResult?.id ? String(firstResult.id) : undefined
          }
        })
        ElMessage.success(`批量评估完成：${task.data.completed}/${task.data.total}`)
      }
    }, 1500)
  } catch (error) {
    batchLoading.value = false
    ElMessage.error(extractErrorMessage(error, '批量评估失败'))
  }
}

const removeOne = async (row) => {
  await ElMessageBox.confirm(`确认删除档案 ${row.batteryCode} 吗？`, '删除确认')
  await deleteBattery(row.id)
  await refresh()
  ElMessage.success('档案已删除')
}

const removeBatch = async () => {
  if (!selectedIds.value.length) return
  await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 条档案吗？`, '批量删除')
  await deleteBatteryBatch({ ids: selectedIds.value })
  selectedIds.value = []
  await refresh()
  ElMessage.success('批量删除完成')
}

const onSingleChange = async (file) => {
  try {
    const formData = new FormData()
    formData.append('file', file.raw)
    const res = await uploadSingle(formData)
    await refresh()
    const warnings = res?.data?.warnings || []
    ElMessage.success(warnings.length ? `上传成功，但需补充: ${warnings.join('；')}` : '文件上传成功，档案已生成')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '文件上传失败'))
  }
}

const onBatchChange = async (uploadFile, uploadFiles) => {
  const files = (uploadFiles || []).map((item) => item.raw).filter(Boolean)
  if (!files.length) return
  try {
    const formData = new FormData()
    files.forEach((item) => formData.append('files', item))
    const res = await uploadBatch(formData)
    await refresh()
    const failCount = Number(res?.data?.failCount || 0)
    ElMessage.success(failCount ? `批量导入完成，成功 ${res.data.successCount} 个，失败 ${failCount} 个` : '批量文件上传成功')
  } catch (error) {
    ElMessage.error(extractErrorMessage(error, '批量上传失败'))
  }
}

onMounted(async () => {
  await refresh()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  min-height: 284px;
  border: 1px solid #dfeaff;
  border-radius: 18px;
  background:
    linear-gradient(90deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.86) 34%, rgba(255, 255, 255, 0.16) 58%),
    url('/battery-archive-hero.png') center right / cover no-repeat;
  box-shadow: 0 18px 44px rgba(55, 105, 190, 0.08);
}

.hero :deep(.el-card__body) {
  display: flex;
  min-height: 284px;
  flex-direction: column;
  justify-content: center;
  padding: 28px 36px;
}

.hero-copy {
  max-width: 560px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #657895;
  font-size: 16px;
  font-weight: 600;
}

.eyebrow span {
  color: #334b73;
  font-size: 26px;
  line-height: 0;
  vertical-align: -2px;
}

.hero h2 {
  margin: 0;
  color: #0b1228;
  font-size: 40px;
  line-height: 1.18;
  font-weight: 800;
}

.desc {
  margin: 14px 0 0;
  color: #61728c;
  font-size: 20px;
}

.hero-note {
  width: 330px;
  margin: 28px 0 0 548px;
  color: #526684;
  font-size: 15px;
  line-height: 1.75;
}

.hero-actions {
  margin-top: 15px;
}

.hero-actions,
.toolbar,
.toolbar-fields,
.toolbar-actions,
.drawer-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar {
  justify-content: space-between;
  flex-wrap: nowrap;
  gap: 16px;
}

.toolbar-fields {
  flex: 1 1 auto;
  flex-wrap: nowrap;
}

.toolbar-actions {
  margin-left: auto;
  flex: 0 0 auto;
}

.field {
  width: 220px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  border: 1px solid #e3ebf7;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(47, 92, 164, 0.06);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  min-height: 122px;
  align-items: center;
  gap: 22px;
  padding: 22px 24px;
}

.stat-icon {
  display: inline-flex;
  width: 66px;
  height: 66px;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 36px;
}

.stat-card--blue .stat-icon {
  color: #2576ff;
  background: #eef5ff;
}

.stat-card--green .stat-icon {
  color: #1f75ff;
  background: #ffffff;
}

.stat-card--orange .stat-icon {
  color: #ff7a1a;
  background: #fff3e9;
}

.stat-card--purple .stat-icon {
  color: #8b61ff;
  background: #f3efff;
}

.stats .el-card span {
  display: block;
  color: #3f4b62;
  font-size: 16px;
}

.stats .el-card strong {
  display: block;
  margin-top: 8px;
  color: #07112a;
  font-size: 36px;
  line-height: 1;
  font-weight: 800;
}

.toolbar-card,
.table-card {
  border: 1px solid #e4ebf7;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 12px 30px rgba(47, 92, 164, 0.06);
}

.toolbar-card :deep(.el-card__body) {
  padding: 22px 24px;
}

.table-card :deep(.el-card__body) {
  padding: 20px 22px 16px;
}

.toolbar :deep(.el-input__wrapper),
.toolbar :deep(.el-select__wrapper) {
  height: 44px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #d7e0ef inset;
}

.toolbar :deep(.el-button) {
  height: 44px;
  min-width: 98px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
}

.hero-actions :deep(.el-button) {
  height: 48px;
  min-width: 144px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
}

.table-card :deep(.el-table) {
  color: #2c3b55;
  font-size: 16px;
}

.table-card :deep(.el-table__header th) {
  height: 58px;
  color: #334159;
  font-weight: 700;
  background: #ffffff;
}

.table-card :deep(.el-table__row td) {
  height: 60px;
}

.status-tag {
  height: 28px;
  padding: 0 14px;
  border: none;
  border-radius: 6px;
  color: #0d9a7d;
  background: #dbf6ef;
  font-weight: 700;
}

.assess-action {
  color: #10a37f;
}

.blue-action {
  color: #1f75ff;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: nowrap;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}

.pager :deep(.el-pager li.is-active) {
  color: #ffffff;
  background: #1f75ff;
  border-radius: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 960px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-note {
    margin-left: 0;
  }
}

@media (max-width: 768px) {
  .hero {
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(255, 255, 255, 0.9) 58%, rgba(255, 255, 255, 0.2) 100%),
      url('/battery-archive-hero.png') center right / cover no-repeat;
  }

  .hero :deep(.el-card__body) {
    padding: 24px;
  }

  .hero h2 {
    font-size: 32px;
  }

  .stats {
    grid-template-columns: 1fr;
  }

  .field {
    width: 100%;
  }

  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions {
    margin-left: 0;
  }
}
</style>
