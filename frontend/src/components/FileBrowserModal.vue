<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>Select Reference Files</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      
      <div class="modal-body">
        <div class="folder-browser">
          <div class="breadcrumb">
            <span 
              class="breadcrumb-item" 
              :class="{ active: currentParentId === 0 }"
              @click="navigateTo(0)"
            >
              Root
            </span>
            <template v-for="folder in breadcrumbs" :key="folder.fileId">
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
          
          <div v-else class="file-list">
            <div 
              v-for="item in items" 
              :key="item.fileId"
              class="file-item"
              :class="{ 
                'is-folder': item.fileType === 0,
                'is-file': item.fileType === 1,
                'selected': isSelected(item.fileId),
                'disabled': item.fileType === 1 && !isSupportedFile(item.fileName)
              }"
              @click="handleClick(item)"
              @dblclick="handleDblClick(item)"
            >
              <div class="file-checkbox" v-if="item.fileType === 1 && isSupportedFile(item.fileName)">
                <input type="checkbox" :checked="isSelected(item.fileId)" @click.stop />
              </div>
              <svg v-if="item.fileType === 0" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
              </svg>
              <svg v-else xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <span class="file-name">{{ item.fileName }}</span>
              <span class="file-size" v-if="item.fileType === 1">{{ item.fileSizeFormat }}</span>
              <span class="file-badge" v-if="item.fileType === 1 && isSupportedFile(item.fileName)">RAG</span>
            </div>
            
            <div v-if="items.length === 0" class="empty-folder">
              No files or folders in this location
            </div>
          </div>
        </div>
        
        <div class="selected-summary">
          <span>{{ localSelectedFiles.length }} file(s) selected</span>
        </div>
      </div>
      
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button class="btn btn-primary" @click="handleConfirm">
          Confirm
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { fileApi } from '../api/file'

const props = defineProps({
  selectedFileIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'confirm'])

const items = ref([])
const loading = ref(false)
const currentParentId = ref(0)
const breadcrumbs = ref([])
const localSelectedFiles = ref([])

const SUPPORTED_EXTENSIONS = ['txt', 'md', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'csv', 'json', 'xml', 'html', 'htm']

function isSupportedFile(fileName) {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  return SUPPORTED_EXTENSIONS.includes(ext)
}

onMounted(async () => {
  await loadItems()
})

watch(() => props.selectedFileIds, (newVal) => {
  localSelectedFiles.value = newVal.map(id => ({ fileId: id }))
}, { immediate: true })

async function loadItems() {
  loading.value = true
  
  try {
    const res = await fileApi.listFiles(currentParentId.value, null, 100)
    if (res.code === 200) {
      items.value = res.data.files
    }
  } catch (e) {
    console.error('Failed to load items:', e)
  } finally {
    loading.value = false
  }
}

async function navigateTo(parentId) {
  currentParentId.value = parentId
  await loadItems()
  await updateBreadcrumbs(parentId)
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

function isSelected(fileId) {
  return localSelectedFiles.value.some(f => f.fileId === fileId)
}

function handleClick(item) {
  if (item.fileType === 0) {
    return
  }
  
  if (!isSupportedFile(item.fileName)) {
    return
  }
  
  const index = localSelectedFiles.value.findIndex(f => f.fileId === item.fileId)
  if (index >= 0) {
    localSelectedFiles.value.splice(index, 1)
  } else {
    localSelectedFiles.value.push({
      fileId: item.fileId,
      fileName: item.fileName,
      fileSize: item.fileSize,
      fileSizeFormat: item.fileSizeFormat
    })
  }
}

function handleDblClick(item) {
  if (item.fileType === 0) {
    navigateTo(item.fileId)
  }
}

function handleConfirm() {
  emit('confirm', localSelectedFiles.value)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200;
}

.modal {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.modal-close {
  background: transparent;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  line-height: 1;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 16px 20px;
  flex: 1;
  overflow-y: auto;
}

.folder-browser {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.breadcrumb {
  padding: 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.breadcrumb-item {
  color: #667eea;
  cursor: pointer;
  font-size: 13px;
}

.breadcrumb-item:hover {
  text-decoration: underline;
}

.breadcrumb-item.active {
  color: #333;
  font-weight: 500;
  cursor: default;
}

.breadcrumb-separator {
  color: #999;
  font-size: 13px;
}

.file-list {
  max-height: 300px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.file-item:hover {
  background: #f5f5f5;
}

.file-item.selected {
  background: #e8f0fe;
}

.file-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.file-item.is-folder {
  cursor: pointer;
}

.file-item.is-folder:hover {
  background: #f0f0f0;
}

.file-checkbox {
  display: flex;
  align-items: center;
}

.file-checkbox input {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.file-item svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.file-item.is-folder svg {
  color: #f5a623;
}

.file-item.is-file svg {
  color: #667eea;
}

.file-item .file-name {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-item .file-size {
  font-size: 12px;
  color: #999;
}

.file-badge {
  font-size: 10px;
  background: #667eea;
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
}

.empty-folder {
  padding: 20px;
  text-align: center;
  color: #999;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.selected-summary {
  margin-top: 12px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: #f5f5f5;
  border: 1px solid #ddd;
  color: #666;
}

.btn-secondary:hover {
  background: #eee;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
}

.btn-primary:hover {
  opacity: 0.9;
}
</style>
