<template>
  <div class="layout-topbar">
    <!-- Logo and Branding Section -->
    <div class="layout-topbar-logo">
      <router-link to="/" class="layout-topbar-logo-link">
        <i class="pi pi-sitemap layout-topbar-logo-icon"></i>
        <span class="layout-topbar-logo-text">Falsify.AI</span>
      </router-link>
    </div>
    
    <!-- Navigation Controls Section -->
    <div class="layout-topbar-menu">
      <!-- Mobile Menu Toggle -->
      <button
        class="p-link layout-menu-button layout-topbar-button"
        @click="onMenuToggle"
        type="button"
        v-tooltip.bottom="'Toggle Menu'"
        aria-label="Toggle navigation menu"
      >
        <i class="pi pi-bars"></i>
      </button>
      
      <!-- Dynamic Context Actions -->
      <div class="layout-topbar-context-actions" v-if="hasContextActions">
        <div class="context-actions-separator"></div>
        <div class="context-actions-container">
          <Button
            v-for="action in contextActions"
            :key="action.id"
            :icon="action.icon"
            :label="action.label"
            :severity="action.severity || 'primary'"
            :outlined="action.outlined"
            :loading="action.loading"
            :disabled="action.disabled"
            :class="['context-action-button', `action-${action.id}`]"
            @click="action.command"
            v-tooltip.bottom="action.tooltip"
            size="small"
          />
        </div>
      </div>

      <!-- Action Items Section -->
      <div class="layout-topbar-menu-section">
        <!-- Connection Status Indicator -->
        <div class="layout-topbar-item connection-item">
          <div class="connection-status" :class="connectionStatusClass">
            <i :class="connectionStatusIcon"></i>
            <span class="connection-text">{{ connectionStatusText }}</span>
          </div>
        </div>
        
        <!-- Notifications Bell -->
        <div class="layout-topbar-item">
          <button
            class="p-link layout-topbar-button notification-button"
            @click="toggleNotifications"
            type="button"
            v-tooltip.bottom="notificationTooltip"
            :class="{ 'has-notifications': notificationCount > 0 }"
            aria-label="View notifications"
          >
            <i class="pi pi-bell"></i>
            <span class="layout-topbar-badge" v-if="notificationCount > 0">
              {{ notificationCount > 99 ? '99+' : notificationCount }}
            </span>
          </button>
        </div>
        
        <!-- Theme Toggle Button -->
        <div class="layout-topbar-item">
          <button
            class="p-link layout-topbar-button theme-toggle"
            @click="toggleTheme"
            type="button"
            v-tooltip.bottom="themeTooltip"
            :class="{ 'theme-dark': currentTheme === 'dark', 'theme-light': currentTheme === 'light' }"
            aria-label="Toggle theme"
          >
            <i :class="themeIcon"></i>
          </button>
        </div>
        
        <!-- Settings Button -->
        <div class="layout-topbar-item">
          <button
            class="p-link layout-topbar-button settings-button"
            @click="toggleConfigSidebar"
            type="button"
            v-tooltip.bottom="'Application Settings'"
            :class="{ 'active': isConfigSidebarVisible }"
            aria-label="Open settings"
          >
            <i class="pi pi-cog"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useLayoutStore } from '@/stores/layout'
import { useThemeStore } from '@/stores/theme'
import { useCrawlerStore } from '@/stores/crawler'
import { useNotificationStore } from '@/stores/notification'
import { useTopBarStore } from '@/stores/topbar'
import Button from 'primevue/button'

// Define emits
const emit = defineEmits<{
  'menu-toggle': []
  'notifications-toggle': []
}>()

// Stores
const layoutStore = useLayoutStore()
const themeStore = useThemeStore()
const crawlerStore = useCrawlerStore()
const notificationStore = useNotificationStore()
const topBarStore = useTopBarStore()

// Computed properties
const currentTheme = computed(() => themeStore.currentMode)
const isOnline = computed(() => crawlerStore.isOnline)
const notificationCount = computed(() => notificationStore.unreadCount)
const isConfigSidebarVisible = computed(() => layoutStore.isConfigSidebarVisible)

// TopBar context actions
const contextActions = computed(() => topBarStore.contextActions)
const hasContextActions = computed(() => topBarStore.hasActions)

// Theme-related computed properties
const themeIcon = computed(() => 
  currentTheme.value === 'light' ? 'pi pi-moon' : 'pi pi-sun'
)

const themeTooltip = computed(() => 
  `Switch to ${currentTheme.value === 'light' ? 'dark' : 'light'} theme`
)

// Connection status computed properties
const connectionStatusClass = computed(() => ({
  'connection-online': isOnline.value,
  'connection-offline': !isOnline.value
}))

const connectionStatusIcon = computed(() => 
  isOnline.value ? 'pi pi-circle-fill' : 'pi pi-exclamation-triangle'
)

const connectionStatusText = computed(() => 
  isOnline.value ? 'Online' : 'Offline'
)

// Notification computed properties
const notificationTooltip = computed(() => {
  if (notificationCount.value === 0) {
    return 'No new notifications'
  }
  return `${notificationCount.value} unread notification${notificationCount.value === 1 ? '' : 's'}`
})

// Methods
const onMenuToggle = () => {
  layoutStore.onMenuToggle()
  emit('menu-toggle')
}

const toggleTheme = () => {
  themeStore.toggleMode()
  
  // Add visual feedback for theme change
  const button = document.querySelector('.theme-toggle')
  if (button) {
    button.classList.add('theme-changing')
    setTimeout(() => {
      button.classList.remove('theme-changing')
    }, 300)
  }
}

const toggleNotifications = () => {
  emit('notifications-toggle')
  // TODO: Implement notifications panel/dropdown
  console.log('Toggle notifications panel')
}

const toggleConfigSidebar = () => {
  layoutStore.setConfigSidebarVisible(!layoutStore.isConfigSidebarVisible)
}
</script>

<style lang="scss" scoped>
// Import Sakai variables for consistency
@import '@/assets/themes/variables';

.layout-topbar {
  position: fixed;
  height: var(--header-height, 4rem);
  z-index: 997;
  left: 0;
  top: 0;
  width: 100%;
  padding: 0 2rem;
  background-color: var(--surface-card);
  border-bottom: 1px solid var(--surface-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: left var(--transition-duration), box-shadow var(--transition-duration);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(8px);
  
  // Enhanced shadow on scroll (can be added via JS)
  &.scrolled {
    box-shadow: var(--card-shadow);
  }
}

// Logo and Branding Section
.layout-topbar-logo {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.layout-topbar-logo-link {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: var(--text-color);
  transition: color var(--transition-duration);
  
  &:hover {
    color: var(--primary-color);
    
    .layout-topbar-logo-icon {
      transform: scale(1.05);
    }
  }
  
  &:focus {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
    border-radius: var(--border-radius);
  }
}

.layout-topbar-logo-icon {
  font-size: 2rem;
  margin-right: 0.75rem;
  color: var(--primary-color);
  transition: transform var(--transition-duration), color var(--transition-duration);
}

.layout-topbar-logo-text {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);
  letter-spacing: -0.025em;
  transition: color var(--transition-duration);
}

// Navigation Controls Section
.layout-topbar-menu {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex: 1;
  justify-content: flex-end;
}

.layout-menu-button {
  display: none;
  margin-right: 1rem;
}

// Dynamic Context Actions Section
.layout-topbar-context-actions {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: center;
  max-width: 600px;
  margin: 0 2rem;
}

.context-actions-separator {
  width: 1px;
  height: 2rem;
  background-color: var(--surface-border);
  margin-right: 1.5rem;
}

.context-actions-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: center;
}

.context-action-button {
  :deep(.p-button) {
    font-size: 0.875rem;
    padding: 0.5rem 1rem;
    border-radius: var(--border-radius);
    transition: all var(--transition-duration);
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    }
    
    &:active {
      transform: translateY(0);
    }
    
    .p-button-icon {
      margin-right: 0.5rem;
    }
  }
  
  // Specific action button styles
  &.action-start-crawl,
  &.action-start-all-healthy,
  &.action-analyze-articles {
    :deep(.p-button) {
      background: linear-gradient(135deg, var(--green-500), var(--green-600));
      border-color: var(--green-500);
      
      &:hover {
        background: linear-gradient(135deg, var(--green-600), var(--green-700));
        border-color: var(--green-600);
      }
    }
  }
  
  &.action-stop-all-running {
    :deep(.p-button) {
      background: linear-gradient(135deg, var(--orange-500), var(--orange-600));
      border-color: var(--orange-500);
      
      &:hover {
        background: linear-gradient(135deg, var(--orange-600), var(--orange-700));
        border-color: var(--orange-600);
      }
    }
  }
  
  &.action-health-check,
  &.action-sync-configuration,
  &.action-refresh-articles {
    :deep(.p-button) {
      background: linear-gradient(135deg, var(--blue-500), var(--blue-600));
      border-color: var(--blue-500);
      
      &:hover {
        background: linear-gradient(135deg, var(--blue-600), var(--blue-700));
        border-color: var(--blue-600);
      }
    }
  }
}

.layout-topbar-menu-section {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.layout-topbar-item {
  position: relative;
  
  &.connection-item {
    margin-right: 0.5rem;
  }
}

// Enhanced Button Styles
.layout-topbar-button {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  color: var(--text-color-secondary);
  background: transparent;
  border: none;
  border-radius: 50%;
  width: 3rem;
  height: 3rem;
  cursor: pointer;
  transition: all var(--transition-duration);
  
  &:hover {
    color: var(--text-color);
    background-color: var(--surface-hover);
    transform: translateY(-1px);
  }
  
  &:focus {
    outline: 0 none;
    outline-offset: 0;
    box-shadow: 0 0 0 2px var(--primary-color);
  }
  
  &:active {
    transform: translateY(0);
  }
  
  &.active {
    color: var(--primary-color);
    background-color: var(--primary-50);
  }
  
  i {
    font-size: 1.25rem;
    transition: transform var(--transition-duration);
  }
  
  // Specific button enhancements
  &.notification-button {
    &.has-notifications {
      color: var(--primary-color);
      
      i {
        animation: bell-ring 2s ease-in-out infinite;
      }
    }
  }
  
  &.theme-toggle {
    &.theme-changing {
      i {
        animation: theme-switch 0.3s ease-in-out;
      }
    }
    
    &.theme-dark {
      color: var(--yellow-500);
    }
    
    &.theme-light {
      color: var(--blue-600);
    }
  }
  
  &.settings-button {
    &:hover i {
      animation: settings-spin 0.5s ease-in-out;
    }
  }
}

// Enhanced Badge Styles
.layout-topbar-badge {
  position: absolute;
  top: -0.25rem;
  right: -0.25rem;
  background: linear-gradient(135deg, var(--red-500), var(--red-600));
  color: white;
  border-radius: 50%;
  padding: 0.25rem 0.5rem;
  font-size: 0.75rem;
  font-weight: 700;
  min-width: 1.5rem;
  height: 1.5rem;
  line-height: 1rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  animation: badge-pulse 2s ease-in-out infinite;
  border: 2px solid var(--surface-card);
}

// Enhanced Connection Status
.connection-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 2rem;
  font-size: 0.875rem;
  font-weight: 500;
  transition: all var(--transition-duration);
  backdrop-filter: blur(4px);
  
  &.connection-online {
    background: linear-gradient(135deg, var(--green-50), var(--green-100));
    color: var(--green-700);
    border: 1px solid var(--green-200);
    
    i {
      color: var(--green-500);
      animation: pulse-green 2s ease-in-out infinite;
    }
  }
  
  &.connection-offline {
    background: linear-gradient(135deg, var(--red-50), var(--red-100));
    color: var(--red-700);
    border: 1px solid var(--red-200);
    
    i {
      color: var(--red-500);
      animation: pulse-red 1s ease-in-out infinite;
    }
  }
}

.connection-text {
  display: none;
  font-weight: 600;
}

// Animations
@keyframes bell-ring {
  0%, 50%, 100% { transform: rotate(0deg); }
  10%, 30% { transform: rotate(-10deg); }
  20%, 40% { transform: rotate(10deg); }
}

@keyframes theme-switch {
  0% { transform: rotate(0deg) scale(1); }
  50% { transform: rotate(180deg) scale(1.1); }
  100% { transform: rotate(360deg) scale(1); }
}

@keyframes settings-spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(90deg); }
}

@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

@keyframes pulse-green {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

// Responsive Design
@media screen and (max-width: 1200px) {
  .layout-topbar {
    padding: 0 1.5rem;
  }
}

@media screen and (max-width: 991px) {
  .layout-topbar {
    justify-content: space-between;
    padding: 0 1rem;
    height: 4rem;
  }
  
  .layout-menu-button {
    display: flex !important;
  }
  
  .layout-topbar-logo-text {
    display: none;
  }
  
  .layout-topbar-logo-icon {
    margin-right: 0;
  }
  
  .connection-status {
    padding: 0.375rem 0.75rem;
    font-size: 0.8125rem;
  }
  
  .layout-topbar-menu-section {
    gap: 0.125rem;
  }
  
  .layout-topbar-button {
    width: 2.75rem;
    height: 2.75rem;
    
    i {
      font-size: 1.125rem;
    }
  }
  
  // Context actions responsive behavior
  .layout-topbar-context-actions {
    margin: 0 1rem;
    max-width: 400px;
  }
  
  .context-actions-container {
    gap: 0.5rem;
  }
  
  .context-action-button {
    :deep(.p-button) {
      font-size: 0.8125rem;
      padding: 0.375rem 0.75rem;
      
      .p-button-label {
        display: none;
      }
      
      .p-button-icon {
        margin-right: 0;
      }
    }
  }
}

@media screen and (min-width: 992px) {
  .connection-text {
    display: inline;
  }
}

@media screen and (max-width: 768px) {
  .layout-topbar {
    padding: 0 0.75rem;
  }
  
  .connection-item {
    display: none;
  }
  
  .layout-topbar-menu-section {
    gap: 0;
  }
  
  // Hide context actions on small screens
  .layout-topbar-context-actions {
    display: none;
  }
}

@media screen and (max-width: 575px) {
  .layout-topbar {
    height: 3.5rem;
    padding: 0 0.5rem;
  }
  
  .layout-topbar-logo-icon {
    font-size: 1.5rem;
  }
  
  .layout-topbar-button {
    width: 2.5rem;
    height: 2.5rem;
    
    i {
      font-size: 1rem;
    }
  }
  
  .layout-topbar-badge {
    top: -0.125rem;
    right: -0.125rem;
    min-width: 1.25rem;
    height: 1.25rem;
    font-size: 0.6875rem;
  }
}

// Dark Theme Enhancements
:global([data-theme="dark"]) {
  .layout-topbar {
    background-color: var(--surface-card);
    border-bottom-color: var(--surface-border);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
    
    &.scrolled {
      box-shadow: var(--card-shadow);
    }
  }
  
  .connection-status {
    &.connection-online {
      background: linear-gradient(135deg, var(--green-900), var(--green-800));
      color: var(--green-100);
      border-color: var(--green-700);
    }
    
    &.connection-offline {
      background: linear-gradient(135deg, var(--red-900), var(--red-800));
      color: var(--red-100);
      border-color: var(--red-700);
    }
  }
  
  .layout-topbar-button {
    &.active {
      background-color: var(--primary-900);
      color: var(--primary-200);
    }
  }
}

// High contrast mode support
@media (prefers-contrast: high) {
  .layout-topbar {
    border-bottom-width: 2px;
  }
  
  .layout-topbar-button {
    border: 1px solid transparent;
    
    &:focus {
      border-color: var(--primary-color);
      box-shadow: none;
    }
  }
  
  .connection-status {
    border-width: 2px;
  }
}

// Reduced motion support
@media (prefers-reduced-motion: reduce) {
  .layout-topbar,
  .layout-topbar-button,
  .layout-topbar-logo-link,
  .layout-topbar-logo-icon,
  .connection-status {
    transition: none;
  }
  
  .layout-topbar-button i,
  .layout-topbar-badge {
    animation: none;
  }
}
</style>