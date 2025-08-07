<template>
  <div 
    class="layout-sidebar" 
    :class="sidebarClasses"
    @click="onSidebarClick"
  >
    <div class="layout-sidebar-content">
      <!-- Sidebar Header -->
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <i class="pi pi-sitemap sidebar-logo-icon"></i>
          <span class="sidebar-logo-text">Falsify.AI</span>
        </div>
        <Button
          v-if="layoutStore.isMobile"
          icon="pi pi-times"
          class="p-button-text p-button-rounded sidebar-close-btn"
          @click="closeMobileSidebar"
          aria-label="Close sidebar"
        />
      </div>

      <!-- Menu -->
      <AppMenu 
        :model="menuItems" 
        @menuitem-click="onMenuItemClick"
        class="sidebar-menu"
      />

      <!-- Connection Status -->
      <div class="sidebar-footer">
        <div class="connection-status" :class="connectionStatusClass">
          <i :class="connectionStatusIcon"></i>
          <span class="connection-text">{{ connectionStatusText }}</span>
        </div>
      </div>
    </div>

    <!-- Mobile Overlay -->
    <div 
      v-if="layoutStore.isMobile && layoutStore.isOverlayMenuActive"
      class="sidebar-overlay"
      @click="closeMobileSidebar"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLayoutStore } from '@/stores/layout'
import { useCrawlerStore } from '@/stores/crawler'
import { useNotificationStore } from '@/stores/notification'
import AppMenu from './AppMenu.vue'
import Button from 'primevue/button'

// Types
export interface MenuItem {
  label?: string
  icon?: string
  to?: string
  items?: MenuItem[]
  badge?: string | number
  badgeClass?: string
  separator?: boolean
  visible?: boolean
}

// Stores and router
const router = useRouter()
const layoutStore = useLayoutStore()
const crawlerStore = useCrawlerStore()
const notificationStore = useNotificationStore()

// Computed properties
const sidebarClasses = computed(() => ({
  'sidebar-mobile': layoutStore.isMobile,
  'sidebar-desktop': !layoutStore.isMobile,
  'sidebar-collapsed': layoutStore.isStaticMenuInactive && !layoutStore.isMobile,
  'sidebar-overlay-active': layoutStore.isOverlayMenuActive && layoutStore.isMobile
}))

const connectionStatusClass = computed(() => ({
  'status-online': crawlerStore.isOnline,
  'status-offline': !crawlerStore.isOnline
}))

const connectionStatusIcon = computed(() => 
  crawlerStore.isOnline ? 'pi pi-wifi' : 'pi pi-wifi text-red-500'
)

const connectionStatusText = computed(() => 
  crawlerStore.isOnline ? 'Connected' : 'Offline'
)

// Updated menu structure with Predictions management section as per design requirements
const menuItems = computed<MenuItem[]>(() => [
  {
    label: 'Dashboard',
    icon: 'pi pi-fw pi-home',
    to: '/'
  },
  {
    separator: true
  },
  {
    label: 'Crawlers Management',
    icon: 'pi pi-fw pi-sitemap',
    items: [
      {
        label: 'Management',
        icon: 'pi pi-fw pi-cog',
        to: '/crawlers/management',
        badge: crawlerStore.totalCrawlers > 0 ? crawlerStore.totalCrawlers.toString() : undefined,
        badgeClass: 'p-badge-info'
      },
      {
        label: 'History',
        icon: 'pi pi-fw pi-history',
        to: '/crawlers/history'
      }
    ]
  },
  {
    label: 'Predictions Management',
    icon: 'pi pi-fw pi-brain',
    items: [
      {
        label: 'Analyze Articles',
        icon: 'pi pi-fw pi-search',
        to: '/predictions/analysis'
      },
      {
        label: 'Prediction History',
        icon: 'pi pi-fw pi-history',
        to: '/predictions/history'
      },
      {
        label: 'Analysis Status',
        icon: 'pi pi-fw pi-chart-bar',
        to: '/analysis/status'
      }
    ]
  }
])

// Helper functions for dynamic badges - removed unused functions for now

// Methods
const onMenuItemClick = (event: { originalEvent: Event; item: MenuItem }) => {
  const { item } = event
  
  if (item.to) {
    router.push(item.to)
    
    // Close mobile menu after navigation
    if (layoutStore.isMobile) {
      layoutStore.setOverlayMenuActive(false)
    }
  }
}

const onSidebarClick = (event: Event) => {
  // Prevent closing when clicking inside sidebar content
  event.stopPropagation()
}

const closeMobileSidebar = () => {
  layoutStore.setOverlayMenuActive(false)
}

// Handle escape key for mobile
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && layoutStore.isMobile && layoutStore.isOverlayMenuActive) {
    closeMobileSidebar()
  }
}

// Initialize data and setup event listeners
onMounted(() => {
  // Load initial crawler data if not already loaded
  if (crawlerStore.totalCrawlers === 0) {
    crawlerStore.loadCrawlerData().catch(console.error)
  }

  // Initialize WebSocket connections for real-time updates
  crawlerStore.initializeWebSocket()
  notificationStore.initWebSocket()

  // Add keyboard event listener
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  // Clean up event listeners
  document.removeEventListener('keydown', handleKeydown)
})

// Watch for mobile state changes to handle responsive behavior
watch(() => layoutStore.isMobile, (isMobile) => {
  if (!isMobile && layoutStore.isOverlayMenuActive) {
    layoutStore.setOverlayMenuActive(false)
  }
})
</script>

<style lang="scss" scoped>
.layout-sidebar {
  background-color: var(--surface-card);
  border-right: 1px solid var(--surface-border);
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  transition: all 0.3s ease;
  z-index: 1000;

  &.sidebar-desktop {
    width: 280px;
    position: static;
  }

  &.sidebar-mobile {
    position: fixed;
    top: 0;
    left: 0;
    width: 280px;
    transform: translateX(-100%);
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);

    &.sidebar-overlay-active {
      transform: translateX(0);
    }
  }

  &.sidebar-collapsed {
    width: 70px;

    .sidebar-logo-text,
    .connection-text {
      opacity: 0;
      visibility: hidden;
    }

    .sidebar-logo {
      justify-content: center;
    }

    .connection-status {
      justify-content: center;
    }
  }
}

.layout-sidebar-content {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--surface-border);
  background-color: var(--surface-section);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  transition: all 0.3s ease;
}

.sidebar-logo-icon {
  font-size: 1.5rem;
  color: var(--primary-color);
  flex-shrink: 0;
}

.sidebar-logo-text {
  font-weight: 600;
  font-size: 1.1rem;
  color: var(--text-color);
  white-space: nowrap;
  transition: all 0.3s ease;
}

.sidebar-close-btn {
  width: 2rem;
  height: 2rem;
  min-width: 2rem;
  
  :deep(.p-button-icon) {
    font-size: 1rem;
  }
}

.sidebar-menu {
  flex: 1;
  padding: 1rem 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-footer {
  padding: 1rem 2rem;
  border-top: 1px solid var(--surface-border);
  background-color: var(--surface-section);
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  transition: all 0.3s ease;

  &.status-online {
    color: var(--green-600);
  }

  &.status-offline {
    color: var(--red-600);
  }
}

.connection-text {
  transition: all 0.3s ease;
}

.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.4);
  z-index: 999;
  backdrop-filter: blur(2px);
}

// Scrollbar styling
.layout-sidebar::-webkit-scrollbar,
.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}

.layout-sidebar::-webkit-scrollbar-track,
.sidebar-menu::-webkit-scrollbar-track {
  background: var(--surface-ground);
}

.layout-sidebar::-webkit-scrollbar-thumb,
.sidebar-menu::-webkit-scrollbar-thumb {
  background: var(--surface-border);
  border-radius: 3px;
}

.layout-sidebar::-webkit-scrollbar-thumb:hover,
.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background: var(--text-color-secondary);
}

// Responsive breakpoints
@media (max-width: 991px) {
  .layout-sidebar {
    &.sidebar-mobile {
      width: 100vw;
      max-width: 320px;
    }
  }
}

@media (max-width: 576px) {
  .layout-sidebar {
    &.sidebar-mobile {
      width: 100vw;
      max-width: 280px;
    }
  }

  .sidebar-header {
    padding: 1rem 1.5rem;
  }

  .sidebar-footer {
    padding: 1rem 1.5rem;
  }
}

// Animation for mobile sidebar
@media (max-width: 991px) {
  .layout-sidebar.sidebar-mobile {
    transition: transform 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  }
}

// Dark theme adjustments
:global([data-theme="dark"]) {
  .layout-sidebar {
    background-color: var(--surface-card);
    border-right-color: var(--surface-border);
  }

  .sidebar-header,
  .sidebar-footer {
    background-color: var(--surface-section);
    border-color: var(--surface-border);
  }

  .sidebar-logo-text {
    color: var(--text-color);
  }

  .sidebar-overlay {
    background-color: rgba(0, 0, 0, 0.6);
  }
}

// High contrast mode support
@media (prefers-contrast: high) {
  .layout-sidebar {
    border-right-width: 2px;
  }

  .sidebar-header,
  .sidebar-footer {
    border-width: 2px;
  }
}

// Reduced motion support
@media (prefers-reduced-motion: reduce) {
  .layout-sidebar,
  .sidebar-logo,
  .sidebar-logo-text,
  .connection-status,
  .connection-text {
    transition: none;
  }

  .sidebar-overlay {
    backdrop-filter: none;
  }
}
</style>