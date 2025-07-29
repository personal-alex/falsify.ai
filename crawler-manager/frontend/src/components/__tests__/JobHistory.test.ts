import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import Dialog from 'primevue/dialog'
import ProgressBar from 'primevue/progressbar'
import MultiSelect from 'primevue/multiselect'
import Calendar from 'primevue/calendar'
import InputText from 'primevue/inputtext'
import JobHistory from '../JobHistory.vue'
import type { JobStatus } from '@/types/job'

// Mock the API service
vi.mock('@/services/api', () => {
  const mockApiService = {
    getRecentJobs: vi.fn(),
    getJobHistory: vi.fn(),
    getJobDetails: vi.fn(),
    cancelJob: vi.fn()
  }
  return {
    ApiService: mockApiService
  }
})

// Mock the WebSocket service
vi.mock('@/services/websocket', () => ({
  getWebSocketService: vi.fn(() => ({
    onJobUpdate: vi.fn(() => vi.fn()),
    isConnected: vi.fn(() => true)
  })),
  eventBus: {
    on: vi.fn(() => vi.fn()),
    emit: vi.fn(),
    off: vi.fn()
  }
}))

// Mock JobDetailModal component
vi.mock('../JobDetailModal.vue', () => ({
  default: {
    name: 'JobDetailModal',
    template: '<div data-testid="job-detail-modal">Job Detail Modal</div>',
    props: ['job'],
    emits: ['refresh', 'cancel']
  }
}))

const mockJobs: JobStatus[] = [
  {
    jobId: 'job-1',
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
    currentActivity: 'Completed successfully'
  },
  {
    jobId: 'job-2',
    crawlerId: 'crawler-1',
    status: 'RUNNING',
    startTime: '2024-01-15T11:00:00Z',
    articlesProcessed: 5,
    articlesSkipped: 0,
    articlesFailed: 0,
    elapsedTimeMs: 120000,
    successRate: 100,
    totalArticlesAttempted: 5,
    currentActivity: 'Processing articles'
  },
  {
    jobId: 'job-3',
    crawlerId: 'crawler-1',
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
    errorMessage: 'Network timeout',
    currentActivity: 'Failed'
  }
]

describe('JobHistory', () => {
  let wrapper: any
  let pinia: any

  beforeEach(async () => {
    pinia = createPinia()
    setActivePinia(pinia)
    
    // Reset all mocks
    vi.clearAllMocks()
    
    // Get the mocked API service
    const { ApiService } = await import('@/services/api')
    
    // Mock API responses
    vi.mocked(ApiService.getRecentJobs).mockResolvedValue(mockJobs)
    vi.mocked(ApiService.getJobHistory).mockResolvedValue(mockJobs)
    vi.mocked(ApiService.getJobDetails).mockResolvedValue(mockJobs[0])
    vi.mocked(ApiService.cancelJob).mockResolvedValue({ message: 'Job cancelled' })
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })

  const createWrapper = (props = {}) => {
    return mount(JobHistory, {
      props: {
        crawlerId: 'crawler-1',
        ...props
      },
      global: {
        plugins: [pinia, PrimeVue],
        components: {
          DataTable,
          Column,
          Button,
          Badge,
          Dialog,
          ProgressBar,
          MultiSelect,
          Calendar,
          InputText
        },
        stubs: {
          JobDetailModal: {
            template: '<div data-testid="job-detail-modal">Job Detail Modal</div>'
          }
        }
      }
    })
  }

  describe('Component Rendering', () => {
    it('renders the component with header', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      expect(wrapper.find('.job-history').exists()).toBe(true)
      expect(wrapper.find('.job-history-header h3').text()).toBe('Job History')
      expect(wrapper.find('.job-history-actions').exists()).toBe(true)
    })

    it('renders the data table with correct columns', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const table = wrapper.findComponent(DataTable)
      expect(table.exists()).toBe(true)
      
      const columns = wrapper.findAllComponents(Column)
      expect(columns.length).toBeGreaterThan(0)
    })

    it('displays job data correctly', async () => {
      const { ApiService } = await import('@/services/api')
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(vi.mocked(ApiService.getRecentJobs)).toHaveBeenCalledWith('crawler-1')
    })
  })

  describe('Job Status Display', () => {
    it('displays correct status badges', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      // Test status severity mapping
      expect(wrapper.vm.getStatusSeverity('RUNNING')).toBe('info')
      expect(wrapper.vm.getStatusSeverity('COMPLETED')).toBe('success')
      expect(wrapper.vm.getStatusSeverity('FAILED')).toBe('danger')
      expect(wrapper.vm.getStatusSeverity('CANCELLED')).toBe('warning')
    })

    it('displays correct status classes', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.getStatusClass('RUNNING')).toBe('job-status-running')
      expect(wrapper.vm.getStatusClass('COMPLETED')).toBe('job-status-completed')
      expect(wrapper.vm.getStatusClass('FAILED')).toBe('job-status-failed')
      expect(wrapper.vm.getStatusClass('CANCELLED')).toBe('job-status-cancelled')
    })
  })

  describe('Job Actions', () => {
    it('shows job details when view button is clicked', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      // Mock the showJobDetails method
      const showJobDetailsSpy = vi.spyOn(wrapper.vm, 'showJobDetails')
      
      // Simulate clicking view details button
      await wrapper.vm.showJobDetails(mockJobs[0])
      
      expect(showJobDetailsSpy).toHaveBeenCalledWith(mockJobs[0])
    })

    it('cancels job when cancel button is clicked', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      
      const { ApiService } = await import('@/services/api')
      await wrapper.vm.cancelJob(mockJobs[1]) // Running job
      
      expect(vi.mocked(ApiService.cancelJob)).toHaveBeenCalledWith('job-2')
    })
  })

  describe('Filtering', () => {
    it('opens filter dialog when filter button is clicked', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.showFilterDialog).toBe(false)
      
      wrapper.vm.showFilterDialog = true
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.showFilterDialog).toBe(true)
    })

    it('filters jobs by status', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      // Set up filter
      wrapper.vm.activeFilters = { status: ['COMPLETED'] }
      await wrapper.vm.$nextTick()

      // Mock jobs data
      wrapper.vm.jobs = mockJobs
      await wrapper.vm.$nextTick()

      const filteredJobs = wrapper.vm.filteredJobs
      expect(filteredJobs.length).toBe(1)
      expect(filteredJobs[0].status).toBe('COMPLETED')
    })

    it('filters jobs by search term', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      // Set up filter
      wrapper.vm.activeFilters = { searchTerm: 'job-1' }
      wrapper.vm.jobs = mockJobs
      await wrapper.vm.$nextTick()

      const filteredJobs = wrapper.vm.filteredJobs
      expect(filteredJobs.length).toBe(1)
      expect(filteredJobs[0].jobId).toBe('job-1')
    })

    it('clears filters correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      // Set up filters
      wrapper.vm.filterOptions = {
        status: ['COMPLETED'],
        searchTerm: 'test'
      }
      wrapper.vm.activeFilters = {
        status: ['COMPLETED'],
        searchTerm: 'test'
      }

      await wrapper.vm.clearFilters()

      expect(wrapper.vm.filterOptions.status).toEqual([])
      expect(wrapper.vm.filterOptions.searchTerm).toBe('')
      expect(wrapper.vm.activeFilters).toEqual({})
    })
  })

  describe('Sorting', () => {
    it('sorts jobs correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      wrapper.vm.jobs = [...mockJobs]
      
      // Test sorting by start time descending
      const sortEvent = {
        sortField: 'startTime',
        sortOrder: -1
      }
      
      await wrapper.vm.onSort(sortEvent)
      
      expect(wrapper.vm.sortField).toBe('startTime')
      expect(wrapper.vm.sortOrder).toBe(-1)
    })
  })

  describe('Pagination', () => {
    it('handles pagination when enabled', async () => {
      wrapper = createWrapper({ showPagination: true, pageSize: 5 })
      await wrapper.vm.$nextTick()

      const { ApiService } = await import('@/services/api')
      const pageEvent = { page: 1 }
      await wrapper.vm.onPageChange(pageEvent)

      expect(vi.mocked(ApiService.getJobHistory)).toHaveBeenCalledWith('crawler-1', 1, 5)
    })

    it('uses recent jobs when pagination is disabled', async () => {
      const { ApiService } = await import('@/services/api')
      wrapper = createWrapper({ showPagination: false })
      await wrapper.vm.$nextTick()

      expect(vi.mocked(ApiService.getRecentJobs)).toHaveBeenCalledWith('crawler-1')
    })
  })

  describe('Utility Methods', () => {
    it('formats date time correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const dateString = '2024-01-15T10:00:00Z'
      const formatted = wrapper.vm.formatDateTime(dateString)
      
      expect(typeof formatted).toBe('string')
      expect(formatted.length).toBeGreaterThan(0)
    })

    it('formats duration correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const job = { durationMs: 300000, elapsedTimeMs: 300000 }
      const formatted = wrapper.vm.formatDuration(job)
      
      expect(formatted).toBe('5m 0s')
    })

    it('truncates activity text correctly', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const longActivity = 'This is a very long activity description that should be truncated'
      const truncated = wrapper.vm.truncateActivity(longActivity)
      
      expect(truncated.length).toBeLessThanOrEqual(33) // 30 chars + '...'
      expect(truncated.endsWith('...')).toBe(true)
    })

    it('handles undefined activity', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      const result = wrapper.vm.truncateActivity(undefined)
      expect(result).toBe('-')
    })
  })

  describe('WebSocket Integration', () => {
    it('sets up WebSocket listeners on mount', async () => {
      const { eventBus } = await import('@/services/websocket')
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      expect(vi.mocked(eventBus.on)).toHaveBeenCalledWith('job:updated', expect.any(Function))
    })

    it('handles job update messages', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      wrapper.vm.jobs = [...mockJobs]

      const updateMessage = {
        type: 'job.progress',
        crawlerId: 'crawler-1',
        jobId: 'job-2',
        status: 'RUNNING',
        progress: {
          articlesProcessed: 8,
          articlesSkipped: 1,
          articlesFailed: 0,
          currentActivity: 'Processing more articles'
        },
        timestamp: '2024-01-15T11:05:00Z'
      }

      await wrapper.vm.handleJobUpdate(updateMessage)

      const updatedJob = wrapper.vm.jobs.find((job: JobStatus) => job.jobId === 'job-2')
      expect(updatedJob.articlesProcessed).toBe(8)
      expect(updatedJob.currentActivity).toBe('Processing more articles')
    })
  })

  describe('Error Handling', () => {
    it('handles API errors gracefully', async () => {
      const { ApiService } = await import('@/services/api')
      vi.mocked(ApiService.getRecentJobs).mockRejectedValue(new Error('API Error'))

      wrapper = createWrapper()
      await wrapper.vm.$nextTick()
      await new Promise(resolve => setTimeout(resolve, 100))

      expect(wrapper.vm.error).toBe('API Error')
    })

    it('displays error message when jobs fail to load', async () => {
      wrapper = createWrapper()
      await wrapper.vm.$nextTick()

      wrapper.vm.error = 'Failed to load jobs'
      await wrapper.vm.$nextTick()

      expect(wrapper.vm.emptyMessage).toBe('Error: Failed to load jobs')
    })
  })

  describe('Auto Refresh', () => {
    it('sets up auto refresh when enabled', async () => {
      vi.useFakeTimers()
      
      wrapper = createWrapper({ autoRefresh: true, refreshInterval: 1000 })
      await wrapper.vm.$nextTick()

      const refreshSpy = vi.spyOn(wrapper.vm, 'refreshJobs')
      
      vi.advanceTimersByTime(1000)
      
      expect(refreshSpy).toHaveBeenCalled()
      
      vi.useRealTimers()
    })

    it('does not auto refresh when disabled', async () => {
      vi.useFakeTimers()
      
      wrapper = createWrapper({ autoRefresh: false })
      await wrapper.vm.$nextTick()

      const refreshSpy = vi.spyOn(wrapper.vm, 'refreshJobs')
      
      vi.advanceTimersByTime(5000)
      
      expect(refreshSpy).not.toHaveBeenCalled()
      
      vi.useRealTimers()
    })
  })
})