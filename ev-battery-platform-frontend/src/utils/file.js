export const fileToDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })

const PDF_MAGIC = '%PDF-'

const toBlob = (data, fallbackType = 'application/pdf') => {
  if (data instanceof Blob) return data
  return new Blob([data], { type: fallbackType })
}

const responseContentType = (response) => {
  const headers = response?.headers || {}
  return String(headers['content-type'] || headers['Content-Type'] || '').toLowerCase()
}

const extractErrorMessage = (text) => {
  if (!text) return 'PDF 下载失败，服务器未返回有效文件'
  try {
    const data = JSON.parse(text)
    return data?.message || data?.msg || data?.error || text
  } catch (error) {
    return text.slice(0, 180)
  }
}

const assertPdfBlob = async (response) => {
  const blob = toBlob(response?.data)
  const contentType = responseContentType(response)
  const head = await blob.slice(0, PDF_MAGIC.length).text()
  if (contentType.includes('application/pdf') && head === PDF_MAGIC) {
    return blob
  }

  const text = await blob.text().catch(() => '')
  throw new Error(extractErrorMessage(text))
}

export const downloadPdfResponse = async (response, filename) => {
  const blob = await assertPdfBlob(response)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || 'document.pdf'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export const createPdfObjectUrl = async (response) => {
  const blob = await assertPdfBlob(response)
  return URL.createObjectURL(blob)
}

export const getDownloadErrorMessage = async (error, fallback = 'PDF 下载失败，请稍后重试') => {
  const data = error?.response?.data
  if (data instanceof Blob) {
    const text = await data.text().catch(() => '')
    return extractErrorMessage(text) || fallback
  }
  if (typeof data === 'string') {
    return extractErrorMessage(data) || fallback
  }
  return error?.message || fallback
}
