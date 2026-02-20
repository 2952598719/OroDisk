<template>
  <div class="modal-overlay" @click.self="handleClose">
    <div class="modal">
      <div class="modal-header">
        <h3>Upload File</h3>
        <button class="modal-close" @click="handleClose">&times;</button>
      </div>
      
      <div class="modal-body">
        <div 
          v-if="!uploading"
          class="drop-zone"
          :class="{ dragging: isDragging }"
          @dragover.prevent="isDragging = true"
          @dragleave="isDragging = false"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input 
            ref="fileInput" 
            type="file" 
            style="display: none" 
            @change="handleFileSelect"
            multiple
          />
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
          </svg>
          <p>Drag and drop files here, or click to select</p>
          <p class="hint">Support chunked upload for large files</p>
        </div>
        
        <div v-else class="upload-progress">
          <div class="file-info">
            <span class="file-name">{{ currentFile?.name }}</span>
            <span class="file-size">({{ formatSize(currentFile?.size || 0) }})</span>
          </div>
          
          <div class="progress-container">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: totalProgress + '%' }"></div>
            </div>
            <div class="progress-text">
              <span>{{ totalProgress }}%</span>
              <span v-if="totalChunks > 1">{{ uploadedChunks }} / {{ totalChunks }} chunks</span>
            </div>
          </div>
          
          <div v-if="uploadSpeed" class="upload-speed">
            {{ uploadSpeed }}
          </div>
        </div>
        
        <div v-if="error" class="alert alert-error">{{ error }}</div>
      </div>
      
      <div v-if="!uploading" class="modal-footer">
        <button class="btn btn-secondary" @click="handleClose">Cancel</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import SparkMD5 from 'spark-md5'
import { fileApi } from '../api/file'
import { useUserStore } from '../stores/user'

const props = defineProps({
  parentId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['close', 'uploaded'])

const userStore = useUserStore()
const fileInput = ref(null)
const isDragging = ref(false)
const uploading = ref(false)
const error = ref('')
const currentFile = ref(null)
const uploadedChunks = ref(0)
const totalChunks = ref(1)
const uploadSpeed = ref('')
const uploadQueue = ref([])
const currentFileIndex = ref(0)

const CHUNK_SIZE = 5 * 1024 * 1024
const CONCURRENCY = 3

const totalProgress = computed(() => {
  if (totalChunks.value === 0) return 0
  return Math.round((uploadedChunks.value / totalChunks.value) * 100)
})

function handleClose() {
  if (!uploading.value) {
    emit('close')
  }
}

function triggerFileInput() {
  fileInput.value?.click()
}

function handleFileSelect(e) {
  const files = e.target.files
  if (files && files.length > 0) {
    uploadQueue.value = Array.from(files)
    currentFileIndex.value = 0
    processQueue()
  }
}

function handleDrop(e) {
  isDragging.value = false
  const files = e.dataTransfer.files
  if (files && files.length > 0) {
    uploadQueue.value = Array.from(files)
    currentFileIndex.value = 0
    processQueue()
  }
}

async function processQueue() {
  while (currentFileIndex.value < uploadQueue.value.length) {
    const file = uploadQueue.value[currentFileIndex.value]
    await uploadFile(file)
    currentFileIndex.value++
  }
  emit('uploaded')
  emit('close')
}

function formatSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

async function calculateMD5(file) {
  return new Promise((resolve) => {
    const chunkSize = 2 * 1024 * 1024
    const chunks = Math.ceil(file.size / chunkSize)
    let currentChunk = 0
    const spark = new SparkMD5.ArrayBuffer()
    const reader = new FileReader()
    
    reader.onload = (e) => {
      spark.append(e.target.result)
      currentChunk++
      if (currentChunk < chunks) {
        loadNext()
      } else {
        resolve(spark.end())
      }
    }
    
    function loadNext() {
      const start = currentChunk * chunkSize
      const end = Math.min(start + chunkSize, file.size)
      reader.readAsArrayBuffer(file.slice(start, end))
    }
    
    loadNext()
  })
}

async function uploadFile(file) {
  currentFile.value = file
  uploading.value = true
  error.value = ''
  uploadedChunks.value = 0
  
  const startTime = Date.now()
  
  try {
    const identifier = await calculateMD5(file)
    
    const checkRes = await fileApi.checkFile(identifier, file.name, file.size, props.parentId)
    
    if (checkRes.data.skipUpload) {
      uploadedChunks.value = Math.ceil(file.size / CHUNK_SIZE)
      totalChunks.value = uploadedChunks.value
      
      await fileApi.instantUpload({
        identifier,
        fileName: file.name,
        totalChunks: totalChunks.value,
        totalSize: file.size,
        parentId: props.parentId,
        storageId: checkRes.data.storageId
      })
      
      await userStore.fetchUserInfo()
      return
    }
    
    const uploadedSet = new Set(checkRes.data.uploadedChunks || [])
    uploadedChunks.value = uploadedSet.size
    
    totalChunks.value = Math.ceil(file.size / CHUNK_SIZE)
    
    const chunksToUpload = []
    for (let i = 1; i <= totalChunks.value; i++) {
      if (!uploadedSet.has(i)) {
        chunksToUpload.push(i)
      }
    }
    
    if (chunksToUpload.length > 0) {
      await uploadChunksWithConcurrency(file, identifier, chunksToUpload)
    }
    
    const elapsed = (Date.now() - startTime) / 1000
    const speed = file.size / elapsed
    uploadSpeed.value = `Average: ${formatSize(speed)}/s`
    
    await fileApi.mergeFile({
      identifier,
      fileName: file.name,
      totalChunks: totalChunks.value,
      totalSize: file.size,
      parentId: props.parentId
    })
    
    await userStore.fetchUserInfo()
    
  } catch (e) {
    error.value = e.message || 'Upload failed'
    throw e
  }
}

async function uploadChunksWithConcurrency(file, identifier, chunks) {
  const results = []
  for (const chunkNumber of chunks) {
    try {
      await uploadSingleChunk(file, identifier, chunkNumber)
      uploadedChunks.value++
      results.push({ chunkNumber, success: true })
    } catch (e) {
      results.push({ chunkNumber, success: false, error: e })
      throw e
    }
  }
  return results
}

async function uploadSingleChunk(file, identifier, chunkNumber) {
  const start = (chunkNumber - 1) * CHUNK_SIZE
  const end = Math.min(start + CHUNK_SIZE, file.size)
  const chunk = file.slice(start, end)
  
  const formData = new FormData()
  formData.append('file', chunk, file.name)
  formData.append('identifier', identifier)
  formData.append('chunkNumber', chunkNumber)
  formData.append('totalChunks', totalChunks.value)
  formData.append('totalSize', file.size)
  formData.append('parentId', props.parentId)
  
  let retries = 3
  let lastError = null
  while (retries > 0) {
    try {
      const res = await fileApi.uploadChunk(formData)
      console.log(`Chunk ${chunkNumber} uploaded:`, res)
      return res
    } catch (e) {
      lastError = e
      console.error(`Chunk ${chunkNumber} upload failed (${retries} retries left):`, e)
      retries--
      if (retries > 0) {
        await new Promise(r => setTimeout(r, 1000))
      }
    }
  }
  throw lastError
}
</script>

<style scoped>
.drop-zone {
  border: 2px dashed #ddd;
  border-radius: 12px;
  padding: 60px 40px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.drop-zone:hover,
.drop-zone.dragging {
  border-color: #667eea;
  background: #f8f9ff;
}

.drop-zone svg {
  width: 48px;
  height: 48px;
  color: #ccc;
  margin-bottom: 16px;
}

.drop-zone:hover svg,
.drop-zone.dragging svg {
  color: #667eea;
}

.drop-zone p {
  color: #666;
  margin-bottom: 8px;
}

.drop-zone .hint {
  font-size: 12px;
  color: #999;
}

.upload-progress {
  padding: 20px 0;
}

.file-info {
  margin-bottom: 16px;
}

.file-info .file-name {
  font-weight: 500;
  font-size: 16px;
}

.file-info .file-size {
  color: #999;
  margin-left: 8px;
}

.progress-container {
  margin-bottom: 12px;
}

.progress-bar {
  height: 12px;
  background: #eee;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  transition: width 0.3s;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #666;
}

.upload-speed {
  text-align: center;
  font-size: 14px;
  color: #10b981;
}

.modal-body {
  margin-bottom: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
