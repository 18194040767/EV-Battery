<template>
  <section class="panel-card admin-panel">
    <div class="head">
      <div>
        <p>档案审核</p>
        <h2>档案审核</h2>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="records">
      <el-table-column prop="batteryCode" label="电池编码" min-width="180" />
      <el-table-column prop="ownerName" label="提交人" min-width="120" />
      <el-table-column prop="sourceType" label="来源类型" min-width="120" />
      <el-table-column prop="capacityRetentionRate" label="容量保持率" min-width="120" />
      <el-table-column prop="cycleCount" label="循环次数" min-width="100" />
      <el-table-column prop="status" label="状态" min-width="120" />
      <el-table-column label="审核结果" width="110">
        <template #default="{ row }">
          <el-tag :type="Number(row.auditStatus) === 1 ? 'success' : Number(row.auditStatus) === 2 ? 'danger' : 'info'">
            {{ Number(row.auditStatus) === 1 ? '已通过' : Number(row.auditStatus) === 2 ? '已驳回' : '待审核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text @click="audit(row.id, 1)">通过</el-button>
          <el-button text @click="audit(row.id, 2)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { auditAdminBattery, getAdminBatteries } from '../../api/admin'

const records = ref([])

const load = async () => {
  const res = await getAdminBatteries()
  records.value = res?.data || []
}

const audit = async (id, auditStatus) => {
  await auditAdminBattery(id, { auditStatus, remark: auditStatus === 1 ? '审核通过' : '审核驳回，请补充资料' })
  ElMessage.success('审核结果已更新')
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

