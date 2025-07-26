import type { HealthStatus, CrawlerConfiguration, CrawlRequest, CrawlResponse } from '@/types/health'

const API_BASE = '/api'

export class ApiService {
  
  /**
   * Get all crawler configurations
   */
  static async getCrawlerConfigurations(): Promise<CrawlerConfiguration[]> {
    const response = await fetch(`${API_BASE}/crawlers`)
    if (!response.ok) {
      throw new Error(`Failed to fetch crawler configurations: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * Get health status for all crawlers
   */
  static async getAllCrawlerHealth(): Promise<Record<string, HealthStatus>> {
    const response = await fetch(`${API_BASE}/health`)
    if (!response.ok) {
      throw new Error(`Failed to fetch crawler health: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * Get health status for a specific crawler
   */
  static async getCrawlerHealth(crawlerId: string): Promise<HealthStatus> {
    const response = await fetch(`${API_BASE}/health/${crawlerId}`)
    if (!response.ok) {
      throw new Error(`Failed to fetch health for crawler ${crawlerId}: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * Force a health check for a specific crawler
   */
  static async forceHealthCheck(crawlerId: string): Promise<HealthStatus> {
    const response = await fetch(`${API_BASE}/health/${crawlerId}/check`, {
      method: 'POST'
    })
    if (!response.ok) {
      throw new Error(`Failed to force health check for crawler ${crawlerId}: ${response.statusText}`)
    }
    return response.json()
  }

  /**
   * Trigger a crawl for a specific crawler
   */
  static async triggerCrawl(crawlerId: string, request?: Partial<CrawlRequest>): Promise<CrawlResponse> {
    const crawlRequest: CrawlRequest = {
      crawlerId,
      ...request
    }

    const response = await fetch(`${API_BASE}/crawlers/${crawlerId}/crawl`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(crawlRequest)
    })

    if (!response.ok) {
      // Try to parse error response
      let errorMessage = `Failed to trigger crawl for ${crawlerId}: ${response.statusText}`
      try {
        const errorData = await response.json()
        if (errorData.message) {
          errorMessage = errorData.message
        }
      } catch {
        // Ignore JSON parsing errors, use default message
      }
      throw new Error(errorMessage)
    }

    return response.json()
  }

  /**
   * Get the current status of a crawler (crawl status, not health)
   */
  static async getCrawlerStatus(crawlerId: string): Promise<any> {
    const response = await fetch(`${API_BASE}/crawlers/${crawlerId}/status`)
    if (!response.ok) {
      throw new Error(`Failed to get status for crawler ${crawlerId}: ${response.statusText}`)
    }
    return response.json()
  }
}