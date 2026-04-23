<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>合同中心</p>
        <h2>我的合同</h2>
      </div>
      <el-button type="primary" @click="$router.push('/contract/verify')">合同查验</el-button>
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
import { ElMessageBox } from 'element-plus'
import { downloadContract, generateContract, listContracts, previewContract, verifyContractById } from '../../api/contract'

const route = useRoute()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const previewVisible = ref(false)
const previewUrl = ref('')
const query = reactive({ page: 1, size: 8 })

const normalizeContract = (item = {}) => ({
  ...item,
  id: Number(item.id || 0),
  orderId: Number(item.orderId || item.order_id || 0),
  contractNo: item.contractNo || item.contract_no || '',
  orderNo: item.orderNo || item.order_no || '',
  productTitle: item.productTitle || item.product_title || '',
  buyerName: item.buyerName || item.buyer_name || '',
  sellerName: item.sellerName || item.seller_name || '',
  verifyCount: item.verifyCount ?? item.verify_count ?? 0,
  createdAt: item.createdAt || item.created_at || ''
})

const revokePreview = () => {
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
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

const download = async (id, contractNo) => {
  const res = await downloadContract(id)
  const blob = new Blob([res.data], { type: 'application/pdf' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${contractNo || `contract-${id}`}.pdf`
  link.click()
  URL.revokeObjectURL(url)
}

const openPreview = async (id) => {
  revokePreview()
  const res = await previewContract(id)
  previewUrl.value = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
  previewVisible.value = true
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
  const orderId = Number(route.query.orderId || 0)
  if (orderId > 0) {
    await generateContract(orderId).catch(() => null)
    await load()
    const targetId = Number(route.query.contractId || 0)
    const match = records.value.find((item) => item.id === targetId) || records.value.find((item) => Number(item.orderId) === orderId)
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

.page-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.page-head h2 {
  margin: 0;
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
</style>
