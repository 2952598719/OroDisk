<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>Move File</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      
      <div class="modal-body">
        <p class="move-info">Moving: <strong>{{ file?.fileName }}</strong></p>
        
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
          
          <div v-else class="folder-list">
            <div 
              v-for="folder in folders" 
              :key="folder.fileId"
              class="folder-item"
              :class="{ 
                selected: selectedFolderId === folder.fileId,
                disabled: folder.fileId === file?.fileId
              }"
              @click="selectFolder(folder)"
              @dblclick="navigateTo(folder.fileId)"
            >
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z" />
              </svg>
              <span>{{ folder.fileName }}</span>
            </div>
            
            <div v-if="folders.length === 0" class="empty-folder">
              No subfolders in this location
            </div>
          </div>
        </div>
        
        <div class="selected-info">
          <span>Destination: </span>
          <strong>{{ selectedFolderId === 0 ? 'Root' : getFolderName(selectedFolderId) }}</strong>
        </div>
        
        <div v-if="error" class="alert alert-error">{{ error }}</div>
      </div>
      
      <div class="modal-footer">
        <button class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button class="btn btn-primary" @click="handleMove" :disabled="loading || moving">
          {{ moving ? 'Moving...' : 'Move Here' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { fileApi } from '../api/file'

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'moved'])

const folders = ref([])
const loading = ref(false)
const moving = ref(false)
const currentParentId = ref(0)
const selectedFolderId = ref(0)
const breadcrumbs = ref([])
const error = ref('')

const originalParentId = computed(() => props.file?.parentId || 0)

onMounted(async () => {
  selectedFolderId.value = originalParentId.value
  currentParentId.value = 0
  await loadFolders()
})

async function loadFolders() {
  loading.value = true
  
  try {
    const res = await fileApi.listFiles(currentParentId.value, null, 100)
    if (res.code === 200) {
      folders.value = res.data.files.filter(f => f.fileType === 0)
    }
  } catch (e) {
    console.error('Failed to load folders:', e)
  } finally {
    loading.value = false
  }
}

async function navigateTo(parentId) {
  currentParentId.value = parentId
  selectedFolderId.value = parentId
  await loadFolders()
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

function selectFolder(folder) {
  if (folder.fileId === props.file?.fileId) return
  selectedFolderId.value = folder.fileId
}

function getFolderName(folderId) {
  if (folderId === 0) return 'Root'
  const folder = breadcrumbs.value.find(f => f.fileId === folderId)
  return folder?.fileName || 'Unknown'
}

async function handleMove() {
  if (selectedFolderId.value === originalParentId.value) {
    emit('close')
    return
  }
  
  moving.value = true
  error.value = ''
  
  try {
    const res = await fileApi.moveFile({
      fileId: props.file.fileId,
      targetParentId: selectedFolderId.value
    })
    
    if (res.code === 200) {
      emit('moved')
      emit('close')
    } else {
      error.value = res.message || 'Failed to move'
    }
  } catch (e) {
    error.value = e.message || 'Failed to move'
  } finally {
    moving.value = false
  }
}
</script>

<style scoped>
.modal-body {
  margin-bottom: 20px;
}

.move-info {
  margin-bottom: 16px;
  color: #666;
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

.folder-list {
  max-height: 250px;
  overflow-y: auto;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.folder-item:hover {
  background: #f5f5f5;
}

.folder-item.selected {
  background: #e8f0fe;
}

.folder-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.folder-item svg {
  width: 20px;
  height: 20px;
  color: #f5a623;
}

.empty-folder {
  padding: 20px;
  text-align: center;
  color: #999;
}

.selected-info {
  margin-top: 12px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
