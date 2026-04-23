import request from '../utils/request'

export const getTradeTrend = (params) => request.get('/statistics/trade-trend', { params })
export const getHealthDistribution = () => request.get('/statistics/health-distribution')
export const getSourceDistribution = () => request.get('/statistics/source-distribution')
export const getProductCategoryDistribution = () => request.get('/statistics/product-category-distribution')
