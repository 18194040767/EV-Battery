<template>
  <el-carousel height="clamp(220px, 31.25vw, 420px)" class="banner-card" indicator-position="outside">
    <el-carousel-item v-for="item in slides" :key="item.src">
      <img class="banner-image" :src="item.src" :alt="item.title" />
    </el-carousel-item>
  </el-carousel>
</template>

<script setup>
defineProps({
  slides: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.banner-card {
  overflow: hidden;
  border-radius: 28px;
  box-shadow: var(--app-shadow);
}

.banner-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
