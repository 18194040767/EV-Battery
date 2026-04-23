<template>
  <div>
    <el-form inline>
      <el-form-item label="电池ID"><el-input v-model="relatedId" /></el-form-item>
      <el-form-item><el-button type="primary" @click="gen">生成报告</el-button></el-form-item>
      <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
    </el-form>
    <el-table :data="list">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="versionNo" label="版本" />
      <el-table-column prop="summary" label="摘要" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" @click="detail(row.id)">详情</el-button>
          <el-button size="small" @click="pick(row.id)">选中对比</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button :disabled="picked.length !== 2" @click="compare">对比选中报告</el-button>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { compareReport, generateReport, listReport, reportDetail } from '../../api/report'
const relatedId = ref('')
const list = ref([])
const picked = ref([])
const load = async () => { list.value = (await listReport()).data || [] }
onMounted(load)
const gen = async () => { await generateReport({ relatedType: 'BATTERY', relatedId: Number(relatedId.value) }); await load() }
const detail = async (id) => { ElMessageBox.alert((await reportDetail(id)).data?.content || '', '报告内容') }
const pick = (id) => {
  if (!picked.value.includes(id)) picked.value.push(id)
  if (picked.value.length > 2) picked.value.shift()
  ElMessage.info(`已选中: ${picked.value.join(', ')}`)
}
const compare = async () => {
  const res = await compareReport(picked.value[0], picked.value[1])
  ElMessageBox.alert(JSON.stringify(res.data?.difference || []), '对比结果')
}
</script>
