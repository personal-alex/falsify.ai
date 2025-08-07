import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import PrimeVue from 'primevue/config'
import ArticleFilter, { type ArticleFilters, type Author } from '../ArticleFilter.vue'

// Mock lodash debounce
vi.mock('lodash-es', () => ({
  debounce: vi.fn((fn) => fn)
}))

const mockAuthors: Author[] = [
  { id: 1, name: 'John Doe' },
  { id: 2, name: 'Jane Smith' },
  { id: 3, name: 'Bob Johnson' }
]

const defaultFilters: ArticleFilters = {
  authorId: null,
  titleSearch: '',
  dateRange: null,
  crawlerSource: null
}

describe('ArticleFilter', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(ArticleFilter, {
      props: {
        filters: { ...defaultFilters },
        authors: mockAuthors
      },
      global: {
        plugins: [PrimeVue],
        stubs: {
          Panel: {
            template: '<div class="mock-panel"><div class="panel-header"><slot name="header" /></div><div class="panel-content"><slot /></div></div>'
          },
          Dropdown: {
            template: '<select><option v-for="option in options" :key="option.id" :value="option.id">{{ option.name }}</option></select>',
            props: ['options', 'modelValue', 'optionLabel', 'optionValue'],
            emits: ['change']
          },
          InputText: {
            template: '<input type="text" :value="modelValue" @input="$emit(\'input\', $event)" />',
            props: ['modelValue'],
            emits: ['input']
          },
          InputGroup: {
            template: '<div class="input-group"><slot /></div>'
          },
          InputGroupAddon: {
            template: '<div class="input-group-addon"><slot /></div>'
          },
          Calendar: {
            template: '<input type="date" :value="modelValue" @change="$emit(\'date-select\', $event)" />',
            props: ['modelValue'],
            emits: ['date-select']
          },
          Button: {
            template: '<button @click="$emit(\'click\')"><slot /></button>',
            emits: ['click']
          },
          Tag: {
            template: '<span class="tag" @click="$emit(\'click\')"><slot /></span>',
            emits: ['click']
          },
          Badge: {
            template: '<span class="badge">{{ value }}</span>',
            props: ['value']
          }
        }
      }
    })
  })

  it('renders correctly', () => {
    expect(wrapper.find('.panel-header').text()).toContain('Filter Articles')
    expect(wrapper.find('select').exists()).toBe(true)
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('input[type="date"]').exists()).toBe(true)
  })

  it('displays source options correctly', () => {
    const sourceOptions = wrapper.vm.sourceOptions
    expect(sourceOptions).toHaveLength(2)
    expect(sourceOptions[0]).toEqual({ label: 'Drucker', value: 'drucker' })
    expect(sourceOptions[1]).toEqual({ label: 'Caspit', value: 'caspit' })
  })

  it('syncs local filters with prop changes', async () => {
    const newFilters: ArticleFilters = {
      authorId: 2,
      titleSearch: 'updated search',
      dateRange: null,
      crawlerSource: 'caspit'
    }

    await wrapper.setProps({ filters: newFilters })
    await nextTick()

    // Check that local state is updated
    expect(wrapper.vm.localFilters.authorId).toBe(2)
    expect(wrapper.vm.localFilters.titleSearch).toBe('updated search')
    expect(wrapper.vm.localFilters.crawlerSource).toBe('caspit')
  })

  it('emits filter-changed when emitFilterChange is called', () => {
    wrapper.vm.emitFilterChange()
    
    expect(wrapper.emitted('filter-changed')).toBeTruthy()
    const emittedFilters = wrapper.emitted('filter-changed')[0][0] as ArticleFilters
    expect(emittedFilters).toEqual(defaultFilters)
  })

  it('clears all filters when clearAllFilters is called', () => {
    // Set some initial filter values
    wrapper.vm.localFilters = {
      authorId: 1,
      titleSearch: 'test',
      dateRange: [new Date(), new Date()],
      crawlerSource: 'drucker'
    }

    wrapper.vm.clearAllFilters()
    
    expect(wrapper.emitted('filter-changed')).toBeTruthy()
    const emittedFilters = wrapper.emitted('filter-changed')[0][0] as ArticleFilters
    expect(emittedFilters).toEqual(defaultFilters)
  })

  it('handles empty authors list', async () => {
    await wrapper.setProps({ authors: [] })
    
    expect(wrapper.props('authors')).toEqual([])
  })

  it('has proper accessibility attributes', () => {
    const authorLabel = wrapper.find('label[for="author-filter"]')
    const titleLabel = wrapper.find('label[for="title-search"]')
    const dateLabel = wrapper.find('label[for="date-range"]')
    const sourceLabel = wrapper.find('label[for="crawler-source"]')

    expect(authorLabel.exists()).toBe(true)
    expect(titleLabel.exists()).toBe(true)
    expect(dateLabel.exists()).toBe(true)
    expect(sourceLabel.exists()).toBe(true)
  })

  it('debounces text input changes', () => {
    const debouncedEmitChange = wrapper.vm.debouncedEmitChange
    expect(typeof debouncedEmitChange).toBe('function')
  })

  it('updates local filters when props change', async () => {
    const initialFilters = wrapper.vm.localFilters
    expect(initialFilters).toEqual(defaultFilters)

    const newFilters: ArticleFilters = {
      authorId: 1,
      titleSearch: 'test search',
      dateRange: [new Date('2024-01-01'), new Date('2024-01-31')],
      crawlerSource: 'drucker'
    }

    await wrapper.setProps({ filters: newFilters })
    await nextTick()

    expect(wrapper.vm.localFilters).toEqual(newFilters)
  })
})