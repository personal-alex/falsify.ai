import { useToast } from 'primevue/usetoast'
import type { ToastMessageOptions } from 'primevue/toast'
import type { Notification } from '@/stores/notification'

export interface ToastNotificationOptions extends Omit<ToastMessageOptions, 'severity' | 'summary' | 'detail'> {
  position?: 'top-left' | 'top-center' | 'top-right' | 'bottom-left' | 'bottom-center' | 'bottom-right'
  group?: string
  closable?: boolean
  sticky?: boolean
}

export class ToastNotificationService {
  private toast = useToast()
  private defaultOptions: ToastNotificationOptions = {
    position: 'top-right',
    closable: true,
    sticky: false,
    life: 5000
  }

  constructor(options?: Partial<ToastNotificationOptions>) {
    if (options) {
      this.defaultOptions = { ...this.defaultOptions, ...options }
    }
  }

  /**
   * Show a success toast notification
   */
  success(summary: string, detail?: string, options?: ToastNotificationOptions) {
    this.show('success', summary, detail, options)
  }

  /**
   * Show an info toast notification
   */
  info(summary: string, detail?: string, options?: ToastNotificationOptions) {
    this.show('info', summary, detail, options)
  }

  /**
   * Show a warning toast notification
   */
  warn(summary: string, detail?: string, options?: ToastNotificationOptions) {
    this.show('warn', summary, detail, options)
  }

  /**
   * Show an error toast notification
   */
  error(summary: string, detail?: string, options?: ToastNotificationOptions) {
    const errorOptions = {
      life: 8000, // Errors stay longer
      sticky: true,
      ...options
    }
    this.show('error', summary, detail, errorOptions)
  }

  /**
   * Show a toast notification from a Notification object
   */
  fromNotification(notification: Notification, options?: ToastNotificationOptions) {
    const toastOptions: ToastNotificationOptions = {
      life: notification.duration || this.getDefaultDuration(notification.type),
      sticky: notification.persistent || notification.type === 'error',
      ...options
    }

    this.show(
      this.mapNotificationTypeToSeverity(notification.type),
      notification.title,
      notification.message,
      toastOptions
    )
  }

  /**
   * Show a custom toast notification
   */
  show(
    severity: 'success' | 'info' | 'warn' | 'error',
    summary: string,
    detail?: string,
    options?: ToastNotificationOptions
  ) {
    const finalOptions = { ...this.defaultOptions, ...options }
    
    const toastMessage: ToastMessageOptions = {
      severity,
      summary,
      detail,
      life: finalOptions.sticky ? 0 : finalOptions.life,
      closable: finalOptions.closable,
      group: finalOptions.group
    }

    this.toast.add(toastMessage)
  }

  /**
   * Clear all toast notifications
   */
  clear(_group?: string) {
    this.toast.removeAllGroups()
  }

  /**
   * Clear toast notifications by group
   */
  clearGroup(group: string) {
    this.toast.removeGroup(group)
  }

  /**
   * Map notification type to PrimeVue severity
   */
  private mapNotificationTypeToSeverity(type: Notification['type']): 'success' | 'info' | 'warn' | 'error' {
    switch (type) {
      case 'success':
        return 'success'
      case 'info':
        return 'info'
      case 'warn':
        return 'warn'
      case 'error':
        return 'error'
      default:
        return 'info'
    }
  }

  /**
   * Get default duration based on notification type
   */
  private getDefaultDuration(type: Notification['type']): number {
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
}

// Singleton instance
let toastServiceInstance: ToastNotificationService | null = null

/**
 * Get the global toast notification service instance
 */
export function getToastService(): ToastNotificationService {
  if (!toastServiceInstance) {
    toastServiceInstance = new ToastNotificationService()
  }
  return toastServiceInstance
}

/**
 * Create a new toast notification service with custom options
 */
export function createToastService(options?: Partial<ToastNotificationOptions>): ToastNotificationService {
  return new ToastNotificationService(options)
}

// Convenience functions for direct use
export const toast = {
  success: (summary: string, detail?: string, options?: ToastNotificationOptions) => 
    getToastService().success(summary, detail, options),
  
  info: (summary: string, detail?: string, options?: ToastNotificationOptions) => 
    getToastService().info(summary, detail, options),
  
  warn: (summary: string, detail?: string, options?: ToastNotificationOptions) => 
    getToastService().warn(summary, detail, options),
  
  error: (summary: string, detail?: string, options?: ToastNotificationOptions) => 
    getToastService().error(summary, detail, options),
  
  show: (severity: 'success' | 'info' | 'warn' | 'error', summary: string, detail?: string, options?: ToastNotificationOptions) => 
    getToastService().show(severity, summary, detail, options),
  
  clear: (group?: string) => 
    getToastService().clear(group),
  
  clearGroup: (group: string) => 
    getToastService().clearGroup(group)
}