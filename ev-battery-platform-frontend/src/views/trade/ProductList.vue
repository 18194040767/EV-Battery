<template>
  <div class="page-shell">
    <section class="panel-card hero-panel">
      <div>
        <p>商品中心</p>
        <h2>商品市场</h2>
        <span>商品列表</span>
      </div>
      <div class="hero-actions">
        <el-button @click="$router.push({ path: '/trade/order-list', query: { tab: 'PENDING_PAYMENT' } })">查看订单中心</el-button>
        <el-button type="primary" @click="openPublish">发布我的商品</el-button>
      </div>
    </section>

    <section class="panel-card filter-panel">
      <el-input v-model="query.keyword" placeholder="搜索商品名称、档案编码或发货地" clearable class="search" @input="debouncedLoadProducts" @keyup.enter="loadProducts" />
      <el-select v-model="query.healthLevel" clearable placeholder="健康等级" class="field" @change="loadProducts">
        <el-option label="优秀" value="优秀" />
        <el-option label="良好" value="良好" />
        <el-option label="一般" value="一般" />
        <el-option label="较差" value="较差" />
      </el-select>
      <el-select v-model="query.batteryType" clearable placeholder="电池类型" class="field" @change="loadProducts">
        <el-option label="磷酸铁锂" value="磷酸铁锂" />
        <el-option label="三元锂" value="三元锂" />
      </el-select>
      <el-select v-model="query.sortBy" class="field" @change="loadProducts">
        <el-option label="综合排序" value="" />
        <el-option label="最新发布" value="latest" />
        <el-option label="价格从低到高" value="priceAsc" />
        <el-option label="价格从高到低" value="priceDesc" />
        <el-option label="卖家信用优先" value="credit" />
      </el-select>
      <el-button @click="loadProducts">刷新列表</el-button>
    </section>

    <el-skeleton v-if="loading" :rows="10" animated />
    <el-empty v-else-if="!products.length" description="当前暂无可展示商品" />

    <section v-else class="grid">
      <article v-for="item in products" :key="item.id" class="panel-card product-card">
        <div class="cover-box" @click="$router.push('/trade/product/' + item.id)">
          <img :src="item.cover_image || placeholder" alt="" class="cover" />
          <span v-if="isOwnProduct(item)" class="owner-badge">我的商品</span>
          <span v-else-if="Number(item.stock || 0) < 1" class="sold-badge">库存告罄</span>
        </div>
        <div class="body">
          <strong class="title" @click="$router.push('/trade/product/' + item.id)">{{ item.title }}</strong>
          <div class="price-row">
            <span class="price">¥{{ item.price }}</span>
            <small v-if="item.original_price">¥{{ item.original_price }}</small>
          </div>
          <p class="meta">{{ item.health_level || '待评估' }} · {{ item.shipping_from || '全国可发' }} · 库存 {{ item.stock || 0 }}</p>
          <p class="meta">卖家 {{ item.sellerNickname || '平台卖家' }} · 信用 {{ item.creditScore || 100 }}</p>
          <div class="actions">
            <el-button text :loading="isProductBusy(item, 'favorite')" @click="toggleFavorite(item)">{{ item.favorite ? '取消收藏' : '加入收藏' }}</el-button>
            <el-button v-if="!isOwnProduct(item)" text :loading="isProductBusy(item, 'cart')" :disabled="Number(item.stock || 0) < 1" @click="addCart(item)">加入购物车</el-button>
            <el-button v-if="!isOwnProduct(item)" type="primary" :loading="isProductBusy(item, 'buy')" :disabled="Number(item.stock || 0) < 1" @click="buyNow(item)">立即下单</el-button>
            <el-button v-else type="primary" plain @click="$router.push('/trade/user')">管理我的商品</el-button>
          </div>
        </div>
      </article>
    </section>

    <div class="pager">
      <el-pagination layout="total, prev, pager, next" :current-page="page" :page-size="size" :total="total" @current-change="changePage" />
    </div>

    <button class="cart-fab" type="button" @click="$router.push('/trade/cart')">
      <el-badge :value="cartCount" :hidden="!cartCount" :max="99">
        <el-icon><ShoppingCart /></el-icon>
      </el-badge>
    </button>

    <el-dialog v-model="publishVisible" title="发布我的商品" width="720px">
      <el-form :model="publishForm" label-position="top">
        <el-form-item label="关联电池档案">
          <el-select v-model="publishForm.batteryId" placeholder="请选择已评估档案">
            <el-option v-for="item in batteryOptions" :key="item.id" :label="`${item.batteryCode} / ${item.latestHealthLevel || '已评估'}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品标题">
          <el-input v-model="publishForm.title" maxlength="60" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="电池类型">
              <el-input v-model="publishForm.batteryType" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发货地">
              <el-input v-model="publishForm.shippingFrom" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="售价">
              <el-input-number v-model="publishForm.price" :controls="false" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存数量">
              <el-input-number v-model="publishForm.stock" :controls="false" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="商品图片">
          <div class="upload-row">
            <el-upload :auto-upload="false" :show-file-list="false" accept="image/*" :on-change="onImageChange">
              <el-button>上传商品图片</el-button>
            </el-upload>
            <span class="upload-tip">支持本地图片预览</span>
          </div>
          <div v-if="publishForm.coverImage" class="preview-box">
            <img :src="publishForm.coverImage" alt="" class="preview-image" />
          </div>
        </el-form-item>
        <el-form-item label="商品说明">
          <el-input v-model="publishForm.description" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="submitPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingCart } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getBatteryList } from '../../api/battery'
import {
  addCartItem,
  addFavoriteProduct,
  createTradeOrder,
  createTradeProduct,
  getCartList,
  getAddresses,
  getFavoriteStatus,
  getTradeProducts,
  removeFavoriteProduct,
  saveAddress
} from '../../api/trade'
import { fileToDataUrl } from '../../utils/file'
import { debounce } from '../../utils/concurrency'

const router = useRouter()
const userStore = useUserStore()

const placeholder = 'https://dummyimage.com/600x420/dce7e5/1e3a38&text=EV'
const loading = ref(false)
const publishing = ref(false)
const publishVisible = ref(false)
const products = ref([])
const batteryOptions = ref([])
const page = ref(1)
const size = ref(8)
const total = ref(0)
const cartCount = ref(0)
const busyProducts = reactive({})

const query = reactive({
  keyword: '',
  healthLevel: '',
  batteryType: '',
  sortBy: ''
})

const publishForm = reactive({
  batteryId: null,
  title: '',
  batteryType: '磷酸铁锂',
  price: 1680,
  stock: 1,
  shippingFrom: '上海',
  coverImage: '',
  description: '提供评估结果、检测摘要与履约演示。'
})

const isOwnProduct = (item) => Number(item.seller_id || item.sellerId) === Number(userStore.userId)

const requireLogin = () => {
  if (!userStore.isGuest) return true
  ElMessage.warning('游客模式下请先登录后使用该功能')
  router.push('/login')
  return false
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

const loadProducts = async () => {
  page.value = Math.max(page.value, 1)
  loading.value = true
  try {
    const res = await getTradeProducts({ page: page.value, size: size.value, ...query })
    products.value = res?.data?.records || []
    total.value = res?.data?.total || 0
    if (userStore.isGuest) return
    const favorites = await Promise.all(products.value.map((item) => getFavoriteStatus(item.id)))
    products.value = products.value.map((item, index) => ({
      ...item,
      favorite: !!favorites[index]?.data?.favorite
    }))
  } finally {
    loading.value = false
  }
}

const loadCartCount = async () => {
  if (userStore.isGuest) {
    cartCount.value = 0
    return
  }
  const res = await getCartList().catch(() => ({ data: [] }))
  cartCount.value = (res?.data || []).length
}

const debouncedLoadProducts = debounce(() => {
  page.value = 1
  loadProducts()
}, 350)

const openPublish = async () => {
  if (!requireLogin()) return
  const res = await getBatteryList({ page: 1, size: 200, statuses: ['ASSESSED'] })
  batteryOptions.value = res?.data?.records || []
  publishVisible.value = true
}

const onImageChange = async (uploadFile) => {
  if (!uploadFile?.raw) return
  publishForm.coverImage = await fileToDataUrl(uploadFile.raw)
}

const submitPublish = async () => {
  if (publishing.value) return
  if (!publishForm.batteryId) {
    ElMessage.warning('请先选择已评估的电池档案')
    return
  }
  if (!publishForm.coverImage) {
    ElMessage.warning('请上传商品图片')
    return
  }
  publishing.value = true
  try {
    await createTradeProduct({
      ...publishForm,
      imageUrls: [publishForm.coverImage],
      publishStatus: 'ON_SHELF'
    })
    ElMessage.success('商品发布成功')
    publishVisible.value = false
    await loadProducts()
  } finally {
    publishing.value = false
  }
}

const isProductBusy = (item, action) => !!busyProducts[`${action}-${item.id}`]

const withProductGuard = async (item, action, task) => {
  const key = `${action}-${item.id}`
  if (busyProducts[key]) return
  busyProducts[key] = true
  try {
    await task()
  } finally {
    busyProducts[key] = false
  }
}

const addCart = async (item) => withProductGuard(item, 'cart', async () => {
  if (!requireLogin()) return
  await addCartItem({ productId: item.id, quantity: 1 })
  ElMessage.success('商品已加入购物车')
  await loadCartCount()
})

const buyNow = async (item) => withProductGuard(item, 'buy', async () => {
  if (!requireLogin()) return
  const address = await ensureDefaultAddress()
  await createTradeOrder({
    addressId: address?.id,
    items: [{ productId: item.id, quantity: 1 }]
  })
  ElMessage.success('订单已创建，已进入待付款')
  await loadCartCount()
  router.push({ path: '/trade/order-list', query: { tab: 'PENDING_PAYMENT' } })
})

const toggleFavorite = async (item) => withProductGuard(item, 'favorite', async () => {
  if (!requireLogin()) return
  if (item.favorite) {
    await removeFavoriteProduct(item.id)
  } else {
    await addFavoriteProduct(item.id)
  }
  item.favorite = !item.favorite
})

const changePage = (value) => {
  page.value = value
  loadProducts()
}

onMounted(async () => {
  await Promise.all([loadProducts(), loadCartCount()])
})
</script>

<style scoped>
.hero-panel,
.filter-panel {
  padding: 24px;
}

.hero-panel,
.filter-panel,
.actions,
.hero-actions,
.upload-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hero-panel p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.hero-panel h2 {
  margin: 0 0 8px;
}

.hero-panel span,
.meta,
.upload-tip {
  color: var(--app-muted);
}

.search {
  width: 320px;
}

.field {
  width: 180px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.product-card {
  overflow: hidden;
}

.cover-box {
  position: relative;
  cursor: pointer;
}

.cover {
  width: 100%;
  height: 220px;
  object-fit: cover;
}

.owner-badge,
.sold-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  padding: 6px 12px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.owner-badge {
  background: rgba(31, 117, 255, 0.92);
}

.sold-badge {
  background: rgba(148, 23, 23, 0.9);
}

.body {
  padding: 18px;
}

.title {
  display: block;
  margin-bottom: 12px;
  cursor: pointer;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.price {
  color: #b45309;
  font-size: 24px;
  font-weight: 700;
}

.price-row small {
  color: #94a3b8;
  text-decoration: line-through;
}

.pager {
  display: flex;
  justify-content: flex-end;
}

.cart-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 62px;
  height: 62px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary-blue), var(--primary-blue-dark));
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 44px var(--blue-shadow);
  cursor: pointer;
  z-index: 30;
  --primary-blue: #2E80FF;
  --primary-blue-light: #5DA2FF;
  --primary-blue-dark: #1F6FFF;
  --blue-shadow: rgba(46,128,255,0.25);
}

.cart-fab .el-icon {
  font-size: 26px;
}

.preview-box {
  margin-top: 14px;
}

.preview-image {
  width: 180px;
  height: 120px;
  border-radius: 16px;
  object-fit: cover;
  border: 1px solid var(--app-border);
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .search,
  .field {
    width: 100%;
  }

  .cart-fab {
    right: 18px;
    bottom: 18px;
  }
}
</style>

