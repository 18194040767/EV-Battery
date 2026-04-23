<template>
  <div>
    <el-card>
      <div class="header-row">
        <div>
          <h3>订单管理</h3>
          <p>异常订单处理</p>
        </div>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
      <el-table :data="orders" stripe style="margin-top: 16px;">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="buyerId" label="买方ID" width="100" />
        <el-table-column prop="sellerId" label="卖方ID" width="100" />
        <el-table-column prop="amount" label="总金额" width="120" />
        <el-table-column prop="orderStatus" label="订单状态" width="140" />
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="cancel(row.id)">取消订单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelOrder, getOrders } from '../../api/admin'

const orders = ref([])

const load = async () => {
  try {
    const res = await getOrders()
    orders.value = res?.data || []
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '加载订单失败')
  }
}

const cancel = async (id) => {
  await ElMessageBox.confirm('确认取消该订单吗？', '风险操作', { type: 'warning' })
  try {
    await cancelOrder(id)
    ElMessage.success('订单已取消')
    load()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || '取消订单失败')
  }
}

onMounted(load)
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; }
</style>

