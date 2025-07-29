<template>
  <Card class="notification-demo">
    <template #header>
      <div class="flex align-items-center">
        <i class="pi pi-bell mr-2 text-primary"></i>
        <span class="font-semibold text-lg">Toast Notification Demo</span>
      </div>
    </template>
    
    <template #content>
      <div class="grid">
        <div class="col-12 md:col-6">
          <h5>Basic Toast Notifications</h5>
          <div class="flex flex-column gap-2">
            <Button 
              label="Success Toast" 
              icon="pi pi-check" 
              severity="success"
              @click="showSuccessToast"
            />
            <Button 
              label="Info Toast" 
              icon="pi pi-info-circle" 
              severity="info"
              @click="showInfoToast"
            />
            <Button 
              label="Warning Toast" 
              icon="pi pi-exclamation-triangle" 
              severity="warning"
              @click="showWarningToast"
            />
            <Button 
              label="Error Toast" 
              icon="pi pi-times-circle" 
              severity="danger"
              @click="showErrorToast"
            />
          </div>
        </div>
        
        <div class="col-12 md:col-6">
          <h5>Crawler Notifications</h5>
          <div class="flex flex-column gap-2">
            <Button 
              label="Job Started" 
              icon="pi pi-play" 
              severity="info"
              outlined
              @click="showJobStarted"
            />
            <Button 
              label="Job Completed" 
              icon="pi pi-check-circle" 
              severity="success"
              outlined
              @click="showJobCompleted"
            />
            <Button 
              label="Job Failed" 
              icon="pi pi-times-circle" 
              severity="danger"
              outlined
              @click="showJobFailed"
            />
            <Button 
              label="Health Changed" 
              icon="pi pi-heart" 
              severity="warning"
              outlined
              @click="showHealthChanged"
            />
          </div>
        </div>
        
        <div class="col-12">
          <h5>Advanced Options</h5>
          <div class="flex flex-wrap gap-2">
            <Button 
              label="Sticky Toast" 
              icon="pi pi-thumbtack" 
              severity="secondary"
              @click="showStickyToast"
            />
            <Button 
              label="Custom Duration" 
              icon="pi pi-clock" 
              severity="secondary"
              @click="showCustomDurationToast"
            />
            <Button 
              label="Silent Notification" 
              icon="pi pi-volume-off" 
              severity="secondary"
              @click="showSilentNotification"
            />
            <Button 
              label="Clear All Toasts" 
              icon="pi pi-trash" 
              severity="secondary"
              outlined
              @click="clearAllToasts"
            />
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<script setup lang="ts">
import Card from 'primevue/card'
import Button from 'primevue/button'
import { useNotifications } from '@/composables/useNotifications'

const { showToast, notify, crawlerNotifications } = useNotifications()

// Basic toast notifications
const showSuccessToast = () => {
  showToast.success('Success!', 'Operation completed successfully')
}

const showInfoToast = () => {
  showToast.info('Information', 'Here is some useful information')
}

const showWarningToast = () => {
  showToast.warn('Warning', 'Please check your configuration')
}

const showErrorToast = () => {
  showToast.error('Error', 'Something went wrong')
}

// Crawler-specific notifications
const showJobStarted = () => {
  crawlerNotifications.jobStarted('demo-crawler', 'job-12345')
}

const showJobCompleted = () => {
  crawlerNotifications.jobCompleted('demo-crawler', 'job-12345', 42)
}

const showJobFailed = () => {
  crawlerNotifications.jobFailed('demo-crawler', 'job-12345', 'Network timeout')
}

const showHealthChanged = () => {
  crawlerNotifications.healthChanged('demo-crawler', 'UNHEALTHY', 'Connection issues detected')
}

// Advanced options
const showStickyToast = () => {
  showToast.warn('Sticky Toast', 'This toast will stay until manually dismissed', {
    sticky: true
  })
}

const showCustomDurationToast = () => {
  showToast.info('Custom Duration', 'This toast will disappear in 10 seconds', {
    life: 10000
  })
}

const showSilentNotification = () => {
  notify.silent.info('Silent Notification', 'This notification is stored but no toast is shown')
}

const clearAllToasts = () => {
  showToast.clear()
}
</script>

<style scoped>
.notification-demo {
  margin: 1rem;
}

.notification-demo :deep(.p-card-header) {
  padding: 1.25rem 1.25rem 0.75rem 1.25rem;
}

.notification-demo :deep(.p-card-content) {
  padding: 1.25rem;
}

h5 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: var(--text-color);
  font-weight: 600;
}
</style>