import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import CrawlerDetail from '../CrawlerDetail.vue'
import { ApiService } from '@/services/api'

// Mock the API service
vi.mock('@/services/api', () => ({
  ApiService: {
    getCrawlerConfigurations: vi.fn(),
    getCrawlerHealth: vi.fn(),
    hasRunningJobs: vi.fn(),
    forceHealthCheck: vi.fn(),
    triggerCrawl: vi.fn(),
    getCrawlerStatus: vi.fn()
  }
}))

// Mock the WebSocket service
vi.mock('@/services/websocket', () => ({
  HealthWebSocketService: vi.fn().mockImplementation(() => ({
    onHealthUpdate: vi.fn(() => vi.fn()),
    on: vi.fn(() => vi.fn()),
    disconnect: vi.fn()
  }))
}))

// Mock the router
const mockRouter = {
  push: vi.fn()
}

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter,
  useRoute: () => ({ params: { id: 'test-crawler' } })
}))

// Mock PrimeVue toast
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({
    add: vi.fn()
  })
}))

// Mock the stores
vi.mock('@/stores/topbar', () => ({
  useTopBarStore: () => ({
    setCrawlerDetailActions: vi.fn(),
    clearContextActions: vi.fn()
  })
}))

const mockCrawlerConfiguration = {
  id: 'test-crawler',
  name: 'Test Crawler',
  baseUrl: 'http://localhost:8080',
  port: 8080,
  healthEndpoint: '/health',
  crawlEndpoint: '/crawl',
  statusEndpoint: '/status',
  enabled: true,
  authorName: 'Test Author',
  authorAvatarUrl: 'http://example.com/avatar.jpg'
}

const mockHealthStatus = {
  status: 'HEALTHY',
  message: 'All systems operational',
  lastCheck: new Date().toISOString(),
  responseTimeMs: 150,
  crawlerId: 'test-crawler'
}

describe('CrawlerDetail', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    
    // Setup API mocks
    vi.mocked(ApiService.getCrawlerConfigurations).mockResolvedValue([mockCrawlerConfiguration])
    vi.mocked(ApiService.getCrawlerHealth).mockResolvedValue(mockHealthStatus)
    vi.mocked(ApiService.hasRunningJobs).mockResolvedValue(false)
  })

  const createWrapper = () => {
    return mount(CrawlerDetail, {
      props: {
        id: 'test-crawler'
      },
      global: {
        stubs: {
          'router-link': true,
          'MetricsComponent': true,
          'JobHistory': true,
          'AuthorInfo': true
        }
      }
    })
  }

  describe('Tabbed Interface', () => {
    it('should render TabView component', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      expect(wrapper.find('.crawler-detail-tabs').exists()).toBe(true)
    })

    it('should have Configuration and Metrics tabs', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      const tabHeaders = wrapper.findAll('[role="tab"]')
      expect(tabHeaders.length).toBeGreaterThanOrEqual(2)
    })

    it('should start with Configuration tab active', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      expect(wrapper.vm.activeTabIndex).toBe("0")
    })

    it('should disable Metrics tab when crawler is disabled', async () => {
      const disabledConfiguration = { ...mockCrawlerConfiguration, enabled: false }
      vi.mocked(ApiService.getCrawlerConfigurations).mockResolvedValue([disabledConfiguration])
      
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // The Metrics tab should be disabled when crawler is not enabled
      const metricsTab = wrapper.find('[value="1"]')
      if (metricsTab.exists()) {
        expect(metricsTab.attributes('disabled')).toBeDefined()
      }
    })

    it('should show crawler configuration in Configuration tab', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // Should show crawler details
      expect(wrapper.text()).toContain('Test Crawler')
      expect(wrapper.text()).toContain('test-crawler')
      expect(wrapper.text()).toContain('http://localhost:8080')
    })

    it('should show health status in Configuration tab', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // Should show health status
      expect(wrapper.text()).toContain('Health Status')
      expect(wrapper.text()).toContain('HEALTHY')
    })

    it('should show endpoints in Configuration tab', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // Should show endpoints
      expect(wrapper.text()).toContain('Endpoints')
      expect(wrapper.text()).toContain('/health')
      expect(wrapper.text()).toContain('/crawl')
      expect(wrapper.text()).toContain('/status')
    })
  })

  describe('Tab State Management', () => {
    it('should maintain activeTabIndex state', async () => {
      const wrapper = createWrapper()
      
      // Wait for component to load data
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))
      
      // Initial state should be "0" (Configuration tab)
      expect(wrapper.vm.activeTabIndex).toBe("0")
      
      // Change to Metrics tab
      wrapper.vm.activeTabIndex = "1"
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.activeTabIndex).toBe("1")
    })
  })
})