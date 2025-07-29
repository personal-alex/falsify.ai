<template>
  <div id="app" class="app-container">
    <!-- Global Toast notifications with enhanced positioning -->
    <Toast 
      position="top-right" 
      :breakpoints="{ '960px': { width: '100%', right: '0', left: '0' } }"
    />
    
    <!-- Global Confirmation dialogs -->
    <ConfirmDialog />
    
    <!-- Main application layout -->
    <AppLayout />
  </div>
</template>

<script setup lang="ts">
import Toast from 'primevue/toast'
import ConfirmDialog from 'primevue/confirmdialog'
import AppLayout from '@/components/AppLayout.vue'
import { onErrorCaptured, ref } from 'vue'
import { useToast } from 'primevue/usetoast'

const toast = useToast()
const hasGlobalError = ref(false)

// Global error handler
onErrorCaptured((error, _instance, info) => {
  console.error('Global error captured:', error, info)
  
  if (!hasGlobalError.value) {
    hasGlobalError.value = true
    toast.add({
      severity: 'error',
      summary: 'Application Error',
      detail: 'An unexpected error occurred. Please refresh the page.',
      life: 8000
    })
    
    // Reset error flag after a delay
    setTimeout(() => {
      hasGlobalError.value = false
    }, 10000)
  }
  
  return false // Prevent error from propagating
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: var(--surface-ground);
  display: flex;
  flex-direction: column;
}


</style>