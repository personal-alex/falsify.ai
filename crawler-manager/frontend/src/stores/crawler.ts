import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CrawlerConfiguration, CrawlerHealthData } from '@/types/health'
import type { CrawlerMetrics } from '@/types/ui'
import { ApiService, ApiServiceError, type ConnectionStatus } from '@/services/api'
import { getWebSocketService, eventBus, type JobUpdateMessage, type MetricsUpdateMessage } from '@/services/websocket'

// Extended crawler data interface
export interface CrawlerData extends CrawlerHealthData {
  metrics?: CrawlerMetrics
  recentJobs?: any[]
  isActive?: boolean
}

export const useCrawlerStore = defineStore('crawler', () => {
  // State
  const crawlers = ref<CrawlerConfiguration[]>([])
  const crawlerData = ref<CrawlerData[]>([])
  const isLoading = ref(false)
  const error = ref<ApiServiceError | null>(null)
  const lastUpdated = ref<Date | null>(null)
  const connectionStatus = ref<ConnectionStatus>({ online: true, lastCheck: new Date() })

  // WebSocket service - initialize it to ensure connection
  getWebSocketService()
  let wsUnsubscribers: (() => void)[] = []

  // Getters
  const totalCrawlers = computed(() => crawlers.value.length)
  
  const healthyCrawlers = computed(() => 
    crawlerData.value.filter(c => c.health.status === 'HEALTHY').length
  )
  
  const unhealthyCrawlers = computed(() => 
    crawlerData.value.filter(c => c.health.status === 'UNHEALTHY').length
  )
  
  const unknownCrawlers = computed(() => 
    crawlerData.value.filter(c => c.health.status === 'UNKNOWN').length
  )

  const activeCrawlers = computed(() =>
    crawlerData.value.filter(c => c.isActive).length
  )

  const getCrawlerById = computed(() => {
    return (id: string) => crawlers.value.find(c => c.id === id)
  })

  const getCrawlerDataById = computed(() => {
    return (id: string) => crawlerData.value.find(c => c.configuration.id === id)
  })

  const isOnline = computed(() => connectionStatus.value.online)

  // Actions
  const initializeWebSocket = () => {
    // Clean up existing subscriptions
    wsUnsubscribers.forEach(unsub => unsub())
    wsUnsubscribers = []

    // Subscribe to job updates via event bus
    wsUnsubscribers.push(
      eventBus.on('job:updated', (message: JobUpdateMessage) => {
        updateCrawlerJobStatus(message.crawlerId, message)
      })
    )

    // Subscribe to metrics updates via event bus
    wsUnsubscribers.push(
      eventBus.on('metrics:updated', (message: MetricsUpdateMessage) => {
        // Convert the metrics to the expected format
        const metrics: CrawlerMetrics = {
          crawlerId: message.crawlerId,
          articlesProcessed: message.metrics.articlesProcessed,
          successRate: message.metrics.successRate,
          averageProcessingTimeMs: message.metrics.averageProcessingTime,
          errorCount: message.metrics.errorCount,
          lastUpdated: message.timestamp,
          totalCrawlsExecuted: 0,
          totalExecutionTimeMs: 0,
          activeCrawls: 0
        }
        updateCrawlerMetrics(message.crawlerId, metrics)
      })
    )

    // Monitor connection status
    wsUnsubscribers.push(
      ApiService.onConnectionStatusChange((status) => {
        connectionStatus.value = status
      })
    )
  }

  const updateCrawlerJobStatus = (crawlerId: string, jobUpdate: JobUpdateMessage) => {
    const crawlerIndex = crawlerData.value.findIndex(c => c.configuration.id === crawlerId)
    if (crawlerIndex !== -1) {
      const crawler = crawlerData.value[crawlerIndex]
      
      // Update active status based on job status
      crawler.isActive = jobUpdate.type === 'job.started' || jobUpdate.type === 'job.progress'
      
      // Update recent jobs if available
      if (crawler.recentJobs) {
        const existingJobIndex = crawler.recentJobs.findIndex(job => job.id === jobUpdate.jobId)
        const jobData = {
          id: jobUpdate.jobId,
          status: jobUpdate.status,
          progress: jobUpdate.progress,
          articlesProcessed: jobUpdate.articlesProcessed,
          timestamp: jobUpdate.timestamp,
          details: jobUpdate.details
        }

        if (existingJobIndex !== -1) {
          crawler.recentJobs[existingJobIndex] = { ...crawler.recentJobs[existingJobIndex], ...jobData }
        } else {
          crawler.recentJobs.unshift(jobData)
          // Keep only the 5 most recent jobs
          crawler.recentJobs = crawler.recentJobs.slice(0, 5)
        }
      }
    }
  }

  const updateCrawlerMetrics = (crawlerId: string, metrics: CrawlerMetrics) => {
    const crawlerIndex = crawlerData.value.findIndex(c => c.configuration.id === crawlerId)
    if (crawlerIndex !== -1) {
      crawlerData.value[crawlerIndex].metrics = metrics
    }
  }

  const loadCrawlers = async (options?: { retries?: number; timeout?: number }) => {
    try {
      isLoading.value = true
      error.value = null
      
      const configurations = await ApiService.getCrawlerConfigurations(options)
      crawlers.value = configurations
      
      return configurations
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to load crawlers'
      )
      error.value = apiError
      throw apiError
    } finally {
      isLoading.value = false
    }
  }

  const loadCrawlerData = async (options?: { retries?: number; timeout?: number }) => {
    try {
      isLoading.value = true
      error.value = null

      // Load crawler configurations and health data in parallel
      const [configurations, healthData] = await Promise.all([
        ApiService.getCrawlerConfigurations(options),
        ApiService.getAllCrawlerHealth(options)
      ])

      crawlers.value = configurations

      // Load additional data for each crawler
      const crawlerDataPromises = configurations.map(async (config) => {
        const health = healthData[config.id] || {
          status: 'UNKNOWN' as const,
          message: 'No health data available',
          lastCheck: '',
          responseTimeMs: null,
          crawlerId: config.id
        }

        try {
          // Try to load metrics and job history in parallel
          const [metrics, recentJobs] = await Promise.all([
            ApiService.getCrawlerMetrics(config.id, undefined, { retries: 1, timeout: 5000 }).catch(() => null),
            ApiService.getRecentJobs(config.id, { retries: 1, timeout: 5000 }).catch(() => [])
          ])

          return {
            configuration: config,
            health,
            metrics,
            recentJobs,
            isActive: recentJobs?.some((job: any) => job.status === 'running') || false
          }
        } catch {
          // If additional data fails to load, return basic data
          return {
            configuration: config,
            health,
            isActive: false
          }
        }
      })

      crawlerData.value = await Promise.all(crawlerDataPromises)
      lastUpdated.value = new Date()
      
      return crawlerData.value
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to load crawler data'
      )
      error.value = apiError
      throw apiError
    } finally {
      isLoading.value = false
    }
  }

  const refreshCrawlerHealth = async (crawlerId: string, options?: { retries?: number; timeout?: number }) => {
    try {
      const updatedHealth = await ApiService.forceHealthCheck(crawlerId, options)
      
      // Update the health data in the store
      const crawlerIndex = crawlerData.value.findIndex(
        c => c.configuration.id === crawlerId
      )
      
      if (crawlerIndex !== -1) {
        crawlerData.value[crawlerIndex].health = updatedHealth
        lastUpdated.value = new Date()
      }
      
      return updatedHealth
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to refresh crawler health'
      )
      error.value = apiError
      throw apiError
    }
  }

  const triggerCrawl = async (
    crawlerId: string, 
    request?: any, 
    options?: { retries?: number; timeout?: number }
  ) => {
    try {
      const response = await ApiService.triggerCrawl(crawlerId, request, options)
      
      // Update crawler active status optimistically
      const crawlerIndex = crawlerData.value.findIndex(c => c.configuration.id === crawlerId)
      if (crawlerIndex !== -1 && response.status === 'ACCEPTED') {
        crawlerData.value[crawlerIndex].isActive = true
      }
      
      return response
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to trigger crawl'
      )
      error.value = apiError
      throw apiError
    }
  }

  const getCrawlerStatus = async (crawlerId: string, options?: { retries?: number; timeout?: number }) => {
    try {
      return await ApiService.getCrawlerStatus(crawlerId, options)
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to get crawler status'
      )
      error.value = apiError
      throw apiError
    }
  }

  const getCrawlerMetrics = async (
    crawlerId: string, 
    timeRange?: string, 
    options?: { retries?: number; timeout?: number }
  ) => {
    try {
      const metrics = await ApiService.getCrawlerMetrics(crawlerId, timeRange, options)
      
      // Update metrics in store
      const crawlerIndex = crawlerData.value.findIndex(c => c.configuration.id === crawlerId)
      if (crawlerIndex !== -1) {
        crawlerData.value[crawlerIndex].metrics = metrics
      }
      
      return metrics
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to get crawler metrics'
      )
      error.value = apiError
      throw apiError
    }
  }

  const getRecentJobs = async (
    crawlerId: string, 
    options?: { retries?: number; timeout?: number }
  ) => {
    try {
      const jobs = await ApiService.getRecentJobs(crawlerId, options)
      
      // Update job history in store
      const crawlerIndex = crawlerData.value.findIndex(c => c.configuration.id === crawlerId)
      if (crawlerIndex !== -1) {
        crawlerData.value[crawlerIndex].recentJobs = jobs
      }
      
      return jobs
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to get recent jobs'
      )
      error.value = apiError
      throw apiError
    }
  }

  const getJobHistory = async (
    crawlerId: string, 
    page: number = 0,
    size: number = 10,
    options?: { retries?: number; timeout?: number }
  ) => {
    try {
      const jobs = await ApiService.getJobHistory(crawlerId, page, size, options)
      return jobs
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to get job history'
      )
      error.value = apiError
      throw apiError
    }
  }

  const getJobDetails = async (
    jobId: string,
    options?: { retries?: number; timeout?: number }
  ) => {
    try {
      return await ApiService.getJobDetails(jobId, options)
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to get job details'
      )
      error.value = apiError
      throw apiError
    }
  }

  const cancelJob = async (
    jobId: string,
    options?: { retries?: number; timeout?: number }
  ) => {
    try {
      return await ApiService.cancelJob(jobId, options)
    } catch (err) {
      const apiError = err instanceof ApiServiceError ? err : new ApiServiceError(
        err instanceof Error ? err.message : 'Failed to cancel job'
      )
      error.value = apiError
      throw apiError
    }
  }

  const clearError = () => {
    error.value = null
  }

  const cleanup = () => {
    wsUnsubscribers.forEach(unsub => unsub())
    wsUnsubscribers = []
  }

  return {
    // State
    crawlers,
    crawlerData,
    isLoading,
    error,
    lastUpdated,
    connectionStatus,
    
    // Getters
    totalCrawlers,
    healthyCrawlers,
    unhealthyCrawlers,
    unknownCrawlers,
    activeCrawlers,
    getCrawlerById,
    getCrawlerDataById,
    isOnline,
    
    // Actions
    initializeWebSocket,
    loadCrawlers,
    loadCrawlerData,
    refreshCrawlerHealth,
    triggerCrawl,
    getCrawlerStatus,
    getCrawlerMetrics,
    getRecentJobs,
    getJobHistory,
    getJobDetails,
    cancelJob,
    clearError,
    cleanup
  }
})