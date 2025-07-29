<template>
  <div class="dashboard">
    <!-- Header Panel -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center">
          <i class="pi pi-chart-line mr-2 text-primary"></i>
          <span class="font-semibold text-lg">Crawler Manager Dashboard</span>
        </div>
      </template>
      
      <div class="text-center">
        <div class="text-600 text-xl">Monitor and control your web crawlers</div>
      </div>
    </Panel>
      
    <!-- KPI Summary Cards with Trend Indicators -->
    <div class="grid mb-4">
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="kpi-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-900 mb-1">{{ totalCrawlers }}</div>
                <div class="text-600 text-sm">Total Crawlers</div>
              </div>
              <div class="kpi-icon bg-blue-100 text-blue-600">
                <i class="pi pi-server"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i class="pi pi-arrow-up text-green-500 text-xs mr-1"></i>
              <span class="text-green-500 text-xs font-medium">Active</span>
            </div>
          </template>
        </Card>
      </div>
      
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="kpi-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-green-500 mb-1">{{ healthyCrawlers }}</div>
                <div class="text-600 text-sm">Healthy</div>
              </div>
              <div class="kpi-icon bg-green-100 text-green-600">
                <i class="pi pi-check-circle"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i class="pi pi-arrow-up text-green-500 text-xs mr-1"></i>
              <span class="text-green-500 text-xs font-medium">{{ healthyPercentage }}%</span>
            </div>
          </template>
        </Card>
      </div>
      
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="kpi-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-red-500 mb-1">{{ unhealthyCrawlers }}</div>
                <div class="text-600 text-sm">Unhealthy</div>
              </div>
              <div class="kpi-icon bg-red-100 text-red-600">
                <i class="pi pi-times-circle"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i class="pi pi-arrow-down text-red-500 text-xs mr-1" v-if="unhealthyCrawlers > 0"></i>
              <i class="pi pi-minus text-600 text-xs mr-1" v-else></i>
              <span class="text-xs font-medium" :class="unhealthyCrawlers > 0 ? 'text-red-500' : 'text-600'">
                {{ unhealthyPercentage }}%
              </span>
            </div>
          </template>
        </Card>
      </div>
      
      <div class="col-12 md:col-6 lg:col-3">
        <Card class="kpi-card">
          <template #content>
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-2xl font-bold text-orange-500 mb-1">{{ unknownCrawlers }}</div>
                <div class="text-600 text-sm">Unknown</div>
              </div>
              <div class="kpi-icon bg-orange-100 text-orange-600">
                <i class="pi pi-question-circle"></i>
              </div>
            </div>
            <div class="flex align-items-center mt-2">
              <i class="pi pi-minus text-600 text-xs mr-1"></i>
              <span class="text-600 text-xs font-medium">{{ unknownPercentage }}%</span>
            </div>
          </template>
        </Card>
      </div>
    </div>

    <!-- Controls Panel -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center">
            <i class="pi pi-list mr-2"></i>
            <span class="font-semibold">Crawler Status</span>
          </div>
          <div class="flex align-items-center">
            <div class="flex align-items-center mr-3">
              <i class="pi pi-circle-fill mr-1" :class="wsConnected ? 'text-green-500' : 'text-red-500'"></i>
              <span class="text-sm text-600">{{ wsConnected ? 'Connected' : 'Disconnected' }}</span>
            </div>
            <Button
              icon="pi pi-refresh"
              label="Refresh All"
              size="small"
              :loading="isRefreshing"
              @click="refreshAllData"
              data-testid="refresh-button"
            />
          </div>
        </div>
      </template>
    </Panel>

    <!-- Loading State with Skeletons -->
    <div v-if="isLoading" data-testid="loading-spinner">
      <div class="grid">
        <div v-for="n in 3" :key="n" class="col-12 md:col-6 lg:col-4">
          <Card>
            <template #content>
              <div class="flex align-items-center mb-3">
                <Skeleton shape="circle" size="3rem" class="mr-3"></Skeleton>
                <div class="flex-1">
                  <Skeleton width="60%" height="1.2rem" class="mb-2"></Skeleton>
                  <Skeleton width="40%" height="1rem"></Skeleton>
                </div>
              </div>
              <div class="grid">
                <div class="col-6">
                  <Skeleton width="100%" height="1rem" class="mb-1"></Skeleton>
                  <Skeleton width="80%" height="1.5rem"></Skeleton>
                </div>
                <div class="col-6">
                  <Skeleton width="100%" height="1rem" class="mb-1"></Skeleton>
                  <Skeleton width="60%" height="1.5rem"></Skeleton>
                </div>
              </div>
              <Skeleton width="100%" height="1rem" class="mt-3"></Skeleton>
            </template>
          </Card>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <Card v-else-if="error" class="error-card">
      <template #content>
        <Message severity="error" :closable="false" class="mb-3">
          <div class="flex align-items-center">
            <i class="pi pi-exclamation-triangle mr-2"></i>
            <div>
              <div class="font-medium">Failed to load crawler data</div>
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

    <!-- Empty State -->
    <Card v-else-if="crawlerData.length === 0" class="empty-state-card">
      <template #content>
        <div class="text-center p-6">
          <div class="empty-state-icon mb-4">
            <i class="pi pi-search text-6xl text-400"></i>
          </div>
          <div class="text-900 font-medium text-xl mb-2">No crawlers configured</div>
          <div class="text-600 mb-4">Configure crawler instances in application.properties to get started</div>
          <Button
            icon="pi pi-plus"
            label="Learn More"
            severity="secondary"
            text
            @click="() => {}"
          />
        </div>
      </template>
    </Card>

    <!-- Crawler Cards -->
    <div v-else class="grid">
      <div 
        v-for="crawler in crawlerData" 
        :key="crawler.configuration.id"
        class="col-12 md:col-6 lg:col-4"
      >
        <CrawlerHealthCard
          :crawler="crawler.configuration"
          :health="crawler.health"
          @health-updated="updateCrawlerHealth"
        />
      </div>
    </div>

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
import Skeleton from 'primevue/skeleton'
import CrawlerHealthCard from '@/components/CrawlerHealthCard.vue'
import type { HealthStatus, CrawlerHealthData } from '@/types/health'
import { ApiService } from '@/services/api'
import { HealthWebSocketService, type HealthUpdateMessage } from '@/services/websocket'
import { useNotifications } from '@/composables/useNotifications'

const { showToast, handleApiError } = useNotifications()

// State
const isLoading = ref(true)
const isRefreshing = ref(false)
const error = ref<string | null>(null)
const crawlerData = ref<CrawlerHealthData[]>([])
const lastUpdated = ref<Date | null>(null)
const wsConnected = ref(true)

// WebSocket service
let wsService: HealthWebSocketService | null = null
let wsUnsubscribe: (() => void) | null = null

// Computed properties
const totalCrawlers = computed(() => crawlerData.value.length)
const healthyCrawlers = computed(() => 
  crawlerData.value.filter(c => c.health.status === 'HEALTHY').length
)
const unhealthyCrawlers = computed(() => 
  crawlerData.value.filter(c => c.health.status === 'UNHEALTHY').length
)
const unknownCrawlers = computed(() => 
  crawlerData.value.filter(c => c.health.status === 'UNKNOWN').length
)

// Percentage calculations for trend indicators
const healthyPercentage = computed(() => {
  if (totalCrawlers.value === 0) return 0
  return Math.round((healthyCrawlers.value / totalCrawlers.value) * 100)
})

const unhealthyPercentage = computed(() => {
  if (totalCrawlers.value === 0) return 0
  return Math.round((unhealthyCrawlers.value / totalCrawlers.value) * 100)
})

const unknownPercentage = computed(() => {
  if (totalCrawlers.value === 0) return 0
  return Math.round((unknownCrawlers.value / totalCrawlers.value) * 100)
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

    // Load crawler configurations and health data in parallel
    const [configurations, healthData] = await Promise.all([
      ApiService.getCrawlerConfigurations(),
      ApiService.getAllCrawlerHealth()
    ])

    // Combine configuration and health data
    crawlerData.value = configurations.map(config => ({
      configuration: config,
      health: healthData[config.id] || {
        status: 'UNKNOWN' as const,
        message: 'No health data available',
        lastCheck: '',
        responseTimeMs: null,
        crawlerId: config.id
      }
    }))

    lastUpdated.value = new Date()
    
    if (crawlerData.value.length > 0) {
      showToast.success(
        'Data Loaded',
        `Loaded ${crawlerData.value.length} crawler(s)`
      )
    }
  } catch (err) {
    console.error('Failed to load data:', err)
    error.value = err instanceof Error ? err.message : 'Unknown error occurred'
    handleApiError(err, 'Load crawler data')
  } finally {
    isLoading.value = false
  }
}

const refreshAllData = async () => {
  isRefreshing.value = true
  try {
    await loadData()
  } finally {
    isRefreshing.value = false
  }
}

const updateCrawlerHealth = (updatedHealth: HealthStatus) => {
  const crawlerIndex = crawlerData.value.findIndex(
    c => c.configuration.id === updatedHealth.crawlerId
  )
  
  if (crawlerIndex !== -1) {
    crawlerData.value[crawlerIndex].health = updatedHealth
    lastUpdated.value = new Date()
  }
}

const handleHealthUpdate = (message: HealthUpdateMessage) => {
  const crawlerIndex = crawlerData.value.findIndex(
    c => c.configuration.id === message.crawlerId
  )
  
  if (crawlerIndex !== -1) {
    crawlerData.value[crawlerIndex].health = {
      status: message.status,
      message: message.message,
      lastCheck: message.timestamp,
      responseTimeMs: null, // WebSocket doesn't include response time
      crawlerId: message.crawlerId
    }
    lastUpdated.value = new Date()
  }
}

const initWebSocket = () => {
  try {
    wsService = new HealthWebSocketService()
    wsUnsubscribe = wsService.onHealthUpdate(handleHealthUpdate)
    wsConnected.value = true
  } catch (error) {
    console.error('Failed to initialize WebSocket:', error)
    wsConnected.value = false
  }
}

// Lifecycle
onMounted(async () => {
  await loadData()
  initWebSocket()
})

onUnmounted(() => {
  if (wsUnsubscribe) {
    wsUnsubscribe()
  }
  if (wsService) {
    wsService.disconnect()
  }
})
</script>

<style scoped>
.dashboard {
  padding: 1.5rem;
  min-height: calc(100vh - 7rem);
  background-color: var(--surface-ground);
}

/* KPI Cards with enhanced styling */
.kpi-card {
  transition: all 0.3s ease;
  border: 1px solid var(--surface-border);
}

.kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 25px 0 rgba(0, 0, 0, 0.1);
}

.kpi-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5rem;
}

/* Error state styling */
.error-card {
  border: 1px solid var(--red-200);
  background: var(--red-50);
}

/* Empty state styling */
.empty-state-card {
  border: 2px dashed var(--surface-border);
  background: var(--surface-50);
}

.empty-state-icon {
  opacity: 0.6;
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
  
  .kpi-icon {
    width: 2.5rem;
    height: 2.5rem;
    font-size: 1.25rem;
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

/* Dark theme adjustments */
:root.dark {
  .error-card {
    border-color: var(--red-400);
    background: rgba(var(--red-500-rgb), 0.1);
  }
  
  .empty-state-card {
    border-color: var(--surface-600);
    background: var(--surface-800);
  }
}
</style>