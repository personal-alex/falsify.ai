import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PredictionResults from '../PredictionResults.vue'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn(() => Promise.resolve())
  },
  share: vi.fn(() => Promise.resolve())
})

describe('PredictionResults', () => {
  const mockAuthor = {
    id: 1,
    name: 'John Doe',
    avatarUrl: 'https://example.com/avatar.jpg'
  }

  const mockArticle = {
    id: 1,
    title: 'Test Article About Market Predictions',
    url: 'https://example.com/article-1',
    crawlerSource: 'drucker',
    createdAt: '2024-01-15T10:00:00Z',
    author: mockAuthor
  }

  const mockPredictions = [
    {
      id: 'pred-1',
      predictionText: 'The market will experience significant volatility in Q2',
      predictionType: 'economic',
      rating: 4,
      confidenceScore: 0.85,
      context: 'Based on current economic indicators...',
      article: mockArticle,
      extractedAt: '2024-01-15T10:30:00Z'
    },
    {
      id: 'pred-2',
      predictionText: 'Technology stocks will outperform traditional sectors',
      predictionType: 'market',
      rating: 3,
      confidenceScore: 0.72,
      context: 'Analysis of recent tech trends...',
      article: mockArticle,
      extractedAt: '2024-01-15T10:31:00Z'
    },
    {
      id: 'pred-3',
      predictionText: 'Interest rates will remain stable through 2024',
      predictionType: 'economic',
      rating: 5,
      confidenceScore: 0.91,
      context: 'Federal Reserve policy analysis...',
      article: mockArticle,
      extractedAt: '2024-01-15T10:32:00Z'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  const createWrapper = (predictions = mockPredictions, props = {}) => {
    return mount(PredictionResults, {
      props: {
        predictions,
        loading: false,
        ...props
      },
      global: {
        plugins: [PrimeVue, ToastService]
      }
    })
  }

  describe('Component Rendering', () => {
    it('renders the component with prediction data', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.find('.prediction-results-panel').exists()).toBe(true)
      expect(wrapper.text()).toContain('Analysis Results')
      expect(wrapper.text()).toContain('3 predictions')
    })

    it('displays summary statistics correctly', () => {
      const wrapper = createWrapper()
      
      // Total predictions
      expect(wrapper.text()).toContain('3')
      
      // Average rating (4+3+5)/3 = 4.0
      expect(wrapper.text()).toContain('4.0')
      
      // High confidence count (>80%): 2 predictions
      expect(wrapper.text()).toContain('2')
      
      // Unique articles count
      expect(wrapper.text()).toContain('1')
    })

    it('shows empty state when no predictions', () => {
      const wrapper = createWrapper([])
      
      expect(wrapper.text()).toContain('No Predictions Found')
      expect(wrapper.text()).toContain('Try adjusting your filters')
    })

    it('shows loading state when loading prop is true', () => {
      const wrapper = createWrapper(mockPredictions, { loading: true })
      
      expect(wrapper.find('[data-pc-name="progressspinner"]').exists()).toBe(true)
      expect(wrapper.text()).toContain('Loading results')
    })
  })

  describe('Data Table', () => {
    it('displays predictions in table format', () => {
      const wrapper = createWrapper()
      
      const table = wrapper.find('[data-pc-name="datatable"]')
      expect(table.exists()).toBe(true)
      
      // Check if prediction texts are displayed
      expect(wrapper.text()).toContain('The market will experience significant volatility')
      expect(wrapper.text()).toContain('Technology stocks will outperform')
      expect(wrapper.text()).toContain('Interest rates will remain stable')
    })

    it('shows rating stars for each prediction', () => {
      const wrapper = createWrapper()
      
      const ratings = wrapper.findAll('[data-pc-name="rating"]')
      expect(ratings.length).toBeGreaterThan(0)
    })

    it('displays confidence scores as progress bars', () => {
      const wrapper = createWrapper()
      
      const progressBars = wrapper.findAll('[data-pc-name="progressbar"]')
      expect(progressBars.length).toBeGreaterThan(0)
    })

    it('shows article information with author details', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.text()).toContain('Test Article About Market Predictions')
      expect(wrapper.text()).toContain('John Doe')
      expect(wrapper.text()).toContain('drucker')
    })
  })

  describe('Filtering', () => {
    it('filters by minimum rating', async () => {
      const wrapper = createWrapper()
      
      // Set minimum rating to 4
      const ratingDropdown = wrapper.find('[data-pc-name="dropdown"]')
      await ratingDropdown.setValue(4)
      await nextTick()
      
      // Should show only predictions with rating >= 4 (2 predictions)
      const tableRows = wrapper.findAll('tbody tr')
      expect(tableRows.length).toBeLessThanOrEqual(2)
    })

    it('filters by minimum confidence', async () => {
      const wrapper = createWrapper()
      
      // Set minimum confidence to 80%
      const confidenceSlider = wrapper.find('[data-pc-name="slider"]')
      await confidenceSlider.setValue(80)
      await nextTick()
      
      // Should show only high confidence predictions
      expect(wrapper.vm.filteredPredictions.length).toBe(2)
    })

    it('filters by search text', async () => {
      const wrapper = createWrapper()
      
      // Search for "technology"
      const searchInput = wrapper.find('input[placeholder*="Search"]')
      await searchInput.setValue('technology')
      await nextTick()
      
      // Should show only predictions containing "technology"
      expect(wrapper.vm.filteredPredictions.length).toBe(1)
      expect(wrapper.vm.filteredPredictions[0].predictionText).toContain('Technology')
    })

    it('clears all filters when clear button is clicked', async () => {
      const wrapper = createWrapper()
      
      // Set some filters
      const searchInput = wrapper.find('input[placeholder*="Search"]')
      await searchInput.setValue('test')
      
      // Click clear filters button
      const clearButton = wrapper.find('button[aria-label*="Clear Filters"]')
      await clearButton.trigger('click')
      
      // All filters should be reset
      expect(wrapper.vm.filters.searchText).toBe('')
      expect(wrapper.vm.filters.minRating).toBeNull()
      expect(wrapper.vm.filters.minConfidence).toBe(0)
    })
  })

  describe('Sorting', () => {
    it('sorts predictions by rating by default', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.vm.sortField).toBe('rating')
      expect(wrapper.vm.sortOrder).toBe(-1) // Descending
    })

    it('handles sort events', async () => {
      const wrapper = createWrapper()
      
      const sortEvent = {
        sortField: 'confidenceScore',
        sortOrder: 1
      }
      
      await wrapper.vm.onSort(sortEvent)
      
      expect(wrapper.vm.sortField).toBe('confidenceScore')
      expect(wrapper.vm.sortOrder).toBe(1)
    })
  })

  describe('Actions', () => {
    it('emits export-results when export button is clicked', async () => {
      const wrapper = createWrapper()
      
      const exportButton = wrapper.find('button[aria-label*="Export"]')
      await exportButton.trigger('click')
      
      // Should show export menu
      expect(wrapper.find('[data-pc-name="menu"]').exists()).toBe(true)
    })

    it('emits refresh-results when refresh button is clicked', async () => {
      const wrapper = createWrapper()
      
      const refreshButton = wrapper.find('button[aria-label*="Refresh"]')
      await refreshButton.trigger('click')
      
      expect(wrapper.emitted('refresh-results')).toBeTruthy()
    })

    it('opens prediction detail dialog when view button is clicked', async () => {
      const wrapper = createWrapper()
      
      const viewButton = wrapper.find('button[aria-label*="View Details"]')
      await viewButton.trigger('click')
      
      expect(wrapper.vm.showDetailDialog).toBe(true)
      expect(wrapper.vm.selectedPrediction).toBeTruthy()
    })

    it('copies prediction text when copy button is clicked', async () => {
      const wrapper = createWrapper()
      
      const copyButton = wrapper.find('button[aria-label*="Copy"]')
      await copyButton.trigger('click')
      
      expect(navigator.clipboard.writeText).toHaveBeenCalledWith(
        mockPredictions[0].predictionText
      )
    })

    it('handles share functionality', async () => {
      const wrapper = createWrapper()
      
      const shareButton = wrapper.find('button[aria-label*="Share"]')
      await shareButton.trigger('click')
      
      expect(navigator.share).toHaveBeenCalled()
    })
  })

  describe('Export Menu', () => {
    it('shows export menu with correct options', async () => {
      const wrapper = createWrapper()
      
      expect(wrapper.vm.exportMenuItems).toHaveLength(3)
      expect(wrapper.vm.exportMenuItems[0].label).toBe('Export as CSV')
      expect(wrapper.vm.exportMenuItems[1].label).toBe('Export as JSON')
      expect(wrapper.vm.exportMenuItems[2].label).toBe('Export as PDF Report')
    })

    it('emits correct export format when menu item is selected', () => {
      const wrapper = createWrapper()
      
      // Simulate CSV export
      wrapper.vm.exportMenuItems[0].command()
      expect(wrapper.emitted('export-results')).toEqual([['csv']])
      
      // Simulate JSON export
      wrapper.vm.exportMenuItems[1].command()
      expect(wrapper.emitted('export-results')).toEqual([['csv'], ['json']])
    })
  })

  describe('Prediction Detail Dialog', () => {
    it('shows detailed prediction information in dialog', async () => {
      const wrapper = createWrapper()
      
      await wrapper.vm.viewPredictionDetail(mockPredictions[0])
      await nextTick()
      
      expect(wrapper.vm.showDetailDialog).toBe(true)
      expect(wrapper.vm.selectedPrediction).toBe(mockPredictions[0])
      
      const dialog = wrapper.find('[data-pc-name="dialog"]')
      expect(dialog.exists()).toBe(true)
    })

    it('emits prediction-selected when prediction is viewed', async () => {
      const wrapper = createWrapper()
      
      await wrapper.vm.viewPredictionDetail(mockPredictions[0])
      
      expect(wrapper.emitted('prediction-selected')).toEqual([[mockPredictions[0]]])
    })
  })

  describe('Utility Functions', () => {
    it('truncates long text correctly', () => {
      const wrapper = createWrapper()
      
      const longText = 'This is a very long text that should be truncated'
      const truncated = wrapper.vm.truncateText(longText, 20)
      
      expect(truncated).toBe('This is a very long ...')
    })

    it('generates author initials correctly', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.vm.getAuthorInitials('John Doe')).toBe('JD')
      expect(wrapper.vm.getAuthorInitials('Jane')).toBe('J')
      expect(wrapper.vm.getAuthorInitials('')).toBe('?')
    })

    it('formats dates correctly', () => {
      const wrapper = createWrapper()
      
      const dateString = '2024-01-15T10:00:00Z'
      const formatted = wrapper.vm.formatDate(dateString)
      
      expect(formatted).toMatch(/\d{1,2}\/\d{1,2}\/\d{4}/)
    })

    it('applies correct confidence classes', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.vm.getConfidenceClass(0.9)).toBe('confidence-high')
      expect(wrapper.vm.getConfidenceClass(0.7)).toBe('confidence-medium')
      expect(wrapper.vm.getConfidenceClass(0.4)).toBe('confidence-low')
    })
  })

  describe('Responsive Design', () => {
    it('applies responsive classes to grid columns', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.find('.col-12').exists()).toBe(true)
      expect(wrapper.find('.md\\:col-3').exists()).toBe(true)
    })

    it('handles mobile layout for table', () => {
      const wrapper = createWrapper()
      
      const table = wrapper.find('[data-pc-name="datatable"]')
      expect(table.attributes('responsivelayout')).toBe('scroll')
    })
  })

  describe('Accessibility', () => {
    it('has proper ARIA labels for interactive elements', () => {
      const wrapper = createWrapper()
      
      const buttons = wrapper.findAll('button')
      buttons.forEach(button => {
        expect(button.attributes()).toHaveProperty('aria-label')
      })
    })

    it('has semantic table structure', () => {
      const wrapper = createWrapper()
      
      expect(wrapper.find('table').exists()).toBe(true)
      expect(wrapper.find('thead').exists()).toBe(true)
      expect(wrapper.find('tbody').exists()).toBe(true)
    })
  })

  describe('Performance', () => {
    it('handles large datasets efficiently', () => {
      const largePredictionSet = Array.from({ length: 1000 }, (_, i) => ({
        ...mockPredictions[0],
        id: `pred-${i}`,
        predictionText: `Prediction ${i}`
      }))
      
      expect(() => createWrapper(largePredictionSet)).not.toThrow()
    })

    it('uses pagination for large result sets', () => {
      const wrapper = createWrapper()
      
      const table = wrapper.find('[data-pc-name="datatable"]')
      expect(table.attributes('paginator')).toBe('true')
      expect(table.attributes('rows')).toBe('10')
    })
  })

  describe('Error Handling', () => {
    it('handles missing prediction data gracefully', () => {
      const incompletePredictions = [
        {
          id: 'pred-incomplete',
          predictionText: 'Incomplete prediction',
          rating: 3,
          confidenceScore: 0.5,
          article: mockArticle,
          extractedAt: '2024-01-15T10:00:00Z'
        }
      ]
      
      expect(() => createWrapper(incompletePredictions)).not.toThrow()
    })

    it('handles clipboard API failures gracefully', async () => {
      navigator.clipboard.writeText = vi.fn(() => Promise.reject(new Error('Clipboard error')))
      
      const wrapper = createWrapper()
      
      // Should not throw error when copy fails
      await expect(wrapper.vm.copyPrediction(mockPredictions[0])).resolves.toBeUndefined()
    })
  })
})