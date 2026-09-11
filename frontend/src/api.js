import axios from 'axios'
import { message } from 'antd'

const api = axios.create({ baseURL: '/api', timeout: 60000 })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.message || err.message || '请求失败'
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!location.hash.includes('/login')) {
        message.warning('登录已过期，请重新登录')
        location.hash = '#/login'
      }
    } else if (!err.config?.silent) {
      message.error(msg)
    }
    return Promise.reject(err)
  }
)

export default api
