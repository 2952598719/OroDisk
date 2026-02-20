import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '../api/user'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  async function login(credentials) {
    const res = await userApi.login(credentials)
    if (res.code === 200) {
      token.value = res.data.token
      userInfo.value = res.data.userInfo
      localStorage.setItem('token', res.data.token)
    }
    return res
  }

  async function register(credentials) {
    return await userApi.register(credentials)
  }

  async function logout() {
    try {
      await userApi.logout()
    } catch (e) {
      // ignore
    }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = await userApi.getCurrentUser()
      if (res.code === 200) {
        userInfo.value = res.data
      }
    } catch (e) {
      // ignore
    }
  }

  function formatBytes(bytes) {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const usedQuotaFormatted = computed(() => {
    return userInfo.value ? formatBytes(userInfo.value.usedQuota) : '0 B'
  })

  const totalQuotaFormatted = computed(() => {
    return userInfo.value ? formatBytes(userInfo.value.totalQuota) : '0 B'
  })

  const quotaPercentage = computed(() => {
    if (!userInfo.value || userInfo.value.totalQuota === 0) return 0
    return Math.round((userInfo.value.usedQuota / userInfo.value.totalQuota) * 100)
  })

  return {
    token,
    userInfo,
    isLoggedIn,
    usedQuotaFormatted,
    totalQuotaFormatted,
    quotaPercentage,
    login,
    register,
    logout,
    fetchUserInfo
  }
})
