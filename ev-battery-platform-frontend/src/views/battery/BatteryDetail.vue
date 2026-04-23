<template>
  <div class="detail-page">
    <div class="toolbar">
      <el-button @click="$router.back()">返回</el-button>
      <div class="toolbar-actions">
        <el-button @click="jumpRelative(-1)">上一条</el-button>
        <el-button @click="jumpRelative(1)">下一条</el-button>
        <el-button type="primary" @click="exportSummary">导出摘要</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :lg="8" :xs="24">
        <el-card shadow="hover">
          <h2>{{ detail.batteryCode }}</h2>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="来源">{{ detail.sourceType }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ formatBatteryStatus(detail.status) }}</el-descriptions-item>
            <el-descriptions-item label="电压">{{ detail.voltage }}</el-descriptions-item>
            <el-descriptions-item label="循环次数">{{ detail.cycleCount }}</el-descriptions-item>
            <el-descriptions-item label="内阻比">{{ detail.internalResistanceRatio }}</el-descriptions-item>
            <el-descriptions-item label="容量保持率">{{ detail.capacityRetentionRate }}</el-descriptions-item>
            <el-descriptions-item label="平均温度">{{ detail.avgTemperature }}</el-descriptions-item>
            <el-descriptions-item label="标签">
              <el-tag v-for="tag in detail.tags || []" :key="tag.id" class="tag-chip">{{ tag.name }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :lg="16" :xs="24">
        <el-card shadow="hover">
          <template #header>关键时间轴</template>
          <el-timeline>
            <el-timeline-item v-for="item in detail.timeline || []" :key="item.title + item.time" :timestamp="String(item.time || '-')">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :lg="14" :xs="24">
        <el-card shadow="hover">
          <template #header>最新评估结果</template>
          <el-empty v-if="!detail.latestAssessment" description="暂无评估数据" />
          <div v-else>
            <el-progress type="dashboard" :percentage="Number(detail.latestAssessment.healthScore || 0)" />
            <p>{{ detail.latestAssessment.healthLevel }}</p>
            <p>{{ detail.latestAssessment.llmSummary }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :lg="10" :xs="24">
        <el-card shadow="hover">
          <template #header>相似电池推荐</template>
          <el-empty v-if="!(detail.similarBatteries || []).length" description="暂无相似电池" />
          <el-card v-for="item in detail.similarBatteries || []" :key="item.id" class="similar-card" shadow="never" @click="$router.push('/battery/detail/' + item.id)">
            <strong>{{ item.batteryCode }}</strong>
            <p>{{ item.sourceType }} · 循环 {{ item.cycleCount }} 次 · {{ item.voltage }}V</p>
          </el-card>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBatteryDetail, getBatteryList } from '../../api/battery'
import { formatBatteryStatus } from '../../utils/batteryStatus'
import { normalizeId } from '../../utils/id'

const route = useRoute()
const router = useRouter()
const detail = ref({})
const orderedIds = ref([])

const loadDetail = async () => {
  detail.value = (await getBatteryDetail(route.params.id)).data || {}
}

const loadNeighborIds = async () => {
  const res = await getBatteryList({ page: 1, size: 200 })
  orderedIds.value = (res?.data?.records || []).map((item) => normalizeId(item.id))
}

const jumpRelative = (offset) => {
  const index = orderedIds.value.indexOf(normalizeId(route.params.id))
  const target = orderedIds.value[index + offset]
  if (target) router.push('/battery/detail/' + target)
}

const exportSummary = () => {
  const text = JSON.stringify(detail.value, null, 2)
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `battery-${detail.value.batteryCode || route.params.id}.txt`
  link.click()
  URL.revokeObjectURL(link.href)
}

onMounted(async () => {
  await Promise.all([loadDetail(), loadNeighborIds()])
})
</script>

<style scoped>
.detail-page { display: flex; flex-direction: column; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-actions { display: flex; gap: 10px; }
.tag-chip { margin-right: 6px; }
.similar-card { margin-bottom: 12px; cursor: pointer; }
@media (max-width: 768px) { .toolbar { flex-direction: column; align-items: stretch; } .toolbar-actions { flex-wrap: wrap; } }
</style>
