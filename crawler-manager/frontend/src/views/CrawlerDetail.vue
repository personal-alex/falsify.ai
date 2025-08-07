<template>
  <div class="crawler-detail">
    <div class="crawler-detail-container">
      <div class="surface-0 shadow-2 p-3 border-1 border-50 border-round">
        <div class="flex align-items-center mb-5">
          <Button 
          icon="pi pi-arrow-left" 
          class="p-button-text p-button-plain mr-3"
          @click="$router.push('/')"
        />
        <div class="flex-1">
          <div class="flex align-items-center">
            <div class="text-900 text-3xl font-medium mr-3">{{ crawlerName }}</div>
            <div 
              class="status-indicator"
              :class="statusIndicatorClass"
              v-if="configuration"
              v-tooltip.bottom="statusTooltip"
            >
              <i :class="statusIconClass"></i>
            </div>
          </div>
          <span class="text-600 text-xl">{{ crawlerId }}</span>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="isLoading" class="flex align-items-center justify-content-center p-6">
        <ProgressSpinner />
        <span class="ml-3 text-600">Loading crawler details...</span>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="card">
        <Message severity="error" :closable="false">
          <div class="flex align-items-center">
            <i class="pi pi-exclamation-triangle mr-2"></i>
            <div>
              <div class="font-medium">Failed to load crawler details</div>
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

      <!-- Tabbed Interface -->
      <div v-else>
        <Tabs v-model:value="activeTabIndex" class="crawler-detail-tabs">
          <TabList>
            <Tab value="0">
              <i class="pi pi-cog mr-2"></i>
              <span>Configuration</span>
            </Tab>
            <Tab value="1" :disabled="!configuration?.enabled">
              <i class="pi pi-chart-line mr-2"></i>
              <span>Metrics</span>
            </Tab>
          </TabList>
          
          <TabPanels>
            <!-- Configuration Tab -->
            <TabPanel value="0">
            
            <div class="grid">
              <!-- Configuration Card -->
              <div class="col-12 lg:col-6">
                <div class="card h-full">
                  <h5 class="flex align-items-center">
                    <i class="pi pi-cog mr-2"></i>
                    Crawler Details
                  </h5>
                  <div class="grid" v-if="configuration">
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Name</div>
                      <div class="text-900 font-medium">{{ configuration.name }}</div>
                    </div>
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">ID</div>
                      <div class="text-900 font-medium">{{ configuration.id }}</div>
                    </div>
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Base URL</div>
                      <div class="text-900 font-medium">{{ configuration.baseUrl }}</div>
                    </div>
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Port</div>
                      <div class="text-900 font-medium">{{ configuration.port }}</div>
                    </div>
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Health Endpoint</div>
                      <div class="text-900 font-medium">{{ configuration.healthEndpoint }}</div>
                    </div>
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Status</div>
                      <div class="flex align-items-center">
                        <Tag 
                          :value="configuration.enabled ? 'Enabled' : 'Disabled'" 
                          :severity="configuration.enabled ? 'success' : 'danger'"
                          class="text-xs"
                        />
                      </div>
                    </div>
                    <!-- Author Information -->
                    <div class="col-12" v-if="configuration.authorName">
                      <div class="text-600 text-sm mb-2">Author</div>
                      <AuthorInfo 
                        :author-name="configuration.authorName"
                        :author-avatar-url="configuration.authorAvatarUrl"
                      />
                    </div>
                  </div>
                </div>
              </div>

              <!-- Health Status Card -->
              <div class="col-12 lg:col-6">
                <div class="card h-full">
                  <h5 class="flex align-items-center">
                    <i class="pi pi-heart mr-2"></i>
                    Health Status
                  </h5>
                  <div class="grid" v-if="health">
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Current Status</div>
                      <div class="flex align-items-center">
                        <Tag 
                          :value="health.status" 
                          :severity="statusSeverity"
                          class="text-xs"
                        />
                      </div>
                    </div>
                    <div class="col-6">
                      <div class="text-600 text-sm mb-1">Response Time</div>
                      <div class="text-900 font-medium">
                        {{ responseTimeDisplay }}
                      </div>
                    </div>
                    <div class="col-12">
                      <div class="text-600 text-sm mb-1">Last Check</div>
                      <div class="text-900 font-medium">{{ lastCheckDisplay }}</div>
                    </div>
                    <div class="col-12" v-if="health.message">
                      <div class="text-600 text-sm mb-1">Message</div>
                      <div class="text-900 font-medium">{{ health.message }}</div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Endpoints Card -->
              <div class="col-12">
                <div class="card">
                  <h5 class="flex align-items-center">
                    <i class="pi pi-link mr-2"></i>
                    Endpoints
                  </h5>
                  <div class="grid" v-if="configuration">
                    <div class="col-12 md:col-4">
                      <div class="text-600 text-sm mb-2">Health Check</div>
                      <div class="flex align-items-center p-2 surface-100 border-round">
                        <span class="text-900 text-sm flex-1">{{ configuration.baseUrl }}{{ configuration.healthEndpoint }}</span>
                        <Button
                          icon="pi pi-copy"
                          class="p-button-text p-button-sm"
                          @click="copyToClipboard(configuration.baseUrl + configuration.healthEndpoint)"
                        />
                      </div>
                    </div>
                    <div class="col-12 md:col-4">
                      <div class="text-600 text-sm mb-2">Crawl Endpoint</div>
                      <div class="flex align-items-center p-2 surface-100 border-round">
                        <span class="text-900 text-sm flex-1">{{ configuration.baseUrl }}{{ configuration.crawlEndpoint }}</span>
                        <Button
                          icon="pi pi-copy"
                          class="p-button-text p-button-sm"
                          @click="copyToClipboard(configuration.baseUrl + configuration.crawlEndpoint)"
                        />
                      </div>
                    </div>
                    <div class="col-12 md:col-4">
                      <div class="text-600 text-sm mb-2">Status Endpoint</div>
                      <div class="flex align-items-center p-2 surface-100 border-round">
                        <span class="text-900 text-sm flex-1">{{ configuration.baseUrl }}{{ configuration.statusEndpoint }}</span>
                        <Button
                          icon="pi pi-copy"
                          class="p-button-text p-button-sm"
                          @click="copyToClipboard(configuration.baseUrl + configuration.statusEndpoint)"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Last Crawl Response Card -->
              <div class="col-12" v-if="lastCrawlResponse">
                <div class="card">
                  <h5 class="flex align-items-center">
                    <i class="pi pi-history mr-2"></i>
                    Last Crawl Response
                  </h5>
                  <div class="grid">
                    <div class="col-12 md:col-6">
                      <div class="text-600 text-sm mb-1">Status</div>
                      <div class="flex align-items-center">
                        <Tag 
                          :value="lastCrawlResponse.status" 
                          :severity="getCrawlStatusSeverity(lastCrawlResponse.status)"
                          class="text-xs"
                        />
                      </div>
                    </div>
                    <div class="col-12 md:col-6">
                      <div class="text-600 text-sm mb-1">Request ID</div>
                      <div class="text-900 font-medium">{{ lastCrawlResponse.requestId }}</div>
                    </div>
                    <div class="col-12 md:col-6" v-if="lastCrawlResponse.crawlId">
                      <div class="text-600 text-sm mb-1">Crawl ID</div>
                      <div class="text-900 font-medium">{{ lastCrawlResponse.crawlId }}</div>
                    </div>
                    <div class="col-12 md:col-6">
                      <div class="text-600 text-sm mb-1">Timestamp</div>
                      <div class="text-900 font-medium">{{ formatTimestamp(lastCrawlResponse.timestamp) }}</div>
                    </div>
                    <div class="col-12" v-if="lastCrawlResponse.message">
                      <div class="text-600 text-sm mb-1">Message</div>
                      <div class="text-900 font-medium">{{ lastCrawlResponse.message }}</div>
                    </div>
                    <div class="col-12" v-if="lastCrawlResponse.suggestion">
                      <div class="text-600 text-sm mb-1">Suggestion</div>
                      <div class="text-900 font-medium">{{ lastCrawlResponse.suggestion }}</div>
                    </div>
                    <div class="col-12" v-if="lastCrawlResponse.estimatedDuration">
                      <div class="text-600 text-sm mb-1">Estimated Duration</div>
                      <div class="text-900 font-medium">{{ lastCrawlResponse.estimatedDuration }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            </TabPanel>

            <!-- Metrics Tab -->
            <TabPanel value="1">
            
            <div class="grid">
              <!-- Metrics Component -->
              <div class="col-12">
                <div class="card">
                  <MetricsComponent 
                    :crawler-id="crawlerId" 
                    :auto-refresh="true"
                    :refresh-interval="30000"
                  />
                </div>
              </div>

              <!-- Job History Component -->
              <div class="col-12">
                <div class="card">
                  <JobHistory 
                    :crawler-id="crawlerId" 
                    :show-pagination="true"
                    :page-size="10"
                    :auto-refresh="true"
                    :refresh-interval="30000"
                  />
                </div>
              </div>
            </div>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>

      <!-- Last Updated -->
      <div v-if="lastUpdated" class="text-center mt-4">
        <small class="text-600">
          Last updated: {{ lastUpdatedDisplay }}
        </small>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { useTopBarStore } from '@/stores/topbar'
import Button from 'primevue/button'
import ProgressSpinner from 'primevue/progressspinner'
import Message from 'primevue/message'
import Tag from 'primevue/tag'
import Tabs from 'primevue/tabs'
import TabList from 'primevue/tablist'
import Tab from 'primevue/tab'
import TabPanels from 'primevue/tabpanels'
import TabPanel from 'primevue/tabpanel'
import MetricsComponent from '@/components/MetricsComponent.vue'
import JobHistory from '@/components/JobHistory.vue'
import AuthorInfo from '@/components/AuthorInfo.vue'
import type { CrawlerConfiguration, HealthStatus, CrawlResponse } from '@/types/health'
import { ApiService } from '@/services/api'
import { HealthWebSocketService, type HealthUpdateMessage } from '@/services/websocket'

// Backend WebSocket message format
interface WebSocketMessage {
  type: string
  data: any
  timestamp: number
}

// Job status from backend
interface JobStatusData {
  jobId: string
  crawlerId: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  startTime: string
  endTime?: string
  articlesProcessed: number
  articlesSkipped: number
  articlesFailed: number
  errorMessage?: string
  requestId?: string
  currentActivity?: string
  lastUpdated?: string
  durationMs?: number
  elapsedTimeMs: number
  successRate: number
  totalArticlesAttempted: number
}

interface Props {
  id: string
}

const props = defineProps<Props>()
const toast = useToast()
const topBarStore = useTopBarStore()

// State
const isLoading = ref(true)
const isRefreshing = ref(false)
const isCrawling = ref(false)
const error = ref<string | null>(null)
const configuration = ref<CrawlerConfiguration | null>(null)
const health = ref<HealthStatus | null>(null)
const lastUpdated = ref<Date | null>(null)
const lastCrawlResponse = ref<CrawlResponse | null>(null)
const hasRunningJobs = ref(false)
const activeTabIndex = ref("0")

// WebSocket service
let wsService: HealthWebSocketService | null = null
let wsUnsubscribe: (() => void) | null = null

// Computed properties
const crawlerId = computed(() => props.id)
const crawlerName = computed(() => configuration.value?.name || props.id)

const statusIndicatorClass = computed(() => {
  if (!configuration.value?.enabled) return 'status-disabled'
  if (hasRunningJobs.value) return 'status-running'
  if (health.value?.status === 'HEALTHY') return 'status-ready'
  return 'status-error'
})

const statusIconClass = computed(() => {
  if (!configuration.value?.enabled) return 'pi pi-ban'
  if (hasRunningJobs.value) return 'pi pi-spin pi-spinner'
  if (health.value?.status === 'HEALTHY') return 'pi pi-check'
  return 'pi pi-times'
})

const statusTooltip = computed(() => {
  if (!configuration.value?.enabled) return 'Disabled'
  if (hasRunningJobs.value) return 'Running'
  if (health.value?.status === 'HEALTHY') return 'Ready'
  return 'Error'
})

const statusSeverity = computed(() => {
  if (!health.value) return 'warning'
  
  switch (health.value.status) {
    case 'HEALTHY':
      return 'success'
    case 'UNHEALTHY':
      return 'danger'
    default:
      return 'warning'
  }
})

const responseTimeDisplay = computed(() => {
  if (!health.value || health.value.responseTimeMs === null) {
    return 'N/A'
  }
  return `${health.value.responseTimeMs}ms`
})

const lastCheckDisplay = computed(() => {
  if (!health.value || !health.value.lastCheck) {
    return 'Never'
  }
  
  const date = new Date(health.value.lastCheck)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  
  if (diffSeconds < 60) {
    return `${diffSeconds}s ago`
  } else if (diffMinutes < 60) {
    return `${diffMinutes}m ago`
  } else {
    return date.toLocaleString()
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
const updateTopBarActions = () => {
  const canStartCrawl = configuration.value?.enabled && health.value?.status === 'HEALTHY'
  
  topBarStore.setCrawlerDetailActions({
    onStartCrawl: triggerCrawl,
    onHealthCheck: forceHealthCheck,
    onStatusCheck: checkCrawlStatus
  }, {
    canStartCrawl,
    isHealthChecking: isRefreshing.value,
    isCrawling: isCrawling.value
  })
}

const checkRunningJobs = async () => {
  try {
    hasRunningJobs.value = await ApiService.hasRunningJobs(props.id)
  } catch (error) {
    console.error('Failed to check running jobs:', error)
    hasRunningJobs.value = false
  }
}

const loadData = async () => {
  try {
    error.value = null
    isLoading.value = true

    // Load crawler configuration, health data, and running jobs status in parallel
    const [configurations, healthStatus] = await Promise.all([
      ApiService.getCrawlerConfigurations(),
      ApiService.getCrawlerHealth(props.id)
    ])

    // Find the specific crawler configuration
    configuration.value = configurations.find(c => c.id === props.id) || null
    health.value = healthStatus

    if (!configuration.value) {
      throw new Error(`Crawler with ID '${props.id}' not found`)
    }

    // Check for running jobs
    await checkRunningJobs()

    lastUpdated.value = new Date()
    
    // Update topbar actions after loading data
    updateTopBarActions()
  } catch (err) {
    console.error('Failed to load crawler details:', err)
    error.value = err instanceof Error ? err.message : 'Unknown error occurred'
    toast.add({
      severity: 'error',
      summary: 'Load Error',
      detail: 'Failed to load crawler details',
      life: 5000
    })
  } finally {
    isLoading.value = false
  }
}

const forceHealthCheck = async () => {
  isRefreshing.value = true
  updateTopBarActions() // Update button states
  
  try {
    const updatedHealth = await ApiService.forceHealthCheck(props.id)
    health.value = updatedHealth
    lastUpdated.value = new Date()
    
    toast.add({
      severity: 'success',
      summary: 'Health Check',
      detail: `Forced health check for ${crawlerName.value}`,
      life: 3000
    })
  } catch (error) {
    console.error('Failed to force health check:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to perform health check',
      life: 5000
    })
  } finally {
    isRefreshing.value = false
    updateTopBarActions() // Update button states
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.add({
      severity: 'success',
      summary: 'Copied',
      detail: 'URL copied to clipboard',
      life: 2000
    })
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to copy URL',
      life: 3000
    })
  }
}



const triggerCrawl = async () => {
  if (!configuration.value) return

  isCrawling.value = true
  updateTopBarActions() // Update button states
  
  try {
    const response = await ApiService.triggerCrawl(props.id)
    lastCrawlResponse.value = response
    
    // Show appropriate toast based on response status
    switch (response.status) {
      case 'ACCEPTED':
        // Immediately update running jobs status when crawl is accepted
        hasRunningJobs.value = true
        toast.add({
          severity: 'success',
          summary: 'Crawl Started',
          detail: `Crawl initiated successfully for ${crawlerName.value}`,
          life: 5000
        })
        break
      case 'CONFLICT':
        toast.add({
          severity: 'warn',
          summary: 'Crawl Already Running',
          detail: response.message,
          life: 5000
        })
        break
      case 'SERVICE_UNAVAILABLE':
        toast.add({
          severity: 'error',
          summary: 'Service Unavailable',
          detail: response.message,
          life: 5000
        })
        break
      case 'ERROR':
        toast.add({
          severity: 'error',
          summary: 'Crawl Failed',
          detail: response.message,
          life: 5000
        })
        break
    }
  } catch (error) {
    console.error('Failed to trigger crawl:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error instanceof Error ? error.message : 'Failed to trigger crawl',
      life: 5000
    })
  } finally {
    isCrawling.value = false
    updateTopBarActions() // Update button states
  }
}

const getCrawlStatusSeverity = (status: string) => {
  switch (status) {
    case 'ACCEPTED':
      return 'success'
    case 'CONFLICT':
      return 'warning'
    case 'SERVICE_UNAVAILABLE':
    case 'ERROR':
      return 'danger'
    default:
      return 'info'
  }
}

const formatTimestamp = (timestamp: string) => {
  try {
    const date = new Date(timestamp)
    return date.toLocaleString()
  } catch {
    return timestamp
  }
}

const checkCrawlStatus = async () => {
  try {
    const status = await ApiService.getCrawlerStatus(props.id)
    
    // Show the status in a toast
    toast.add({
      severity: 'info',
      summary: 'Crawl Status',
      detail: `Current status: ${JSON.stringify(status, null, 2)}`,
      life: 8000
    })
  } catch (error) {
    console.error('Failed to check crawl status:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: error instanceof Error ? error.message : 'Failed to check crawl status',
      life: 5000
    })
  }
}

const handleHealthUpdate = (message: HealthUpdateMessage) => {
  if (message.crawlerId === props.id) {
    health.value = {
      status: message.status,
      message: message.message,
      lastCheck: message.timestamp,
      responseTimeMs: null, // WebSocket doesn't include response time
      crawlerId: message.crawlerId
    }
    lastUpdated.value = new Date()
  }
}

const handleJobUpdate = (message: WebSocketMessage) => {
  // The message structure from backend is: { type, data: JobStatus, timestamp }
  const jobData = message.data as JobStatusData
  if (!jobData || !jobData.crawlerId) {
    return
  }
  
  if (jobData.crawlerId === props.id) {
    // Update running jobs status based on job updates
    switch (message.type) {
      case 'job.started':
        hasRunningJobs.value = true
        break
      case 'job.completed':
      case 'job.failed':
      case 'job.cancelled':
        // Check if there are still other running jobs
        checkRunningJobs()
        break
    }
    lastUpdated.value = new Date()
  }
}

const initWebSocket = () => {
  try {
    wsService = new HealthWebSocketService()
    const healthUnsubscribe = wsService.onHealthUpdate(handleHealthUpdate)
    
    // Use the generic 'on' method to listen for job events with correct message format
    const jobStartedUnsubscribe = wsService.on('job.started', handleJobUpdate)
    const jobCompletedUnsubscribe = wsService.on('job.completed', handleJobUpdate)
    const jobFailedUnsubscribe = wsService.on('job.failed', handleJobUpdate)
    const jobCancelledUnsubscribe = wsService.on('job.cancelled', handleJobUpdate)
    
    wsUnsubscribe = () => {
      healthUnsubscribe()
      jobStartedUnsubscribe()
      jobCompletedUnsubscribe()
      jobFailedUnsubscribe()
      jobCancelledUnsubscribe()
    }
  } catch (error) {
    console.error('Failed to initialize WebSocket:', error)
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
  
  // Clear topbar actions when leaving the view
  topBarStore.clearContextActions()
})
</script>

<style scoped>
/* MODERN CRAWLER DETAIL STYLING */
.crawler-detail {
  height: 100%;
  overflow-y: auto;
  background: linear-gradient(135deg, var(--surface-ground) 0%, var(--surface-50) 100%);
  
  /* CSS Custom Properties for consistent theming */
  --tab-border-radius: 16px;
  --card-border-radius: 12px;
  --transition-duration: 0.3s;
  --transition-easing: cubic-bezier(0.4, 0, 0.2, 1);
  --shadow-light: 0 2px 8px rgba(0, 0, 0, 0.08);
  --shadow-medium: 0 4px 12px rgba(0, 0, 0, 0.12);
  --shadow-heavy: 0 8px 25px rgba(0, 0, 0, 0.15);
  --primary-color-rgb: 59, 130, 246;
}

.crawler-detail-container {
  padding: 2rem;
  min-height: 100%;
  max-width: 100%;
}

.card {
  background: var(--surface-card);
  padding: 1.5rem;
  border-radius: var(--card-border-radius);
  box-shadow: var(--shadow-light);
  margin-bottom: 1rem;
  border: 1px solid var(--surface-border);
  transition: all var(--transition-duration) var(--transition-easing);
}

.card:hover {
  box-shadow: var(--shadow-medium);
  transform: translateY(-2px);
}

.status-indicator {
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: white;
}

.status-ready {
  background-color: var(--green-500);
}

.status-running {
  background-color: var(--blue-500);
}

.status-disabled {
  background-color: var(--gray-500);
}

.status-error {
  background-color: var(--red-500);
}

/* MODERN TABBED INTERFACE STYLING */
.crawler-detail-tabs {
  margin-top: 2rem;
  background: var(--surface-card);
  border-radius: var(--tab-border-radius);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  border: 1px solid var(--surface-border);
}

/* Tab List Container */
.crawler-detail-tabs :deep(.p-tabs-tablist) {
  background: linear-gradient(135deg, 
    var(--surface-card) 0%, 
    var(--surface-50) 50%, 
    var(--surface-card) 100%);
  border: none;
  border-bottom: 2px solid var(--surface-border);
  padding: 0.75rem 1rem;
  display: flex;
  gap: 0.5rem;
  position: relative;
}

.crawler-detail-tabs :deep(.p-tabs-tablist::before) {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    var(--primary-color) 50%, 
    transparent 100%);
  opacity: 0.3;
}

/* Individual Tab Styling */
.crawler-detail-tabs :deep(.p-tabs-tab) {
  background: transparent;
  border: 2px solid transparent;
  border-radius: 12px;
  padding: 1rem 1.5rem;
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--text-color-secondary);
  cursor: pointer;
  transition: all 0.3s var(--transition-easing);
  position: relative;
  overflow: hidden;
  min-width: 120px;
  text-align: center;
}

/* Tab Hover Effect */
.crawler-detail-tabs :deep(.p-tabs-tab:hover:not([data-p-disabled="true"])) {
  background: linear-gradient(135deg, 
    var(--surface-hover) 0%, 
    var(--surface-100) 100%);
  color: var(--text-color);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
  border-color: var(--surface-border);
}

/* Active Tab Styling */
.crawler-detail-tabs :deep(.p-tabs-tab[data-p-active="true"]) {
  background: linear-gradient(135deg, 
    var(--primary-color) 0%, 
    var(--primary-600) 100%);
  color: var(--primary-color-text);
  border-color: var(--primary-color);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(var(--primary-color-rgb), 0.3);
  position: relative;
  z-index: 2;
}

.crawler-detail-tabs :deep(.p-tabs-tab[data-p-active="true"]) i {
  color: var(--primary-color-text);
}

/* Disabled Tab Styling */
.crawler-detail-tabs :deep(.p-tabs-tab[data-p-disabled="true"]) {
  opacity: 0.4;
  cursor: not-allowed;
  background: var(--surface-100);
  color: var(--text-color-secondary);
}

.crawler-detail-tabs :deep(.p-tabs-tab[data-p-disabled="true"]:hover) {
  transform: none;
  box-shadow: none;
  background: var(--surface-100);
}

/* Tab Icon Styling */
.crawler-detail-tabs :deep(.p-tabs-tab) i {
  margin-right: 0.5rem;
  font-size: 1rem;
  transition: all 0.3s ease;
}

/* Tab Panels Container */
.crawler-detail-tabs :deep(.p-tabs-tabpanels) {
  background: var(--surface-card);
  padding: 2.5rem;
  min-height: 500px;
  position: relative;
}

/* Individual Tab Panel */
.crawler-detail-tabs :deep(.p-tabs-tabpanel) {
  padding: 0;
  animation: fadeInUp 0.5s ease-out;
}

/* Enhanced Card Styling Within Tabs */
.crawler-detail-tabs .card {
  background: linear-gradient(135deg, 
    var(--surface-card) 0%, 
    var(--surface-50) 100%);
  border: 1px solid var(--surface-border);
  border-radius: var(--card-border-radius);
  box-shadow: var(--shadow-light);
  transition: all var(--transition-duration) var(--transition-easing);
  position: relative;
  overflow: hidden;
}

.crawler-detail-tabs .card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, 
    var(--primary-color), 
    var(--primary-600));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.crawler-detail-tabs .card:hover {
  box-shadow: var(--shadow-heavy);
  transform: translateY(-4px);
}

.crawler-detail-tabs .card:hover::before {
  opacity: 1;
}

/* Card Headers */
.crawler-detail-tabs .card h5 {
  color: var(--text-color);
  font-weight: 700;
  font-size: 1.2rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid var(--surface-border);
  display: flex;
  align-items: center;
}

.crawler-detail-tabs .card h5 i {
  color: var(--primary-color);
  font-size: 1.3rem;
  margin-right: 0.75rem;
}

/* Enhanced Metrics Styling */
.crawler-detail-tabs :deep(.metrics-component) {
  background: transparent;
}

.crawler-detail-tabs :deep(.metrics-component .card) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0;
  margin-bottom: 0;
}

.crawler-detail-tabs :deep(.metric-card) {
  background: linear-gradient(135deg, 
    var(--surface-card) 0%, 
    var(--surface-50) 100%);
  border: 1px solid var(--surface-border);
  border-radius: var(--card-border-radius);
  padding: 1.75rem;
  transition: all 0.3s var(--transition-easing);
  position: relative;
  overflow: hidden;
}

.crawler-detail-tabs :deep(.metric-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, 
    var(--primary-color), 
    var(--primary-600));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.crawler-detail-tabs :deep(.metric-card:hover) {
  transform: translateY(-6px);
  box-shadow: var(--shadow-heavy);
}

.crawler-detail-tabs :deep(.metric-card:hover::before) {
  opacity: 1;
}

/* Chart Panel Styling */
.crawler-detail-tabs :deep(.chart-panel) {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--card-border-radius);
  overflow: hidden;
  box-shadow: var(--shadow-light);
  transition: all 0.3s var(--transition-easing);
}

.crawler-detail-tabs :deep(.chart-panel:hover) {
  box-shadow: var(--shadow-heavy);
  transform: translateY(-3px);
}

.crawler-detail-tabs :deep(.chart-panel .p-panel-header) {
  background: linear-gradient(135deg, 
    var(--surface-50) 0%, 
    var(--surface-100) 100%);
  border-bottom: 2px solid var(--surface-border);
  padding: 1.5rem 2rem;
  font-weight: 600;
}

.crawler-detail-tabs :deep(.chart-panel .p-panel-content) {
  padding: 2rem;
  background: var(--surface-card);
}

/* Job History Styling */
.crawler-detail-tabs :deep(.job-history) {
  background: transparent;
}

.crawler-detail-tabs :deep(.job-history-header) {
  background: linear-gradient(135deg, 
    var(--surface-50) 0%, 
    var(--surface-100) 100%);
  padding: 1.5rem 2rem;
  border-bottom: 2px solid var(--surface-border);
  border-radius: var(--card-border-radius) var(--card-border-radius) 0 0;
}

.crawler-detail-tabs :deep(.job-history-header h3) {
  color: var(--text-color);
  font-weight: 700;
  font-size: 1.2rem;
  margin: 0;
}

.crawler-detail-tabs :deep(.job-history-content) {
  background: var(--surface-card);
  border-radius: 0 0 var(--card-border-radius) var(--card-border-radius);
  overflow: hidden;
}

.crawler-detail-tabs :deep(.job-history-table) {
  border: none;
}

.crawler-detail-tabs :deep(.job-history-table .p-datatable-tbody > tr) {
  transition: all 0.2s ease;
}

.crawler-detail-tabs :deep(.job-history-table .p-datatable-tbody > tr:hover) {
  background: var(--surface-hover);
  transform: scale(1.005);
}

/* Enhanced Endpoint Styling */
.crawler-detail-tabs .surface-100 {
  background: linear-gradient(135deg, 
    var(--surface-100) 0%, 
    var(--surface-200) 100%);
  border: 1px solid var(--surface-border);
  border-radius: 8px;
  transition: all 0.2s ease;
}

.crawler-detail-tabs .surface-100:hover {
  background: linear-gradient(135deg, 
    var(--surface-200) 0%, 
    var(--surface-300) 100%);
  box-shadow: var(--shadow-light);
  transform: translateY(-1px);
}

/* Enhanced Tag Styling */
.crawler-detail-tabs :deep(.p-tag) {
  font-weight: 700;
  padding: 0.6rem 1.2rem;
  border-radius: 25px;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Animation Keyframes */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Focus States for Accessibility */
.crawler-detail-tabs :deep(.p-tabs-tab:focus-visible) {
  outline: 3px solid var(--primary-color);
  outline-offset: 3px;
  border-radius: 12px;
}

/* Button Enhancements */
.crawler-detail-tabs :deep(.p-button) {
  border-radius: 8px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.crawler-detail-tabs :deep(.p-button:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

/* Header action buttons responsive behavior */
@media screen and (max-width: 1200px) {
  .flex.align-items-center.gap-2.flex-wrap .p-button {
    font-size: 0.875rem;
    padding: 0.5rem 0.75rem;
  }
  
  .flex.align-items-center.gap-2.flex-wrap .p-button .p-button-label {
    display: none;
  }
}

@media screen and (max-width: 768px) {
  .surface-0.shadow-2 .flex.align-items-center.mb-5 {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .flex.align-items-center.gap-2.flex-wrap {
    width: 100%;
    justify-content: flex-start;
  }
  
  .flex.align-items-center.gap-2.flex-wrap .p-button {
    flex: 1;
    min-width: auto;
  }
}

/* Enhanced Tab styling for UI facelift */
.crawler-detail-tabs {
  margin-top: 1.5rem;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  border-radius: 12px;
  overflow: hidden;
  background: var(--surface-card);
}

.crawler-detail-tabs :deep(.p-tabs-tablist) {
  background: linear-gradient(135deg, var(--surface-card) 0%, var(--surface-50) 100%);
  border: none;
  border-bottom: 1px solid var(--surface-border);
  border-radius: 12px 12px 0 0;
  padding: 0.5rem;
  box-shadow: inset 0 -1px 0 var(--surface-border);
}

.crawler-detail-tabs :deep(.p-tabs-tab) {
  padding: 1rem 2rem;
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--text-color-secondary);
  border: none;
  background: transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  border-radius: 8px;
  margin: 0 0.25rem;
  position: relative;
  overflow: hidden;
}

.crawler-detail-tabs :deep(.p-tabs-tab::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-600));
  opacity: 0;
  transition: opacity 0.3s ease;
  border-radius: 8px;
}

.crawler-detail-tabs :deep(.p-tabs-tab > *) {
  position: relative;
  z-index: 1;
}

.crawler-detail-tabs :deep(.p-tabs-tab:hover) {
  background: var(--surface-hover);
  color: var(--text-color);
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

.crawler-detail-tabs :deep(.p-tabs-tab:focus) {
  outline: none;
  box-shadow: 0 0 0 3px rgba(var(--primary-color-rgb), 0.2);
}

.crawler-detail-tabs :deep(.p-tabs-tab[data-p-active="true"]) {
  background: var(--primary-color);
  color: var(--primary-color-text);
  box-shadow: 0 4px 12px rgba(var(--primary-color-rgb), 0.3);
  transform: translateY(-2px);
}

.crawler-detail-tabs :deep(.p-tabs-tab[data-p-active="true"]::before) {
  opacity: 1;
}

.crawler-detail-tabs :deep(.p-tabs-tab[data-p-active="true"]) i {
  color: var(--primary-color-text);
}

.crawler-detail-tabs :deep(.p-tabs-tabpanels) {
  background: var(--surface-card);
  border: none;
  border-radius: 0 0 12px 12px;
  padding: 2rem;
  min-height: 400px;
}

.crawler-detail-tabs :deep(.p-tabs-tabpanel) {
  padding: 0;
  animation: fadeInUp 0.4s ease-out;
}

/* Disabled tab styling */
.crawler-detail-tabs :deep(.p-tabs-tab[data-p-disabled="true"]) {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--surface-100);
  color: var(--text-color-secondary);
}

.crawler-detail-tabs :deep(.p-tabs-tab[data-p-disabled="true"]:hover) {
  background: var(--surface-100);
  color: var(--text-color-secondary);
  transform: none;
  box-shadow: none;
}

/* Enhanced card styling within tabs */
.crawler-detail-tabs .card {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;
}

.crawler-detail-tabs .card:hover {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.crawler-detail-tabs .card h5 {
  color: var(--text-color);
  font-weight: 600;
  font-size: 1.1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 0.75rem;
  border-bottom: 2px solid var(--surface-border);
}

.crawler-detail-tabs .card h5 i {
  color: var(--primary-color);
  font-size: 1.2rem;
}

/* Enhanced metrics and charts styling */
.crawler-detail-tabs :deep(.metrics-component) {
  background: transparent;
}

.crawler-detail-tabs :deep(.metrics-component .card) {
  border: none;
  box-shadow: none;
  background: transparent;
  padding: 0;
  margin-bottom: 0;
}

.crawler-detail-tabs :deep(.metric-card) {
  background: linear-gradient(135deg, var(--surface-card) 0%, var(--surface-50) 100%);
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  padding: 1.5rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.crawler-detail-tabs :deep(.metric-card::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-color), var(--primary-600));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.crawler-detail-tabs :deep(.metric-card:hover) {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15);
}

.crawler-detail-tabs :deep(.metric-card:hover::before) {
  opacity: 1;
}

.crawler-detail-tabs :deep(.chart-panel) {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.crawler-detail-tabs :deep(.chart-panel:hover) {
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.crawler-detail-tabs :deep(.chart-panel .p-panel-header) {
  background: linear-gradient(135deg, var(--surface-50) 0%, var(--surface-100) 100%);
  border-bottom: 1px solid var(--surface-border);
  padding: 1.25rem 1.5rem;
}

.crawler-detail-tabs :deep(.chart-panel .p-panel-content) {
  padding: 1.5rem;
  background: var(--surface-card);
}

/* Enhanced job history styling */
.crawler-detail-tabs :deep(.job-history) {
  background: transparent;
}

.crawler-detail-tabs :deep(.job-history-header) {
  background: linear-gradient(135deg, var(--surface-50) 0%, var(--surface-100) 100%);
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--surface-border);
  border-radius: 12px 12px 0 0;
}

.crawler-detail-tabs :deep(.job-history-header h3) {
  color: var(--text-color);
  font-weight: 600;
  font-size: 1.1rem;
  margin: 0;
}

.crawler-detail-tabs :deep(.job-history-content) {
  background: var(--surface-card);
  border-radius: 0 0 12px 12px;
  overflow: hidden;
}

.crawler-detail-tabs :deep(.job-history-table) {
  border: none;
}

.crawler-detail-tabs :deep(.job-history-table .p-datatable-header) {
  background: var(--surface-50);
  border: none;
  padding: 1rem 1.5rem;
}

.crawler-detail-tabs :deep(.job-history-table .p-datatable-tbody > tr) {
  transition: all 0.2s ease;
}

.crawler-detail-tabs :deep(.job-history-table .p-datatable-tbody > tr:hover) {
  background: var(--surface-hover);
  transform: scale(1.01);
}

/* Animation keyframes */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Enhanced endpoint styling */
.crawler-detail-tabs .surface-100 {
  background: linear-gradient(135deg, var(--surface-100) 0%, var(--surface-200) 100%);
  border: 1px solid var(--surface-border);
  transition: all 0.2s ease;
}

.crawler-detail-tabs .surface-100:hover {
  background: linear-gradient(135deg, var(--surface-200) 0%, var(--surface-300) 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Enhanced tag styling */
.crawler-detail-tabs :deep(.p-tag) {
  font-weight: 600;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* Responsive adjustments */
@media screen and (max-width: 991px) {
  .crawler-detail-container {
    padding: 1.5rem;
  }
}

/* Enhanced responsive design */
@media screen and (max-width: 1024px) {
  .crawler-detail-tabs :deep(.p-tabs-tabpanels) {
    padding: 1.5rem;
  }
  
  .crawler-detail-tabs :deep(.metric-card) {
    padding: 1.25rem;
  }
  
  .crawler-detail-tabs :deep(.chart-panel .p-panel-content) {
    padding: 1.25rem;
  }
}

@media screen and (max-width: 768px) {
  .crawler-detail-tabs {
    margin-top: 1rem;
    border-radius: 8px;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tablist) {
    padding: 0.25rem;
    border-radius: 8px 8px 0 0;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tab) {
    padding: 0.75rem 1.25rem;
    font-size: 0.875rem;
    margin: 0 0.125rem;
    border-radius: 6px;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tabpanels) {
    padding: 1.25rem;
    border-radius: 0 0 8px 8px;
  }
  
  .crawler-detail-tabs .card {
    border-radius: 8px;
  }
  
  .crawler-detail-tabs :deep(.metric-card) {
    padding: 1rem;
    border-radius: 8px;
  }
  
  .crawler-detail-tabs :deep(.chart-panel) {
    border-radius: 8px;
  }
}

@media screen and (max-width: 575px) {
  .crawler-detail-container {
    padding: 1rem;
  }
  
  .card {
    padding: 1rem;
    border-radius: 8px;
  }
  
  .flex.align-items-center.gap-2.flex-wrap .p-button {
    font-size: 0.75rem;
    padding: 0.375rem 0.5rem;
  }
  
  .crawler-detail-tabs {
    border-radius: 6px;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tablist) {
    padding: 0.25rem;
    border-radius: 6px 6px 0 0;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tab) {
    padding: 0.625rem 1rem;
    font-size: 0.8rem;
    margin: 0 0.125rem;
    border-radius: 4px;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tab i) {
    margin-right: 0.375rem;
    font-size: 0.875rem;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tabpanels) {
    padding: 1rem;
    border-radius: 0 0 6px 6px;
  }
  
  .crawler-detail-tabs .card h5 {
    font-size: 1rem;
    margin-bottom: 1rem;
    padding-bottom: 0.5rem;
  }
  
  .crawler-detail-tabs :deep(.metric-card) {
    padding: 0.875rem;
    border-radius: 6px;
  }
  
  .crawler-detail-tabs :deep(.chart-panel) {
    border-radius: 6px;
  }
  
  .crawler-detail-tabs :deep(.chart-panel .p-panel-header) {
    padding: 1rem;
  }
  
  .crawler-detail-tabs :deep(.chart-panel .p-panel-content) {
    padding: 1rem;
  }
}

/* Dark mode enhancements */
@media (prefers-color-scheme: dark) {
  .crawler-detail-tabs :deep(.p-tabs-tablist) {
    background: linear-gradient(135deg, var(--surface-800) 0%, var(--surface-700) 100%);
  }
  
  .crawler-detail-tabs :deep(.metric-card) {
    background: linear-gradient(135deg, var(--surface-800) 0%, var(--surface-700) 100%);
  }
  
  .crawler-detail-tabs :deep(.chart-panel .p-panel-header) {
    background: linear-gradient(135deg, var(--surface-700) 0%, var(--surface-600) 100%);
  }
  
  .crawler-detail-tabs .surface-100 {
    background: linear-gradient(135deg, var(--surface-700) 0%, var(--surface-600) 100%);
  }
  
  .crawler-detail-tabs .surface-100:hover {
    background: linear-gradient(135deg, var(--surface-600) 0%, var(--surface-500) 100%);
  }
}

/* Loading and transition improvements */
.crawler-detail-tabs :deep(.p-progressspinner) {
  width: 2rem;
  height: 2rem;
}

.crawler-detail-tabs :deep(.p-progressspinner .p-progressspinner-circle) {
  stroke: var(--primary-color);
  stroke-width: 3;
}

/* Enhanced focus states for accessibility */
.crawler-detail-tabs :deep(.p-tabs-tab:focus-visible) {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
  border-radius: 8px;
}

/* Smooth scrolling for tab content */
.crawler-detail-tabs :deep(.p-tabs-tabpanel) {
  scroll-behavior: smooth;
}

/* Enhanced button styling within tabs */
.crawler-detail-tabs :deep(.p-button) {
  border-radius: 8px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.crawler-detail-tabs :deep(.p-button:hover) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.12);
}

/* Responsive Design */
@media screen and (max-width: 1024px) {
  .crawler-detail-tabs :deep(.p-tabs-tabpanels) {
    padding: 2rem;
  }
  
  .crawler-detail-tabs :deep(.metric-card) {
    padding: 1.5rem;
  }
}

@media screen and (max-width: 768px) {
  .crawler-detail-tabs {
    margin-top: 1.5rem;
    border-radius: 12px;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tablist) {
    padding: 0.5rem;
    flex-direction: column;
    gap: 0.25rem;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tab) {
    padding: 0.75rem 1rem;
    font-size: 0.9rem;
    min-width: auto;
    width: 100%;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tabpanels) {
    padding: 1.5rem;
  }
  
  .crawler-detail-tabs .card {
    border-radius: 10px;
  }
}

@media screen and (max-width: 575px) {
  .crawler-detail-container {
    padding: 1rem;
  }
  
  .crawler-detail-tabs {
    border-radius: 10px;
  }
  
  .crawler-detail-tabs :deep(.p-tabs-tabpanels) {
    padding: 1rem;
  }
  
  .crawler-detail-tabs .card {
    padding: 1rem;
    border-radius: 8px;
  }
  
  .crawler-detail-tabs .card h5 {
    font-size: 1.1rem;
    margin-bottom: 1rem;
  }
}

/* Dark Mode Enhancements */
@media (prefers-color-scheme: dark) {
  .crawler-detail-tabs :deep(.p-tabs-tablist) {
    background: linear-gradient(135deg, 
      var(--surface-800) 0%, 
      var(--surface-700) 50%, 
      var(--surface-800) 100%);
  }
  
  .crawler-detail-tabs :deep(.metric-card) {
    background: linear-gradient(135deg, 
      var(--surface-800) 0%, 
      var(--surface-700) 100%);
  }
  
  .crawler-detail-tabs :deep(.chart-panel .p-panel-header) {
    background: linear-gradient(135deg, 
      var(--surface-700) 0%, 
      var(--surface-600) 100%);
  }
}

/* Header action buttons responsive behavior */
@media screen and (max-width: 1200px) {
  .flex.align-items-center.gap-2.flex-wrap .p-button {
    font-size: 0.875rem;
    padding: 0.5rem 0.75rem;
  }
  
  .flex.align-items-center.gap-2.flex-wrap .p-button .p-button-label {
    display: none;
  }
}

@media screen and (max-width: 768px) {
  .surface-0.shadow-2 .flex.align-items-center.mb-5 {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .flex.align-items-center.gap-2.flex-wrap {
    width: 100%;
    justify-content: flex-start;
  }
  
  .flex.align-items-center.gap-2.flex-wrap .p-button {
    flex: 1;
    min-width: auto;
  }
}
</style>