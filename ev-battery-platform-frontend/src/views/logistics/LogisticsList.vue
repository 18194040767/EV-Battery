<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>物流追踪</p>
        <h2>物流查询</h2>
        <span>运单检索</span>
      </div>
      <div class="search-row">
        <el-input v-model="searchKeyword" placeholder="请输入快递运单号" clearable class="search-input" @keyup.enter="load" />
        <el-button @click="load">刷新</el-button>
      </div>
    </section>

    <section class="content-grid">
      <section class="panel-card list-panel">
        <div v-if="searchKeyword" class="result-tip">
          搜索结果：{{ filteredList.length }} 条
        </div>
        <el-empty v-if="!filteredList.length" description="未找到运单" />
        <div v-else class="card-list">
          <article
            v-for="item in filteredList"
            :key="item.orderId"
            class="tracking-card"
            :class="{ active: activeItem?.orderId === item.orderId }"
          >
            <div class="tracking-top">
              <strong>{{ item.trackingNo }}</strong>
              <el-tag>{{ item.status || '运输中' }}</el-tag>
            </div>
            <p>{{ item.title }}</p>
            <p>{{ item.route?.origin?.city || '-' }} 至 {{ item.route?.destination?.city || '-' }}</p>
            <p>当前位置：{{ item.currentCheckpoint?.city || '待揽收' }} · 预计 {{ item.etaDays || 0 }} 天后到达</p>
            <div class="tracking-actions">
              <el-button type="primary" plain @click="openDetail(item)">查看详情</el-button>
              <el-button text @click="$router.push('/trade/order-list')">返回订单中心</el-button>
            </div>
          </article>
        </div>
      </section>

      <section class="panel-card detail-panel">
        <el-empty v-if="!activeItem" description="请输入运单号" />
        <template v-else>
          <div class="detail-head">
            <div>
              <strong>{{ activeItem.trackingNo }}</strong>
              <p>{{ activeItem.title }}</p>
            </div>
            <div class="detail-meta">
              <el-tag>{{ activeItem.status }}</el-tag>
              <span>{{ activeItem.currentCheckpoint?.city || '待揽收' }}</span>
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
import { getTradeOrders } from '../../api/trade'
import { queryLogisticsStatus } from '../../api/logistics'
import LogisticsMap from '../../components/LogisticsMap.vue'
import { mergeMockLogisticsState } from '../../utils/mockLogistics'

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

    const exactOrderId = Number(route.query.orderId || 0)
    activeItem.value =
      list.value.find((item) => item.orderId === exactOrderId) ||
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
.content-grid {
  display: grid;
  grid-template-columns: 0.88fr 1.32fr;
  gap: 18px;
}

.page-head,
.list-panel,
.detail-panel {
  padding: 24px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
}

.page-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.page-head h2 {
  margin: 0 0 8px;
}

.page-head span,
.tracking-card p,
.detail-head p,
.detail-meta span,
.result-tip {
  color: var(--app-muted);
}

.search-row,
.tracking-top,
.tracking-actions,
.detail-head,
.detail-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.search-input {
  width: 320px;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tracking-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--app-border);
  background: #fbfdfc;
}

.tracking-card.active {
  border-color: rgba(29, 92, 87, 0.42);
  box-shadow: 0 10px 24px rgba(29, 92, 87, 0.08);
}

.result-tip {
  margin-bottom: 12px;
}

.detail-head {
  margin-bottom: 18px;
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

  .search-input {
    width: 100%;
  }
}
</style>


