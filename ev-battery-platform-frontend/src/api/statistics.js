import request from '../utils/request'

export const getTradeTrend = (params) => request.get('/statistics/trade-trend', { params })
export const getHealthDistribution = (params) => request.get('/statistics/health-distribution', { params })
export const getSourceDistribution = () => request.get('/statistics/source-distribution')
export const getProductCategoryDistribution = (params) => request.get('/statistics/product-category-distribution', { params })
