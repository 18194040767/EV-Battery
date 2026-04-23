<template>
  <div class="page-shell">
    <section class="panel-card map-panel">
      <div class="section-title">
        <h3>分布统计</h3>
        <span>区域分布</span>
      </div>
      <div class="province-grid">
        <article v-for="item in provinces" :key="item.name" :style="{ opacity: normalize(item.value) }">
          <strong>{{ item.name }}</strong>
          <span>{{ item.value }}</span>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAdminDashboard } from '../../api/admin'

const provinces = ref([])

const maxValue = computed(() => Math.max(...provinces.value.map((item) => Number(item.value) || 0), 1))
const normalize = (value) => 0.35 + (Number(value || 0) / maxValue.value) * 0.65

onMounted(async () => {
  const res = await getAdminDashboard()
  provinces.value = res?.data?.provinceDistribution || []
})
</script>

<style scoped>
.map-panel {
  padding: 24px;
}

.section-title h3 {
  margin: 0 0 8px;
}

.section-title span {
  color: #64748b;
}

.province-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.province-grid article {
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.18), rgba(15, 118, 110, 0.22));
  border: 1px solid rgba(14, 165, 233, 0.18);
}

.province-grid strong {
  display: block;
  margin-bottom: 8px;
}

@media (max-width: 1100px) {
  .province-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .province-grid {
    grid-template-columns: 1fr;
  }
}
</style>


