// 本地订单记录工具:保存最近提交/查看的订单(订单号 + 访问令牌)
// 仅存于本浏览器;用于"我的订单"快速找回,不依赖后端用户体系
const KEY = 'wemove_orders'
const MAX = 50

export function loadOrders() {
  try {
    const raw = localStorage.getItem(KEY)
    const list = raw ? JSON.parse(raw) : []
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
}

export function saveOrder(record) {
  try {
    const list = loadOrders()
    const idx = list.findIndex((o) => o.number === record.number)
    const item = {
      number: record.number,
      token: record.token,
      name: record.name || '',
      total_cents: record.total_cents || 0,
      status: record.status || '',
      created_at: record.created_at || new Date().toISOString(),
    }
    if (idx >= 0) list.splice(idx, 1, item)
    else list.unshift(item)
    localStorage.setItem(KEY, JSON.stringify(list.slice(0, MAX)))
  } catch {
    /* localStorage 不可用时静默忽略 */
  }
}

export function removeOrder(number) {
  try {
    const list = loadOrders().filter((o) => o.number !== number)
    localStorage.setItem(KEY, JSON.stringify(list))
  } catch {
    /* ignore */
  }
}
