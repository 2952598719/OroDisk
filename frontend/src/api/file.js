import api from './index'

function getToken() {
  return localStorage.getItem('token') || ''
}

export const fileApi = {
  listFiles(parentId = 0, lastFileId = null, pageSize = null) {
    const params = { parentId }
    if (lastFileId) params.lastFileId = lastFileId
    if (pageSize) params.pageSize = pageSize
    return api.get('/file/list', { params })
  },

  createFolder(data) {
    return api.post('/file/folder', data)
  },

  checkFile(identifier, fileName, totalSize, parentId = 0) {
    return api.get('/file/check', {
      params: { identifier, fileName, totalSize, parentId }
    })
  },

  uploadChunk(formData, onProgress) {
    return api.post('/file/chunk', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress
    })
  },

  mergeFile(data) {
    return api.post('/file/merge', data)
  },

  instantUpload(data) {
    return api.post('/file/instant', data)
  },

  deleteFile(fileId) {
    return api.delete(`/file/${fileId}`)
  },

  deletePermanently(fileId) {
    return api.delete(`/file/permanent/${fileId}`)
  },

  listRecycle(lastFileId = null, pageSize = null) {
    const params = {}
    if (lastFileId) params.lastFileId = lastFileId
    if (pageSize) params.pageSize = pageSize
    return api.get('/file/recycle', { params })
  },

  restoreFile(fileId) {
    return api.put(`/file/restore/${fileId}`)
  },

  clearRecycle() {
    return api.delete('/file/recycle/clear')
  },

  renameFile(data) {
    return api.put('/file/rename', data)
  },

  moveFile(data) {
    return api.put('/file/move', data)
  },

  async downloadFile(fileId, fileName) {
    const response = await fetch(`/api/file/download/${fileId}`, {
      method: 'GET',
      headers: {
        'Authorization': getToken()
      }
    })

    if (!response.ok) {
      throw new Error(`Download failed: ${response.status}`)
    }

    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  },

  async previewFile(fileId) {
    const response = await fetch(`/api/file/preview/${fileId}`, {
      method: 'GET',
      headers: {
        'Authorization': getToken()
      }
    })

    if (!response.ok) {
      throw new Error(`Preview failed: ${response.status}`)
    }

    return response
  },

  async previewFileAsText(fileId) {
    const response = await this.previewFile(fileId)
    return await response.text()
  },

  async previewFileAsBlob(fileId) {
    const response = await this.previewFile(fileId)
    return await response.blob()
  },

  getFileInfo(fileId) {
    return api.get(`/file/info/${fileId}`)
  },

  deleteFolder(folderId) {
    return api.delete(`/file/folder/${folderId}`)
  },

  getFilePath(fileId) {
    return api.get(`/file/path/${fileId}`)
  }
}
