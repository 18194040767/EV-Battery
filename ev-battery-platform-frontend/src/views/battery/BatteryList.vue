<template>
  <div class="page">
    <el-card class="hero" shadow="never">
      <div>
        <p class="eyebrow">档案中心</p>
        <h2>电池档案管理</h2>
        <p class="desc">档案录入与评估</p>
      </div>
      <div class="hero-actions">
        <el-upload :auto-upload="false" :show-file-list="false" :on-change="onSingleChange">
          <el-button type="primary">上传文件</el-button>
        </el-upload>
        <el-upload multiple :auto-upload="false" :show-file-list="false" :on-change="onBatchChange">
          <el-button>批量导入</el-button>
        </el-upload>
        <el-button @click="openCreate()">手动添加</el-button>
      </div>
    </el-card>

    <div class="stats">
      <el-card shadow="hover">
        <span>档案总数</span>
        <strong>{{ stats.total || 0 }}</strong>
      </el-card>
      <el-card shadow="hover">
        <span>已评估</span>
        <strong>{{ stats.statusCounts?.ASSESSED || 0 }}</strong>
      </el-card>
      <el-card shadow="hover">
        <span>待评估</span>
        <strong>{{ stats.statusCounts?.PENDING_ASSESSMENT || 0 }}</strong>
      </el-card>
      <el-card shadow="hover">
        <span>平均健康分</span>
        <strong>{{ stats.averageHealthScore || 0 }}</strong>
      </el-card>
    </div>

    <el-card shadow="never">
      <div class="toolbar">
        <div class="toolbar-fields">
          <el-input v-model="filters.keyword" clearable placeholder="搜索电池编码" class="field" @keyup.enter="loadList" />
          <el-select v-model="filters.status" clearable placeholder="状态" class="field" @change="loadList">
            <el-option label="待评估" value="PENDING_ASSESSMENT" />
            <el-option label="已评估" value="ASSESSED" />
            <el-option label="已交易" value="TRADED" />
            <el-option label="已下架" value="OFFLINE" />
          </el-select>
          <el-button @click="loadList">查询</el-button>
        </div>
        <div class="toolbar-actions">
          <el-button :disabled="!selectedIds.length" :loading="batchLoading" @click="runBatchAssessment">批量评估</el-button>
          <el-button :disabled="!selectedIds.length" type="danger" @click="removeBatch">批量删除</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="records" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="batteryCode" label="电池编码" min-width="180" />
        <el-table-column prop="sourceType" label="来源" min-width="120" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">{{ formatBatteryStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="voltage" label="电压(V)" width="110" />
        <el-table-column prop="cycleCount" label="循环次数" width="120" />
        <el-table-column label="最新健康分" min-width="180">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.latestHealthScore || 0)" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column prop="latestHealthLevel" label="健康等级" width="120" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openDetail(row)">详情</el-button>
            <el-button link @click="runSingleAssessment(row)">评估</el-button>
            <el-button link @click="openCreate(row)">编辑</el-button>
            <el-button link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        @current-change="changePage"
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
  gap: 16px;
}

.hero {
  border-radius: 24px;
  border: 1px solid #dbe8f4;
  background: linear-gradient(135deg, #f7fbff, #eef5ff);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.eyebrow {
  margin: 0 0 8px;
  color: #6482a8;
  font-size: 13px;
}

.desc {
  margin: 8px 0 0;
  color: #60748a;
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
}

.field {
  width: 220px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stats .el-card span {
  display: block;
  color: #73849b;
  font-size: 13px;
}

.stats .el-card strong {
  display: block;
  margin-top: 10px;
  color: #223348;
  font-size: 28px;
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
}

@media (max-width: 768px) {
  .stats {
    grid-template-columns: 1fr;
  }

  .field {
    width: 100%;
  }
}
</style>


