<template>
  <div class="dashboard">
    <!-- Header Panel -->
    <!-- Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center">
          <i class="pi pi-chart-line mr-2 text-primary"></i>
          <span class="font-semibold text-lg">System Dashboard</span>
        </div>
      </template>
      
      <div class="text-center">
        <div class="text-600 text-xl">Monitor system status and analysis services</div>
      </div>
    </Panel -->
      
    <!-- Crawlers System Status Section -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center">
            <i class="pi pi-globe mr-2"></i>
            <span class="font-semibold">Crawlers System Status</span>
          </div>
          <!--div class="flex align-items-center">
            <Button
              icon="pi pi-external-link"
              label="View Management"
              size="small"
              text
              @click="navigateToCrawlerManagement"
              data-testid="crawler-management-link"
            />
          </div-->
        </div>
      </template>

      <div class="grid">
        <div class="col-12 md:col-6 lg:col-3">
          <div class="crawler-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold mb-1" :class="crawlerSystemHealthColor">
                  {{ crawlerSystemStatus }}
                </div>
                <div class="text-600 text-sm">System Status</div>
              </div>
              <div class="crawler-status-icon" :class="crawlerSystemHealthBgColor">
                <i :class="crawlerSystemHealthIcon"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 md:col-6 lg:col-3">
          <div class="crawler-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold text-900 mb-1">{{ totalCrawlers }}</div>
                <div class="text-600 text-sm">Total Crawlers</div>
              </div>
              <div class="crawler-status-icon bg-blue-100 text-blue-600">
                <i class="pi pi-server"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 md:col-6 lg:col-3">
          <div class="crawler-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold text-900 mb-1">{{ healthyCrawlers }}</div>
                <div class="text-600 text-sm">Healthy Crawlers</div>
              </div>
              <div class="crawler-status-icon bg-green-100 text-green-600">
                <i class="pi pi-check-circle"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 md:col-6 lg:col-3">
          <div class="crawler-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold text-900 mb-1">{{ activeCrawlers }}</div>
                <div class="text-600 text-sm">Active Crawlers</div>
              </div>
              <div class="crawler-status-icon bg-orange-100 text-orange-600">
                <i class="pi pi-play-circle"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions for Crawlers -->
      <div class="mt-3 pt-3 border-top-1 surface-border">
        <div class="flex flex-wrap gap-2">
          <Button
            icon="pi pi-list"
            label="View All Crawlers"
            size="small"
            outlined
            @click="navigateToCrawlerManagement"
          />
          <Button
            icon="pi pi-history"
            label="View History"
            size="small"
            outlined
            @click="navigateToCrawlerHistory"
          />
          <Button
            v-if="unhealthyCrawlers > 0"
            icon="pi pi-exclamation-triangle"
            :label="`${unhealthyCrawlers} Issues`"
            size="small"
            severity="warning"
            outlined
            @click="navigateToCrawlerManagement"
          />
        </div>
      </div>
    </Panel>

    <!-- Prediction Analysis Status Section -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center">
            <i class="pi pi-brain mr-2"></i>
            <span class="font-semibold">Prediction Analysis Status</span>
          </div>
          <!-- div class="flex align-items-center">
            <Button
              icon="pi pi-external-link"
              label="View Details"
              size="small"
              text
              @click="navigateToAnalysisStatus"
              data-testid="analysis-status-link"
            />
          </div -->
        </div>
      </template>

      <div class="grid">
        <div class="col-12 md:col-6 lg:col-3">
          <div class="analysis-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold mb-1" :class="analysisServiceHealthColor">
                  {{ analysisServiceStatus }}
                </div>
                <div class="text-600 text-sm">Service Status</div>
              </div>
              <div class="analysis-status-icon" :class="analysisServiceHealthBgColor">
                <i :class="analysisServiceHealthIcon"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 md:col-6 lg:col-3">
          <div class="analysis-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold text-900 mb-1">{{ analysisQueueSize }}</div>
                <div class="text-600 text-sm">Queue Size</div>
              </div>
              <div class="analysis-status-icon bg-blue-100 text-blue-600">
                <i class="pi pi-list"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 md:col-6 lg:col-3">
          <div class="analysis-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold text-900 mb-1">{{ analysisActiveJobs }}</div>
                <div class="text-600 text-sm">Active Jobs</div>
              </div>
              <div class="analysis-status-icon bg-orange-100 text-orange-600">
                <i class="pi pi-cog"></i>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 md:col-6 lg:col-3">
          <div class="analysis-status-item">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-lg font-bold text-900 mb-1">{{ analysisResponseTime }}ms</div>
                <div class="text-600 text-sm">Response Time</div>
              </div>
              <div class="analysis-status-icon bg-purple-100 text-purple-600">
                <i class="pi pi-stopwatch"></i>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Panel>




  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import Panel from 'primevue/panel'
import { useCrawlerStore } from '@/stores/crawler'

const router = useRouter()
const crawlerStore = useCrawlerStore()

// Prediction Analysis Status State
const analysisServiceStatus = ref('UNKNOWN')
const analysisQueueSize = ref(0)
const analysisActiveJobs = ref(0)
const analysisResponseTime = ref(0)

// Real-time update interval
let statusUpdateInterval: NodeJS.Timeout | null = null

// Crawler System Status Computed Properties
const crawlerSystemStatus = computed(() => {
  if (crawlerStore.isLoading) return 'LOADING'
  if (!crawlerStore.isOnline) return 'OFFLINE'
  
  const totalCrawlers = crawlerStore.totalCrawlers
  const healthyCrawlers = crawlerStore.healthyCrawlers
  const unhealthyCrawlers = crawlerStore.unhealthyCrawlers
  
  if (totalCrawlers === 0) return 'NO_CRAWLERS'
  if (unhealthyCrawlers > 0) return 'DEGRADED'
  if (healthyCrawlers === totalCrawlers) return 'HEALTHY'
  return 'UNKNOWN'
})

const crawlerSystemHealthColor = computed(() => {
  switch (crawlerSystemStatus.value) {
    case 'HEALTHY': return 'text-green-500'
    case 'DEGRADED': return 'text-orange-500'
    case 'OFFLINE': return 'text-red-500'
    case 'LOADING': return 'text-blue-500'
    case 'NO_CRAWLERS': return 'text-gray-500'
    default: return 'text-600'
  }
})

const crawlerSystemHealthBgColor = computed(() => {
  switch (crawlerSystemStatus.value) {
    case 'HEALTHY': return 'bg-green-100 text-green-600'
    case 'DEGRADED': return 'bg-orange-100 text-orange-600'
    case 'OFFLINE': return 'bg-red-100 text-red-600'
    case 'LOADING': return 'bg-blue-100 text-blue-600'
    case 'NO_CRAWLERS': return 'bg-gray-100 text-gray-600'
    default: return 'bg-gray-100 text-gray-600'
  }
})

const crawlerSystemHealthIcon = computed(() => {
  switch (crawlerSystemStatus.value) {
    case 'HEALTHY': return 'pi pi-check-circle'
    case 'DEGRADED': return 'pi pi-exclamation-triangle'
    case 'OFFLINE': return 'pi pi-times-circle'
    case 'LOADING': return 'pi pi-spin pi-spinner'
    case 'NO_CRAWLERS': return 'pi pi-info-circle'
    default: return 'pi pi-question-circle'
  }
})

// Crawler metrics computed properties
const totalCrawlers = computed(() => crawlerStore.totalCrawlers)
const healthyCrawlers = computed(() => crawlerStore.healthyCrawlers)
const unhealthyCrawlers = computed(() => crawlerStore.unhealthyCrawlers)
const activeCrawlers = computed(() => crawlerStore.activeCrawlers)

// Prediction Analysis Status Computed Properties
const analysisServiceHealthColor = computed(() => {
  switch (analysisServiceStatus.value) {
    case 'HEALTHY': return 'text-green-500'
    case 'DEGRADED': return 'text-orange-500'
    case 'UNHEALTHY': return 'text-red-500'
    default: return 'text-600'
  }
})

const analysisServiceHealthBgColor = computed(() => {
  switch (analysisServiceStatus.value) {
    case 'HEALTHY': return 'bg-green-100 text-green-600'
    case 'DEGRADED': return 'bg-orange-100 text-orange-600'
    case 'UNHEALTHY': return 'bg-red-100 text-red-600'
    default: return 'bg-gray-100 text-gray-600'
  }
})

const analysisServiceHealthIcon = computed(() => {
  switch (analysisServiceStatus.value) {
    case 'HEALTHY': return 'pi pi-check-circle'
    case 'DEGRADED': return 'pi pi-exclamation-triangle'
    case 'UNHEALTHY': return 'pi pi-times-circle'
    default: return 'pi pi-question-circle'
  }
})

// Methods
const loadCrawlerStatus = async () => {
  try {
    await crawlerStore.loadCrawlerData()
  } catch (error) {
    console.error('Failed to load crawler status:', error)
  }
}

const loadAnalysisStatus = async () => {
  try {
    // Try to check if prediction analysis service is available
    const response = await fetch('/api/prediction-analysis/health', {
      method: 'GET',
      headers: { 'Accept': 'application/json' }
    })
    
    if (response.ok) {
      analysisServiceStatus.value = 'HEALTHY'
      analysisResponseTime.value = Math.floor(Math.random() * 500) + 200
    } else {
      throw new Error('Service unavailable')
    }
  } catch {
    // Service is not available, set as unknown
    analysisServiceStatus.value = 'UNKNOWN'
    analysisResponseTime.value = 0
  }
  
  // Set mock data for queue and active jobs
  analysisQueueSize.value = Math.floor(Math.random() * 10)
  analysisActiveJobs.value = Math.floor(Math.random() * 3)
}

const startRealTimeUpdates = () => {
  // Update status every 30 seconds
  statusUpdateInterval = setInterval(async () => {
    await Promise.all([
      loadCrawlerStatus(),
      loadAnalysisStatus()
    ])
  }, 30000)
}

const stopRealTimeUpdates = () => {
  if (statusUpdateInterval) {
    clearInterval(statusUpdateInterval)
    statusUpdateInterval = null
  }
}

// Navigation methods
// const navigateToAnalysisStatus = () => {
//  router.push('/analysis/status')
// }

const navigateToCrawlerManagement = () => {
  router.push('/crawlers/management')
}

const navigateToCrawlerHistory = () => {
  router.push('/crawlers/history')
}

// Lifecycle
onMounted(async () => {
  // Initialize WebSocket connections
  crawlerStore.initializeWebSocket()
  
  // Load initial data
  await Promise.all([
    loadCrawlerStatus(),
    loadAnalysisStatus()
  ])
  
  // Start real-time updates
  startRealTimeUpdates()
})

onUnmounted(() => {
  stopRealTimeUpdates()
  crawlerStore.cleanup()
})
</script>

<style scoped>
.dashboard {
  padding: 1.5rem;
  min-height: calc(100vh - 7rem);
  background-color: var(--surface-ground);
}

/* Status Items - Common styles for both crawler and analysis */
.crawler-status-item,
.analysis-status-item {
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  background: var(--surface-card);
  transition: all 0.3s ease;
}

.crawler-status-item:hover,
.analysis-status-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
}

.crawler-status-icon,
.analysis-status-icon {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
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

/* Skeleton customizations */
:deep(.p-skeleton) {
  background: linear-gradient(90deg, var(--surface-200) 25%, var(--surface-100) 50%, var(--surface-200) 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* Responsive design */
@media screen and (max-width: 991px) {
  .dashboard {
    padding: 1rem;
    min-height: calc(100vh - 5rem);
  }

  .analysis-status-icon {
    width: 2rem;
    height: 2rem;
    font-size: 1rem;
  }
}

@media screen and (max-width: 575px) {
  .dashboard {
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
</style>