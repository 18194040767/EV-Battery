export const normalizeId = (value) => {
  if (value === null || value === undefined || value === '') return ''
  return String(value)
}

export const normalizeIdList = (values) =>
  (Array.isArray(values) ? values : [])
    .map((item) => normalizeId(item))
    .filter(Boolean)
