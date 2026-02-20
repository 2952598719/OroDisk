<template>
  <Teleport to="body">
    <div v-if="isOpen" class="ai-overlay" @click="$emit('close')"></div>
    <div class="ai-panel" :class="{ open: isOpen }">
      <div class="ai-panel-header">
        <h3>AI Assistant</h3>
        <button class="btn-close" @click="$emit('close')">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div class="ai-panel-sidebar" v-if="showSidebar">
        <button class="btn btn-primary btn-new-chat" @click="createNewConversation">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
          </svg>
          New Chat
        </button>

        <div class="conversation-list" v-if="conversations.length > 0">
          <div 
            v-for="conv in conversations" 
            :key="conv.conversationId" 
            class="conversation-item"
            @click="selectConversation(conv.conversationId)"
          >
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span class="conversation-title">{{ conv.title }}</span>
          </div>
        </div>

        <div v-else class="empty-conversations">
          <p>No conversations yet</p>
          <p class="hint">Start a new chat to begin</p>
        </div>
      </div>

      <div class="ai-panel-chat" v-else>
        <div class="chat-header">
          <button class="btn-back" @click="goBack">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <span class="chat-title">{{ conversationData?.title || 'New Chat' }}</span>
          <button class="btn-delete" @click="deleteCurrentConversation" v-if="currentConversation">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>
        </div>

        <div class="context-files">
          <div class="context-header">
            <span>Reference Files ({{ selectedFiles.length }})</span>
            <button class="btn-add-files" @click="showFileBrowser = true">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
              </svg>
            </button>
          </div>
          <div class="selected-files" v-if="selectedFiles.length > 0">
            <div v-for="file in selectedFiles" :key="file.fileId" class="selected-file">
              <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <span class="file-name" :title="file.fileName">{{ file.fileName }}</span>
              <button class="btn-remove-file" @click="removeFile(file.fileId)">×</button>
            </div>
          </div>
          <div v-else class="no-files">
            <span>No reference files selected</span>
          </div>
        </div>

        <div class="chat-messages" ref="messagesContainer" @click="handleFileLinkClick">
          <div 
            v-for="msg in messages" 
            :key="msg.messageId" 
            class="message"
            :class="msg.role"
          >
            <div class="message-avatar">
              <span v-if="msg.role === 'user'">U</span>
              <span v-else>AI</span>
            </div>
            <div class="message-content">
              <div v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</div>
              <div v-else class="message-text markdown-body" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <textarea 
            v-model="inputMessage" 
            placeholder="Type a message..."
            @keydown.enter.exact.prevent="sendMessage"
            rows="1"
            ref="inputTextarea"
          ></textarea>
          <button class="btn-send" @click="sendMessage" :disabled="!inputMessage.trim() || loading">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </div>
      </div>

      <FileBrowserModal
        v-if="showFileBrowser"
        :selected-file-ids="selectedFileIds"
        @close="showFileBrowser = false"
        @confirm="handleFileSelect"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch, nextTick, onUnmounted, computed } from 'vue'
import { aiApi } from '../api/ai'
import { fileApi } from '../api/file'
import { marked } from 'marked'
import hljs from 'highlight.js'
import FileBrowserModal from './FileBrowserModal.vue'

marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (e) {}
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const props = defineProps({
  isOpen: Boolean,
  currentFolderId: {
    type: Number,
    default: 0
  },
  previewFile: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'navigateToFile'])

const conversations = ref([])
const currentConversation = ref(null)
const conversationData = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const inputTextarea = ref(null)
const abortController = ref(null)

const selectedFiles = ref([])
const showFileBrowser = ref(false)

const selectedFileIds = computed(() => selectedFiles.value.map(f => f.fileId))
const showSidebar = computed(() => !currentConversation.value && !isInNewChat.value)

const isInNewChat = ref(false)

const SUPPORTED_EXTENSIONS = ['txt', 'md', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'csv', 'json', 'xml', 'html', 'htm']

function isSupportedFile(fileName) {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  return SUPPORTED_EXTENSIONS.includes(ext)
}

onUnmounted(() => {
  if (abortController.value) {
    abortController.value.abort()
  }
})

watch(() => props.isOpen, async (newVal) => {
  if (newVal) {
    await loadConversations()
    await initSelectedFiles()
    nextTick(() => {
      inputTextarea.value?.focus()
    })
  }
})

watch([() => props.currentFolderId, () => props.previewFile], async () => {
  if (props.isOpen) {
    await initSelectedFiles()
  }
}, { deep: true })

async function initSelectedFiles() {
  if (props.previewFile) {
    if (isSupportedFile(props.previewFile.fileName)) {
      selectedFiles.value = [{
        fileId: props.previewFile.fileId,
        fileName: props.previewFile.fileName,
        fileSize: props.previewFile.fileSize,
        fileSizeFormat: props.previewFile.fileSizeFormat
      }]
    } else {
      selectedFiles.value = []
    }
  } else {
    await loadFolderFiles()
  }
}

async function loadFolderFiles() {
  try {
    const res = await fileApi.listFiles(props.currentFolderId || 0, null, 100)
    if (res.code === 200) {
      selectedFiles.value = res.data.files
        .filter(f => f.fileType === 1 && isSupportedFile(f.fileName))
        .map(f => ({
          fileId: f.fileId,
          fileName: f.fileName,
          fileSize: f.fileSize,
          fileSizeFormat: f.fileSizeFormat
        }))
    }
  } catch (e) {
    console.error('Failed to load folder files:', e)
  }
}

function renderMarkdown(content) {
  if (!content) return ''
  let processed = content.replace(
    /\[([^\]]+)\]\(file:\/\/(\d+)\)/g,
    '<a href="javascript:void(0)" class="file-link" data-file-id="$2" data-file-name="$1">$1</a>'
  )
  return marked.parse(processed)
}

async function handleFileLinkClick(event) {
  const target = event.target
  if (target.classList.contains('file-link')) {
    event.preventDefault()
    const fileId = parseInt(target.dataset.fileId)
    const fileName = target.dataset.fileName
    
    try {
      const res = await fileApi.getFilePath(fileId)
      if (res.code === 200) {
        emit('navigateToFile', { fileId, fileName, pathIds: res.data.pathIds })
      }
    } catch (e) {
      console.error('Failed to get file path:', e)
    }
  }
}

async function loadConversations() {
  try {
    const res = await aiApi.listConversations()
    if (res.code === 200) {
      conversations.value = res.data
    }
  } catch (e) {
    console.error('Failed to load conversations:', e)
  }
}

async function createNewConversation() {
  currentConversation.value = null
  conversationData.value = null
  messages.value = []
  isInNewChat.value = true
}

function goBack() {
  currentConversation.value = null
  conversationData.value = null
  messages.value = []
  isInNewChat.value = false
}

async function selectConversation(conversationId) {
  try {
    const res = await aiApi.getConversation(conversationId)
    if (res.code === 200) {
      currentConversation.value = conversationId
      conversationData.value = res.data
      messages.value = res.data.messages || []
      isInNewChat.value = false
      scrollToBottom()
    }
  } catch (e) {
    console.error('Failed to load conversation:', e)
  }
}

function removeFile(fileId) {
  const index = selectedFiles.value.findIndex(f => f.fileId === fileId)
  if (index >= 0) {
    selectedFiles.value.splice(index, 1)
  }
}

function handleFileSelect(files) {
  selectedFiles.value = files
  showFileBrowser.value = false
}

async function sendMessage() {
  if (!inputMessage.value.trim() || loading.value) return

  const content = inputMessage.value.trim()
  inputMessage.value = ''

  if (!currentConversation.value) {
    try {
      const res = await aiApi.createConversation({})
      if (res.code === 200) {
        currentConversation.value = res.data.conversationId
        conversationData.value = res.data
        isInNewChat.value = false
        await loadConversations()
      }
    } catch (e) {
      console.error('Failed to create conversation:', e)
      return
    }
  }

  messages.value.push({
    messageId: Date.now(),
    role: 'user',
    content: content,
    createdTime: new Date().toISOString()
  })

  scrollToBottom()
  loading.value = true

  const assistantMsgId = Date.now() + 1
  messages.value.push({
    messageId: assistantMsgId,
    role: 'assistant',
    content: '',
    createdTime: new Date().toISOString()
  })

  try {
    const token = localStorage.getItem('token') || ''
    abortController.value = new AbortController()
    
    const contextFileIds = selectedFiles.value.map(f => f.fileId)
    let url = `/api/ai/stream?conversationId=${currentConversation.value}&content=${encodeURIComponent(content)}&currentFolderId=${props.currentFolderId || 0}`
    if (contextFileIds.length > 0) {
      url += `&contextFileIds=${contextFileIds.join(',')}`
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': token,
        'Accept': 'text/event-stream'
      },
      signal: abortController.value.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          continue
        }
        if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            continue
          }
          if (data) {
            const msgIndex = messages.value.findIndex(m => m.messageId === assistantMsgId)
            if (msgIndex !== -1) {
              messages.value[msgIndex].content += data
              scrollToBottom()
            }
          }
        }
      }
    }

    if (conversationData.value?.title === 'New Chat') {
      await loadConversations()
      const updated = await aiApi.getConversation(currentConversation.value)
      if (updated.code === 200) {
        conversationData.value = updated.data
      }
    }

  } catch (e) {
    if (e.name === 'AbortError') {
      console.log('Stream aborted')
    } else {
      console.error('Failed to send message:', e)
      const msgIndex = messages.value.findIndex(m => m.messageId === assistantMsgId)
      if (msgIndex !== -1) {
        messages.value[msgIndex].content = 'Sorry, an error occurred. Please try again.'
      }
    }
  } finally {
    loading.value = false
    abortController.value = null
    scrollToBottom()
  }
}

async function deleteCurrentConversation() {
  if (!confirm('Delete this conversation?')) return

  try {
    await aiApi.deleteConversation(currentConversation.value)
    currentConversation.value = null
    conversationData.value = null
    messages.value = []
    await loadConversations()
  } catch (e) {
    console.error('Failed to delete conversation:', e)
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
@import 'highlight.js/styles/github-dark.css';

.ai-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 1099;
}

.ai-panel {
  position: fixed;
  top: 0;
  right: -400px;
  width: 400px;
  height: 100vh;
  background: white;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
  z-index: 1100;
  display: flex;
  flex-direction: column;
  transition: right 0.3s ease;
}

.ai-panel.open {
  right: 0;
}

.ai-panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-panel-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.btn-close {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: white;
}

.btn-close svg {
  width: 24px;
  height: 24px;
}

.ai-panel-sidebar {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow-y: auto;
}

.btn-new-chat {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}

.btn-new-chat svg {
  width: 20px;
  height: 20px;
}

.conversation-list {
  flex: 1;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.conversation-item:hover {
  background: #f5f5f5;
}

.conversation-item svg {
  width: 20px;
  height: 20px;
  color: #667eea;
  flex-shrink: 0;
}

.conversation-title {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-conversations {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.empty-conversations p {
  margin: 4px 0;
}

.empty-conversations .hint {
  font-size: 13px;
}

.ai-panel-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.btn-back, .btn-delete {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  color: #666;
}

.btn-back:hover, .btn-delete:hover {
  color: #333;
}

.btn-back svg, .btn-delete svg {
  width: 20px;
  height: 20px;
}

.btn-delete:hover {
  color: #dc2626;
}

.chat-title {
  flex: 1;
  font-weight: 500;
  font-size: 15px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.context-files {
  border-bottom: 1px solid #eee;
  padding: 8px 16px;
  background: #fafafa;
}

.context-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.btn-add-files {
  background: #667eea;
  border: none;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-add-files svg {
  width: 14px;
  height: 14px;
}

.btn-add-files:hover {
  background: #5a6fd6;
}

.selected-files {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 80px;
  overflow-y: auto;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #667eea;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.selected-file svg {
  width: 12px;
  height: 12px;
}

.selected-file .file-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-remove-file {
  background: transparent;
  border: none;
  color: white;
  cursor: pointer;
  padding: 0 4px;
  font-size: 14px;
  line-height: 1;
}

.no-files {
  font-size: 12px;
  color: #999;
  padding: 4px 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: #667eea;
  color: white;
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-content {
  max-width: 80%;
}

.message-text {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message.user .message-text {
  background: #667eea;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .message-text {
  background: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
  white-space: normal;
}

.message.assistant .markdown-body {
  white-space: normal;
}

.message.assistant .markdown-body :deep(p) {
  margin: 0 0 8px 0;
}

.message.assistant .markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.message.assistant .markdown-body :deep(code) {
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.message.assistant .markdown-body :deep(pre) {
  background: #1e1e1e;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}

.message.assistant .markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #d4d4d4;
}

.message.assistant .markdown-body :deep(ul),
.message.assistant .markdown-body :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message.assistant .markdown-body :deep(li) {
  margin: 4px 0;
}

.message.assistant .markdown-body :deep(h1),
.message.assistant .markdown-body :deep(h2),
.message.assistant .markdown-body :deep(h3),
.message.assistant .markdown-body :deep(h4) {
  margin: 12px 0 8px 0;
  font-weight: 600;
}

.message.assistant .markdown-body :deep(h1) { font-size: 18px; }
.message.assistant .markdown-body :deep(h2) { font-size: 16px; }
.message.assistant .markdown-body :deep(h3) { font-size: 15px; }
.message.assistant .markdown-body :deep(h4) { font-size: 14px; }

.message.assistant .markdown-body :deep(blockquote) {
  border-left: 3px solid #667eea;
  margin: 8px 0;
  padding-left: 12px;
  color: #666;
}

.message.assistant .markdown-body :deep(table) {
  border-collapse: collapse;
  margin: 8px 0;
}

.message.assistant .markdown-body :deep(th),
.message.assistant .markdown-body :deep(td) {
  border: 1px solid #ddd;
  padding: 6px 12px;
}

.message.assistant .markdown-body :deep(th) {
  background: #f5f5f5;
  font-weight: 600;
}

.message.assistant .markdown-body :deep(.file-link) {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}

.message.assistant .markdown-body :deep(.file-link:hover) {
  text-decoration: underline;
}

.chat-input {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #eee;
  background: #fafafa;
}

.chat-input textarea {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 24px;
  font-size: 14px;
  resize: none;
  outline: none;
  font-family: inherit;
  max-height: 120px;
  line-height: 1.4;
}

.chat-input textarea:focus {
  border-color: #667eea;
}

.btn-send {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: opacity 0.2s;
}

.btn-send:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-send svg {
  width: 20px;
  height: 20px;
}
</style>
