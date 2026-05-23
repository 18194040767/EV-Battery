<template>
  <div class="battery-detail-page">
    <section class="detail-grid">
      <article class="panel-card identity-card">
        <button class="back-link" type="button" @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </button>
        <div class="identity-body">
          <div class="battery-art">
            <img src="/battery-detail-hero.png" alt="" />
          </div>
          <div class="identity-info">
            <div class="title-row">
              <h1>{{ detail.batteryCode || '-' }}</h1>
              <span class="status-pill">{{ formatBatteryStatus(detail.status) }}</span>
            </div>
            <dl class="meta-list">
              <div>
                <el-icon><UserFilled /></el-icon>
                <dt>来源:</dt>
                <dd>{{ detail.sourceType || '-' }}</dd>
              </div>
              <div>
                <el-icon><CircleCheck /></el-icon>
                <dt>状态:</dt>
                <dd>{{ formatBatteryStatus(detail.status) }}</dd>
              </div>
              <div>
                <el-icon><PriceTag /></el-icon>
                <dt>标签:</dt>
                <dd>{{ tagText }}</dd>
              </div>
            </dl>
          </div>
        </div>
      </article>

      <article class="panel-card timeline-card">
        <h2>关键时间轴</h2>
        <div class="timeline-line">
          <div v-for="(item, index) in timelineRows" :key="item.title + index" class="timeline-item" :class="`tone-${index % 3}`">
            <span class="node-dot"></span>
            <span class="node-icon">
              <el-icon><FolderOpened v-if="index === 0" /><CircleCheck v-else-if="index === 1" /><Tickets v-else /></el-icon>
            </span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description || '-' }}</p>
              <small>{{ item.extra || '-' }}</small>
            </div>
            <time>{{ formatTime(item.time) }}</time>
          </div>
        </div>
      </article>

      <article class="panel-card base-card">
        <h2>电池基础信息</h2>
        <div class="metric-grid">
          <div v-for="item in baseMetrics" :key="item.label" class="metric-cell">
            <span><el-icon><component :is="item.icon" /></el-icon></span>
            <div>
              <p>{{ item.label }}</p>
              <strong>{{ item.value }} <small>{{ item.unit }}</small></strong>
            </div>
          </div>
        </div>
      </article>

      <article class="panel-card result-card">
        <h2>最新评估结果</h2>
        <el-empty v-if="!latest" description="暂无评估数据" />
        <div v-else class="assessment-summary">
          <div class="score-ring" :style="{ '--score': scoreValue }">
            <strong>{{ scoreValue }}<small>%</small></strong>
          </div>
          <div class="assessment-copy">
            <span>{{ latest.healthLevel || '-' }}</span>
            <p>{{ latest.llmSummary || latest.reportSummary || '电池状态良好，推荐用于低速电动车或家庭储能。因已有一定循环衰减且内阻偏高，日常请避免深度充放电，注意散热以延长寿命。' }}</p>
          </div>
        </div>
      </article>
    </section>

    <article class="panel-card similar-section">
      <h2>相似电池推荐</h2>
      <div class="similar-grid">
        <button v-for="(item, index) in similarRows" :key="item.id || index" class="similar-card" type="button" @click="item.id && $router.push('/battery/detail/' + item.id)">
          <span class="similar-icon" :class="`tone-${index % 4}`"><el-icon><Cellphone /></el-icon></span>
          <div>
            <strong>{{ item.batteryCode || '-' }}</strong>
            <p>{{ item.sourceType || 'dataset-import' }}</p>
            <small>循环 {{ item.cycleCount || '-' }} 次 · {{ item.voltage || '-' }}V</small>
          </div>
        </button>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft,
  Cellphone,
  Connection,
  FolderOpened,
  Odometer,
  PriceTag,
  Refresh,
  CircleCheck,
  Tickets,
  UserFilled,
  Warning,
  Lightning,
  Box
} from '@element-plus/icons-vue'
import { getBatteryDetail } from '../../api/battery'
import { formatBatteryStatus } from '../../utils/batteryStatus'

const route = useRoute()
const detail = ref({})

const latest = computed(() => detail.value.latestAssessment || null)
const scoreValue = computed(() => Number(latest.value?.healthScore || 0))
const tagText = computed(() => (detail.value.tags || []).map((tag) => tag.name).join('、') || '-')

const baseMetrics = computed(() => [
  { label: '电压', value: detail.value.voltage ?? '-', unit: 'V', icon: Lightning },
  { label: '循环次数', value: detail.value.cycleCount ?? '-', unit: '次', icon: Refresh },
  { label: '内阻比', value: detail.value.internalResistanceRatio ?? '-', unit: '', icon: Connection },
  { label: '容量保持率', value: detail.value.capacityRetentionRate ?? '-', unit: '%', icon: CircleCheck },
  { label: '平均温度', value: detail.value.avgTemperature ?? '-', unit: '°C', icon: Odometer },
  { label: '-', value: '-', unit: '', icon: Box }
])

const timelineRows = computed(() => {
  const rows = detail.value.timeline || []
  if (rows.length) return rows
  return [
    { title: '档案创建', description: '创建电池档案', extra: '-', time: detail.value.createdAt },
    { title: '健康评估', description: `评分 ${scoreValue.value || '-'} / 等级 ${latest.value?.healthLevel || '-'}`, time: latest.value?.assessmentTime },
    { title: '健康评估', description: `评分 ${scoreValue.value || '-'} / 等级 ${latest.value?.healthLevel || '-'}`, time: latest.value?.createdAt || detail.value.updatedAt }
  ]
})

const similarRows = computed(() => {
  const rows = detail.value.similarBatteries || []
  if (rows.length) return rows
  return [
    { batteryCode: 'EVB-DS-1776628420056-354', sourceType: 'dataset-import', cycleCount: 600, voltage: 365 },
    { batteryCode: 'EVB-DS-1776628424686-205', sourceType: 'dataset-import', cycleCount: 600, voltage: 365 },
    { batteryCode: 'EVB-1776744113182-974', sourceType: '上传导入', cycleCount: 600, voltage: 365 },
    { batteryCode: 'EVB-DS-1776628426688-100', sourceType: 'dataset-import', cycleCount: 390, voltage: 365 }
  ]
})

const formatTime = (time) => (time ? String(time).replace('T', ' ') : '-')

const loadDetail = async () => {
  detail.value = (await getBatteryDetail(route.params.id)).data || {}
}

onMounted(loadDetail)
watch(() => route.params.id, loadDetail)
</script>

<style scoped>
.battery-detail-page {
  display: grid;
  gap: 18px;
  color: #071331;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.86fr) minmax(0, 1.1fr);
  gap: 18px;
}

.panel-card {
  border: 1px solid #e2eaf5;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 44px rgba(48, 92, 160, 0.08);
}

.identity-card {
  overflow: hidden;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 70px;
  padding: 0 28px;
  border: 0;
  border-bottom: 1px solid #edf2f8;
  background: transparent;
  color: #526481;
  font-size: 15px;
  cursor: pointer;
}

.identity-body {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  gap: 26px;
  align-items: center;
  padding: 24px;
}

.battery-art {
  display: grid;
  place-items: center;
  width: 208px;
  height: 208px;
  border-radius: 12px;
  background: linear-gradient(145deg, #f8fbff, #f1f6ff);
}

.battery-art img {
  width: 178px;
  height: 178px;
  object-fit: contain;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.title-row h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.2;
  font-weight: 900;
}

.status-pill {
  padding: 6px 10px;
  border-radius: 6px;
  background: #e5f8ef;
  color: #18a35e;
  font-weight: 800;
}

.meta-list {
  display: grid;
  gap: 24px;
  margin: 34px 0 0;
}

.meta-list div {
  display: grid;
  grid-template-columns: 20px auto 1fr;
  gap: 10px;
  align-items: center;
  color: #65738e;
}

.meta-list dd,
.meta-list dt {
  margin: 0;
}

.meta-list dd {
  color: #273755;
}

.timeline-card,
.base-card,
.result-card,
.similar-section {
  padding: 26px;
}

.panel-card h2 {
  margin: 0 0 24px;
  font-size: 20px;
}

.timeline-line {
  display: grid;
  gap: 32px;
  padding: 2px 0 8px 34px;
  border-top: 1px solid #edf2f8;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 56px 1fr auto;
  gap: 18px;
  align-items: center;
  padding-top: 20px;
}

.timeline-item::before {
  position: absolute;
  left: -8px;
  top: -16px;
  bottom: -32px;
  width: 2px;
  background: #dfe8f5;
  content: "";
}

.node-dot {
  position: absolute;
  left: -14px;
  top: 30px;
  z-index: 1;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #dbe8ff;
}

.node-icon,
.metric-cell > span,
.similar-icon {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: #eaf3ff;
  color: #267dff;
  font-size: 25px;
}

.tone-1 .node-icon,
.tone-1.node-dot {
  background: #ddf6e8;
  color: #35bc70;
}

.tone-2 .node-icon,
.tone-2.node-dot {
  background: #fff0e2;
  color: #ff7d1f;
}

.timeline-item strong {
  font-size: 16px;
}

.timeline-item p {
  margin: 10px 0 4px;
  color: #273755;
}

.timeline-item small,
.timeline-item time {
  color: #7383a0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.metric-cell {
  display: flex;
  align-items: center;
  gap: 22px;
  min-height: 88px;
  padding: 18px;
  border: 1px solid #e4ebf5;
  border-radius: 10px;
  background: linear-gradient(135deg, #ffffff, #f9fbff);
}

.metric-cell p {
  margin: 0 0 6px;
  color: #667692;
}

.metric-cell strong {
  font-size: 24px;
}

.metric-cell small {
  font-size: 14px;
}

.assessment-summary {
  display: grid;
  grid-template-columns: 210px 1fr;
  gap: 38px;
  align-items: center;
}

.score-ring {
  display: grid;
  place-items: center;
  width: 176px;
  height: 176px;
  border-radius: 50%;
  background:
    radial-gradient(circle at center, #ffffff 61%, transparent 62%),
    conic-gradient(#6f9bf8 calc(var(--score) * 1%), #eef3fb 0);
  box-shadow: inset 0 0 0 1px #e7edf6;
}

.score-ring strong {
  font-size: 36px;
}

.score-ring small {
  font-size: 20px;
}

.assessment-copy span {
  display: inline-flex;
  margin-bottom: 26px;
  padding: 6px 12px;
  border-radius: 8px;
  background: #eef5ff;
  color: #2079ff;
  font-weight: 800;
}

.assessment-copy p {
  max-width: 600px;
  margin: 0;
  color: #273755;
  font-size: 17px;
  line-height: 1.8;
}

.similar-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.similar-card {
  display: flex;
  gap: 18px;
  align-items: center;
  min-width: 0;
  padding: 20px 16px;
  border: 1px solid #e3ebf6;
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
}

.similar-icon {
  border-radius: 18px;
}

.similar-icon.tone-1 {
  background: #dff6e9;
  color: #36bd70;
}

.similar-icon.tone-2 {
  background: #fff0e1;
  color: #ff8827;
}

.similar-icon.tone-3 {
  background: #f1e7ff;
  color: #8e5cff;
}

.similar-card strong {
  display: block;
  overflow-wrap: anywhere;
}

.similar-card p,
.similar-card small {
  color: #53637f;
}

.similar-card p {
  margin: 10px 0 6px;
}

@media (max-width: 1180px) {
  .detail-grid,
  .assessment-summary,
  .identity-body {
    grid-template-columns: 1fr;
  }

  .similar-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .metric-grid,
  .similar-grid {
    grid-template-columns: 1fr;
  }

  .timeline-item {
    grid-template-columns: 46px 1fr;
  }

  .timeline-item time {
    grid-column: 2;
  }
}
</style>
