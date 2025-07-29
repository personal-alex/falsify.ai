import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'
export type ThemeScale = 12 | 13 | 14 | 15 | 16
export type InputStyle = 'outlined' | 'filled'

export interface ThemeConfig {
  mode: ThemeMode
  scale: ThemeScale
  inputStyle: InputStyle
  ripple: boolean
}

export interface ThemeColors {
  primary: string
  surface: string
  text: string
  border: string
}

export const useThemeStore = defineStore('theme', () => {
  // State
  const config = ref<ThemeConfig>({
    mode: 'light',
    scale: 14,
    inputStyle: 'outlined',
    ripple: true
  })

  const isInitialized = ref(false)

  // Getters
  const currentMode = computed(() => config.value.mode)
  const currentScale = computed(() => config.value.scale)
  const currentInputStyle = computed(() => config.value.inputStyle)
  const isRippleEnabled = computed(() => config.value.ripple)
  const isDarkMode = computed(() => config.value.mode === 'dark')
  const isLightMode = computed(() => config.value.mode === 'light')

  // Theme colors based on current mode
  const colors = computed<ThemeColors>(() => {
    if (config.value.mode === 'dark') {
      return {
        primary: '#38bdf8',
        surface: '#121212',
        text: '#ffffff',
        border: '#3d3d3d'
      }
    } else {
      return {
        primary: '#0ea5e9',
        surface: '#ffffff',
        text: '#212121',
        border: '#e0e0e0'
      }
    }
  })

  // CSS classes for theme application
  const themeClasses = computed(() => ({
    [`theme-${config.value.mode}`]: true,
    [`scale-${config.value.scale}`]: true,
    [`input-${config.value.inputStyle}`]: true,
    'ripple-disabled': !config.value.ripple
  }))

  // Actions
  const setMode = (mode: ThemeMode) => {
    config.value.mode = mode
    applyThemeToDocument()
    persistTheme()
  }

  const toggleMode = () => {
    const newMode = config.value.mode === 'light' ? 'dark' : 'light'
    setMode(newMode)
  }

  const setScale = (scale: ThemeScale) => {
    config.value.scale = scale
    applyScaleToDocument()
    persistTheme()
  }

  const setInputStyle = (style: InputStyle) => {
    config.value.inputStyle = style
    applyInputStyleToDocument()
    persistTheme()
  }

  const setRipple = (enabled: boolean) => {
    config.value.ripple = enabled
    applyRippleToDocument()
    persistTheme()
  }

  const updateConfig = (newConfig: Partial<ThemeConfig>) => {
    config.value = { ...config.value, ...newConfig }
    applyThemeToDocument()
    persistTheme()
  }

  // Apply theme to document
  const applyThemeToDocument = () => {
    const html = document.documentElement
    
    // Set theme mode
    html.setAttribute('data-theme', config.value.mode)
    
    // Update body class for theme mode
    document.body.className = document.body.className
      .replace(/theme-(light|dark)/g, '')
      .trim()
    document.body.classList.add(`theme-${config.value.mode}`)
    
    // Apply scale
    applyScaleToDocument()
    
    // Apply input style
    applyInputStyleToDocument()
    
    // Apply ripple
    applyRippleToDocument()
  }

  const applyScaleToDocument = () => {
    document.documentElement.style.fontSize = `${config.value.scale}px`
    document.documentElement.setAttribute('data-scale', config.value.scale.toString())
  }

  const applyInputStyleToDocument = () => {
    const html = document.documentElement
    html.classList.remove('p-input-filled', 'p-input-outlined')
    html.classList.add(`p-input-${config.value.inputStyle}`)
  }

  const applyRippleToDocument = () => {
    const html = document.documentElement
    if (config.value.ripple) {
      html.classList.remove('p-ripple-disabled')
    } else {
      html.classList.add('p-ripple-disabled')
    }
  }

  // Persistence
  const persistTheme = () => {
    try {
      localStorage.setItem('theme-config', JSON.stringify(config.value))
    } catch (error) {
      console.warn('Failed to persist theme configuration:', error)
    }
  }

  const loadPersistedTheme = (): ThemeConfig | null => {
    try {
      const stored = localStorage.getItem('theme-config')
      if (stored) {
        const parsed = JSON.parse(stored)
        // Validate the parsed config
        if (isValidThemeConfig(parsed)) {
          return parsed
        }
      }
    } catch (error) {
      console.warn('Failed to load persisted theme configuration:', error)
    }
    return null
  }

  const isValidThemeConfig = (config: any): config is ThemeConfig => {
    return (
      config &&
      typeof config === 'object' &&
      ['light', 'dark'].includes(config.mode) &&
      [12, 13, 14, 15, 16].includes(config.scale) &&
      ['outlined', 'filled'].includes(config.inputStyle) &&
      typeof config.ripple === 'boolean'
    )
  }

  // System theme detection
  const detectSystemTheme = (): ThemeMode => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }
    return 'light'
  }

  const watchSystemTheme = () => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      
      const handleChange = (e: MediaQueryListEvent) => {
        // Only auto-switch if user hasn't manually set a preference
        const hasManualPreference = localStorage.getItem('theme-config')
        if (!hasManualPreference) {
          setMode(e.matches ? 'dark' : 'light')
        }
      }
      
      mediaQuery.addEventListener('change', handleChange)
      
      // Return cleanup function
      return () => {
        mediaQuery.removeEventListener('change', handleChange)
      }
    }
    return () => {}
  }

  // Initialization
  const initialize = () => {
    if (isInitialized.value) return

    // Load persisted theme or use system preference
    const persistedTheme = loadPersistedTheme()
    if (persistedTheme) {
      config.value = persistedTheme
    } else {
      // Use system preference for initial theme
      config.value.mode = detectSystemTheme()
    }

    // Apply theme to document
    applyThemeToDocument()

    // Watch for system theme changes
    const cleanupSystemWatch = watchSystemTheme()

    isInitialized.value = true

    // Return cleanup function
    return cleanupSystemWatch
  }

  // Reset to defaults
  const reset = () => {
    config.value = {
      mode: detectSystemTheme(),
      scale: 14,
      inputStyle: 'outlined',
      ripple: true
    }
    applyThemeToDocument()
    persistTheme()
  }

  // Watch for config changes to apply them
  watch(
    () => config.value,
    () => {
      if (isInitialized.value) {
        applyThemeToDocument()
      }
    },
    { deep: true }
  )

  return {
    // State
    config,
    isInitialized,
    
    // Getters
    currentMode,
    currentScale,
    currentInputStyle,
    isRippleEnabled,
    isDarkMode,
    isLightMode,
    colors,
    themeClasses,
    
    // Actions
    setMode,
    toggleMode,
    setScale,
    setInputStyle,
    setRipple,
    updateConfig,
    initialize,
    reset,
    
    // Utilities
    detectSystemTheme,
    applyThemeToDocument
  }
})