<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>收藏夹</p>
        <h2>收藏商品</h2>
      </div>
    </section>

    <el-empty v-if="!items.length" description="暂无收藏商品" />

    <section v-else class="grid">
      <article v-for="item in items" :key="item.id" class="panel-card favorite-card">
        <img :src="item.cover_image || placeholder" alt="" class="cover" @click="$router.push('/trade/product/' + item.id)" />
        <strong>{{ item.title }}</strong>
        <p>¥{{ item.price }}</p>
        <div class="actions">
          <el-button @click="$router.push('/trade/product/' + item.id)">查看详情</el-button>
          <el-button type="danger" plain @click="remove(item.id)">取消收藏</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFavoriteProducts, removeFavoriteProduct } from '../../api/trade'

const placeholder = 'https://dummyimage.com/600x420/dce7e5/1e3a38&text=Favorite'
const items = ref([])

const load = async () => {
  const res = await getFavoriteProducts()
  items.value = res?.data || []
}

const remove = async (id) => {
  await removeFavoriteProduct(id)
  ElMessage.success('已取消收藏')
  load()
}

onMounted(load)
</script>

<style scoped>
.page-head,
.favorite-card {
  padding: 24px;
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

.page-head span,
.favorite-card p {
  color: var(--app-muted);
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.favorite-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cover {
  width: 100%;
  height: 220px;
  border-radius: 18px;
  object-fit: cover;
  cursor: pointer;
}

.actions {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

