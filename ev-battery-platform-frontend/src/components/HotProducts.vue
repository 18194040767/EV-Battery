<template>
  <div class="panel-card hot-panel">
    <div class="section-head">
      <div>
        <p class="section-kicker">热门推荐</p>
        <h3>优选货源</h3>
      </div>
      <el-button text @click="$router.push('/trade/product-list')">进入商品市场</el-button>
    </div>
    <div class="product-strip">
      <article v-for="item in products" :key="item.id" class="product-card" @click="$router.push('/trade/product/' + item.id)">
        <img :src="item.cover_image || item.coverImage || placeholder" alt="" />
        <div>
          <strong>{{ item.title }}</strong>
          <p>{{ item.health_level || item.healthLevel || '待评估' }} · {{ item.shipping_from || item.shippingFrom || '全国可发' }}</p>
          <span>¥{{ item.price }}</span>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
const placeholder = 'https://dummyimage.com/600x420/eaf2ff/1f75ff&text=EV'

defineProps({
  products: {
    type: Array,
    default: () => []
  }
})
</script>

<style scoped>
.hot-panel {
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.section-kicker {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.section-head h3 {
  margin: 0;
}

.product-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.product-card {
  cursor: pointer;
  border-radius: 18px;
  overflow: hidden;
  background: #ffffff;
  border: 1px solid rgba(210, 222, 242, 0.9);
}

.product-card img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.product-card div {
  padding: 16px;
}

.product-card strong {
  display: block;
  margin-bottom: 8px;
}

.product-card p {
  margin: 0 0 10px;
  color: var(--app-muted);
  font-size: 13px;
}

.product-card span {
  color: #d97706;
  font-size: 20px;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .product-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .product-strip {
    grid-template-columns: 1fr;
  }
}
</style>

