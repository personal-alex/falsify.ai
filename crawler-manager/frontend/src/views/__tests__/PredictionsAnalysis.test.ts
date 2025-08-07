import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, VueWrapper } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import Tooltip from 'primevue/tooltip'
import PredictionsAnalysis from '../PredictionsAnalysis.vue'

// Mock the router
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>Home</div>' } },
    { path: '/predictions/history', component: { template: '<div>History</div>' } }
  ]
})

// Mock API Service
vi.mock('@/services/api', () => ({
  ApiService: {
    getArticlesForAnalysis: vi.fn().mockResolvedValue({
      articles: [],
      pagination: { page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false, hasPrevious: false }
    }),
    getAllAuthors: vi.fn().mockResolvedValue([]),
    startAnalysis: vi.fn().mockResolvedValue({ id: 1, jobId: 'test-job', status: 'PENDING' }),
    getAnalysisJobStatus: vi.fn().mockResolvedValue({ status: 'COMPLETED' }),
    cancelAnalysisJob: vi.fn().mockResolvedValue({}),
    exportAnalysisResults: vi.fn().mockResolvedValue(new Blob())
  },
  ApiServiceError: class extends Error {
    constructor(message: string) {
      super(message)
      this.name = 'ApiServiceError'
    }
  }
}))

// Mock WebSocket service
vi.mock('@/services/websocket', () => ({
  getWebSocketService: vi.fn(() => ({
    onAnalysisJobUpdate: vi.fn(() => vi.fn()),
    onPredictionExtracted: vi.fn(() => vi.fn())
  }))
}))

// Mock components
vi.mock('@/components/ArticleFilter.vue', () => ({
  default: {
    name: 'ArticleFilter',
    template: '<div data-testid="article-filter"></div>',
    props: ['filters', 'authors'],
    emits: ['filter-changed']
  }
}))

vi.mock('@/components/ArticleSelectionTable.vue', () => ({
  default: {
    name: 'ArticleSelectionTable',
    template: '<div data-testid="article-selection-table"></div>',
    props: ['articles', 'selected-articles', 'loading'],
    emits: ['selection-changed', 'refresh-articles']
  }
}))

vi.mock('@/components/AnalysisProgress.vue', () => ({
  default: {
    name: 'AnalysisProgress',
    template: '<div data-testid="analysis-progress"></div>',
    props: ['job'],
    emits: ['cancel-analysis', 'retry-analysis', 'job-updated']
  }
}))

vi.mock('@/components/PredictionResults.vue', () => ({
  default: {
    name: 'PredictionResults',
    template: '<div data-testid="prediction-results"></div>',
    props: ['predictions', 'loading'],
    emits: ['export-results', 'refresh-results', 'prediction-selected']
  }
}))

// Mock toast and confirm
const mockToast = {
  add: vi.fn()
}

const mockConfirm = {
  require: vi.fn()
}

describe('PredictionsAnalysis', () => {
  let wrapper: VueWrapper<any>

  const createWrapper = (props = {}) => {
    return mount(PredictionsAnalysis, {
      props,
      global: {
        plugins: [
          PrimeVue,
          ToastService,
          ConfirmationService,
          router
        ],
        directives: {
          tooltip: Tooltip
        },
        provide: {
          $toast: mockToast,
          $confirm: mockConfirm
        },
        stubs: {
          Panel: {
            template: '<div class="panel"><div class="panel-header"><slot name="header"></slot></div><div class="panel-content"><slot></slot></div></div>'
          },
          Toolbar: {
            template: '<div class="toolbar"><div class="toolbar-start"><slot name="start"></slot></div><div class="toolbar-end"><slot name="end"></slot></div></div>'
          },
          Splitter: {
            template: '<div class="splitter"><slot></slot></div>'
          },
          SplitterPanel: {
            template: '<div class="splitter-panel"><slot></slot></div>',
            props: ['size', 'minSize']
          },
          Button: {
            template: '<button @click="$emit(\'click\')" :disabled="disabled" :loading="loading" class="p-button"><slot></slot></button>',
            props: ['disabled', 'loading', 'severity', 'size', 'outlined', 'icon', 'label'],
            emits: ['click']
          },
          Menu: {
            template: '<div class="menu"></div>',
            props: ['model', 'popup'],
            methods: {
              toggle: vi.fn()
            }
          },
          ConfirmDialog: {
            template: '<div class="confirm-dialog"></div>'
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

  describe('Toolbar Interface', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('renders the unified toolbar', () => {
      const toolbar = wrapper.find('.unified-toolbar')
      expect(toolbar.exists()).toBe(true)
    })

    it('renders toolbar sections', () => {
      const primaryActions = wrapper.find('.toolbar-section.primary-actions')
      const secondaryActions = wrapper.find('.toolbar-section.secondary-actions')
      
      expect(primaryActions.exists()).toBe(true)
      expect(secondaryActions.exists()).toBe(true)
    })

    it('shows selection info when articles are selected', async () => {
      // Simulate selection of articles
      await wrapper.vm.onSelectionChanged([
        { id: 1, title: 'Test Article 1' },
        { id: 2, title: 'Test Article 2' }
      ])
      await wrapper.vm.$nextTick()

      const selectionInfo = wrapper.find('.selection-info')
      expect(selectionInfo.exists()).toBe(true)
      expect(selectionInfo.text()).toContain('Estimated time')
    })

    it('shows warning for large batch analysis', async () => {
      // Simulate selection of many articles
      const manyArticles = Array.from({ length: 101 }, (_, i) => ({
        id: i + 1,
        title: `Test Article ${i + 1}`
      }))
      
      await wrapper.vm.onSelectionChanged(manyArticles)
      await wrapper.vm.$nextTick()

      const selectionInfo = wrapper.find('.selection-info')
      expect(selectionInfo.text()).toContain('Large batch analysis may take several minutes')
    })
  })

  describe('Component State', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('initializes with empty selection', () => {
      expect(wrapper.vm.selectedArticles).toHaveLength(0)
    })

    it('updates selection when onSelectionChanged is called', async () => {
      const testArticles = [{ id: 1, title: 'Test Article' }]
      await wrapper.vm.onSelectionChanged(testArticles)
      
      expect(wrapper.vm.selectedArticles).toEqual(testArticles)
    })

    it('calculates estimated time correctly', async () => {
      await wrapper.vm.onSelectionChanged([
        { id: 1, title: 'Test Article 1' },
        { id: 2, title: 'Test Article 2' }
      ])
      
      expect(wrapper.vm.estimatedTime).toBe(1) // 2 articles = 1 minute (10 articles per minute)
    })
  })

  describe('Methods', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('clearSelection method clears selected articles', async () => {
      // First select some articles
      await wrapper.vm.onSelectionChanged([{ id: 1, title: 'Test Article' }])
      expect(wrapper.vm.selectedArticles).toHaveLength(1)

      // Clear selection
      await wrapper.vm.clearSelection()
      expect(wrapper.vm.selectedArticles).toHaveLength(0)
    })

    it('viewHistory method navigates to history page', async () => {
      const routerSpy = vi.spyOn(router, 'push')
      
      await wrapper.vm.viewHistory()
      expect(routerSpy).toHaveBeenCalledWith('/predictions/history')
    })

    it('has export menu items configured', () => {
      expect(wrapper.vm.exportMenuItems).toHaveLength(2)
      expect(wrapper.vm.exportMenuItems[0].label).toBe('Export as CSV')
      expect(wrapper.vm.exportMenuItems[1].label).toBe('Export as JSON')
    })
  })

  describe('Keyboard Shortcuts', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('has keyboard shortcut handler method', () => {
      expect(typeof wrapper.vm.handleKeyboardShortcuts).toBe('function')
    })

    it('does not trigger shortcuts without Ctrl key', async () => {
      const analysisSpy = vi.spyOn(wrapper.vm, 'startAnalysis')
      
      // Call the handler directly with a non-ctrl event
      const event = new KeyboardEvent('keydown', { key: 'a' })
      wrapper.vm.handleKeyboardShortcuts(event)

      expect(analysisSpy).not.toHaveBeenCalled()
    })

    it('handles keyboard shortcuts with Ctrl key and calls preventDefault', async () => {
      // Create a proper event object with preventDefault
      const event = {
        key: 'r',
        ctrlKey: true,
        metaKey: false,
        preventDefault: vi.fn()
      }
      
      // Test the keyboard shortcut handler directly with Ctrl+R
      wrapper.vm.handleKeyboardShortcuts(event)
      
      // At minimum, preventDefault should be called for recognized shortcuts
      expect(event.preventDefault).toHaveBeenCalled()
    })
  })

  describe('Responsive Design', () => {
    beforeEach(() => {
      wrapper = createWrapper()
    })

    it('renders toolbar with responsive classes', () => {
      const toolbar = wrapper.find('.unified-toolbar')
      expect(toolbar.exists()).toBe(true)

      const primaryActions = wrapper.find('.toolbar-section.primary-actions')
      const secondaryActions = wrapper.find('.toolbar-section.secondary-actions')
      
      expect(primaryActions.exists()).toBe(true)
      expect(secondaryActions.exists()).toBe(true)
    })
  })
})