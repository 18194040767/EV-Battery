<template>
  <div v-if="detail.id" class="page-shell">
    <section class="detail-grid">
      <div class="panel-card gallery-card">
        <el-carousel height="380px" indicator-position="outside">
          <el-carousel-item v-for="(img, index) in detail.images || [placeholder]" :key="index">
            <img :src="img || placeholder" alt="" class="gallery-image" />
          </el-carousel-item>
        </el-carousel>
      </div>

      <div class="panel-card info-card">
        <p class="kicker">商品详情</p>
        <h2>{{ detail.title }}</h2>
        <div class="price-box">
          <strong>¥{{ detail.price }}</strong>
          <span v-if="detail.original_price">¥{{ detail.original_price }}</span>
        </div>
        <p class="summary">销量 {{ detail.sale_count || 0 }} · 库存 {{ detail.stock || 0 }} · 浏览 {{ detail.view_count || 0 }}</p>
        <p class="summary">卖家 {{ detail.sellerNickname || '平台卖家' }} · 信用 {{ detail.creditScore || 100 }}</p>
        <p class="summary">发货地 {{ detail.shipping_from || '全国可发' }} · 健康等级 {{ detail.latestAssessment?.healthLevel || detail.health_level || '待评估' }}</p>
        <div class="action-row">
          <el-button :type="detail.favorite ? 'danger' : 'default'" @click="toggleFavorite">{{ detail.favorite ? '取消收藏' : '加入收藏' }}</el-button>
          <el-button v-if="!isOwnProduct" :disabled="Number(detail.stock || 0) < 1" @click="addCart">加入购物车</el-button>
          <el-button v-if="!isOwnProduct" type="primary" :disabled="Number(detail.stock || 0) < 1" @click="buyNow">立即下单</el-button>
          <el-button v-else type="primary" plain @click="$router.push('/trade/user')">我的商品</el-button>
        </div>
      </div>
    </section>

    <section class="detail-grid">
      <div class="panel-card spec-card">
        <h3>健康与参数</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="健康评分">{{ detail.latestAssessment?.healthScore || '-' }}</el-descriptions-item>
          <el-descriptions-item label="健康等级">{{ detail.latestAssessment?.healthLevel || detail.health_level || '-' }}</el-descriptions-item>
          <el-descriptions-item label="容量保持率">{{ detail.capacityRetentionRate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="内阻比">{{ detail.internalResistanceRatio || '-' }}</el-descriptions-item>
          <el-descriptions-item label="循环次数">{{ detail.cycleCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="建议场景">{{ detail.latestAssessment?.suggestedScene || '-' }}</el-descriptions-item>
        </el-descriptions>
        <p class="description">{{ detail.description || '暂无说明' }}</p>
      </div>

      <div class="panel-card review-card">
        <h3>买家评价</h3>
        <el-empty v-if="!reviews.length" description="暂无评价" />
        <article v-for="item in reviews" :key="item.id" class="review-item">
          <strong>{{ item.reviewerName || '平台买家' }}</strong>
          <p>{{ item.content || '未填写评价' }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import {
  addCartItem,
  addFavoriteProduct,
  createTradeOrder,
  getAddresses,
  getProductReviews,
  getTradeProductDetail,
  removeFavoriteProduct,
  saveAddress
} from '../../api/trade'

const placeholder = 'https://dummyimage.com/600x420/dce7e5/1e3a38&text=EV'
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const detail = ref({})
const reviews = ref([])

const isOwnProduct = computed(() => Number(detail.value.seller_id || detail.value.sellerId) === Number(userStore.userId))

const load = async () => {
  const [detailRes, reviewsRes] = await Promise.all([
    getTradeProductDetail(route.params.id),
    getProductReviews(route.params.id)
  ])
  detail.value = detailRes?.data || {}
  reviews.value = reviewsRes?.data || []
}

const ensureDefaultAddress = async () => {
  let addresses = (await getAddresses()).data || []
  if (addresses.length) return addresses[0]
  await saveAddress({
    receiverName: '平台演示收货人',
    receiverPhone: '13800000000',
    province: '上海市',
    city: '上海市',
    district: '浦东新区',
    detailAddress: '世纪大道 1888 号',
    isDefault: true
  })
  addresses = (await getAddresses()).data || []
  return addresses[0]
}

const toggleFavorite = async () => {
  if (detail.value.favorite) {
    await removeFavoriteProduct(detail.value.id)
  } else {
    await addFavoriteProduct(detail.value.id)
  }
  detail.value.favorite = !detail.value.favorite
}

const addCart = async () => {
  await addCartItem({ productId: detail.value.id, quantity: 1 })
  ElMessage.success('已加入购物车')
}

const buyNow = async () => {
  const address = await ensureDefaultAddress()
  await createTradeOrder({
    addressId: address?.id,
    items: [{ productId: detail.value.id, quantity: 1 }]
  })
  ElMessage.success('订单已创建，已进入待付款')
  router.push({ path: '/trade/order-list', query: { tab: 'PENDING_PAYMENT' } })
}

onMounted(load)
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
}

.gallery-card,
.info-card,
.spec-card,
.review-card {
  padding: 24px;
}

.gallery-image {
  width: 100%;
  height: 380px;
  border-radius: 20px;
  object-fit: cover;
}

.kicker {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.info-card h2,
.spec-card h3,
.review-card h3 {
  margin-top: 0;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 16px 0;
}

.price-box strong {
  color: #b45309;
  font-size: 34px;
}

.price-box span {
  color: #94a3b8;
  text-decoration: line-through;
}

.summary,
.description,
.review-item p {
  color: var(--app-muted);
}

.action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 20px;
}

.description {
  margin-top: 18px;
  line-height: 1.8;
}

.review-item {
  padding: 14px 0;
  border-bottom: 1px solid var(--app-border);
}

.review-item:last-child {
  border-bottom: none;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>


