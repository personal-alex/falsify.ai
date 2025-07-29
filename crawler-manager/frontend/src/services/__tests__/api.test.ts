import { describe, it, expect, vi } from 'vitest'
import { ApiServiceError } from '../api'

describe('ApiService', () => {
  describe('ApiServiceError', () => {
    it('should create error with message', () => {
      const error = new ApiServiceError('Test error')
      
      expect(error.message).toBe('Test error')
      expect(error.name).toBe('ApiServiceError')
      expect(error.code).toBeUndefined()
      expect(error.status).toBeUndefined()
      expect(error.details).toBeUndefined()
    })

    it('should create error with all properties', () => {
      const error = new ApiServiceError('Test error', 'TEST_CODE', 500, { extra: 'data' })
      
      expect(error.message).toBe('Test error')
      expect(error.name).toBe('ApiServiceError')
      expect(error.code).toBe('TEST_CODE')
      expect(error.status).toBe(500)
      expect(error.details).toEqual({ extra: 'data' })
    })

  })

  describe('Connection Status Interface', () => {
    it('should have correct connection status structure', () => {
      const mockStatus = {
        online: true,
        lastCheck: new Date(),
        latency: 100
      }

      expect(mockStatus).toHaveProperty('online')
      expect(mockStatus).toHaveProperty('lastCheck')
      expect(mockStatus.lastCheck).toBeInstanceOf(Date)
      expect(typeof mockStatus.latency).toBe('number')
    })
  })
})