import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { HealthStatus } from '@/types/health'
import type { WebSocketConnectionStatus } from '@/types/api'
import { getWebSocketService, type HealthUpdateMessage } from '@/services/websocket'

export const useHealthStore = defineStore('health', () => {
  // State
  const healthStatuses = ref<Record<string, HealthStatus>>({})
  const wsConnectionStatus = ref<WebSocketConnectionStatus>({
    connected: false,
    reconnectAttempts: 0
  })
  const lastHealthUpdate = ref<Date | null>(null)
  
  // WebSocket service instance
  const wsService = getWebSocketService()
  let wsUnsubscribers: (() => void)[] = []

  // Getters
  const getHealthStatus = computed(() => {
    return (crawlerId: string) => healthStatuses.value[crawlerId]
  })

  const overallHealthStatus = computed(() => {
    const statuses = Object.values(healthStatuses.value)
    if (statuses.length === 0) return 'UNKNOWN'
    
    if (statuses.some(s => s.status === 'UNHEALTHY')) return 'UNHEALTHY'
    if (statuses.every(s => s.status === 'HEALTHY')) return 'HEALTHY'
    return 'UNKNOWN'
  })

  const healthSummary = computed(() => {
    const statuses = Object.values(healthStatuses.value)
    return {
      total: statuses.length,
      healthy: statuses.filter(s => s.status === 'HEALTHY').length,
      unhealthy: statuses.filter(s => s.status === 'UNHEALTHY').length,
      unknown: statuses.filter(s => s.status === 'UNKNOWN').length
    }
  })

  const isWebSocketConnected = computed(() => wsConnectionStatus.value.connected)

  const connectionQuality = computed(() => {
    if (!wsConnectionStatus.value.connected) return 'disconnected'
    if (wsConnectionStatus.value.reconnectAttempts > 0) return 'unstable'
    return 'stable'
  })

  // Actions
  const updateHealthStatus = (crawlerId: string, health: HealthStatus) => {
    healthStatuses.value[crawlerId] = health
    lastHealthUpdate.value = new Date()
  }

  const updateHealthStatuses = (statuses: Record<string, HealthStatus>) => {
    healthStatuses.value = { ...statuses }
    lastHealthUpdate.value = new Date()
  }

  const handleHealthUpdate = (message: HealthUpdateMessage) => {
    const healthStatus: HealthStatus = {
      status: message.status,
      message: message.message,
      lastCheck: message.timestamp,
      responseTimeMs: null, // WebSocket doesn't include response time
      crawlerId: message.crawlerId
    }
    
    updateHealthStatus(message.crawlerId, healthStatus)
  }

  const handleConnectionStatusChange = (status: WebSocketConnectionStatus) => {
    wsConnectionStatus.value = status
  }

  const initWebSocket = () => {
    try {
      // Clean up existing subscriptions
      wsUnsubscribers.forEach(unsub => unsub())
      wsUnsubscribers = []
      
      // Subscribe to health updates
      wsUnsubscribers.push(
        wsService.onHealthUpdate(handleHealthUpdate)
      )
      
      // Subscribe to connection status changes
      wsUnsubscribers.push(
        wsService.onConnectionStatusChange(handleConnectionStatusChange)
      )

      // Request current health status on connection
      wsUnsubscribers.push(
        wsService.on('connect', () => {
          wsService.emit('health.request.current')
        })
      )

      // Handle bulk health updates
      wsUnsubscribers.push(
        wsService.on('health.bulk.updated', (data: Record<string, HealthStatus>) => {
          updateHealthStatuses(data)
        })
      )
      
    } catch (error) {
      console.error('Failed to initialize health WebSocket:', error)
    }
  }

  const disconnectWebSocket = () => {
    wsUnsubscribers.forEach(unsub => unsub())
    wsUnsubscribers = []
    
    wsConnectionStatus.value = {
      connected: false,
      reconnectAttempts: 0,
      lastDisconnected: new Date()
    }
  }

  const requestHealthUpdate = (crawlerId?: string) => {
    if (wsService.isConnected()) {
      if (crawlerId) {
        wsService.emit('health.request.single', { crawlerId })
      } else {
        wsService.emit('health.request.all')
      }
    }
  }

  const clearHealthStatuses = () => {
    healthStatuses.value = {}
    lastHealthUpdate.value = null
  }

  const getHealthStatusWithFallback = (crawlerId: string): HealthStatus => {
    return healthStatuses.value[crawlerId] || {
      status: 'UNKNOWN',
      message: 'No health data available',
      lastCheck: '',
      responseTimeMs: null,
      crawlerId
    }
  }

  return {
    // State
    healthStatuses,
    wsConnectionStatus,
    lastHealthUpdate,
    
    // Getters
    getHealthStatus,
    overallHealthStatus,
    healthSummary,
    isWebSocketConnected,
    connectionQuality,
    
    // Actions
    updateHealthStatus,
    updateHealthStatuses,
    handleHealthUpdate,
    initWebSocket,
    disconnectWebSocket,
    requestHealthUpdate,
    clearHealthStatuses,
    getHealthStatusWithFallback
  }
})