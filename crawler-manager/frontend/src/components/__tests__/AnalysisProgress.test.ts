import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import AnalysisProgress from '../AnalysisProgress.vue'
import PrimeVue from 'primevue/config'

// Mock WebSocket service
const mockWebSocketService = {
  isConnected: vi.fn(() => true),
  onConnectionStatusChange: vi.fn(() => vi.fn()),
  on: vi.fn(() => vi.fn()),
  onAnalysisJobUpdate: vi.fn(() => vi.fn())
}

vi.mock('@/services/websocket', () => ({
  getWebSocketService: () => mockWebSocketService
}))

describe('AnalysisProgress', () => {
  const mockJob = {
    id: 'test-job-1',
    jobId: 'job-123',
    status: 'RUNNING' as const,
    startedAt: new Date().toISOString(),
    totalArticles: 100,
    processedArticles: 45,
    predictionsFound: 12,
    analysisType: 'mock' as const,
    currentActivity: 'Processing article 45 of 100'
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const createWrapper = (job = mockJob, props = {}) => {
    return mount(AnalysisProgress, {
      props: {
        job,
        ...props
      },
      global: {
        plugins: [PrimeVue]
      }
    })
  }

  describe('Component Rendering', () => {
    it('renders the component with job data', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.find('.analysis-progress-panel').exists()).toBe(true)
      expect(wrapper.text()).toContain('Analysis Progress')
      expect(wrapper.text()).toContain('45') // processed articles
      expect(wrapper.text()).toContain('100') // total articles
      expect(wrapper.text()).toContain('12') // predictions found
    })

    it('displays correct progress percentage', () => {
      const wrapper = createWrapper()
      
      // 45/100 = 45%
      expect(wrapper.text()).toContain('45%')
    })

    it('shows current activity when provided', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.text()).toContain('Processing article 45 of 100')
    })

    it('displays correct status tag', () => {
      const wrapper = createWrapper()
      
      const statusTag = wrapper.find('[data-pc-name="tag"]')
      expect(statusTag.exists()).toBe(true)
      expect(statusTag.text()).toContain('RUNNING')
    })
  })

  describe('Job Status Handling', () => {
    it('shows cancel button for running jobs', () => {
      const wrapper = createWrapper()
      
      const cancelButton = wrapper.find('button[aria-label*="Cancel"]')
      expect(cancelButton.exists()).toBe(true)
    })

    it('shows retry button for failed jobs', () => {
      const failedJob = {
        ...mockJob,
        status: 'FAILED' as const,
        errorMessage: 'Network error occurred'
      }
      
      const wrapper = createWrapper(failedJob)
      
      const retryButton = wrapper.find('button[aria-label*="Retry"]')
      expect(retryButton.exists()).toBe(true)
    })

    it('emits cancel-analysis when cancel button is clicked', async () => {
      const wrapper = createWrapper()
      
      const cancelButton = wrapper.find('button[aria-label*="Cancel"]')
      await cancelButton.trigger('click')
      
      expect(wrapper.emitted('cancel-analysis')).toBeTruthy()
    })

    it('emits retry-analysis when retry button is clicked', async () => {
      const failedJob = {
        ...mockJob,
        status: 'FAILED' as const,
        errorMessage: 'Network error occurred'
      }
      
      const wrapper = createWrapper(failedJob)
      
      const retryButton = wrapper.find('button[aria-label*="Retry"]')
      await retryButton.trigger('click')
      
      expect(wrapper.emitted('retry-analysis')).toBeTruthy()
    })
  })

  describe('Timeline Visualization', () => {
    it('shows timeline with correct events', () => {
      const wrapper = createWrapper()
      
      const timeline = wrapper.find('[data-pc-name="timeline"]')
      expect(timeline.exists()).toBe(true)
      
      // Should show timeline events
      expect(wrapper.text()).toContain('Analysis Started')
      expect(wrapper.text()).toContain('Processing Articles')
      expect(wrapper.text()).toContain('Extracting Predictions')
    })

    it('marks completed events correctly', () => {
      const completedJob = {
        ...mockJob,
        status: 'COMPLETED' as const,
        completedAt: new Date().toISOString()
      }
      
      const wrapper = createWrapper(completedJob)
      
      expect(wrapper.text()).toContain('Analysis Complete')
    })

    it('shows error message for failed jobs', () => {
      const failedJob = {
        ...mockJob,
        status: 'FAILED' as const,
        errorMessage: 'Network connection failed'
      }
      
      const wrapper = createWrapper(failedJob)
      
      expect(wrapper.text()).toContain('Network connection failed')
    })
  })

  describe('Real-time Updates', () => {
    it('sets up WebSocket listeners on mount', () => {
      createWrapper()
      
      expect(mockWebSocketService.onConnectionStatusChange).toHaveBeenCalled()
      expect(mockWebSocketService.on).toHaveBeenCalledWith('analysis.job.progress', expect.any(Function))
    })

    it('shows connection status indicator', () => {
      const wrapper = createWrapper()
      
      const connectionIndicator = wrapper.find('.connection-indicator')
      expect(connectionIndicator.exists()).toBe(true)
      expect(connectionIndicator.classes()).toContain('connected')
    })

    it('handles job updates via WebSocket', async () => {
      const wrapper = createWrapper()
      
      // Simulate WebSocket job update
      const updatedJob = {
        ...mockJob,
        processedArticles: 60,
        predictionsFound: 18
      }
      
      await wrapper.setProps({ job: updatedJob })
      await nextTick()
      
      expect(wrapper.text()).toContain('60') // updated processed articles
      expect(wrapper.text()).toContain('18') // updated predictions found
    })
  })

  describe('Success Rate Calculation', () => {
    it('calculates success rate correctly', () => {
      const wrapper = createWrapper()
      
      // 12 predictions found / 45 processed = 26.67% ≈ 27%
      expect(wrapper.text()).toContain('27%')
    })

    it('shows 0% success rate when no articles processed', () => {
      const newJob = {
        ...mockJob,
        processedArticles: 0,
        predictionsFound: 0
      }
      
      const wrapper = createWrapper(newJob)
      
      expect(wrapper.text()).toContain('0%')
    })
  })

  describe('Time Formatting', () => {
    it('formats elapsed time correctly', () => {
      const oneMinuteAgo = new Date(Date.now() - 60000).toISOString()
      const jobWithTime = {
        ...mockJob,
        startedAt: oneMinuteAgo
      }
      
      const wrapper = createWrapper(jobWithTime)
      
      expect(wrapper.text()).toMatch(/1m.*ago/)
    })

    it('shows estimated completion time for running jobs', () => {
      const wrapper = createWrapper()
      
      // Should show some estimated completion time
      expect(wrapper.html()).toContain('Estimated completion')
    })
  })

  describe('Accessibility', () => {
    it('has proper ARIA labels for buttons', () => {
      const wrapper = createWrapper()
      
      const cancelButton = wrapper.find('button[aria-label*="Cancel"]')
      expect(cancelButton.attributes('aria-label')).toBeDefined()
    })

    it('has semantic HTML structure', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.find('h1, h2, h3, h4, h5, h6').exists()).toBe(true)
      expect(wrapper.find('[role]').exists()).toBe(true)
    })
  })

  describe('Responsive Design', () => {
    it('applies responsive classes', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.find('.col-12').exists()).toBe(true)
      expect(wrapper.find('.md\\:col-3').exists()).toBe(true)
    })
  })

  describe('Error Handling', () => {
    it('handles missing job data gracefully', () => {
      const incompleteJob = {
        id: 'test-job-2',
        jobId: 'job-456',
        status: 'RUNNING' as const,
        startedAt: new Date().toISOString(),
        totalArticles: 0,
        processedArticles: 0,
        predictionsFound: 0,
        analysisType: 'mock' as const
      }
      
      expect(() => createWrapper(incompleteJob)).not.toThrow()
    })

    it('handles WebSocket connection errors', () => {
      mockWebSocketService.isConnected.mockReturnValue(false)
      
      const wrapper = createWrapper()
      
      const connectionIndicator = wrapper.find('.connection-indicator')
      expect(connectionIndicator.classes()).toContain('disconnected')
      expect(wrapper.text()).toContain('Connection lost')
    })
  })
})