<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>合同中心</p>
        <h2>我的合同</h2>
      </div>
      <div class="head-actions">
        <el-button @click="loadCompletedOrders">刷新订单</el-button>
        <el-button type="primary" @click="$router.push('/contract/verify')">合同查验</el-button>
      </div>
    </section>

    <section class="panel-card generator-panel">
      <div class="generator-copy">
        <p>电子合同生成</p>
        <h3>选择已完成订单生成 PDF</h3>
        <span>自动关联合同信息，生成后同步到列表。</span>
      </div>
      <div class="generator-form">
        <el-select
          v-model="selectedOrderId"
          filterable
          placeholder="请选择已完成订单"
          class="order-select"
          :loading="orderLoading"
          no-data-text="暂无已完成订单"
        >
          <el-option
            v-for="item in completedOrders"
            :key="item.id"
            :label="`${item.orderNo} / ${item.title}`"
            :value="item.id"
          >
            <div class="order-option">
              <strong>{{ item.orderNo }}</strong>
              <span>{{ item.title }} · ¥{{ item.amount }}</span>
            </div>
          </el-option>
        </el-select>
        <el-button type="primary" :loading="generating" :disabled="!selectedOrderId" @click="generateAndDownload">
          生成并下载 PDF
        </el-button>
      </div>
    </section>

    <section class="panel-card table-panel">
      <el-skeleton v-if="loading" :rows="8" animated />
      <el-empty v-else-if="!records.length" description="暂无合同" />
      <template v-else>
        <el-table :data="records">
          <el-table-column prop="contractNo" label="合同编号" min-width="180" />
          <el-table-column prop="orderNo" label="订单编号" min-width="180" />
          <el-table-column prop="productTitle" label="商品" min-width="200" />
          <el-table-column prop="buyerName" label="买方" min-width="120" />
          <el-table-column prop="sellerName" label="卖方" min-width="120" />
          <el-table-column prop="verifyCount" label="查验次数" width="110" />
          <el-table-column prop="createdAt" label="生成时间" min-width="180" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button text @click="openPreview(row.id)">预览</el-button>
              <el-button text @click="download(row.id, row.contractNo)">下载</el-button>
              <el-button type="primary" plain size="small" @click="verify(row.id)">查验</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination layout="total, prev, pager, next" :current-page="query.page" :page-size="query.size" :total="total" @current-change="changePage" />
        </div>
      </template>
    </section>

    <el-dialog v-model="previewVisible" title="合同预览" width="80%" top="4vh">
      <iframe v-if="previewUrl" :src="previewUrl" class="preview-frame" />
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadContract, generateContract, listContracts, previewContract, verifyContractById } from '../../api/contract'
import { getTradeOrders } from '../../api/trade'
import { createPdfObjectUrl, downloadPdfResponse, getDownloadErrorMessage } from '../../utils/file'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const previewVisible = ref(false)
const previewUrl = ref('')
const orderLoading = ref(false)
const generating = ref(false)
const selectedOrderId = ref(null)
const completedOrders = ref([])
const query = reactive({ page: 1, size: 8 })

const normalizeContract = (item = {}) => ({
  ...item,
  id: String(item.id || ''),
  orderId: String(item.orderId || item.order_id || ''),
  contractNo: item.contractNo || item.contract_no || '',
  orderNo: item.orderNo || item.order_no || '',
  productTitle: item.productTitle || item.product_title || '',
  buyerName: item.buyerName || item.buyer_name || '',
  sellerName: item.sellerName || item.seller_name || '',
  verifyCount: item.verifyCount ?? item.verify_count ?? 0,
  createdAt: item.createdAt || item.created_at || ''
})

const normalizeOrder = (item = {}) => ({
  ...item,
  id: String(item.id || ''),
  orderNo: item.orderNo || item.order_no || '',
  orderStatus: item.orderStatus || item.order_status || '',
  amount: item.amount || 0,
  title: item.title || item.productSnapshot?.title || item.product_snapshot?.title || '电池商品'
})

const revokePreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}

const loadCompletedOrders = async () => {
  orderLoading.value = true
  try {
    const res = await getTradeOrders({ tab: 'ALL' })
    completedOrders.value = (res?.data || [])
      .map(normalizeOrder)
      .filter((item) => ['COMPLETED', 'COMPLETED_PENDING_REVIEW'].includes(item.orderStatus))
    if (!selectedOrderId.value && completedOrders.value.length) {
      selectedOrderId.value = completedOrders.value[0].id
    }
  } finally {
    orderLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await listContracts(query)
    records.value = (res?.data?.records || []).map(normalizeContract)
    total.value = res?.data?.total || 0
  } finally {
    loading.value = false
  }
}

const showPdfError = async (error, fallback = 'PDF 下载失败，请稍后重试') => {
  ElMessage.error(await getDownloadErrorMessage(error, fallback))
}

const download = async (id, contractNo) => {
  try {
    const res = await downloadContract(id)
    await downloadPdfResponse(res, `${contractNo || `contract-${id}`}.pdf`)
  } catch (error) {
    await showPdfError(error)
  }
}

const generateAndDownload = async () => {
  if (!selectedOrderId.value) {
    ElMessage.warning('请先选择一个已完成订单')
    return
  }
  generating.value = true
  try {
    const selected = completedOrders.value.find((item) => String(item.id) === String(selectedOrderId.value))
    const generated = await generateContract(selectedOrderId.value)
    const contract = normalizeContract({
      ...(generated?.data || {}),
      orderNo: generated?.data?.orderNo || selected?.orderNo,
      productTitle: generated?.data?.productTitle || selected?.title
    })
    if (!contract.id) {
      ElMessage.error('合同记录生成失败，请稍后重试')
      return
    }
    const res = await downloadContract(contract.id)
    await downloadPdfResponse(res, `${contract.contractNo || `电子合同-${selected?.orderNo || selectedOrderId.value}`}.pdf`)
    query.page = 1
    await load()
    if (!records.value.some((item) => item.id === contract.id)) {
      records.value = [contract, ...records.value].slice(0, query.size)
      total.value += 1
    }
    ElMessage.success('电子合同 PDF 已生成并开始下载')
  } catch (error) {
    await showPdfError(error, '电子合同 PDF 生成或下载失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

const openPreview = async (id) => {
  try {
    revokePreview()
    const res = await previewContract(id)
    previewUrl.value = await createPdfObjectUrl(res)
    previewVisible.value = true
  } catch (error) {
    await showPdfError(error, '合同预览失败，请稍后重试')
  }
}

const verify = async (id) => {
  const res = await verifyContractById(id)
  const data = res?.data || {}
  await ElMessageBox.alert(`合同号：${data.contractNo || '-'}\n结果：${data.valid ? '一致' : '异常'}`, '查验结果', { confirmButtonText: '确定' })
}

const changePage = (page) => {
  query.page = page
  load()
}

onBeforeUnmount(revokePreview)

onMounted(async () => {
  await loadCompletedOrders()
  const orderId = String(route.query.orderId || '')
  if (orderId) {
    await generateContract(orderId).catch(() => null)
    await load()
    const targetId = String(route.query.contractId || '')
    const match = records.value.find((item) => item.id === targetId) || records.value.find((item) => String(item.orderId) === orderId)
    if (match) {
      await openPreview(match.id)
    }
    return
  }
  await load()
})
</script>

<style scoped>
.page-head,
.table-panel {
  padding: 24px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.head-actions,
.generator-form {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.page-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.page-head h2 {
  margin: 0;
}

.generator-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.9fr);
  gap: 22px;
  align-items: center;
  padding: 24px;
  border: 1px solid rgba(47, 124, 255, 0.18);
  background:
    linear-gradient(135deg, rgba(47, 124, 255, 0.1), rgba(20, 184, 166, 0.08)),
    #ffffff;
}

.generator-copy p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 800;
}

.generator-copy h3 {
  margin: 0 0 8px;
  color: #101a33;
  font-size: 20px;
}

.generator-copy span {
  color: var(--app-muted);
  line-height: 1.7;
}

.generator-form {
  justify-content: flex-end;
}

.order-select {
  width: min(100%, 360px);
}

.order-option {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-option span {
  color: var(--app-muted);
  font-size: 12px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.preview-frame {
  width: 100%;
  min-height: 72vh;
  border: none;
}

@media (max-width: 768px) {
  .generator-panel {
    grid-template-columns: 1fr;
  }

  .generator-form,
  .head-actions,
  .order-select,
  .generator-form :deep(.el-button) {
    width: 100%;
  }
}
</style>
