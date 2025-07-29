import { describe, it, expect, vi } from 'vitest'
import type { 
  HealthUpdateMessage,
  JobUpdateMessage,
  MetricsUpdateMessage,
  NotificationMessage
} from '../websocket'

// Mock socket.io-client
vi.mock('socket.io-client', () => ({
  io: vi.fn(() => ({
    on: vi.fn(),
    onAny: vi.fn(),
    emit: vi.fn(),
    disconnect: vi.fn(),
    connected: false
  }))
}))

describe('WebSocket Message Types', () => {

  describe('HealthUpdateMessage', () => {
    it('should have correct structure', () => {
      const message: HealthUpdateMessage = {
        type: 'health.updated',
        crawlerId: 'crawler-1',
        status: 'HEALTHY',
        message: 'OK',
        timestamp: '2024-01-01T00:00:00Z'
      }

      expect(message.type).toBe('health.updated')
      expect(message.crawlerId).toBe('crawler-1')
      expect(message.status).toBe('HEALTHY')
      expect(message.message).toBe('OK')
      expect(message.timestamp).toBe('2024-01-01T00:00:00Z')
    })
  })

  describe('JobUpdateMessage', () => {
    it('should have correct structure', () => {
      const message: JobUpdateMessage = {
        type: 'job.started',
        crawlerId: 'crawler-1',
        jobId: 'job-123',
        status: 'running',
        timestamp: '2024-01-01T00:00:00Z',
        progress: 50,
        articlesProcessed: 25
      }

      expect(message.type).toBe('job.started')
      expect(message.crawlerId).toBe('crawler-1')
      expect(message.jobId).toBe('job-123')
      expect(message.status).toBe('running')
      expect(message.progress).toBe(50)
      expect(message.articlesProcessed).toBe(25)
    })
  })

  describe('MetricsUpdateMessage', () => {
    it('should have correct structure', () => {
      const message: MetricsUpdateMessage = {
        type: 'metrics.updated',
        crawlerId: 'crawler-1',
        metrics: {
          articlesProcessed: 100,
          successRate: 95.5,
          averageProcessingTime: 1500,
          errorCount: 5
        },
        timestamp: '2024-01-01T00:00:00Z'
      }

      expect(message.type).toBe('metrics.updated')
      expect(message.crawlerId).toBe('crawler-1')
      expect(message.metrics.articlesProcessed).toBe(100)
      expect(message.metrics.successRate).toBe(95.5)
      expect(message.metrics.averageProcessingTime).toBe(1500)
      expect(message.metrics.errorCount).toBe(5)
    })
  })

  describe('NotificationMessage', () => {
    it('should have correct structure', () => {
      const message: NotificationMessage = {
        type: 'notification',
        level: 'info',
        title: 'Test Notification',
        message: 'This is a test',
        timestamp: '2024-01-01T00:00:00Z',
        crawlerId: 'crawler-1',
        autoClose: true
      }

      expect(message.type).toBe('notification')
      expect(message.level).toBe('info')
      expect(message.title).toBe('Test Notification')
      expect(message.message).toBe('This is a test')
      expect(message.crawlerId).toBe('crawler-1')
      expect(message.autoClose).toBe(true)
    })
  })}
)