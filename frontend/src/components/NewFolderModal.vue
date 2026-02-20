<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>New Folder</h3>
        <button class="modal-close" @click="$emit('close')">&times;</button>
      </div>
      
      <form @submit.prevent="handleCreate">
        <div class="form-group">
          <label>Folder Name</label>
          <input 
            v-model="folderName" 
            type="text" 
            placeholder="Enter folder name"
            required
            autofocus
          />
        </div>
        
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="$emit('close')">Cancel</button>
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? 'Creating...' : 'Create' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { fileApi } from '../api/file'

const props = defineProps({
  parentId: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['close', 'created'])

const folderName = ref('')
const loading = ref(false)
const error = ref('')

async function handleCreate() {
  if (!folderName.value.trim()) {
    error.value = 'Please enter a folder name'
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const res = await fileApi.createFolder({
      folderName: folderName.value.trim(),
      parentId: props.parentId
    })
    
    if (res.code === 200) {
      emit('created')
      emit('close')
    } else {
      error.value = res.message || 'Failed to create folder'
    }
  } catch (e) {
    error.value = e.message || 'Failed to create folder'
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
