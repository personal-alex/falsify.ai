<template>
  <div class="job-detail-modal">
    <div class="job-overview">
      <div class="job-header">
        <div class="job-title">
          <h4>{{ job.jobId }}</h4>
          <Badge
            :value="job.status"
            :severity="getStatusSeverity(job.status)"
            :class="getStatusClass(job.status)"
          />
        </div>
        <div class="job-actions" v-if="job.status === 'RUNNING'">
          <Button
            icon="pi pi-refresh"
            severity="secondary"
            size="small"
            @click="$emit('refresh')"
            v-tooltip.top="'Refresh'"
          />
          <Button
            icon="pi pi-times"
            severity="danger"
            size="small"
            @click="confirmCancel = true"
            v-tooltip.top="'Cancel job'"
          />
        </div>
      </div>

      <div class="job-metadata">
        <div class="metadata-grid">
          <div class="metadata-item">
            <label>Crawler ID</label>
            <span>{{ job.crawlerId }}</span>
          </div>
          <div class="metadata-item">
            <label>Request ID</label>
            <span>{{ job.requestId || 'N/A' }}</span>
          </div>
          <div class="metadata-item">
            <label>Started</label>
            <span>{{ formatDateTime(job.startTime) }}</span>
          </div>
          <div class="metadata-item" v-if="job.endTime">
            <label>Ended</label>
            <span>{{ formatDateTime(job.endTime) }}</span>
          </div>
          <div class="metadata-item">
            <label>Duration</label>
            <span>{{ formatDuration(job) }}</span>
          </div>
          <div class="metadata-item" v-if="job.lastUpdated">
            <label>Last Updated</label>
            <span>{{ formatDateTime(job.lastUpdated) }}</span>
          </div>
        </div>
      </div>
    </div>

    <Divider />

    <div class="job-progress">
      <h5>Progress & Statistics</h5>
      
      <div class="progress-grid">
        <div class="progress-item">
          <div class="progress-header">
            <span class="progress-label">Articles Processed</span>
            <span class="progress-value">{{ job.articlesProcessed }}</span>
          </div>
          <ProgressBar
            :value="getProgressPercentage('processed')"
            :showValue="false"
            class="progress-bar success"
          />
        </div>

        <div class="progress-item">
          <div class="progress-header">
            <span class="progress-label">Articles Skipped</span>
            <span class="progress-value">{{ job.articlesSkipped }}</span>
          </div>
          <ProgressBar
            :value="getProgressPercentage('skipped')"
            :showValue="false"
            class="progress-bar warning"
          />
        </div>

        <div class="progress-item">
          <div class="progress-header">
            <span class="progress-label">Articles Failed</span>
            <span class="progress-value">{{ job.articlesFailed }}</span>
          </div>
          <ProgressBar
            :value="getProgressPercentage('failed')"
            :showValue="false"
            class="progress-bar danger"
          />
        </div>

        <div class="progress-item">
          <div class="progress-header">
            <span class="progress-label">Success Rate</span>
            <span class="progress-value">{{ job.successRate.toFixed(1) }}%</span>
          </div>
          <ProgressBar
            :value="job.successRate"
            :showValue="false"
            class="progress-bar info"
          />
        </div>
      </div>

      <div class="statistics-summary">
        <div class="summary-item">
          <i class="pi pi-chart-bar"></i>
          <div>
            <span class="summary-label">Total Attempted</span>
            <span class="summary-value">{{ job.totalArticlesAttempted }}</span>
          </div>
        </div>
        <div class="summary-item" v-if="job.status === 'RUNNING'">
          <i class="pi pi-clock"></i>
          <div>
            <span class="summary-label">Elapsed Time</span>
            <span class="summary-value">{{ formatElapsedTime(job.elapsedTimeMs) }}</span>
          </div>
        </div>
        <div class="summary-item" v-if="averageProcessingTime">
          <i class="pi pi-stopwatch"></i>
          <div>
            <span class="summary-label">Avg. Processing Time</span>
            <span class="summary-value">{{ averageProcessingTime }}</span>
          </div>
        </div>
      </div>
    </div>

    <Divider />

    <div class="job-activity">
      <h5>Current Activity</h5>
      <div class="activity-display">
        <div class="activity-text">
          <i class="pi pi-spin pi-spinner" v-if="job.status === 'RUNNING'"></i>
          <i class="pi pi-check-circle" v-else-if="job.status === 'COMPLETED'"></i>
          <i class="pi pi-times-circle" v-else-if="job.status === 'FAILED'"></i>
          <i class="pi pi-ban" v-else-if="job.status === 'CANCELLED'"></i>
          <span>{{ job.currentActivity || 'No activity information' }}</span>
        </div>
        <div class="activity-time" v-if="job.lastUpdated">
          Last updated: {{ formatRelativeTime(job.lastUpdated) }}
        </div>
      </div>
    </div>

    <div v-if="job.errorMessage" class="job-error">
      <Divider />
      <h5>Error Details</h5>
      <div class="error-display">
        <Message severity="error" :closable="false">
          <div class="error-content">
            <pre>{{ job.errorMessage }}</pre>
          </div>
        </Message>
      </div>
    </div>

    <!-- Cancel Confirmation Dialog -->
    <Dialog
      v-model:visible="confirmCancel"
      header="Cancel Job"
      modal
      :style="{ width: '25rem' }"
    >
      <div class="cancel-confirmation">
        <i class="pi pi-exclamation-triangle" style="font-size: 2rem; color: var(--orange-500)"></i>
        <div>
          <p>Are you sure you want to cancel this job?</p>
          <p class="cancel-warning">This action cannot be undone.</p>
        </div>
      </div>
      
      <template #footer>
        <Button
          label="No"
          severity="secondary"
          @click="confirmCancel = false"
        />
        <Button
          label="Yes, Cancel Job"
          severity="danger"
          @click="handleCancel"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { JobStatus } from '@/types/job'

interface Props {
  job: JobStatus
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  cancel: [jobId: string]
}>()

// State
const confirmCancel = ref(false)

// Computed
const averageProcessingTime = computed(() => {
  if (props.job.articlesProcessed > 0 && props.job.elapsedTimeMs > 0) {
    const avgMs = props.job.elapsedTimeMs / props.job.articlesProcessed
    return formatDuration({ elapsedTimeMs: avgMs } as JobStatus)
  }
  return null
})

// Methods
const getStatusSeverity = (status: JobStatus['status']) => {
  switch (status) {
    case 'RUNNING': return 'info'
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    case 'CANCELLED': return 'warning'
    default: return 'secondary'
  }
}

const getStatusClass = (status: JobStatus['status']) => {
  return `job-status-${status.toLowerCase()}`
}

const getProgressPercentage = (type: 'processed' | 'skipped' | 'failed') => {
  if (props.job.totalArticlesAttempted === 0) return 0
  
  const value = type === 'processed' ? props.job.articlesProcessed :
                type === 'skipped' ? props.job.articlesSkipped :
                props.job.articlesFailed
  
  return (value / props.job.totalArticlesAttempted) * 100
}

const formatDateTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString()
}

const formatRelativeTime = (dateString: string) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  const diffHours = Math.floor(diffMinutes / 60)
  
  if (diffSeconds < 60) {
    return `${diffSeconds} seconds ago`
  } else if (diffMinutes < 60) {
    return `${diffMinutes} minutes ago`
  } else if (diffHours < 24) {
    return `${diffHours} hours ago`
  } else {
    return date.toLocaleString()
  }
}

const formatDuration = (job: JobStatus) => {
  const duration = job.durationMs || job.elapsedTimeMs
  if (!duration) return '-'
  
  const seconds = Math.floor(duration / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)
  
  if (days > 0) {
    return `${days}d ${hours % 24}h ${minutes % 60}m`
  } else if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

const formatElapsedTime = (ms: number) => {
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m ${seconds % 60}s`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

const handleCancel = () => {
  emit('cancel', props.job.jobId)
  confirmCancel.value = false
}
</script>

<style scoped>
.job-detail-modal {
  padding: 0;
}

.job-overview {
  margin-bottom: 1rem;
}

.job-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.job-title {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.job-title h4 {
  margin: 0;
  font-family: monospace;
  color: var(--text-color);
}

.job-actions {
  display: flex;
  gap: 0.5rem;
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.metadata-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.metadata-item label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-color-secondary);
}

.metadata-item span {
  font-size: 0.875rem;
  color: var(--text-color);
}

.job-progress h5,
.job-activity h5 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
}

.progress-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.progress-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-color-secondary);
}

.progress-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-color);
}

.progress-bar {
  height: 0.5rem;
}

.progress-bar.success :deep(.p-progressbar-value) {
  background: var(--green-500);
}

.progress-bar.warning :deep(.p-progressbar-value) {
  background: var(--orange-500);
}

.progress-bar.danger :deep(.p-progressbar-value) {
  background: var(--red-500);
}

.progress-bar.info :deep(.p-progressbar-value) {
  background: var(--blue-500);
}

.statistics-summary {
  display: flex;
  gap: 2rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: var(--border-radius);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.summary-item i {
  font-size: 1.25rem;
  color: var(--primary-color);
}

.summary-item div {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.summary-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.summary-value {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-color);
}

.activity-display {
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: var(--border-radius);
  border-left: 4px solid var(--primary-color);
}

.activity-text {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}

.activity-text i {
  font-size: 1rem;
}

.activity-text span {
  font-size: 0.875rem;
  color: var(--text-color);
}

.activity-time {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.job-error {
  margin-top: 1rem;
}

.error-display {
  margin-top: 0.5rem;
}

.error-content pre {
  margin: 0;
  font-size: 0.8rem;
  white-space: pre-wrap;
  word-break: break-word;
}

.job-status-running {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.cancel-confirmation {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 0;
}

.cancel-confirmation div {
  flex: 1;
}

.cancel-confirmation p {
  margin: 0 0 0.5rem 0;
}

.cancel-warning {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .job-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .job-actions {
    align-self: stretch;
    justify-content: center;
  }
  
  .metadata-grid {
    grid-template-columns: 1fr;
  }
  
  .progress-grid {
    grid-template-columns: 1fr;
  }
  
  .statistics-summary {
    flex-direction: column;
    gap: 1rem;
  }
  
  .cancel-confirmation {
    flex-direction: column;
    text-align: center;
  }
}
</style>