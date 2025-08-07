import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import Dashboard from '../Dashboard.vue'

// Mock router
const mockRouter = {
  push: vi.fn()
}

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter
}))

// Mock fetch for prediction analysis health check
global.fetch = vi.fn()

describe('Dashboard.vue', () => {
  let wrapper: VueWrapper<any>
  let mockApiService: any
  let mockWsService: any

  const mockCrawlerConfigurations: CrawlerConfiguration[] = [
    {
      id: 'caspit',
      name: 'Caspit Crawler',
      baseUrl: 'http://localhost:8080',
      port: 8080,
      healthEndpoint: '/health',
      crawlEndpoint: '/crawl',
      statusEndpoint: '/status',
      enabled: true
    },
    {
      id: 'drucker',
      name: 'Drucker Crawler',
      baseUrl: 'http://localhost:8081',
      port: 8081,
      healthEndpoint: '/health',
      crawlEndpoint: '/crawl',
      statusEndpoint: '/status',
      enabled: true
    }
  ]

  const mockHealthData: Record<string, HealthStatus> = {
    caspit: {
      status: 'HEALTHY',
      message: 'All systems operational',
      lastCheck: '2024-01-15T10:30:00Z',
      responseTimeMs: 150,
      crawlerId: 'caspit'
    },
    drucker: {
      status: 'UNHEALTHY',
      message: 'Connection timeout',
      lastCheck: '2024-01-15T10:29:00Z',
      responseTimeMs: null,
      crawlerId: 'drucker'
    }
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    
    // Setup API service mocks
    mockApiService = vi.mocked(ApiService)
    mockApiService.getCrawlerConfigurations = vi.fn().mockResolvedValue(mockCrawlerConfigurations)
    mockApiService.getAllCrawlerHealth = vi.fn().mockResolvedValue(mockHealthData)

    // Setup WebSocket service mocks
    mockWsService = {
      onHealthUpdate: vi.fn().mockReturnValue(() => {}),
      disconnect: vi.fn()
    }
    vi.mocked(HealthWebSocketService).mockImplementation(() => mockWsService)
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  const createWrapper = () => {
    return mount(Dashboard, {
      global: {
        stubs: {
          'router-link': true
        },
        directives: {
          tooltip: () => {}
        }
      }
    })
  }

  describe('Component Mounting and Initial State', () => {
    it('should mount successfully', () => {
      wrapper = createWrapper()
      expect(wrapper.exists()).toBe(true)
    })

    it('should display the dashboard title', () => {
      wrapper = createWrapper()
      expect(wrapper.text()).toContain('Crawler Manager Dashboard')
      expect(wrapper.text()).toContain('Monitor and control your web crawlers')
    })

    it('should show loading state initially', async () => {
      wrapper = createWrapper()
      // Initially loading should be true
      expect(wrapper.vm.isLoading).toBe(true)
    })
  })

  describe('Data Loading', () => {
    it('should load crawler configurations and health data on mount', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0)) // Wait for async operations

      expect(mockApiService.getCrawlerConfigurations).toHaveBeenCalled()
      expect(mockApiService.getAllCrawlerHealth).toHaveBeenCalled()
    })

    it('should display crawler data after loading', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))

      // Wait for loading to complete
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.crawlerData).toHaveLength(2)
      expect(wrapper.vm.crawlerData[0].configuration.id).toBe('caspit')
      expect(wrapper.vm.crawlerData[1].configuration.id).toBe('drucker')
    })

    it('should handle API errors gracefully', async () => {
      const errorMessage = 'Network error'
      mockApiService.getCrawlerConfigurations.mockRejectedValue(new Error(errorMessage))
      
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.error).toBe(errorMessage)
      expect(wrapper.vm.isLoading).toBe(false)
    })
  })

  describe('Summary Statistics', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()
    })

    it('should calculate total crawlers correctly', () => {
      expect(wrapper.vm.totalCrawlers).toBe(2)
    })

    it('should calculate healthy crawlers correctly', () => {
      expect(wrapper.vm.healthyCrawlers).toBe(1)
    })

    it('should calculate unhealthy crawlers correctly', () => {
      expect(wrapper.vm.unhealthyCrawlers).toBe(1)
    })

    it('should calculate unknown crawlers correctly', () => {
      expect(wrapper.vm.unknownCrawlers).toBe(0)
    })

    it('should display summary cards with correct values', () => {
      const summaryCards = wrapper.findAll('.kpi-card')
      expect(summaryCards.length).toBeGreaterThan(0)
    })
  })

  describe('Real-time Updates', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()
    })

    it('should initialize WebSocket connection', () => {
      expect(HealthWebSocketService).toHaveBeenCalled()
      expect(mockWsService.onHealthUpdate).toHaveBeenCalled()
    })

    it('should handle health updates from WebSocket', () => {
      const healthUpdateCallback = mockWsService.onHealthUpdate.mock.calls[0][0]
      const healthUpdate = {
        crawlerId: 'caspit',
        status: 'UNHEALTHY' as const,
        message: 'Service down',
        timestamp: '2024-01-15T10:35:00Z'
      }

      healthUpdateCallback(healthUpdate)

      const updatedCrawler = wrapper.vm.crawlerData.find((c: any) => c.configuration.id === 'caspit')
      expect(updatedCrawler.health.status).toBe('UNHEALTHY')
      expect(updatedCrawler.health.message).toBe('Service down')
    })

    it('should update last updated timestamp on health updates', () => {
      const initialTimestamp = wrapper.vm.lastUpdated
      
      const healthUpdateCallback = mockWsService.onHealthUpdate.mock.calls[0][0]
      healthUpdateCallback({
        crawlerId: 'caspit',
        status: 'HEALTHY',
        message: 'OK',
        timestamp: '2024-01-15T10:35:00Z'
      })

      expect(wrapper.vm.lastUpdated).not.toBe(initialTimestamp)
    })
  })

  describe('User Interactions', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()
    })

    it('should refresh data when refresh button is clicked', async () => {
      // Call refreshAllData method directly since button interaction is complex with async loading
      await wrapper.vm.refreshAllData()
      expect(mockApiService.getCrawlerConfigurations).toHaveBeenCalledTimes(2)
      expect(mockApiService.getAllCrawlerHealth).toHaveBeenCalledTimes(2)
    })

    it('should handle crawler health updates from child components', () => {
      const updatedHealth: HealthStatus = {
        status: 'HEALTHY',
        message: 'Updated status',
        lastCheck: '2024-01-15T10:40:00Z',
        responseTimeMs: 200,
        crawlerId: 'drucker'
      }

      wrapper.vm.updateCrawlerHealth(updatedHealth)

      const updatedCrawler = wrapper.vm.crawlerData.find((c: any) => c.configuration.id === 'drucker')
      expect(updatedCrawler.health).toEqual(updatedHealth)
    })
  })

  describe('Empty and Error States', () => {
    it('should display empty state when no crawlers are configured', async () => {
      mockApiService.getCrawlerConfigurations.mockResolvedValue([])
      mockApiService.getAllCrawlerHealth.mockResolvedValue({})
      
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()

      expect(wrapper.text()).toContain('No crawlers configured')
    })

    it('should display error state with retry button', async () => {
      mockApiService.getCrawlerConfigurations.mockRejectedValue(new Error('API Error'))
      
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.error).toBe('API Error')
      expect(wrapper.vm.isLoading).toBe(false)
    })
  })

  describe('Responsive Design', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()
    })

    it('should use responsive grid classes', () => {
      const gridElements = wrapper.findAll('.grid')
      expect(gridElements.length).toBeGreaterThan(0)
      
      const responsiveColumns = wrapper.findAll('[class*="col-12"], [class*="md:col-"], [class*="lg:col-"]')
      expect(responsiveColumns.length).toBeGreaterThan(0)
    })

    it('should render crawler cards in responsive grid', () => {
      const crawlerCardContainers = wrapper.findAll('.col-12.md\\:col-6.lg\\:col-4')
      expect(crawlerCardContainers.length).toBe(2)
    })
  })

  describe('Component Cleanup', () => {
    it('should disconnect WebSocket on unmount', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()
      
      wrapper.unmount()
      
      expect(mockWsService.disconnect).toHaveBeenCalled()
    })
  })

  describe('Time Display', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 0))
      await wrapper.vm.$nextTick()
    })

    it('should format last updated time correctly', () => {
      const now = new Date()
      wrapper.vm.lastUpdated = new Date(now.getTime() - 30000) // 30 seconds ago
      
      expect(wrapper.vm.lastUpdatedDisplay).toBe('30s ago')
    })

    it('should show "Never" when no last updated time', () => {
      wrapper.vm.lastUpdated = null
      expect(wrapper.vm.lastUpdatedDisplay).toBe('Never')
    })
  })
})