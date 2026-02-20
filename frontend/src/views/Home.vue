<template>
  <div class="home-page">
    <header class="header">
      <div class="header-left">
        <h1 class="logo">OroDisk</h1>
      </div>
      <div class="header-right">
        <div class="user-info">
          <span class="username">{{ userStore.userInfo?.username }}</span>
          <span class="quota">{{ userStore.usedQuotaFormatted }} / {{ userStore.totalQuotaFormatted }}</span>
          <div class="quota-bar">
            <div class="quota-used" :style="{ width: userStore.quotaPercentage + '%' }"></div>
          </div>
        </div>
        <button class="btn btn-secondary btn-small" @click="handleLogout">Logout</button>
      </div>
    </header>
    
    <main class="main-content">
      <div class="toolbar">
        <div class="toolbar-left">
          <button class="btn btn-primary" @click="showUploadModal = true">
            Upload
          </button>
          <button class="btn btn-secondary" @click="showNewFolderModal = true">
            New Folder
          </button>
        </div>
        <div class="toolbar-right">
          <button class="btn btn-icon-only" @click="refreshFiles" :disabled="loading" title="Refresh">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
          <router-link to="/recycle" class="btn btn-icon-only" title="Recycle Bin">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </router-link>
        </div>
      </div>
      
      <div class="breadcrumb">
        <span 
          class="breadcrumb-item" 
          :class="{ active: currentParentId === 0 }"
          @click="navigateTo(0)"
        >
          Root
        </span>
        <template v-for="(folder, index) in breadcrumbs" :key="folder.fileId">
          <span class="breadcrumb-separator">/</span>
          <span 
            class="breadcrumb-item"
            :class="{ active: currentParentId === folder.fileId }"
            @click="navigateTo(folder.fileId)"
          >
            {{ folder.fileName }}
          </span>
        </template>
      </div>
      
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
      </div>
      
      <div v-else-if="files.length === 0" class="empty-state">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
        </svg>
        <p>This folder is empty</p>
        <p class="hint">Upload files or create a new folder to get started</p>
      </div>
      
      <div v-else class="file-list">
        <div class="file-header">
          <div class="file-col name">Name</div>
          <div class="file-col size">Size</div>
          <div class="file-col date">Modified</div>
          <div class="file-col actions">Actions</div>
        </div>
        
        <div 
          v-for="file in files" 
          :key="file.fileId" 
          class="file-row"
          :class="{ 'is-folder': file.fileType === 0, 'is-previewable': file.fileType === 1 && isPreviewable(file.fileName) }"
          @click="handleClick(file)"
        >
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
          <div class="file-col actions" @click.stop>
            <button v-if="file.fileType === 1" class="btn btn-small btn-secondary" @click="handleDownload(file)">
              Download
            </button>
            <button class="btn btn-small btn-icon" @click.stop="toggleContextMenu($event, file)">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z" />
              </svg>
            </button>
          </div>
        </div>
      </div>
      
      <div 
        v-if="contextMenu.visible" 
        class="context-menu" 
        :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      >
        <div class="context-menu-item" @click="handleContextAction('preview')" v-if="contextMenu.file?.fileType === 1 && isPreviewable(contextMenu.file?.fileName)">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
          </svg>
          Preview
        </div>
        <div class="context-menu-item" @click="handleContextAction('rename')">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
          </svg>
          Rename
        </div>
        <div class="context-menu-item" @click="handleContextAction('move')">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4" />
          </svg>
          Move
        </div>
        <div class="context-menu-item" @click="handleContextAction('download')" v-if="contextMenu.file?.fileType === 1">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Download
        </div>
        <div class="context-menu-item" @click="handleContextAction('share')" v-if="contextMenu.file?.fileType === 1">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
          </svg>
          Share
        </div>
        <div class="context-menu-divider"></div>
        <div class="context-menu-item danger" @click="handleContextAction('delete')">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
          Delete
        </div>
      </div>
      
      <div v-if="hasMore" class="load-more">
        <button class="btn btn-secondary" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? 'Loading...' : 'Load More' }}
        </button>
      </div>
    </main>
    
    <UploadModal 
      v-if="showUploadModal" 
      :parent-id="currentParentId"
      @close="showUploadModal = false"
      @uploaded="handleUploaded"
    />
    
    <NewFolderModal 
      v-if="showNewFolderModal" 
      :parent-id="currentParentId"
      @close="showNewFolderModal = false"
      @created="handleFolderCreated"
    />
    
    <RenameModal 
      v-if="showRenameModal" 
      :file="selectedFile"
      @close="showRenameModal = false"
      @renamed="handleRenamed"
    />
    
    <MoveModal 
      v-if="showMoveModal" 
      :file="selectedFile"
      @close="showMoveModal = false"
      @moved="handleMoved"
    />
    
    <PreviewModal 
      v-if="showPreviewModal" 
      :file="selectedFile"
      @close="showPreviewModal = false"
    />
    
    <ShareModal 
      v-if="showShareModal" 
      :file="selectedFile"
      @close="showShareModal = false"
    />

    <button class="ai-fab" @click="showAiPanel = true" title="AI Assistant">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
      </svg>
    </button>

    <AiPanel 
      :is-open="showAiPanel"
      :current-folder-id="currentParentId"
      :preview-file="showPreviewModal ? selectedFile : null"
      @close="showAiPanel = false"
      @navigate-to-file="handleNavigateToFile"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { fileApi } from '../api/file'
import UploadModal from '../components/UploadModal.vue'
import NewFolderModal from '../components/NewFolderModal.vue'
import RenameModal from '../components/RenameModal.vue'
import MoveModal from '../components/MoveModal.vue'
import PreviewModal from '../components/PreviewModal.vue'
import ShareModal from '../components/ShareModal.vue'
import AiPanel from '../components/AiPanel.vue'

const router = useRouter()
const userStore = useUserStore()

const files = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const currentParentId = ref(0)
const lastFileId = ref(null)
const hasMore = ref(false)
const breadcrumbs = ref([])

const showUploadModal = ref(false)
const showNewFolderModal = ref(false)
const showRenameModal = ref(false)
const showMoveModal = ref(false)
const showPreviewModal = ref(false)
const showShareModal = ref(false)
const showAiPanel = ref(false)
const selectedFile = ref(null)

const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  file: null
})

onMounted(async () => {
  await userStore.fetchUserInfo()
  await loadFiles()
  document.addEventListener('click', closeContextMenu)
})

onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})

watch(currentParentId, () => {
  loadFiles()
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
    const res = await fileApi.listFiles(currentParentId.value, lastFileId.value, 20)
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
    console.error('Failed to load files:', e)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadMore() {
  await loadFiles(true)
}

async function refreshFiles() {
  await loadFiles()
  await userStore.fetchUserInfo()
}

function navigateTo(parentId) {
  currentParentId.value = parentId
  updateBreadcrumbs(parentId)
}

async function updateBreadcrumbs(parentId) {
  breadcrumbs.value = []
  if (parentId === 0) return
  
  const path = []
  let currentId = parentId
  
  while (currentId && currentId !== 0) {
    try {
      const res = await fileApi.getFileInfo(currentId)
      if (res.code === 200) {
        path.unshift(res.data)
        currentId = res.data.parentId
      } else {
        break
      }
    } catch (e) {
      break
    }
  }
  
  breadcrumbs.value = path
}

function handleClick(file) {
  if (file.fileType === 0) {
    currentParentId.value = file.fileId
    updateBreadcrumbs(file.fileId)
  } else if (isPreviewable(file.fileName)) {
    openPreviewModal(file)
  }
}

function isPreviewable(fileName) {
  const ext = fileName.split('.').pop().toLowerCase()
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico']
  const pdfExts = ['pdf']
  const codeExts = ['js', 'ts', 'jsx', 'tsx', 'vue', 'html', 'css', 'scss', 'sass', 'less',
    'json', 'xml', 'yaml', 'yml', 'java', 'py', 'go', 'rs', 'c', 'cpp', 'h',
    'cs', 'php', 'rb', 'swift', 'kt', 'scala', 'sh', 'bat', 'sql']
  const textExts = ['txt', 'md', 'log', 'csv', 'ini', 'conf', 'cfg', 'properties']
  
  return imageExts.includes(ext) || pdfExts.includes(ext) || codeExts.includes(ext) || textExts.includes(ext)
}

async function handleDelete(file) {
  if (!confirm(`Are you sure you want to delete "${file.fileName}"?`)) return
  
  try {
    let res
    if (file.fileType === 0) {
      res = await fileApi.deleteFolder(file.fileId)
    } else {
      res = await fileApi.deleteFile(file.fileId)
    }
    if (res.code === 200) {
      files.value = files.value.filter(f => f.fileId !== file.fileId)
      await userStore.fetchUserInfo()
    }
  } catch (e) {
    alert(e.message || 'Delete failed')
  }
}

async function handleDownload(file) {
  try {
    await fileApi.downloadFile(file.fileId, file.fileName)
  } catch (e) {
    alert(e.message || 'Download failed')
  }
}

function openRenameModal(file) {
  selectedFile.value = file
  showRenameModal.value = true
}

function openMoveModal(file) {
  selectedFile.value = file
  showMoveModal.value = true
}

function toggleContextMenu(event, file) {
  event.stopPropagation()
  
  const menuWidth = 160
  const menuHeight = 250
  
  let x = event.clientX
  let y = event.clientY
  
  if (x + menuWidth > window.innerWidth) {
    x = window.innerWidth - menuWidth - 10
  }
  if (y + menuHeight > window.innerHeight) {
    y = window.innerHeight - menuHeight - 10
  }
  
  contextMenu.value = {
    visible: true,
    x: x,
    y: y,
    file: file
  }
}

function closeContextMenu() {
  contextMenu.value.visible = false
}

function handleContextAction(action) {
  const file = contextMenu.value.file
  closeContextMenu()
  
  switch (action) {
    case 'rename':
      openRenameModal(file)
      break
    case 'move':
      openMoveModal(file)
      break
    case 'download':
      handleDownload(file)
      break
    case 'delete':
      handleDelete(file)
      break
    case 'preview':
      openPreviewModal(file)
      break
    case 'share':
      openShareModal(file)
      break
  }
}

function openPreviewModal(file) {
  selectedFile.value = file
  showPreviewModal.value = true
}

function openShareModal(file) {
  selectedFile.value = file
  showShareModal.value = true
}

function handleUploaded() {
  loadFiles()
  userStore.fetchUserInfo()
}

function handleFolderCreated() {
  loadFiles()
}

function handleRenamed(updatedFile) {
  const index = files.value.findIndex(f => f.fileId === updatedFile.fileId)
  if (index !== -1) {
    files.value[index] = updatedFile
  }
}

function handleMoved() {
  loadFiles()
}

async function handleLogout() {
  await userStore.logout()
  router.push('/login')
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

function handleNavigateToFile(data) {
  showAiPanel.value = false
  const { fileId, fileName, pathIds } = data
  const parentId = pathIds.length > 1 ? pathIds[pathIds.length - 2] : 0
  
  currentParentId.value = parentId
  updateBreadcrumbs(parentId)
  
  setTimeout(() => {
    const file = files.value.find(f => f.fileId === fileId)
    if (file) {
      selectedFile.value = file
      if (isPreviewable(fileName)) {
        showPreviewModal.value = true
      }
    }
  }, 500)
}
</script>

<style scoped>
.home-page {
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
  position: sticky;
  top: 0;
  z-index: 100;
}

.logo {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.username {
  font-weight: 600;
  color: #333;
}

.quota {
  font-size: 12px;
  color: #999;
}

.quota-bar {
  width: 120px;
  height: 4px;
  background: #eee;
  border-radius: 2px;
  overflow: hidden;
}

.quota-used {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s;
}

.main-content {
  flex: 1;
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.toolbar-left {
  display: flex;
  gap: 12px;
}

.toolbar-right {
  display: flex;
  gap: 12px;
}

.breadcrumb {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.breadcrumb-item {
  color: #667eea;
  cursor: pointer;
  font-size: 14px;
}

.breadcrumb-item:hover {
  text-decoration: underline;
}

.breadcrumb-item.active {
  color: #333;
  font-weight: 500;
  cursor: default;
}

.breadcrumb-item.active:hover {
  text-decoration: none;
}

.breadcrumb-separator {
  color: #999;
  font-size: 14px;
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

.file-row.is-folder {
  cursor: pointer;
}

.file-row.is-folder:hover {
  background: #f0f4ff;
}

.file-row.is-previewable {
  cursor: pointer;
}

.file-row.is-previewable:hover {
  background: #f0fff4;
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
  width: 120px;
  justify-content: flex-end;
  gap: 8px;
}

.btn-icon {
  padding: 6px;
  background: transparent;
  border: none;
  cursor: pointer;
  border-radius: 4px;
}

.btn-icon:hover {
  background: #e0e0e0;
}

.btn-icon svg {
  width: 18px;
  height: 18px;
  color: #666;
}

.context-menu {
  position: fixed;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 160px;
  padding: 6px 0;
  z-index: 1000;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: background 0.2s;
}

.context-menu-item:hover {
  background: #f5f5f5;
}

.context-menu-item svg {
  width: 16px;
  height: 16px;
  color: #666;
}

.context-menu-item.danger {
  color: #dc2626;
}

.context-menu-item.danger svg {
  color: #dc2626;
}

.context-menu-divider {
  height: 1px;
  background: #eee;
  margin: 6px 0;
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
  margin-bottom: 8px;
}

.empty-state .hint {
  color: #999;
  font-size: 14px;
}

.ai-fab {
  position: fixed;
  bottom: 24px;
  right: 24px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ai-fab:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.5);
}

.ai-fab svg {
  width: 28px;
  height: 28px;
}
</style>
