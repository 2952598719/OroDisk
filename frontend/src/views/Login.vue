<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <h1>OroDisk</h1>
        <p>Cloud Storage</p>
      </div>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>Username</label>
          <input 
            v-model="form.username" 
            type="text" 
            placeholder="Enter username"
            required
          />
        </div>
        
        <div class="form-group">
          <label>Password</label>
          <input 
            v-model="form.password" 
            type="password" 
            placeholder="Enter password"
            required
          />
        </div>
        
        <div v-if="error" class="alert alert-error">{{ error }}</div>
        
        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? 'Logging in...' : 'Login' }}
        </button>
      </form>
      
      <div class="auth-footer">
        <p>Don't have an account? <router-link to="/register">Register</router-link></p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  password: ''
})
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  
  try {
    const res = await userStore.login(form.value)
    if (res.code === 200) {
      router.push('/')
    } else {
      error.value = res.message || 'Login failed'
    }
  } catch (e) {
    error.value = e.message || 'Login failed'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-header h1 {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-header p {
  color: #999;
  margin-top: 8px;
}

.btn-block {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  margin-top: 8px;
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  color: #666;
}

.auth-footer a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.auth-footer a:hover {
  text-decoration: underline;
}
</style>
