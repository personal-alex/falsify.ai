import { useNotificationStore } from '@/stores/notification'
import { getToastService, type ToastNotificationOptions } from '@/services/toast'
import type { Notification } from '@/stores/notification'

/**
 * Composable for managing notifications and toast messages
 */
export function useNotifications() {
  const notificationStore = useNotificationStore()
  const toastService = getToastService()

  // Toast notification methods
  const showToast = {
    success: (summary: string, detail?: string, options?: ToastNotificationOptions) => {
      toastService.success(summary, detail, options)
    },
    
    info: (summary: string, detail?: string, options?: ToastNotificationOptions) => {
      toastService.info(summary, detail, options)
    },
    
    warn: (summary: string, detail?: string, options?: ToastNotificationOptions) => {
      toastService.warn(summary, detail, options)
    },
    
    error: (summary: string, detail?: string, options?: ToastNotificationOptions) => {
      toastService.error(summary, detail, options)
    },
    
    clear: (group?: string) => {
      toastService.clear(group)
    }
  }

  // Notification store methods with toast integration
  const notify = {
    success: (title: string, message: string, options?: Partial<Notification>) => {
      return notificationStore.success(title, message, {
        showToast: true,
        ...options
      })
    },
    
    info: (title: string, message: string, options?: Partial<Notification>) => {
      return notificationStore.info(title, message, {
        showToast: true,
        ...options
      })
    },
    
    warn: (title: string, message: string, options?: Partial<Notification>) => {
      return notificationStore.warn(title, message, {
        showToast: true,
        ...options
      })
    },
    
    error: (title: string, message: string, options?: Partial<Notification>) => {
      return notificationStore.error(title, message, {
        showToast: true,
        ...options
      })
    },
    
    // Silent notifications (no toast)
    silent: {
      success: (title: string, message: string, options?: Partial<Notification>) => {
        return notificationStore.success(title, message, {
          showToast: false,
          ...options
        })
      },
      
      info: (title: string, message: string, options?: Partial<Notification>) => {
        return notificationStore.info(title, message, {
          showToast: false,
          ...options
        })
      },
      
      warn: (title: string, message: string, options?: Partial<Notification>) => {
        return notificationStore.warn(title, message, {
          showToast: false,
          ...options
        })
      },
      
      error: (title: string, message: string, options?: Partial<Notification>) => {
        return notificationStore.error(title, message, {
          showToast: false,
          ...options
        })
      }
    }
  }

  // API error handler with toast
  const handleApiError = (error: any, context?: string) => {
    return notificationStore.handleApiError(error, context)
  }

  // WebSocket connection status notifications
  const handleConnectionStatus = (connected: boolean, reconnectAttempts: number = 0) => {
    if (connected && reconnectAttempts > 0) {
      showToast.success('Connection Restored', 'Real-time updates are now available')
    } else if (!connected && reconnectAttempts === 0) {
      showToast.warn('Connection Lost', 'Attempting to reconnect...', { sticky: true })
    }
  }

  // Crawler-specific notifications
  const crawlerNotifications = {
    jobStarted: (crawlerId: string, jobId: string) => {
      notify.info(
        'Crawl Started',
        `Job ${jobId} has started for ${crawlerId}`,
        { crawlerId, duration: 3000 }
      )
    },
    
    jobCompleted: (crawlerId: string, jobId: string, articlesProcessed: number) => {
      notify.success(
        'Crawl Completed',
        `Job ${jobId} completed successfully. Processed ${articlesProcessed} articles.`,
        { crawlerId, duration: 5000 }
      )
    },
    
    jobFailed: (crawlerId: string, jobId: string, error?: string) => {
      notify.error(
        'Crawl Failed',
        `Job ${jobId} failed${error ? `: ${error}` : ''}`,
        { crawlerId, persistent: true }
      )
    },
    
    healthChanged: (crawlerId: string, status: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN', message?: string) => {
      const severity = status === 'HEALTHY' ? 'success' : status === 'UNHEALTHY' ? 'error' : 'warn'
      const title = `${crawlerId} ${status === 'HEALTHY' ? 'Healthy' : status === 'UNHEALTHY' ? 'Unhealthy' : 'Status Unknown'}`
      
      if (severity === 'success') {
        notify.success(title, message || 'Crawler is operating normally', { crawlerId })
      } else if (severity === 'error') {
        notify.error(title, message || 'Crawler is experiencing issues', { crawlerId })
      } else {
        notify.warn(title, message || 'Crawler status is unknown', { crawlerId })
      }
    }
  }

  return {
    // Store access
    notifications: notificationStore.notifications,
    unreadCount: notificationStore.unreadCount,
    recentNotifications: notificationStore.recentNotifications,
    
    // Toast methods
    showToast,
    
    // Notification methods
    notify,
    
    // Utility methods
    handleApiError,
    handleConnectionStatus,
    crawlerNotifications,
    
    // Store methods
    clearNotifications: notificationStore.clearAllNotifications,
    clearToasts: notificationStore.clearToasts,
    removeNotification: notificationStore.removeNotification,
    initWebSocket: notificationStore.initWebSocket,
    disconnectWebSocket: notificationStore.disconnectWebSocket
  }
}

// Type exports for convenience
export type { Notification, ToastNotificationOptions }