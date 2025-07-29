import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Badge from 'primevue/badge'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import Divider from 'primevue/divider'
import Dialog from 'primevue/dialog'
import Message from 'primevue/message'
import JobDetailModal from '../JobDetailModal.vue'
import type { JobStatus } from '@/types/job'

const mockCompletedJob: JobStatus = {
  jobId: 'job-123',
  crawlerId: 'crawler-1',
  status: 'COMPLETED',
  startTime: '2024-01-15T10:00:00Z',
  endTime: '2024-01-15T10:05:00Z',
  articlesProcessed: 10,
  articlesSkipped: 2,
  articlesFailed: 1,
  elapsedTimeMs: 300000,
  successRate: 76.9,
  totalArticlesAttempted: 13,
  durationMs: 300000,
  requestId: 'req-456',
  currentActivity: 'Completed successfully',
  lastUpdated: '2024-01-15T10:05:00Z'
}

const mockRunningJob: JobStatus = {
  jobId: 'job-456',
  crawlerId: 'crawler-2',
  status: 'RUNNING',
  startTime: '2024-01-15T11:00:00Z',
  articlesProcessed: 5,
  articlesSkipped: 0,
  articlesFailed: 0,
  elapsedTimeMs: 120000,
  successRate: 100,
  totalArticlesAttempted: 5,
  requestId: 'req-789',
  currentActivity: 'Processing articles',
  lastUpdated: '2024-01-15T11:02:00Z'
}

const mockFailedJob: JobStatus = {
  jobId: 'job-789',
  crawlerId: 'crawler-3',
  status: 'FAILED',
  startTime: '2024-01-15T09:00:00Z',
  endTime: '2024-01-15T09:02:00Z',
  articlesProcessed: 2,
  articlesSkipped: 0,
  articlesFailed: 3,
  elapsedTimeMs: 120000,
  successRate: 40,
  totalArticlesAttempted: 5,
  durationMs: 120000,
  errorMessage: 'Network timeout occurred while processing articles',
  currentActivity: 'Failed',
  lastUpdated: '2024-01-15T09:02:00Z'
}

describe('JobDetailModal', () => {
  let wrapper: any
  let pinia: any

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  const createWrapper = (job: JobStatus = mockCompletedJob) => {
    return mount(JobDetailModal, {
      props: {
        job
      },
      global: {
        plugins: [pinia, PrimeVue],
        components: {
          Badge,
          Button,
          ProgressBar,
          Divider,
          Dialog,
          Message
        }
      }
    })
  }

  describe('Component Rendering', () => {
    it('renders the component with job details', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.job-detail-modal').exists()).toBe(true)
      expect(wrapper.find('.job-title h4').text()).toBe('job-123')
      expect(wrapper.findComponent(Badge).exists()).toBe(true)
    })

    it('displays job metadata correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const metadataItems = wrapper.findAll('.metadata-item')
      expect(metadataItems.length).toBeGreaterThan(0)
      
      // Check that crawler ID is displayed
      const crawlerIdItem = metadataItems.find(item => 
        item.find('label').text() === 'Crawler ID'
      )
      expect(crawlerIdItem?.find('span').text()).toBe('crawler-1')
    })

    it('shows progress statistics', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const progressItems = wrapper.findAll('.progress-item')
      expect(progressItems.length).toBe(4) // processed, skipped, failed, success rate
      
      const progressBars = wrapper.findAllComponents(ProgressBar)
      expect(progressBars.length).toBe(4)
    })

    it('displays current activity', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const activityDisplay = wrapper.find('.activity-display')
      expect(activityDisplay.exists()).toBe(true)
      expect(activityDisplay.text()).toContain('Completed successfully')
    })
  })

  describe('Status-Specific Rendering', () => {
    it('shows correct badge for completed job', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      const badge = wrapper.findComponent(Badge)
      expect(badge.props('value')).toBe('COMPLETED')
      expect(wrapper.vm.getStatusSeverity('COMPLETED')).toBe('success')
    })

    it('shows correct badge for running job', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      const badge = wrapper.findComponent(Badge)
      expect(badge.props('value')).toBe('RUNNING')
      expect(wrapper.vm.getStatusSeverity('RUNNING')).toBe('info')
    })

    it('shows correct badge for failed job', async () => {
      wrapper = createWrapper(mockFailedJob)
      await wrapper.vm.$nextTick()

      const badge = wrapper.findComponent(Badge)
      expect(badge.props('value')).toBe('FAILED')
      expect(wrapper.vm.getStatusSeverity('FAILED')).toBe('danger')
    })

    it('shows action buttons for running job', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      const actionButtons = wrapper.find('.job-actions')
      expect(actionButtons.exists()).toBe(true)
      
      const buttons = actionButtons.findAllComponents(Button)
      expect(buttons.length).toBe(2) // refresh and cancel
    })

    it('does not show action buttons for completed job', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      const actionButtons = wrapper.find('.job-actions')
      expect(actionButtons.exists()).toBe(false)
    })

    it('shows error message for failed job', async () => {
      wrapper = createWrapper(mockFailedJob)
      await wrapper.vm.$nextTick()

      const errorSection = wrapper.find('.job-error')
      expect(errorSection.exists()).toBe(true)
      
      const errorMessage = wrapper.findComponent(Message)
      expect(errorMessage.exists()).toBe(true)
      expect(errorMessage.props('severity')).toBe('error')
    })

    it('does not show error section for successful job', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      const errorSection = wrapper.find('.job-error')
      expect(errorSection.exists()).toBe(false)
    })
  })

  describe('Progress Calculations', () => {
    it('calculates progress percentages correctly', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      // Total articles attempted: 13 (10 + 2 + 1)
      expect(wrapper.vm.getProgressPercentage('processed')).toBeCloseTo(76.9, 1) // 10/13
      expect(wrapper.vm.getProgressPercentage('skipped')).toBeCloseTo(15.4, 1)   // 2/13
      expect(wrapper.vm.getProgressPercentage('failed')).toBeCloseTo(7.7, 1)     // 1/13
    })

    it('handles zero total articles', async () => {
      const emptyJob = { ...mockCompletedJob, totalArticlesAttempted: 0 }
      wrapper = createWrapper(emptyJob)
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.getProgressPercentage('processed')).toBe(0)
      expect(wrapper.vm.getProgressPercentage('skipped')).toBe(0)
      expect(wrapper.vm.getProgressPercentage('failed')).toBe(0)
    })
  })

  describe('Time Formatting', () => {
    it('formats date time correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const formatted = wrapper.vm.formatDateTime('2024-01-15T10:00:00Z')
      expect(typeof formatted).toBe('string')
      expect(formatted.length).toBeGreaterThan(0)
    })

    it('formats duration correctly for completed job', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      const formatted = wrapper.vm.formatDuration(mockCompletedJob)
      expect(formatted).toBe('5m 0s') // 300000ms = 5 minutes
    })

    it('formats elapsed time for running job', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      const formatted = wrapper.vm.formatElapsedTime(120000) // 2 minutes
      expect(formatted).toBe('2m 0s')
    })

    it('formats relative time correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      // Mock current time to be 1 minute after the job's last update
      const now = new Date('2024-01-15T10:06:00Z')
      vi.setSystemTime(now)

      const formatted = wrapper.vm.formatRelativeTime('2024-01-15T10:05:00Z')
      expect(formatted).toBe('1 minutes ago')

      vi.useRealTimers()
    })
  })

  describe('Average Processing Time', () => {
    it('calculates average processing time correctly', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      // 300000ms / 10 articles = 30000ms per article = 30s
      const avgTime = wrapper.vm.averageProcessingTime
      expect(avgTime).toBe('30s')
    })

    it('returns null when no articles processed', async () => {
      const jobWithNoArticles = { ...mockCompletedJob, articlesProcessed: 0 }
      wrapper = createWrapper(jobWithNoArticles)
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.averageProcessingTime).toBeNull()
    })
  })

  describe('Event Handling', () => {
    it('emits refresh event when refresh button is clicked', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      const refreshButton = wrapper.find('[data-testid="refresh-button"]')
      if (refreshButton.exists()) {
        await refreshButton.trigger('click')
        expect(wrapper.emitted('refresh')).toBeTruthy()
      }
    })

    it('shows cancel confirmation dialog when cancel button is clicked', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.confirmCancel).toBe(false)
      
      // Simulate clicking cancel button
      wrapper.vm.confirmCancel = true
      await wrapper.vm.$nextTick()

      const cancelDialog = wrapper.findComponent(Dialog)
      expect(cancelDialog.exists()).toBe(true)
      expect(wrapper.vm.confirmCancel).toBe(true)
    })

    it('emits cancel event when job cancellation is confirmed', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      await wrapper.vm.handleCancel()

      expect(wrapper.emitted('cancel')).toBeTruthy()
      expect(wrapper.emitted('cancel')[0]).toEqual(['job-456'])
      expect(wrapper.vm.confirmCancel).toBe(false)
    })
  })

  describe('Status Icons', () => {
    it('shows spinner icon for running job', async () => {
      wrapper = createWrapper(mockRunningJob)
      await wrapper.vm.$nextTick()

      const spinnerIcon = wrapper.find('.pi-spinner')
      expect(spinnerIcon.exists()).toBe(true)
    })

    it('shows check icon for completed job', async () => {
      wrapper = createWrapper(mockCompletedJob)
      await wrapper.vm.$nextTick()

      const checkIcon = wrapper.find('.pi-check-circle')
      expect(checkIcon.exists()).toBe(true)
    })

    it('shows error icon for failed job', async () => {
      wrapper = createWrapper(mockFailedJob)
      await wrapper.vm.$nextTick()

      const errorIcon = wrapper.find('.pi-times-circle')
      expect(errorIcon.exists()).toBe(true)
    })
  })

  describe('Responsive Design', () => {
    it('applies responsive classes correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const metadataGrid = wrapper.find('.metadata-grid')
      expect(metadataGrid.exists()).toBe(true)
      
      const progressGrid = wrapper.find('.progress-grid')
      expect(progressGrid.exists()).toBe(true)
    })
  })

  describe('Edge Cases', () => {
    it('handles job without request ID', async () => {
      const jobWithoutRequestId = { ...mockCompletedJob, requestId: undefined }
      wrapper = createWrapper(jobWithoutRequestId)
      await wrapper.vm.$nextTick()

      const requestIdItem = wrapper.findAll('.metadata-item').find(item => 
        item.find('label').text() === 'Request ID'
      )
      expect(requestIdItem?.find('span').text()).toBe('N/A')
    })

    it('handles job without end time', async () => {
      const jobWithoutEndTime = { ...mockRunningJob, endTime: undefined }
      wrapper = createWrapper(jobWithoutEndTime)
      await wrapper.vm.$nextTick()

      const endTimeItem = wrapper.findAll('.metadata-item').find(item => 
        item.find('label').text() === 'Ended'
      )
      expect(endTimeItem).toBeFalsy()
    })

    it('handles job without current activity', async () => {
      const jobWithoutActivity = { ...mockCompletedJob, currentActivity: undefined }
      wrapper = createWrapper(jobWithoutActivity)
      await wrapper.vm.$nextTick()

      const activityText = wrapper.find('.activity-text span')
      expect(activityText.text()).toBe('No activity information')
    })
  })
})