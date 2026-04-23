<template>
  <div class="page-shell">
    <section class="panel-card order-head">
      <div>
        <p>订单中心</p>
        <h2>订单履约</h2>
        <span>订单状态</span>
      </div>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="全部订单" name="ALL" />
        <el-tab-pane label="待支付" name="PENDING_PAYMENT" />
        <el-tab-pane label="待发货" name="PAID_PENDING_SHIP" />
        <el-tab-pane label="运输中" name="SHIPPED_PENDING_RECEIVE" />
        <el-tab-pane label="待评价" name="COMPLETED_PENDING_REVIEW" />
      </el-tabs>
    </section>

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="!orders.length" description="当前没有匹配的订单" />

    <section
      v-for="item in orders"
      :key="item.id"
      class="panel-card order-card"
      :class="{ 'pending-payment-card': item.orderStatus === 'PENDING_PAYMENT' }"
      @click="handleOrderCardClick(item)"
    >
      <div class="card-top">
        <div>
          <strong>{{ item.orderNo }}</strong>
          <p>{{ statusTextMap[item.orderStatus] || item.orderStatus }}</p>
        </div>
        <el-tag :type="statusTypeMap[item.orderStatus] || 'info'">
          {{ statusTextMap[item.orderStatus] || item.orderStatus }}
        </el-tag>
      </div>

      <div class="card-main">
        <img :src="item.productSnapshot?.cover_image || item.productSnapshot?.coverImage || placeholder" alt="" class="cover" />
        <div class="order-meta">
          <h3>{{ item.productSnapshot?.title || '电池商品' }}</h3>
          <p>数量 {{ item.quantity }} · 单价 ¥{{ item.unitPrice }} · 总价 ¥{{ item.amount }}</p>
          <p>收货地址 {{ formatAddress(item.addressSnapshot) || '待选择地址' }}</p>
          <p v-if="item.logisticsCompany || logisticsMap[item.id]?.trackingNo">
            运单号 {{ logisticsMap[item.id]?.trackingNo || item.logisticsNo || '-' }} ·
            {{ logisticsMap[item.id]?.currentCheckpoint?.city || '等待揽收' }}
          </p>
          <p v-if="item.orderStatus === 'SHIPPED_PENDING_RECEIVE'">
            预计 {{ logisticsMap[item.id]?.etaDays ?? 0 }} 天后到达 ·
            当前进度 {{ logisticsMap[item.id]?.currentStatus || '运输中' }}
          </p>
          <p v-if="item.orderStatus === 'COMPLETED' && item.review">
            评价结果 {{ reviewLabel(item.review.score) }}
          </p>
        </div>
      </div>

      <div v-if="item.orderStatus === 'PENDING_PAYMENT'" class="pay-entry-box">
        <el-button class="pay-entry-button" type="primary" size="large" @click.stop="openPayDialog(item)">
          进入支付
        </el-button>
      </div>

      <div v-if="showTimeline(item)" class="timeline-box">
        <div class="timeline-head">
          <strong>物流详情</strong>
          <div class="timeline-actions">
            <el-tag v-if="logisticsMap[item.id]?.trackingNo">{{ logisticsMap[item.id]?.trackingNo }}</el-tag>
            <el-tag v-if="item.orderStatus === 'SHIPPED_PENDING_RECEIVE'" type="success">
              {{ logisticsMap[item.id]?.currentCheckpoint?.city || '运输中' }}
            </el-tag>
          </div>
        </div>
        <el-progress :percentage="Number(logisticsMap[item.id]?.progressPercent || 0)" :stroke-width="10" />
        <el-timeline>
          <el-timeline-item
            v-for="node in logisticsMap[item.id].nodes"
            :key="`${item.id}-${node.time}-${node.status}`"
            :timestamp="formatTime(node.time)"
            :type="node.isCurrent ? 'primary' : 'info'"
          >
            <strong>{{ node.status }}</strong>
            <p>{{ node.description }}</p>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div v-if="item.review && item.orderStatus === 'COMPLETED'" class="review-box">
        <div class="review-head">
          <strong>我的评价</strong>
          <span>{{ reviewLabel(item.review.score) }}</span>
        </div>
        <p>{{ item.review.content || '未填写评价说明。' }}</p>
      </div>

      <div class="card-actions">
        <el-button
          v-if="item.orderStatus !== 'PENDING_PAYMENT' && logisticsMap[item.id]?.nodes?.length"
          @click.stop="toggleLogistics(item.id)"
        >
          {{ expandedLogistics[item.id] ? '收起物流' : '展开物流' }}
        </el-button>
        <el-button v-if="item.orderStatus !== 'PENDING_PAYMENT' && hasLogistics(item)" @click.stop="openLogisticsDetail(item)">
          查看物流
        </el-button>
        <el-button v-if="item.orderStatus === 'COMPLETED_PENDING_REVIEW'" type="primary" @click.stop="openReview(item)">
          评价订单
        </el-button>
        <el-button v-if="item.orderStatus === 'COMPLETED'" @click.stop="openContractCenter(item)">
          查看合同
        </el-button>
        <el-dropdown
          v-if="showMockMenu(item)"
          trigger="click"
          placement="bottom-end"
          @command="(command) => handleMockCommand(command, item)"
        >
          <el-button text class="mock-trigger" @click.stop>模拟物流</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="item.orderStatus === 'PAID_PENDING_SHIP'" command="ship">一键发货</el-dropdown-item>
              <el-dropdown-item v-if="item.orderStatus === 'SHIPPED_PENDING_RECEIVE'" command="advance">刷新中转站</el-dropdown-item>
              <el-dropdown-item v-if="item.orderStatus === 'SHIPPED_PENDING_RECEIVE'" command="sign">一键签收</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </section>

    <el-dialog v-model="payDialogVisible" title="模拟支付" width="620px">
      <div v-if="currentPayOrder" class="pay-dialog">
        <div class="pay-summary">
          <strong>{{ currentPayOrder.productSnapshot?.title || '电池商品' }}</strong>
          <span>订单金额 ¥{{ currentPayOrder.amount }}</span>
        </div>

        <el-form label-position="top">
          <div class="address-toolbar">
            <el-button plain @click="openAddressDialog">
              <el-icon><Plus /></el-icon>
              新增地址
            </el-button>
          </div>

          <el-form-item label="选择收货地址">
            <el-radio-group v-model="selectedAddressId" class="address-list">
              <el-radio
                v-for="item in addresses"
                :key="item.id"
                :label="item.id"
                border
                class="address-card"
              >
                <div>
                  <strong>{{ item.receiver_name || item.receiverName }} {{ item.receiver_phone || item.receiverPhone }}</strong>
                  <p>{{ formatAddress(item) }}</p>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedAddressId" @click="submitPayment">确认支付</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="addressDialogVisible" title="新增收货地址" width="720px">
      <el-form :model="addressForm" label-position="top" class="address-form">
        <el-row :gutter="16">
          <el-col :md="12" :xs="24">
            <el-form-item label="收货人">
              <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="联系电话">
              <el-input v-model="addressForm.receiverPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="address-locate-row">
          <el-button :loading="locatingAddress" @click="locateAddress">
            <el-icon><Location /></el-icon>
            一键定位
          </el-button>
          <span>定位依赖浏览器授权，成功后会自动填充省市区。</span>
        </div>

        <el-row :gutter="16">
          <el-col :md="8" :xs="24">
            <el-form-item label="省份">
              <el-select v-model="addressForm.province" placeholder="请选择省份" filterable @change="handleProvinceChange">
                <el-option v-for="item in provinceOptions" :key="item.name" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="城市">
              <el-select v-model="addressForm.city" placeholder="请选择城市" filterable :disabled="!cityOptions.length" @change="handleCityChange">
                <el-option v-for="item in cityOptions" :key="item.name" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item label="区县">
              <el-select v-model="addressForm.district" placeholder="请选择区县" filterable :disabled="!districtOptions.length">
                <el-option v-for="item in districtOptions" :key="item.name" :label="item.name" :value="item.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细地址">
          <el-input v-model="addressForm.detailAddress" type="textarea" :rows="3" placeholder="请输入街道、门牌号等信息" />
        </el-form-item>

        <el-switch v-model="addressForm.isDefault" active-text="设为默认地址" />
      </el-form>
      <template #footer>
        <el-button @click="addressDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingAddress" @click="submitAddress">保存地址</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="payingVisible"
      title="支付处理中"
      width="360px"
      :show-close="false"
      :close-on-click-modal="false"
    >
      <div class="paying-box">
        <div class="spinner"></div>
        <p>系统正在模拟支付，请稍候 2 秒...</p>
      </div>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="发布评价" width="560px">
      <div class="rating-row">
        <button
          v-for="option in reviewOptions"
          :key="option.value"
          class="rating-card"
          :class="{ active: reviewForm.score === option.value }"
          type="button"
          @click="reviewForm.score = option.value"
        >
          <component :is="option.icon" class="rating-icon" />
          <strong>{{ option.label }}</strong>
        </button>
      </div>
      <el-form :model="reviewForm" label-position="top">
        <el-form-item label="评价原因">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="请填写评价原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">发布评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheck, Location, Plus, RemoveFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { mockShipTradeOrder, queryLogisticsStatus } from '../../api/logistics'
import {
  confirmTradeOrder,
  createTradeReview,
  getAddresses,
  getTradeOrders,
  payTradeOrder,
  saveAddress,
  updateTradeOrderAddress
} from '../../api/trade'
import { generateContract } from '../../api/contract'
import { advanceMockLogistics, mergeMockLogisticsState, resetMockLogistics } from '../../utils/mockLogistics'
import { loadAmap } from '../../utils/amap'

const router = useRouter()
const route = useRoute()
const placeholder = 'https://dummyimage.com/600x420/dce7e5/1e3a38&text=EV'
const validTabs = ['ALL', 'PENDING_PAYMENT', 'PAID_PENDING_SHIP', 'SHIPPED_PENDING_RECEIVE', 'COMPLETED_PENDING_REVIEW']

const activeTab = ref(validTabs.includes(route.query.tab) ? route.query.tab : 'ALL')
const loading = ref(false)
const orders = ref([])
const logisticsMap = ref({})
const addresses = ref([])
const currentPayOrder = ref(null)
const payDialogVisible = ref(false)
const payingVisible = ref(false)
const selectedAddressId = ref(null)
const reviewVisible = ref(false)
const addressDialogVisible = ref(false)
const savingAddress = ref(false)
const locatingAddress = ref(false)
const provinceOptions = ref([])
const cityOptions = ref([])
const districtOptions = ref([])
const expandedLogistics = reactive({})
const addressForm = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: true
})
const reviewForm = reactive({
  orderId: null,
  score: 5,
  content: '物流准时，商品与描述一致。'
})

const reviewOptions = [
  { value: 5, label: '好评', icon: CircleCheck },
  { value: 3, label: '中评', icon: WarningFilled },
  { value: 1, label: '差评', icon: RemoveFilled }
]

const statusTextMap = {
  PENDING_PAYMENT: '待支付',
  PAID_PENDING_SHIP: '待发货',
  SHIPPED_PENDING_RECEIVE: '运输中',
  COMPLETED_PENDING_REVIEW: '待评价',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const statusTypeMap = {
  PENDING_PAYMENT: 'warning',
  PAID_PENDING_SHIP: 'info',
  SHIPPED_PENDING_RECEIVE: 'primary',
  COMPLETED_PENDING_REVIEW: 'success',
  COMPLETED: 'success',
  CANCELLED: 'info'
}

const formatAddress = (address = {}) =>
  [address.province, address.city, address.district, address.detail_address || address.detailAddress].filter(Boolean).join(' ')

const formatTime = (time) => (time ? String(time).replace('T', ' ') : '-')

const reviewLabel = (score) => {
  if (Number(score) >= 5) return '好评'
  if (Number(score) >= 3) return '中评'
  return '差评'
}

const hasLogistics = (item) => Boolean(item.logisticsCompany || item.logisticsNo || logisticsMap.value[item.id]?.trackingNo)
const showMockMenu = (item) => ['PAID_PENDING_SHIP', 'SHIPPED_PENDING_RECEIVE'].includes(item.orderStatus)
const showTimeline = (item) => item.orderStatus !== 'PENDING_PAYMENT' && logisticsMap.value[item.id]?.nodes?.length && expandedLogistics[item.id]

const normalizeOrder = (item = {}) => ({
  ...item,
  orderStatus: item.orderStatus || item.order_status || '',
  logisticsCompany: item.logisticsCompany || item.logistics_company || '',
  logisticsNo: item.logisticsNo || item.logistics_no || '',
  unitPrice: item.unitPrice ?? item.unit_price,
  productId: item.productId ?? item.product_id
})

const normalizeAddress = (item = {}) => ({
  ...item,
  id: Number(item.id || 0),
  receiverName: item.receiverName || item.receiver_name || '',
  receiverPhone: item.receiverPhone || item.receiver_phone || '',
  province: item.province || '',
  city: item.city || '',
  district: item.district || '',
  detailAddress: item.detailAddress || item.detail_address || '',
  isDefault: String(item.isDefault ?? item.is_default ?? '') === '1' || item.isDefault === true
})

const ensureAddresses = async () => {
  let list = (await getAddresses()).data || []
  if (!list.length) {
    await saveAddress({
      receiverName: '平台演示收货人',
      receiverPhone: '13800000000',
      province: '上海市',
      city: '上海市',
      district: '浦东新区',
      detailAddress: '世纪大道 1888 号',
      isDefault: true
    })
    list = (await getAddresses()).data || []
  }
  addresses.value = list.map(normalizeAddress)
  return addresses.value
}

const loadOrders = async () => {
  loading.value = true
  try {
    const res = await getTradeOrders({ tab: activeTab.value })
    orders.value = (res?.data || []).map(normalizeOrder)
    await Promise.all(
      orders.value
        .filter((item) => ['SHIPPED_PENDING_RECEIVE', 'COMPLETED_PENDING_REVIEW', 'COMPLETED'].includes(item.orderStatus))
        .map((item) => refreshLogistics(item.id, false))
    )
  } finally {
    loading.value = false
  }
}

const refreshLogistics = async (orderId, notice = true) => {
  const res = await queryLogisticsStatus(orderId)
  logisticsMap.value = {
    ...logisticsMap.value,
    [orderId]: mergeMockLogisticsState(orderId, res?.data || {})
  }
  if (notice) ElMessage.success('物流状态已刷新')
}

const handleTabChange = async (tab) => {
  activeTab.value = tab
  await router.replace({ query: { ...route.query, tab } })
  await loadOrders()
}

const openPayDialog = async (item) => {
  currentPayOrder.value = item
  const list = await ensureAddresses()
  selectedAddressId.value = list[0]?.id || null
  payDialogVisible.value = true
}

const resetAddressForm = () => {
  addressForm.receiverName = ''
  addressForm.receiverPhone = ''
  addressForm.province = ''
  addressForm.city = ''
  addressForm.district = ''
  addressForm.detailAddress = ''
  addressForm.isDefault = !addresses.value.length
  cityOptions.value = []
  districtOptions.value = []
}

const districtItems = (nodes = []) =>
  (nodes || []).map((item) => ({
    name: item.name,
    adcode: item.adcode
  }))

const searchDistrict = async (keyword, level = 'district') => {
  const AMap = await loadAmap()
  await new Promise((resolve, reject) => {
    AMap.plugin(['AMap.DistrictSearch', 'AMap.Geocoder'], () => resolve())
    window.setTimeout(() => reject(new Error('高德插件加载失败')), 8000)
  }).catch(() => null)
  return await new Promise((resolve) => {
    const search = new AMap.DistrictSearch({ subdistrict: 1, extensions: 'base', level })
    search.search(keyword, (status, result) => {
      if (status !== 'complete') {
        resolve([])
        return
      }
      resolve(districtItems(result?.districtList?.[0]?.districtList))
    })
  })
}

const initProvinceOptions = async () => {
  if (provinceOptions.value.length) return
  provinceOptions.value = await searchDistrict('中国', 'country')
}

const handleProvinceChange = async (value) => {
  addressForm.city = ''
  addressForm.district = ''
  cityOptions.value = await searchDistrict(value, 'province')
  districtOptions.value = []
}

const handleCityChange = async (value) => {
  addressForm.district = ''
  districtOptions.value = await searchDistrict(value, 'city')
}

const openAddressDialog = async () => {
  resetAddressForm()
  await initProvinceOptions()
  addressDialogVisible.value = true
}

const locateAddress = async () => {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位')
    return
  }
  locatingAddress.value = true
  try {
    await initProvinceOptions()
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      })
    })
    const AMap = await loadAmap()
    await new Promise((resolve, reject) => {
      AMap.plugin(['AMap.Geocoder'], () => resolve())
      window.setTimeout(() => reject(new Error('地理编码插件加载失败')), 8000)
    }).catch(() => null)
    const geocoder = new AMap.Geocoder({ city: '全国' })
    const detail = await new Promise((resolve, reject) => {
      geocoder.getAddress(
        [position.coords.longitude, position.coords.latitude],
        (status, result) => {
          if (status !== 'complete' || !result?.regeocode) {
            reject(new Error('定位解析失败'))
            return
          }
          resolve(result.regeocode)
        }
      )
    })
    const component = detail.addressComponent || {}
    addressForm.province = component.province || ''
    await handleProvinceChange(addressForm.province)
    addressForm.city = component.city || component.province || ''
    await handleCityChange(addressForm.city)
    addressForm.district = component.district || ''
    addressForm.detailAddress = detail.formattedAddress || addressForm.detailAddress
    ElMessage.success('定位成功，已填充地址')
  } catch (error) {
    ElMessage.warning('定位失败，请手动填写地址')
  } finally {
    locatingAddress.value = false
  }
}

const submitAddress = async () => {
  if (!addressForm.receiverName || !addressForm.receiverPhone || !addressForm.province || !addressForm.city || !addressForm.district || !addressForm.detailAddress) {
    ElMessage.warning('请完整填写收货地址')
    return
  }
  savingAddress.value = true
  try {
    await saveAddress({ ...addressForm })
    const list = await ensureAddresses()
    selectedAddressId.value = list[0]?.id || null
    addressDialogVisible.value = false
    ElMessage.success('地址已保存')
  } finally {
    savingAddress.value = false
  }
}

const handleOrderCardClick = (item) => {
  if (item.orderStatus !== 'PENDING_PAYMENT') return
  openPayDialog(item)
}

const toggleLogistics = (orderId) => {
  expandedLogistics[orderId] = !expandedLogistics[orderId]
}

const submitPayment = async () => {
  if (!currentPayOrder.value || !selectedAddressId.value) {
    ElMessage.warning('请先选择收货地址')
    return
  }

  await updateTradeOrderAddress(currentPayOrder.value.id, { addressId: selectedAddressId.value })
  payDialogVisible.value = false
  payingVisible.value = true
  await new Promise((resolve) => window.setTimeout(resolve, 2000))
  await payTradeOrder(currentPayOrder.value.id)
  payingVisible.value = false
  ElMessage.success('支付成功，订单已移入待发货')
  await router.replace({ query: { ...route.query, tab: 'PAID_PENDING_SHIP' } })
  activeTab.value = 'PAID_PENDING_SHIP'
  await loadOrders()
}

const handleMockCommand = async (command, item) => {
  if (command === 'ship') {
    await mockShipTradeOrder(item.id, { company: '平台模拟物流' })
    resetMockLogistics(item.id, 5, 0)
    ElMessage.success('已一键发货，订单进入运输中')
    await router.replace({ query: { ...route.query, tab: 'SHIPPED_PENDING_RECEIVE' } })
    activeTab.value = 'SHIPPED_PENDING_RECEIVE'
    await loadOrders()
    await refreshLogistics(item.id, false)
    return
  }

  if (command === 'advance') {
    const total = Math.max((logisticsMap.value[item.id]?.nodes || []).length, 5)
    advanceMockLogistics(item.id, total)
    await refreshLogistics(item.id, false)
    ElMessage.success(`物流已推进到 ${logisticsMap.value[item.id]?.currentCheckpoint?.city || '下一中转站'}`)
    return
  }

  if (command === 'sign') {
    await confirmTradeOrder(item.id)
    await generateContract(item.id).catch(() => null)
    ElMessage.success('已一键签收，订单进入待评价')
    await router.replace({ query: { ...route.query, tab: 'COMPLETED_PENDING_REVIEW' } })
    activeTab.value = 'COMPLETED_PENDING_REVIEW'
    await loadOrders()
  }
}

const openLogisticsDetail = (item) => {
  const waybillNo = logisticsMap.value[item.id]?.trackingNo || item.logisticsNo || ''
  router.push({
    path: '/logistics/list',
    query: {
      waybillNo,
      orderId: String(item.id)
    }
  })
}

const openContractCenter = async (item) => {
  const result = await generateContract(item.id).catch(() => null)
  const contractId = result?.data?.id || result?.id || ''
  router.push({
    path: '/contract/list',
    query: {
      orderId: String(item.id),
      contractId: contractId ? String(contractId) : undefined
    }
  })
}

const openReview = (item) => {
  reviewForm.orderId = item.id
  reviewForm.score = 5
  reviewForm.content = '物流准时，商品与描述一致。'
  reviewVisible.value = true
}

const submitReview = async () => {
  await createTradeReview({ ...reviewForm })
  reviewVisible.value = false
  ElMessage.success('评价已发布，订单已完成')
  await router.replace({ query: { ...route.query, tab: 'ALL' } })
  activeTab.value = 'ALL'
  await loadOrders()
}

watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab !== 'string' || !validTabs.includes(tab) || tab === activeTab.value) return
    activeTab.value = tab
    loadOrders()
  }
)

onMounted(loadOrders)
</script>

<style scoped>
.order-head,
.order-card {
  padding: 24px;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
  flex-wrap: wrap;
}

.order-head p {
  margin: 0 0 8px;
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 700;
}

.order-head h2 {
  margin: 0 0 8px;
}

.order-head span,
.card-top p,
.order-meta p,
.timeline-box p,
.review-box p,
.pay-entry-box p {
  color: var(--app-muted);
}

.card-top,
.card-main,
.card-actions,
.timeline-head,
.timeline-actions,
.review-head,
.pay-summary,
.pay-entry-box {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.card-main {
  margin: 18px 0;
  align-items: center;
}

.cover {
  width: 128px;
  height: 128px;
  border-radius: 20px;
  object-fit: cover;
}

.order-meta {
  flex: 1;
}

.order-meta h3 {
  margin: 0 0 12px;
}

.pending-payment-card {
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.pending-payment-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.pay-entry-box,
.timeline-box,
.review-box {
  margin: 10px 0 20px;
  padding: 16px 18px;
  border-radius: 18px;
}

.pay-entry-box {
  justify-content: flex-end;
  padding: 0;
  margin-top: 4px;
  background: transparent;
  border-radius: 0;
}

.pay-entry-button {
  min-width: 132px;
  min-height: 40px;
  font-size: 14px;
  font-weight: 600;
}

.timeline-box,
.review-box {
  background: #f7fbfa;
}

.review-head {
  margin-bottom: 8px;
}

.card-actions {
  flex-wrap: wrap;
  align-items: center;
}

.mock-trigger {
  color: #64748b;
  font-size: 12px;
}

.pay-dialog {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.pay-summary {
  align-items: center;
}

.address-list {
  display: grid;
  gap: 12px;
}

.address-card {
  margin-right: 0;
  width: 100%;
  min-height: 88px;
}

.address-toolbar,
.address-locate-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.address-locate-row span {
  color: var(--app-muted);
  font-size: 12px;
}

.address-form :deep(.el-select),
.address-form :deep(.el-input) {
  width: 100%;
}

.address-list :deep(.el-radio__input) {
  margin-top: 4px;
}

.address-list :deep(.el-radio__label) {
  display: block;
  width: calc(100% - 28px);
  padding-right: 0;
  white-space: normal;
}

.address-card p,
.address-card strong {
  display: block;
  overflow-wrap: anywhere;
}

.rating-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.rating-card {
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: #fff;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
  cursor: pointer;
}

.rating-card.active {
  border-color: var(--app-primary);
  background: rgba(29, 92, 87, 0.06);
}

.rating-icon {
  width: 24px;
  height: 24px;
}

.paying-box {
  padding: 16px 0 8px;
  text-align: center;
}

.spinner {
  width: 52px;
  height: 52px;
  margin: 0 auto 18px;
  border-radius: 50%;
  border: 4px solid rgba(29, 92, 87, 0.14);
  border-top-color: var(--app-primary);
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .card-main,
  .card-top,
  .timeline-head,
  .pay-summary,
  .pay-entry-box,
  .address-toolbar,
  .address-locate-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .rating-row {
    grid-template-columns: 1fr;
  }

  .pay-entry-button {
    width: 100%;
  }
}
</style>

