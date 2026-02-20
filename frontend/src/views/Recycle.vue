<template>
  <div class="recycle-page">
    <header class="header">
      <div class="header-left">
        <router-link to="/" class="back-link">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Files
        </router-link>
      </div>
      <div class="header-right">
        <h1>Recycle Bin</h1>
      </div>
    </header>
    
    <main class="main-content">
      <div class="toolbar">
        <div class="toolbar-left">
          <button 
            class="btn btn-danger" 
            @click="handleClearAll"
            :disabled="files.length === 0"
          >
            Empty Recycle Bin
          </button>
        </div>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>
      
      <div v-else-if="files.length === 0" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
        </svg>
        <p>Recycle bin is empty</p>
      </div>
      
      <div v-else class="file-list">
        <div class="file-header">
          <div class="file-col name">Name</div>
          <div class="file-col size">Size</div>
          <div class="file-col date">Deleted</div>
          <div class="file-col actions">Actions</div>
        </div>
        
        <div v-for="file in files" :key="file.fileId" class="file-row">
          <div class="file-col name">
            <span class="file-icon" :class="getFileIconClass(file)">
              <svg v-if="file.fileType === 0" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'image'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'video'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'audio'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19V6l12-3v13M9 19c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zm12-3c0 1.105-1.343 2-3 2s-3-.895-3-2 1.343-2 3-2 3 .895 3 2zM9 10l12-3" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'pdf'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'code'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'archive'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4" />
              </svg>
              <svg v-else-if="getFileType(file.fileName) === 'document'" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
            </span>
            <span class="file-name">{{ file.fileName }}</span>
          </div>
          <div class="file-col size">
            {{ file.fileType === 0 ? '-' : file.fileSizeFormat }}
          </div>
          <div class="file-col date">
            {{ formatDate(file.updatedTime) }}
          </div>
          <div class="file-col actions">
            <button class="btn btn-small btn-success" @click="handleRestore(file)">
              Restore
            </button>
            <button class="btn btn-small btn-danger" @click="handleDeletePermanently(file)">
              Delete Permanently
            </button>
          </div>
        </div>
      </div>
      
      <div v-if="hasMore" class="load-more">
        <button class="btn btn-secondary" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? 'Loading...' : 'Load More' }}
        </button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { fileApi } from '../api/file'

const router = useRouter()
const userStore = useUserStore()

const files = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const lastFileId = ref(null)
const hasMore = ref(false)

onMounted(async () => {
  await loadFiles()
})

async function loadFiles(append = false) {
  if (!append) {
    loading.value = true
    files.value = []
    lastFileId.value = null
  } else {
    loadingMore.value = true
  }
  
  try {
    const res = await fileApi.listRecycle(lastFileId.value, 20)
    if (res.code === 200) {
      if (append) {
        files.value = [...files.value, ...res.data.files]
      } else {
        files.value = res.data.files
      }
      lastFileId.value = res.data.lastFileId
      hasMore.value = res.data.hasMore
    }
  } catch (e) {
    console.error('Failed to load recycle:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  await loadFiles(true)
}

async function handleRestore(file) {
  try {
    const res = await fileApi.restoreFile(file.fileId)
    if (res.code === 200) {
      files.value = files.value.filter(f => f.fileId !== file.fileId)
      await userStore.fetchUserInfo()
    }
  } catch (e) {
    alert(e.message || 'Restore failed')
  }
}

async function handleDeletePermanently(file) {
  if (!confirm(`Are you sure you want to permanently delete "${file.fileName}"? This cannot be undone.`)) return
  
  try {
    const res = await fileApi.deletePermanently(file.fileId)
    if (res.code === 200) {
      files.value = files.value.filter(f => f.fileId !== file.fileId)
      await userStore.fetchUserInfo()
    }
  } catch (e) {
    alert(e.message || 'Delete failed')
  }
}

async function handleClearAll() {
  if (!confirm('Are you sure you want to empty the recycle bin? This cannot be undone.')) return
  
  try {
    const res = await fileApi.clearRecycle()
    if (res.code === 200) {
      files.value = []
      await userStore.fetchUserInfo()
    }
  } catch (e) {
    alert(e.message || 'Clear failed')
  }
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function getFileType(fileName) {
  if (!fileName) return 'file'
  const ext = fileName.split('.').pop().toLowerCase()
  
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico', 'tiff']
  const videoExts = ['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv', 'webm', 'm4v']
  const audioExts = ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma', 'm4a']
  const pdfExts = ['pdf']
  const codeExts = ['js', 'ts', 'jsx', 'tsx', 'vue', 'html', 'css', 'scss', 'sass', 'less', 'json', 'xml', 'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'cs', 'php', 'rb', 'swift', 'kt', 'scala', 'sh', 'bat', 'sql']
  const archiveExts = ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz']
  const documentExts = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'md', 'rtf', 'odt', 'ods', 'odp']
  
  if (imageExts.includes(ext)) return 'image'
  if (videoExts.includes(ext)) return 'video'
  if (audioExts.includes(ext)) return 'audio'
  if (pdfExts.includes(ext)) return 'pdf'
  if (codeExts.includes(ext)) return 'code'
  if (archiveExts.includes(ext)) return 'archive'
  if (documentExts.includes(ext)) return 'document'
  
  return 'file'
}

function getFileIconClass(file) {
  if (file.fileType === 0) return 'folder'
  return 'file file-' + getFileType(file.fileName)
}
</script>

<style scoped>
.recycle-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  background: white;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header h1 {
  font-size: 20px;
  font-weight: 600;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.back-link:hover {
  text-decoration: underline;
}

.back-link svg {
  width: 20px;
  height: 20px;
}

.main-content {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.toolbar {
  margin-bottom: 20px;
}

.file-list {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.file-header {
  display: flex;
  padding: 12px 16px;
  background: #f8f9fa;
  font-weight: 600;
  font-size: 13px;
  color: #666;
  border-bottom: 1px solid #eee;
}

.file-row {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.2s;
}

.file-row:hover {
  background: #f8f9fa;
}

.file-row:last-child {
  border-bottom: none;
}

.file-col {
  display: flex;
  align-items: center;
}

.file-col.name {
  flex: 1;
  min-width: 200px;
}

.file-col.size {
  width: 100px;
  color: #666;
  font-size: 14px;
}

.file-col.date {
  width: 160px;
  color: #666;
  font-size: 14px;
}

.file-col.actions {
  width: 240px;
  justify-content: flex-end;
  gap: 8px;
}

.file-icon {
  width: 24px;
  height: 24px;
  margin-right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-icon.folder svg {
  color: #f5a623;
}

.file-icon.file svg {
  color: #667eea;
}

.file-icon.file-image svg {
  color: #10b981;
}

.file-icon.file-video svg {
  color: #ef4444;
}

.file-icon.file-audio svg {
  color: #8b5cf6;
}

.file-icon.file-pdf svg {
  color: #dc2626;
}

.file-icon.file-code svg {
  color: #0ea5e9;
}

.file-icon.file-archive svg {
  color: #f59e0b;
}

.file-icon.file-document svg {
  color: #3b82f6;
}

.file-name {
  font-size: 14px;
  color: #333;
}

.load-more {
  text-align: center;
  padding: 20px;
}

.empty-state {
  background: white;
  border-radius: 12px;
  padding: 60px 20px;
  text-align: center;
}

.empty-state svg {
  width: 64px;
  height: 64px;
  color: #ddd;
  margin-bottom: 16px;
}

.empty-state p {
  color: #666;
}
</style>
