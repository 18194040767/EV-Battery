const AMAP_KEY = '63a8c4d8fdae7b7c86a14bed80a94ae3'
const AMAP_SECURITY_CODE = '52ac8d269d0daa3f75ee8c107970f9ed'

let loaderPromise = null

export const loadAmap = () => {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (loaderPromise) return loaderPromise

  loaderPromise = new Promise((resolve, reject) => {
    window._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE
    }

    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${AMAP_KEY}`
    script.async = true
    script.onload = () => {
      if (window.AMap) {
        resolve(window.AMap)
        return
      }
      reject(new Error('高德地图加载失败'))
    }
    script.onerror = () => reject(new Error('高德地图脚本加载失败'))
    document.head.appendChild(script)
  })

  return loaderPromise
}
