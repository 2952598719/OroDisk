import api from './index'

export const userApi = {
  register(data) {
    return api.post('/user/register', data)
  },

  login(data) {
    return api.post('/user/login', data)
  },

  logout() {
    return api.post('/user/logout')
  },

  getCurrentUser() {
    return api.get('/user/info')
  },

  getUserById(userId) {
    return api.get(`/user/info/${userId}`)
  },

  updatePassword(data) {
    return api.put('/user/password', data)
  }
}
