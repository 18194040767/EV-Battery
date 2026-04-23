import request from '../utils/request'

export const getTradeProducts = (params) => request.get('/trade/products', { params })
export const getTradeProductDetail = (id) => request.get(`/trade/products/${id}`)
export const getMyTradeProducts = () => request.get('/trade/products/mine')
export const createTradeProduct = (data) => request.post('/trade/products', data)
export const updateTradeProduct = (id, data) => request.put(`/trade/products/${id}`, data)
export const changeTradeProductStatus = (id, data) => request.patch(`/trade/products/${id}/status`, data)
export const saveTradeProductDraft = (data) => request.post('/trade/products/draft', data)
export const getTradeProductDrafts = () => request.get('/trade/products/draft/list')
export const deleteTradeProductDraft = (id) => request.delete(`/trade/products/draft/${id}`)

export const addFavoriteProduct = (productId) => request.post(`/trade/favorites/${productId}`)
export const removeFavoriteProduct = (productId) => request.delete(`/trade/favorites/${productId}`)
export const getFavoriteProducts = () => request.get('/trade/favorites')
export const getFavoriteStatus = (productId) => request.get(`/trade/favorites/${productId}/status`)

export const getCartList = () => request.get('/trade/cart')
export const addCartItem = (data) => request.post('/trade/cart', data)
export const updateCartItem = (id, data) => request.put(`/trade/cart/${id}`, data)
export const deleteCartItem = (id) => request.delete(`/trade/cart/${id}`)
export const clearInvalidCartItems = () => request.delete('/trade/cart')

export const getAddresses = () => request.get('/trade/addresses')
export const saveAddress = (data) => request.post('/trade/addresses', data)

export const createTradeOrder = (data) => request.post('/trade/orders/confirm', data)
export const getTradeOrders = (params) => request.get('/trade/orders', { params })
export const getTradeOrderDetail = (id) => request.get(`/trade/orders/${id}`)
export const updateTradeOrderAddress = (id, data) => request.put(`/trade/orders/${id}/address`, data)
export const payTradeOrder = (id) => request.post(`/trade/orders/${id}/pay`)
export const cancelTradeOrder = (id) => request.post(`/trade/orders/${id}/cancel`)
export const shipTradeOrder = (id, data) => request.post(`/trade/orders/${id}/ship`, data)
export const confirmTradeOrder = (id) => request.post(`/trade/orders/${id}/confirm`)
export const deleteTradeOrder = (id) => request.delete(`/trade/orders/${id}`)

export const createTradeReview = (data) => request.post('/trade/reviews', data)
export const getProductReviews = (productId) => request.get(`/trade/reviews/product/${productId}`)
export const getSellerReviewSummary = (sellerId) => request.get(`/trade/reviews/seller/${sellerId}/summary`)
export const replyTradeReview = (id, data) => request.post(`/trade/reviews/${id}/reply`, data)

export const sendTradeMessage = (data) => request.post('/trade/messages/send', data)
export const getTradeMessageSessions = () => request.get('/trade/messages/sessions')
export const getTradeMessageHistory = (params) => request.get('/trade/messages/history', { params })

export const getSellerHome = (sellerId) => request.get(`/trade/sellers/${sellerId}`)
export const getTradeProfile = (userId) => request.get(userId ? `/trade/profile/${userId}` : '/trade/profile')
export const updateTradeProfile = (data) => request.put('/trade/profile', data)

// legacy compatibility
export const publishDemand = (data) => request.post('/trade/demand/publish', data)
export const getDemandList = (params) => request.get('/trade/demand/list', { params })
