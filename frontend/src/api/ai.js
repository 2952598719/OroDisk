import api from './index'

export const aiApi = {
  createConversation(data) {
    return api.post('/ai/conversation', data)
  },

  listConversations() {
    return api.get('/ai/conversations')
  },

  getConversation(conversationId) {
    return api.get(`/ai/conversation/${conversationId}`)
  },

  deleteConversation(conversationId) {
    return api.delete(`/ai/conversation/${conversationId}`)
  }
}
