<template>
  <el-carousel height="360px" class="banner-card" indicator-position="outside">
    <el-carousel-item v-for="item in slides" :key="item.title">
      <section class="banner-slide" :style="{ background: item.background }">
        <div class="slide-copy">
          <p>{{ item.kicker }}</p>
          <h2>{{ item.title }}</h2>
          <span>{{ item.description }}</span>
        </div>
      </section>
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

.banner-slide {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 42px;
  color: #fff;
}

.slide-copy {
  max-width: 520px;
}

.slide-copy p {
  margin: 0 0 14px;
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  opacity: 0.82;
}

.slide-copy h2 {
  margin: 0 0 14px;
  font-size: 38px;
  line-height: 1.2;
}

.slide-copy span {
  display: block;
  font-size: 16px;
  line-height: 1.8;
  opacity: 0.92;
}
</style>
