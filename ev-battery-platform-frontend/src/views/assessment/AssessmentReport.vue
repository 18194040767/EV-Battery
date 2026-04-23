<template>
  <div>
    <el-input v-model="assessmentId" placeholder="输入评估ID" style="width: 260px; margin-right: 8px;" />
    <el-button type="primary" @click="load">查询报告</el-button>
    <el-card style="margin-top: 12px;">
      <pre>{{ JSON.stringify(report, null, 2) }}</pre>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getAssessmentReport } from '../../api/assessment'
import { normalizeId } from '../../utils/id'
const assessmentId = ref('')
const report = ref({})
const load = async () => {
  if (!assessmentId.value) return
  report.value = (await getAssessmentReport(normalizeId(assessmentId.value))).data || {}
}
</script>
