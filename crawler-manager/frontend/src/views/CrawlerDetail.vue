<template>
  <div class="crawler-detail">
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
              class="health-indicator"
              :class="healthIndicatorClass"
              v-if="health"
            >
              <i :class="healthIconClass"></i>
            </div>
          </div>
          <span class="text-600 text-xl">{{ crawlerId }}</span>
        </div>
        <div class="flex align-items-center">
          <Button
            icon="pi pi-refresh"
            label="Refresh"
            class="p-button-sm"
            :loading="isRefreshing"
            @click="forceHealthCheck"
          />
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

      <!-- Crawler Details -->
      <div v-else class="grid">
        <!-- Configuration Card -->
        <div class="col-12 lg:col-6">
          <div class="card h-full">
            <h5 class="flex align-items-center">
              <i class="pi pi-cog mr-2"></i>
              Configuration
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

        <!-- Actions Card -->
        <div class="col-12">
          <div class="card">
            <h5 class="flex align-items-center">
              <i class="pi pi-wrench mr-2"></i>
              Actions
            </h5>
            <div class="flex flex-wrap gap-2">
              <div class="flex align-items-center">
                <Button
                  icon="pi pi-play"
                  label="Start Crawl"
                  :loading="isCrawling"
                  :disabled="!configuration?.enabled || health?.status !== 'HEALTHY'"
                  @click="triggerCrawl"
                  class="p-button-success"
                />
                <small 
                  v-if="!configuration?.enabled || health?.status !== 'HEALTHY'" 
                  class="text-600 ml-2"
                >
                  {{ !configuration?.enabled ? 'Crawler is disabled' : 'Crawler must be healthy to start crawl' }}
                </small>
              </div>
              <Button
                icon="pi pi-refresh"
                label="Force Health Check"
                :loading="isRefreshing"
                @click="forceHealthCheck"
              />
              <Button
                icon="pi pi-info-circle"
                label="Check Crawl Status"
                class="p-button-outlined"
                @click="checkCrawlStatus"
                v-if="configuration"
              />
              <Button
                icon="pi pi-external-link"
                label="Open Health Endpoint"
                class="p-button-outlined"
                @click="openHealthEndpoint"
                v-if="configuration"
              />
            </div>
          </div>
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
import Tag from 'primevue/tag'
import type { CrawlerConfiguration, HealthStatus, CrawlResponse } from '@/types/health'
import { ApiService } from '@/services/api'
import { HealthWebSocketService, type HealthUpdateMessage } from '@/services/websocket'

interface Props {
  id: string
}

const props = defineProps<Props>()
const toast = useToast()

// State
const isLoading = ref(true)
const isRefreshing = ref(false)
const isCrawling = ref(false)
const error = ref<string | null>(null)
const configuration = ref<CrawlerConfiguration | null>(null)
const health = ref<HealthStatus | null>(null)
const lastUpdated = ref<Date | null>(null)
const lastCrawlResponse = ref<CrawlResponse | null>(null)

// WebSocket service
let wsService: HealthWebSocketService | null = null
let wsUnsubscribe: (() => void) | null = null

// Computed properties
const crawlerId = computed(() => props.id)
const crawlerName = computed(() => configuration.value?.name || props.id)

const healthIndicatorClass = computed(() => {
  if (!health.value) return 'health-unknown'
  
  switch (health.value.status) {
    case 'HEALTHY':
      return 'health-healthy'
    case 'UNHEALTHY':
      return 'health-unhealthy'
    default:
      return 'health-unknown'
  }
})

const healthIconClass = computed(() => {
  if (!health.value) return 'pi pi-question-circle'
  
  switch (health.value.status) {
    case 'HEALTHY':
      return 'pi pi-check-circle'
    case 'UNHEALTHY':
      return 'pi pi-times-circle'
    default:
      return 'pi pi-question-circle'
  }
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
const loadData = async () => {
  try {
    error.value = null
    isLoading.value = true

    // Load crawler configuration and health data in parallel
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

    lastUpdated.value = new Date()
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

const openHealthEndpoint = () => {
  if (configuration.value) {
    const url = configuration.value.baseUrl + configuration.value.healthEndpoint
    window.open(url, '_blank')
  }
}

const triggerCrawl = async () => {
  if (!configuration.value) return

  isCrawling.value = true
  try {
    const response = await ApiService.triggerCrawl(props.id)
    lastCrawlResponse.value = response
    
    // Show appropriate toast based on response status
    switch (response.status) {
      case 'ACCEPTED':
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

const initWebSocket = () => {
  try {
    wsService = new HealthWebSocketService()
    wsUnsubscribe = wsService.onHealthUpdate(handleHealthUpdate)
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
})
</script>

<style scoped>
.crawler-detail {
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

.health-indicator {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  color: white;
}

.health-healthy {
  background-color: var(--green-500);
}

.health-unhealthy {
  background-color: var(--red-500);
}

.health-unknown {
  background-color: var(--orange-500);
}
</style>