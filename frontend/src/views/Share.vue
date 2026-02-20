<template>
  <div class="share-page">
    <div v-if="loading" class="loading-container">
      <div class="spinner"></div>
      <p>Loading...</p>
    </div>
    
    <div v-else-if="error" class="error-container">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
      <h2>{{ error }}</h2>
      <div class="error-actions">
        <router-link v-if="isLoggedIn" to="/" class="btn btn-primary">Go to My Disk</router-link>
        <router-link v-else to="/login" class="btn btn-primary">Login</router-link>
      </div>
    </div>
    
    <div v-else-if="needPassword" class="password-container">
      <div class="password-card">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
        </svg>
        <h2>Password Required</h2>
        <p>This share requires a password to access</p>
        
        <div class="password-form">
          <input 
            v-model="password" 
            type="password" 
            placeholder="Enter password"
            @keyup.enter="verifyPassword"
          />
          <button class="btn btn-primary" @click="verifyPassword" :disabled="verifying">
            {{ verifying ? 'Verifying...' : 'Submit' }}
          </button>
        </div>
        
        <p v-if="passwordError" class="password-error">{{ passwordError }}</p>
      </div>
    </div>
    
    <div v-else class="share-content">
      <div class="file-card">
        <div class="file-icon">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        
        <div class="file-info">
          <h2>{{ shareInfo.fileName }}</h2>
          <p class="file-size">{{ shareInfo.fileSizeFormat }}</p>
        </div>
        
        <div class="share-meta">
          <div v-if="shareInfo.expireTime" class="meta-item">
            <span class="meta-label">Expires:</span>
            <span class="meta-value">{{ formatDate(shareInfo.expireTime) }}</span>
          </div>
          <div v-if="shareInfo.maxDownloads > 0" class="meta-item">
            <span class="meta-label">Downloads:</span>
            <span class="meta-value">{{ shareInfo.currentDownloads }} / {{ shareInfo.maxDownloads }}</span>
          </div>
        </div>
        
        <div class="actions">
          <button 
            v-if="canPreview" 
            class="btn btn-secondary" 
            @click="openPreview"
          >
            Preview
          </button>
          <button 
            v-if="shareInfo.allowDownload" 
            class="btn btn-primary" 
            @click="handleDownload"
          >
            Download
          </button>
        </div>
      </div>
    </div>
    
    <PreviewModal 
      v-if="showPreview" 
      :file="previewFile"
      :shareCode="route.params.shareCode"
      :sharePassword="password"
      :allowDownload="shareInfo?.allowDownload"
      @close="showPreview = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { shareApi } from '../api/share'
import { useUserStore } from '../stores/user'
import PreviewModal from '../components/PreviewModal.vue'

const route = useRoute()
const userStore = useUserStore()

const loading = ref(true)
const error = ref('')
const shareInfo = ref(null)
const needPassword = ref(false)
const password = ref('')
const passwordError = ref('')
const verifying = ref(false)
const showPreview = ref(false)

const isLoggedIn = computed(() => userStore.isLoggedIn)

const previewFile = computed(() => {
  if (!shareInfo.value) return null
  return {
    fileId: 0,
    fileName: shareInfo.value.fileName,
    fileType: shareInfo.value.fileType
  }
})

const canPreview = computed(() => {
  if (!shareInfo.value) return false
  const ext = shareInfo.value.fileName.split('.').pop().toLowerCase()
  const previewableExts = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg', 'pdf', 
    'txt', 'md', 'log', 'csv', 'json', 'xml', 'html', 'css', 'js', 'ts', 'vue',
    'java', 'py', 'go', 'rs', 'c', 'cpp', 'h', 'cs', 'php', 'rb', 'swift', 'kt', 'scala', 'sh', 'sql']
  return previewableExts.includes(ext)
})

onMounted(async () => {
  await loadShareInfo()
})

async function loadShareInfo() {
  const shareCode = route.params.shareCode
  
  try {
    const res = await shareApi.getShareInfo(shareCode)
    shareInfo.value = res.data
    needPassword.value = res.data.hasPassword
  } catch (e) {
    error.value = e.message || 'Share not found'
  } finally {
    loading.value = false
  }
}

async function verifyPassword() {
  verifying.value = true
  passwordError.value = ''
  
  try {
    const res = await shareApi.verifyPassword({
      shareCode: route.params.shareCode,
      password: password.value
    })
    
    if (res.data) {
      needPassword.value = false
    } else {
      passwordError.value = 'Incorrect password'
    }
  } catch (e) {
    passwordError.value = e.message || 'Verification failed'
  } finally {
    verifying.value = false
  }
}

async function handleDownload() {
  try {
    await shareApi.downloadShareFile(
      route.params.shareCode, 
      password.value, 
      shareInfo.value.fileName
    )
  } catch (e) {
    alert(e.message || 'Download failed')
  }
}

function openPreview() {
  showPreview.value = true
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.share-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.loading-container,
.error-container {
  text-align: center;
  color: white;
}

.loading-container .spinner {
  width: 48px;
  height: 48px;
  border: 4px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-container svg {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
}

.error-container h2 {
  font-size: 24px;
  margin-bottom: 24px;
}

.password-container {
  width: 100%;
  max-width: 400px;
}

.password-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
}

.password-card svg {
  width: 64px;
  height: 64px;
  color: #667eea;
  margin-bottom: 16px;
}

.password-card h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
}

.password-card p {
  color: #666;
  margin-bottom: 24px;
}

.password-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.password-form input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 16px;
}

.password-form input:focus {
  outline: none;
  border-color: #667eea;
}

.password-error {
  color: #ef4444;
  margin-top: 12px;
  font-size: 14px;
}

.share-content {
  width: 100%;
  max-width: 500px;
}

.file-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
}

.file-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.file-icon svg {
  width: 40px;
  height: 40px;
  color: white;
}

.file-info h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 8px;
  word-break: break-all;
}

.file-size {
  color: #666;
  font-size: 16px;
  margin-bottom: 24px;
}

.share-meta {
  background: #f5f5f5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.meta-item:last-child {
  margin-bottom: 0;
}

.meta-label {
  color: #666;
}

.meta-value {
  color: #333;
  font-weight: 500;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
