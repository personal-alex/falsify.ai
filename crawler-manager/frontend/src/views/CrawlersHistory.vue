<template>
  <div class="crawlers-history">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">Crawlers History</h1>
        <p class="page-description">View crawler job history and details</p>
      </div>
      <div class="header-actions">
        <Button 
          icon="pi pi-refresh" 
          label="Refresh"
          @click="refreshHistory"
          :loading="isRefreshing"
          severity="secondary"
        />
        <Button 
          icon="pi pi-download" 
          label="Export"
          @click="exportHistory"
          severity="primary"
        />
      </div>
    </div>

    <!-- Filters -->
    <Card class="filters-card">
      <template #content>
        <div class="filters-grid">
          <div class="field">
            <label for="crawler-filter">Crawler</label>
            <Dropdown
              id="crawler-filter"
              v-model="filters.crawlerId"
              :options="crawlerOptions"
              optionLabel="name"
              optionValue="id"
              placeholder="All Crawlers"
              showClear
              @change="applyFilters"
            />
          </div>
          
          <div class="field">
            <label for="status-filter">Status</label>
            <Dropdown
              id="status-filter"
              v-model="filters.status"
              :options="statusOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="All Statuses"
              showClear
              @change="applyFilters"
            />
          </div>
          
          <div class="field">
            <label for="date-range">Date Range</label>
            <Calendar
              id="date-range"
              v-model="filters.dateRange"
              selectionMode="range"
              dateFormat="yy-mm-dd"
              showButtonBar
              @date-select="applyFilters"
            />
          </div>
          
          <div class="field">
            <label for="search">Search</label>
            <InputText
              id="search"
              v-model="filters.search"
              placeholder="Search jobs..."
              @input="debouncedSearch"
            />
          </div>
        </div>
      </template>
    </Card>

    <!-- Job History Table -->
    <Card class="history-table-card">
      <template #content>
        <JobHistory 
          :jobs="filteredJobs"
          :loading="isLoading"
          crawlerId="all"
          @view-details="viewJobDetails"
          @retry-job="retryJob"
          @cancel-job="cancelJob"
        />
      </template>
    </Card>

    <!-- Job Detail Modal -->
    <JobDetailModal
      v-model:visible="showJobDetail"
      :job="selectedJob || { id: '', status: 'pending', startedAt: '', crawlerId: '' }"
      @retry="retryJob"
      @cancel="cancelJob"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCrawlerStore } from '@/stores/crawler'
import { useToast } from 'primevue/usetoast'
import { debounce } from 'lodash-es'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Dropdown from 'primevue/dropdown'
import Calendar from 'primevue/calendar'
import InputText from 'primevue/inputtext'
import JobHistory from '@/components/JobHistory.vue'
import JobDetailModal from '@/components/JobDetailModal.vue'

const crawlerStore = useCrawlerStore()
const toast = useToast()

// Reactive state
const isLoading = ref(false)
const isRefreshing = ref(false)
const showJobDetail = ref(false)
const selectedJob = ref<any>(null)

// Filters
const filters = ref<{
  crawlerId: string | null
  status: string | null
  dateRange: Date[] | null
  search: string
}>({
  crawlerId: null,
  status: null,
  dateRange: null,
  search: ''
})

// Mock data - replace with actual store methods when available
const jobs = ref<any[]>([])
const crawlers = computed(() => crawlerStore.crawlers)

const crawlerOptions = computed(() => [
  ...crawlers.value.map(c => ({ id: c.id, name: c.name }))
])

const statusOptions = computed(() => [
  { label: 'Completed', value: 'completed' },
  { label: 'Running', value: 'running' },
  { label: 'Failed', value: 'failed' },
  { label: 'Cancelled', value: 'cancelled' },
  { label: 'Pending', value: 'pending' }
])

const filteredJobs = computed(() => {
  let filtered = [...jobs.value]

  // Filter by crawler
  if (filters.value.crawlerId) {
    filtered = filtered.filter(job => job.crawlerId === filters.value.crawlerId)
  }

  // Filter by status
  if (filters.value.status) {
    filtered = filtered.filter(job => job.status === filters.value.status)
  }

  // Filter by date range
  if (filters.value.dateRange && filters.value.dateRange.length === 2) {
    const [startDate, endDate] = filters.value.dateRange
    filtered = filtered.filter(job => {
      const jobDate = new Date(job.startedAt)
      return jobDate >= startDate && jobDate <= endDate
    })
  }

  // Filter by search
  if (filters.value.search) {
    const searchTerm = filters.value.search.toLowerCase()
    filtered = filtered.filter(job => 
      job.id.toLowerCase().includes(searchTerm) ||
      job.crawlerName?.toLowerCase().includes(searchTerm) ||
      job.url?.toLowerCase().includes(searchTerm)
    )
  }

  return filtered
})

// Methods
const refreshHistory = async () => {
  isRefreshing.value = true
  try {
    // Mock implementation - replace with actual store method
    await loadMockJobHistory()
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Job history refreshed successfully',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to refresh job history',
      life: 5000
    })
  } finally {
    isRefreshing.value = false
  }
}

const exportHistory = async () => {
  try {
    const data = filteredJobs.value.map(job => ({
      id: job.id,
      crawler: job.crawlerName,
      status: job.status,
      startedAt: job.startedAt,
      completedAt: job.completedAt,
      duration: job.duration,
      articlesFound: job.articlesFound,
      url: job.url
    }))

    const csv = convertToCSV(data)
    downloadCSV(csv, 'crawler-history.csv')

    toast.add({
      severity: 'success',
      summary: 'Export Complete',
      detail: 'Job history exported successfully',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Export Failed',
      detail: 'Failed to export job history',
      life: 5000
    })
  }
}

const convertToCSV = (data: any[]) => {
  if (data.length === 0) return ''
  
  const headers = Object.keys(data[0])
  const csvContent = [
    headers.join(','),
    ...data.map(row => 
      headers.map(header => {
        const value = row[header]
        return typeof value === 'string' && value.includes(',') 
          ? `"${value}"` 
          : value
      }).join(',')
    )
  ].join('\n')
  
  return csvContent
}

const downloadCSV = (csv: string, filename: string) => {
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  const url = URL.createObjectURL(blob)
  link.setAttribute('href', url)
  link.setAttribute('download', filename)
  link.style.visibility = 'hidden'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const applyFilters = () => {
  // Filters are applied automatically through computed property
}

const debouncedSearch = debounce(() => {
  applyFilters()
}, 300)

const viewJobDetails = (job: any) => {
  selectedJob.value = job
  showJobDetail.value = true
}

const retryJob = async (jobId: string) => {
  try {
    // Mock implementation - replace with actual store method
    await new Promise(resolve => setTimeout(resolve, 1000))
    toast.add({
      severity: 'success',
      summary: 'Job Retried',
      detail: `Job ${jobId} has been queued for retry`,
      life: 3000
    })
    await refreshHistory()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Retry Failed',
      detail: `Failed to retry job ${jobId}`,
      life: 5000
    })
  }
}

const cancelJob = async (jobId: string) => {
  try {
    // Mock implementation - replace with actual store method
    await new Promise(resolve => setTimeout(resolve, 500))
    toast.add({
      severity: 'success',
      summary: 'Job Cancelled',
      detail: `Job ${jobId} has been cancelled`,
      life: 3000
    })
    await refreshHistory()
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Cancel Failed',
      detail: `Failed to cancel job ${jobId}`,
      life: 5000
    })
  }
}

const loadMockJobHistory = async () => {
  // Mock data - replace with actual API call
  jobs.value = Array.from({ length: 20 }, (_, i) => ({
    id: `job-${i + 1}`,
    crawlerId: i % 2 === 0 ? 'drucker' : 'caspit',
    crawlerName: i % 2 === 0 ? 'Drucker Crawler' : 'Caspit Crawler',
    status: ['completed', 'running', 'failed', 'cancelled'][Math.floor(Math.random() * 4)],
    startedAt: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
    completedAt: Math.random() > 0.3 ? new Date(Date.now() - Math.random() * 6 * 24 * 60 * 60 * 1000).toISOString() : null,
    duration: Math.floor(Math.random() * 120) + 30, // 30-150 minutes
    articlesFound: Math.floor(Math.random() * 50) + 5,
    url: `https://example.com/article-${i + 1}`
  }))
}

// Initialize data
onMounted(async () => {
  isLoading.value = true
  try {
    await Promise.all([
      crawlerStore.loadCrawlerData(),
      loadMockJobHistory()
    ])
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Load Failed',
      detail: 'Failed to load history data',
      life: 5000
    })
  } finally {
    isLoading.value = false
  }
})
</script>

<style lang="scss" scoped>
.crawlers-history {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  gap: 2rem;

  .header-content {
    flex: 1;
  }

  .page-title {
    font-size: 2rem;
    font-weight: 600;
    color: var(--text-color);
    margin: 0 0 0.5rem 0;
  }

  .page-description {
    color: var(--text-color-secondary);
    margin: 0;
    font-size: 1.1rem;
  }

  .header-actions {
    display: flex;
    gap: 1rem;
    flex-shrink: 0;
  }
}

.filters-card {
  margin-bottom: 2rem;

  :deep(.p-card-content) {
    padding: 1.5rem;
  }
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  align-items: end;

  .field {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;

    label {
      font-weight: 600;
      color: var(--text-color);
      font-size: 0.875rem;
    }

    :deep(.p-inputtext),
    :deep(.p-dropdown),
    :deep(.p-calendar) {
      width: 100%;
    }
  }
}

.history-table-card {
  :deep(.p-card-content) {
    padding: 0;
  }
}

// Responsive design
@media (max-width: 768px) {
  .crawlers-history {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;

    .header-actions {
      justify-content: stretch;

      :deep(.p-button) {
        flex: 1;
      }
    }
  }

  .filters-grid {
    grid-template-columns: 1fr;
    gap: 1rem;
  }
}
</style>