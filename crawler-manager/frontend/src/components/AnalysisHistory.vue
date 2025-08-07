<template>
  <div class="analysis-history">
    <!-- Header with actions -->
    <div class="history-header">
      <div class="header-content">
        <h3>Analysis History</h3>
        <p class="header-description">View and manage prediction analysis jobs</p>
      </div>
      <div class="header-actions">
        <Button 
          icon="pi pi-refresh" 
          label="Refresh"
          @click="refreshHistory"
          :loading="isRefreshing"
          severity="secondary"
          size="small"
        />
        <Button 
          icon="pi pi-filter" 
          label="Filter"
          @click="showFilterDialog = true"
          severity="secondary"
          size="small"
        />
      </div>
    </div>

    <!-- Quick filters -->
    <div class="quick-filters">
      <div class="filter-chips">
        <Chip 
          v-for="status in quickFilterStatuses"
          :key="status.value"
          :label="`${status.label} (${getStatusCount(status.value)})`"
          :class="{ 'active': activeQuickFilter === status.value }"
          @click="toggleQuickFilter(status.value)"
          :removable="activeQuickFilter === status.value"
          @remove="clearQuickFilter"
        />
      </div>
      <div class="filter-summary" v-if="hasActiveFilters">
        <span class="filter-count">{{ filteredJobs.length }} of {{ analysisJobs.length }} jobs</span>
        <Button 
          icon="pi pi-times"
          label="Clear All"
          @click="clearAllFilters"
          severity="secondary"
          size="small"
          text
        />
      </div>
    </div>

    <!-- Analysis Jobs Table -->
    <DataTable 
      :value="paginatedJobs"
      :loading="isLoading"
      :paginator="true"
      :rows="pageSize"
      :totalRecords="filteredJobs.length"
      :lazy="false"
      v-model:first="first"
      sortField="startedAt"
      :sortOrder="-1"
      class="analysis-history-table"
      responsiveLayout="scroll"
      :emptyMessage="emptyMessage"
      stripedRows
    >
      <Column field="jobId" header="Job ID" sortable>
        <template #body="{ data }">
          <div class="job-id-cell">
            <code class="job-id">{{ truncateJobId(data.jobId) }}</code>
            <Button 
              icon="pi pi-copy"
              class="p-button-text p-button-sm copy-btn"
              @click="copyJobId(data.jobId)"
              v-tooltip="'Copy full Job ID'"
            />
          </div>
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
      
      <Column field="startedAt" header="Started" sortable>
        <template #body="{ data }">
          <div class="datetime-cell">
            <span class="date">{{ formatDate(data.startedAt) }}</span>
            <span class="time">{{ formatTime(data.startedAt) }}</span>
          </div>
        </template>
      </Column>
      
      <Column field="completedAt" header="Completed" sortable>
        <template #body="{ data }">
          <div class="datetime-cell" v-if="data.completedAt">
            <span class="date">{{ formatDate(data.completedAt) }}</span>
            <span class="time">{{ formatTime(data.completedAt) }}</span>
          </div>
          <span v-else class="text-muted">-</span>
        </template>
      </Column>
      
      <Column field="duration" header="Duration" sortable>
        <template #body="{ data }">
          <span class="duration">{{ formatDuration(data) }}</span>
        </template>
      </Column>
      
      <Column field="totalArticles" header="Articles" sortable>
        <template #body="{ data }">
          <div class="article-progress">
            <span class="progress-text">
              {{ data.processedArticles || 0 }} / {{ data.totalArticles }}
            </span>
            <ProgressBar 
              v-if="data.status === 'RUNNING'"
              :value="getProgressPercentage(data)"
              :showValue="false"
              class="mini-progress"
            />
          </div>
        </template>
      </Column>
      
      <Column field="predictionsFound" header="Predictions" sortable>
        <template #body="{ data }">
          <div class="predictions-cell">
            <Badge 
              v-if="data.predictionsFound > 0"
              :value="data.predictionsFound"
              severity="success"
            />
            <span v-else class="text-muted">0</span>
          </div>
        </template>
      </Column>
      
      <Column field="analysisType" header="Type" sortable>
        <template #body="{ data }">
          <Badge 
            :value="data.analysisType"
            :severity="data.analysisType === 'llm' ? 'info' : 'secondary'"
          />
        </template>
      </Column>
      
      <Column header="Actions" :exportable="false">
        <template #body="{ data }">
          <div class="action-buttons">
            <Button 
              icon="pi pi-eye"
              class="p-button-text p-button-sm"
              @click="viewJobDetails(data)"
              v-tooltip="'View Details'"
              severity="info"
            />
            <Button 
              icon="pi pi-download"
              class="p-button-text p-button-sm"
              @click="exportResults(data)"
              v-tooltip="'Export Results'"
              :disabled="!canExport(data)"
              severity="secondary"
            />
            <Button 
              icon="pi pi-refresh"
              class="p-button-text p-button-sm"
              @click="retryAnalysis(data)"
              v-tooltip="'Retry Analysis'"
              :disabled="!canRetry(data)"
              severity="warning"
            />
            <Button 
              icon="pi pi-times"
              class="p-button-text p-button-sm"
              @click="confirmCancelJob(data)"
              v-tooltip="'Cancel Analysis'"
              :disabled="!canCancel(data)"
              severity="danger"
            />
          </div>
        </template>
      </Column>
    </DataTable>

    <!-- Job Detail Modal -->
    <Dialog 
      v-model:visible="showDetailModal"
      :header="`Analysis Job Details - ${selectedJob?.jobId}`"
      :modal="true"
      :style="{ width: '90vw', maxWidth: '1200px' }"
      :maximizable="true"
      :closable="true"
    >
      <AnalysisJobDetail
        v-if="selectedJob"
        :job="selectedJob"
        @refresh="refreshJobDetails"
        @cancel="handleCancelFromModal"
        @retry="handleRetryFromModal"
        @export="handleExportFromModal"
      />
    </Dialog>

    <!-- Filter Dialog -->
    <Dialog
      v-model:visible="showFilterDialog"
      header="Filter Analysis Jobs"
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
          <label for="analysis-type-filter">Analysis Type</label>
          <MultiSelect
            id="analysis-type-filter"
            v-model="filterOptions.analysisType"
            :options="analysisTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Select types"
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
          <label for="min-predictions">Min Predictions</label>
          <InputNumber
            id="min-predictions"
            v-model="filterOptions.minPredictions"
            placeholder="0"
            :min="0"
            class="w-full"
          />
        </div>

        <div class="field">
          <label for="search-term">Search</label>
          <InputText
            id="search-term"
            v-model="filterOptions.searchTerm"
            placeholder="Search job ID or error message"
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

    <!-- Cancel Confirmation Dialog -->
    <Dialog
      v-model:visible="showCancelDialog"
      header="Cancel Analysis Job"
      modal
      :style="{ width: '25rem' }"
    >
      <div class="cancel-confirmation">
        <i class="pi pi-exclamation-triangle warning-icon"></i>
        <div class="confirmation-content">
          <p>Are you sure you want to cancel this analysis job?</p>
          <p class="job-info">Job ID: <code>{{ jobToCancel?.jobId }}</code></p>
          <p class="warning-text">This action cannot be undone.</p>
        </div>
      </div>
      
      <template #footer>
        <Button
          label="No"
          severity="secondary"
          @click="showCancelDialog = false"
        />
        <Button
          label="Yes, Cancel Job"
          severity="danger"
          @click="cancelJob"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Badge from 'primevue/badge'
import ProgressBar from 'primevue/progressbar'
import Dialog from 'primevue/dialog'
import MultiSelect from 'primevue/multiselect'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Calendar from 'primevue/calendar'
import Chip from 'primevue/chip'
import AnalysisJobDetail from './AnalysisJobDetail.vue'

interface Props {
  history?: any[]
  autoRefresh?: boolean
  refreshInterval?: number
  pageSize?: number
}

const props = withDefaults(defineProps<Props>(), {
  history: () => [],
  autoRefresh: true,
  refreshInterval: 30000,
  pageSize: 20
})

const emit = defineEmits<{
  'view-results': [job: any]
  'refresh': []
}>()

const toast = useToast()

// State
const isLoading = ref(false)
const isRefreshing = ref(false)
const analysisJobs = ref<any[]>([])
const showDetailModal = ref(false)
const showFilterDialog = ref(false)
const showCancelDialog = ref(false)
const selectedJob = ref<any>(null)
const jobToCancel = ref<any>(null)
const first = ref(0)

// Quick filter state
const activeQuickFilter = ref<string | null>(null)

// Filter state
const filterOptions = ref<{
  status: string[]
  analysisType: string[]
  dateFrom: Date | null
  dateTo: Date | null
  minPredictions: number | null
  searchTerm: string
}>({
  status: [],
  analysisType: [],
  dateFrom: null,
  dateTo: null,
  minPredictions: null,
  searchTerm: ''
})

const activeFilters = ref<typeof filterOptions.value>({
  status: [],
  analysisType: [],
  dateFrom: null,
  dateTo: null,
  minPredictions: null,
  searchTerm: ''
})

// Options
const statusOptions = [
  { label: 'Pending', value: 'PENDING' },
  { label: 'Running', value: 'RUNNING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' },
  { label: 'Cancelled', value: 'CANCELLED' }
]

const analysisTypeOptions = [
  { label: 'Mock', value: 'mock' },
  { label: 'LLM', value: 'llm' }
]

const quickFilterStatuses = [
  { label: 'Running', value: 'RUNNING' },
  { label: 'Completed', value: 'COMPLETED' },
  { label: 'Failed', value: 'FAILED' }
]

// Computed
const filteredJobs = computed(() => {
  let filtered = [...analysisJobs.value]

  // Apply quick filter
  if (activeQuickFilter.value) {
    filtered = filtered.filter(job => job.status === activeQuickFilter.value)
  }

  // Apply advanced filters
  if (activeFilters.value.status.length > 0) {
    filtered = filtered.filter(job => activeFilters.value.status.includes(job.status))
  }

  if (activeFilters.value.analysisType.length > 0) {
    filtered = filtered.filter(job => activeFilters.value.analysisType.includes(job.analysisType))
  }

  if (activeFilters.value.dateFrom) {
    const fromDate = new Date(activeFilters.value.dateFrom)
    filtered = filtered.filter(job => new Date(job.startedAt) >= fromDate)
  }

  if (activeFilters.value.dateTo) {
    const toDate = new Date(activeFilters.value.dateTo)
    toDate.setHours(23, 59, 59, 999)
    filtered = filtered.filter(job => new Date(job.startedAt) <= toDate)
  }

  if (activeFilters.value.minPredictions !== null && activeFilters.value.minPredictions >= 0) {
    filtered = filtered.filter(job => (job.predictionsFound || 0) >= (activeFilters.value.minPredictions || 0))
  }

  if (activeFilters.value.searchTerm) {
    const searchTerm = activeFilters.value.searchTerm.toLowerCase()
    filtered = filtered.filter(job =>
      job.jobId.toLowerCase().includes(searchTerm) ||
      (job.errorMessage && job.errorMessage.toLowerCase().includes(searchTerm))
    )
  }

  return filtered
})

const paginatedJobs = computed(() => {
  const start = first.value
  const end = start + props.pageSize
  return filteredJobs.value.slice(start, end)
})

const hasActiveFilters = computed(() => {
  return activeQuickFilter.value !== null ||
         activeFilters.value.status.length > 0 ||
         activeFilters.value.analysisType.length > 0 ||
         activeFilters.value.dateFrom !== null ||
         activeFilters.value.dateTo !== null ||
         activeFilters.value.minPredictions !== null ||
         activeFilters.value.searchTerm !== ''
})

const emptyMessage = computed(() => {
  if (isLoading.value) return 'Loading analysis jobs...'
  if (hasActiveFilters.value) return 'No jobs match the current filters'
  return 'No analysis jobs found'
})

// Methods
const loadAnalysisHistory = async () => {
  isLoading.value = true
  try {
    // Mock data - replace with actual API call
    analysisJobs.value = generateMockAnalysisJobs()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Load Failed',
      detail: 'Failed to load analysis history',
      life: 5000
    })
  } finally {
    isLoading.value = false
  }
}

const refreshHistory = async () => {
  isRefreshing.value = true
  try {
    await loadAnalysisHistory()
    emit('refresh')
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Analysis history refreshed',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to refresh history',
      life: 5000
    })
  } finally {
    isRefreshing.value = false
  }
}

const getStatusCount = (status: string) => {
  return analysisJobs.value.filter(job => job.status === status).length
}

const toggleQuickFilter = (status: string) => {
  if (activeQuickFilter.value === status) {
    activeQuickFilter.value = null
  } else {
    activeQuickFilter.value = status
  }
  first.value = 0 // Reset pagination
}

const clearQuickFilter = () => {
  activeQuickFilter.value = null
}

const clearAllFilters = () => {
  activeQuickFilter.value = null
  activeFilters.value = {
    status: [],
    analysisType: [],
    dateFrom: null,
    dateTo: null,
    minPredictions: null,
    searchTerm: ''
  }
  filterOptions.value = { ...activeFilters.value }
  first.value = 0
}

const applyFilters = () => {
  activeFilters.value = { ...filterOptions.value }
  showFilterDialog.value = false
  first.value = 0
}

const clearFilters = () => {
  filterOptions.value = {
    status: [],
    analysisType: [],
    dateFrom: null,
    dateTo: null,
    minPredictions: null,
    searchTerm: ''
  }
}

const viewJobDetails = async (job: any) => {
  selectedJob.value = job
  showDetailModal.value = true
}

const refreshJobDetails = async () => {
  if (selectedJob.value) {
    // Refresh the selected job details
    const updatedJob = analysisJobs.value.find(j => j.jobId === selectedJob.value.jobId)
    if (updatedJob) {
      selectedJob.value = { ...updatedJob }
    }
  }
}

const confirmCancelJob = (job: any) => {
  jobToCancel.value = job
  showCancelDialog.value = true
}

const cancelJob = async () => {
  if (!jobToCancel.value) return

  try {
    // Mock API call - replace with actual implementation
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // Update job status locally
    const jobIndex = analysisJobs.value.findIndex(j => j.jobId === jobToCancel.value.jobId)
    if (jobIndex !== -1) {
      analysisJobs.value[jobIndex].status = 'CANCELLED'
      analysisJobs.value[jobIndex].completedAt = new Date().toISOString()
    }

    toast.add({
      severity: 'success',
      summary: 'Job Cancelled',
      detail: `Analysis job ${jobToCancel.value.jobId} has been cancelled`,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Cancel Failed',
      detail: 'Failed to cancel analysis job',
      life: 5000
    })
  } finally {
    showCancelDialog.value = false
    jobToCancel.value = null
  }
}

const retryAnalysis = async (job: any) => {
  try {
    _unused(job) // Suppress unused parameter warning
    // Mock API call - replace with actual implementation
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    toast.add({
      severity: 'success',
      summary: 'Analysis Retried',
      detail: `Analysis job has been queued for retry`,
      life: 3000
    })
    
    await refreshHistory()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Retry Failed',
      detail: 'Failed to retry analysis',
      life: 5000
    })
  }
}

const exportResults = async (job: any) => {
  try {
    // Mock export - replace with actual implementation
    const results = generateMockResults(job)
    const csv = convertToCSV(results)
    downloadFile(csv, `analysis-${job.jobId}.csv`, 'text/csv')
    
    toast.add({
      severity: 'success',
      summary: 'Export Complete',
      detail: 'Results exported successfully',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Export Failed',
      detail: 'Failed to export results',
      life: 5000
    })
  }
}

const handleCancelFromModal = async (jobId: string) => {
  const job = analysisJobs.value.find(j => j.jobId === jobId)
  if (job) {
    jobToCancel.value = job
    showDetailModal.value = false
    showCancelDialog.value = true
  }
}

const handleRetryFromModal = async (jobId: string) => {
  const job = analysisJobs.value.find(j => j.jobId === jobId)
  if (job) {
    showDetailModal.value = false
    await retryAnalysis(job)
  }
}

const handleExportFromModal = async (jobId: string) => {
  const job = analysisJobs.value.find(j => j.jobId === jobId)
  if (job) {
    await exportResults(job)
  }
}

// Utility methods
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

const canExport = (job: any) => {
  return job.status === 'COMPLETED' && job.predictionsFound > 0
}

const canRetry = (job: any) => {
  return job.status === 'FAILED' || job.status === 'CANCELLED'
}

const canCancel = (job: any) => {
  return job.status === 'RUNNING' || job.status === 'PENDING'
}

const truncateJobId = (jobId: string) => {
  return jobId.length > 8 ? jobId.substring(0, 8) + '...' : jobId
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}

const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString()
}

const formatDuration = (job: any) => {
  if (!job.startedAt) return '-'
  
  const start = new Date(job.startedAt)
  const end = job.completedAt ? new Date(job.completedAt) : new Date()
  const diffMs = end.getTime() - start.getTime()
  
  const seconds = Math.floor(diffMs / 1000)
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

const convertToCSV = (data: any[]) => {
  const headers = ['Prediction', 'Rating', 'Confidence', 'Article Title', 'Author', 'Context']
  const rows = data.map(item => [
    `"${item.predictionText}"`,
    item.rating,
    item.confidenceScore,
    `"${item.article.title}"`,
    `"${item.article.author?.name || 'Unknown'}"`,
    `"${item.context}"`
  ])
  
  return [headers.join(','), ...rows.map(row => row.join(','))].join('\n')
}

const downloadFile = (content: string, filename: string, mimeType: string) => {
  const blob = new Blob([content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

const generateMockResults = (job: any) => {
  _unused(job) // Suppress unused parameter warning
  const mockPredictions = [
    'The market will experience significant volatility in the coming months',
    'Technology stocks are expected to outperform traditional sectors',
    'Interest rates will likely remain stable through the next quarter'
  ]

  return Array.from({ length: job.predictionsFound }, (_, i) => ({
    id: `pred-${job.jobId}-${i}`,
    predictionText: mockPredictions[Math.floor(Math.random() * mockPredictions.length)],
    rating: Math.floor(Math.random() * 5) + 1,
    confidenceScore: Math.random(),
    context: `Sample context for prediction ${i + 1}...`,
    article: {
      id: i + 1,
      title: `Sample Article ${i + 1}`,
      author: { name: 'Sample Author' }
    }
  }))
}

const generateMockAnalysisJobs = () => {
  const statuses = ['COMPLETED', 'RUNNING', 'FAILED', 'CANCELLED', 'PENDING']
  const analysisTypes = ['mock', 'llm']
  
  return Array.from({ length: 50 }, (_, i) => {
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    const startedAt = new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString()
    const completedAt = status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED' 
      ? new Date(new Date(startedAt).getTime() + Math.random() * 60 * 60 * 1000).toISOString()
      : null

    return {
      id: i + 1,
      jobId: `analysis-${Date.now()}-${i}`,
      status,
      startedAt,
      completedAt,
      totalArticles: Math.floor(Math.random() * 100) + 10,
      processedArticles: status === 'RUNNING' ? Math.floor(Math.random() * 50) : 
                        status === 'COMPLETED' ? Math.floor(Math.random() * 100) + 10 : 0,
      predictionsFound: status === 'COMPLETED' ? Math.floor(Math.random() * 50) : 0,
      analysisType: analysisTypes[Math.floor(Math.random() * analysisTypes.length)],
      errorMessage: status === 'FAILED' ? 'Sample error message for testing' : null
    }
  })
}

// Unused parameter helper
const _unused = (..._args: any[]) => {
  // This function helps suppress unused parameter warnings
}

// Watch for prop changes
watch(() => props.history, (newHistory) => {
  if (newHistory && newHistory.length > 0) {
    analysisJobs.value = newHistory
  }
}, { immediate: true })

// Auto refresh setup
let refreshTimer: NodeJS.Timeout | null = null

const setupAutoRefresh = () => {
  if (props.autoRefresh && props.refreshInterval > 0) {
    refreshTimer = setInterval(() => {
      if (!isLoading.value && !showDetailModal.value) {
        refreshHistory()
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
  if (!props.history || props.history.length === 0) {
    loadAnalysisHistory()
  }
  setupAutoRefresh()
})

// Cleanup on unmount
watch(() => props.autoRefresh, (newValue) => {
  if (newValue) {
    setupAutoRefresh()
  } else {
    cleanupAutoRefresh()
  }
})
</script>

<style lang="scss" scoped>
.analysis-history {
  background: var(--surface-card);
  border-radius: var(--border-radius);
  padding: 1.5rem;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
  gap: 1rem;

  .header-content {
    flex: 1;

    h3 {
      margin: 0 0 0.25rem 0;
      color: var(--text-color);
      font-size: 1.25rem;
      font-weight: 600;
    }

    .header-description {
      margin: 0;
      color: var(--text-color-secondary);
      font-size: 0.875rem;
    }
  }

  .header-actions {
    display: flex;
    gap: 0.5rem;
    flex-shrink: 0;
  }
}

.quick-filters {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  gap: 1rem;

  .filter-chips {
    display: flex;
    gap: 0.5rem;
    flex-wrap: wrap;

    :deep(.p-chip) {
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: var(--primary-color);
        color: var(--primary-color-text);
      }

      &.active {
        background: var(--primary-color);
        color: var(--primary-color-text);
      }
    }
  }

  .filter-summary {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.875rem;

    .filter-count {
      color: var(--text-color-secondary);
    }
  }
}

.analysis-history-table {
  :deep(.p-datatable-header) {
    background: var(--surface-section);
    border: 1px solid var(--surface-border);
  }

  :deep(.p-paginator) {
    background: var(--surface-section);
    border: 1px solid var(--surface-border);
    border-top: none;
  }
}

.job-id-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;

  .job-id {
    background: var(--surface-section);
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    font-size: 0.75rem;
    font-family: monospace;
  }

  .copy-btn {
    width: 1.5rem;
    height: 1.5rem;
    min-width: 1.5rem;
  }
}

.datetime-cell {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;

  .date {
    font-size: 0.875rem;
    color: var(--text-color);
  }

  .time {
    font-size: 0.75rem;
    color: var(--text-color-secondary);
  }
}

.duration {
  font-family: monospace;
  font-size: 0.875rem;
  color: var(--text-color);
}

.article-progress {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;

  .progress-text {
    font-size: 0.875rem;
    color: var(--text-color);
  }

  .mini-progress {
    height: 4px;
  }
}

.predictions-cell {
  text-align: center;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;

  :deep(.p-button) {
    width: 2rem;
    height: 2rem;
    min-width: 2rem;
  }
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

  .field {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;

    label {
      font-weight: 600;
      color: var(--text-color);
      font-size: 0.875rem;
    }
  }
}

.cancel-confirmation {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem 0;

  .warning-icon {
    font-size: 2rem;
    color: var(--orange-500);
    flex-shrink: 0;
  }

  .confirmation-content {
    flex: 1;

    p {
      margin: 0 0 0.5rem 0;
    }

    .job-info {
      font-size: 0.875rem;
      color: var(--text-color-secondary);

      code {
        background: var(--surface-section);
        padding: 0.125rem 0.25rem;
        border-radius: 2px;
        font-size: 0.75rem;
      }
    }

    .warning-text {
      font-size: 0.875rem;
      color: var(--text-color-secondary);
      font-style: italic;
    }
  }
}

// Responsive design
@media (max-width: 768px) {
  .analysis-history {
    padding: 1rem;
  }

  .history-header {
    flex-direction: column;
    align-items: stretch;

    .header-actions {
      justify-content: stretch;

      :deep(.p-button) {
        flex: 1;
      }
    }
  }

  .quick-filters {
    flex-direction: column;
    align-items: stretch;
    gap: 0.75rem;

    .filter-summary {
      justify-content: center;
    }
  }

  .action-buttons {
    flex-wrap: wrap;
  }

  .cancel-confirmation {
    flex-direction: column;
    text-align: center;
    gap: 0.75rem;
  }
}
</style>