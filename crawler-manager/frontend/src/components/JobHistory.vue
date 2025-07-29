<template>
  <div class="job-history">
    <div class="job-history-header">
      <h3>Job History</h3>
      <div class="job-history-actions">
        <Button
          icon="pi pi-refresh"
          severity="secondary"
          size="small"
          @click="refreshJobs"
          :loading="isRefreshing"
          v-tooltip.top="'Refresh job history'"
        />
        <Button
          icon="pi pi-filter"
          severity="secondary"
          size="small"
          @click="showFilterDialog = true"
          v-tooltip.top="'Filter jobs'"
        />
      </div>
    </div>

    <div class="job-history-content">
      <DataTable
        :value="filteredJobs"
        :loading="isLoading"
        :paginator="showPagination"
        :rows="pageSize"
        :totalRecords="totalJobs"
        :lazy="showPagination"
        @page="onPageChange"
        @sort="onSort"
        sortMode="single"
        :sortField="sortField"
        :sortOrder="sortOrder"
        size="small"
        stripedRows
        responsiveLayout="scroll"
        :emptyMessage="emptyMessage"
        class="job-history-table"
      >
        <Column field="startTime" header="Started" sortable>
          <template #body="{ data }">
            <span class="job-start-time">
              {{ formatDateTime(data.startTime) }}
            </span>
          </template>
        </Column>

        <Column field="status" header="Status" sortable>
          <template #body="{ data }">
            <Badge
              :value="data.status"
              :severity="getStatusSeverity(data.status)"
              :class="getStatusClass(data.status)"
            />
          </template>
        </Column>

        <Column field="articlesProcessed" header="Articles" sortable>
          <template #body="{ data }">
            <div class="job-articles">
              <span class="articles-processed">{{ data.articlesProcessed }}</span>
              <span v-if="data.articlesFailed > 0" class="articles-failed">
                ({{ data.articlesFailed }} failed)
              </span>
            </div>
          </template>
        </Column>

        <Column field="successRate" header="Success Rate" sortable>
          <template #body="{ data }">
            <div class="success-rate">
              <ProgressBar
                :value="data.successRate"
                :showValue="false"
                class="success-rate-bar"
              />
              <span class="success-rate-text">{{ data.successRate.toFixed(1) }}%</span>
            </div>
          </template>
        </Column>

        <Column field="durationMs" header="Duration" sortable>
          <template #body="{ data }">
            <span class="job-duration">
              {{ formatDuration(data) }}
            </span>
          </template>
        </Column>

        <Column field="currentActivity" header="Activity" v-if="showActivityColumn">
          <template #body="{ data }">
            <span class="job-activity" :title="data.currentActivity">
              {{ truncateActivity(data.currentActivity) }}
            </span>
          </template>
        </Column>

        <Column header="Actions" :exportable="false">
          <template #body="{ data }">
            <div class="job-actions">
              <Button
                icon="pi pi-eye"
                severity="info"
                size="small"
                text
                @click="showJobDetails(data)"
                v-tooltip.top="'View details'"
              />
              <Button
                v-if="data.status === 'RUNNING'"
                icon="pi pi-times"
                severity="danger"
                size="small"
                text
                @click="cancelJob(data)"
                v-tooltip.top="'Cancel job'"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Job Detail Modal -->
    <Dialog
      v-model:visible="showDetailModal"
      :header="`Job Details - ${selectedJob?.jobId}`"
      modal
      :style="{ width: '50rem' }"
      :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    >
      <JobDetailModal
        v-if="selectedJob"
        :job="selectedJob"
        @refresh="refreshJobDetails"
        @cancel="cancelJobFromModal"
      />
    </Dialog>

    <!-- Filter Dialog -->
    <Dialog
      v-model:visible="showFilterDialog"
      header="Filter Jobs"
      modal
      :style="{ width: '30rem' }"
      :breakpoints="{ '1199px': '75vw', '575px': '90vw' }"
    >
      <div class="filter-form">
        <div class="field">
          <label for="status-filter">Status</label>
          <MultiSelect
            id="status-filter"
            v-model="filterOptions.status"
            :options="statusOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select statuses"
            class="w-full"
          />
        </div>

        <div class="field">
          <label for="date-from">Date From</label>
          <Calendar
            id="date-from"
            v-model="filterOptions.dateFrom"
            showIcon
            dateFormat="yy-mm-dd"
            class="w-full"
          />
        </div>

        <div class="field">
          <label for="date-to">Date To</label>
          <Calendar
            id="date-to"
            v-model="filterOptions.dateTo"
            showIcon
            dateFormat="yy-mm-dd"
            class="w-full"
          />
        </div>

        <div class="field">
          <label for="search-term">Search</label>
          <InputText
            id="search-term"
            v-model="filterOptions.searchTerm"
            placeholder="Search in job ID or activity"
            class="w-full"
          />
        </div>
      </div>

      <template #footer>
        <Button
          label="Clear"
          severity="secondary"
          @click="clearFilters"
        />
        <Button
          label="Apply"
          @click="applyFilters"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ApiService } from '@/services/api'
import { eventBus } from '@/services/websocket'
import type { JobStatus, JobHistoryFilter, JobUpdateMessage } from '@/types/job'
import JobDetailModal from './JobDetailModal.vue'

interface Props {
  crawlerId: string
  showPagination?: boolean
  pageSize?: number
  showActivityColumn?: boolean
  autoRefresh?: boolean
  refreshInterval?: number
}

const props = withDefaults(defineProps<Props>(), {
  showPagination: false,
  pageSize: 10,
  showActivityColumn: true,
  autoRefresh: true,
  refreshInterval: 30000 // 30 seconds
})

// State
const jobs = ref<JobStatus[]>([])
const isLoading = ref(false)
const isRefreshing = ref(false)
const error = ref<string | null>(null)
const currentPage = ref(0)
const totalJobs = ref(0)
const sortField = ref<string>('startTime')
const sortOrder = ref<number>(-1) // -1 for desc, 1 for asc

// Filter state
const showFilterDialog = ref(false)
const filterOptions = ref<JobHistoryFilter>({
  status: [],
  dateFrom: undefined,
  dateTo: undefined,
  searchTerm: ''
})
const activeFilters = ref<JobHistoryFilter>({})

// Detail modal state
const showDetailModal = ref(false)
const selectedJob = ref<JobStatus | null>(null)

// Auto refresh
let refreshTimer: NodeJS.Timeout | null = null
let wsUnsubscribers: (() => void)[] = []

// Status options for filter
const statusOptions = [
  { label: 'Running', value: 'RUNNING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

// Computed
const filteredJobs = computed(() => {
  let filtered = [...jobs.value]

  // Apply status filter
  if (activeFilters.value.status && activeFilters.value.status.length > 0) {
    filtered = filtered.filter(job => activeFilters.value.status!.includes(job.status))
  }

  // Apply date range filter
  if (activeFilters.value.dateFrom) {
    const fromDate = new Date(activeFilters.value.dateFrom)
    filtered = filtered.filter(job => new Date(job.startTime) >= fromDate)
  }

  if (activeFilters.value.dateTo) {
    const toDate = new Date(activeFilters.value.dateTo)
    toDate.setHours(23, 59, 59, 999) // End of day
    filtered = filtered.filter(job => new Date(job.startTime) <= toDate)
  }

  // Apply search filter
  if (activeFilters.value.searchTerm) {
    const searchTerm = activeFilters.value.searchTerm.toLowerCase()
    filtered = filtered.filter(job =>
      job.jobId.toLowerCase().includes(searchTerm) ||
      (job.currentActivity && job.currentActivity.toLowerCase().includes(searchTerm))
    )
  }

  return filtered
})

const emptyMessage = computed(() => {
  if (isLoading.value) return 'Loading jobs...'
  if (error.value) return `Error: ${error.value}`
  if (Object.keys(activeFilters.value).length > 0) return 'No jobs match the current filters'
  return 'No jobs found'
})

// Methods
const loadJobs = async (page: number = 0) => {
  try {
    isLoading.value = true
    error.value = null

    let jobData: JobStatus[]
    
    if (props.showPagination) {
      jobData = await ApiService.getJobHistory(props.crawlerId, page, props.pageSize)
    } else {
      jobData = await ApiService.getRecentJobs(props.crawlerId)
    }

    jobs.value = jobData
    totalJobs.value = jobData.length
    currentPage.value = page
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Failed to load jobs'
    console.error('Failed to load jobs:', err)
  } finally {
    isLoading.value = false
  }
}

const refreshJobs = async () => {
  try {
    isRefreshing.value = true
    await loadJobs(currentPage.value)
  } finally {
    isRefreshing.value = false
  }
}

const onPageChange = (event: any) => {
  loadJobs(event.page)
}

const onSort = (event: any) => {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder
  
  // Sort the current jobs array
  jobs.value.sort((a, b) => {
    const aVal = getNestedValue(a, event.sortField)
    const bVal = getNestedValue(b, event.sortField)
    
    if (aVal < bVal) return -1 * event.sortOrder
    if (aVal > bVal) return 1 * event.sortOrder
    return 0
  })
}

const getNestedValue = (obj: any, path: string): any => {
  return path.split('.').reduce((o, p) => o && o[p], obj)
}

const showJobDetails = async (job: JobStatus) => {
  try {
    // Load fresh job details
    const jobDetails = await ApiService.getJobDetails(job.jobId)
    selectedJob.value = jobDetails
    showDetailModal.value = true
  } catch (err) {
    console.error('Failed to load job details:', err)
    // Show basic details if API call fails
    selectedJob.value = job
    showDetailModal.value = true
  }
}

const refreshJobDetails = async () => {
  if (selectedJob.value) {
    try {
      const jobDetails = await ApiService.getJobDetails(selectedJob.value.jobId)
      selectedJob.value = jobDetails
    } catch (err) {
      console.error('Failed to refresh job details:', err)
    }
  }
}

const cancelJob = async (job: JobStatus) => {
  try {
    await ApiService.cancelJob(job.jobId)
    // Refresh the job list to show updated status
    await refreshJobs()
  } catch (err) {
    console.error('Failed to cancel job:', err)
    error.value = err instanceof Error ? err.message : 'Failed to cancel job'
  }
}

const cancelJobFromModal = async (jobId: string) => {
  try {
    await ApiService.cancelJob(jobId)
    await refreshJobDetails()
    await refreshJobs()
  } catch (err) {
    console.error('Failed to cancel job:', err)
  }
}

const applyFilters = () => {
  activeFilters.value = { ...filterOptions.value }
  showFilterDialog.value = false
}

const clearFilters = () => {
  filterOptions.value = {
    status: [],
    dateFrom: undefined,
    dateTo: undefined,
    searchTerm: ''
  }
  activeFilters.value = {}
  showFilterDialog.value = false
}

// Utility methods
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

const formatDateTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString()
}

const formatDuration = (job: JobStatus) => {
  const duration = job.durationMs || job.elapsedTimeMs
  if (!duration) return '-'
  
  const seconds = Math.floor(duration / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  
  if (hours > 0) {
    return `${hours}h ${minutes % 60}m`
  } else if (minutes > 0) {
    return `${minutes}m ${seconds % 60}s`
  } else {
    return `${seconds}s`
  }
}

const truncateActivity = (activity: string | undefined) => {
  if (!activity) return '-'
  return activity.length > 30 ? activity.substring(0, 30) + '...' : activity
}

// WebSocket setup
const setupWebSocket = () => {
  // Clean up existing subscriptions
  wsUnsubscribers.forEach(unsub => unsub())
  wsUnsubscribers = []

  // Subscribe to job updates for this crawler
  wsUnsubscribers.push(
    eventBus.on('job:updated', (message: JobUpdateMessage) => {
      if (message.crawlerId === props.crawlerId) {
        handleJobUpdate(message)
      }
    })
  )
}

const handleJobUpdate = (message: JobUpdateMessage) => {
  const jobIndex = jobs.value.findIndex(job => job.jobId === message.jobId)
  
  if (jobIndex !== -1) {
    // Update existing job
    const updatedJob = { ...jobs.value[jobIndex] }
    updatedJob.status = message.status
    updatedJob.lastUpdated = message.timestamp
    
    if (message.progress) {
      updatedJob.articlesProcessed = message.progress.articlesProcessed
      updatedJob.articlesSkipped = message.progress.articlesSkipped
      updatedJob.articlesFailed = message.progress.articlesFailed
      updatedJob.currentActivity = message.progress.currentActivity
      updatedJob.totalArticlesAttempted = 
        message.progress.articlesProcessed + 
        message.progress.articlesSkipped + 
        message.progress.articlesFailed
      
      if (updatedJob.totalArticlesAttempted > 0) {
        updatedJob.successRate = (message.progress.articlesProcessed / updatedJob.totalArticlesAttempted) * 100
      }
    }
    
    jobs.value[jobIndex] = updatedJob
    
    // Update selected job if it's the same
    if (selectedJob.value && selectedJob.value.jobId === message.jobId) {
      selectedJob.value = updatedJob
    }
  } else if (message.type === 'job.started') {
    // Add new job to the beginning of the list
    const newJob: JobStatus = {
      jobId: message.jobId,
      crawlerId: message.crawlerId,
      status: message.status,
      startTime: message.timestamp,
      articlesProcessed: 0,
      articlesSkipped: 0,
      articlesFailed: 0,
      elapsedTimeMs: 0,
      successRate: 0,
      totalArticlesAttempted: 0,
      currentActivity: 'Starting...'
    }
    
    jobs.value.unshift(newJob)
    
    // Keep only the most recent jobs if not using pagination
    if (!props.showPagination && jobs.value.length > 10) {
      jobs.value = jobs.value.slice(0, 10)
    }
  }
}

// Auto refresh setup
const setupAutoRefresh = () => {
  if (props.autoRefresh && props.refreshInterval > 0) {
    refreshTimer = setInterval(() => {
      if (!isLoading.value && !showDetailModal.value) {
        refreshJobs()
      }
    }, props.refreshInterval)
  }
}

const cleanupAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// Lifecycle
onMounted(() => {
  loadJobs()
  setupWebSocket()
  setupAutoRefresh()
})

onUnmounted(() => {
  wsUnsubscribers.forEach(unsub => unsub())
  cleanupAutoRefresh()
})

// Watch for crawler ID changes
watch(() => props.crawlerId, () => {
  loadJobs()
  setupWebSocket()
})
</script>

<style scoped>
.job-history {
  background: var(--surface-card);
  border-radius: var(--border-radius);
  padding: 1rem;
}

.job-history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.job-history-header h3 {
  margin: 0;
  color: var(--text-color);
}

.job-history-actions {
  display: flex;
  gap: 0.5rem;
}

.job-history-content {
  min-height: 200px;
}

.job-history-table {
  font-size: 0.875rem;
}

.job-start-time {
  font-size: 0.8rem;
  color: var(--text-color-secondary);
}

.job-articles {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.articles-processed {
  font-weight: 600;
}

.articles-failed {
  font-size: 0.75rem;
  color: var(--red-500);
}

.success-rate {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.success-rate-bar {
  flex: 1;
  height: 0.5rem;
}

.success-rate-text {
  font-size: 0.75rem;
  font-weight: 600;
  min-width: 3rem;
  text-align: right;
}

.job-duration {
  font-family: monospace;
  font-size: 0.8rem;
}

.job-activity {
  font-size: 0.8rem;
  color: var(--text-color-secondary);
}

.job-actions {
  display: flex;
  gap: 0.25rem;
}

.job-status-running {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.filter-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field label {
  font-weight: 600;
  color: var(--text-color);
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .job-history-header {
    flex-direction: column;
    gap: 1rem;
    align-items: stretch;
  }
  
  .job-history-actions {
    justify-content: center;
  }
  
  .success-rate {
    flex-direction: column;
    gap: 0.25rem;
  }
  
  .success-rate-text {
    text-align: center;
  }
}
</style>