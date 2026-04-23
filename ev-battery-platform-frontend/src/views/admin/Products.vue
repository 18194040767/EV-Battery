<template>
  <section class="panel-card admin-panel">
    <div class="head">
      <div>
        <p>商品审核</p>
        <h2>商品审核</h2>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="products">
      <el-table-column prop="title" label="商品名称" min-width="200" />
      <el-table-column prop="sellerName" label="卖家" min-width="120" />
      <el-table-column prop="batteryType" label="电池类型" min-width="120" />
      <el-table-column prop="healthLevel" label="健康等级" min-width="120" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="publishStatus" label="上架状态" min-width="140" />
      <el-table-column prop="auditStatus" label="审核状态" min-width="120" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text @click="audit(row.id, 'APPROVED', 'ON_SHELF')">上架</el-button>
          <el-button text @click="audit(row.id, 'REJECTED', 'OFF_SHELF')">下架</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditAdminProduct, getAdminProducts } from '../../api/admin'

const products = ref([])

const load = async () => {
  const res = await getAdminProducts()
  products.value = res?.data || []
}

const audit = async (id, auditStatus, publishStatus) => {
  await auditAdminProduct(id, { auditStatus, publishStatus })
  ElMessage.success('商品状态已更新')
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


