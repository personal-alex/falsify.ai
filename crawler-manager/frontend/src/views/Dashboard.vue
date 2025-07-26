<template>
  <div class="dashboard">
    <div class="surface-0 shadow-2 p-3 border-1 border-50 border-round">
      <div class="text-center mb-5">
        <div class="text-900 text-3xl font-medium mb-3">Crawler Manager Dashboard</div>
        <span class="text-600 text-2xl">Monitor and control your web crawlers</span>
      </div>
      
      <!-- Summary Cards -->
      <div class="grid mb-4">
        <div class="col-12 md:col-3">
          <div class="card text-center">
            <div class="text-2xl font-bold text-900">{{ totalCrawlers }}</div>
            <div class="text-600">Total Crawlers</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="card text-center">
            <div class="text-2xl font-bold text-green-500">{{ healthyCrawlers }}</div>
            <div class="text-600">Healthy</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="card text-center">
            <div class="text-2xl font-bold text-red-500">{{ unhealthyCrawlers }}</div>
            <div class="text-600">Unhealthy</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="card text-center">
            <div class="text-2xl font-bold text-orange-500">{{ unknownCrawlers }}</div>
            <div class="text-600">Unknown</div>
          </div>
        </div>
      </div>

      <!-- Controls -->
      <div class="flex justify-content-between align-items-center mb-4">
        <h5 class="m-0">Crawler Status</h5>
        <div class="flex align-items-center">
          <div class="flex align-items-center mr-3">
            <i class="pi pi-circle-fill text-green-500 mr-1"></i>
            <span class="text-sm text-600 mr-3">Connected</span>
            <i class="pi pi-circle-fill text-red-500 mr-1" v-if="!wsConnected"></i>
            <span class="text-sm text-600 mr-3" v-if="!wsConnected">Disconnected</span>
          </div>
          <Button
            icon="pi pi-refresh"
            label="Refresh All"
            class="p-button-sm"
            :loading="isRefreshing"
            @click="refreshAllData"
          />
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="isLoading" class="flex align-items-center justify-content-center p-6">
        <ProgressSpinner />
        <span class="ml-3 text-600">Loading crawler data...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="card">
        <Message severity="error" :closable="false">
          <div class="flex align-items-center">
            <i class="pi pi-exclamation-triangle mr-2"></i>
            <div>
              <div class="font-medium">Failed to load crawler data</div>
              <div class="text-sm">{{ error }}</div>
            </div>
          </div>
        </Message>
        <div class="text-center mt-3">
          <Button
            icon="pi pi-refresh"
            label="Retry"
            @click="loadData"
          />
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="crawlerData.length === 0" class="card">
        <div class="flex align-items-center justify-content-center p-6 border-2 border-dashed surface-border border-round">
          <div class="text-center">
            <i class="pi pi-search text-4xl text-400 mb-3"></i>
            <div class="text-900 font-medium mb-2">No crawlers configured</div>
            <div class="text-600">Configure crawler instances in application.properties to get started</div>
          </div>
        </div>
      </div>

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

      <!-- Last Updated -->
      <div v-if="lastUpdated" class="text-center mt-4">
        <small class="text-600">
          Last updated: {{ lastUpdatedDisplay }}
        </small>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import Message from 'primevue/message'
import CrawlerHealthCard from '@/components/CrawlerHealthCard.vue'
import type { CrawlerConfiguration, HealthStatus, CrawlerHealthData } from '@/types/health'
import { ApiService } from '@/services/api'
import { HealthWebSocketService, type HealthUpdateMessage } from '@/services/websocket'

const toast = useToast()

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
      toast.add({
        severity: 'success',
        summary: 'Data Loaded',
        detail: `Loaded ${crawlerData.value.length} crawler(s)`,
        life: 3000
      })
    }
  } catch (err) {
    console.error('Failed to load data:', err)
    error.value = err instanceof Error ? err.message : 'Unknown error occurred'
    toast.add({
      severity: 'error',
      summary: 'Load Error',
      detail: 'Failed to load crawler data',
      life: 5000
    })
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
  padding: 2rem;
  min-height: 100vh;
  background-color: var(--surface-ground);
}

.card {
  background: var(--surface-card);
  padding: 1.5rem;
  border-radius: 6px;
  box-shadow: 0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12);
  margin-bottom: 1rem;
}
</style>