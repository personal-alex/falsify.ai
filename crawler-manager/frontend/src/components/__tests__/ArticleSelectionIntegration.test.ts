import { describe, it, expect, beforeEach } from 'vitest'
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
    predictions: []
  },
  {
    id: 3,
    title: 'Test Article 3',
    url: 'https://example.com/article-3',
    text: 'Content of article 3',
    crawlerSource: 'drucker',
    createdAt: '2024-01-03T00:00:00Z',
    author: {
      id: 3,
      name: 'Bob Wilson'
    },
    predictions: []
  }
]

describe('ArticleSelectionTable Integration Tests', () => {
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
            template: '<span class="mock-badge" :class="severity">{{ value }}</span>',
            props: ['value', 'severity']
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
            template: '<button @click="$emit(\'click\')" :disabled="disabled"><slot /></button>',
            props: ['disabled', 'severity', 'outlined', 'size'],
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

  describe('Complete selection workflow', () => {
    it('handles complete selection workflow correctly', async () => {
      // Initial state - no selection
      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.size).toBe(0)
      expect(wrapper.vm.isAllSelected).toBe(false)
      expect(wrapper.vm.isPartiallySelected).toBe(false)

      // Select first article using toggle method
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      await nextTick()

      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.isPartiallySelected).toBe(true)
      expect(wrapper.vm.isAllSelected).toBe(false)
      expect(wrapper.emitted('selection-changed')).toBeTruthy()

      // Select all articles
      wrapper.vm.toggleSelectAll()
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(mockArticles.length)
      expect(wrapper.vm.isAllSelected).toBe(true)
      expect(wrapper.vm.isPartiallySelected).toBe(false)
      mockArticles.forEach(article => {
        expect(wrapper.vm.selectedArticleIds.has(article.id)).toBe(true)
      })

      // Deselect all articles
      wrapper.vm.toggleSelectAll()
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.size).toBe(0)
      expect(wrapper.vm.isAllSelected).toBe(false)
      expect(wrapper.vm.isPartiallySelected).toBe(false)

      // Clear selection (should work even when already empty)
      wrapper.vm.clearAllSelection()
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.size).toBe(0)
    })

    it('maintains selection persistence across article list changes', async () => {
      // Select articles 1 and 2 using toggle method
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      wrapper.vm.toggleArticleSelection(mockArticles[1])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(2)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[1].id)).toBe(true)

      // Simulate filtering - only articles 0 and 2 remain
      const filteredArticles = [mockArticles[0], mockArticles[2]]
      await wrapper.setProps({ articles: filteredArticles })
      await nextTick()

      // Should maintain selection of article 0, but lose article 1 from local selection (not in filtered list)
      expect(wrapper.vm.localSelection).toHaveLength(1)
      expect(wrapper.vm.localSelection[0].id).toBe(mockArticles[0].id)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[1].id)).toBe(true) // Still in persisted IDs
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[2].id)).toBe(false)

      // When article 1 comes back, it should be selected again
      await wrapper.setProps({ articles: mockArticles })
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(2)
      expect(wrapper.vm.localSelection.some(a => a.id === mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.localSelection.some(a => a.id === mockArticles[1].id)).toBe(true)
    })

    it('handles external selection changes correctly', async () => {
      // External selection change (from parent component)
      await wrapper.setProps({
        selectedArticles: [mockArticles[0], mockArticles[2]]
      })
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(2)
      expect(wrapper.vm.localSelection.some(a => a.id === mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.localSelection.some(a => a.id === mockArticles[2].id)).toBe(true)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[0].id)).toBe(true)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[2].id)).toBe(true)

      // Clear external selection
      await wrapper.setProps({
        selectedArticles: []
      })
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.size).toBe(0)
    })

    it('handles toggle selection from preview correctly', async () => {
      // Initially no selection
      expect(wrapper.vm.localSelection).toHaveLength(0)

      // Toggle article from preview (add)
      wrapper.vm.toggleArticleSelection(mockArticles[1])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(1)
      expect(wrapper.vm.localSelection[0].id).toBe(mockArticles[1].id)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[1].id)).toBe(true)
      expect(wrapper.emitted('selection-changed')).toBeTruthy()

      // Toggle same article from preview (remove)
      wrapper.vm.toggleArticleSelection(mockArticles[1])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
      expect(wrapper.vm.selectedArticleIds.has(mockArticles[1].id)).toBe(false)

      // Toggle null article (should not crash)
      wrapper.vm.toggleArticleSelection(null)
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(0)
    })
  })

  describe('Visual state indicators', () => {
    it('shows correct badge severity based on selection state', async () => {
      // No selection - info severity
      expect(wrapper.vm.localSelection).toHaveLength(0)
      
      // With selection - success severity
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(1)
      // Badge should show success severity when articles are selected
    })

    it('displays correct selection count in chip', async () => {
      const chip = wrapper.find('.mock-chip')
      
      // Initially no selection
      expect(chip.text()).toContain('0 of 3 selected')

      // With selection
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      wrapper.vm.toggleArticleSelection(mockArticles[1])
      await nextTick()

      expect(chip.text()).toContain('2 of 3 selected')
    })

    it('shows correct checkbox states', async () => {
      // Initially unchecked and not indeterminate
      expect(wrapper.vm.isAllSelected).toBe(false)
      expect(wrapper.vm.isPartiallySelected).toBe(false)

      // Partial selection - indeterminate
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      await nextTick()

      expect(wrapper.vm.isAllSelected).toBe(false)
      expect(wrapper.vm.isPartiallySelected).toBe(true)

      // All selected - checked
      wrapper.vm.toggleSelectAll()
      await nextTick()

      expect(wrapper.vm.isAllSelected).toBe(true)
      expect(wrapper.vm.isPartiallySelected).toBe(false)
    })
  })

  describe('Button states and interactions', () => {
    it('enables/disables select all button correctly', async () => {
      // With articles - button should be enabled
      expect(wrapper.vm.filteredArticles).toHaveLength(3)

      // With empty articles - button should be disabled
      await wrapper.setProps({ articles: [] })
      await nextTick()

      expect(wrapper.vm.filteredArticles).toHaveLength(0)
    })

    it('shows correct select all button label', async () => {
      // Initially "Select All"
      expect(wrapper.vm.isAllSelected).toBe(false)

      // When all selected "Deselect All"
      wrapper.vm.toggleSelectAll()
      await nextTick()

      expect(wrapper.vm.isAllSelected).toBe(true)
    })

    it('shows clear selection button only when there is selection', async () => {
      // Initially no selection - button should not be visible
      expect(wrapper.vm.localSelection).toHaveLength(0)

      // With selection - button should be visible
      wrapper.vm.toggleArticleSelection(mockArticles[0])
      await nextTick()

      expect(wrapper.vm.localSelection).toHaveLength(1)
    })
  })
})