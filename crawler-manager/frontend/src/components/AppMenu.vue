<template>
  <ul class="layout-menu">
    <template v-for="item in model" :key="item.label || Math.random().toString(36)">
      <li v-if="item.separator" class="layout-menuitem-separator"></li>
      <li v-else-if="item.visible !== false" class="layout-menuitem" :class="{ 'active-menuitem': isActiveMenuItem(item) }">
        <div 
          v-if="item.items" 
          class="layout-menuitem-root-text"
          @click="onMenuItemClick($event, item)"
          @keydown.enter="onMenuItemClick($event, item)"
          @keydown.space.prevent="onMenuItemClick($event, item)"
          tabindex="0"
          role="button"
          :aria-expanded="isActiveMenuItem(item)"
          :aria-label="`${item.label} menu section`"
        >
          {{ item.label }}
          <i 
            class="layout-menuitem-toggle-icon pi"
            :class="isActiveMenuItem(item) ? 'pi-chevron-down' : 'pi-chevron-right'"
          ></i>
        </div>
        <a
          v-else
          :href="item.to || '#'"
          @click="onMenuItemClick($event, item)"
          @keydown.enter="onMenuItemClick($event, item)"
          @keydown.space.prevent="onMenuItemClick($event, item)"
          class="layout-menuitem-link"
          :class="{ 'active-route': isActiveRoute(item) }"
          :aria-label="item.label"
          tabindex="0"
          role="menuitem"
        >
          <i :class="item.icon" class="layout-menuitem-icon"></i>
          <span class="layout-menuitem-text">{{ item.label }}</span>
          <Badge
            v-if="item.badge"
            :value="item.badge"
            :class="item.badgeClass"
            class="layout-menuitem-badge"
          />
        </a>
        
        <transition name="layout-submenu">
          <ul v-if="item.items && isActiveMenuItem(item)" class="layout-submenu">
            <li v-for="child in item.items" :key="child.label || Math.random().toString(36)" class="layout-submenuitem">
              <a
                :href="child.to || '#'"
                @click="onMenuItemClick($event, child)"
                @keydown.enter="onMenuItemClick($event, child)"
                @keydown.space.prevent="onMenuItemClick($event, child)"
                class="layout-submenuitem-link"
                :class="{ 'active-route': isActiveRoute(child) }"
                :aria-label="child.label"
                tabindex="0"
                role="menuitem"
              >
                <i :class="child.icon" class="layout-submenuitem-icon"></i>
                <span class="layout-submenuitem-text">{{ child.label }}</span>
                <Badge
                  v-if="child.badge"
                  :value="child.badge"
                  :class="child.badgeClass"
                  class="layout-submenuitem-badge"
                />
              </a>
            </li>
          </ul>
        </transition>
      </li>
    </template>
  </ul>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import Badge from 'primevue/badge'
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

// Props
interface Props {
  model: MenuItem[]
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'menuitem-click': [event: { originalEvent: Event; item: MenuItem }]
}>()

// State
const route = useRoute()
const activeMenuItems = ref<Set<string>>(new Set())

// Computed
const currentPath = computed(() => route.path)

// Methods
const isActiveRoute = (item: MenuItem): boolean => {
  if (!item.to) return false
  
  if (item.to === '/') {
    return currentPath.value === '/'
  }
  
  return currentPath.value.startsWith(item.to)
}

const isActiveMenuItem = (item: MenuItem): boolean => {
  if (item.items) {
    // Check if any child item is active
    const hasActiveChild = item.items.some(child => isActiveRoute(child))
    if (hasActiveChild && item.label) {
      activeMenuItems.value.add(item.label)
      return true
    }
    
    // Check if this menu item was manually activated
    return item.label ? activeMenuItems.value.has(item.label) : false
  }
  
  return isActiveRoute(item)
}

const onMenuItemClick = (event: Event, item: MenuItem) => {
  if (item.items && item.label) {
    // Toggle submenu
    event.preventDefault()
    
    if (activeMenuItems.value.has(item.label)) {
      activeMenuItems.value.delete(item.label)
    } else {
      activeMenuItems.value.add(item.label)
    }
  } else if (item.to) {
    // Navigate to route
    event.preventDefault()
    emit('menuitem-click', { originalEvent: event, item })
  }
}

// Initialize active menu items based on current route
const initializeActiveMenuItems = () => {
  const findActiveParent = (items: MenuItem[], path: string): string | null => {
    for (const item of items) {
      if (item.items && item.label) {
        const hasActiveChild = item.items.some(child => 
          child.to && (child.to === path || (child.to !== '/' && path.startsWith(child.to)))
        )
        if (hasActiveChild) {
          return item.label
        }
      }
    }
    return null
  }
  
  const activeParent = findActiveParent(props.model, currentPath.value)
  if (activeParent) {
    activeMenuItems.value.add(activeParent)
  }
}

// Initialize on mount
initializeActiveMenuItems()
</script>

<style lang="scss" scoped>
.layout-menu {
  margin: 0;
  padding: 0;
  list-style-type: none;
}

.layout-menuitem {
  position: relative;
}

.layout-menuitem-separator {
  border-top: 1px solid var(--surface-border);
  margin: 1rem 0;
}

.layout-menuitem-root-text {
  font-weight: 600;
  font-size: 0.875rem;
  letter-spacing: 0.5px;
  color: var(--text-color-secondary);
  margin: 2rem 0 1rem 2rem;
  text-transform: uppercase;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 0;
  border-radius: 6px;
  transition: all 0.15s ease;
  user-select: none;
  min-height: 44px; // Minimum touch target size

  &:hover {
    background-color: var(--surface-hover);
    color: var(--text-color);
  }

  &:focus {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }
}

.layout-menuitem-link {
  padding: 1rem 2rem;
  color: var(--text-color);
  border-radius: 6px;
  margin: 0 1rem;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  position: relative;
  text-decoration: none;
  overflow: hidden;
  cursor: pointer;
  min-height: 44px; // Minimum touch target size
  
  &:hover {
    background-color: var(--surface-hover);
    transform: translateX(4px);
  }
  
  &:focus {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }
  
  &.active-route {
    font-weight: 700;
    background-color: var(--highlight-bg);
    color: var(--highlight-text-color);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .layout-menuitem-icon {
      color: var(--highlight-text-color);
    }

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 4px;
      background-color: var(--primary-color);
      border-radius: 0 2px 2px 0;
    }
  }
}

.layout-menuitem-icon {
  margin-right: 0.5rem;
  color: var(--text-color-secondary);
  transition: color 0.15s;
}

.layout-menuitem-text {
  font-weight: 500;
  flex: 1;
}

.layout-menuitem-badge {
  margin-left: auto;
}

.layout-submenu {
  margin: 0;
  padding: 0;
  list-style-type: none;
  overflow: hidden;
}

.layout-submenuitem {
  position: relative;
}

.layout-submenuitem-link {
  padding: 0.75rem 2rem 0.75rem 4rem;
  color: var(--text-color);
  border-radius: 6px;
  margin: 0 1rem;
  transition: all 0.15s ease;
  display: flex;
  align-items: center;
  position: relative;
  text-decoration: none;
  overflow: hidden;
  cursor: pointer;
  min-height: 40px; // Slightly smaller for sub-items
  
  &:hover {
    background-color: var(--surface-hover);
    transform: translateX(8px);
  }
  
  &:focus {
    outline: 2px solid var(--primary-color);
    outline-offset: 2px;
  }
  
  &.active-route {
    font-weight: 600;
    background-color: var(--highlight-bg);
    color: var(--highlight-text-color);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
    
    .layout-submenuitem-icon {
      color: var(--highlight-text-color);
    }

    &::before {
      content: '';
      position: absolute;
      left: 2rem;
      top: 0;
      bottom: 0;
      width: 3px;
      background-color: var(--primary-color);
      border-radius: 0 2px 2px 0;
    }
  }
}

.layout-submenuitem-icon {
  margin-right: 0.5rem;
  color: var(--text-color-secondary);
  transition: color 0.15s;
}

.layout-submenuitem-text {
  font-weight: 500;
  flex: 1;
}

.layout-submenuitem-badge {
  margin-left: auto;
}

.layout-menuitem-toggle-icon {
  margin-left: auto;
  font-size: 0.75rem;
  transition: transform 0.2s ease;
  color: var(--text-color-secondary);
}

// Submenu transition
.layout-submenu-enter-active,
.layout-submenu-leave-active {
  transition: all 0.2s ease;
  max-height: 1000px;
}

.layout-submenu-enter-from,
.layout-submenu-leave-to {
  max-height: 0;
  opacity: 0;
}

// Badge styling
:deep(.p-badge) {
  min-width: 1.5rem;
  height: 1.5rem;
  line-height: 1.5rem;
  color: #ffffff;
  text-align: center;
  padding: 0 0.5rem;
  
  &.p-badge-info {
    background-color: var(--blue-500);
  }
  
  &.p-badge-success {
    background-color: var(--green-500);
  }
  
  &.p-badge-warning {
    background-color: var(--orange-500);
  }
  
  &.p-badge-danger {
    background-color: var(--red-500);
  }
}

// Mobile responsive adjustments
@media (max-width: 991px) {
  .layout-menuitem-link,
  .layout-submenuitem-link {
    min-height: 48px; // Larger touch targets on mobile
    
    &:hover {
      transform: none; // Disable hover transforms on mobile
    }
  }

  .layout-menuitem-root-text {
    min-height: 48px;
    padding: 0.75rem 0;
  }
}

@media (max-width: 576px) {
  .layout-menuitem-link {
    padding: 1rem 1.5rem;
    margin: 0 0.5rem;
  }

  .layout-submenuitem-link {
    padding: 0.75rem 1.5rem 0.75rem 3rem;
    margin: 0 0.5rem;
  }

  .layout-menuitem-root-text {
    margin: 1.5rem 0 0.5rem 1.5rem;
  }
}

// High contrast mode support
@media (prefers-contrast: high) {
  .layout-menuitem-link,
  .layout-submenuitem-link {
    border: 1px solid var(--surface-border);
    
    &.active-route {
      border-color: var(--primary-color);
      border-width: 2px;
    }
  }
}

// Reduced motion support
@media (prefers-reduced-motion: reduce) {
  .layout-menuitem-link,
  .layout-submenuitem-link,
  .layout-menuitem-toggle-icon,
  .layout-submenu {
    transition: none;
  }

  .layout-menuitem-link:hover,
  .layout-submenuitem-link:hover {
    transform: none;
  }
}

// Dark theme adjustments
:global([data-theme="dark"]) {
  .layout-menuitem-root-text {
    color: var(--text-color-secondary);
    
    &:hover {
      color: var(--text-color);
    }
  }
  
  .layout-menuitem-link,
  .layout-submenuitem-link {
    color: var(--text-color);
    
    &:hover {
      background-color: var(--surface-hover);
    }
    
    &.active-route {
      background-color: var(--highlight-bg);
      color: var(--highlight-text-color);
    }
  }
  
  .layout-menuitem-icon,
  .layout-submenuitem-icon {
    color: var(--text-color-secondary);
  }

  .layout-menuitem-toggle-icon {
    color: var(--text-color-secondary);
  }
}
</style>