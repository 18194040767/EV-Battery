<template>
  <div class="logistics-shell">
    <div class="map-stage" :style="{ height: `${mapHeight}px` }">
      <div ref="mapContainer" class="map-container"></div>

      <div class="map-meta">
        <strong>{{ originLabel }} → {{ destinationLabel }}</strong>
        <span>{{ currentCheckpointLabel }}</span>
      </div>

      <div class="control-bar">
        <button class="icon-button" :disabled="isPlaying || !path.length" @click="play" aria-label="播放">
          <el-icon><VideoPlay /></el-icon>
        </button>
        <button class="icon-button" :disabled="!isPlaying" @click="pause" aria-label="暂停">
          <el-icon><VideoPause /></el-icon>
        </button>
        <button class="icon-button" :disabled="!path.length" @click="reset" aria-label="重置">
          <el-icon><RefreshRight /></el-icon>
        </button>
        <el-select v-model="speed" size="small" class="speed-select" @change="handleSpeedChange">
          <el-option label="0.5x" :value="0.5" />
          <el-option label="1x" :value="1" />
          <el-option label="2x" :value="2" />
        </el-select>
      </div>
    </div>

    <section class="info-sheet" :style="{ height: `${sheetHeight}px` }">
      <button class="sheet-handle" type="button" @click="toggleSheet" />
      <div v-if="detail" class="sheet-head">
        <div>
          <strong>运单号：{{ detail.waybillNo || waybillNo || '-' }}</strong>
          <p>发货地：{{ originLabel }} · 收货地：{{ destinationLabel }}</p>
          <p>当前位置：{{ currentCheckpointLabel }} · {{ etaText }}</p>
        </div>
        <span class="status-badge">{{ currentStatus }}</span>
      </div>

      <el-timeline v-if="detail" class="timeline">
        <el-timeline-item
          v-for="(node, index) in timelineNodes"
          :key="`${node.time}-${node.status}-${index}`"
          :timestamp="node.time"
          :type="node.isCurrent ? 'primary' : 'info'"
          :hollow="!node.isCurrent"
        >
          <strong>{{ node.status }}</strong>
          <p v-if="node.description">{{ node.description }}</p>
        </el-timeline-item>
      </el-timeline>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { RefreshRight, VideoPause, VideoPlay } from '@element-plus/icons-vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import { getTrackData } from '../api/logistics'

const props = defineProps({
  orderId: { type: [String, Number], default: '' },
  waybillNo: { type: String, default: '' },
  autoPlay: { type: Boolean, default: false },
  refreshToken: { type: Number, default: 0 }
})

const emit = defineEmits(['loaded'])

const mapContainer = ref(null)
const map = shallowRef(null)
const movingMarker = shallowRef(null)
const polyline = shallowRef(null)
const startMarker = shallowRef(null)
const endMarker = shallowRef(null)
const amapRef = shallowRef(null)

const detail = ref(null)
const timelineNodes = ref([])
const path = ref([])
const isPlaying = ref(false)
const speed = ref(1)
const currentIndex = ref(0)
const sheetExpanded = ref(false)

let progressTimer = null
let moveEndHandler = null

const AMAP_KEY = '63a8c4d8fdae7b7c86a14bed80a94ae3'
const AMAP_SECURITY = '52ac8d269d0daa3f75ee8c107970f9ed'

const mapHeight = computed(() => (sheetExpanded.value ? 420 : 560))
const sheetHeight = computed(() => (sheetExpanded.value ? 320 : 164))
const fitPaddingBottom = computed(() => (sheetExpanded.value ? 80 : 40))

const currentStatus = computed(() => timelineNodes.value[currentIndex.value]?.status || detail.value?.currentStatus || '待发货')
const originLabel = computed(() => detail.value?.route?.origin?.city || '发货地')
const destinationLabel = computed(() => detail.value?.route?.destination?.city || '收货地')
const currentCheckpointLabel = computed(() => detail.value?.currentCheckpoint?.city || destinationLabel.value || '-')
const etaText = computed(() => {
  const days = Number(detail.value?.etaDays || 0)
  return days > 0 ? `预计 ${days} 天后到达` : '预计今日送达或已送达'
})

const syncCurrentNode = (index) => {
  currentIndex.value = Math.min(Math.max(index, 0), Math.max(timelineNodes.value.length - 1, 0))
  timelineNodes.value = timelineNodes.value.map((node, idx) => ({
    ...node,
    isCurrent: idx === currentIndex.value
  }))
}

const fitView = () => {
  if (!map.value || !polyline.value || !startMarker.value || !endMarker.value) return
  map.value.setFitView([polyline.value, startMarker.value, endMarker.value], false, [50, 50, fitPaddingBottom.value, 50])
}

const initMap = async () => {
  if (!mapContainer.value || !path.value.length) return
  window._AMapSecurityConfig = { securityJsCode: AMAP_SECURITY }
  const AMap = await AMapLoader.load({
    key: AMAP_KEY,
    version: '2.0',
    plugins: ['AMap.MoveAnimation']
  })
  amapRef.value = AMap

  if (!map.value) {
    map.value = new AMap.Map(mapContainer.value, {
      zoom: 6,
      center: [path.value[0].lng, path.value[0].lat],
      viewMode: '3D'
    })
  }

  drawTrack()
}

const clearMapOverlays = () => {
  ;[polyline.value, startMarker.value, endMarker.value, movingMarker.value].forEach((overlay) => {
    if (overlay) overlay.setMap?.(null)
  })
  polyline.value = null
  startMarker.value = null
  endMarker.value = null
  movingMarker.value = null
}

const drawTrack = () => {
  const AMap = amapRef.value
  if (!AMap || !map.value || !path.value.length) return

  clearMapOverlays()

  const linePath = path.value.map((point) => [point.lng, point.lat])
  polyline.value = new AMap.Polyline({
    path: linePath,
    strokeColor: '#2f7cff',
    strokeWeight: 6,
    lineJoin: 'round',
    lineCap: 'round',
    showDir: true
  })

  startMarker.value = new AMap.Marker({
    position: linePath[0],
    icon: 'https://webapi.amap.com/theme/v1.3/markers/n/start.png',
    anchor: 'bottom-center'
  })

  endMarker.value = new AMap.Marker({
    position: linePath[linePath.length - 1],
    icon: 'https://webapi.amap.com/theme/v1.3/markers/n/end.png',
    anchor: 'bottom-center'
  })

  movingMarker.value = new AMap.Marker({
    position: linePath[0],
    map: map.value,
    autoRotation: true,
    offset: new AMap.Pixel(-16, -16),
    content: '<div class="vehicle-marker">🚚</div>'
  })

  map.value.add([polyline.value, startMarker.value, endMarker.value, movingMarker.value])
  fitView()
}

const stopProgressTimer = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
}

const startProgressTimer = () => {
  stopProgressTimer()
  if (timelineNodes.value.length <= 1) return
  const duration = 10000 / Number(speed.value || 1)
  const step = Math.max(1200, Math.floor(duration / timelineNodes.value.length))
  progressTimer = setInterval(() => {
    if (!isPlaying.value) return
    if (currentIndex.value >= timelineNodes.value.length - 1) {
      stopProgressTimer()
      return
    }
    syncCurrentNode(currentIndex.value + 1)
  }, step)
}

const bindMoveEnd = () => {
  if (!movingMarker.value) return
  if (moveEndHandler) movingMarker.value.off?.('moveend', moveEndHandler)
  moveEndHandler = () => {
    isPlaying.value = false
    syncCurrentNode(Math.max(timelineNodes.value.length - 1, 0))
    stopProgressTimer()
  }
  movingMarker.value.on('moveend', moveEndHandler)
}

const play = () => {
  if (!movingMarker.value || !path.value.length) return
  pause()
  isPlaying.value = true
  const movePath = path.value.map((point) => [point.lng, point.lat])
  bindMoveEnd()
  movingMarker.value.moveAlong(movePath, {
    duration: 10000 / Number(speed.value || 1),
    autoRotation: true
  })
  startProgressTimer()
}

const pause = () => {
  movingMarker.value?.stopMove?.()
  isPlaying.value = false
  stopProgressTimer()
}

const reset = () => {
  pause()
  if (!movingMarker.value || !path.value.length) return
  movingMarker.value.setPosition([path.value[0].lng, path.value[0].lat])
  syncCurrentNode(detail.value?.currentIndex || 0)
  fitView()
}

const handleSpeedChange = () => {
  if (isPlaying.value) play()
}

const toggleSheet = async () => {
  sheetExpanded.value = !sheetExpanded.value
  await nextTick()
  map.value?.resize?.()
  fitView()
}

const fetchData = async () => {
  if (!props.orderId) return
  const res = await getTrackData(props.orderId)
  detail.value = res
  path.value = res.path || []
  timelineNodes.value = res.nodes || []
  syncCurrentNode(res.currentIndex || 0)
  await nextTick()
  await initMap()
  emit('loaded', res)
  if (props.autoPlay) play()
}

watch(() => props.orderId, async () => {
  pause()
  await fetchData()
}, { immediate: false })

watch(() => props.refreshToken, async () => {
  if (!props.orderId) return
  pause()
  await fetchData()
}, { immediate: false })

watch(sheetExpanded, async () => {
  await nextTick()
  map.value?.resize?.()
  fitView()
})

onMounted(fetchData)

onUnmounted(() => {
  pause()
  clearMapOverlays()
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
})
</script>

<style scoped>
.logistics-shell {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.map-stage {
  position: relative;
  transition: height 0.24s ease;
}

.map-container {
  width: 100%;
  height: 100%;
  border-radius: 22px;
  overflow: hidden;
  background: linear-gradient(180deg, #eef5ff, #e6efff);
}

.map-meta {
  position: absolute;
  top: 16px;
  left: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(10px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
}

.map-meta strong {
  color: #1f2937;
}

.map-meta span {
  color: #64748b;
  font-size: 13px;
}

.control-bar {
  position: absolute;
  right: 18px;
  bottom: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.64);
  backdrop-filter: blur(10px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
}

.icon-button {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: #2f7cff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.icon-button:hover:not(:disabled) {
  background: rgba(47, 124, 255, 0.12);
  transform: translateY(-1px);
}

.icon-button:disabled {
  opacity: 0.38;
  cursor: not-allowed;
}

.speed-select {
  width: 84px;
}

.info-sheet {
  position: relative;
  overflow: auto;
  padding: 22px 18px 18px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid rgba(210, 222, 242, 0.9);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
  transition: height 0.24s ease;
}

.sheet-handle {
  position: sticky;
  top: 0;
  display: block;
  width: 56px;
  height: 6px;
  margin: 0 auto 14px;
  border: none;
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.28);
  cursor: pointer;
}

.sheet-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.sheet-head strong {
  display: block;
  color: #1f2937;
}

.sheet-head p,
.timeline p {
  margin: 6px 0 0;
  color: #64748b;
}

.timeline {
  padding-right: 4px;
}

.status-badge {
  align-self: flex-start;
  background: #e6f4ff;
  color: #4096ff;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  white-space: nowrap;
}

:deep(.vehicle-marker) {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(47, 124, 255, 0.12);
  border: 1px solid rgba(47, 124, 255, 0.24);
  font-size: 18px;
}

@media (max-width: 768px) {
  .sheet-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .control-bar {
    right: 12px;
    left: 12px;
    justify-content: flex-end;
  }
}
</style>
