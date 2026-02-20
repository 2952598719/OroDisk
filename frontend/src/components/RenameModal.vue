<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>Rename</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      
      <form @submit.prevent="handleRename">
        <div class="form-group">
          <label>New Name</label>
          <input 
            v-model="newName" 
            type="text" 
            placeholder="Enter new name"
            required
            autofocus
          />
        </div>
        
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? 'Renaming...' : 'Rename' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { fileApi } from '../api/file'

const props = defineProps({
  file: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'renamed'])

const newName = ref('')
const loading = ref(false)
const error = ref('')

watch(() => props.file, (newFile) => {
  if (newFile) {
    newName.value = newFile.fileName
  }
}, { immediate: true })

async function handleRename() {
  if (!newName.value.trim()) {
    error.value = 'Please enter a name'
    return
  }
  
  if (newName.value.trim() === props.file.fileName) {
    emit('close')
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const res = await fileApi.renameFile({
      fileId: props.file.fileId,
      newName: newName.value.trim()
    })
    
    if (res.code === 200) {
      emit('renamed', res.data)
      emit('close')
    } else {
      error.value = res.message || 'Failed to rename'
    }
  } catch (e) {
    error.value = e.message || 'Failed to rename'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
</style>
