<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>运营总览</p>
        <h2>平台统计</h2>
        <span>统计看板</span>
      </div>
      <el-button @click="load">鍒锋柊缁熻</el-button>
    </section>

    <section class="panel-card content-panel">
      <pre>{{ JSON.stringify(data, null, 2) }}</pre>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getHealthDistribution, getSourceDistribution, getTradeTrend } from '../../api/statistics'

const data = ref({})

const load = async () => {
  const [trend, health, source] = await Promise.all([
    getTradeTrend({ days: 7 }),
    getHealthDistribution(),
    getSourceDistribution()
  ])
  data.value = {
    trend: trend?.data || {},
    health: health?.data || [],
    source: source?.data || []
  }
}

onMounted(load)
</script>

<style scoped>
.page-head,
.content-panel {
  padding: 24px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
}

.page-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.page-head h2 {
  margin: 0 0 8px;
}

.page-head span {
  color: var(--app-muted);
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>

