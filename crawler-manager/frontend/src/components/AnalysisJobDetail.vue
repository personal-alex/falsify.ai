<template>
  <div class="analysis-job-detail">
    <!-- Job Overview -->
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
        <div class="job-actions">
          <Button
            icon="pi pi-refresh"
            severity="secondary"
            size="small"
            @click="$emit('refresh')"
            v-tooltip.top="'Refresh'"
          />
          <Button
            v-if="canCancel(job)"
            icon="pi pi-times"
            severity="danger"
            size="small"
            @click="$emit('cancel', job.jobId)"
            v-tooltip.top="'Cancel job'"
          />
          <Button
            v-if="canRetry(job)"
            icon="pi pi-refresh"
            severity="warning"
            size="small"
            @click="$emit('retry', job.jobId)"
            v-tooltip.top="'Retry job'"
          />
          <Button
            v-if="canExport(job)"
            icon="pi pi-download"
            severity="info"
            size="small"
            @click="$emit('export', job.jobId)"
            v-tooltip.top="'Export results'"
          />
        </div>
      </div>

      <div class="job-metadata">
        <div class="metadata-grid">
          <div class="metadata-item">
            <label>Job ID</label>
            <div class="metadata-value">
              <code>{{ job.jobId }}</code>
              <Button 
                icon="pi pi-copy"
                class="p-button-text p-button-sm"
                @click="copyJobId(job.jobId)"
                v-tooltip="'Copy Job ID'"
              />
            </div>
          </div>
          <div class="metadata-item">
            <label>Analysis Type</label>
            <span>
              <Badge 
                :value="job.analysisType"
                :severity="job.analysisType === 'llm' ? 'info' : 'secondary'"
              />
            </span>
          </div>
          <div class="metadata-item">
            <label>Started</label>
            <span>{{ formatDateTime(job.startedAt) }}</span>
          </div>
          <div class="metadata-item" v-if="job.completedAt">
            <label>Completed</label>
            <span>{{ formatDateTime(job.completedAt) }}</span>
          </div>
          <div class="metadata-item">
            <label>Duration</label>
            <span>{{ formatDuration(job) }}</span>
          </div>
          <div class="metadata-item" v-if="job.status === 'RUNNING'">
            <label>Elapsed Time</label>
            <span>{{ formatElapsedTime(job) }}</span>
          </div>
        </div>
      </div>
    </div>

    <Divider />

    <!-- Progress & Statistics -->
    <div class="job-progress">
      <h5>Progress & Statistics</h5>
      
      <div class="progress-overview">
        <div class="progress-circle">
          <div class="circle-container">
            <svg class="progress-ring" width="120" height="120">
              <circle
                class="progress-ring-background"
                stroke="var(--surface-border)"
                stroke-width="8"
                fill="transparent"
                r="52"
                cx="60"
                cy="60"
              />
              <circle
                class="progress-ring-progress"
                :stroke="getProgressColor(job.status)"
                stroke-width="8"
                fill="transparent"
                r="52"
                cx="60"
                cy="60"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="progressOffset"
                stroke-linecap="round"
              />
            </svg>
            <div class="progress-text">
              <span class="progress-percentage">{{ Math.round(getProgressPercentage(job)) }}%</span>
              <span class="progress-label">Complete</span>
            </div>
          </div>
        </div>

        <div class="progress-stats">
          <div class="stat-item">
            <div class="stat-value">{{ job.totalArticles || 0 }}</div>
            <div class="stat-label">Total Articles</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ job.processedArticles || 0 }}</div>
            <div class="stat-label">Processed</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ job.predictionsFound || 0 }}</div>
            <div class="stat-label">Predictions Found</div>
          </div>
          <div class="stat-item" v-if="job.status === 'COMPLETED'">
            <div class="stat-value">{{ getSuccessRate(job) }}%</div>
            <div class="stat-label">Success Rate</div>
          </div>
        </div>
      </div>

      <div class="progress-bars">
        <div class="progress-item">
          <div class="progress-header">
            <span class="progress-label">Articles Processed</span>
            <span class="progress-value">{{ job.processedArticles || 0 }} / {{ job.totalArticles || 0 }}</span>
          </div>
          <ProgressBar
            :value="getArticleProgressPercentage(job)"
            :showValue="false"
            class="progress-bar success"
          />
        </div>

        <div class="progress-item" v-if="job.predictionsFound > 0">
          <div class="progress-header">
            <span class="progress-label">Prediction Density</span>
            <span class="progress-value">{{ getPredictionDensity(job) }} per article</span>
          </div>
          <ProgressBar
            :value="Math.min(100, parseFloat(getPredictionDensity(job)) * 20)"
            :showValue="false"
            class="progress-bar info"
          />
        </div>
      </div>
    </div>

    <Divider />

    <!-- Current Status -->
    <div class="job-status">
      <h5>Current Status</h5>
      <div class="status-display">
        <div class="status-icon">
          <i class="pi pi-spin pi-spinner" v-if="job.status === 'RUNNING'"></i>
          <i class="pi pi-check-circle text-green-500" v-else-if="job.status === 'COMPLETED'"></i>
          <i class="pi pi-times-circle text-red-500" v-else-if="job.status === 'FAILED'"></i>
          <i class="pi pi-ban text-orange-500" v-else-if="job.status === 'CANCELLED'"></i>
          <i class="pi pi-clock text-blue-500" v-else></i>
        </div>
        <div class="status-content">
          <div class="status-message">
            {{ getStatusMessage(job) }}
          </div>
          <div class="status-time" v-if="getStatusTime(job)">
            {{ getStatusTime(job) }}
          </div>
        </div>
      </div>
    </div>

    <!-- Error Details -->
    <div v-if="job.errorMessage" class="job-error">
      <Divider />
      <h5>Error Details</h5>
      <div class="error-display">
        <Message severity="error" :closable="false">
          <div class="error-content">
            <div class="error-message">{{ job.errorMessage }}</div>
            <div class="error-actions">
              <Button
                label="Copy Error"
                icon="pi pi-copy"
                severity="secondary"
                size="small"
                @click="copyError"
              />
            </div>
          </div>
        </Message>
      </div>
    </div>

    <!-- Performance Metrics -->
    <div v-if="job.status === 'COMPLETED' || job.status === 'RUNNING'" class="performance-metrics">
      <Divider />
      <h5>Performance Metrics</h5>
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-icon">
            <i class="pi pi-clock"></i>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ getAverageProcessingTime(job) }}</div>
            <div class="metric-label">Avg. Processing Time</div>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon">
            <i class="pi pi-chart-line"></i>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ getProcessingRate(job) }}</div>
            <div class="metric-label">Articles/Min</div>
          </div>
        </div>
        <div class="metric-card" v-if="job.predictionsFound > 0">
          <div class="metric-icon">
            <i class="pi pi-search"></i>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ getPredictionRate(job) }}</div>
            <div class="metric-label">Predictions/Min</div>
          </div>
        </div>
        <div class="metric-card" v-if="job.status === 'RUNNING'">
          <div class="metric-icon">
            <i class="pi pi-forward"></i>
          </div>
          <div class="metric-content">
            <div class="metric-value">{{ getEstimatedCompletion(job) }}</div>
            <div class="metric-label">Est. Completion</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity Timeline -->
    <div class="activity-timeline">
      <Divider />
      <h5>Activity Timeline</h5>
      <Timeline :value="getTimelineEvents(job)" class="job-timeline">
        <template #marker="{ item }">
          <div class="timeline-marker" :class="item.type">
            <i :class="item.icon"></i>
          </div>
        </template>
        <template #content="{ item }">
          <div class="timeline-content">
            <div class="timeline-title">{{ item.title }}</div>
            <div class="timeline-description">{{ item.description }}</div>
            <div class="timeline-time">{{ item.time }}</div>
          </div>
        </template>
      </Timeline>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import ProgressBar from 'primevue/progressbar'
import Divider from 'primevue/divider'
import Message from 'primevue/message'
import Timeline from 'primevue/timeline'

interface Props {
  job: any
}

const props = defineProps<Props>()

const emit = defineEmits<{
  refresh: []
  cancel: [jobId: string]
  retry: [jobId: string]
  export: [jobId: string]
}>()

const toast = useToast()

// Computed for progress ring
const circumference = computed(() => 2 * Math.PI * 52)
const progressOffset = computed(() => {
  const progress = getProgressPercentage(props.job)
  return circumference.value - (progress / 100) * circumference.value
})

// Methods
const getStatusSeverity = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'RUNNING': return 'info'
    case 'FAILED': return 'danger'
    case 'CANCELLED': return 'warning'
    case 'PENDING': return 'secondary'
    default: return 'secondary'
  }
}

const getStatusClass = (status: string) => {
  return `job-status-${status.toLowerCase()}`
}

const getProgressPercentage = (job: any) => {
  if (!job.totalArticles || job.totalArticles === 0) return 0
  return Math.min(100, ((job.processedArticles || 0) / job.totalArticles) * 100)
}

const getArticleProgressPercentage = (job: any) => {
  return getProgressPercentage(job)
}

const getProgressColor = (status: string) => {
  switch (status) {
    case 'COMPLETED': return 'var(--green-500)'
    case 'RUNNING': return 'var(--blue-500)'
    case 'FAILED': return 'var(--red-500)'
    case 'CANCELLED': return 'var(--orange-500)'
    default: return 'var(--surface-border)'
  }
}

const getSuccessRate = (job: any) => {
  if (!job.totalArticles || job.totalArticles === 0) return 0
  return Math.round(((job.processedArticles || 0) / job.totalArticles) * 100)
}

const getPredictionDensity = (job: any) => {
  if (!job.processedArticles || job.processedArticles === 0) return '0.00'
  return ((job.predictionsFound || 0) / job.processedArticles).toFixed(2)
}

const getAverageProcessingTime = (job: any) => {
  if (!job.processedArticles || job.processedArticles === 0) return 'N/A'
  const durationMs = getDurationMs(job)
  if (!durationMs) return 'N/A'
  
  const avgMs = durationMs / job.processedArticles
  if (avgMs < 1000) {
    return `${Math.round(avgMs)}ms`
  } else {
    return `${(avgMs / 1000).toFixed(1)}s`
  }
}

const getProcessingRate = (job: any) => {
  if (!job.processedArticles || job.processedArticles === 0) return '0'
  const durationMs = getDurationMs(job)
  if (!durationMs) return '0'
  
  const rate = (job.processedArticles / (durationMs / 60000))
  return rate.toFixed(1)
}

const getPredictionRate = (job: any) => {
  if (!job.predictionsFound || job.predictionsFound === 0) return '0'
  const durationMs = getDurationMs(job)
  if (!durationMs) return '0'
  
  const rate = (job.predictionsFound / (durationMs / 60000))
  return rate.toFixed(1)
}

const getEstimatedCompletion = (job: any) => {
  if (job.status !== 'RUNNING' || !job.processedArticles || job.processedArticles === 0) {
    return 'N/A'
  }
  
  const durationMs = getDurationMs(job)
  if (!durationMs) return 'N/A'
  
  const rate = job.processedArticles / (durationMs / 1000) // articles per second
  const remaining = job.totalArticles - job.processedArticles
  const estimatedSeconds = remaining / rate
  
  const now = new Date()
  const estimated = new Date(now.getTime() + estimatedSeconds * 1000)
  
  return estimated.toLocaleTimeString()
}

const getDurationMs = (job: any) => {
  if (!job.startedAt) return null
  const start = new Date(job.startedAt)
  const end = job.completedAt ? new Date(job.completedAt) : new Date()
  return end.getTime() - start.getTime()
}

const getStatusMessage = (job: any) => {
  switch (job.status) {
    case 'PENDING':
      return 'Job is queued and waiting to start'
    case 'RUNNING':
      return `Processing articles... ${job.processedArticles || 0} of ${job.totalArticles || 0} completed`
    case 'COMPLETED':
      return `Job completed successfully. Found ${job.predictionsFound || 0} predictions.`
    case 'FAILED':
      return 'Job failed during processing'
    case 'CANCELLED':
      return 'Job was cancelled by user'
    default:
      return 'Unknown status'
  }
}

const getStatusTime = (job: any) => {
  switch (job.status) {
    case 'RUNNING':
      return `Started ${formatRelativeTime(job.startedAt)}`
    case 'COMPLETED':
    case 'FAILED':
    case 'CANCELLED':
      return job.completedAt ? `Finished ${formatRelativeTime(job.completedAt)}` : null
    default:
      return job.startedAt ? `Created ${formatRelativeTime(job.startedAt)}` : null
  }
}

const getTimelineEvents = (job: any) => {
  const events = []
  
  // Job created
  events.push({
    type: 'info',
    icon: 'pi pi-plus',
    title: 'Job Created',
    description: `Analysis job created for ${job.totalArticles} articles`,
    time: formatDateTime(job.startedAt)
  })
  
  // Job started (if different from created)
  if (job.status !== 'PENDING') {
    events.push({
      type: 'info',
      icon: 'pi pi-play',
      title: 'Processing Started',
      description: `Began processing articles using ${job.analysisType} analysis`,
      time: formatDateTime(job.startedAt)
    })
  }
  
  // Progress milestones
  if (job.processedArticles > 0) {
    const milestones = [25, 50, 75]
    const currentProgress = getProgressPercentage(job)
    
    milestones.forEach(milestone => {
      if (currentProgress >= milestone) {
        events.push({
          type: 'success',
          icon: 'pi pi-check',
          title: `${milestone}% Complete`,
          description: `Processed ${Math.round((milestone / 100) * job.totalArticles)} articles`,
          time: 'Estimated time' // In real implementation, track actual times
        })
      }
    })
  }
  
  // Job completion
  if (job.completedAt) {
    const completionEvent = {
      time: formatDateTime(job.completedAt)
    }
    
    switch (job.status) {
      case 'COMPLETED':
        events.push({
          ...completionEvent,
          type: 'success',
          icon: 'pi pi-check-circle',
          title: 'Job Completed',
          description: `Successfully processed all articles. Found ${job.predictionsFound} predictions.`
        })
        break
      case 'FAILED':
        events.push({
          ...completionEvent,
          type: 'error',
          icon: 'pi pi-times-circle',
          title: 'Job Failed',
          description: job.errorMessage || 'Job failed due to an error'
        })
        break
      case 'CANCELLED':
        events.push({
          ...completionEvent,
          type: 'warning',
          icon: 'pi pi-ban',
          title: 'Job Cancelled',
          description: 'Job was cancelled by user request'
        })
        break
    }
  }
  
  return events.reverse() // Show most recent first
}

const canCancel = (job: any) => {
  return job.status === 'RUNNING' || job.status === 'PENDING'
}

const canRetry = (job: any) => {
  return job.status === 'FAILED' || job.status === 'CANCELLED'
}

const canExport = (job: any) => {
  return job.status === 'COMPLETED' && job.predictionsFound > 0
}

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString()
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

const formatDuration = (job: any) => {
  const durationMs = getDurationMs(job)
  if (!durationMs) return '-'
  
  const seconds = Math.floor(durationMs / 1000)
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

const formatElapsedTime = (job: any) => {
  return formatDuration(job)
}

const copyJobId = async (jobId: string) => {
  try {
    await navigator.clipboard.writeText(jobId)
    toast.add({
      severity: 'success',
      summary: 'Copied',
      detail: 'Job ID copied to clipboard',
      life: 2000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Copy Failed',
      detail: 'Failed to copy job ID',
      life: 3000
    })
  }
}

const copyError = async () => {
  try {
    await navigator.clipboard.writeText(props.job.errorMessage)
    toast.add({
      severity: 'success',
      summary: 'Copied',
      detail: 'Error message copied to clipboard',
      life: 2000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Copy Failed',
      detail: 'Failed to copy error message',
      life: 3000
    })
  }
}
</script>

<style lang="scss" scoped>
.analysis-job-detail {
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

  h4 {
    margin: 0;
    font-family: monospace;
    color: var(--text-color);
    font-size: 1rem;
  }
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

  label {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--text-color-secondary);
  }

  span {
    font-size: 0.875rem;
    color: var(--text-color);
  }

  .metadata-value {
    display: flex;
    align-items: center;
    gap: 0.5rem;

    code {
      background: var(--surface-section);
      padding: 0.25rem 0.5rem;
      border-radius: 4px;
      font-size: 0.75rem;
    }
  }
}

.job-progress h5,
.job-status h5,
.performance-metrics h5,
.activity-timeline h5 {
  margin: 0 0 1rem 0;
  color: var(--text-color);
  font-size: 1.1rem;
}

.progress-overview {
  display: flex;
  gap: 2rem;
  margin-bottom: 1.5rem;
  align-items: center;
}

.progress-circle {
  .circle-container {
    position: relative;
    display: inline-block;
  }

  .progress-ring {
    transform: rotate(-90deg);
  }

  .progress-ring-background {
    opacity: 0.3;
  }

  .progress-ring-progress {
    transition: stroke-dashoffset 0.5s ease-in-out;
  }

  .progress-text {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    text-align: center;

    .progress-percentage {
      display: block;
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--text-color);
    }

    .progress-label {
      display: block;
      font-size: 0.75rem;
      color: var(--text-color-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  }
}

.progress-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 1rem;
  flex: 1;

  .stat-item {
    text-align: center;
    padding: 1rem;
    background: var(--surface-ground);
    border-radius: var(--border-radius);
    border: 1px solid var(--surface-border);

    .stat-value {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--primary-color);
      margin-bottom: 0.25rem;
    }

    .stat-label {
      font-size: 0.75rem;
      color: var(--text-color-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  }
}

.progress-bars {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

  &.success :deep(.p-progressbar-value) {
    background: var(--green-500);
  }

  &.info :deep(.p-progressbar-value) {
    background: var(--blue-500);
  }
}

.status-display {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: var(--border-radius);
  border-left: 4px solid var(--primary-color);

  .status-icon {
    font-size: 1.5rem;
    flex-shrink: 0;
    margin-top: 0.25rem;
  }

  .status-content {
    flex: 1;

    .status-message {
      font-size: 0.875rem;
      color: var(--text-color);
      margin-bottom: 0.5rem;
    }

    .status-time {
      font-size: 0.75rem;
      color: var(--text-color-secondary);
    }
  }
}

.error-display {
  margin-top: 0.5rem;

  .error-content {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;

    .error-message {
      flex: 1;
      font-size: 0.875rem;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .error-actions {
      flex-shrink: 0;
    }
  }
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.metric-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--surface-ground);
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);

  .metric-icon {
    font-size: 1.5rem;
    color: var(--primary-color);
    flex-shrink: 0;
  }

  .metric-content {
    flex: 1;

    .metric-value {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text-color);
      margin-bottom: 0.25rem;
    }

    .metric-label {
      font-size: 0.75rem;
      color: var(--text-color-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  }
}

.job-timeline {
  :deep(.p-timeline-event-content) {
    padding-left: 1rem;
  }
}

.timeline-marker {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 0.875rem;

  &.info {
    background: var(--blue-500);
  }

  &.success {
    background: var(--green-500);
  }

  &.warning {
    background: var(--orange-500);
  }

  &.error {
    background: var(--red-500);
  }
}

.timeline-content {
  .timeline-title {
    font-weight: 600;
    color: var(--text-color);
    margin-bottom: 0.25rem;
  }

  .timeline-description {
    font-size: 0.875rem;
    color: var(--text-color-secondary);
    margin-bottom: 0.5rem;
  }

  .timeline-time {
    font-size: 0.75rem;
    color: var(--text-color-secondary);
  }
}

.job-status-running {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

// Responsive design
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

  .progress-overview {
    flex-direction: column;
    gap: 1rem;
  }

  .progress-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .metrics-grid {
    grid-template-columns: 1fr;
  }

  .error-content {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>