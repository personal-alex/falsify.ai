import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AppTopbar from '../AppTopbar.vue'
import { useLayoutStore } from '@/stores/layout'
import { useCrawlerStore } from '@/stores/crawler'
import { useNotificationStore } from '@/stores/notification'

// Mock tooltip directive
const mockTooltip = {
  mounted: vi.fn(),
  updated: vi.fn(),
  unmounted: vi.fn()
}

// Mock router-link
const RouterLinkStub = {
  template: '<a><slot /></a>',
  props: ['to']
}

describe('AppTopbar', () => {
  let wrapper: any
  let layoutStore: any
  let crawlerStore: any
  let notificationStore: any

  beforeEach(() => {
    setActivePinia(createPinia())
    
    layoutStore = useLayoutStore()
    crawlerStore = useCrawlerStore()
    notificationStore = useNotificationStore()

    wrapper = mount(AppTopbar, {
      global: {
        directives: {
          tooltip: mockTooltip
        },
        stubs: {
          'router-link': RouterLinkStub
        }
      }
    })
  })

  describe('Component Structure', () => {
    it('renders the topbar with correct structure', () => {
      expect(wrapper.find('.layout-topbar').exists()).toBe(true)
      expect(wrapper.find('.layout-topbar-logo').exists()).toBe(true)
      expect(wrapper.find('.layout-topbar-menu').exists()).toBe(true)
    })

    it('displays the logo and branding correctly', () => {
      const logo = wrapper.find('.layout-topbar-logo-link')
      expect(logo.exists()).toBe(true)
      
      const icon = wrapper.find('.layout-topbar-logo-icon')
      expect(icon.exists()).toBe(true)
      expect(icon.classes()).toContain('pi-sitemap')
      
      const text = wrapper.find('.layout-topbar-logo-text')
      expect(text.exists()).toBe(true)
      expect(text.text()).toBe('Crawler Manager')
    })

    it('renders all navigation buttons', () => {
      const buttons = wrapper.findAll('.layout-topbar-button')
      expect(buttons.length).toBeGreaterThanOrEqual(4) // menu, notifications, theme, settings
      
      // Check for specific buttons
      expect(wrapper.find('.layout-menu-button').exists()).toBe(true)
      expect(wrapper.find('.notification-button').exists()).toBe(true)
      expect(wrapper.find('.theme-toggle').exists()).toBe(true)
      expect(wrapper.find('.settings-button').exists()).toBe(true)
    })
  })

  describe('Theme Toggle Functionality', () => {
    it('displays correct theme icon based on current theme', async () => {
      // Test light theme
      layoutStore.setTheme('light')
      await wrapper.vm.$nextTick()
      
      const themeButton = wrapper.find('.theme-toggle i')
      expect(themeButton.classes()).toContain('pi-moon')
      
      // Test dark theme
      layoutStore.setTheme('dark')
      await wrapper.vm.$nextTick()
      
      expect(themeButton.classes()).toContain('pi-sun')
    })

    it('toggles theme when theme button is clicked', async () => {
      const initialTheme = layoutStore.currentTheme
      const themeButton = wrapper.find('.theme-toggle')
      
      await themeButton.trigger('click')
      
      expect(layoutStore.currentTheme).not.toBe(initialTheme)
    })

    it('applies correct CSS classes based on theme', async () => {
      layoutStore.setTheme('dark')
      await wrapper.vm.$nextTick()
      
      const themeButton = wrapper.find('.theme-toggle')
      expect(themeButton.classes()).toContain('theme-dark')
      
      layoutStore.setTheme('light')
      await wrapper.vm.$nextTick()
      
      expect(themeButton.classes()).toContain('theme-light')
    })
  })

  describe('Notification System', () => {
    it('displays notification count when there are unread notifications', async () => {
      // Add notifications to the store
      notificationStore.addNotification({
        type: 'info',
        title: 'Test',
        message: 'Test message',
        persistent: true,
        autoClose: false
      })
      notificationStore.addNotification({
        type: 'warn',
        title: 'Test 2',
        message: 'Test message 2',
        persistent: true,
        autoClose: false
      })
      
      await wrapper.vm.$nextTick()
      
      const badge = wrapper.find('.layout-topbar-badge')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toBe('2')
    })

    it('displays 99+ for notification counts over 99', async () => {
      // Clear existing notifications and add many
      notificationStore.clearAllNotifications()
      
      // The store limits to 50 notifications, but we can test the display logic
      // by directly mocking the unreadCount getter before creating the wrapper
      const mockUnreadCount = vi.spyOn(notificationStore, 'unreadCount', 'get')
      mockUnreadCount.mockReturnValue(150)
      
      // Create a new wrapper with the mocked value
      const testWrapper = mount(AppTopbar, {
        global: {
          directives: {
            tooltip: mockTooltip
          },
          stubs: {
            'router-link': RouterLinkStub
          }
        }
      })
      
      await testWrapper.vm.$nextTick()
      
      const badge = testWrapper.find('.layout-topbar-badge')
      expect(badge.exists()).toBe(true)
      expect(badge.text()).toBe('99+')
      
      mockUnreadCount.mockRestore()
      testWrapper.unmount()
    })

    it('hides notification badge when count is 0', async () => {
      notificationStore.clearAllNotifications()
      await wrapper.vm.$nextTick()
      
      const badge = wrapper.find('.layout-topbar-badge')
      expect(badge.exists()).toBe(false)
    })

    it('applies has-notifications class when there are notifications', async () => {
      notificationStore.clearAllNotifications()
      notificationStore.addNotification({
        type: 'info',
        title: 'Test',
        message: 'Test message',
        persistent: true,
        autoClose: false
      })
      
      await wrapper.vm.$nextTick()
      
      const notificationButton = wrapper.find('.notification-button')
      expect(notificationButton.classes()).toContain('has-notifications')
    })

    it('emits notifications-toggle event when notification button is clicked', async () => {
      const notificationButton = wrapper.find('.notification-button')
      await notificationButton.trigger('click')
      
      expect(wrapper.emitted('notifications-toggle')).toBeTruthy()
    })
  })

  describe('Connection Status', () => {
    it('displays online status correctly', async () => {
      // Set the connection status in the crawler store
      crawlerStore.$patch({ 
        connectionStatus: { online: true, lastCheck: new Date() }
      })
      await wrapper.vm.$nextTick()
      
      const connectionStatus = wrapper.find('.connection-status')
      expect(connectionStatus.classes()).toContain('connection-online')
      expect(connectionStatus.text()).toContain('Online')
      
      const icon = connectionStatus.find('i')
      expect(icon.classes()).toContain('pi-circle-fill')
    })

    it('displays offline status correctly', async () => {
      // Set the connection status in the crawler store
      crawlerStore.$patch({ 
        connectionStatus: { online: false, lastCheck: new Date() }
      })
      await wrapper.vm.$nextTick()
      
      const connectionStatus = wrapper.find('.connection-status')
      expect(connectionStatus.classes()).toContain('connection-offline')
      expect(connectionStatus.text()).toContain('Offline')
      
      const icon = connectionStatus.find('i')
      expect(icon.classes()).toContain('pi-exclamation-triangle')
    })
  })

  describe('Menu Toggle', () => {
    it('emits menu-toggle event when menu button is clicked', async () => {
      const menuButton = wrapper.find('.layout-menu-button')
      await menuButton.trigger('click')
      
      expect(wrapper.emitted('menu-toggle')).toBeTruthy()
    })

    it('calls layoutStore.onMenuToggle when menu button is clicked', async () => {
      const onMenuToggleSpy = vi.spyOn(layoutStore, 'onMenuToggle')
      const menuButton = wrapper.find('.layout-menu-button')
      
      await menuButton.trigger('click')
      
      expect(onMenuToggleSpy).toHaveBeenCalled()
    })
  })

  describe('Settings Sidebar', () => {
    it('toggles config sidebar when settings button is clicked', async () => {
      const initialState = layoutStore.isConfigSidebarVisible
      const settingsButton = wrapper.find('.settings-button')
      
      await settingsButton.trigger('click')
      
      expect(layoutStore.isConfigSidebarVisible).toBe(!initialState)
    })

    it('applies active class when config sidebar is visible', async () => {
      layoutStore.setConfigSidebarVisible(true)
      await wrapper.vm.$nextTick()
      
      const settingsButton = wrapper.find('.settings-button')
      expect(settingsButton.classes()).toContain('active')
    })
  })

  describe('Accessibility', () => {
    it('has proper aria-labels on buttons', () => {
      const menuButton = wrapper.find('.layout-menu-button')
      const notificationButton = wrapper.find('.notification-button')
      const themeButton = wrapper.find('.theme-toggle')
      const settingsButton = wrapper.find('.settings-button')
      
      expect(menuButton.attributes('aria-label')).toBe('Toggle navigation menu')
      expect(notificationButton.attributes('aria-label')).toBe('View notifications')
      expect(themeButton.attributes('aria-label')).toBe('Toggle theme')
      expect(settingsButton.attributes('aria-label')).toBe('Open settings')
    })

    it('has proper button types', () => {
      const buttons = wrapper.findAll('button')
      buttons.forEach(button => {
        expect(button.attributes('type')).toBe('button')
      })
    })
  })

  describe('Responsive Behavior', () => {
    it('has responsive CSS classes', () => {
      const topbar = wrapper.find('.layout-topbar')
      expect(topbar.exists()).toBe(true)
      
      // Check that responsive styles are applied through CSS classes
      const logo = wrapper.find('.layout-topbar-logo')
      const menu = wrapper.find('.layout-topbar-menu')
      
      expect(logo.exists()).toBe(true)
      expect(menu.exists()).toBe(true)
    })
  })

  describe('Computed Properties', () => {
    it('computes theme tooltip correctly', async () => {
      layoutStore.setTheme('light')
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.themeTooltip).toBe('Switch to dark theme')
      
      layoutStore.setTheme('dark')
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.themeTooltip).toBe('Switch to light theme')
    })

    it('computes notification tooltip correctly', async () => {
      notificationStore.clearAllNotifications()
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.notificationTooltip).toBe('No new notifications')
      
      notificationStore.addNotification({
        type: 'info',
        title: 'Test',
        message: 'Test message',
        persistent: true,
        autoClose: false
      })
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.notificationTooltip).toBe('1 unread notification')
      
      // Add more notifications
      for (let i = 0; i < 4; i++) {
        notificationStore.addNotification({
          type: 'info',
          title: `Test ${i}`,
          message: `Test message ${i}`,
          persistent: true,
          autoClose: false
        })
      }
      await wrapper.vm.$nextTick()
      
      expect(wrapper.vm.notificationTooltip).toBe('5 unread notifications')
    })
  })
})