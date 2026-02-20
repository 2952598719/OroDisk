<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="preview-modal">
      <div class="preview-header">
        <h3>{{ file?.fileName }}</h3>
        <div class="preview-actions">
          <button v-if="showDownload" class="btn btn-small btn-secondary" @click="handleDownload">
            Download
          </button>
          <button class="modal-close" @click="$emit('close')">&times;</button>
        </div>
      </div>
      
      <div class="preview-content">
        <div v-if="loading" class="loading">
          <div class="spinner"></div>
        </div>
        
        <div v-else-if="error" class="preview-error">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <p>{{ error }}</p>
          <button v-if="showDownload" class="btn btn-primary" @click="handleDownload">Download to view</button>
        </div>
        
        <div v-else-if="previewType === 'image'" class="preview-image">
          <img :src="previewUrl" />
        </div>
        
        <div v-else-if="previewType === 'pdf'" class="preview-pdf">
          <iframe :src="previewUrl"></iframe>
        </div>
        
        <div v-else-if="previewType === 'code'" class="preview-code">
          <pre><code>{{ content }}</code></pre>
        </div>
        
        <div v-else-if="previewType === 'text'" class="preview-text">
          <pre>{{ content }}</pre>
        </div>
        
        <div v-else class="preview-unsupported">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p>Preview not available for this file type</p>
          <button v-if="showDownload" class="btn btn-primary" @click="handleDownload">Download to view</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { fileApi } from '../api/file'
import { shareApi } from '../api/share'
import { useRoute } from 'vue-router'

const props = defineProps({
  file: {
    type: Object,
    required: true
  },
  shareCode: {
    type: String,
    default: ''
  },
  sharePassword: {
    type: String,
    default: ''
  },
  allowDownload: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['close'])

const route = useRoute()
const loading = ref(true)
const content = ref('')
const error = ref('')
const previewUrl = ref('')

const showDownload = computed(() => {
  if (props.shareCode) {
    return props.allowDownload
  }
  return true
})

const previewType = computed(() => {
  const fileName = props.file.fileName
  const ext = fileName.split('.').pop().toLowerCase()
  
  const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'ico']
  const pdfExts = ['pdf']
  const codeExts = ['js', 'ts', 'jsx', 'tsx', 'vue', 'html', 'css', 'scss', 'sass', 'less',
    'json', 'xml', 'yaml', 'yml', 'java', 'py', 'go', 'rs', 'c', 'cpp', 'h',
    'cs', 'php', 'rb', 'swift', 'kt', 'scala', 'sh', 'bat', 'sql']
  const textExts = ['txt', 'md', 'log', 'csv', 'ini', 'conf', 'cfg', 'properties']
  
  if (imageExts.includes(ext)) return 'image'
  if (pdfExts.includes(ext)) return 'pdf'
  if (codeExts.includes(ext)) return 'code'
  if (textExts.includes(ext)) return 'text'
  
  return null
})

onMounted(async () => {
  await loadPreview()
})

onUnmounted(() => {
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value)
  }
})

async function loadPreview() {
  loading.value = true
  error.value = ''
  
  try {
    if (props.shareCode) {
      await loadSharePreview()
    } else {
      await loadNormalPreview()
    }
  } catch (e) {
    error.value = e.message || 'Failed to load preview'
  } finally {
    loading.value = false
  }
}

async function loadNormalPreview() {
  if (previewType.value === 'image') {
    const blob = await fileApi.previewFileAsBlob(props.file.fileId)
    previewUrl.value = URL.createObjectURL(blob)
  } else if (previewType.value === 'pdf') {
    const blob = await fileApi.previewFileAsBlob(props.file.fileId)
    previewUrl.value = URL.createObjectURL(blob) + '#view=FitH'
  } else if (previewType.value === 'code' || previewType.value === 'text') {
    content.value = await fileApi.previewFileAsText(props.file.fileId)
  }
}

async function loadSharePreview() {
  if (previewType.value === 'image') {
    const blob = await shareApi.previewShareAsBlob(props.shareCode, props.sharePassword)
    previewUrl.value = URL.createObjectURL(blob)
  } else if (previewType.value === 'pdf') {
    const blob = await shareApi.previewShareAsBlob(props.shareCode, props.sharePassword)
    previewUrl.value = URL.createObjectURL(blob) + '#view=FitH'
  } else if (previewType.value === 'code' || previewType.value === 'text') {
    content.value = await shareApi.previewShareAsText(props.shareCode, props.sharePassword)
  }
}

async function handleDownload() {
  try {
    if (props.shareCode) {
      await shareApi.downloadShareFile(props.shareCode, props.sharePassword, props.file.fileName)
    } else {
      await fileApi.downloadFile(props.file.fileId, props.file.fileName)
    }
  } catch (e) {
    alert(e.message || 'Download failed')
  }
}
</script>

<style scoped>
.preview-modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 900px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.preview-header h3 {
  font-size: 16px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  margin-right: 16px;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-close:hover {
  color: #333;
}

.preview-content {
  flex: 1;
  overflow: auto;
  min-height: 300px;
}

.preview-image {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  min-height: 400px;
  background: #f5f5f5;
}

.preview-image img {
  max-width: 100%;
  max-height: 70vh;
  object-fit: contain;
}

.preview-pdf {
  height: 70vh;
}

.preview-pdf iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-code,
.preview-text {
  padding: 20px;
  background: #1e1e1e;
  color: #d4d4d4;
  min-height: 400px;
  overflow: auto;
}

.preview-code pre,
.preview-text pre {
  margin: 0;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.preview-error,
.preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.preview-error svg,
.preview-unsupported svg {
  width: 64px;
  height: 64px;
  color: #ddd;
  margin-bottom: 16px;
}

.preview-error p,
.preview-unsupported p {
  color: #666;
  margin-bottom: 20px;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
}
</style>
