import api from './index'

const publicApi = {
  async get(url) {
    const response = await fetch(url)
    const data = await response.json()
    return data
  },

  async post(url, body) {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    })
    const data = await response.json()
    return data
  }
}

export const shareApi = {
  createShare(data) {
    return api.post('/share/create', data)
  },

  getShareInfo(shareCode) {
    return publicApi.get(`/api/share/info/${shareCode}`)
  },

  verifyPassword(data) {
    return publicApi.post('/api/share/verify', data)
  },

  downloadShare(shareCode, password = '') {
    let url = `/api/share/download/${shareCode}`
    if (password) {
      url += `?password=${encodeURIComponent(password)}`
    }
    return url
  },

  previewShare(shareCode, password = '') {
    let url = `/api/share/preview/${shareCode}`
    if (password) {
      url += `?password=${encodeURIComponent(password)}`
    }
    return url
  },

  async downloadShareFile(shareCode, password, fileName) {
    const url = this.downloadShare(shareCode, password)
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error('Download failed')
    }
    const blob = await response.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(blobUrl)
  },

  async previewShareAsBlob(shareCode, password) {
    const url = this.previewShare(shareCode, password)
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error('Preview failed')
    }
    return await response.blob()
  },

  async previewShareAsText(shareCode, password) {
    const url = this.previewShare(shareCode, password)
    const response = await fetch(url)
    if (!response.ok) {
      throw new Error('Preview failed')
    }
    return await response.text()
  },

  listMyShares() {
    return api.get('/share/list')
  },

  cancelShare(shareId) {
    return api.delete(`/share/cancel/${shareId}`)
  }
}
