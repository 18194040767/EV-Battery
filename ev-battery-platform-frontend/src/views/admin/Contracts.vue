<template>
  <section class="panel-card admin-panel">
    <div class="head">
      <div>
        <p>合同管理</p>
        <h2>合同管理</h2>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="contracts">
      <el-table-column prop="contractNo" label="合同编号" min-width="180" />
      <el-table-column prop="orderNo" label="订单编号" min-width="180" />
      <el-table-column prop="buyerName" label="买方" min-width="120" />
      <el-table-column prop="sellerName" label="卖方" min-width="120" />
      <el-table-column prop="verifyCount" label="查验次数" width="110" />
      <el-table-column prop="hashDigest" label="哈希摘要" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" plain size="small" @click="verify(row.id)">重新校验</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getAdminContracts, verifyAdminContract } from '../../api/admin'

const contracts = ref([])

const load = async () => {
  const res = await getAdminContracts()
  contracts.value = res?.data || []
}

const verify = async (id) => {
  const res = await verifyAdminContract(id)
  const data = res?.data || {}
  ElMessageBox.alert(
    `合同编号：${data.contractNo || '-'}\n存证哈希：${data.storedHash || '-'}\n当前哈希：${data.currentHash || '-'}\n校验结果：${data.valid ? '一致' : '不一致'}`,
    '重新校验结果',
    { confirmButtonText: '关闭' }
  )
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

