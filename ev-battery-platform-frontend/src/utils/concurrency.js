export const debounce = (fn, wait = 300) => {
  let timer = null
  return (...args) => {
    window.clearTimeout(timer)
    timer = window.setTimeout(() => fn(...args), wait)
  }
}

export const throttle = (fn, wait = 800) => {
  let lastRun = 0
  let timer = null
  return (...args) => {
    const now = Date.now()
    const remaining = wait - (now - lastRun)
    if (remaining <= 0) {
      window.clearTimeout(timer)
      timer = null
      lastRun = now
      fn(...args)
      return
    }
    if (!timer) {
      timer = window.setTimeout(() => {
        lastRun = Date.now()
        timer = null
        fn(...args)
      }, remaining)
    }
  }
}
