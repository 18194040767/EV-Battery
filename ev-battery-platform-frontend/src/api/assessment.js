import request from '../utils/request'

export const trigger = (batteryId, useML) =>
  request.post('/assessment/trigger', null, { params: { batteryId, useML } })

export const getLatest = (batteryId) =>
  request.get(`/assessment/battery/${batteryId}/latest`)

export const getHistory = (batteryId) =>
  request.get(`/assessment/battery/${batteryId}/history`)

export const getDetail = (id) =>
  request.get(`/assessment/${id}`)

export const triggerAssessment = trigger
export const getAssessmentHistory = (params) => getHistory(params?.batteryId)
export const getAssessmentReport = getDetail

export const triggerBatchAssessment = (batteryIdsOrPayload, useML = false) => {
  const payload = Array.isArray(batteryIdsOrPayload)
    ? { batteryIds: batteryIdsOrPayload, useML }
    : {
        batteryIds: batteryIdsOrPayload?.batteryIds || [],
        useML: batteryIdsOrPayload?.useML ?? useML
      }
  return request.post('/assessment/batch/trigger', payload)
}

export const getBatchAssessmentTask = (taskId) =>
  request.get(`/assessment/batch/task/${taskId}`)

export const quickRunDatasetAssessment = (limit = 10, useML = true) =>
  request.post('/assessment/dataset/quick-run', null, { params: { limit, useML } })
