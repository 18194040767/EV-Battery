<template>
  <div class="page-shell">
    <section class="panel-card page-head">
      <div>
        <p>购物车</p>
        <h2>待结算</h2>
      </div>
      <div class="actions">
        <el-button @click="clearInvalid">一键清理失效商品</el-button>
        <el-button type="primary" @click="checkout">生成待付款订单</el-button>
      </div>
    </section>

    <el-empty v-if="!items.length" description="购物车内暂无商品" />

    <section v-for="item in items" :key="item.cartId || item.id" class="panel-card item-card">
      <el-checkbox v-model="item.checkedFlag" :disabled="item.invalidFlag" @change="saveItem(item)" />
      <img :src="item.cover_image || placeholder" alt="" class="cover" />
      <div class="item-info">
        <strong>{{ item.title }}</strong>
        <p>单价 ¥{{ item.price }} · 当前库存 {{ item.stock }}</p>
        <p v-if="item.invalidFlag" class="warn">该商品已下架或库存不足，当前不可下单。</p>
      </div>
      <el-input-number
        v-model="item.quantity"
        :min="1"
        :max="Math.max(Number(item.stock || 1), 1)"
        :controls="false"
        @change="saveItem(item)"
      />
      <strong class="amount">¥{{ (Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2) }}</strong>
      <el-button text @click="removeItem(item.cartId || item.id)">删除</el-button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  clearInvalidCartItems,
  createTradeOrder,
  deleteCartItem,
  getAddresses,
  getCartList,
  saveAddress,
  updateCartItem
} from '../../api/trade'

const router = useRouter()
const placeholder = 'https://dummyimage.com/600x420/dce7e5/1e3a38&text=Cart'
const items = ref([])

const normalizeCartItem = (item = {}) => ({
  ...item,
  stock: Number(item.stock || 0),
  quantity: Number(item.quantity || 1),
  checkedFlag: String(item.checkedFlag) === '1' || item.checkedFlag === true,
  invalidFlag: String(item.invalidFlag) === '1' || item.invalidFlag === true
})

const checkedItems = computed(() => items.value.filter((item) => item.checkedFlag && !item.invalidFlag))

const load = async () => {
  const res = await getCartList()
  items.value = (res?.data || []).map(normalizeCartItem)
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

const saveItem = async (item) => {
  await updateCartItem(item.cartId || item.id, { quantity: item.quantity, checkedFlag: item.checkedFlag })
}

const removeItem = async (id) => {
  await deleteCartItem(id)
  ElMessage.success('商品已移出购物车')
  load()
}

const clearInvalid = async () => {
  await clearInvalidCartItems()
  ElMessage.success('失效商品已清理')
  load()
}

const checkout = async () => {
  if (!checkedItems.value.length) {
    ElMessage.warning('请先勾选可结算商品')
    return
  }

  const address = await ensureDefaultAddress()
  await createTradeOrder({
    addressId: address?.id,
    items: checkedItems.value.map((item) => ({
      productId: item.productId || item.product_id || item.id,
      quantity: item.quantity
    }))
  })
  ElMessage.success('订单已创建，正在跳转到待付款列表')
  await load()
  router.push({ path: '/trade/order-list', query: { tab: 'PENDING_PAYMENT' } })
}

onMounted(load)
</script>

<style scoped>
.page-head,
.item-card {
  padding: 24px;
}

.page-head,
.item-card,
.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
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
.item-info p {
  color: var(--app-muted);
}

.cover {
  width: 110px;
  height: 110px;
  border-radius: 18px;
  object-fit: cover;
}

.item-info {
  flex: 1;
}

.warn {
  color: #b45309;
}

.amount {
  color: #b45309;
}

@media (max-width: 768px) {
  .page-head,
  .item-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

