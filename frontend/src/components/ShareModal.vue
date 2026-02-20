<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>Share File</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      
      <div class="modal-body">
        <div v-if="!shareUrl" class="share-form">
          <div class="form-group">
            <label>Password (optional)</label>
            <input 
              v-model="password" 
              type="text" 
              placeholder="Leave empty for no password" 
              maxlength="8"
            />
          </div>
          
          <div class="form-group">
            <label>Expiration</label>
            <select v-model="expireType">
              <option value="never">Never</option>
              <option value="1h">1 Hour</option>
              <option value="1d">1 Day</option>
              <option value="7d">7 Days</option>
              <option value="30d">30 Days</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>Max Downloads</label>
            <select v-model="maxDownloads">
              <option :value="-1">Unlimited</option>
              <option :value="1">1 time</option>
              <option :value="5">5 times</option>
              <option :value="10">10 times</option>
              <option :value="100">100 times</option>
            </select>
          </div>
          
          <div class="form-group checkbox">
            <label>
              <input type="checkbox" v-model="allowDownload" />
              Allow download
            </label>
          </div>
        </div>
        
        <div v-else class="share-result">
          <div class="success-icon">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <p class="success-text">Share link created!</p>
          
          <div class="share-link">
            <input :value="shareUrl" readonly ref="shareInput" />
            <button class="btn btn-small btn-primary" @click="copyLink">
              {{ copied ? 'Copied!' : 'Copy' }}
            </button>
          </div>
          
          <div v-if="password" class="share-password">
            <span>Password: </span>
            <strong>{{ password }}</strong>
          </div>
        </div>
        
        <div v-if="error" class="alert alert-error">{{ error }}</div>
      </div>
      
      <div class="modal-footer">
        <button v-if="!shareUrl" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
        <button v-if="!shareUrl" class="btn btn-primary" @click="createShare" :disabled="loading">
          {{ loading ? 'Creating...' : 'Create Share' }}
        </button>
        <button v-else class="btn btn-primary" @click="$emit('close')">Done</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { shareApi } from '../api/share'

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close'])

const password = ref('')
const expireType = ref('never')
const maxDownloads = ref(-1)
const allowDownload = ref(true)
const loading = ref(false)
const error = ref('')
const shareUrl = ref('')
const copied = ref(false)
const shareInput = ref(null)

function getExpireTime() {
  const now = new Date()
  switch (expireType.value) {
    case '1h':
      return new Date(now.getTime() + 60 * 60 * 1000).toISOString()
    case '1d':
      return new Date(now.getTime() + 24 * 60 * 60 * 1000).toISOString()
    case '7d':
      return new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString()
    case '30d':
      return new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString()
    default:
      return null
  }
}

async function createShare() {
  loading.value = true
  error.value = ''
  
  try {
    const res = await shareApi.createShare({
      fileId: props.file.fileId,
      password: password.value || null,
      expireTime: getExpireTime(),
      maxDownloads: maxDownloads.value,
      allowDownload: allowDownload.value
    })
    
    shareUrl.value = res.data.shareUrl
  } catch (e) {
    error.value = e.message || 'Failed to create share'
  } finally {
    loading.value = false
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (e) {
    shareInput.value?.select()
    document.execCommand('copy')
    copied.value = true
  }
}
</script>

<style scoped>
.share-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.form-group input[type="text"],
.form-group select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: #667eea;
}

.form-group.checkbox {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.form-group.checkbox label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.form-group.checkbox input {
  width: 16px;
  height: 16px;
}

.share-result {
  text-align: center;
}

.success-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  color: #10b981;
}

.success-text {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.share-link {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.share-link input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: #f5f5f5;
}

.share-password {
  font-size: 14px;
  color: #666;
}

.share-password strong {
  color: #333;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
