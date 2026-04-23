import axios from 'axios'
import request from '../utils/request'

const authHeaders = () => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

export const generateContract = (orderId) =>
  request.post('/contract/generate', null, { params: { orderId } })

export const listContracts = (params) => request.get('/contract/list', { params })

export const verifyContractById = (id) => request.get(`/contract/verify/${id}`)

export const verifyContract = ({ contractNo, file }) => {
  const formData = new FormData()
  formData.append('contractNo', contractNo)
  if (file) formData.append('file', file)
  return axios.post('/api/contract/verify', formData, {
    headers: {
      ...authHeaders(),
      'Content-Type': 'multipart/form-data'
    }
  }).then((res) => res.data)
}

export const downloadContract = (id) =>
  axios.get(`/api/contract/${id}/download`, {
    responseType: 'blob',
    headers: authHeaders()
  })

export const previewContract = (id) =>
  axios.get(`/api/contract/preview/${id}`, {
    responseType: 'blob',
    headers: authHeaders()
  })
