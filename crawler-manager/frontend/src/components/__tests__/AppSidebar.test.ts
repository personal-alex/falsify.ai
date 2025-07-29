import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import AppSidebar from '../AppSidebar.vue'
import { useLayoutStore } from '@/stores/layout'
import { useCrawlerStore } from '@/stores/crawler'
import { useNotificationStore } from '@/stores/notification'

// Mock PrimeVue components
vi.mock('primevue/button', () => ({
  default: {
    name: 'Button',
    template: '<button><slot /></button>'
  }
}))

// Mock the stores
vi.mock('@/stores/layout')
vi.mock('@/stores/crawler')
vi.mock('@/stores/notification')

// Mock router
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: { template: '<div>Home</div>' } },
    { path: '/crawlers', component: { template: '<div>Crawlers</div>' } }
  ]
})

describe('AppSidebar', () => {
  let pinia: any
  let layoutStore: any
  let crawlerStore: any
  let notificationStore: any

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)

    // Mock store implementations
    layoutStore = {
      isMobile: false,
      isOverlayMenuActive: false,
      isStaticMenuInactive: false,
      setOverlayMenuActive: vi.fn()
    }

    crawlerStore = {
      totalCrawlers: 3,
      activeCrawlers: 1,
      unhealthyCrawlers: 0,
      unknownCrawlers: 0,
      isOnline: true,
      loadCrawlerData: vi.fn().mockResolvedValue([]),
      initializeWebSocket: vi.fn()
    }

    notificationStore = {
      unreadCount: 2,
      notificationsByType: {
        error: [],
        warn: [{ id: '1' }],
        info: [],
        success: []
      },
      initWebSocket: vi.fn()
    }

    // Mock the store composables
    vi.mocked(useLayoutStore).mockReturnValue(layoutStore)
    vi.mocked(useCrawlerStore).mockReturnValue(crawlerStore)
    vi.mocked(useNotificationStore).mockReturnValue(notificationStore)
  })

  const createWrapper = (props = {}) => {
    return mount(AppSidebar, {
      props,
      global: {
        plugins: [pinia, router],
        stubs: {
          AppMenu: {
            name: 'AppMenu',
            props: ['model'],
            emits: ['menuitem-click'],
            template: '<div class="app-menu"><slot /></div>'
          }
        }
      }
    })
  }

  it('renders sidebar structure correctly', () => {
    const wrapper = createWrapper()
    
    expect(wrapper.find('.layout-sidebar').exists()).toBe(true)
    expect(wrapper.find('.sidebar-header').exists()).toBe(true)
    expect(wrapper.find('.sidebar-logo').exists()).toBe(true)
    expect(wrapper.find('.sidebar-menu').exists()).toBe(true)
    expect(wrapper.find('.sidebar-footer').exists()).toBe(true)
    expect(wrapper.find('.connection-status').exists()).toBe(true)
  })

  it('displays correct logo and branding', () => {
    const wrapper = createWrapper()
    
    const logo = wrapper.find('.sidebar-logo')
    expect(logo.find('.sidebar-logo-icon').exists()).toBe(true)
    expect(logo.find('.sidebar-logo-text').text()).toBe('Crawler Manager')
  })

  it('shows connection status correctly', () => {
    const wrapper = createWrapper()
    
    const connectionStatus = wrapper.find('.connection-status')
    expect(connectionStatus.classes()).toContain('status-online')
    expect(connectionStatus.find('.connection-text').text()).toBe('Connected')
  })

  it('shows offline status when not connected', async () => {
    crawlerStore.isOnline = false
    const wrapper = createWrapper()
    
    const connectionStatus = wrapper.find('.connection-status')
    expect(connectionStatus.classes()).toContain('status-offline')
    expect(connectionStatus.find('.connection-text').text()).toBe('Offline')
  })

  it('applies correct CSS classes for desktop', () => {
    layoutStore.isMobile = false
    const wrapper = createWrapper()
    
    const sidebar = wrapper.find('.layout-sidebar')
    expect(sidebar.classes()).toContain('sidebar-desktop')
    expect(sidebar.classes()).not.toContain('sidebar-mobile')
  })

  it('applies correct CSS classes for mobile', () => {
    layoutStore.isMobile = true
    const wrapper = createWrapper()
    
    const sidebar = wrapper.find('.layout-sidebar')
    expect(sidebar.classes()).toContain('sidebar-mobile')
    expect(sidebar.classes()).not.toContain('sidebar-desktop')
  })

  it('shows mobile overlay when active', () => {
    layoutStore.isMobile = true
    layoutStore.isOverlayMenuActive = true
    const wrapper = createWrapper()
    
    const sidebar = wrapper.find('.layout-sidebar')
    expect(sidebar.classes()).toContain('sidebar-overlay-active')
    expect(wrapper.find('.sidebar-overlay').exists()).toBe(true)
  })

  it('shows close button on mobile', () => {
    layoutStore.isMobile = true
    const wrapper = createWrapper()
    
    expect(wrapper.find('.sidebar-close-btn').exists()).toBe(true)
  })

  it('hides close button on desktop', () => {
    layoutStore.isMobile = false
    const wrapper = createWrapper()
    
    expect(wrapper.find('.sidebar-close-btn').exists()).toBe(false)
  })

  it('applies collapsed class when menu is inactive', () => {
    layoutStore.isStaticMenuInactive = true
    layoutStore.isMobile = false
    const wrapper = createWrapper()
    
    const sidebar = wrapper.find('.layout-sidebar')
    expect(sidebar.classes()).toContain('sidebar-collapsed')
  })

  it('generates menu items with correct structure', () => {
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    
    expect(appMenu.exists()).toBe(true)
    expect(appMenu.props('model')).toBeDefined()
    
    const menuItems = appMenu.props('model')
    expect(Array.isArray(menuItems)).toBe(true)
    expect(menuItems.length).toBeGreaterThan(0)
    
    // Check for main sections
    const dashboardItem = menuItems.find((item: any) => item.label === 'Dashboard')
    expect(dashboardItem).toBeDefined()
    expect(dashboardItem.icon).toBe('pi pi-fw pi-home')
    expect(dashboardItem.to).toBe('/')
    
    const crawlerSection = menuItems.find((item: any) => item.label === 'Crawler Management')
    expect(crawlerSection).toBeDefined()
    expect(crawlerSection.items).toBeDefined()
    expect(Array.isArray(crawlerSection.items)).toBe(true)
  })

  it('includes real-time badges in menu items', () => {
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    const menuItems = appMenu.props('model')
    
    // Find crawler management section
    const crawlerSection = menuItems.find((item: any) => item.label === 'Crawler Management')
    expect(crawlerSection).toBeDefined()
    
    // Check overview item has total crawlers badge
    const overviewItem = crawlerSection.items.find((item: any) => item.label === 'Overview')
    expect(overviewItem.badge).toBe('3')
    expect(overviewItem.badgeClass).toBe('p-badge-info')
    
    // Check active crawls item has active crawlers badge
    const activeItem = crawlerSection.items.find((item: any) => item.label === 'Active Crawls')
    expect(activeItem.badge).toBe('1')
    expect(activeItem.badgeClass).toBe('p-badge-success')
  })

  it('shows notification badges correctly', () => {
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    const menuItems = appMenu.props('model')
    
    // Find monitoring section
    const monitoringSection = menuItems.find((item: any) => item.label === 'Monitoring & Analytics')
    expect(monitoringSection).toBeDefined()
    
    // Check alerts item has notification badge
    const alertsItem = monitoringSection.items.find((item: any) => item.label === 'Alerts & Notifications')
    expect(alertsItem.badge).toBe('2')
    expect(alertsItem.badgeClass).toBe('p-badge-warning') // Should be warning due to warn notifications
  })

  it('handles menu item clicks correctly', async () => {
    const routerPushSpy = vi.spyOn(router, 'push')
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    
    const mockItem = { to: '/crawlers', label: 'Test Item' }
    await appMenu.vm.$emit('menuitem-click', { originalEvent: new Event('click'), item: mockItem })
    
    // Should call router.push with the correct route
    expect(routerPushSpy).toHaveBeenCalledWith('/crawlers')
  })

  it('closes mobile menu after navigation', async () => {
    layoutStore.isMobile = true
    const routerPushSpy = vi.spyOn(router, 'push')
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    
    const mockItem = { to: '/crawlers', label: 'Test Item' }
    await appMenu.vm.$emit('menuitem-click', { originalEvent: new Event('click'), item: mockItem })
    
    expect(routerPushSpy).toHaveBeenCalledWith('/crawlers')
    expect(layoutStore.setOverlayMenuActive).toHaveBeenCalledWith(false)
  })

  it('handles close button click on mobile', async () => {
    layoutStore.isMobile = true
    const wrapper = createWrapper()
    
    const closeBtn = wrapper.find('.sidebar-close-btn')
    await closeBtn.trigger('click')
    
    expect(layoutStore.setOverlayMenuActive).toHaveBeenCalledWith(false)
  })

  it('handles overlay click to close mobile menu', async () => {
    layoutStore.isMobile = true
    layoutStore.isOverlayMenuActive = true
    const wrapper = createWrapper()
    
    const overlay = wrapper.find('.sidebar-overlay')
    await overlay.trigger('click')
    
    expect(layoutStore.setOverlayMenuActive).toHaveBeenCalledWith(false)
  })

  it('initializes WebSocket connections on mount', () => {
    createWrapper()
    
    expect(crawlerStore.initializeWebSocket).toHaveBeenCalled()
    expect(notificationStore.initWebSocket).toHaveBeenCalled()
  })

  it('loads crawler data on mount if not already loaded', () => {
    crawlerStore.totalCrawlers = 0
    createWrapper()
    
    expect(crawlerStore.loadCrawlerData).toHaveBeenCalled()
  })

  it('does not load crawler data if already loaded', () => {
    crawlerStore.totalCrawlers = 3
    createWrapper()
    
    expect(crawlerStore.loadCrawlerData).not.toHaveBeenCalled()
  })

  it('handles keyboard navigation correctly', async () => {
    const wrapper = createWrapper()
    
    // Mock document.addEventListener
    const addEventListenerSpy = vi.spyOn(document, 'addEventListener')
    
    // Remount to trigger onMounted
    wrapper.unmount()
    createWrapper()
    
    expect(addEventListenerSpy).toHaveBeenCalledWith('keydown', expect.any(Function))
  })

  it('shows hierarchical menu structure', () => {
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    const menuItems = appMenu.props('model')
    
    // Check that main sections have sub-items
    const sectionsWithItems = menuItems.filter((item: any) => item.items && item.items.length > 0)
    expect(sectionsWithItems.length).toBeGreaterThan(0)
    
    // Check specific sections
    const crawlerSection = menuItems.find((item: any) => item.label === 'Crawler Management')
    expect(crawlerSection.items.length).toBeGreaterThan(0)
    
    const jobSection = menuItems.find((item: any) => item.label === 'Job Management')
    expect(jobSection.items.length).toBeGreaterThan(0)
    
    const monitoringSection = menuItems.find((item: any) => item.label === 'Monitoring & Analytics')
    expect(monitoringSection.items.length).toBeGreaterThan(0)
  })

  it('includes separators in menu structure', () => {
    const wrapper = createWrapper()
    const appMenu = wrapper.findComponent({ name: 'AppMenu' })
    const menuItems = appMenu.props('model')
    
    const separators = menuItems.filter((item: any) => item.separator === true)
    expect(separators.length).toBeGreaterThan(0)
  })
})