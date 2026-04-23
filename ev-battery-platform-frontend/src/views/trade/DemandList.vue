<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>需求中心</p>
        <h2>采购需求</h2>
      </div>
    </section>

    <section class="panel-card form-panel">
      <el-form :model="form" inline>
        <el-form-item label="需求标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="预算下限"><el-input v-model="form.budgetMin" /></el-form-item>
        <el-form-item label="预算上限"><el-input v-model="form.budgetMax" /></el-form-item>
        <el-form-item><el-button type="primary" @click="publish">发布需求</el-button></el-form-item>
      </el-form>
    </section>

    <section class="panel-card table-panel">
      <el-table :data="list">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="需求标题" />
        <el-table-column prop="budgetMin" label="预算下限" />
        <el-table-column prop="budgetMax" label="预算上限" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getDemandList, publishDemand } from '../../api/trade'

const list = ref([])
const form = reactive({ title: '', budgetMin: '', budgetMax: '', requirement: '' })

const load = async () => {
  const res = await getDemandList()
  list.value = res?.data || []
}

const publish = async () => {
  await publishDemand(form)
  ElMessage.success('采购需求已发布')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-head,
.form-panel,
.table-panel {
  padding: 24px;
}

.page-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.page-head h2 {
  margin: 0;
}
</style>

