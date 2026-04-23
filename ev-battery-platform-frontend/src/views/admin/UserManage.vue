<template>
  <div>
    <el-table :data="users">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="toggle(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deleteUser, getUsers, updateUser } from '../../api/admin'
const users = ref([])
const load = async () => { users.value = (await getUsers()).data || [] }
onMounted(load)
const toggle = async (row) => {
  await updateUser({ id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('更新成功')
  load()
}
const remove = async (id) => {
  await deleteUser(id)
  ElMessage.success('删除成功')
  load()
}
</script>
