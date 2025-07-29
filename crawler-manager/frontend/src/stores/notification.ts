import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getWebSocketService, type NotificationMessage } from '@/services/websocket'
import { getToastService } from '@/services/toast'

export interface Notification {
  id: string
  type: 'success' | 'info' | 'warn' | 'error'
  title: string
  message: string
  timestamp: Date
  duration?: number
  persistent?: boolean
  autoClose?: boolean
  crawlerId?: string
  source?: 'user' | 'websocket' | 'api'
  actions?: NotificationAction[]
  showToast?: boolean // Whether to show as toast notification
}

export interface NotificationAction {
  label: string
  action: () => void
  style?: 'primary' | 'secondary' | 'danger'
}

export const useNotificationStore = defineStore('notification', () => {
  // State
  const notifications = ref<Notification[]>([])
  const maxNotifications = ref(50) // Increased for better history
  
  // Services
  const wsService = getWebSocketService()
  const toastService = getToastService()
  let wsUnsubscribers: (() => void)[] = []

  // Getters
  const unreadCount = computed(() => 
    notifications.value.filter(n => n.persistent && !n.autoClose).length
  )

  const notificationsByType = computed(() => {
    return {
      success: notifications.value.filter(n => n.type === 'success'),
      info: notifications.value.filter(n => n.type === 'info'),
      warn: notifications.value.filter(n => n.type === 'warn'),
      error: notifications.value.filter(n => n.type === 'error')
    }
  })

  const recentNotifications = computed(() => 
    notifications.value.slice(0, 10)
  )

  // Actions
  const addNotification = (notification: Omit<Notification, 'id' | 'timestamp'>) => {
    const newNotification: Notification = {
      ...notification,
      id: generateId(),
      timestamp: new Date(),
      autoClose: notification.autoClose ?? !notification.persistent,
      source: notification.source ?? 'user',
      showToast: notification.showToast ?? true // Default to showing toast
    }

    notifications.value.unshift(newNotification)

    // Show toast notification if enabled
    if (newNotification.showToast) {
      toastService.fromNotification(newNotification)
    }

    // Limit the number of notifications
    if (notifications.value.length > maxNotifications.value) {
      notifications.value = notifications.value.slice(0, maxNotifications.value)
    }

    // Auto-remove non-persistent notifications
    if (newNotification.autoClose) {
      const duration = newNotification.duration || getDefaultDuration(newNotification.type)
      setTimeout(() => {
        removeNotification(newNotification.id)
      }, duration)
    }

    return newNotification.id
  }

  const removeNotification = (id: string) => {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index > -1) {
      notifications.value.splice(index, 1)
    }
  }

  const markAsRead = (id: string) => {
    const notification = notifications.value.find(n => n.id === id)
    if (notification) {
      notification.autoClose = true
    }
  }

  const clearAllNotifications = () => {
    notifications.value = []
  }

  const clearNotificationsByType = (type: Notification['type']) => {
    notifications.value = notifications.value.filter(n => n.type !== type)
  }

  const clearByCrawler = (crawlerId: string) => {
    notifications.value = notifications.value.filter(n => n.crawlerId !== crawlerId)
  }

  const clearRead = () => {
    notifications.value = notifications.value.filter(n => !n.autoClose)
  }

  const clearToasts = () => {
    toastService.clear()
  }

  // WebSocket integration
  const handleWebSocketNotification = (message: NotificationMessage) => {
    addNotification({
      type: message.level,
      title: message.title,
      message: message.message,
      crawlerId: message.crawlerId,
      autoClose: message.autoClose,
      persistent: !message.autoClose,
      source: 'websocket',
      showToast: true // Always show WebSocket notifications as toasts
    })
  }

  const initWebSocket = () => {
    // Clean up existing subscriptions
    wsUnsubscribers.forEach(unsub => unsub())
    wsUnsubscribers = []

    // Subscribe to WebSocket notifications
    wsUnsubscribers.push(
      wsService.onNotification(handleWebSocketNotification)
    )

    // Subscribe to connection events for system notifications
    wsUnsubscribers.push(
      wsService.onConnectionStatusChange((status) => {
        if (status.connected && status.reconnectAttempts > 0) {
          addNotification({
            type: 'success',
            title: 'Connection Restored',
            message: 'Real-time updates are now available',
            source: 'websocket',
            showToast: true
          })
        } else if (!status.connected && status.reconnectAttempts === 0) {
          addNotification({
            type: 'warn',
            title: 'Connection Lost',
            message: 'Attempting to reconnect...',
            persistent: true,
            autoClose: false,
            source: 'websocket',
            showToast: true
          })
        }
      })
    )
  }

  const disconnectWebSocket = () => {
    wsUnsubscribers.forEach(unsub => unsub())
    wsUnsubscribers = []
  }

  // Convenience methods for different notification types
  const success = (title: string, message: string, options?: Partial<Notification>) => {
    return addNotification({
      type: 'success',
      title,
      message,
      ...options
    })
  }

  const info = (title: string, message: string, options?: Partial<Notification>) => {
    return addNotification({
      type: 'info',
      title,
      message,
      ...options
    })
  }

  const warn = (title: string, message: string, options?: Partial<Notification>) => {
    return addNotification({
      type: 'warn',
      title,
      message,
      ...options
    })
  }

  const error = (title: string, message: string, options?: Partial<Notification>) => {
    return addNotification({
      type: 'error',
      title,
      message,
      duration: 8000, // Errors stay longer by default
      persistent: true,
      autoClose: false,
      ...options
    })
  }

  // API error handler
  const handleApiError = (error: any, context?: string) => {
    const title = context ? `${context} Failed` : 'Operation Failed'
    const message = error?.message || 'An unexpected error occurred'
    
    return addNotification({
      type: 'error',
      title,
      message,
      persistent: true,
      autoClose: false,
      source: 'api'
    })
  }

  // Helper functions
  const generateId = () => {
    return `notification-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
  }

  const getDefaultDuration = (type: Notification['type']) => {
    switch (type) {
      case 'success':
        return 3000
      case 'info':
        return 5000
      case 'warn':
        return 7000
      case 'error':
        return 0 // Don't auto-close errors
      default:
        return 5000
    }
  }

  return {
    // State
    notifications,
    maxNotifications,
    
    // Getters
    unreadCount,
    notificationsByType,
    recentNotifications,
    
    // Actions
    addNotification,
    removeNotification,
    markAsRead,
    clearAllNotifications,
    clearNotificationsByType,
    clearByCrawler,
    clearRead,
    clearToasts,
    initWebSocket,
    disconnectWebSocket,
    
    // Convenience methods
    success,
    info,
    warn,
    error,
    handleApiError
  }
})