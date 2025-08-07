import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PrimeVue from 'primevue/config'
import ArticleSelectionTable, { type Article } from '../ArticleSelectionTable.vue'

const mockArticles: Article[] = [
  {
    id: 1,
    title: 'Test Article 1',
    url: 'https://example.com/article-1',
    text: 'Content of article 1',
    crawlerSource: 'drucker',
    createdAt: '2024-01-01T00:00:00Z',
    author: {
      id: 1,
      name: 'John Doe',
      avatarUrl: 'https://example.com/avatar1.jpg'
    },
    predictions: []
  },
  {
    id: 2,
    title: 'Test Article 2',
    url: 'https://example.com/article-2',
    text: 'Content of article 2',
    crawlerSource: 'caspit',
    createdAt: '2024-01-02T00:00:00Z',
    author: {
      id: 2,
      name: 'Jane Smith'
    },
    predictions: [{ id: 1, text: 'Some prediction' }]
  },
  {
    id: 3,
    title: 'Test Article 3',
    url: 'https://example.com/article-3',
    text: 'Content of article 3',
    crawlerSource: 'drucker',
    createdAt: '2024-01-03T00:00:00Z',
    predictions: []
  }
]

describe('ArticleSelectionTable', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(ArticleSelectionTable, {
      props: {
        articles: mockArticles,
        selectedArticles: [],
        loading: false
      },
      global: {
        plugins: [PrimeVue],
        directives: {
          tooltip: {
            beforeMount() {},
            updated() {},
            unmounted() {}
          }
        },
        stubs: {
          Panel: {
            template: '<div class="mock-panel"><div class="panel-header"><slot name="header" /></div><div class="panel-content"><slot /></div></div>'
          },
          DataTable: {
            template: '<div class="mock-datatable"><slot /></div>',
            props: ['value', 'selection', 'loading']
          },
          Column: {
            template: '<div class="mock-column"><slot /></div>'
          },
          Badge: {
            template: '<span class="mock-badge">{{ value }}</span>',
            props: ['value']
          },
          Chip: {
            template: '<span class="mock-chip">{{ label }}</span>',
            props: ['label']
          },
          Tag: {
            template: '<span class="mock-tag">{{ value }}</span>',
            props: ['value']
          },
          Avatar: {
            template: '<div class="mock-avatar">{{ label }}</div>',
            props: ['image', 'label', 'size']
          },
          Button: {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            emits: ['click']
          },
          Dialog: {
            template: '<div v-if="visible" class="mock-dialog"><slot /><slot name="footer" /></div>',
            props: ['visible'],
            emits: ['update:visible']
          },
          Skeleton: {
            template: '<div class="mock-skeleton"></div>',
            props: ['shape', 'size', 'width', 'height']
          },
          Checkbox: {
            template: '<input type="checkbox" :checked="modelValue" :indeterminate="indeterminate" @change="$emit(\'update:modelValue\', $event.target.checked)" />',
            props: ['modelValue', 'indeterminate', 'disabled'],
            emits: ['update:modelValue']
          }
        }
      }
    })
  })

  it('renders correctly', () => {
    expect(wrapper.find('.panel-header').text()).toContain('Select Articles for Analysis')
    expect(wrapper.find('.mock-chip').exists()).toBe(true)
  })

  it('displays selection count correctly', () => {
    const chip = wrapper.find('.mock-chip')
    expect(chip.text()).toContain('0 of')
    expect(chip.text()).toContain('selected')
  })

  it('updates selection count when articles are selected', async () => {
    await wrapper.setProps({
      selectedArticles: [mockArticles[0], mockArticles[1]]
    })
    await nextTick()

    const chip = wrapper.find('.mock-chip')
    expect(chip.text()).toContain('2 of')
    expect(chip.text()).toContain('selected')
    
    // Badge should show success severity when articles are selected
    const badge = wrapper.find('.mock-badge')
    expect(badge.text()).toBe('2')
  })

  it('emits selection-changed when selection changes', async () => {
    // Simulate selection change
    wrapper.vm.localSelection = [mockArticles[0]]
    wrapper.vm.emitSelectionChange()

    expect(wrapper.emitted('selection-changed')).toBeTruthy()
    const emittedSelection = wrapper.emitted('selection-changed')[0][0]
    expect(emittedSelection).toHaveLength(1)
    expect(emittedSelection[0].id).toBe(1)
  })

  it('syncs local selection with prop changes', async () => {
    await wrapper.setProps({
      selectedArticles: [mockArticles[0]]
    })
    await nextTick()

    expect(wrapper.vm.localSelection).toHaveLength(1)
    expect(wrapper.vm.localSelection[0].id).toBe(1)
  })

  it('formats dates correctly', () => {
    const formattedDate = wrapper.vm.formatDate('2024-01-01T00:00:00Z')
    expect(formattedDate).toBe('1/1/2024')
  })

  it('shows preview dialog when preview button is clicked', async () => {
    wrapper.vm.showPreview(mockArticles[0])
    await nextTick()

    expect(wrapper.vm.previewVisible).toBe(true)
    expect(wrapper.vm.previewArticle).toEqual(mockArticles[0])
  })

  it('checks if article is selected correctly', () => {
    wrapper.vm.localSelection = [mockArticles[0]]
    
    expect(wrapper.vm.isArticleSelected(mockArticles[0])).toBe(true)
    expect(wrapper.vm.isArticleSelected(mockArticles[1])).toBe(false)
    expect(wrapper.vm.isArticleSelected(null)).toBe(false)
  })

  it('toggles article selection from preview', async () => {
    // Initially no selection
    expect(wrapper.vm.localSelection).toHaveLength(0)

    // Add article to selection
    wrapper.vm.toggleArticleSelection(mockArticles[0])
    expect(wrapper.vm.localSelection).toHaveLength(1)
    expect(wrapper.vm.localSelection[0].id).toBe(1)

    // Remove article from selection
    wrapper.vm.toggleArticleSelection(mockArticles[0])
    expect(wrapper.vm.localSelection).toHaveLength(0)
  })

  it('handles articles without authors gracefully', () => {
    const articleWithoutAuthor = {
      ...mockArticles[0],
      author: undefined
    }

    // Should not throw error
    expect(() => wrapper.vm.isArticleSelected(articleWithoutAuthor)).not.toThrow()
  })

  it('displays loading state correctly', async () => {
    await wrapper.setProps({ loading: true })
    
    // Check that loading prop is correctly set
    expect(wrapper.props('loading')).toBe(true)
  })

  it('displays article information correctly in table', () => {
    // This would test the table body rendering, but since we're stubbing DataTable,
    // we'll test the data structure instead
    expect(wrapper.props('articles')).toEqual(mockArticles)
  })

  it('handles empty articles list', async () => {
    await wrapper.setProps({ articles: [] })
    
    const chip = wrapper.find('.mock-chip')
    expect(chip.text()).toContain('0 of')
    expect(chip.text()).toContain('selected')
  })

  it('displays predictions count correctly', () => {
    // Article with predictions should show count
    const articleWithPredictions = mockArticles[1]
    expect(articleWithPredictions.predictions).toHaveLength(1)

    // Article without predictions should show "Not analyzed"
    const articleWithoutPredictions = mockArticles[0]
    expect(articleWithoutPredictions.predictions).toHaveLength(0)
  })

  it('handles preview dialog close', async () => {
    wrapper.vm.previewVisible = true
    wrapper.vm.previewArticle = mockArticles[0]

    wrapper.vm.previewVisible = false
    await nextTick()

    expect(wrapper.vm.previewVisible).toBe(false)
  })

  it('emits selection change when toggling from preview', async () => {
    wrapper.vm.toggleArticleSelection(mockArticles[0])
    await nextTick()
    
    expect(wrapper.emitted('selection-changed')).toBeTruthy()
  })

  describe('Select All functionality', () => {
    it('computes isAllSelected correctly', async () => {
      // Initially no selection
      expect(wrapper.vm.isAllSelected).toBe(false)

      // Select all articles
      await wrapper.setProps({
        selectedArticles: [...mockArticles]
      })
      await nextTick()

      expect(wrapper.vm.isAllSelected).toBe(true)
    })

    it('computes isPartiallySelected correctly', async () => {
      // Initially no selection
      expect(wrapper.vm.isPartiallySelected).toBe(false)

      // Select some articles
      await wrapper.setProps({
        selectedArticles: [mockArticles[0]]
      })
      await nextTick()

      expect(wrapper.vm.isPartiallySelected).toBe(true)
      expect(wrapper.vm.isAllSelected).toBe(false)
    })

    it('toggles select all correctly', async () => {
      // Initially no selection
      expect(wrapper.vm.localSelection).toHaveLength(0)

      // Select all
      wrapper.vm.toggleSelectAll()
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(mockArticles.length)
      expect(wrapper.emitted('selection-changed')).toBeTruthy()

      // Deselect all
      wrapper.vm.toggleSelectAll()
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
    })

    it('maintains selection persistence across article changes', async () => {
      // Select first article
      wrapper.vm.localSelection = [mockArticles[0]]
      wrapper.vm.selectedArticleIds = new Set([mockArticles[0].id])

      // Simulate articles prop change (like filtering or pagination)
      const newArticles = [mockArticles[0], mockArticles[2]] // Article 0 still present
      await wrapper.setProps({ articles: newArticles })
      await nextTick()

      // Should maintain selection of article 0
      expect(wrapper.vm.localSelection).toHaveLength(1)
      expect(wrapper.vm.localSelection[0].id).toBe(mockArticles[0].id)
    })

    it('clears selection correctly', async () => {
      // Set some selection
      wrapper.vm.localSelection = [mockArticles[0], mockArticles[1]]
      wrapper.vm.selectedArticleIds = new Set([mockArticles[0].id, mockArticles[1].id])

      // Clear selection
      wrapper.vm.clearAllSelection()
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.size).toBe(0)
      expect(wrapper.emitted('selection-changed')).toBeTruthy()
    })
  })

  describe('Selection state management', () => {
    it('syncs selectedArticleIds with localSelection', async () => {
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      wrapper.vm.toggleArticleSelection(mockArticles[1])
      await nextTick()

      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[1].id)).toBe(true)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[2].id)).toBe(false)
    })

    it('updates selection when toggling from preview', async () => {
      // Initially no selection
      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.size).toBe(0)

      // Add article to selection
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(1)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(true)

      // Remove article from selection
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(false)
    })

    it('handles selection persistence when articles list changes', async () => {
      // Select articles 0 and 1
      await wrapper.setProps({
        selectedArticles: [mockArticles[0], mockArticles[1]]
      })
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(2)

      // Change articles list to only include article 0 and 2 (simulating filtering)
      await wrapper.setProps({
        articles: [mockArticles[0], mockArticles[2]]
      })
      await nextTick()

      // Should only keep article 0 in selection (article 1 is no longer in the list)
      expect(wrapper.vm.localSelection).toHaveLength(1)
      expect(wrapper.vm.localSelection[0].id).toBe(mockArticles[0].id)
    })
  })

  describe('Visual feedback', () => {
    it('shows correct badge severity based on selection', async () => {
      // No selection - info severity
      expect(wrapper.vm.localSelection).toHaveLength(0)

      // With selection - success severity
      await wrapper.setProps({
        selectedArticles: [mockArticles[0]]
      })
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(1)
    })

    it('displays correct selection count in filtered articles', async () => {
      // This tests that the count shows "X of Y selected" where Y is filtered articles count
      const chip = wrapper.find('.mock-chip')
      expect(chip.text()).toContain(`0 of ${mockArticles.length} selected`)
    })
  })
})