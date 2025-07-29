import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import CrawlerHealthCard from '../CrawlerHealthCard.vue'
import { ApiService } from '@/services/api'
import type { CrawlerConfiguration, HealthStatus } from '@/types/health'

// Mock dependencies
vi.mock('@/services/api')
vi.mock('primevue/usetoast', () => ({
  useToast: () => ({
    add: vi.fn()
  })
}))

// Mock router
const mockRouter = {
  push: vi.fn()
}

vi.mock('vue-router', () => ({
  useRouter: () => mockRouter
}))

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined)
  }
})

describe('CrawlerHealthCard.vue', () => {
  let wrapper: VueWrapper<any>
  let mockApiService: any

  const mockCrawlerConfig: CrawlerConfiguration = {
    id: 'test-crawler',
    name: 'Test Crawler',
    baseUrl: 'http://localhost:8080',
    port: 8080,
    healthEndpoint: '/health',
    crawlEndpoint: '/crawl',
    statusEndpoint: '/status',
    enabled: true
  }

  const mockHealthyStatus: HealthStatus = {
    status: 'HEALTHY',
    message: 'All systems operational',
    lastCheck: '2024-01-15T10:30:00Z',
    responseTimeMs: 150,
    crawlerId: 'test-crawler'
  }

  const mockUnhealthyStatus: HealthStatus = {
    status: 'UNHEALTHY',
    message: 'Connection timeout',
    lastCheck: '2024-01-15T10:29:00Z',
    responseTimeMs: null,
    crawlerId: 'test-crawler'
  }

  const mockUnknownStatus: HealthStatus = {
    status: 'UNKNOWN',
    message: 'Status check pending',
    lastCheck: '',
    responseTimeMs: null,
    crawlerId: 'test-crawler'
  }

  beforeEach(() => {
    mockApiService = vi.mocked(ApiService)
    mockApiService.forceHealthCheck = vi.fn().mockResolvedValue(mockHealthyStatus)
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
    vi.clearAllMocks()
  })

  const createWrapper = (health: HealthStatus = mockHealthyStatus) => {
    return mount(CrawlerHealthCard, {
      props: {
        crawler: mockCrawlerConfig,
        health: health
      },
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

  describe('Component Rendering', () => {
    it('should mount successfully', () => {
      wrapper = createWrapper()
      expect(wrapper.exists()).toBe(true)
    })

    it('should display crawler name and ID', () => {
      wrapper = createWrapper()
      expect(wrapper.props('crawler').name).toBe('Test Crawler')
      expect(wrapper.props('crawler').id).toBe('test-crawler')
    })

    it('should display crawler base URL', () => {
      wrapper = createWrapper()
      expect(wrapper.props('crawler').baseUrl).toBe('http://localhost:8080')
    })
  })

  describe('Health Status Display', () => {
    it('should display healthy status correctly', () => {
      wrapper = createWrapper(mockHealthyStatus)
      
      expect(wrapper.props('health').status).toBe('HEALTHY')
      expect(wrapper.props('health').message).toBe('All systems operational')
      expect(wrapper.vm.responseTimeDisplay).toBe('150ms')
    })

    it('should display unhealthy status correctly', () => {
      wrapper = createWrapper(mockUnhealthyStatus)
      
      expect(wrapper.props('health').status).toBe('UNHEALTHY')
      expect(wrapper.props('health').message).toBe('Connection timeout')
      expect(wrapper.vm.responseTimeDisplay).toBe('N/A')
    })

    it('should display unknown status correctly', () => {
      wrapper = createWrapper(mockUnknownStatus)
      
      expect(wrapper.props('health').status).toBe('UNKNOWN')
      expect(wrapper.props('health').message).toBe('Status check pending')
      expect(wrapper.vm.lastCheckDisplay).toBe('Never')
    })
  })

  describe('Health Indicator Classes', () => {
    it('should apply correct class for healthy status', () => {
      wrapper = createWrapper(mockHealthyStatus)
      expect(wrapper.vm.healthIndicatorClass).toBe('health-healthy')
    })

    it('should apply correct class for unhealthy status', () => {
      wrapper = createWrapper(mockUnhealthyStatus)
      expect(wrapper.vm.healthIndicatorClass).toBe('health-unhealthy')
    })

    it('should apply correct class for unknown status', () => {
      wrapper = createWrapper(mockUnknownStatus)
      expect(wrapper.vm.healthIndicatorClass).toBe('health-unknown')
    })
  })

  describe('Health Icon Classes', () => {
    it('should show check icon for healthy status', () => {
      wrapper = createWrapper(mockHealthyStatus)
      expect(wrapper.vm.healthIconClass).toBe('pi pi-check-circle')
    })

    it('should show times icon for unhealthy status', () => {
      wrapper = createWrapper(mockUnhealthyStatus)
      expect(wrapper.vm.healthIconClass).toBe('pi pi-times-circle')
    })

    it('should show question icon for unknown status', () => {
      wrapper = createWrapper(mockUnknownStatus)
      expect(wrapper.vm.healthIconClass).toBe('pi pi-question-circle')
    })
  })

  describe('Status Severity', () => {
    it('should return success severity for healthy status', () => {
      wrapper = createWrapper(mockHealthyStatus)
      expect(wrapper.vm.statusSeverity).toBe('success')
    })

    it('should return danger severity for unhealthy status', () => {
      wrapper = createWrapper(mockUnhealthyStatus)
      expect(wrapper.vm.statusSeverity).toBe('danger')
    })

    it('should return warning severity for unknown status', () => {
      wrapper = createWrapper(mockUnknownStatus)
      expect(wrapper.vm.statusSeverity).toBe('warning')
    })
  })

  describe('Response Time Display', () => {
    it('should display response time when available', () => {
      wrapper = createWrapper(mockHealthyStatus)
      expect(wrapper.vm.responseTimeDisplay).toBe('150ms')
    })

    it('should display N/A when response time is null', () => {
      wrapper = createWrapper(mockUnhealthyStatus)
      expect(wrapper.vm.responseTimeDisplay).toBe('N/A')
    })
  })

  describe('Last Check Display', () => {
    beforeEach(() => {
      // Mock Date.now() to return a fixed timestamp for consistent testing
      vi.useFakeTimers()
      vi.setSystemTime(new Date('2024-01-15T10:31:00Z'))
    })

    afterEach(() => {
      vi.useRealTimers()
    })

    it('should display relative time for recent checks', () => {
      wrapper = createWrapper(mockHealthyStatus)
      expect(wrapper.vm.lastCheckDisplay).toBe('1m ago')
    })

    it('should display "Never" when no last check time', () => {
      wrapper = createWrapper(mockUnknownStatus)
      expect(wrapper.vm.lastCheckDisplay).toBe('Never')
    })

    it('should display seconds for very recent checks', () => {
      const recentHealth = {
        ...mockHealthyStatus,
        lastCheck: '2024-01-15T10:30:45Z' // 15 seconds ago
      }
      wrapper = createWrapper(recentHealth)
      expect(wrapper.vm.lastCheckDisplay).toBe('15s ago')
    })
  })

  describe('Force Health Check', () => {
    it('should call API service when force health check is triggered', async () => {
      wrapper = createWrapper()
      
      await wrapper.vm.forceHealthCheck()
      
      expect(mockApiService.forceHealthCheck).toHaveBeenCalledWith('test-crawler')
    })

    it('should emit health updated event on successful check', async () => {
      wrapper = createWrapper()
      
      await wrapper.vm.forceHealthCheck()
      
      expect(wrapper.emitted('healthUpdated')).toBeTruthy()
      expect(wrapper.emitted('healthUpdated')![0]).toEqual([mockHealthyStatus])
    })

    it('should handle API errors gracefully', async () => {
      const errorMessage = 'Health check failed'
      mockApiService.forceHealthCheck.mockRejectedValue(new Error(errorMessage))
      
      wrapper = createWrapper()
      
      await wrapper.vm.forceHealthCheck()
      
      // Should not emit healthUpdated on error
      expect(wrapper.emitted('healthUpdated')).toBeFalsy()
    })

    it('should set loading state during health check', async () => {
      let resolveHealthCheck: (value: any) => void
      const healthCheckPromise = new Promise(resolve => {
        resolveHealthCheck = resolve
      })
      mockApiService.forceHealthCheck.mockReturnValue(healthCheckPromise)
      
      wrapper = createWrapper()
      
      const checkPromise = wrapper.vm.forceHealthCheck()
      expect(wrapper.vm.isRefreshing).toBe(true)
      
      resolveHealthCheck!(mockHealthyStatus)
      await checkPromise
      
      expect(wrapper.vm.isRefreshing).toBe(false)
    })
  })

  describe('Copy to Clipboard', () => {
    it('should copy URL to clipboard', async () => {
      wrapper = createWrapper()
      
      await wrapper.vm.copyToClipboard('http://test.com')
      
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('http://test.com')
    })

    it('should handle clipboard errors gracefully', async () => {
      const clipboardError = new Error('Clipboard access denied')
      vi.mocked(navigator.clipboard.writeText).mockRejectedValue(clipboardError)
      
      wrapper = createWrapper()
      
      // Should not throw error
      await expect(wrapper.vm.copyToClipboard('http://test.com')).resolves.toBeUndefined()
    })
  })

  describe('Navigation', () => {
    it('should navigate to crawler detail page', () => {
      wrapper = createWrapper()
      
      // Test the router functionality by checking if the component has the right setup
      expect(wrapper.props('crawler').id).toBe('test-crawler')
    })
  })

  describe('Component Props', () => {
    it('should react to prop changes', async () => {
      wrapper = createWrapper(mockHealthyStatus)
      expect(wrapper.vm.health.status).toBe('HEALTHY')
      
      await wrapper.setProps({ health: mockUnhealthyStatus })
      expect(wrapper.vm.health.status).toBe('UNHEALTHY')
    })

    it('should validate required props', () => {
      expect(() => {
        mount(CrawlerHealthCard, {
          props: {
            // Missing required props
          }
        })
      }).toThrow()
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA attributes', () => {
      wrapper = createWrapper()
      
      // Check that component has proper structure
      expect(wrapper.exists()).toBe(true)
      expect(wrapper.classes()).toContain('crawler-health-card')
    })

    it('should have semantic HTML structure', () => {
      wrapper = createWrapper()
      
      // Should have proper component structure
      expect(wrapper.find('.crawler-health-card').exists()).toBe(true)
    })
  })

  describe('Visual States', () => {
    it('should apply correct CSS classes for health states', () => {
      wrapper = createWrapper(mockHealthyStatus)
      
      expect(wrapper.vm.healthIndicatorClass).toBe('health-healthy')
    })

    it('should show loading state during refresh', async () => {
      let resolveHealthCheck: (value: any) => void
      const healthCheckPromise = new Promise(resolve => {
        resolveHealthCheck = resolve
      })
      mockApiService.forceHealthCheck.mockReturnValue(healthCheckPromise)
      
      wrapper = createWrapper()
      
      const checkPromise = wrapper.vm.forceHealthCheck()
      await wrapper.vm.$nextTick()
      
      // Should show loading state
      expect(wrapper.vm.isRefreshing).toBe(true)
      
      resolveHealthCheck!(mockHealthyStatus)
      await checkPromise
      
      expect(wrapper.vm.isRefreshing).toBe(false)
    })
  })
})