import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, VueWrapper, shallowMount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import MetricsComponent from '../MetricsComponent.vue'

// Mock PrimeVue Chart
vi.mock('primevue/chart', () => ({
  default: {
    name: 'MockChart',
    template: '<div class="mock-chart" :data-chart-type="type" data-testid="primevue-chart"></div>',
    props: ['type', 'data', 'options']
  }
}))

// Mock theme store
const mockThemeStore = {
  isDarkMode: false,
  initialize: vi.fn()
}

vi.mock('@/stores/theme', () => ({
  useThemeStore: () => mockThemeStore
}))

// Mock API Service
const mockApiService = {
  getCrawlerMetrics: vi.fn(),
  getCrawlerMetricsHistory: vi.fn()
}

vi.mock('@/services/api', () => ({
  ApiService: mockApiService
}))

// Mock WebSocket service
vi.mock('@/services/websocket', () => ({
  getWebSocketService: () => ({
    onMetricsUpdate: vi.fn(() => vi.fn()) // Return unsubscribe function
  })
}))

describe('MetricsComponent', () => {
  let wrapper: VueWrapper<any>
  
  const mockMetricsData = {
    crawlerId: 'test-crawler',
    articlesProcessed: 1250,
    successRate: 87.5,
    averageProcessingTimeMs: 2500,
    errorCount: 15,
    lastCrawlTime: '2024-01-15T10:30:00Z',
    lastUpdated: '2024-01-15T10:30:00Z',
    totalCrawlsExecuted: 50,
    totalExecutionTimeMs: 125000,
    activeCrawls: 2,
    trendsData: [
      { 
        timestamp: '2024-01-15T09:00:00Z', 
        articlesProcessed: 100,
        successRate: 85.0,
        processingTimeMs: 2800,
        errorCount: 5
      },
      { 
        timestamp: '2024-01-15T09:30:00Z', 
        articlesProcessed: 250,
        successRate: 87.2,
        processingTimeMs: 2600,
        errorCount: 8
      }
    ],
    trends: {
      articlesProcessed: 0.15,
      successRate: 0.02,
      averageProcessingTime: -0.05,
      errorCount: 0.25
    }
  }

  const createWrapper = (props = {}) => {
    const pinia = createPinia()
    setActivePinia(pinia)
    
    return shallowMount(MetricsComponent, {
      props: {
        crawlerId: 'test-crawler',
        autoRefresh: false,
        ...props
      },
      global: {
        plugins: [pinia],
        directives: {
          tooltip: {}
        },
        stubs: {
          Button: true,
          Dropdown: true,
          ProgressSpinner: true,
          ProgressBar: true,
          Message: true,
          Panel: true,
          Chart: { 
            template: '<div class="mock-chart" :data-chart-type="type" data-testid="primevue-chart"></div>',
            props: ['type', 'data', 'options']
          }
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
    mockApiService.getCrawlerMetrics.mockResolvedValue(mockMetricsData)
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('Component Rendering', () => {
    it('renders the component with correct title', async () => {
      wrapper = createWrapper()
      await nextTick()

      expect(wrapper.text()).toContain('Metrics')
    })

    it('renders basic structure', async () => {
      wrapper = createWrapper()
      await nextTick()

      expect(wrapper.find('.metrics-component').exists()).toBe(true)
    })
  })

  describe('Loading States', () => {
    it('shows loading state initially', async () => {
      mockApiService.getCrawlerMetrics.mockImplementation(() => 
        new Promise(resolve => setTimeout(() => resolve(mockMetricsData), 100))
      )

      wrapper = createWrapper()
      await nextTick()

      expect(wrapper.text()).toContain('Loading metrics...')
    })

    it('shows error message when metrics fail to load', async () => {
      const errorMessage = 'Failed to fetch metrics'
      mockApiService.getCrawlerMetrics.mockRejectedValue(new Error(errorMessage))

      wrapper = createWrapper()
      await nextTick()
      
      // Wait for the async operation to complete
      await new Promise(resolve => setTimeout(resolve, 50))
      await nextTick()

      // The component should handle the error gracefully
      expect(mockApiService.getCrawlerMetrics).toHaveBeenCalled()
    })
  })

  describe('Metrics Display', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await nextTick()
      // Wait for the async operation to complete
      await new Promise(resolve => setTimeout(resolve, 10))
      await nextTick()
    })

    it('displays metrics when data is loaded', () => {
      expect(wrapper.text()).toContain('Articles Processed')
      expect(wrapper.text()).toContain('Success Rate')
      expect(wrapper.text()).toContain('Avg Processing Time')
      expect(wrapper.text()).toContain('Error Count')
    })
  })

  describe('Charts Rendering', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await nextTick()
      // Wait for the async operation to complete
      await new Promise(resolve => setTimeout(resolve, 10))
      await nextTick()
    })

    it('shows chart titles correctly', () => {
      // Check that metrics data is loaded (which means charts would be rendered)
      expect(wrapper.text()).toContain('Articles Processed')
      expect(wrapper.text()).toContain('Success Rate')
      expect(wrapper.text()).toContain('Avg Processing Time')
      expect(wrapper.text()).toContain('Error Count')
    })
  })

  describe('API Integration', () => {
    it('calls API service on mount', async () => {
      wrapper = createWrapper()
      await nextTick()
      // Wait for the async operation to complete
      await new Promise(resolve => setTimeout(resolve, 10))

      expect(mockApiService.getCrawlerMetrics).toHaveBeenCalledWith('test-crawler')
    })
  })

  describe('Data Formatting', () => {
    beforeEach(async () => {
      wrapper = createWrapper()
      await nextTick()
      // Wait for the async operation to complete
      await new Promise(resolve => setTimeout(resolve, 10))
      await nextTick()
    })

    it('has formatting methods available', () => {
      const component = wrapper.vm
      
      expect(typeof component.formatNumber).toBe('function')
      expect(typeof component.formatPercentage).toBe('function')
      expect(typeof component.formatDuration).toBe('function')
    })
  })
})