import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useThemeStore } from './theme'

export interface LayoutState {
  sidebarVisible: boolean
  staticMenuInactive: boolean
  overlayMenuActive: boolean
  profileSidebarVisible: boolean
  configSidebarVisible: boolean
  menuMode: 'static' | 'overlay' | 'horizontal'
}

export const useLayoutStore = defineStore('layout', () => {
  // State
  const state = ref<LayoutState>({
    sidebarVisible: true,
    staticMenuInactive: false,
    overlayMenuActive: false,
    profileSidebarVisible: false,
    configSidebarVisible: false,
    menuMode: 'static'
  })

  // Theme store integration
  const themeStore = useThemeStore()

  // Getters
  const isSidebarVisible = computed(() => state.value.sidebarVisible)
  const isStaticMenuInactive = computed(() => state.value.staticMenuInactive)
  const isOverlayMenuActive = computed(() => state.value.overlayMenuActive)
  const isProfileSidebarVisible = computed(() => state.value.profileSidebarVisible)
  const isConfigSidebarVisible = computed(() => state.value.configSidebarVisible)
  const currentMenuMode = computed(() => state.value.menuMode)

  // Theme-related getters (delegated to theme store)
  const currentTheme = computed(() => themeStore.currentMode)
  const currentScale = computed(() => themeStore.currentScale)
  const currentInputStyle = computed(() => themeStore.currentInputStyle)
  const isRippleEnabled = computed(() => themeStore.isRippleEnabled)

  const isMobile = computed(() => window.innerWidth <= 991)
  const isDesktop = computed(() => !isMobile.value)

  const containerClass = computed(() => {
    return {
      'layout-theme-light': themeStore.currentMode === 'light',
      'layout-theme-dark': themeStore.currentMode === 'dark',
      'layout-overlay': state.value.menuMode === 'overlay',
      'layout-static': state.value.menuMode === 'static',
      'layout-static-inactive': state.value.staticMenuInactive && state.value.menuMode === 'static',
      'layout-overlay-active': state.value.overlayMenuActive && state.value.menuMode === 'overlay',
      'layout-mobile-active': state.value.overlayMenuActive && isMobile.value,
      'p-input-filled': themeStore.currentInputStyle === 'filled',
      'p-ripple-disabled': !themeStore.isRippleEnabled,
      ...themeStore.themeClasses
    }
  })

  // Actions
  const setSidebarVisible = (visible: boolean) => {
    state.value.sidebarVisible = visible
  }

  const toggleSidebar = () => {
    if (isMobile.value) {
      state.value.overlayMenuActive = !state.value.overlayMenuActive
    } else {
      state.value.staticMenuInactive = !state.value.staticMenuInactive
    }
  }

  const setStaticMenuInactive = (inactive: boolean) => {
    state.value.staticMenuInactive = inactive
  }

  const setOverlayMenuActive = (active: boolean) => {
    state.value.overlayMenuActive = active
  }

  const setProfileSidebarVisible = (visible: boolean) => {
    state.value.profileSidebarVisible = visible
  }

  const setConfigSidebarVisible = (visible: boolean) => {
    state.value.configSidebarVisible = visible
  }

  // Theme-related actions (delegated to theme store)
  const setTheme = (theme: 'light' | 'dark') => {
    themeStore.setMode(theme)
  }

  const toggleTheme = () => {
    themeStore.toggleMode()
  }

  const setScale = (scale: number) => {
    if ([12, 13, 14, 15, 16].includes(scale)) {
      themeStore.setScale(scale as any)
    }
  }

  const setInputStyle = (style: 'outlined' | 'filled') => {
    themeStore.setInputStyle(style)
  }

  const setRipple = (ripple: boolean) => {
    themeStore.setRipple(ripple)
  }

  const setMenuMode = (mode: 'static' | 'overlay' | 'horizontal') => {
    state.value.menuMode = mode
    localStorage.setItem('menuMode', mode)
  }

  const onMenuToggle = () => {
    if (state.value.overlayMenuActive) {
      state.value.overlayMenuActive = false
    } else {
      toggleSidebar()
    }
  }

  const onSidebarClick = () => {
    // Close overlay menu when clicking on sidebar in mobile
    if (isMobile.value && state.value.overlayMenuActive) {
      state.value.overlayMenuActive = false
    }
  }

  const onMenuClick = (event: Event) => {
    // Prevent menu from closing when clicking on menu items
    event.stopPropagation()
  }

  const resetMenu = () => {
    state.value.overlayMenuActive = false
    state.value.staticMenuInactive = false
  }

  const initializeLayout = () => {
    // Initialize theme store
    const cleanupTheme = themeStore.initialize()

    // Load saved layout preferences from localStorage
    const savedMenuMode = localStorage.getItem('menuMode') as 'static' | 'overlay' | 'horizontal' | null
    if (savedMenuMode) {
      setMenuMode(savedMenuMode)
    }

    // Handle window resize
    const handleResize = () => {
      if (window.innerWidth > 991) {
        state.value.overlayMenuActive = false
      }
    }

    window.addEventListener('resize', handleResize)

    // Return cleanup function
    return () => {
      window.removeEventListener('resize', handleResize)
      cleanupTheme?.()
    }
  }

  return {
    // State
    state,
    
    // Getters
    isSidebarVisible,
    isStaticMenuInactive,
    isOverlayMenuActive,
    isProfileSidebarVisible,
    isConfigSidebarVisible,
    currentTheme,
    currentScale,
    currentMenuMode,
    currentInputStyle,
    isRippleEnabled,
    isMobile,
    isDesktop,
    containerClass,
    
    // Actions
    setSidebarVisible,
    toggleSidebar,
    setStaticMenuInactive,
    setOverlayMenuActive,
    setProfileSidebarVisible,
    setConfigSidebarVisible,
    setTheme,
    toggleTheme,
    setScale,
    setMenuMode,
    setInputStyle,
    setRipple,
    onMenuToggle,
    onSidebarClick,
    onMenuClick,
    resetMenu,
    initializeLayout
  }
})