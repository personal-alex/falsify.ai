import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useThemeStore } from '../theme'

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clear: vi.fn(),
}
Object.defineProperty(window, 'localStorage', {
  value: localStorageMock
})

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

describe('Theme Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('Initial State', () => {
    it('should have default theme configuration', () => {
      const themeStore = useThemeStore()
      
      expect(themeStore.currentMode).toBe('light')
      expect(themeStore.currentScale).toBe(14)
      expect(themeStore.currentInputStyle).toBe('outlined')
      expect(themeStore.isRippleEnabled).toBe(true)
    })

    it('should provide theme colors based on current mode', () => {
      const themeStore = useThemeStore()
      
      expect(themeStore.colors.primary).toBe('#0ea5e9')
      expect(themeStore.colors.surface).toBe('#ffffff')
      expect(themeStore.colors.text).toBe('#212121')
      expect(themeStore.colors.border).toBe('#e0e0e0')
    })

    it('should provide correct theme classes', () => {
      const themeStore = useThemeStore()
      
      expect(themeStore.themeClasses).toEqual({
        'theme-light': true,
        'scale-14': true,
        'input-outlined': true,
        'ripple-disabled': false
      })
    })
  })

  describe('Theme Mode Management', () => {
    it('should toggle between light and dark modes', () => {
      const themeStore = useThemeStore()
      
      expect(themeStore.currentMode).toBe('light')
      expect(themeStore.isLightMode).toBe(true)
      expect(themeStore.isDarkMode).toBe(false)
      
      themeStore.toggleMode()
      
      expect(themeStore.currentMode).toBe('dark')
      expect(themeStore.isLightMode).toBe(false)
      expect(themeStore.isDarkMode).toBe(true)
    })

    it('should set specific theme mode', () => {
      const themeStore = useThemeStore()
      
      themeStore.setMode('dark')
      
      expect(themeStore.currentMode).toBe('dark')
      expect(themeStore.colors.primary).toBe('#38bdf8')
      expect(themeStore.colors.surface).toBe('#121212')
      expect(themeStore.colors.text).toBe('#ffffff')
      expect(themeStore.colors.border).toBe('#3d3d3d')
    })

    it('should update theme classes when mode changes', () => {
      const themeStore = useThemeStore()
      
      themeStore.setMode('dark')
      
      expect(themeStore.themeClasses).toEqual({
        'theme-dark': true,
        'scale-14': true,
        'input-outlined': true,
        'ripple-disabled': false
      })
    })
  })

  describe('Scale Management', () => {
    it('should set font scale', () => {
      const themeStore = useThemeStore()
      
      themeStore.setScale(16)
      
      expect(themeStore.currentScale).toBe(16)
      expect(themeStore.themeClasses['scale-16']).toBe(true)
    })

    it('should validate scale values', () => {
      const themeStore = useThemeStore()
      
      // Valid scales should work
      themeStore.setScale(12)
      expect(themeStore.currentScale).toBe(12)
      
      themeStore.setScale(16)
      expect(themeStore.currentScale).toBe(16)
    })
  })

  describe('Input Style Management', () => {
    it('should set input style', () => {
      const themeStore = useThemeStore()
      
      themeStore.setInputStyle('filled')
      
      expect(themeStore.currentInputStyle).toBe('filled')
      expect(themeStore.themeClasses['input-filled']).toBe(true)
    })
  })

  describe('Ripple Management', () => {
    it('should toggle ripple effects', () => {
      const themeStore = useThemeStore()
      
      expect(themeStore.isRippleEnabled).toBe(true)
      expect(themeStore.themeClasses['ripple-disabled']).toBe(false)
      
      themeStore.setRipple(false)
      
      expect(themeStore.isRippleEnabled).toBe(false)
      expect(themeStore.themeClasses['ripple-disabled']).toBe(true)
    })
  })

  describe('Configuration Management', () => {
    it('should update multiple config properties at once', () => {
      const themeStore = useThemeStore()
      
      themeStore.updateConfig({
        mode: 'dark',
        scale: 16,
        inputStyle: 'filled',
        ripple: false
      })
      
      expect(themeStore.currentMode).toBe('dark')
      expect(themeStore.currentScale).toBe(16)
      expect(themeStore.currentInputStyle).toBe('filled')
      expect(themeStore.isRippleEnabled).toBe(false)
    })

    it('should reset to default configuration', () => {
      const themeStore = useThemeStore()
      
      // Change some settings
      themeStore.setMode('dark')
      themeStore.setScale(16)
      themeStore.setInputStyle('filled')
      themeStore.setRipple(false)
      
      // Reset
      themeStore.reset()
      
      expect(themeStore.currentMode).toBe('light')
      expect(themeStore.currentScale).toBe(14)
      expect(themeStore.currentInputStyle).toBe('outlined')
      expect(themeStore.isRippleEnabled).toBe(true)
    })
  })

  describe('Persistence', () => {
    it('should persist theme configuration to localStorage', () => {
      const themeStore = useThemeStore()
      
      themeStore.setMode('dark')
      
      expect(localStorageMock.setItem).toHaveBeenCalledWith(
        'theme-config',
        JSON.stringify({
          mode: 'dark',
          scale: 14,
          inputStyle: 'outlined',
          ripple: true
        })
      )
    })

    it('should load persisted theme configuration', () => {
      localStorageMock.getItem.mockReturnValue(JSON.stringify({
        mode: 'dark',
        scale: 16,
        inputStyle: 'filled',
        ripple: false
      }))
      
      const themeStore = useThemeStore()
      themeStore.initialize()
      
      expect(themeStore.currentMode).toBe('dark')
      expect(themeStore.currentScale).toBe(16)
      expect(themeStore.currentInputStyle).toBe('filled')
      expect(themeStore.isRippleEnabled).toBe(false)
    })

    it('should handle invalid persisted configuration gracefully', () => {
      localStorageMock.getItem.mockReturnValue('invalid-json')
      
      const themeStore = useThemeStore()
      themeStore.initialize()
      
      // Should fall back to defaults
      expect(themeStore.currentMode).toBe('light')
      expect(themeStore.currentScale).toBe(14)
    })
  })

  describe('System Theme Detection', () => {
    it('should detect system theme preference', () => {
      const themeStore = useThemeStore()
      
      expect(themeStore.detectSystemTheme()).toBe('light')
    })

    it('should use system theme when no preference is stored', () => {
      // Mock dark system preference
      window.matchMedia = vi.fn().mockImplementation(query => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }))
      
      localStorageMock.getItem.mockReturnValue(null)
      
      const themeStore = useThemeStore()
      themeStore.initialize()
      
      expect(themeStore.currentMode).toBe('dark')
    })
  })
})