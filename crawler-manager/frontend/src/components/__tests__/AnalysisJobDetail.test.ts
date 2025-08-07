import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import AnalysisJobDetail from '../AnalysisJobDetail.vue'

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined)
  }
})

// Mock toast
const mockToast = {
  add: vi.fn()
}

const mockCompletedJob = {
  id: 1,
  jobId: 'job-completed-123',
  status: 'COMPLETED',
  startedAt: '2024-01-01T10:00:00Z',
  completedAt: '2024-01-01T10:30:00Z',
  totalArticles: 100,
  processedArticles: 100,
  predictionsFound: 45,
  analysisType: 'mock'
}

const mockRunningJob = {
  id: 2,
  jobId: 'job-running-456',
  status: 'RUNNING',
  startedAt: '2024-01-01T11:00:00Z',
  completedAt: null,
  totalArticles: 200,
  processedArticles: 75,
  predictionsFound: 20,
  analysisType: 'llm'
}

const mockFailedJob = {
  id: 3,
  jobId: 'job-failed-789',
  status: 'FAILED',
  startedAt: '2024-01-01T09:00:00Z',
  completedAt: '2024-01-01T09:15:00Z',
  totalArticles: 50,
  processedArticles: 25,
  predictionsFound: 0,
  analysisType: 'mock',
  errorMessage: 'Network timeout during processing'
}

describe('AnalysisJobDetail', () => {
  let wrapper: VueWrapper<any>

  const createWrapper = (job = mockCompletedJob) => {
    return mount(AnalysisJobDetail, {
      props: {
        job
      },
      global: {
        plugins: [PrimeVue, ToastService],
        mocks: {
          $toast: mockToast
        },
        stubs: {
          Button: {
            template: '<button class="mock-button" @click="$emit(\'click\')" :disabled="disabled"><slot /></button>',
            props: ['icon', 'severity', 'size', 'disabled']
          },
          Badge: {
            template: '<span class="mock-badge">{{ value }}</span>',
            props: ['value', 'severity']
          },
          ProgressBar: {
            template: '<div class="mock-progressbar" :data-value="value"></div>',
            props: ['value', 'showValue']
          },
          Divider: {
            template: '<hr class="mock-divider" />'
          },
          Message: {
            template: '<div class="mock-message"><slot /></div>',
            props: ['severity', 'closable']
          },
          Timeline: {
            template: '<div class="mock-timeline"><div v-for="item in value" :key="item.title" class="timeline-item">{{ item.title }}</div></div>',
            props: ['value']
          }
        }
      }
    })
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  describe('Component Rendering', () => {
    it('renders correctly with completed job', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      expect(wrapper.find('.analysis-job-detail').exists()).toBe(true)
      expect(wrapper.find('.job-overview').exists()).toBe(true)
      expect(wrapper.find('.job-progress').exists()).toBe(true)
      expect(wrapper.find('.job-status').exists()).toBe(true)
    })

    it('displays job header with correct information', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const jobTitle = wrapper.find('.job-title h4')
      expect(jobTitle.text()).toBe(mockCompletedJob.jobId)
      
      const badge = wrapper.find('.mock-badge')
      expect(badge.text()).toBe('COMPLETED')
    })

    it('shows appropriate action buttons for different job states', () => {
      // Completed job - should show export button
      wrapper = createWrapper(mockCompletedJob)
      let buttons = wrapper.findAll('.mock-button')
      expect(buttons.length).toBeGreaterThan(0)
      
      // Running job - should show cancel button
      wrapper.unmount()
      wrapper = createWrapper(mockRunningJob)
      buttons = wrapper.findAll('.mock-button')
      expect(buttons.length).toBeGreaterThan(0)
      
      // Failed job - should show retry button
      wrapper.unmount()
      wrapper = createWrapper(mockFailedJob)
      buttons = wrapper.findAll('.mock-button')
      expect(buttons.length).toBeGreaterThan(0)
    })
  })

  describe('Job Metadata Display', () => {
    it('displays all metadata fields correctly', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const metadataGrid = wrapper.find('.metadata-grid')
      expect(metadataGrid.exists()).toBe(true)
      
      // Check that job ID is displayed
      expect(wrapper.html()).toContain(mockCompletedJob.jobId)
      
      // Check that analysis type is displayed
      expect(wrapper.html()).toContain(mockCompletedJob.analysisType)
    })

    it('shows duration for completed jobs', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      // Should show duration since job is completed
      expect(wrapper.html()).toContain('30m') // 30 minute duration
    })

    it('shows elapsed time for running jobs', () => {
      wrapper = createWrapper(mockRunningJob)
      
      // Should show elapsed time for running job
      const elapsedTime = wrapper.vm.formatElapsedTime(mockRunningJob)
      expect(elapsedTime).toBeTruthy()
    })
  })

  describe('Progress Display', () => {
    it('calculates progress percentage correctly', () => {
      wrapper = createWrapper(mockRunningJob)
      
      const percentage = wrapper.vm.getProgressPercentage(mockRunningJob)
      expect(percentage).toBe(37.5) // 75/200 * 100
    })

    it('displays progress ring with correct values', () => {
      wrapper = createWrapper(mockRunningJob)
      
      const progressRing = wrapper.find('.progress-ring-progress')
      expect(progressRing.exists()).toBe(true)
    })

    it('shows progress statistics', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const progressStats = wrapper.find('.progress-stats')
      expect(progressStats.exists()).toBe(true)
      
      // Should show total articles, processed, and predictions found
      expect(wrapper.html()).toContain('100') // total articles
      expect(wrapper.html()).toContain('45') // predictions found
    })

    it('calculates success rate correctly', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const successRate = wrapper.vm.getSuccessRate(mockCompletedJob)
      expect(successRate).toBe(100) // 100/100 * 100
    })

    it('calculates prediction density correctly', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const density = wrapper.vm.getPredictionDensity(mockCompletedJob)
      expect(density).toBe('0.45') // 45/100
    })
  })

  describe('Status Display', () => {
    it('shows correct status message for different states', () => {
      // Completed job
      wrapper = createWrapper(mockCompletedJob)
      let statusMessage = wrapper.vm.getStatusMessage(mockCompletedJob)
      expect(statusMessage).toContain('completed successfully')
      expect(statusMessage).toContain('45 predictions')
      
      // Running job
      wrapper.unmount()
      wrapper = createWrapper(mockRunningJob)
      statusMessage = wrapper.vm.getStatusMessage(mockRunningJob)
      expect(statusMessage).toContain('Processing articles')
      expect(statusMessage).toContain('75 of 200')
      
      // Failed job
      wrapper.unmount()
      wrapper = createWrapper(mockFailedJob)
      statusMessage = wrapper.vm.getStatusMessage(mockFailedJob)
      expect(statusMessage).toContain('failed during processing')
    })

    it('displays status icon correctly', () => {
      wrapper = createWrapper(mockRunningJob)
      
      const statusIcon = wrapper.find('.status-icon i')
      expect(statusIcon.exists()).toBe(true)
    })

    it('shows status time information', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const statusTime = wrapper.vm.getStatusTime(mockCompletedJob)
      expect(statusTime).toBeTruthy()
      expect(statusTime).toContain('Finished')
    })
  })

  describe('Error Display', () => {
    it('shows error section for failed jobs', () => {
      wrapper = createWrapper(mockFailedJob)
      
      const errorSection = wrapper.find('.job-error')
      expect(errorSection.exists()).toBe(true)
      
      const errorMessage = wrapper.find('.mock-message')
      expect(errorMessage.exists()).toBe(true)
    })

    it('does not show error section for successful jobs', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const errorSection = wrapper.find('.job-error')
      expect(errorSection.exists()).toBe(false)
    })

    it('can copy error message', async () => {
      wrapper = createWrapper(mockFailedJob)
      
      await wrapper.vm.copyError()
      
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(mockFailedJob.errorMessage)
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Copied',
        detail: 'Error message copied to clipboard',
        life: 2000
      })
    })
  })

  describe('Performance Metrics', () => {
    it('calculates average processing time correctly', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const avgTime = wrapper.vm.getAverageProcessingTime(mockCompletedJob)
      expect(avgTime).toBeTruthy()
      expect(avgTime).not.toBe('N/A')
    })

    it('calculates processing rate correctly', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const rate = wrapper.vm.getProcessingRate(mockCompletedJob)
      expect(rate).toBeTruthy()
      expect(parseFloat(rate)).toBeGreaterThan(0)
    })

    it('calculates prediction rate for jobs with predictions', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const rate = wrapper.vm.getPredictionRate(mockCompletedJob)
      expect(rate).toBeTruthy()
      expect(parseFloat(rate)).toBeGreaterThan(0)
    })

    it('estimates completion time for running jobs', () => {
      wrapper = createWrapper(mockRunningJob)
      
      const estimation = wrapper.vm.getEstimatedCompletion(mockRunningJob)
      expect(estimation).toBeTruthy()
    })

    it('shows performance metrics section for completed/running jobs', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const metricsSection = wrapper.find('.performance-metrics')
      expect(metricsSection.exists()).toBe(true)
      
      const metricCards = wrapper.findAll('.metric-card')
      expect(metricCards.length).toBeGreaterThan(0)
    })
  })

  describe('Timeline Display', () => {
    it('generates timeline events correctly', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const events = wrapper.vm.getTimelineEvents(mockCompletedJob)
      expect(events.length).toBeGreaterThan(0)
      
      // Should have job created event
      expect(events.some(e => e.title === 'Job Created')).toBe(true)
      
      // Should have job completed event for completed job
      expect(events.some(e => e.title === 'Job Completed')).toBe(true)
    })

    it('includes progress milestones for jobs with progress', () => {
      const jobWithProgress = {
        ...mockRunningJob,
        processedArticles: 150 // 75% of 200
      }
      wrapper = createWrapper(jobWithProgress)
      
      const events = wrapper.vm.getTimelineEvents(jobWithProgress)
      
      // Should include milestone events
      expect(events.some(e => e.title.includes('25% Complete'))).toBe(true)
      expect(events.some(e => e.title.includes('50% Complete'))).toBe(true)
      expect(events.some(e => e.title.includes('75% Complete'))).toBe(true)
    })

    it('shows timeline component', () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const timeline = wrapper.find('.mock-timeline')
      expect(timeline.exists()).toBe(true)
    })
  })

  describe('Action Availability', () => {
    it('determines cancel availability correctly', () => {
      wrapper = createWrapper()
      
      expect(wrapper.vm.canCancel(mockRunningJob)).toBe(true)
      expect(wrapper.vm.canCancel(mockCompletedJob)).toBe(false)
      expect(wrapper.vm.canCancel(mockFailedJob)).toBe(false)
    })

    it('determines retry availability correctly', () => {
      wrapper = createWrapper()
      
      expect(wrapper.vm.canRetry(mockFailedJob)).toBe(true)
      expect(wrapper.vm.canRetry(mockCompletedJob)).toBe(false)
      expect(wrapper.vm.canRetry(mockRunningJob)).toBe(false)
    })

    it('determines export availability correctly', () => {
      wrapper = createWrapper()
      
      expect(wrapper.vm.canExport(mockCompletedJob)).toBe(true)
      expect(wrapper.vm.canExport(mockRunningJob)).toBe(false)
      expect(wrapper.vm.canExport(mockFailedJob)).toBe(false)
    })
  })

  describe('Event Emissions', () => {
    it('emits refresh event when refresh button clicked', async () => {
      wrapper = createWrapper(mockCompletedJob)
      
      const refreshButton = wrapper.findAll('.mock-button')[0]
      await refreshButton.trigger('click')
      
      expect(wrapper.emitted('refresh')).toBeTruthy()
    })

    it('emits cancel event when cancel button clicked', async () => {
      wrapper = createWrapper(mockRunningJob)
      
      // Find cancel button (should be present for running job)
      const buttons = wrapper.findAll('.mock-button')
      const cancelButton = buttons.find(btn => !btn.attributes('disabled'))
      
      if (cancelButton) {
        await cancelButton.trigger('click')
        expect(wrapper.emitted('cancel')).toBeTruthy()
        expect(wrapper.emitted('cancel')[0]).toEqual([mockRunningJob.jobId])
      }
    })

    it('emits retry event when retry button clicked', async () => {
      wrapper = createWrapper(mockFailedJob)
      
      // Find retry button (should be present for failed job)
      const buttons = wrapper.findAll('.mock-button')
      const retryButton = buttons.find(btn => !btn.attributes('disabled'))
      
      if (retryButton) {
        await retryButton.trigger('click')
        expect(wrapper.emitted('retry')).toBeTruthy()
        expect(wrapper.emitted('retry')[0]).toEqual([mockFailedJob.jobId])
      }
    })

    it('emits export event when export button clicked', async () => {
      wrapper = createWrapper(mockCompletedJob)
      
      // Find export button (should be present for completed job with predictions)
      const buttons = wrapper.findAll('.mock-button')
      const exportButton = buttons.find(btn => !btn.attributes('disabled'))
      
      if (exportButton) {
        await exportButton.trigger('click')
        expect(wrapper.emitted('export')).toBeTruthy()
        expect(wrapper.emitted('export')[0]).toEqual([mockCompletedJob.jobId])
      }
    })
  })

  describe('Utility Functions', () => {
    it('formats date time correctly', () => {
      wrapper = createWrapper()
      
      const dateString = '2024-01-01T10:00:00Z'
      const formatted = wrapper.vm.formatDateTime(dateString)
      
      expect(formatted).toBeTruthy()
      expect(typeof formatted).toBe('string')
    })

    it('formats relative time correctly', () => {
      wrapper = createWrapper()
      
      const recentDate = new Date(Date.now() - 5 * 60 * 1000).toISOString() // 5 minutes ago
      const relativeTime = wrapper.vm.formatRelativeTime(recentDate)
      
      expect(relativeTime).toContain('minutes ago')
    })

    it('calculates duration correctly', () => {
      wrapper = createWrapper()
      
      const job = {
        startedAt: '2024-01-01T10:00:00Z',
        completedAt: '2024-01-01T10:30:00Z'
      }
      
      const duration = wrapper.vm.formatDuration(job)
      expect(duration).toBe('30m')
    })

    it('gets duration in milliseconds correctly', () => {
      wrapper = createWrapper()
      
      const durationMs = wrapper.vm.getDurationMs(mockCompletedJob)
      expect(durationMs).toBe(30 * 60 * 1000) // 30 minutes in ms
    })

    it('gets correct status severity', () => {
      wrapper = createWrapper()
      
      expect(wrapper.vm.getStatusSeverity('COMPLETED')).toBe('success')
      expect(wrapper.vm.getStatusSeverity('RUNNING')).toBe('info')
      expect(wrapper.vm.getStatusSeverity('FAILED')).toBe('danger')
      expect(wrapper.vm.getStatusSeverity('CANCELLED')).toBe('warning')
    })

    it('gets correct progress color', () => {
      wrapper = createWrapper()
      
      expect(wrapper.vm.getProgressColor('COMPLETED')).toBe('var(--green-500)')
      expect(wrapper.vm.getProgressColor('RUNNING')).toBe('var(--blue-500)')
      expect(wrapper.vm.getProgressColor('FAILED')).toBe('var(--red-500)')
    })
  })

  describe('Copy Functionality', () => {
    it('copies job ID successfully', async () => {
      wrapper = createWrapper(mockCompletedJob)
      
      await wrapper.vm.copyJobId(mockCompletedJob.jobId)
      
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(mockCompletedJob.jobId)
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Copied',
        detail: 'Job ID copied to clipboard',
        life: 2000
      })
    })

    it('handles copy failure gracefully', async () => {
      wrapper = createWrapper(mockCompletedJob)
      
      vi.mocked(navigator.clipboard.writeText).mockRejectedValueOnce(new Error('Copy failed'))
      
      await wrapper.vm.copyJobId(mockCompletedJob.jobId)
      
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Copy Failed',
        detail: 'Failed to copy job ID',
        life: 3000
      })
    })
  })

  describe('Edge Cases', () => {
    it('handles job with no processed articles', () => {
      const jobWithNoProgress = {
        ...mockRunningJob,
        processedArticles: 0
      }
      wrapper = createWrapper(jobWithNoProgress)
      
      const percentage = wrapper.vm.getProgressPercentage(jobWithNoProgress)
      expect(percentage).toBe(0)
      
      const avgTime = wrapper.vm.getAverageProcessingTime(jobWithNoProgress)
      expect(avgTime).toBe('N/A')
    })

    it('handles job with no total articles', () => {
      const jobWithNoTotal = {
        ...mockRunningJob,
        totalArticles: 0
      }
      wrapper = createWrapper(jobWithNoTotal)
      
      const percentage = wrapper.vm.getProgressPercentage(jobWithNoTotal)
      expect(percentage).toBe(0)
    })

    it('handles job with no start time', () => {
      const jobWithNoStart = {
        ...mockCompletedJob,
        startedAt: null
      }
      wrapper = createWrapper(jobWithNoStart)
      
      const duration = wrapper.vm.getDurationMs(jobWithNoStart)
      expect(duration).toBe(null)
    })
  })
})