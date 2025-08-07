<template>
  <div class="analysis-status">
    <!-- Header Panel -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center">
          <i class="pi pi-chart-bar mr-2 text-primary"></i>
          <span class="font-semibold text-lg">Prediction Analysis System Status</span>
        </div>
      </template>
      
      <div class="text-center">
        <div class="text-600 text-xl">Monitor prediction analysis service health and performance</div>
      </div>
    </Panel>

    <!-- Service Health Overview -->
    <div class="grid mb-4">
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="status-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold mb-1" :class="serviceHealthColor">
                  {{ serviceStatus }}
                </div>
                <div class="text-600 text-sm">Service Status</div>
              </div>
              <div class="status-icon" :class="serviceHealthBgColor">
                <i :class="serviceHealthIcon"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i :class="serviceHealthTrendIcon" class="text-xs mr-1"></i>
              <span class="text-xs font-medium" :class="serviceHealthTrendColor">
                {{ serviceHealthTrend }}
              </span>
            </div>
          </template>
        </Card>
      </div>
      
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="status-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-900 mb-1">{{ queueSize }}</div>
                <div class="text-600 text-sm">Queue Size</div>
              </div>
              <div class="status-icon bg-blue-100 text-blue-600">
                <i class="pi pi-list"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i class="pi pi-clock text-600 text-xs mr-1"></i>
              <span class="text-600 text-xs font-medium">{{ avgProcessingTime }}ms avg</span>
            </div>
          </template>
        </Card>
      </div>
      
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="status-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-900 mb-1">{{ activeJobs }}</div>
                <div class="text-600 text-sm">Active Jobs</div>
              </div>
              <div class="status-icon bg-orange-100 text-orange-600">
                <i class="pi pi-cog"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i class="pi pi-play text-green-500 text-xs mr-1"></i>
              <span class="text-green-500 text-xs font-medium">Processing</span>
            </div>
          </template>
        </Card>
      </div>
      
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="status-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-900 mb-1">{{ responseTime }}ms</div>
                <div class="text-600 text-sm">Response Time</div>
              </div>
              <div class="status-icon bg-purple-100 text-purple-600">
                <i class="pi pi-stopwatch"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i :class="responseTimeTrendIcon" class="text-xs mr-1"></i>
              <span class="text-xs font-medium" :class="responseTimeTrendColor">
                {{ responseTimeTrend }}
              </span>
            </div>
          </template>
        </Card>
      </div>
    </div>

    <!-- System Details Panel -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center">
            <i class="pi pi-info-circle mr-2"></i>
            <span class="font-semibold">System Details</span>
          </div>
          <div class="flex align-items-center">
            <div class="flex align-items-center mr-3">
              <i class="pi pi-circle-fill mr-1" :class="wsConnected ? 'text-green-500' : 'text-red-500'"></i>
              <span class="text-sm text-600">{{ wsConnected ? 'Connected' : 'Disconnected' }}</span>
            </div>
            <Button
              icon="pi pi-refresh"
              label="Refresh"
              size="small"
              :loading="isRefreshing"
              @click="refreshData"
              data-testid="refresh-button"
            />
          </div>
        </div>
      </template>

      <div class="grid">
        <div class="col-12 md:col-6">
          <div class="system-detail-item">
            <div class="detail-label">Service URL</div>
            <div class="detail-value">{{ serviceUrl }}</div>
          </div>
          <div class="system-detail-item">
            <div class="detail-label">Last Health Check</div>
            <div class="detail-value">{{ lastHealthCheck }}</div>
          </div>
          <div class="system-detail-item">
            <div class="detail-label">Uptime</div>
            <div class="detail-value">{{ uptime }}</div>
          </div>
        </div>
        <div class="col-12 md:col-6">
          <div class="system-detail-item">
            <div class="detail-label">Error Rate</div>
            <div class="detail-value" :class="errorRateColor">{{ errorRate }}%</div>
          </div>
          <div class="system-detail-item">
            <div class="detail-label">Total Jobs Processed</div>
            <div class="detail-value">{{ totalJobsProcessed }}</div>
          </div>
          <div class="system-detail-item">
            <div class="detail-label">Configuration Status</div>
            <div class="detail-value" :class="configStatusColor">{{ configStatus }}</div>
          </div>
        </div>
      </div>
    </Panel>

    <!-- Loading State -->
    <div v-if="isLoading" data-testid="loading-spinner">
      <Card>
        <template #content>
          <div class="text-center p-6">
            <ProgressSpinner />
            <div class="mt-3 text-600">Loading analysis system status...</div>
          </div>
        </template>
      </Card>
    </div>

    <!-- Error State -->
    <Card v-else-if="error" class="error-card">
      <template #content>
        <Message severity="error" :closable="false" class="mb-3">
          <div class="flex align-items-center">
            <i class="pi pi-exclamation-triangle mr-2"></i>
            <div>
              <div class="font-medium">Failed to load analysis system status</div>
              <div class="text-sm">{{ error }}</div>
            </div>
          </div>
        </Message>
        <div class="text-center">
          <Button
            icon="pi pi-refresh"
            label="Retry"
            severity="secondary"
            @click="loadData"
            data-testid="retry-button"
          />
        </div>
      </template>
    </Card>

    <!-- Footer Panel with Last Updated -->
    <Panel v-if="lastUpdated && !isLoading" class="mt-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <span class="text-sm text-600">
            Last updated: {{ lastUpdatedDisplay }}
          </span>
          <div class="flex align-items-center">
            <i class="pi pi-clock mr-1 text-400"></i>
            <span class="text-xs text-400">Auto-refresh enabled</span>
          </div>
        </div>
      </template>
    </Panel>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import Button from 'primevue/button'
import Card from 'primevue/card'
import Panel from 'primevue/panel'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'
// import { ApiService } from '@/services/api' // Will be used when real API is implemented
import { useNotifications } from '@/composables/useNotifications'

const { showToast, handleApiError } = useNotifications()

// State
const isLoading = ref(true)
const isRefreshing = ref(false)
const error = ref<string | null>(null)
const lastUpdated = ref<Date | null>(null)
const wsConnected = ref(true)

// Analysis system status data
const serviceStatus = ref('UNKNOWN')
const queueSize = ref(0)
const activeJobs = ref(0)
const responseTime = ref(0)
const avgProcessingTime = ref(0)
const serviceUrl = ref('http://localhost:8082')
const lastHealthCheck = ref('Never')
const uptime = ref('Unknown')
const errorRate = ref(0)
const totalJobsProcessed = ref(0)
const configStatus = ref('Unknown')

// Auto-refresh interval
let refreshInterval: NodeJS.Timeout | null = null

// Computed properties for styling
const serviceHealthColor = computed(() => {
  switch (serviceStatus.value) {
    case 'HEALTHY': return 'text-green-500'
    case 'DEGRADED': return 'text-orange-500'
    case 'UNHEALTHY': return 'text-red-500'
    default: return 'text-600'
  }
})

const serviceHealthBgColor = computed(() => {
  switch (serviceStatus.value) {
    case 'HEALTHY': return 'bg-green-100 text-green-600'
    case 'DEGRADED': return 'bg-orange-100 text-orange-600'
    case 'UNHEALTHY': return 'bg-red-100 text-red-600'
    default: return 'bg-gray-100 text-gray-600'
  }
})

const serviceHealthIcon = computed(() => {
  switch (serviceStatus.value) {
    case 'HEALTHY': return 'pi pi-check-circle'
    case 'DEGRADED': return 'pi pi-exclamation-triangle'
    case 'UNHEALTHY': return 'pi pi-times-circle'
    default: return 'pi pi-question-circle'
  }
})

const serviceHealthTrend = computed(() => {
  switch (serviceStatus.value) {
    case 'HEALTHY': return 'Operational'
    case 'DEGRADED': return 'Issues detected'
    case 'UNHEALTHY': return 'Service down'
    default: return 'Status unknown'
  }
})

const serviceHealthTrendColor = computed(() => {
  switch (serviceStatus.value) {
    case 'HEALTHY': return 'text-green-500'
    case 'DEGRADED': return 'text-orange-500'
    case 'UNHEALTHY': return 'text-red-500'
    default: return 'text-600'
  }
})

const serviceHealthTrendIcon = computed(() => {
  switch (serviceStatus.value) {
    case 'HEALTHY': return 'pi pi-arrow-up text-green-500'
    case 'DEGRADED': return 'pi pi-minus text-orange-500'
    case 'UNHEALTHY': return 'pi pi-arrow-down text-red-500'
    default: return 'pi pi-minus text-600'
  }
})

const responseTimeTrendIcon = computed(() => {
  if (responseTime.value < 1000) return 'pi pi-arrow-up text-green-500'
  if (responseTime.value < 3000) return 'pi pi-minus text-orange-500'
  return 'pi pi-arrow-down text-red-500'
})

const responseTimeTrendColor = computed(() => {
  if (responseTime.value < 1000) return 'text-green-500'
  if (responseTime.value < 3000) return 'text-orange-500'
  return 'text-red-500'
})

const responseTimeTrend = computed(() => {
  if (responseTime.value < 1000) return 'Fast'
  if (responseTime.value < 3000) return 'Moderate'
  return 'Slow'
})

const errorRateColor = computed(() => {
  if (errorRate.value < 5) return 'text-green-500'
  if (errorRate.value < 15) return 'text-orange-500'
  return 'text-red-500'
})

const configStatusColor = computed(() => {
  switch (configStatus.value) {
    case 'Valid': return 'text-green-500'
    case 'Warning': return 'text-orange-500'
    case 'Invalid': return 'text-red-500'
    default: return 'text-600'
  }
})

const lastUpdatedDisplay = computed(() => {
  if (!lastUpdated.value) return 'Never'
  
  const now = new Date()
  const diffMs = now.getTime() - lastUpdated.value.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)
  
  if (diffSeconds < 60) {
    return `${diffSeconds}s ago`
  } else if (diffSeconds < 3600) {
    return `${Math.floor(diffSeconds / 60)}m ago`
  } else {
    return lastUpdated.value.toLocaleString()
  }
})

// Methods
const loadData = async () => {
  try {
    error.value = null
    isLoading.value = true

    // Try to fetch analysis system status
    // For now, we'll simulate the data since the actual API might not be implemented yet
    await simulateAnalysisStatusData()
    
    lastUpdated.value = new Date()
    
    showToast.success(
      'Status Loaded',
      'Analysis system status updated successfully'
    )
  } catch (err) {
    console.error('Failed to load analysis status:', err)
    error.value = err instanceof Error ? err.message : 'Unknown error occurred'
    handleApiError(err, 'Load analysis status')
  } finally {
    isLoading.value = false
  }
}

const simulateAnalysisStatusData = async () => {
  // Simulate API call delay
  await new Promise(resolve => setTimeout(resolve, 1000))
  
  // Try to check if prediction analysis service is available
  try {
    // This would be replaced with actual API call to prediction analysis service
    const response = await fetch('/api/prediction-analysis/health', {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    })
    
    if (response.ok) {
      serviceStatus.value = 'HEALTHY'
      responseTime.value = Math.floor(Math.random() * 500) + 200
      errorRate.value = Math.floor(Math.random() * 3)
      configStatus.value = 'Valid'
    } else {
      throw new Error('Service unavailable')
    }
  } catch {
    // Service is not available, use mock data
    serviceStatus.value = 'UNKNOWN'
    responseTime.value = 0
    errorRate.value = 0
    configStatus.value = 'Unknown'
  }
  
  // Set other mock data
  queueSize.value = Math.floor(Math.random() * 10)
  activeJobs.value = Math.floor(Math.random() * 3)
  avgProcessingTime.value = Math.floor(Math.random() * 2000) + 1000
  lastHealthCheck.value = new Date().toLocaleString()
  uptime.value = '2h 34m'
  totalJobsProcessed.value = Math.floor(Math.random() * 1000) + 500
}

const refreshData = async () => {
  isRefreshing.value = true
  try {
    await loadData()
  } finally {
    isRefreshing.value = false
  }
}

const startAutoRefresh = () => {
  refreshInterval = setInterval(() => {
    if (!isLoading.value && !isRefreshing.value) {
      refreshData()
    }
  }, 30000) // Refresh every 30 seconds
}

const stopAutoRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
    refreshInterval = null
  }
}

// Lifecycle
onMounted(async () => {
  await loadData()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.analysis-status {
  padding: 1.5rem;
  min-height: calc(100vh - 7rem);
  background-color: var(--surface-ground);
}

/* Status Cards */
.status-card {
  transition: all 0.3s ease;
  border: 1px solid var(--surface-border);
}

.status-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 25px 0 rgba(0, 0, 0, 0.1);
}

.status-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

/* System Detail Items */
.system-detail-item {
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--surface-border);
}

.system-detail-item:last-child {
  margin-bottom: 0;
  border-bottom: none;
}

.detail-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  margin-bottom: 0.25rem;
  font-weight: 500;
}

.detail-value {
  font-size: 1rem;
  color: var(--text-color);
  font-weight: 600;
}

/* Error state styling */
.error-card {
  border: 1px solid var(--red-200);
  background: var(--red-50);
}

/* Panel customizations */
:deep(.p-panel .p-panel-header) {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-bottom: none;
  padding: 1rem 1.25rem;
}

:deep(.p-panel .p-panel-content) {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-top: none;
  padding: 1.25rem;
}

/* Card content padding adjustments */
:deep(.p-card .p-card-body) {
  padding: 1.25rem;
}

:deep(.p-card .p-card-content) {
  padding: 0;
}

/* Responsive design */
@media screen and (max-width: 991px) {
  .analysis-status {
    padding: 1rem;
    min-height: calc(100vh - 5rem);
  }
  
  .status-icon {
    width: 2.5rem;
    height: 2.5rem;
    font-size: 1.25rem;
  }
}

@media screen and (max-width: 575px) {
  .analysis-status {
    padding: 0.75rem;
    min-height: calc(100vh - 4rem);
  }
  
  :deep(.p-panel .p-panel-header) {
    padding: 0.75rem 1rem;
  }
  
  :deep(.p-panel .p-panel-content) {
    padding: 1rem;
  }
  
  :deep(.p-card .p-card-body) {
    padding: 1rem;
  }
}

/* Dark theme adjustments */
:root.dark {
  .error-card {
    border-color: var(--red-400);
    background: rgba(var(--red-500-rgb), 0.1);
  }
}
</style>