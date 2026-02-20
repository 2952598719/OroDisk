import axios from 'axios'
import { useUserStore } from '../stores/user'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 300000
})

api.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = userStore.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      return Promise.reject(error.response.data)
    }
    return Promise.reject({ message: 'Network error' })
  }
)

export default api
