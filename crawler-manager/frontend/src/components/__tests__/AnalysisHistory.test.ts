import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { nextTick } from 'vue'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import AnalysisHistory from '../AnalysisHistory.vue'

// Mock the API service
vi.mock('@/services/api', () => ({
  ApiService: {
    getAnalysisHistory: vi.fn(),
    cancelAnalysisJob: vi.fn(),
    retryAnalysisJob: vi.fn(),
    exportAnalysisResults: vi.fn()
  }
}))

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined)
  }
})

// Mock URL.createObjectURL and revokeObjectURL
global.URL.createObjectURL = vi.fn(() => 'mock-url')
global.URL.revokeObjectURL = vi.fn()

// Mock toast
const mockToast = {
  add: vi.fn()
}

const mockAnalysisJobs = [
  {
    id: 1,
    jobId: 'job-123',
    status: 'COMPLETED',
    startedAt: '2024-01-01T10:00:00Z',
    completedAt: '2024-01-01T10:30:00Z',
    totalArticles: 50,
    processedArticles: 50,
    predictionsFound: 25,
    analysisType: 'mock'
  },
  {
    id: 2,
    jobId: 'job-456',
    status: 'RUNNING',
    startedAt: '2024-01-01T11:00:00Z',
    completedAt: null,
    totalArticles: 100,
    processedArticles: 30,
    predictionsFound: 0,
    analysisType: 'llm'
  },
  {
    id: 3,
    jobId: 'job-789',
    status: 'FAILED',
    startedAt: '2024-01-01T09:00:00Z',
    completedAt: '2024-01-01T09:15:00Z',
    totalArticles: 25,
    processedArticles: 10,
    predictionsFound: 0,
    analysisType: 'mock',
    errorMessage: 'Processing failed due to network error'
  }
]

describe('AnalysisHistory', () => {
  let wrapper: VueWrapper<any>

  const createWrapper = (props = {}) => {
    return mount(AnalysisHistory, {
      props: {
        history: mockAnalysisJobs,
        autoRefresh: false,
        ...props
      },
      global: {
        plugins: [PrimeVue, ToastService],
        mocks: {
          $toast: mockToast
        },
        stubs: {
          DataTable: {
            template: '<div class="mock-datatable"><slot /></div>',
            props: ['value', 'loading', 'paginator', 'rows', 'totalRecords']
          },
          Column: {
            template: '<div class="mock-column"><slot /></div>',
            props: ['field', 'header', 'sortable']
          },
          Button: {
            template: '<button class="mock-button" @click="$emit(\'click\')"><slot /></button>',
            props: ['icon', 'label', 'severity', 'size', 'loading', 'disabled']
          },
          Badge: {
            template: '<span class="mock-badge">{{ value }}</span>',
            props: ['value', 'severity']
          },
          ProgressBar: {
            template: '<div class="mock-progressbar"></div>',
            props: ['value', 'showValue']
          },
          Dialog: {
            template: '<div v-if="visible" class="mock-dialog"><slot /></div>',
            props: ['visible', 'header', 'modal', 'style', 'breakpoints']
          },
          MultiSelect: {
            template: '<div class="mock-multiselect"></div>',
            props: ['modelValue', 'options', 'optionLabel', 'optionValue', 'placeholder']
          },
          InputText: {
            template: '<input class="mock-inputtext" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
            props: ['modelValue', 'placeholder']
          },
          InputNumber: {
            template: '<input class="mock-inputnumber" type="number" :value="modelValue" @input="$emit(\'update:modelValue\', parseInt($event.target.value))" />',
            props: ['modelValue', 'placeholder', 'min']
          },
          Calendar: {
            template: '<input class="mock-calendar" type="date" :value="modelValue" @input="$emit(\'update:modelValue\', new Date($event.target.value))" />',
            props: ['modelValue', 'showIcon', 'dateFormat']
          },
          Chip: {
            template: '<span class="mock-chip" @click="$emit(\'click\')">{{ label }} <button v-if="removable" @click="$emit(\'remove\')">×</button></span>',
            props: ['label', 'removable']
          },
          AnalysisJobDetail: {
            template: '<div class="mock-analysis-job-detail"></div>',
            props: ['job']
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
    it('renders correctly with analysis jobs', () => {
      wrapper = createWrapper()
      
      expect(wrapper.find('.analysis-history').exists()).toBe(true)
      expect(wrapper.find('.history-header h3').text()).toBe('Analysis History')
      expect(wrapper.find('.header-description').text()).toBe('View and manage prediction analysis jobs')
    })

    it('displays header actions', () => {
      wrapper = createWrapper()
      
      const headerActions = wrapper.find('.header-actions')
      expect(headerActions.exists()).toBe(true)
      
      const buttons = headerActions.findAll('.mock-button')
      expect(buttons).toHaveLength(2)
      expect(buttons[0].text()).toContain('Refresh')
      expect(buttons[1].text()).toContain('Filter')
    })

    it('displays quick filter chips', () => {
      wrapper = createWrapper()
      
      const filterChips = wrapper.find('.filter-chips')
      expect(filterChips.exists()).toBe(true)
      
      const chips = filterChips.findAll('.mock-chip')
      expect(chips.length).toBeGreaterThan(0)
      expect(chips[0].text()).toContain('Running')
      expect(chips[1].text()).toContain('Completed')
      expect(chips[2].text()).toContain('Failed')
    })

    it('displays analysis jobs table', () => {
      wrapper = createWrapper()
      
      expect(wrapper.find('.analysis-history-table').exists()).toBe(true)
      expect(wrapper.find('.mock-datatable').exists()).toBe(true)
    })
  })

  describe('Job Status Display', () => {
    it('displays correct status badges', () => {
      wrapper = createWrapper()
      
      const badges = wrapper.findAll('.mock-badge')
      expect(badges.length).toBeGreaterThan(0)
    })

    it('shows progress bar for running jobs', async () => {
      wrapper = createWrapper()
      await nextTick()
      
      // Progress bars should be shown for running jobs
      const progressBars = wrapper.findAll('.mock-progressbar')
      expect(progressBars.length).toBeGreaterThan(0)
    })

    it('displays job duration correctly', () => {
      wrapper = createWrapper()
      
      // Check that duration formatting is working
      expect(wrapper.html()).toContain('30m') // Completed job duration
    })
  })

  describe('Filtering Functionality', () => {
    it('applies quick filters correctly', async () => {
      wrapper = createWrapper()
      
      const runningChip = wrapper.findAll('.mock-chip')[0]
      await runningChip.trigger('click')
      
      // Should filter to only running jobs
      expect(wrapper.vm.activeQuickFilter).toBe('RUNNING')
    })

    it('clears quick filter when clicked again', async () => {
      wrapper = createWrapper()
      
      const runningChip = wrapper.findAll('.mock-chip')[0]
      await runningChip.trigger('click')
      await runningChip.trigger('click')
      
      expect(wrapper.vm.activeQuickFilter).toBe(null)
    })

    it('opens filter dialog', async () => {
      wrapper = createWrapper()
      
      const filterButton = wrapper.findAll('.mock-button')[1]
      await filterButton.trigger('click')
      
      expect(wrapper.vm.showFilterDialog).toBe(true)
    })

    it('applies advanced filters', async () => {
      wrapper = createWrapper()
      
      // Set filter options
      wrapper.vm.filterOptions.status = ['COMPLETED']
      wrapper.vm.filterOptions.analysisType = ['mock']
      wrapper.vm.applyFilters()
      
      expect(wrapper.vm.activeFilters.status).toEqual(['COMPLETED'])
      expect(wrapper.vm.activeFilters.analysisType).toEqual(['mock'])
    })

    it('clears all filters', async () => {
      wrapper = createWrapper()
      
      // Set some filters first
      wrapper.vm.activeQuickFilter = 'RUNNING'
      wrapper.vm.activeFilters.status = ['COMPLETED']
      
      wrapper.vm.clearAllFilters()
      
      expect(wrapper.vm.activeQuickFilter).toBe(null)
      expect(wrapper.vm.activeFilters.status).toEqual([])
    })
  })

  describe('Job Actions', () => {
    it('can view job details', async () => {
      wrapper = createWrapper()
      
      await wrapper.vm.viewJobDetails(mockAnalysisJobs[0])
      
      expect(wrapper.vm.selectedJob).toEqual(mockAnalysisJobs[0])
      expect(wrapper.vm.showDetailModal).toBe(true)
    })

    it('can cancel running job', async () => {
      wrapper = createWrapper()
      
      const runningJob = mockAnalysisJobs[1]
      wrapper.vm.confirmCancelJob(runningJob)
      
      expect(wrapper.vm.jobToCancel).toEqual(runningJob)
      expect(wrapper.vm.showCancelDialog).toBe(true)
    })

    it('determines correct action availability', () => {
      wrapper = createWrapper()
      
      const completedJob = mockAnalysisJobs[0]
      const runningJob = mockAnalysisJobs[1]
      const failedJob = mockAnalysisJobs[2]
      
      // Export should be available for completed jobs with predictions
      expect(wrapper.vm.canExport(completedJob)).toBe(true)
      expect(wrapper.vm.canExport(runningJob)).toBe(false)
      
      // Cancel should be available for running jobs
      expect(wrapper.vm.canCancel(runningJob)).toBe(true)
      expect(wrapper.vm.canCancel(completedJob)).toBe(false)
      
      // Retry should be available for failed jobs
      expect(wrapper.vm.canRetry(failedJob)).toBe(true)
      expect(wrapper.vm.canRetry(completedJob)).toBe(false)
    })
  })

  describe('Utility Functions', () => {
    it('formats dates correctly', () => {
      wrapper = createWrapper()
      
      const dateString = '2024-01-01T10:00:00Z'
      const formattedDate = wrapper.vm.formatDate(dateString)
      const formattedTime = wrapper.vm.formatTime(dateString)
      
      expect(formattedDate).toBeTruthy()
      expect(formattedTime).toBeTruthy()
    })

    it('calculates progress percentage correctly', () => {
      wrapper = createWrapper()
      
      const job = {
        totalArticles: 100,
        processedArticles: 30
      }
      
      const percentage = wrapper.vm.getProgressPercentage(job)
      expect(percentage).toBe(30)
    })

    it('truncates job ID correctly', () => {
      wrapper = createWrapper()
      
      const longJobId = 'very-long-job-id-that-should-be-truncated'
      const truncated = wrapper.vm.truncateJobId(longJobId)
      
      expect(truncated).toBe('very-lon...')
    })

    it('formats duration correctly', () => {
      wrapper = createWrapper()
      
      const job = {
        startedAt: '2024-01-01T10:00:00Z',
        completedAt: '2024-01-01T10:30:00Z'
      }
      
      const duration = wrapper.vm.formatDuration(job)
      expect(duration).toBe('30m')
    })
  })

  describe('Copy Functionality', () => {
    it('copies job ID to clipboard', async () => {
      wrapper = createWrapper()
      
      await wrapper.vm.copyJobId('test-job-id')
      
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith('test-job-id')
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Copied',
        detail: 'Job ID copied to clipboard',
        life: 2000
      })
    })

    it('handles clipboard copy failure', async () => {
      wrapper = createWrapper()
      
      vi.mocked(navigator.clipboard.writeText).mockRejectedValueOnce(new Error('Copy failed'))
      
      await wrapper.vm.copyJobId('test-job-id')
      
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Copy Failed',
        detail: 'Failed to copy job ID',
        life: 3000
      })
    })
  })

  describe('Export Functionality', () => {
    it('exports results as CSV', async () => {
      wrapper = createWrapper()
      
      const job = mockAnalysisJobs[0]
      await wrapper.vm.exportResults(job)
      
      expect(global.URL.createObjectURL).toHaveBeenCalled()
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Export Complete',
        detail: 'Results exported successfully',
        life: 3000
      })
    })

    it('converts data to CSV format correctly', () => {
      wrapper = createWrapper()
      
      const mockData = [
        {
          predictionText: 'Test prediction',
          rating: 5,
          confidenceScore: 0.9,
          context: 'Test context',
          article: {
            title: 'Test Article',
            author: { name: 'Test Author' }
          }
        }
      ]
      
      const csv = wrapper.vm.convertToCSV(mockData)
      
      expect(csv).toContain('Prediction,Rating,Confidence,Article Title,Author,Context')
      expect(csv).toContain('"Test prediction"')
      expect(csv).toContain('5')
      expect(csv).toContain('0.9')
    })
  })

  describe('Pagination', () => {
    it('handles pagination correctly', () => {
      wrapper = createWrapper({ pageSize: 2 })
      
      expect(wrapper.vm.paginatedJobs.length).toBeLessThanOrEqual(2)
    })

    it('resets pagination when filters change', async () => {
      wrapper = createWrapper()
      
      wrapper.vm.first = 20
      wrapper.vm.toggleQuickFilter('RUNNING')
      
      expect(wrapper.vm.first).toBe(0)
    })
  })

  describe('Status Counts', () => {
    it('calculates status counts correctly', () => {
      wrapper = createWrapper()
      
      const runningCount = wrapper.vm.getStatusCount('RUNNING')
      const completedCount = wrapper.vm.getStatusCount('COMPLETED')
      const failedCount = wrapper.vm.getStatusCount('FAILED')
      
      expect(runningCount).toBe(1)
      expect(completedCount).toBe(1)
      expect(failedCount).toBe(1)
    })
  })

  describe('Modal Interactions', () => {
    it('handles cancel from modal', async () => {
      wrapper = createWrapper()
      
      const jobId = 'test-job-id'
      await wrapper.vm.handleCancelFromModal(jobId)
      
      expect(wrapper.vm.showDetailModal).toBe(false)
      expect(wrapper.vm.showCancelDialog).toBe(true)
    })

    it('handles retry from modal', async () => {
      wrapper = createWrapper()
      
      const jobId = mockAnalysisJobs[2].jobId
      await wrapper.vm.handleRetryFromModal(jobId)
      
      expect(wrapper.vm.showDetailModal).toBe(false)
    })

    it('handles export from modal', async () => {
      wrapper = createWrapper()
      
      const jobId = mockAnalysisJobs[0].jobId
      await wrapper.vm.handleExportFromModal(jobId)
      
      expect(global.URL.createObjectURL).toHaveBeenCalled()
    })
  })

  describe('Error Handling', () => {
    it('handles refresh errors gracefully', async () => {
      wrapper = createWrapper()
      
      // Mock a failed refresh
      wrapper.vm.loadAnalysisHistory = vi.fn().mockRejectedValue(new Error('Network error'))
      
      await wrapper.vm.refreshHistory()
      
      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Error',
        detail: 'Failed to refresh history',
        life: 5000
      })
    })

    it('displays appropriate empty message', () => {
      wrapper = createWrapper({ history: [] })
      
      expect(wrapper.vm.emptyMessage).toBe('No analysis jobs found')
      
      // With filters applied
      wrapper.vm.activeQuickFilter = 'RUNNING'
      expect(wrapper.vm.emptyMessage).toBe('No jobs match the current filters')
    })
  })

  describe('Props Handling', () => {
    it('uses provided history prop', () => {
      const customHistory = [mockAnalysisJobs[0]]
      wrapper = createWrapper({ history: customHistory })
      
      expect(wrapper.vm.analysisJobs).toEqual(customHistory)
    })

    it('handles empty history prop', () => {
      wrapper = createWrapper({ history: [] })
      
      expect(wrapper.vm.analysisJobs).toEqual([])
    })
  })
})