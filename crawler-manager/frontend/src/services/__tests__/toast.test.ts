import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ToastNotificationService, getToastService, toast } from '../toast'
import type { Notification } from '@/stores/notification'

// Mock PrimeVue useToast
const mockToast = {
  add: vi.fn(),
  removeAllGroups: vi.fn(),
  removeGroup: vi.fn()
}

vi.mock('primevue/usetoast', () => ({
  useToast: () => mockToast
}))

describe('ToastNotificationService', () => {
  let service: ToastNotificationService

  beforeEach(() => {
    vi.clearAllMocks()
    service = new ToastNotificationService()
  })

  describe('Basic toast methods', () => {
    it('should show success toast', () => {
      service.success('Success Title', 'Success message')

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Success Title',
        detail: 'Success message',
        life: 5000,
        closable: true,
        group: undefined
      })
    })

    it('should show info toast', () => {
      service.info('Info Title', 'Info message')

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'info',
        summary: 'Info Title',
        detail: 'Info message',
        life: 5000,
        closable: true,
        group: undefined
      })
    })

    it('should show warning toast', () => {
      service.warn('Warning Title', 'Warning message')

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'warn',
        summary: 'Warning Title',
        detail: 'Warning message',
        life: 5000,
        closable: true,
        group: undefined
      })
    })

    it('should show error toast with sticky behavior', () => {
      service.error('Error Title', 'Error message')

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Error Title',
        detail: 'Error message',
        life: 0, // Sticky
        closable: true,
        group: undefined
      })
    })
  })

  describe('Custom options', () => {
    it('should apply custom options', () => {
      service.success('Title', 'Message', {
        life: 3000,
        sticky: false,
        group: 'test-group'
      })

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Title',
        detail: 'Message',
        life: 3000,
        closable: true,
        group: 'test-group'
      })
    })

    it('should handle sticky option correctly', () => {
      service.info('Title', 'Message', {
        sticky: true
      })

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'info',
        summary: 'Title',
        detail: 'Message',
        life: 0, // Sticky means life: 0
        closable: true,
        group: undefined
      })
    })
  })

  describe('fromNotification method', () => {
    it('should create toast from notification object', () => {
      const notification: Notification = {
        id: 'test-1',
        type: 'success',
        title: 'Test Success',
        message: 'Test message',
        timestamp: new Date(),
        duration: 4000,
        persistent: false,
        autoClose: true,
        source: 'user'
      }

      service.fromNotification(notification)

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'success',
        summary: 'Test Success',
        detail: 'Test message',
        life: 4000,
        closable: true,
        group: undefined
      })
    })

    it('should handle persistent notifications as sticky', () => {
      const notification: Notification = {
        id: 'test-2',
        type: 'error',
        title: 'Test Error',
        message: 'Error message',
        timestamp: new Date(),
        persistent: true,
        autoClose: false,
        source: 'api'
      }

      service.fromNotification(notification)

      expect(mockToast.add).toHaveBeenCalledWith({
        severity: 'error',
        summary: 'Test Error',
        detail: 'Error message',
        life: 0, // Sticky
        closable: true,
        group: undefined
      })
    })

    it('should map notification types correctly', () => {
      const types: Array<{ type: Notification['type'], expected: string }> = [
        { type: 'success', expected: 'success' },
        { type: 'info', expected: 'info' },
        { type: 'warn', expected: 'warn' },
        { type: 'error', expected: 'error' }
      ]

      types.forEach(({ type, expected }) => {
        const notification: Notification = {
          id: `test-${type}`,
          type,
          title: `Test ${type}`,
          message: 'Test message',
          timestamp: new Date(),
          autoClose: true,
          source: 'user'
        }

        service.fromNotification(notification)

        expect(mockToast.add).toHaveBeenCalledWith(
          expect.objectContaining({
            severity: expected
          })
        )
      })
    })
  })

  describe('Clear methods', () => {
    it('should clear all toasts', () => {
      service.clear()
      expect(mockToast.removeAllGroups).toHaveBeenCalled()
    })

    it('should clear specific group', () => {
      service.clearGroup('test-group')
      expect(mockToast.removeGroup).toHaveBeenCalledWith('test-group')
    })
  })

  describe('Default duration mapping', () => {
    it('should use correct default durations', () => {
      // Test default durations by creating notifications without explicit duration
      const testCases = [
        { type: 'success' as const, expectedLife: 3000 },
        { type: 'info' as const, expectedLife: 5000 },
        { type: 'warn' as const, expectedLife: 7000 },
        { type: 'error' as const, expectedLife: 0 } // Error should be sticky
      ]

      testCases.forEach(({ type, expectedLife }) => {
        const notification: Notification = {
          id: `test-${type}`,
          type,
          title: `Test ${type}`,
          message: 'Test message',
          timestamp: new Date(),
          autoClose: type !== 'error',
          persistent: type === 'error',
          source: 'user'
        }

        service.fromNotification(notification)

        expect(mockToast.add).toHaveBeenCalledWith(
          expect.objectContaining({
            life: expectedLife
          })
        )
      })
    })
  })
})

describe('Singleton service', () => {
  it('should return same instance', () => {
    const service1 = getToastService()
    const service2 = getToastService()
    expect(service1).toBe(service2)
  })
})

describe('Convenience functions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should provide convenience success function', () => {
    toast.success('Success', 'Message')
    expect(mockToast.add).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'success',
        summary: 'Success',
        detail: 'Message'
      })
    )
  })

  it('should provide convenience error function', () => {
    toast.error('Error', 'Message')
    expect(mockToast.add).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        summary: 'Error',
        detail: 'Message'
      })
    )
  })

  it('should provide convenience clear function', () => {
    toast.clear()
    expect(mockToast.removeAllGroups).toHaveBeenCalled()
  })
})