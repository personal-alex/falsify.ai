import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useCrawlerStore } from '@/stores/crawler'
import { useHealthStore } from '@/stores/health'
import { useNotificationStore } from '@/stores/notification'
import { ApiService } from '@/services/api'

export function useApp() {
  const crawlerStore = useCrawlerStore()
  const healthStore = useHealthStore()
  const notificationStore = useNotificationStore()
  
  const isInitialized = ref(false)
  const isInitializing = ref(false)
  const initializationError = ref<string | null>(null)

  // Computed properties for application state
  const isOnline = computed(() => crawlerStore.isOnline)
  const connectionStatus = computed(() => crawlerStore.connectionStatus)
  const wsConnectionStatus = computed(() => healthStore.wsConnectionStatus)

  const initialize = async () => {
    if (isInitializing.value) return

    try {
      isInitializing.value = true
      isInitialized.value = false
      initializationError.value = null

      // Check API health first
      const apiHealthy = await ApiService.healthCheck()
      if (!apiHealthy) {
        throw new Error('API service is not available')
      }

      // Initialize WebSocket connections
      healthStore.initWebSocket()
      notificationStore.initWebSocket()
      crawlerStore.initializeWebSocket()

      // Load initial data with enhanced error handling
      await crawlerStore.loadCrawlerData({
        retries: 2,
        timeout: 15000
      })

      // Show success notification
      notificationStore.success(
        'Application Initialized',
        `Loaded ${crawlerStore.totalCrawlers} crawler(s) - ${crawlerStore.healthyCrawlers} healthy, ${crawlerStore.unhealthyCrawlers} unhealthy`
      )

      isInitialized.value = true
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to initialize application'
      initializationError.value = message
      
      notificationStore.error(
        'Initialization Failed',
        message,
        { persistent: true }
      )
      
      console.error('Application initialization failed:', error)
      throw error
    } finally {
      isInitializing.value = false
    }
  }

  const cleanup = () => {
    try {
      // Disconnect WebSocket connections
      healthStore.disconnectWebSocket()
      notificationStore.disconnectWebSocket()
      crawlerStore.cleanup()

      // Clear store states
      crawlerStore.clearError()
      healthStore.clearHealthStatuses()
      
      isInitialized.value = false
      initializationError.value = null
    } catch (error) {
      console.error('Cleanup failed:', error)
    }
  }

  const refresh = async () => {
    try {
      // Refresh crawler data with retry logic
      await crawlerStore.loadCrawlerData({
        retries: 1,
        timeout: 10000
      })
      
      notificationStore.success('Refreshed', 'Data updated successfully')
    } catch (error) {
      notificationStore.handleApiError(error, 'Data Refresh')
      throw error
    }
  }

  const retryInitialization = async () => {
    initializationError.value = null
    await initialize()
  }

  // Health check for the application
  const performHealthCheck = async () => {
    try {
      const apiHealthy = await ApiService.healthCheck()
      const wsConnected = healthStore.isWebSocketConnected
      
      const status = {
        api: apiHealthy,
        websocket: wsConnected,
        overall: apiHealthy && wsConnected
      }

      if (status.overall) {
        notificationStore.success('Health Check', 'All systems operational')
      } else {
        const issues = []
        if (!status.api) issues.push('API service')
        if (!status.websocket) issues.push('WebSocket connection')
        
        notificationStore.warn(
          'Health Check Warning',
          `Issues detected with: ${issues.join(', ')}`
        )
      }

      return status
    } catch (error) {
      notificationStore.error('Health Check Failed', 'Unable to perform health check')
      throw error
    }
  }

  // Force refresh of specific crawler
  const refreshCrawler = async (crawlerId: string) => {
    try {
      await crawlerStore.refreshCrawlerHealth(crawlerId, {
        retries: 1,
        timeout: 5000
      })
      
      notificationStore.success(
        'Crawler Refreshed',
        `Health status updated for ${crawlerId}`
      )
    } catch (error) {
      notificationStore.handleApiError(error, `Refresh ${crawlerId}`)
      throw error
    }
  }

  // Trigger crawl with enhanced error handling
  const triggerCrawl = async (crawlerId: string, options?: any) => {
    try {
      const response = await crawlerStore.triggerCrawl(crawlerId, options, {
        retries: 1,
        timeout: 10000
      })

      if (response.status === 'ACCEPTED') {
        notificationStore.success(
          'Crawl Started',
          `Crawl initiated for ${crawlerId}`,
          { crawlerId }
        )
      } else {
        notificationStore.warn(
          'Crawl Status',
          response.message || 'Crawl request processed with warnings',
          { crawlerId }
        )
      }

      return response
    } catch (error) {
      notificationStore.handleApiError(error, `Start Crawl for ${crawlerId}`)
      throw error
    }
  }

  onMounted(() => {
    initialize()
  })

  onUnmounted(() => {
    cleanup()
  })

  return {
    // State
    isInitialized,
    isInitializing,
    initializationError,
    isOnline,
    connectionStatus,
    wsConnectionStatus,
    
    // Actions
    initialize,
    cleanup,
    refresh,
    retryInitialization,
    performHealthCheck,
    refreshCrawler,
    triggerCrawl,
    
    // Store references for convenience
    crawlerStore,
    healthStore,
    notificationStore
  }
}