<template>
  <div class="layout-wrapper" :class="containerClass">
    <AppTopbar @menu-toggle="onMenuToggle" />
    
    <div class="layout-sidebar" @click="onSidebarClick">
      <AppSidebar />
    </div>
    
    <div class="layout-main-container">
      <div class="layout-main">
        <!-- Breadcrumb Navigation -->
        <div class="breadcrumb-container" v-if="showBreadcrumb">
          <Breadcrumb :model="breadcrumbItems" class="layout-breadcrumb">
            <template #item="{ item }">
              <router-link v-if="item.to" :to="item.to" class="breadcrumb-link">
                <i v-if="item.icon" :class="item.icon"></i>
                <span>{{ item.label }}</span>
              </router-link>
              <span v-else class="breadcrumb-current">
                <i v-if="item.icon" :class="item.icon"></i>
                <span>{{ item.label }}</span>
              </span>
            </template>
          </Breadcrumb>
        </div>

        <router-view v-slot="{ Component, route }">
          <transition name="layout" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </div>
    </div>
    
    <div class="layout-mask" @click="hideMenu" v-if="isOverlayMenuActive"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLayoutStore } from '@/stores/layout'
import { useNotifications } from '@/composables/useNotifications'
import AppTopbar from './AppTopbar.vue'
import AppSidebar from './AppSidebar.vue'
import Breadcrumb from 'primevue/breadcrumb'

const route = useRoute()
const layoutStore = useLayoutStore()
const { initWebSocket, disconnectWebSocket } = useNotifications()

// Computed properties from store
const { 
  containerClass, 
  isOverlayMenuActive 
} = layoutStore

// Methods from store
const { 
  onMenuToggle, 
  onSidebarClick, 
  setOverlayMenuActive,
  initializeLayout 
} = layoutStore

// Breadcrumb navigation
const showBreadcrumb = computed(() => {
  const breadcrumb = route.meta?.breadcrumb as any[]
  return breadcrumb && breadcrumb.length > 1
})

const breadcrumbItems = computed(() => {
  const breadcrumb = route.meta?.breadcrumb as any[]
  if (!breadcrumb) return []
  
  return breadcrumb.map((item: any, index: number) => ({
    ...item,
    // Don't make the last item clickable
    to: index === breadcrumb.length - 1 ? undefined : item.to
  }))
})

const hideMenu = () => {
  setOverlayMenuActive(false)
}

let cleanupLayout: (() => void) | null = null

onMounted(() => {
  cleanupLayout = initializeLayout()
  // Initialize WebSocket for notifications
  initWebSocket()
})

onUnmounted(() => {
  if (cleanupLayout) {
    cleanupLayout()
  }
  // Disconnect WebSocket
  disconnectWebSocket()
})
</script>

<style lang="scss">
.layout-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
  
  &.layout-static {
    .layout-sidebar {
      position: fixed;
      width: 300px;
      height: 100vh;
      left: 0;
      top: 0;
      z-index: 999;
      transition: transform 0.2s, left 0.2s;
    }
    
    .layout-main-container {
      margin-left: 300px;
      min-height: 100vh;
      transition: margin-left 0.2s;
    }
    
    &.layout-static-inactive {
      .layout-sidebar {
        transform: translateX(-100%);
      }
      
      .layout-main-container {
        margin-left: 0;
      }
    }
  }
  
  &.layout-overlay {
    .layout-sidebar {
      position: fixed;
      width: 300px;
      height: 100vh;
      left: -300px;
      top: 0;
      z-index: 999;
      transition: left 0.2s;
    }
    
    .layout-main-container {
      margin-left: 0;
      min-height: 100vh;
    }
    
    &.layout-overlay-active {
      .layout-sidebar {
        left: 0;
      }
    }
  }
  
  &.layout-mobile-active {
    .layout-mask {
      background-color: rgba(0, 0, 0, 0.4);
    }
  }
}

.layout-main-container {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.layout-main {
  flex: 1;
  padding-top: 7rem;
  background-color: var(--surface-ground);
  height: calc(100vh - 7rem);
  overflow-y: auto;
}

.breadcrumb-container {
  background-color: var(--surface-card);
  border-bottom: 1px solid var(--surface-border);
  padding: 1rem 2rem;
  margin-bottom: 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.layout-breadcrumb {
  :deep(.p-breadcrumb) {
    background: transparent;
    border: none;
    padding: 0;
  }

  :deep(.p-breadcrumb-list) {
    margin: 0;
    padding: 0;
  }

  .breadcrumb-link {
    color: var(--primary-color);
    text-decoration: none;
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.25rem 0.5rem;
    border-radius: 4px;
    transition: background-color 0.15s;

    &:hover {
      background-color: var(--surface-hover);
    }

    i {
      font-size: 0.875rem;
    }
  }

  .breadcrumb-current {
    color: var(--text-color);
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.25rem 0.5rem;
    font-weight: 500;

    i {
      font-size: 0.875rem;
    }
  }
}

.layout-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 998;
  background-color: transparent;
  transition: background-color 0.2s;
}

// Page transition animations
.layout-enter-active,
.layout-leave-active {
  transition: opacity 0.2s ease-in-out;
}

.layout-enter-from,
.layout-leave-to {
  opacity: 0;
}

// Responsive design
@media screen and (max-width: 991px) {
  .layout-wrapper {
    &.layout-static {
      .layout-sidebar {
        position: fixed;
        left: -300px;
        transition: left 0.2s;
      }
      
      .layout-main-container {
        margin-left: 0;
      }
      
      &.layout-mobile-active {
        .layout-sidebar {
          left: 0;
        }
      }
    }
    
    &.layout-overlay {
      .layout-sidebar {
        left: -300px;
      }
      
      &.layout-overlay-active {
        .layout-sidebar {
          left: 0;
        }
      }
    }
  }
  
  .layout-main {
    padding-top: 5rem;
    height: calc(100vh - 5rem);
  }

  .breadcrumb-container {
    padding: 0.75rem 1.5rem;
  }
}

@media screen and (max-width: 575px) {
  .layout-wrapper {
    .layout-sidebar {
      width: 100% !important;
      left: -100% !important;
      
      &.layout-mobile-active {
        left: 0 !important;
      }
    }
  }
  
  .layout-main {
    padding-top: 4rem;
    height: calc(100vh - 4rem);
  }

  .breadcrumb-container {
    padding: 0.5rem 1rem;
  }
}
</style>