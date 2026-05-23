<template>
  <div class="logistics-query-page">
    <section class="page-head">
      <div>
        <p>物流追踪</p>
        <h2>物流查询</h2>
        <span>运单检索追踪，实时掌握运输动态</span>
      </div>
      <div class="search-row">
        <el-input v-model="searchKeyword" placeholder="请输入快递运单号" clearable class="search-input" @keyup.enter="load">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :loading="loading" @click="load">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </section>

    <section class="content-grid">
      <section class="list-panel">
        <div v-if="searchKeyword" class="result-tip">搜索结果：{{ filteredList.length }} 条</div>
        <el-empty v-if="!filteredList.length" description="未找到运单" />
        <div v-else class="card-list">
          <article
            v-for="item in filteredList"
            :key="item.orderId"
            class="tracking-card"
            :class="{ active: activeItem?.orderId === item.orderId }"
            @click="openDetail(item)"
          >
            <div class="tracking-top">
              <span class="select-dot" />
              <span class="package-icon">
                <el-icon><Box /></el-icon>
              </span>
              <strong>{{ item.trackingNo }}</strong>
              <span class="status-pill">{{ item.status || '运输中' }}</span>
            </div>
            <p class="cargo-title">{{ item.title }}</p>
            <div class="route-line">
              <strong>{{ item.route?.origin?.city || '-' }}</strong>
              <el-icon><Right /></el-icon>
              <strong>{{ item.route?.destination?.city || '-' }}</strong>
            </div>
            <p class="meta-line">
              <el-icon><Location /></el-icon>
              当前位置：{{ item.currentCheckpoint?.city || '待揽收' }} · 预计 {{ item.etaDays || 0 }} 天后到达
            </p>
            <p class="meta-line">
              <el-icon><Clock /></el-icon>
              预计到达：2024-05-28（周二）
            </p>
            <div class="tracking-actions">
              <el-button type="primary" @click.stop="openDetail(item)">查看详情</el-button>
              <el-button @click.stop="downloadNotice(item)">下载告知单</el-button>
            </div>
          </article>
        </div>
      </section>

      <section class="detail-panel">
        <el-empty v-if="!activeItem" description="请输入运单号" />
        <template v-else>
          <div class="detail-head">
            <div>
              <strong>{{ activeItem.trackingNo }}</strong>
              <p>{{ activeItem.title }}</p>
            </div>
            <div class="detail-meta">
              <span class="status-pill">{{ activeItem.status || '运输中' }}</span>
              <span>{{ activeItem.currentCheckpoint?.city || '待揽收' }}</span>
              <el-button size="small" type="primary" plain @click="downloadNotice(activeItem)">下载告知单</el-button>
            </div>
          </div>

          <LogisticsMap :order-id="activeItem.orderId" :waybill-no="activeItem.trackingNo" :refresh-token="refreshToken" />
        </template>
      </section>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Box, Clock, Location, Refresh, Right, Search } from '@element-plus/icons-vue'
import { getTradeOrders } from '../../api/trade'
import { downloadHazardousNotice, queryLogisticsStatus } from '../../api/logistics'
import LogisticsMap from '../../components/LogisticsMap.vue'
import { mergeMockLogisticsState } from '../../utils/mockLogistics'
import { downloadPdfResponse, getDownloadErrorMessage } from '../../utils/file'

const route = useRoute()
const loading = ref(false)
const list = ref([])
const activeItem = ref(null)
const searchKeyword = ref(String(route.query.waybillNo || ''))
const refreshToken = ref(Date.now())

const normalizeOrder = (item = {}) => ({
  ...item,
  orderStatus: item.orderStatus || item.order_status || '',
  logisticsNo: item.logisticsNo || item.logistics_no || '',
  logisticsCompany: item.logisticsCompany || item.logistics_company || '',
  orderNo: item.orderNo || item.order_no || ''
})

const filteredList = computed(() => {
  if (!searchKeyword.value) return list.value
  const keyword = searchKeyword.value.trim().toLowerCase()
  return list.value.filter((item) => String(item.trackingNo || '').toLowerCase().includes(keyword))
})

const openDetail = (item) => {
  activeItem.value = item
  refreshToken.value = Date.now()
}

const downloadNotice = async (item) => {
  if (!item?.orderId) {
    ElMessage.warning('缺少订单编号，无法下载告知单')
    return
  }
  try {
    const res = await downloadHazardousNotice(item.orderId)
    await downloadPdfResponse(res, `危险品运输告知单-${item.trackingNo || item.orderId}.pdf`)
  } catch (error) {
    ElMessage.error(await getDownloadErrorMessage(error, '告知单 PDF 下载失败，请稍后重试'))
  }
}

const load = async () => {
  loading.value = true
  try {
    const ordersRes = await getTradeOrders({ tab: 'ALL' })
    const orders = ((ordersRes?.data || []).map(normalizeOrder)).filter((item) =>
      ['SHIPPED_PENDING_RECEIVE', 'COMPLETED_PENDING_REVIEW', 'COMPLETED'].includes(item.orderStatus) || item.logisticsNo
    )
    const trackingList = await Promise.all(
      orders.map(async (item) => {
        const logisticsRes = await queryLogisticsStatus(item.id).catch(() => ({ data: {} }))
        return {
          orderId: item.id,
          orderNo: item.orderNo,
          title: item.productSnapshot?.title || '电池商品',
          ...mergeMockLogisticsState(item.id, logisticsRes.data || {})
        }
      })
    )
    list.value = trackingList.filter((item) => item.trackingNo)

    const exactOrderId = String(route.query.orderId || '')
    activeItem.value =
      list.value.find((item) => String(item.orderId) === exactOrderId) ||
      filteredList.value[0] ||
      list.value[0] ||
      null
    refreshToken.value = Date.now()
  } finally {
    loading.value = false
  }
}

onMounted(load)

watch(searchKeyword, () => {
  const exactKeyword = searchKeyword.value.trim().toLowerCase()
  if (!exactKeyword) {
    activeItem.value = list.value[0] || null
    refreshToken.value = Date.now()
    return
  }
  const matched = filteredList.value[0] || null
  activeItem.value = matched
  refreshToken.value = Date.now()
})
</script>

<style scoped>
.logistics-query-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: calc(100vh - 112px);
  color: #101a33;
}

.content-grid {
  display: grid;
  grid-template-columns: 0.92fr 1.58fr;
  gap: 16px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: center;
  min-height: 158px;
  padding: 28px 34px;
  overflow: hidden;
  border: 1px solid rgba(75, 113, 177, 0.12);
  border-radius: 18px;
  background:rgb(239, 243, 251);
  box-shadow: 0 18px 44px rgba(47, 124, 255, 0.08);
}

.page-head p {
  margin: 0 0 12px;
  color: #2f7cff;
  font-size: 14px;
  font-weight: 700;
}

.page-head h2 {
  margin: 0 0 10px;
  color: #101a33;
  font-size: 30px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.page-head span,
.detail-head p,
.detail-meta span,
.result-tip {
  color: #687791;
}

.search-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex: 0 0 auto;
}

.search-input {
  width: 336px;
}

.search-row :deep(.el-input__wrapper) {
  height: 38px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 0 0 1px rgba(77, 106, 154, 0.18) inset;
}

.search-row :deep(.el-button) {
  height: 38px;
  min-width: 86px;
  border-radius: 6px;
  font-weight: 700;
  box-shadow: 0 8px 16px rgba(31, 117, 255, 0.18);
}

.list-panel,
.detail-panel {
  overflow: hidden;
  padding: 16px;
  border: 1px solid rgba(75, 113, 177, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 18px 42px rgba(47, 124, 255, 0.06);
  backdrop-filter: blur(14px);
}

.list-panel {
  min-height: 640px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tracking-card {
  position: relative;
  min-height: 170px;
  padding: 24px 22px 16px 66px;
  border: 1px solid #e6edf8;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.96);
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.tracking-card::before {
  content: "";
  position: absolute;
  left: 34px;
  top: 48px;
  bottom: 26px;
  width: 1px;
  background: #e3eaf5;
}

.tracking-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(47, 124, 255, 0.08);
}

.tracking-card.active {
  border-color: #2f7cff;
  box-shadow: 0 14px 34px rgba(47, 124, 255, 0.13);
}

.tracking-top,
.tracking-actions,
.detail-head,
.detail-meta,
.route-line,
.meta-line {
  display: flex;
  align-items: center;
}

.tracking-top {
  gap: 12px;
}

.select-dot {
  position: absolute;
  left: 24px;
  top: 31px;
  width: 22px;
  height: 22px;
  border: 1px solid #d5deec;
  border-radius: 50%;
  background: #ffffff;
}

.tracking-card.active .select-dot::after {
  content: "";
  position: absolute;
  inset: 5px;
  border-radius: 50%;
  background: #2f7cff;
  box-shadow: 0 0 0 3px rgba(47, 124, 255, 0.12);
}

.package-icon {
  display: inline-flex;
  width: 31px;
  height: 31px;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 2px solid #6aa4ff;
  border-radius: 50%;
  color: #2f7cff;
  background: #eef5ff;
  font-size: 18px;
}

.tracking-top strong {
  color: #101a33;
  font-size: 16px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 68px;
  height: 28px;
  margin-left: auto;
  padding: 0 12px;
  border-radius: 5px;
  background: #edf4ff;
  color: #2f7cff;
  font-size: 13px;
  font-weight: 700;
}

.cargo-title {
  margin: 14px 0 12px;
  color: #687791;
  font-size: 15px;
}

.route-line {
  gap: 12px;
  margin-bottom: 12px;
  color: #101a33;
  font-size: 15px;
}

.route-line .el-icon {
  color: #2f7cff;
}

.meta-line {
  gap: 7px;
  margin: 8px 0 0;
  color: #687791;
  font-size: 14px;
}

.meta-line .el-icon {
  color: #7d8ca6;
}

.tracking-actions {
  justify-content: space-between;
  flex-wrap: wrap;
  margin-top: 16px;
}

.tracking-actions :deep(.el-button--primary) {
  min-width: 94px;
  height: 34px;
  border-radius: 5px;
  font-weight: 700;
  box-shadow: 0 8px 14px rgba(31, 117, 255, 0.16);
}

.tracking-actions :deep(.el-button.is-text) {
  color: #33415c;
}

.result-tip {
  margin-bottom: 12px;
  font-size: 13px;
}

.detail-panel {
  padding: 18px;
}

.detail-head {
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
  padding: 4px 2px 0;
}

.detail-head strong {
  color: #101a33;
  font-size: 18px;
}

.detail-head p {
  margin: 7px 0 0;
  font-size: 15px;
}

.detail-meta {
  gap: 16px;
  flex-wrap: wrap;
  white-space: nowrap;
}

.detail-meta .status-pill {
  margin-left: 0;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-head,
  .search-row,
  .detail-head,
  .detail-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-head {
    padding: 24px;
    background-position: center;
  }

  .search-input {
    width: 100%;
  }

  .tracking-card {
    padding-right: 16px;
  }
}
</style>
