<template>
  <div>
    <el-form inline>
      <el-form-item label="电池ID"><el-input v-model="batteryId" /></el-form-item>
      <el-form-item><el-button type="primary" @click="go">触发评估</el-button></el-form-item>
      <el-form-item><el-button @click="load">历史记录</el-button></el-form-item>
    </el-form>
    <el-table :data="list">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="batteryId" label="电池ID" />
      <el-table-column prop="score" label="分数" />
      <el-table-column prop="grade" label="等级" />
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getAssessmentHistory, triggerAssessment } from '../../api/assessment'
import { normalizeId } from '../../utils/id'
const batteryId = ref('')
const list = ref([])
const load = async () => { list.value = (await getAssessmentHistory(batteryId.value ? { batteryId: normalizeId(batteryId.value) } : {})).data || [] }
const go = async () => {
  const res = await triggerAssessment(normalizeId(batteryId.value))
  ElMessageBox.alert(JSON.stringify(res.data), '评估结果')
  load()
}
</script>
