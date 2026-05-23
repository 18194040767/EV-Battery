<template>
  <div v-if="detail.id" class="product-detail-page">
    <section class="product-shell">
      <header class="crumb-row">
        <button type="button" @click="$router.push('/trade/product-list')">
          <el-icon><ArrowLeft /></el-icon>
          商品市场
        </button>
        <span>/</span>
        <strong>商品详情</strong>
      </header>

      <main class="product-main">
        <section class="gallery-zone">
          <div class="hero-image">
            <div v-if="activeSlide?.type === 'model'" class="model-poster">
              <strong>{{ productModel }}</strong>
            </div>
            <img v-else :src="activeSlide?.src" alt="" />
            <span class="image-count">{{ activeIndex + 1 }}/{{ carouselSlides.length }}</span>
          </div>
          <div class="thumb-row">
            <button v-for="(slide, index) in carouselSlides" :key="slide.key" type="button" :class="{ active: index === activeIndex }" @click="activeIndex = index">
              <span v-if="slide.type === 'model'">{{ productModel }}</span>
              <img v-else :src="slide.src" alt="" />
            </button>
          </div>
        </section>

        <section class="product-info">
          <h1>{{ detail.title || '三元锂 72V 动力模组检测件' }}</h1>
          <div class="tag-row">
            <span>{{ scenarioText }}</span>
            <span>回收梯次利用</span>
            <span>检测认证</span>
          </div>
          <div class="price-row">
            <strong>¥ {{ priceText }}</strong>
            <del v-if="detail.original_price || detail.originalPrice">¥{{ originalPriceText }}</del>
            <em>8.4 折</em>
          </div>
          <div class="sales-row">
            <span>销量 {{ detail.sale_count || detail.saleCount || 0 }}</span>
            <i></i>
            <span>库存 {{ detail.stock || 0 }}</span>
            <i></i>
            <span>浏览 {{ detail.view_count || detail.viewCount || 0 }}</span>
          </div>
          <dl class="info-list">
            <div>
              <dt><el-icon><User /></el-icon>卖家</dt>
              <dd>{{ sellerName }} · 信用 {{ creditScore }}</dd>
              <button type="button"><el-icon><ArrowRight /></el-icon></button>
            </div>
            <div>
              <dt><el-icon><Location /></el-icon>发货地</dt>
              <dd>{{ shippingFrom }} · 健康等级 {{ healthLevel }}</dd>
            </div>
            <div>
              <dt><el-icon><Box /></el-icon>商品类型</dt>
              <dd>{{ batteryType }}</dd>
            </div>
            <div>
              <dt><el-icon><Clock /></el-icon>更新时间</dt>
              <dd>{{ updateTime }}</dd>
            </div>
          </dl>
          <div class="action-row">
            <el-button class="plain-action" size="large" :loading="favoriteBusy" @click="toggleFavorite">☆ 收藏</el-button>
            <el-button class="outline-action" size="large" :loading="cartBusy" :disabled="isOwnProduct || Number(detail.stock || 0) < 1" @click="addCart">
              <el-icon><ShoppingCart /></el-icon>
              加入购物车
            </el-button>
            <el-button class="buy-action" size="large" type="primary" :loading="buyBusy" :disabled="isOwnProduct || Number(detail.stock || 0) < 1" @click="buyNow">立即下单</el-button>
          </div>
        </section>

        <aside class="side-stack">
          <section class="seller-card">
            <h2>卖家信息</h2>
            <div class="seller-head">
              <img :src="detail.sellerAvatar || '/seller-avatar.png'" alt="" />
              <div>
                <strong>{{ sellerName }}</strong>
                <span>企业认证</span>
                <p>信用 {{ creditScore }}</p>
              </div>
            </div>
            <div class="seller-stats">
              <div><strong>98%</strong><span>好评率</span></div>
              <div><strong>{{ creditScore }}</strong><span>信用分</span></div>
              <div><strong>12</strong><span>商品数</span></div>
              <div><strong>356</strong><span>成交数</span></div>
            </div>
            <div class="seller-actions">
              <button type="button">进入店铺</button>
              <button type="button">关注店铺</button>
            </div>
          </section>

          <section class="chat-card">
            <div class="chat-title">
              <h2>在线沟通</h2>
              <span><i></i>在线</span>
            </div>
            <div class="chat-body">
              <time>05-24 14:30</time>
              <p class="system-msg">您已与 {{ sellerName }} 建立联系</p>
              <div v-for="msg in chatMessages" :key="msg.id" class="chat-message" :class="{ mine: msg.mine }">
                <img v-if="!msg.mine" :src="detail.sellerAvatar || '/seller-avatar.png'" alt="" />
                <div>
                  <p>{{ msg.content }}</p>
                  <small>{{ msg.time }}</small>
                </div>
              </div>
            </div>
            <div class="chat-input">
              <button type="button">☺</button>
              <button type="button">▧</button>
              <input v-model="messageText" placeholder="输入消息..." @keyup.enter="sendMessage" />
              <button class="send-btn" type="button" @click="sendMessage">发送</button>
            </div>
          </section>
        </aside>

        <section class="health-card info-card">
          <h2>健康与参数</h2>
          <table>
            <tbody>
              <tr><th>健康评分</th><td>{{ healthScore }}</td><th>健康等级</th><td>{{ healthLevel }}</td></tr>
              <tr><th>容量保持率</th><td>{{ detail.capacityRetentionRate || detail.capacity_retention_rate || '-' }}</td><th>内阻比</th><td>{{ detail.internalResistanceRatio || detail.internal_resistance_ratio || '-' }}</td></tr>
              <tr><th>循环次数</th><td>{{ detail.cycleCount || detail.cycle_count || '-' }}</td><th>建议场景</th><td>{{ scenarioText }}</td></tr>
            </tbody>
          </table>
          <p>适合实验验证、拆解研究和教学演示。</p>
        </section>

        <section class="reviews-card info-card">
          <h2>买家评价 <span>({{ reviews.length }})</span></h2>
          <div v-if="!reviews.length" class="empty-record">
            <img src="/empty-state.png" alt="" />
            <strong>暂无评价</strong>
            <p>期待第一个评价</p>
          </div>
          <div v-else class="review-list">
            <article v-for="item in reviews" :key="item.id || item.createdAt || item.content" class="review-item">
              <strong>{{ item.nickname || item.userNickname || item.buyerName || '匿名买家' }}</strong>
              <p>{{ item.content || item.reviewContent || '买家未填写评价内容' }}</p>
              <time>{{ String(item.createdAt || item.created_at || item.createTime || '').replace('T', ' ') }}</time>
            </article>
          </div>
        </section>
      </main>

      <nav class="sticky-tabs">
        <button v-for="tab in tabs" :key="tab.key" type="button" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
      </nav>
    </section>

    <section class="tab-panel">
      <article v-if="activeTab === 'detail'" class="detail-tab tab-card">
        <div>
          <h2>{{ detail.title || '三元锂 72V 动力模组检测件' }}</h2>
          <div class="tab-price">
            <strong>¥ {{ priceText }}</strong>
            <del>¥{{ originalPriceText }}</del>
            <span>8.4 折</span>
          </div>
          <p class="mini-meta">库存 {{ detail.stock || 0 }}　|　浏览 {{ detail.view_count || detail.viewCount || 0 }}　|　销量 {{ detail.sale_count || detail.saleCount || 0 }}</p>
          <p>{{ detail.description || '本产品为三元锂 72V 动力电池模组，适用于低功率设备及回收梯次利用场景，经过初步检测，性能稳定，性价比高。' }}</p>
        </div>
        <dl>
          <div><dt><el-icon><Box /></el-icon>商品类型</dt><dd>{{ batteryType }}</dd></div>
          <div><dt><el-icon><Lightning /></el-icon>电压</dt><dd>{{ voltageText }}</dd></div>
          <div><dt><el-icon><Clock /></el-icon>更新日期</dt><dd>{{ updateTime }}</dd></div>
          <div><dt><el-icon><Location /></el-icon>发货地</dt><dd>{{ shippingFrom }} · 健康等级 {{ healthLevel }}</dd></div>
          <div><dt><el-icon><View /></el-icon>适用场景</dt><dd>{{ scenarioText }}</dd></div>
        </dl>
      </article>

      <article v-else-if="activeTab === 'spec'" class="spec-tab tab-card">
        <table>
          <tbody>
            <tr><th>电压</th><td>{{ voltageText }}</td><th>健康评分</th><td>{{ healthScore }}</td></tr>
            <tr><th>容量保持率</th><td>{{ detail.capacityRetentionRate || '-' }}</td><th>健康等级</th><td>{{ healthLevel }}</td></tr>
            <tr><th>循环次数</th><td>{{ detail.cycleCount || '-' }}</td><th>建议场景</th><td>{{ scenarioText }}</td></tr>
            <tr><th>内阻比</th><td>{{ detail.internalResistanceRatio || '-' }}</td><th>备注</th><td>-</td></tr>
          </tbody>
        </table>
      </article>

      <article v-else-if="activeTab === 'report'" class="report-tab tab-card">
        <div>
          <h2>检测报告</h2>
          <table>
            <tbody>
              <tr><th>检测机构</th><td>第三方检测中心</td><th>检测日期</th><td>2024-05-20</td></tr>
              <tr><th>检测标准</th><td>GB/T 31486-2015</td><th>报告编号</th><td>BG20240520-7281</td></tr>
              <tr><th>检测结论</th><td colspan="3">该电池模组健康等级{{ healthLevel }}，建议用于{{ scenarioText }}。</td></tr>
            </tbody>
          </table>
        </div>
        <div class="report-action">
          <button type="button" @click="openReport"><el-icon><Document /></el-icon>查看报告（PDF）</button>
        </div>
      </article>

      <article v-else class="notice-tab tab-card">
        <div v-for="item in notices" :key="item.title" class="notice-item">
          <span><el-icon><component :is="item.icon" /></el-icon></span>
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.text }}</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  ArrowRight,
  Box,
  CircleCheck,
  Clock,
  Document,
  Lightning,
  Location,
  ShoppingCart,
  User,
  View
} from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'
import {
  addCartItem,
  addFavoriteProduct,
  createTradeOrder,
  getAddresses,
  getProductReviews,
  getTradeMessageHistory,
  getTradeProductDetail,
  removeFavoriteProduct,
  saveAddress,
  sendTradeMessage
} from '../../api/trade'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const detail = ref({})
const reviews = ref([])
const activeIndex = ref(0)
const activeTab = ref('detail')
const favoriteBusy = ref(false)
const cartBusy = ref(false)
const buyBusy = ref(false)
const messageText = ref('')
const chatMessages = ref([
  { id: 'demo-1', content: '您好，请问这个模组的循环次数大概是多少？', time: '14:31', mine: true },
  { id: 'demo-2', content: '您好，大概在 300-450 次之间，具体以检测报告为准。', time: '14:32', mine: false },
  { id: 'demo-3', content: '可以发一份检测报告参考吗？', time: '14:33', mine: true },
  { id: 'demo-4', content: '可以的，稍后发您。', time: '14:34', mine: false }
])

const tabs = [
  { key: 'detail', label: '商品详情' },
  { key: 'spec', label: '规格参数' },
  { key: 'report', label: '检测报告' },
  { key: 'notice', label: '交易须知' }
]

const sellerName = computed(() => detail.value.sellerNickname || '供应商-启储能源')
const creditScore = computed(() => detail.value.creditScore || 108)
const productModel = computed(() => detail.value.batteryCode || 'NCM-B1')
const priceText = computed(() => Number(detail.value.price || 1680).toFixed(0))
const originalPriceText = computed(() => Number(detail.value.original_price || detail.value.originalPrice || 2000).toFixed(0))
const batteryType = computed(() => detail.value.battery_type || detail.value.batteryType || '动力电池模组')
const shippingFrom = computed(() => detail.value.shipping_from || detail.value.shippingFrom || '广州市')
const healthLevel = computed(() => detail.value.latestAssessment?.healthLevel || detail.value.health_level || detail.value.healthLevel || '较差')
const healthScore = computed(() => detail.value.latestAssessment?.healthScore || 49)
const scenarioText = computed(() => detail.value.latestAssessment?.suggestedScene || '低功率设备 / 回收梯次利用')
const voltageText = computed(() => `${detail.value.voltage || 72}V`)
const updateTime = computed(() => String(detail.value.updated_at || detail.value.updatedAt || detail.value.created_at || detail.value.createdAt || '2024-05-24 14:30').replace('T', ' '))
const isOwnProduct = computed(() => Number(detail.value.seller_id || detail.value.sellerId) === Number(userStore.userId))
const activeSlide = computed(() => carouselSlides.value[activeIndex.value] || carouselSlides.value[0])

const carouselSlides = computed(() => [
  { key: 'model', type: 'model' },
  { key: 'default-1', type: 'image', src: '/product-default-1.png' },
  { key: 'default-2', type: 'image', src: '/product-default-2.png' },
  { key: 'report', type: 'image', src: '/product-report-sample.png' }
])

const notices = [
  { title: '交易流程', text: '下单付款后，卖家发货，买家确认无误后交易完成。', icon: CircleCheck },
  { title: '验收说明', text: '请在收货后 3 天内完成验收，如有问题请及时联系平台处理。', icon: CircleCheck },
  { title: '退换政策', text: '非质量问题不支持退换，因质量问题产生的退换货运费由卖家承担。', icon: Box },
  { title: '安全保障', text: '平台提供交易保障服务，保障买卖双方权益。', icon: CircleCheck }
]

const requireLogin = () => {
  if (!userStore.isGuest) return true
  ElMessage.warning('游客模式下请先登录后使用该功能')
  router.push('/login')
  return false
}

const load = async () => {
  const [detailRes, reviewsRes] = await Promise.all([
    getTradeProductDetail(route.params.id),
    getProductReviews(route.params.id).catch(() => ({ data: [] }))
  ])
  detail.value = detailRes?.data || {}
  reviews.value = reviewsRes?.data || detail.value.reviews || []
  await loadChatHistory()
}

const loadChatHistory = async () => {
  const sellerId = detail.value.seller_id || detail.value.sellerId
  if (!sellerId || userStore.isGuest) return
  try {
    const res = await getTradeMessageHistory({ productId: detail.value.id, otherUserId: sellerId })
    const rows = res?.data || []
    if (rows.length) {
      chatMessages.value = rows.map((item) => ({
        id: item.id,
        content: item.content,
        time: String(item.createdAt || item.created_at || '').slice(11, 16) || '刚刚',
        mine: Number(item.senderId || item.sender_id) === Number(userStore.userId)
      }))
    }
  } catch {
    // 真实接口不可用时保留演示消息。
  }
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
  if (favoriteBusy.value || !requireLogin()) return
  favoriteBusy.value = true
  try {
    if (detail.value.favorite) await removeFavoriteProduct(detail.value.id)
    else await addFavoriteProduct(detail.value.id)
    detail.value.favorite = !detail.value.favorite
  } finally {
    favoriteBusy.value = false
  }
}

const addCart = async () => {
  if (cartBusy.value || !requireLogin()) return
  cartBusy.value = true
  try {
    await addCartItem({ productId: detail.value.id, quantity: 1 })
    ElMessage.success('已加入购物车')
  } finally {
    cartBusy.value = false
  }
}

const buyNow = async () => {
  if (buyBusy.value || !requireLogin()) return
  buyBusy.value = true
  try {
    const address = await ensureDefaultAddress()
    await createTradeOrder({ addressId: address?.id, items: [{ productId: detail.value.id, quantity: 1 }] })
    ElMessage.success('订单已创建，已进入待付款')
    router.push({ path: '/trade/order-list', query: { tab: 'PENDING_PAYMENT' } })
  } finally {
    buyBusy.value = false
  }
}

const sendMessage = async () => {
  const content = messageText.value.trim()
  if (!content) return
  const sellerId = detail.value.seller_id || detail.value.sellerId
  chatMessages.value.push({ id: `local-${Date.now()}`, content, time: '刚刚', mine: true })
  messageText.value = ''
  if (!sellerId || userStore.isGuest) return
  try {
    await sendTradeMessage({ productId: detail.value.id, receiverId: sellerId, messageType: 'TEXT', content })
  } catch {
    ElMessage.info('消息已本地记录，真实接口待联调')
  }
}

const openReport = () => {
  window.open('/product-report-sample.png', '_blank')
}

onMounted(load)
</script>

<style scoped>
.product-detail-page {
  display: grid;
  gap: 16px;
  color: #071331;
}

.product-shell,
.tab-panel {
  border: 1px solid #e1e8f3;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 38px rgba(47, 92, 164, 0.07);
}

.crumb-row {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 70px;
  padding: 0 28px;
  color: #66758f;
}

.crumb-row button {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.crumb-row strong {
  color: #071331;
}

.product-main {
  display: grid;
  grid-template-columns: minmax(320px, 496px) minmax(300px, 1fr) minmax(300px, 420px);
  grid-auto-flow: dense;
  gap: 24px;
  padding: 0 28px 20px;
}

.hero-image {
  position: relative;
  display: grid;
  place-items: center;
  height: 408px;
  overflow: hidden;
  border-radius: 14px;
  background: #f5efe8;
}

.hero-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.model-poster {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  background: #ffffff;
}

.model-poster strong {
  max-width: 92%;
  color: #111827;
  font-size: 92px;
  font-weight: 400;
  letter-spacing: 0;
  overflow-wrap: anywhere;
  text-align: center;
}

.image-count {
  position: absolute;
  right: 18px;
  bottom: 16px;
  padding: 4px 8px;
  border-radius: 8px;
  background: rgba(20, 25, 35, 0.66);
  color: #ffffff;
}

.thumb-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 13px;
  margin-top: 16px;
}

.thumb-row button {
  display: grid;
  place-items: center;
  height: 84px;
  overflow: hidden;
  border: 2px solid transparent;
  border-radius: 12px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px #edf2f8;
  cursor: pointer;
}

.thumb-row button.active {
  border-color: #1980ff;
}

.thumb-row img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-row span {
  color: #111827;
  font-size: 24px;
  line-height: 1.12;
  overflow-wrap: anywhere;
  text-align: center;
}

.product-info h1 {
  margin: 8px 0 12px;
  font-size: 28px;
}

.tag-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.tag-row span,
.seller-head span {
  padding: 5px 10px;
  border-radius: 5px;
  background: #f1f4f8;
  color: #6c7892;
  font-size: 13px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin: 26px 0 18px;
}

.price-row strong,
.tab-price strong {
  color: #ff3d12;
  font-size: 34px;
  font-weight: 500;
}

.price-row del,
.tab-price del {
  color: #7a879e;
}

.price-row em,
.tab-price span {
  padding: 4px 8px;
  border-radius: 5px;
  background: #ffe9e6;
  color: #ff3d12;
  font-style: normal;
}

.sales-row,
.mini-meta {
  display: flex;
  gap: 14px;
  align-items: center;
  color: #7a879e;
}

.sales-row i {
  width: 1px;
  height: 14px;
  background: #d6deeb;
}

.info-list {
  display: grid;
  gap: 18px;
  margin: 28px 0 30px;
  padding: 18px;
  border-radius: 9px;
  background: linear-gradient(135deg, #f8fbff, #f4f8fe);
}

.info-list div {
  display: grid;
  grid-template-columns: 110px 1fr 22px;
  gap: 16px;
  align-items: center;
}

.info-list dt {
  display: flex;
  gap: 8px;
  margin: 0;
  color: #73839f;
}

.info-list dd {
  margin: 0;
}

.info-list button {
  border: 0;
  background: transparent;
  color: #52627d;
}

.action-row {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr) minmax(0, 1fr);
  gap: 12px;
  width: 100%;
}

.action-row :deep(.el-button) {
  min-width: 0;
  width: 100%;
  height: 52px;
  margin-left: 0;
  padding: 0 12px;
  border-radius: 9px;
  font-size: 15px;
  font-weight: 800;
  white-space: nowrap;
}

.action-row :deep(.el-button .el-icon) {
  margin-right: 4px;
}

.outline-action {
  border-color: #1980ff;
  color: #1980ff;
}

.buy-action {
  background: linear-gradient(135deg, #2387ff, #1272f3);
  border: 0;
}

.side-stack {
  display: grid;
  gap: 22px;
  grid-row: span 2;
}

.info-card {
  min-width: 0;
  min-height: 214px;
  padding: 18px 20px;
  border: 1px solid #e1e8f3;
  border-radius: 12px;
  background: #ffffff;
}

.info-card h2 {
  margin: 0 0 16px;
  color: #071331;
  font-size: 16px;
}

.info-card h2 span {
  color: #7a879e;
  font-weight: 600;
}

.health-card table {
  width: 100%;
  border-collapse: collapse;
}

.health-card th,
.health-card td {
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e8eef7;
  text-align: left;
  font-size: 14px;
}

.health-card th {
  width: 22%;
  background: #f7f9fc;
  color: #53637f;
  font-weight: 700;
}

.health-card td {
  color: #273755;
}

.health-card p {
  margin: 12px 0 0;
  padding: 10px 12px;
  border: 1px solid #d7e8ff;
  border-radius: 7px;
  background: #f4f9ff;
  color: #1476ff;
  font-size: 13px;
}

.empty-record {
  min-height: 156px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: #8b9ab2;
  text-align: center;
}

.empty-record img {
  width: min(220px, 78%);
  aspect-ratio: 16 / 10;
  object-fit: contain;
}

.empty-record strong {
  color: #66758f;
  font-size: 14px;
}

.empty-record p {
  margin: 0;
  font-size: 13px;
}

.review-list {
  display: grid;
  gap: 14px;
}

.review-item {
  padding: 12px 14px;
  border: 1px solid #e8eef7;
  border-radius: 8px;
  background: #fbfdff;
}

.review-item strong {
  color: #071331;
  font-size: 14px;
}

.review-item p {
  margin: 8px 0;
  color: #273755;
  font-size: 13px;
  line-height: 1.6;
}

.review-item time {
  color: #93a0b6;
  font-size: 12px;
}

.seller-card,
.chat-card {
  border: 1px solid #e1e8f3;
  border-radius: 12px;
  background: #ffffff;
}

.seller-card {
  padding: 20px;
}

.seller-card h2,
.chat-title h2 {
  margin: 0;
  font-size: 18px;
}

.seller-head {
  display: flex;
  gap: 18px;
  align-items: center;
  margin-top: 24px;
}

.seller-head img,
.chat-message img {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
}

.seller-head strong {
  display: block;
  margin-bottom: 8px;
}

.seller-head p {
  margin: 9px 0 0;
  color: #53637f;
}

.seller-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 28px 0;
}

.seller-stats div {
  display: grid;
  place-items: center;
  gap: 6px;
  border-right: 1px solid #edf2f8;
}

.seller-stats div:last-child {
  border-right: 0;
}

.seller-stats span {
  color: #72809a;
}

.seller-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.seller-actions button {
  height: 40px;
  border: 1px solid #dfe7f2;
  border-radius: 6px;
  background: #ffffff;
  cursor: pointer;
}

.chat-card {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 568px;
}

.chat-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.chat-title span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.chat-title i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #35c983;
  box-shadow: 0 0 0 7px #e7f8f0;
}

.chat-body {
  display: grid;
  align-content: start;
  gap: 12px;
  padding: 20px;
  border-top: 1px solid #edf2f8;
  overflow: auto;
}

.chat-body > time,
.system-msg {
  justify-self: center;
  padding: 4px 8px;
  border-radius: 5px;
  background: #f1f4f8;
  color: #9aa6ba;
  font-size: 12px;
}

.chat-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.chat-message.mine {
  justify-content: flex-end;
}

.chat-message div {
  max-width: 70%;
}

.chat-message p {
  margin: 0;
  padding: 12px 14px;
  border: 1px solid #e0e8f3;
  border-radius: 9px;
  background: #ffffff;
  line-height: 1.55;
}

.chat-message.mine p {
  border: 0;
  background: #eef4ff;
  color: #1476ff;
}

.chat-message small {
  display: block;
  margin-top: 4px;
  color: #9aa6ba;
  font-size: 12px;
}

.chat-input {
  display: grid;
  grid-template-columns: 26px 26px 1fr 64px;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid #edf2f8;
}

.chat-input input {
  min-width: 0;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #dfe7f2;
  border-radius: 6px;
}

.chat-input button {
  border: 0;
  background: transparent;
  color: #7a879e;
  cursor: pointer;
}

.chat-input .send-btn {
  border-radius: 6px;
  background: #1980ff;
  color: #ffffff;
}

.sticky-tabs {
  display: flex;
  gap: 48px;
  height: 56px;
  padding: 0 28px;
  border-top: 1px solid #e4ebf5;
}

.sticky-tabs button {
  position: relative;
  border: 0;
  background: transparent;
  color: #273755;
  cursor: pointer;
}

.sticky-tabs button.active {
  color: #1476ff;
  font-weight: 800;
}

.sticky-tabs button.active::after {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: #1476ff;
  content: "";
}

.tab-panel {
  overflow: hidden;
}

.tab-card {
  margin: 26px 28px;
  padding: 20px 26px;
  border: 1px solid #e1e8f3;
  border-radius: 9px;
}

.detail-tab {
  display: grid;
  grid-template-columns: 1.25fr 0.88fr;
  gap: 60px;
}

.detail-tab h2 {
  margin: 0 0 18px;
  font-size: 26px;
}

.tab-price {
  display: flex;
  align-items: baseline;
  gap: 18px;
  margin-bottom: 22px;
}

.detail-tab p {
  color: #273755;
  line-height: 1.8;
}

.detail-tab dl,
.report-tab table,
.spec-tab table {
  width: 100%;
  border-collapse: collapse;
}

.detail-tab dl {
  display: grid;
  gap: 14px;
}

.detail-tab dl div {
  display: grid;
  grid-template-columns: 140px 1fr;
}

.detail-tab dt {
  display: flex;
  gap: 8px;
  color: #53637f;
}

.detail-tab dd,
.detail-tab dt {
  margin: 0;
}

.spec-tab th,
.spec-tab td,
.report-tab th,
.report-tab td {
  height: 44px;
  padding: 0 18px;
  border: 1px solid #e1e8f3;
  text-align: left;
}

.spec-tab th,
.report-tab th {
  width: 18%;
  background: #f7f9fc;
  color: #273755;
}

.report-tab {
  display: grid;
  grid-template-columns: 1fr 244px;
  gap: 14px;
}

.report-action {
  display: grid;
  place-items: center;
  border: 1px solid #e1e8f3;
  border-radius: 8px;
}

.report-action button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 22px;
  border: 1px solid #9dc3ff;
  border-radius: 6px;
  background: #ffffff;
  color: #1476ff;
  cursor: pointer;
}

.notice-tab {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 18px;
}

.notice-item > span {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #eaf3ff;
  color: #1980ff;
  font-size: 26px;
}

.notice-item strong {
  display: block;
  margin-bottom: 6px;
}

.notice-item p {
  margin: 0;
  color: #52627d;
  line-height: 1.6;
}

@media (max-width: 1440px) {
  .product-main {
    grid-template-columns: minmax(320px, 496px) minmax(300px, 1fr);
  }

  .side-stack {
    grid-column: 1 / -1;
    grid-template-columns: 420px 1fr;
    grid-row: auto;
  }
}

@media (max-width: 980px) {
  .product-main,
  .side-stack,
  .detail-tab,
  .report-tab,
  .notice-tab {
    grid-template-columns: 1fr;
  }

  .action-row {
    grid-template-columns: 1fr;
  }
}
</style>
