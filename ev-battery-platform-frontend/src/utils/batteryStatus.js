const STATUS_LABELS = {
  PENDING_ASSESSMENT: '待评估',
  ASSESSED: '已评估',
  TRADED: '已交易',
  OFFLINE: '已下架',
  DRAFT: '草稿'
}

export const formatBatteryStatus = (value) => STATUS_LABELS[value] || value || '-'
