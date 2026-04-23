<template>
  <section class="panel-card admin-panel">
    <div class="head">
      <div>
        <p>账号管理</p>
        <h2>账号管理</h2>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="users">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column prop="realName" label="姓名" min-width="140" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="phone" label="手机号" min-width="130" />
      <el-table-column prop="roles" label="角色" min-width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="Number(row.status) === 1 ? 'success' : 'danger'">{{ Number(row.status) === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button text @click="toggleStatus(row)">{{ Number(row.status) === 1 ? '禁用' : '启用' }}</el-button>
          <el-button text @click="switchRole(row)">{{ row.roles?.includes('ROLE_ADMIN') ? '设为用户' : '设为管理员' }}</el-button>
          <el-button type="primary" plain size="small" @click="resetPassword(row.id)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsers, resetAdminUserPassword, updateAdminUser } from '../../api/admin'

const users = ref([])

const load = async () => {
  const res = await getAdminUsers()
  users.value = res?.data || []
}

const toggleStatus = async (row) => {
  await updateAdminUser({ id: row.id, status: Number(row.status) === 1 ? 0 : 1 })
  ElMessage.success('用户状态已更新')
  load()
}

const switchRole = async (row) => {
  await updateAdminUser({ id: row.id, roleCode: row.roles?.includes('ROLE_ADMIN') ? 'ROLE_USER' : 'ROLE_ADMIN' })
  ElMessage.success('角色权限已更新')
  load()
}

const resetPassword = async (id) => {
  await resetAdminUserPassword(id)
  ElMessage.success('密码已重置为 123456')
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
  gap: 18px;
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


