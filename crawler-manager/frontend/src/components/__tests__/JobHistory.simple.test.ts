import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import JobHistory from '../JobHistory.vue'

// Mock the API service
vi.mock('@/services/api', () => ({
  ApiService: {
    getRecentJobs: vi.fn().mockResolvedValue([]),
    getJobHistory: vi.fn().mockResolvedValue([]),
    getJobDetails: vi.fn().mockResolvedValue({}),
    cancelJob: vi.fn().mockResolvedValue({})
  }
}))

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

describe('JobHistory - Simple Tests', () => {
  let pinia: any

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  it('should render without crashing', () => {
    const wrapper = mount(JobHistory, {
      props: {
        crawlerId: 'test-crawler'
      },
      global: {
        plugins: [pinia, PrimeVue],
        stubs: {
          DataTable: { template: '<div data-testid="data-table"><slot /></div>' },
          Column: { template: '<div data-testid="column"><slot /></div>' },
          Button: { template: '<button data-testid="button"><slot /></button>' },
          Badge: { template: '<span data-testid="badge"><slot /></span>' },
          Dialog: { template: '<div data-testid="dialog"><slot /></div>' },
          ProgressBar: { template: '<div data-testid="progress-bar"></div>' },
          MultiSelect: { template: '<div data-testid="multi-select"></div>' },
          Calendar: { template: '<div data-testid="calendar"></div>' },
          InputText: { template: '<input data-testid="input-text" />' },
          JobDetailModal: { template: '<div data-testid="job-detail-modal"></div>' }
        }
      }
    })

    expect(wrapper.find('.job-history').exists()).toBe(true)
    expect(wrapper.find('[data-testid="data-table"]').exists()).toBe(true)
  })

  it('should have the correct component structure', () => {
    const wrapper = mount(JobHistory, {
      props: {
        crawlerId: 'test-crawler'
      },
      global: {
        plugins: [pinia, PrimeVue],
        stubs: {
          DataTable: { template: '<div data-testid="data-table"><slot /></div>' },
          Column: { template: '<div data-testid="column"><slot /></div>' },
          Button: { template: '<button data-testid="button"><slot /></button>' },
          Badge: { template: '<span data-testid="badge"><slot /></span>' },
          Dialog: { template: '<div data-testid="dialog"><slot /></div>' },
          ProgressBar: { template: '<div data-testid="progress-bar"></div>' },
          MultiSelect: { template: '<div data-testid="multi-select"></div>' },
          Calendar: { template: '<div data-testid="calendar"></div>' },
          InputText: { template: '<input data-testid="input-text" />' },
          JobDetailModal: { template: '<div data-testid="job-detail-modal"></div>' }
        }
      }
    })

    // Check for main sections
    expect(wrapper.find('.job-history-header').exists()).toBe(true)
    expect(wrapper.find('.job-history-content').exists()).toBe(true)
    expect(wrapper.find('.job-history-actions').exists()).toBe(true)
  })
})