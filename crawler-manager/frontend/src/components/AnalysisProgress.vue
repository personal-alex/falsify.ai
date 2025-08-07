<template>
  <Panel class="analysis-progress-panel">
    <template #header>
      <div class="flex align-items-center justify-content-between w-full">
        <div class="flex align-items-center">
          <i class="pi pi-clock mr-2 text-primary"></i>
          <span class="font-semibold">Analysis Progress</span>
        </div>
        <div class="flex align-items-center gap-2">
          <Tag 
            :value="job.status" 
            :severity="getStatusSeverity(job.status)"
            :icon="getStatusIcon(job.status)"
          />
          <small class="text-600">{{ formatElapsedTime(job.startedAt) }}</small>
        </div>
      </div>
    </template>
    
    <div class="p-panel-content">
      <!-- Progress Statistics -->
      <div class="grid mb-4">
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center">
            <div class="text-2xl font-bold text-primary mb-1">{{ job.processedArticles }}</div>
            <div class="text-600 text-sm">Processed</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center">
            <div class="text-2xl font-bold text-900 mb-1">{{ job.totalArticles }}</div>
            <div class="text-600 text-sm">Total</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center">
            <div class="text-2xl font-bold text-green-500 mb-1">{{ job.predictionsFound }}</div>
            <div class="text-600 text-sm">Predictions Found</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center">
            <div class="text-2xl font-bold text-orange-500 mb-1">{{ successRate }}%</div>
            <div class="text-600 text-sm">Success Rate</div>
          </div>
        </div>
      </div>
      
      <!-- Progress Bar -->
      <div class="mb-4">
        <div class="flex justify-content-between align-items-center mb-2">
          <span class="font-medium">Overall Progress</span>
          <span class="text-600">{{ progressPercentage }}%</span>
        </div>
        <ProgressBar 
          :value="progressPercentage" 
          :showValue="false"
          class="progress-bar-large"
        />
      </div>
      
      <!-- Current Activity -->
      <div v-if="job.currentActivity" class="mb-4">
        <div class="flex align-items-center gap-2 mb-2">
          <i class="pi pi-spin pi-spinner text-primary"></i>
          <span class="font-medium">Current Activity</span>
        </div>
        <div class="surface-card p-3 border-round">
          <div class="text-900">{{ job.currentActivity }}</div>
          <div v-if="estimatedCompletion" class="text-600 text-sm mt-1">
            Estimated completion: {{ estimatedCompletion }}
          </div>
        </div>
      </div>
      
      <!-- Timeline Visualization -->
      <div class="mb-4">
        <div class="font-medium mb-3">Analysis Timeline</div>
        <Timeline 
          :value="timelineEvents" 
          layout="horizontal"
          class="analysis-timeline"
        >
          <template #marker="{ item }">
            <div 
              class="timeline-marker"
              :class="{
                'completed': item.completed,
                'current': item.current,
                'pending': !item.completed && !item.current
              }"
            >
              <i :class="item.icon"></i>
            </div>
          </template>
          
          <template #content="{ item }">
            <div class="timeline-content">
              <div class="font-medium text-sm">{{ item.title }}</div>
              <div v-if="item.description" class="text-600 text-xs">{{ item.description }}</div>
              <div v-if="item.timestamp" class="text-500 text-xs mt-1">{{ formatTime(item.timestamp) }}</div>
            </div>
          </template>
        </Timeline>
      </div>
      
      <!-- Real-time Updates Indicator -->
      <div class="flex align-items-center justify-content-between">
        <div class="flex align-items-center gap-2">
          <div 
            class="connection-indicator"
            :class="{ 'connected': isConnected, 'disconnected': !isConnected }"
          ></div>
          <span class="text-600 text-sm">
            {{ isConnected ? 'Real-time updates active' : 'Connection lost - retrying...' }}
          </span>
        </div>
        
        <div class="flex align-items-center gap-2">
          <Button 
            v-if="job.status === 'RUNNING'"
            icon="pi pi-times"
            label="Cancel"
            @click="$emit('cancel-analysis')"
            severity="danger"
            size="small"
            outlined
          />
          
          <Button 
            v-if="job.status === 'FAILED'"
            icon="pi pi-refresh"
            label="Retry"
            @click="$emit('retry-analysis')"
            severity="secondary"
            size="small"
          />
        </div>
      </div>
    </div>
  </Panel>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import Panel from 'primevue/panel'
import ProgressBar from 'primevue/progressbar'
import Timeline from 'primevue/timeline'
import Tag from 'primevue/tag'
import Button from 'primevue/button'
import { getWebSocketService } from '@/services/websocket'
import type { JobUpdateMessage } from '@/services/websocket'

// Props
interface AnalysisJob {
  id: string
  jobId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  startedAt: string
  completedAt?: string
  totalArticles: number
  processedArticles: number
  predictionsFound: number
  analysisType: 'mock' | 'llm'
  currentActivity?: string
  errorMessage?: string
}

interface Props {
  job: AnalysisJob
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'cancel-analysis': []
  'retry-analysis': []
  'job-updated': [job: AnalysisJob]
}>()

// Reactive state
const isConnected = ref(false)
const wsService = getWebSocketService()

// Computed properties
const progressPercentage = computed(() => {
  if (props.job.totalArticles === 0) return 0
  return Math.round((props.job.processedArticles / props.job.totalArticles) * 100)
})

const successRate = computed(() => {
  if (props.job.processedArticles === 0) return 0
  return Math.round((props.job.predictionsFound / props.job.processedArticles) * 100)
})

const estimatedCompletion = computed(() => {
  if (props.job.status !== 'RUNNING' || props.job.processedArticles === 0) return null
  
  const elapsed = Date.now() - new Date(props.job.startedAt).getTime()
  const avgTimePerArticle = elapsed / props.job.processedArticles
  const remainingArticles = props.job.totalArticles - props.job.processedArticles
  const estimatedRemainingTime = remainingArticles * avgTimePerArticle
  
  const completionTime = new Date(Date.now() + estimatedRemainingTime)
  return completionTime.toLocaleTimeString()
})

const timelineEvents = computed(() => {
  const events = [
    {
      id: 'started',
      title: 'Analysis Started',
      description: `Processing ${props.job.totalArticles} articles`,
      icon: 'pi pi-play',
      completed: true,
      current: false,
      timestamp: props.job.startedAt
    },
    {
      id: 'processing',
      title: 'Processing Articles',
      description: `${props.job.processedArticles}/${props.job.totalArticles} completed`,
      icon: 'pi pi-cog',
      completed: props.job.status === 'COMPLETED' || props.job.status === 'FAILED',
      current: props.job.status === 'RUNNING',
      timestamp: props.job.status === 'RUNNING' ? new Date().toISOString() : undefined
    },
    {
      id: 'extracting',
      title: 'Extracting Predictions',
      description: `${props.job.predictionsFound} predictions found`,
      icon: 'pi pi-search',
      completed: props.job.status === 'COMPLETED',
      current: props.job.status === 'RUNNING' && props.job.processedArticles > 0,
      timestamp: undefined
    },
    {
      id: 'completed',
      title: getCompletionTitle(),
      description: getCompletionDescription(),
      icon: getCompletionIcon(),
      completed: props.job.status === 'COMPLETED' || props.job.status === 'FAILED' || props.job.status === 'CANCELLED',
      current: false,
      timestamp: props.job.completedAt
    }
  ]
  
  return events
})

// Helper functions
function getStatusSeverity(status: string) {
  switch (status) {
    case 'RUNNING': return 'info'
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'CANCELLED': return 'warning'
    case 'PENDING': return 'secondary'
    default: return 'secondary'
  }
}

function getStatusIcon(status: string) {
  switch (status) {
    case 'RUNNING': return 'pi-spin pi-spinner'
    case 'COMPLETED': return 'pi-check'
    case 'FAILED': return 'pi-times'
    case 'CANCELLED': return 'pi-ban'
    case 'PENDING': return 'pi-clock'
    default: return 'pi-question'
  }
}

function getCompletionTitle() {
  switch (props.job.status) {
    case 'COMPLETED': return 'Analysis Complete'
    case 'FAILED': return 'Analysis Failed'
    case 'CANCELLED': return 'Analysis Cancelled'
    default: return 'Finalizing Results'
  }
}

function getCompletionDescription() {
  switch (props.job.status) {
    case 'COMPLETED': return `Successfully analyzed ${props.job.totalArticles} articles`
    case 'FAILED': return props.job.errorMessage || 'Analysis failed due to an error'
    case 'CANCELLED': return 'Analysis was cancelled by user'
    default: return 'Preparing final results...'
  }
}

function getCompletionIcon() {
  switch (props.job.status) {
    case 'COMPLETED': return 'pi pi-check-circle'
    case 'FAILED': return 'pi pi-times-circle'
    case 'CANCELLED': return 'pi pi-ban'
    default: return 'pi pi-hourglass'
  }
}

function formatElapsedTime(startTime: string) {
  const start = new Date(startTime)
  const now = new Date()
  const elapsed = now.getTime() - start.getTime()
  
  const seconds = Math.floor(elapsed / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ago`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s ago`
  } else {
    return `${seconds}s ago`
  }
}

function formatTime(timestamp?: string) {
  if (!timestamp) return ''
  return new Date(timestamp).toLocaleTimeString()
}

// WebSocket connection management
const handleConnectionStatus = (status: any) => {
  isConnected.value = status.connected
}

const handleJobUpdate = (message: JobUpdateMessage) => {
  // Only handle updates for this specific job
  if (message.jobId !== props.job.jobId) return
  
  // Update job data based on WebSocket message
  const updatedJob: AnalysisJob = {
    ...props.job,
    status: message.status,
    processedArticles: message.progress?.articlesProcessed || props.job.processedArticles,
    predictionsFound: (message.progress as any)?.predictionsFound ?? props.job.predictionsFound,
    currentActivity: message.progress?.currentActivity,
    completedAt: message.type === 'job.completed' || message.type === 'job.failed' 
      ? message.timestamp 
      : props.job.completedAt
  }
  
  emit('job-updated', updatedJob)
}

// Lifecycle hooks
onMounted(() => {
  // Set up WebSocket listeners
  const unsubscribeStatus = wsService.onConnectionStatusChange(handleConnectionStatus)
  const unsubscribeJob = wsService.on('analysis.job.progress', handleJobUpdate)
  const unsubscribeJobCompleted = wsService.on('analysis.job.completed', handleJobUpdate)
  const unsubscribeJobFailed = wsService.on('analysis.job.failed', handleJobUpdate)
  
  // Set initial connection status
  isConnected.value = wsService.isConnected()
  
  // Cleanup function
  onUnmounted(() => {
    unsubscribeStatus()
    unsubscribeJob()
    unsubscribeJobCompleted()
    unsubscribeJobFailed()
  })
})

// Watch for job status changes to emit events
watch(() => props.job.status, (newStatus, oldStatus) => {
  if (oldStatus !== newStatus) {
    // Could emit additional events here if needed
  }
})
</script>

<style lang="scss" scoped>
.analysis-progress-panel {
  :deep(.p-panel-content) {
    padding: 1.5rem;
  }
}

.progress-bar-large {
  :deep(.p-progressbar) {
    height: 1rem;
    border-radius: 0.5rem;
  }
  
  :deep(.p-progressbar-value) {
    border-radius: 0.5rem;
  }
}

.analysis-timeline {
  :deep(.p-timeline-event-content) {
    padding: 0.5rem 0;
  }
  
  :deep(.p-timeline-event-connector) {
    background: var(--surface-border);
  }
}

.timeline-marker {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  border: 2px solid;
  background: var(--surface-card);
  
  &.completed {
    border-color: var(--green-500);
    color: var(--green-500);
    background: var(--green-50);
  }
  
  &.current {
    border-color: var(--primary-500);
    color: var(--primary-500);
    background: var(--primary-50);
    animation: pulse 2s infinite;
  }
  
  &.pending {
    border-color: var(--surface-400);
    color: var(--surface-400);
  }
}

.timeline-content {
  margin-left: 1rem;
  
  @media screen and (max-width: 768px) {
    margin-left: 0.5rem;
  }
}

.connection-indicator {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  
  &.connected {
    background: var(--green-500);
    animation: pulse-green 2s infinite;
  }
  
  &.disconnected {
    background: var(--red-500);
    animation: pulse-red 1s infinite;
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(var(--primary-500-rgb), 0.7);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(var(--primary-500-rgb), 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(var(--primary-500-rgb), 0);
  }
}

@keyframes pulse-green {
  0% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.7);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(34, 197, 94, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0);
  }
}

@keyframes pulse-red {
  0% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
  }
}

// Responsive design
@media screen and (max-width: 768px) {
  .analysis-progress-panel {
    :deep(.p-panel-content) {
      padding: 1rem;
    }
  }
  
  .timeline-marker {
    width: 1.5rem;
    height: 1.5rem;
    font-size: 0.75rem;
  }
}

// Dark theme adjustments
:root.dark {
  .timeline-marker {
    &.completed {
      background: var(--green-900);
    }
    
    &.current {
      background: var(--primary-900);
    }
  }
}
</style>