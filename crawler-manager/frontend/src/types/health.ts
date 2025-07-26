export interface HealthStatus {
  status: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN'
  message: string
  lastCheck: string
  responseTimeMs: number | null
  crawlerId: string
}

export interface CrawlerConfiguration {
  id: string
  name: string
  baseUrl: string
  port: number
  healthEndpoint: string
  crawlEndpoint: string
  statusEndpoint: string
  enabled: boolean
}

export interface CrawlerHealthData {
  configuration: CrawlerConfiguration
  health: HealthStatus
}

export interface CrawlRequest {
  crawlerId: string
  priority?: string
  maxArticles?: number
  dateRange?: string
}

export interface CrawlResponse {
  status: 'ACCEPTED' | 'CONFLICT' | 'ERROR' | 'SERVICE_UNAVAILABLE'
  message: string
  requestId: string
  crawlId?: string
  timestamp: string
  crawlerId: string
  statusEndpoint?: string
  estimatedDuration?: string
  errorCategory?: string
  suggestion?: string
}