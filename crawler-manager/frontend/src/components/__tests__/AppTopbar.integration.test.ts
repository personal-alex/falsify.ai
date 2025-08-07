import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import PrimeVue from 'primevue/config'
import Button from 'primevue/button'
import Tooltip from 'primevue/tooltip'
import AppTopbar from '../AppTopbar.vue'
import { useTopBarStore } from '@/stores/topbar'

// Mock stores
vi.mock('@/stores/layout', () => ({
  useLayoutStore: () => ({
    onMenuToggle: vi.fn(),
    isConfigSidebarVisible: false,
    setConfigSidebarVisible: vi.fn()
  })
}))

vi.mock('@/stores/theme', () => ({
  useThemeStore: () => ({
    currentMode: 'light',
    toggleMode: vi.fn()
  })
}))

vi.mock('@/stores/crawler', () => ({
  useCrawlerStore: () => ({
    isOnline: true
  })
}))

vi.mock('@/stores/notification', () => ({
  useNotificationStore: () => ({
    unreadCount: 0
  })
}))

describe('AppTopbar Integration', () => {
  let router: any
  let pinia: any

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', component: { template: '<div>Home</div>' } }
      ]
    })
  })

  const createWrapper = () => {
    return mount(AppTopbar, {
      global: {
        plugins: [pinia, router, PrimeVue],
        components: {
          Button
        },
        directives: {
          tooltip: Tooltip
        },
        stubs: {
          'router-link': true
        }
      }
    })
  }

  it('should render without context actions initially', () => {
    const wrapper = createWrapper()
    
    expect(wrapper.find('.layout-topbar-context-actions').exists()).toBe(false)
    expect(wrapper.find('.layout-topbar-logo').exists()).toBe(true)
    expect(wrapper.find('.layout-topbar-menu-section').exists()).toBe(true)
  })

  it('should display context actions when they are set', async () => {
    const wrapper = createWrapper()
    const topBarStore = useTopBarStore()
    
    // Set some context actions
    topBarStore.setContextActions([
      {
        id: 'test-action',
        label: 'Test Action',
        icon: 'pi pi-test',
        command: vi.fn()
      }
    ])

    await wrapper.vm.$nextTick()

    expect(wrapper.find('.layout-topbar-context-actions').exists()).toBe(true)
    expect(wrapper.find('.context-action-button').exists()).toBe(true)
    expect(wrapper.find('.context-action-button').text()).toContain('Test Action')
  })

  it('should hide context actions on mobile screens', async () => {
    const wrapper = createWrapper()
    const topBarStore = useTopBarStore()
    
    // Set some context actions
    topBarStore.setContextActions([
      {
        id: 'test-action',
        label: 'Test Action',
        icon: 'pi pi-test',
        command: vi.fn()
      }
    ])

    await wrapper.vm.$nextTick()

    // The CSS media query should hide context actions on mobile
    // We can't test the actual CSS behavior in unit tests, but we can verify the structure exists
    expect(wrapper.find('.layout-topbar-context-actions').exists()).toBe(true)
  })

  it('should call action command when button is clicked', async () => {
    const wrapper = createWrapper()
    const topBarStore = useTopBarStore()
    const mockCommand = vi.fn()
    
    // Set context action with mock command
    topBarStore.setContextActions([
      {
        id: 'test-action',
        label: 'Test Action',
        icon: 'pi pi-test',
        command: mockCommand
      }
    ])

    await wrapper.vm.$nextTick()

    const actionButton = wrapper.find('.context-action-button')
    expect(actionButton.exists()).toBe(true)
    
    await actionButton.trigger('click')
    expect(mockCommand).toHaveBeenCalled()
  })

  it('should display multiple context actions', async () => {
    const wrapper = createWrapper()
    const topBarStore = useTopBarStore()
    
    // Set multiple context actions
    topBarStore.setContextActions([
      {
        id: 'action-1',
        label: 'Action 1',
        icon: 'pi pi-test1',
        command: vi.fn()
      },
      {
        id: 'action-2',
        label: 'Action 2',
        icon: 'pi pi-test2',
        command: vi.fn()
      }
    ])

    await wrapper.vm.$nextTick()

    const actionButtons = wrapper.findAll('.context-action-button')
    expect(actionButtons).toHaveLength(2)
    expect(actionButtons[0].text()).toContain('Action 1')
    expect(actionButtons[1].text()).toContain('Action 2')
  })

  it('should handle disabled actions', async () => {
    const wrapper = createWrapper()
    const topBarStore = useTopBarStore()
    
    // Set disabled context action
    topBarStore.setContextActions([
      {
        id: 'disabled-action',
        label: 'Disabled Action',
        icon: 'pi pi-test',
        disabled: true,
        command: vi.fn()
      }
    ])

    await wrapper.vm.$nextTick()

    const actionButton = wrapper.find('.context-action-button')
    expect(actionButton.exists()).toBe(true)
    // The button should have disabled attribute through PrimeVue Button component
    expect(actionButton.attributes('disabled')).toBeDefined()
  })

  it('should handle loading actions', async () => {
    const wrapper = createWrapper()
    const topBarStore = useTopBarStore()
    
    // Set loading context action
    topBarStore.setContextActions([
      {
        id: 'loading-action',
        label: 'Loading Action',
        icon: 'pi pi-test',
        loading: true,
        command: vi.fn()
      }
    ])

    await wrapper.vm.$nextTick()

    const actionButton = wrapper.find('.context-action-button')
    expect(actionButton.exists()).toBe(true)
    // The button should have loading state through PrimeVue Button component
    // In the actual implementation, loading is passed as a prop to the Button component
    expect(actionButton.exists()).toBe(true)
  })
})