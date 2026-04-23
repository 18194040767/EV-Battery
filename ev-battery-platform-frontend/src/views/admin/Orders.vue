<template>
  <section class="panel-card admin-panel">
    <div class="head">
      <div>
        <p>订单管理</p>
        <h2>订单管理</h2>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="orders">
      <el-table-column prop="orderNo" label="订单编号" min-width="180" />
      <el-table-column prop="title" label="商品名称" min-width="180" />
      <el-table-column prop="buyerName" label="买方" min-width="120" />
      <el-table-column prop="sellerName" label="卖方" min-width="120" />
      <el-table-column prop="amount" label="金额" width="120" />
      <el-table-column prop="orderStatus" label="订单状态" min-width="140" />
      <el-table-column prop="logisticsNo" label="运单号" min-width="160" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button text @click="ship(row)">标记发货</el-button>
          <el-button text @click="cancel(row.id)">强制取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { cancelAdminOrder, getAdminOrders, shipAdminOrder } from '../../api/admin'

const orders = ref([])

const load = async () => {
  const res = await getAdminOrders()
  orders.value = res?.data || []
}

const cancel = async (id) => {
  await cancelAdminOrder(id)
  ElMessage.success('订单已取消')
  load()
}

const ship = async (row) => {
  await shipAdminOrder(row.id, {
    company: row.logisticsCompany || '顺丰速运',
    trackingNo: row.logisticsNo || `SF${Date.now()}`,
    contactName: '平台应急联系人',
    contactPhone: '400-800-1234'
  })
  ElMessage.success('订单已标记发货')
  load()
}

onMounted(load)
</script>

<style scoped>
.admin-panel {
  padding: 24px;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.head p {
  margin: 0 0 8px;
  color: var(--app-accent);
  font-size: 12px;
  font-weight: 700;
}

.head h2 {
  margin: 0;
}
</style>


