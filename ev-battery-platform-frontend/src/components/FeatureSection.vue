<template>
  <div class="feature-grid">
    <article v-for="item in features" :key="item.title" class="feature-item panel-card">
      <span class="feature-mark">{{ item.mark }}</span>
      <h3>{{ item.title }}</h3>
      <p>{{ item.description }}</p>
    </article>
  </div>
</template>

<script setup>
defineProps({
  features: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.feature-item {
  padding: 24px;
}

.feature-mark {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  color: var(--app-primary);
  background: #eef5ff;
  font-size: 12px;
  font-weight: 700;
}

.feature-item h3 {
  margin: 16px 0 10px;
}

.feature-item p {
  margin: 0;
  color: var(--app-muted);
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
