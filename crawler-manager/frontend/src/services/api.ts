import axios, { type AxiosInstance, type AxiosResponse, AxiosError } from 'axios'
import type {
  HealthStatus,
  CrawlerConfiguration,
  CrawlRequest,
  CrawlResponse
} from '@/types/health'
import type {
  ApiRequestOptions
} from '@/types/api'

// API Error class for better error handling
export class ApiServiceError extends Error {
  public readonly code?: string
  public readonly status?: number
  public readonly details?: any

  constructor(message: string, code?: string, status?: number, details?: any) {
    super(message)
    this.name = 'ApiServiceError'
    this.code = code
    this.status = status
    this.details = details
  }
}

// Connection status interface
export interface ConnectionStatus {
  online: boolean
  lastCheck: Date
  latency?: number
}

export class ApiService {
  private static instance: AxiosInstance
  private static connectionStatus: ConnectionStatus = {
    online: true,
    lastCheck: new Date()
  }
  private static connectionListeners: ((status: ConnectionStatus) => void)[] = []

  static {
    this.initializeAxios()
  }

  private static initializeAxios() {
    this.instance = axios.create({
      baseURL: '/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // Request interceptor for adding common headers and monitoring
    this.instance.interceptors.request.use(
      (config) => {
        // Add timestamp for latency calculation
        config.metadata = { startTime: Date.now() }
        return config
      },
      (error) => Promise.reject(error)
    )

    // Response interceptor for error handling and connection monitoring
    this.instance.interceptors.response.use(
      (response: AxiosResponse) => {
        // Calculate latency
        const latency = Date.now() - (response.config.metadata?.startTime || Date.now())
        this.updateConnectionStatus(true, latency)
        return response
      },
      (error: AxiosError) => {
        this.updateConnectionStatus(false)
        return Promise.reject(this.handleApiError(error))
      }
    )
  }

  private static updateConnectionStatus(online: boolean, latency?: number) {
    const newStatus: ConnectionStatus = {
      online,
      lastCheck: new Date(),
      latency
    }

    if (this.connectionStatus.online !== online) {
      this.connectionStatus = newStatus
      this.connectionListeners.forEach(listener => listener(newStatus))
    } else {
      this.connectionStatus = newStatus
    }
  }

  private static handleApiError(error: AxiosError): ApiServiceError {
    if (error.response) {
      // Server responded with error status
      const data = error.response.data as any
      return new ApiServiceError(
        data?.message || error.message,
        data?.code || 'API_ERROR',
        error.response.status,
        data?.details
      )
    } else if (error.request) {
      // Network error
      return new ApiServiceError(
        'Network error - please check your connection',
        'NETWORK_ERROR',
        0,
        error.request
      )
    } else {
      // Request setup error
      return new ApiServiceError(
        error.message,
        'REQUEST_ERROR'
      )
    }
  }

  private static async retryRequest<T>(
    requestFn: () => Promise<T>,
    options: ApiRequestOptions = {}
  ): Promise<T> {
    const { retries = 3 } = options
    let lastError: Error

    for (let attempt = 0; attempt <= retries; attempt++) {
      try {
        if (attempt > 0) {
          // Exponential backoff
          const delay = Math.min(1000 * Math.pow(2, attempt - 1), 5000)
          await new Promise(resolve => setTimeout(resolve, delay))
        }

        return await requestFn()
      } catch (error) {
        lastError = error as Error

        // Don't retry on client errors (4xx)
        if (error instanceof ApiServiceError && error.status && error.status >= 400 && error.status < 500) {
          throw error
        }

        if (attempt === retries) {
          throw lastError
        }
      }
    }

    throw lastError!
  }

  // Connection monitoring
  static onConnectionStatusChange(listener: (status: ConnectionStatus) => void): () => void {
    this.connectionListeners.push(listener)
    return () => {
      const index = this.connectionListeners.indexOf(listener)
      if (index > -1) {
        this.connectionListeners.splice(index, 1)
      }
    }
  }

  static getConnectionStatus(): ConnectionStatus {
    return { ...this.connectionStatus }
  }

  // Health check for the API service itself
  static async healthCheck(): Promise<boolean> {
    try {
      await this.instance.get('/health/ping')
      return true
    } catch {
      return false
    }
  }

  /**
   * Get all crawler configurations
   */
  static async getCrawlerConfigurations(options?: ApiRequestOptions): Promise<CrawlerConfiguration[]> {
    return this.retryRequest(async () => {
      const response = await this.instance.get<CrawlerConfiguration[]>('/crawlers', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get health status for all crawlers
   */
  static async getAllCrawlerHealth(options?: ApiRequestOptions): Promise<Record<string, HealthStatus>> {
    return this.retryRequest(async () => {
      const response = await this.instance.get<Record<string, HealthStatus>>('/health', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get health status for a specific crawler
   */
  static async getCrawlerHealth(crawlerId: string, options?: ApiRequestOptions): Promise<HealthStatus> {
    return this.retryRequest(async () => {
      const response = await this.instance.get<HealthStatus>(`/health/${crawlerId}`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Force a health check for a specific crawler
   */
  static async forceHealthCheck(crawlerId: string, options?: ApiRequestOptions): Promise<HealthStatus> {
    return this.retryRequest(async () => {
      const response = await this.instance.post<HealthStatus>(`/health/${crawlerId}/check`, {}, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Trigger a crawl for a specific crawler
   */
  static async triggerCrawl(
    crawlerId: string,
    request?: Partial<CrawlRequest>,
    options?: ApiRequestOptions
  ): Promise<CrawlResponse> {
    return this.retryRequest(async () => {
      const crawlRequest: CrawlRequest = {
        crawlerId,
        ...request
      }

      const response = await this.instance.post<CrawlResponse>(
        `/crawlers/${crawlerId}/crawl`,
        crawlRequest,
        {
          timeout: options?.timeout,
          headers: options?.headers
        }
      )
      return response.data
    }, options)
  }

  /**
   * Get the current status of a crawler (crawl status, not health)
   */
  static async getCrawlerStatus(crawlerId: string, options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/crawlers/${crawlerId}/status`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get crawler metrics
   */
  static async getCrawlerMetrics(
    crawlerId: string,
    timeRange?: string,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const params = timeRange ? { timeRange } : {}
      const response = await this.instance.get(`/metrics/${crawlerId}`, {
        params,
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get all crawler metrics
   */
  static async getAllCrawlerMetrics(options?: ApiRequestOptions): Promise<Record<string, any>> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/metrics', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get historical crawler metrics
   */
  static async getCrawlerMetricsHistory(
    crawlerId: string,
    hours: number = 24,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/metrics/${crawlerId}/history`, {
        params: { hours },
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Trigger metrics collection for a crawler
   */
  static async triggerMetricsCollection(
    crawlerId: string,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.post(`/metrics/${crawlerId}/collect`, {}, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get recent jobs for a crawler (last 5 jobs)
   */
  static async getRecentJobs(
    crawlerId: string,
    options?: ApiRequestOptions
  ): Promise<any[]> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/jobs/crawler/${crawlerId}/recent`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get job history for a crawler with pagination
   */
  static async getJobHistory(
    crawlerId: string,
    page: number = 0,
    size: number = 10,
    options?: ApiRequestOptions
  ): Promise<any[]> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/jobs/crawler/${crawlerId}`, {
        params: { page, size },
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get specific job details
   */
  static async getJobDetails(jobId: string, options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/jobs/${jobId}`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get all running jobs
   */
  static async getRunningJobs(options?: ApiRequestOptions): Promise<any[]> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/jobs/running', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get running jobs for a specific crawler
   */
  static async getCrawlerRunningJobs(
    crawlerId: string,
    options?: ApiRequestOptions
  ): Promise<any[]> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/jobs/crawler/${crawlerId}/running`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Cancel a running job
   */
  static async cancelJob(jobId: string, options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.post(`/jobs/${jobId}/cancel`, {}, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Check if a crawler has running jobs
   */
  static async hasRunningJobs(crawlerId: string, options?: ApiRequestOptions): Promise<boolean> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/jobs/crawler/${crawlerId}/has-running`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data.hasRunningJobs
    }, options)
  }

  /**
   * Trigger metrics collection for all crawlers
   */
  static async triggerAllMetricsCollection(options?: ApiRequestOptions): Promise<void> {
    return this.retryRequest(async () => {
      const response = await this.instance.post('/metrics/collect', {}, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Clear metrics cache for a crawler
   */
  static async clearMetricsCache(crawlerId: string, options?: ApiRequestOptions): Promise<void> {
    return this.retryRequest(async () => {
      const response = await this.instance.delete(`/metrics/${crawlerId}/cache`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get metrics collection status
   */
  static async getMetricsStatus(options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/metrics/status', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  // Prediction Analysis API Methods

  /**
   * Get articles available for analysis with filtering
   */
  static async getArticlesForAnalysis(
    filters?: {
      authorId?: number | null
      titleSearch?: string
      fromDate?: string
      toDate?: string
      page?: number
      size?: number
    },
    options?: ApiRequestOptions
  ): Promise<{
    articles: any[]
    pagination: {
      page: number
      size: number
      totalElements: number
      totalPages: number
      hasNext: boolean
      hasPrevious: boolean
    }
  }> {
    return this.retryRequest(async () => {
      const params: Record<string, any> = {}

      if (filters?.authorId) {
        params.authorId = filters.authorId
      }
      if (filters?.titleSearch) {
        params.titleSearch = filters.titleSearch
      }
      if (filters?.fromDate) {
        params.fromDate = filters.fromDate
      }
      if (filters?.toDate) {
        params.toDate = filters.toDate
      }
      if (filters?.page !== undefined) {
        params.page = filters.page
      }
      if (filters?.size !== undefined) {
        params.size = filters.size
      }

      const response = await this.instance.get('/prediction-analysis/articles', {
        params,
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get all authors for filter dropdowns
   */
  static async getAllAuthors(options?: ApiRequestOptions): Promise<{
    author: {
      id: number
      name: string
      avatarUrl?: string
    }
    articleCount: number
  }[]> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/authors', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Start a new prediction analysis job
   */
  static async startAnalysis(
    articleIds: number[],
    analysisType: string = 'llm',
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.post('/prediction-analysis/jobs', {
        articleIds,
        analysisType
      }, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get analysis job status
   */
  static async getAnalysisJobStatus(
    jobId: string,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/prediction-analysis/jobs/${jobId}`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get analysis job results
   */
  static async getAnalysisResults(
    jobId: string,
    page: number = 0,
    size: number = 20,
    minRating?: number,
    options?: ApiRequestOptions
  ): Promise<{
    results: any[]
    job: any
    pagination: {
      page: number
      size: number
      totalElements: number
      totalPages: number
      hasNext: boolean
      hasPrevious: boolean
    }
  }> {
    return this.retryRequest(async () => {
      const params: Record<string, any> = { page, size }
      if (minRating !== undefined) {
        params.minRating = minRating
      }

      const response = await this.instance.get(`/prediction-analysis/jobs/${jobId}/results`, {
        params,
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get analysis history with pagination
   */
  static async getAnalysisHistory(
    page: number = 0,
    size: number = 20,
    options?: ApiRequestOptions
  ): Promise<{
    jobs: any[]
    pagination: {
      page: number
      size: number
      totalElements: number
      totalPages: number
      hasNext: boolean
      hasPrevious: boolean
    }
  }> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/jobs', {
        params: { page, size },
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Cancel a running analysis job
   */
  static async cancelAnalysisJob(
    jobId: string,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.delete(`/prediction-analysis/jobs/${jobId}`, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Retry a failed analysis job
   */
  static async retryAnalysisJob(
    jobId: string,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.post(`/analysis/jobs/${jobId}/retry`, {}, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Export analysis results
   */
  static async exportAnalysisResults(
    jobId: string,
    format: 'csv' | 'json' = 'csv',
    options?: ApiRequestOptions
  ): Promise<Blob> {
    return this.retryRequest(async () => {
      const response = await this.instance.get(`/prediction-analysis/jobs/${jobId}/export`, {
        params: { format },
        responseType: 'blob',
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get comprehensive analysis system status
   */
  static async getAnalysisStatus(options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/status', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get extractor configurations and status
   */
  static async getExtractorConfigurations(options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/extractors', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get job statistics and performance metrics
   */
  static async getJobStatistics(options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/statistics', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Test prediction extraction with sample text
   */
  static async testPredictionExtraction(
    text: string,
    title?: string,
    options?: ApiRequestOptions
  ): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.post('/prediction-analysis/test', {
        text,
        title
      }, {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get prediction analysis configuration
   */
  static async getConfiguration(options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/config', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }

  /**
   * Get prediction analysis health status (uses status endpoint)
   */
  static async getPredictionAnalysisHealth(options?: ApiRequestOptions): Promise<any> {
    return this.retryRequest(async () => {
      const response = await this.instance.get('/prediction-analysis/status', {
        timeout: options?.timeout,
        headers: options?.headers
      })
      return response.data
    }, options)
  }
}

// Metrics API - separate namespace for metrics operations
export const metricsApi = {
  getAllMetrics: (options?: ApiRequestOptions): Promise<Record<string, any>> =>
    ApiService.getAllCrawlerMetrics(options),

  getCrawlerMetrics: (crawlerId: string, options?: ApiRequestOptions): Promise<any> =>
    ApiService.getCrawlerMetrics(crawlerId, undefined, options),

  getCrawlerMetricsHistory: (crawlerId: string, hours: number = 24, options?: ApiRequestOptions): Promise<any> =>
    ApiService.getCrawlerMetricsHistory(crawlerId, hours, options),

  triggerMetricsCollection: (crawlerId: string, options?: ApiRequestOptions): Promise<void> =>
    ApiService.triggerMetricsCollection(crawlerId, options),

  triggerAllMetricsCollection: (options?: ApiRequestOptions): Promise<void> =>
    ApiService.triggerAllMetricsCollection(options),

  clearMetricsCache: (crawlerId: string, options?: ApiRequestOptions): Promise<void> =>
    ApiService.clearMetricsCache(crawlerId, options),

  getMetricsStatus: (options?: ApiRequestOptions): Promise<any> =>
    ApiService.getMetricsStatus(options)
}

// Extend the AxiosRequestConfig to include metadata
declare module 'axios' {
  interface AxiosRequestConfig {
    metadata?: {
      startTime: number
    }
  }
}