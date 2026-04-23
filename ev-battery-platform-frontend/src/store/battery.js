import { defineStore } from 'pinia'

const STORAGE_KEY = 'battery_manage_prefs'

const defaultColumns = [
  { key: 'batteryCode', label: '电池编码', visible: true },
  { key: 'sourceType', label: '来源', visible: true },
  { key: 'status', label: '状态', visible: true },
  { key: 'voltage', label: '电压(V)', visible: true },
  { key: 'cycleCount', label: '循环次数', visible: true },
  { key: 'capacityRetentionRate', label: '容量保持率(%)', visible: true },
  { key: 'internalResistanceRatio', label: '内阻比', visible: true },
  { key: 'avgTemperature', label: '平均温度(°C)', visible: true },
  { key: 'latestHealthScore', label: '最新健康分', visible: true },
  { key: 'createdAt', label: '创建时间', visible: true }
]

const loadPrefs = () => {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}')
  } catch (e) {
    return {}
  }
}

export const useBatteryStore = defineStore('battery', {
  state: () => {
    const prefs = loadPrefs()
    return {
      viewMode: prefs.viewMode || 'table',
      columns: prefs.columns || defaultColumns,
      savedFilters: prefs.savedFilters || [],
      lastListQuery: prefs.lastListQuery || null
    }
  },
  actions: {
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        viewMode: this.viewMode,
        columns: this.columns,
        savedFilters: this.savedFilters,
        lastListQuery: this.lastListQuery
      }))
    },
    setViewMode(viewMode) {
      this.viewMode = viewMode
      this.persist()
    },
    setColumns(columns) {
      this.columns = columns
      this.persist()
    },
    saveFilterScheme(name, filters) {
      const existing = this.savedFilters.filter((item) => item.name !== name)
      existing.unshift({ name, filters, savedAt: Date.now() })
      this.savedFilters = existing.slice(0, 10)
      this.persist()
    },
    setLastListQuery(query) {
      this.lastListQuery = query
      this.persist()
    }
  }
})
