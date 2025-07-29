import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useNotifications } from '../useNotifications'

// Mock the notification store
const mockNotificationStore = {
  notifications: [],
  unreadCount: 0,
  recentNotifications: [],
  success: vi.fn(),
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  handleApiError: vi.fn(),
  clearAllNotifications: vi.fn(),
  clearToasts: vi.fn(),
  removeNotification: vi.fn(),
  initWebSocket: vi.fn(),
  disconnectWebSocket: vi.fn()
}

vi.mock('@/stores/notification', () => ({
  useNotificationStore: () => mockNotificationStore
}))

// Mock the toast service
const mockToastService = {
  success: vi.fn(),
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  clear: vi.fn()
}

vi.mock('@/services/toast', () => ({
  getToastService: () => mockToastService
}))

describe('useNotifications', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('Toast methods', () => {
    it('should provide showToast methods', () => {
      const { showToast } = useNotifications()

      showToast.success('Success', 'Message')
      expect(mockToastService.success).toHaveBeenCalledWith('Success', 'Message', undefined)

      showToast.error('Error', 'Message')
      expect(mockToastService.error).toHaveBeenCalledWith('Error', 'Message', undefined)

      showToast.clear()
      expect(mockToastService.clear).toHaveBeenCalledWith(undefined)
    })
  })

  describe('Notification methods', () => {
    it('should provide notify methods with toast enabled', () => {
      const { notify } = useNotifications()

      notify.success('Title', 'Message')
      expect(mockNotificationStore.success).toHaveBeenCalledWith('Title', 'Message', {
        showToast: true
      })

      notify.error('Title', 'Message', { persistent: true })
      expect(mockNotificationStore.error).toHaveBeenCalledWith('Title', 'Message', {
        showToast: true,
        persistent: true
      })
    })

    it('should provide silent notification methods', () => {
      const { notify } = useNotifications()

      notify.silent.success('Title', 'Message')
      expect(mockNotificationStore.success).toHaveBeenCalledWith('Title', 'Message', {
        showToast: false
      })

      notify.silent.error('Title', 'Message')
      expect(mockNotificationStore.error).toHaveBeenCalledWith('Title', 'Message', {
        showToast: false
      })
    })
  })

  describe('API error handling', () => {
    it('should handle API errors', () => {
      const { handleApiError } = useNotifications()
      const error = new Error('Test error')

      handleApiError(error, 'Test operation')
      expect(mockNotificationStore.handleApiError).toHaveBeenCalledWith(error, 'Test operation')
    })
  })

  describe('Connection status handling', () => {
    it('should handle connection restored', () => {
      const { handleConnectionStatus } = useNotifications()

      handleConnectionStatus(true, 1)
      expect(mockToastService.success).toHaveBeenCalledWith(
        'Connection Restored',
        'Real-time updates are now available',
        undefined
      )
    })

    it('should handle connection lost', () => {
      const { handleConnectionStatus } = useNotifications()

      handleConnectionStatus(false, 0)
      expect(mockToastService.warn).toHaveBeenCalledWith(
        'Connection Lost',
        'Attempting to reconnect...',
        { sticky: true }
      )
    })
  })

  describe('Crawler notifications', () => {
    it('should handle job started notification', () => {
      const { crawlerNotifications } = useNotifications()

      crawlerNotifications.jobStarted('crawler-1', 'job-123')
      expect(mockNotificationStore.info).toHaveBeenCalledWith(
        'Crawl Started',
        'Job job-123 has started for crawler-1',
        { crawlerId: 'crawler-1', duration: 3000, showToast: true }
      )
    })

    it('should handle job completed notification', () => {
      const { crawlerNotifications } = useNotifications()

      crawlerNotifications.jobCompleted('crawler-1', 'job-123', 42)
      expect(mockNotificationStore.success).toHaveBeenCalledWith(
        'Crawl Completed',
        'Job job-123 completed successfully. Processed 42 articles.',
        { crawlerId: 'crawler-1', duration: 5000, showToast: true }
      )
    })

    it('should handle job failed notification', () => {
      const { crawlerNotifications } = useNotifications()

      crawlerNotifications.jobFailed('crawler-1', 'job-123', 'Network error')
      expect(mockNotificationStore.error).toHaveBeenCalledWith(
        'Crawl Failed',
        'Job job-123 failed: Network error',
        { crawlerId: 'crawler-1', persistent: true, showToast: true }
      )
    })

    it('should handle health status changes', () => {
      const { crawlerNotifications } = useNotifications()

      // Healthy status
      crawlerNotifications.healthChanged('crawler-1', 'HEALTHY', 'All systems operational')
      expect(mockNotificationStore.success).toHaveBeenCalledWith(
        'crawler-1 Healthy',
        'All systems operational',
        { crawlerId: 'crawler-1', showToast: true }
      )

      // Unhealthy status
      crawlerNotifications.healthChanged('crawler-1', 'UNHEALTHY', 'Connection timeout')
      expect(mockNotificationStore.error).toHaveBeenCalledWith(
        'crawler-1 Unhealthy',
        'Connection timeout',
        { crawlerId: 'crawler-1', showToast: true }
      )

      // Unknown status
      crawlerNotifications.healthChanged('crawler-1', 'UNKNOWN')
      expect(mockNotificationStore.warn).toHaveBeenCalledWith(
        'crawler-1 Status Unknown',
        'Crawler status is unknown',
        { crawlerId: 'crawler-1', showToast: true }
      )
    })
  })

  describe('Store integration', () => {
    it('should expose store properties and methods', () => {
      const {
        notifications,
        unreadCount,
        recentNotifications,
        clearNotifications,
        clearToasts,
        removeNotification,
        initWebSocket,
        disconnectWebSocket
      } = useNotifications()

      expect(notifications).toBe(mockNotificationStore.notifications)
      expect(unreadCount).toBe(mockNotificationStore.unreadCount)
      expect(recentNotifications).toBe(mockNotificationStore.recentNotifications)

      clearNotifications()
      expect(mockNotificationStore.clearAllNotifications).toHaveBeenCalled()

      clearToasts()
      expect(mockNotificationStore.clearToasts).toHaveBeenCalled()

      removeNotification('test-id')
      expect(mockNotificationStore.removeNotification).toHaveBeenCalledWith('test-id')

      initWebSocket()
      expect(mockNotificationStore.initWebSocket).toHaveBeenCalled()

      disconnectWebSocket()
      expect(mockNotificationStore.disconnectWebSocket).toHaveBeenCalled()
    })
  })
})