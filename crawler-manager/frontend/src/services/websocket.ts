import type { WebSocketConnectionStatus } from '@/types/api'

// Message type definitions
export interface HealthUpdateMessage {
  type: 'health.updated' | 'health.current'
  crawlerId: string
  status: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN'
  message: string
  timestamp: string
}

export interface JobUpdateMessage {
  type: 'job.started' | 'job.progress' | 'job.completed' | 'job.failed' | 'job.cancelled'
  crawlerId: string
  jobId: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  progress?: {
    articlesProcessed: number
    articlesSkipped: number
    articlesFailed: number
    currentActivity?: string
    estimatedCompletion?: string
  }
  articlesProcessed?: number
  timestamp: string
  details?: any
}

export interface MetricsUpdateMessage {
  type: 'metrics.updated'
  crawlerId: string
  metrics: {
    articlesProcessed: number
    successRate: number
    averageProcessingTime: number
    errorCount: number
  }
  timestamp: string
}

export interface NotificationMessage {
  type: 'notification'
  level: 'info' | 'success' | 'warn' | 'error'
  title: string
  message: string
  crawlerId?: string
  timestamp: string
  autoClose?: boolean
}

export interface AnalysisJobUpdateMessage {
  type: 'analysis.job.started' | 'analysis.job.progress' | 'analysis.job.completed' | 'analysis.job.failed' | 'analysis.job.cancelled'
  jobId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  progress?: {
    articlesProcessed: number
    articlesSkipped: number
    articlesFailed: number
    totalArticles?: number
    predictionsFound?: number
    currentActivity?: string
    estimatedCompletion?: string
  }
  timestamp: string
  details?: any
}

export interface PredictionExtractedMessage {
  type: 'prediction.extracted'
  jobId: string
  predictionId: string
  predictionText: string
  rating: number
  confidenceScore: number
  articleId: number
  timestamp: string
}

// Union type for all possible WebSocket messages
export type WebSocketMessageType = 
  | HealthUpdateMessage 
  | JobUpdateMessage 
  | MetricsUpdateMessage 
  | NotificationMessage
  | AnalysisJobUpdateMessage
  | PredictionExtractedMessage

// WebSocket service class
export class WebSocketService {
  private socket: WebSocket | null = null
  private connectionStatus: WebSocketConnectionStatus = {
    connected: false,
    reconnectAttempts: 0
  }
  private statusListeners: ((status: WebSocketConnectionStatus) => void)[] = []
  private messageListeners: Map<string, ((message: any) => void)[]> = new Map()
  private reconnectTimer: NodeJS.Timeout | null = null
  private maxReconnectAttempts = 10
  private reconnectDelay = 1000

  constructor(private endpoint: string = '/websocket/jobs') {
    this.connect()
  }

  private connect() {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsUrl = `${protocol}//${window.location.host}${this.endpoint}`
      
      this.socket = new WebSocket(wsUrl)
      this.setupEventHandlers()
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
      this.handleConnectionError()
    }
  }

  private setupEventHandlers() {
    if (!this.socket) return

    this.socket.onopen = () => {
      console.log(`WebSocket connected to ${this.endpoint}`)
      this.updateConnectionStatus({
        connected: true,
        reconnectAttempts: 0,
        lastConnected: new Date()
      })
    }

    this.socket.onclose = (event) => {
      console.log(`WebSocket disconnected from ${this.endpoint}:`, event.reason)
      this.updateConnectionStatus({
        connected: false,
        reconnectAttempts: this.connectionStatus.reconnectAttempts,
        lastDisconnected: new Date()
      })
      
      if (event.code === 1000) {
        // Normal closure, don't reconnect
        return
      }
      
      this.attemptReconnect()
    }

    this.socket.onerror = (error) => {
      console.error('WebSocket connection error:', error)
      this.handleConnectionError()
    }

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        this.handleMessage(message.type || 'message', message)
      } catch (error) {
        console.error('Failed to parse WebSocket message:', error)
      }
    }
  }

  private handleMessage(eventName: string, data: any) {
    const listeners = this.messageListeners.get(eventName)
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(data)
        } catch (error) {
          console.error(`Error in WebSocket message listener for ${eventName}:`, error)
        }
      })
    }
  }

  private handleConnectionError() {
    this.updateConnectionStatus({
      connected: false,
      reconnectAttempts: this.connectionStatus.reconnectAttempts + 1,
      lastDisconnected: new Date()
    })
    
    this.attemptReconnect()
  }

  private attemptReconnect() {
    if (this.connectionStatus.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max WebSocket reconnection attempts reached')
      return
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }

    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.connectionStatus.reconnectAttempts),
      30000 // Max 30 seconds
    )

    console.log(`Attempting to reconnect WebSocket in ${delay}ms (attempt ${this.connectionStatus.reconnectAttempts + 1}/${this.maxReconnectAttempts})`)

    this.reconnectTimer = setTimeout(() => {
      this.disconnect()
      this.connect()
    }, delay)
  }

  private updateConnectionStatus(status: Partial<WebSocketConnectionStatus>) {
    this.connectionStatus = { ...this.connectionStatus, ...status }
    this.statusListeners.forEach(listener => {
      try {
        listener(this.connectionStatus)
      } catch (error) {
        console.error('Error in connection status listener:', error)
      }
    })
  }

  // Public API methods
  public getConnectionStatus(): WebSocketConnectionStatus {
    return { ...this.connectionStatus }
  }

  public isConnected(): boolean {
    return this.connectionStatus.connected && this.socket?.readyState === WebSocket.OPEN
  }

  public onConnectionStatusChange(listener: (status: WebSocketConnectionStatus) => void): () => void {
    this.statusListeners.push(listener)
    return () => {
      const index = this.statusListeners.indexOf(listener)
      if (index > -1) {
        this.statusListeners.splice(index, 1)
      }
    }
  }

  public on<T = any>(eventName: string, listener: (data: T) => void): () => void {
    if (!this.messageListeners.has(eventName)) {
      this.messageListeners.set(eventName, [])
    }
    
    this.messageListeners.get(eventName)!.push(listener)
    
    return () => {
      const listeners = this.messageListeners.get(eventName)
      if (listeners) {
        const index = listeners.indexOf(listener)
        if (index > -1) {
          listeners.splice(index, 1)
        }
      }
    }
  }

  public emit(eventName: string, data?: any): void {
    if (this.socket && this.isConnected()) {
      const message = { type: eventName, data }
      this.socket.send(JSON.stringify(message))
    } else {
      console.warn(`Cannot emit ${eventName}: WebSocket not connected`)
    }
  }

  public disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.socket) {
      this.socket.close(1000, 'Client disconnect')
      this.socket = null
    }

    this.messageListeners.clear()
    this.statusListeners = []
    
    this.updateConnectionStatus({
      connected: false,
      lastDisconnected: new Date()
    })
  }

  // Convenience methods for specific message types
  public onHealthUpdate(listener: (message: HealthUpdateMessage) => void): () => void {
    return this.on<HealthUpdateMessage>('health.updated', listener)
  }

  public onJobUpdate(listener: (message: JobUpdateMessage) => void): () => void {
    // Listen for multiple job event types
    const unsubscribers = [
      this.on<JobUpdateMessage>('job.started', listener),
      this.on<JobUpdateMessage>('job.progress', listener),
      this.on<JobUpdateMessage>('job.completed', listener),
      this.on<JobUpdateMessage>('job.failed', listener),
      this.on<JobUpdateMessage>('job.cancelled', listener),
      this.on<JobUpdateMessage>('job.updated', listener)
    ]
    
    return () => {
      unsubscribers.forEach(unsub => unsub())
    }
  }

  public onMetricsUpdate(listener: (message: MetricsUpdateMessage) => void): () => void {
    return this.on<MetricsUpdateMessage>('metrics.updated', listener)
  }

  public onNotification(listener: (message: NotificationMessage) => void): () => void {
    return this.on<NotificationMessage>('notification', listener)
  }

  public onAnalysisJobUpdate(listener: (message: AnalysisJobUpdateMessage) => void): () => void {
    // Listen for multiple analysis job event types
    const unsubscribers = [
      this.on<AnalysisJobUpdateMessage>('analysis.job.started', listener),
      this.on<AnalysisJobUpdateMessage>('analysis.job.progress', listener),
      this.on<AnalysisJobUpdateMessage>('analysis.job.completed', listener),
      this.on<AnalysisJobUpdateMessage>('analysis.job.failed', listener),
      this.on<AnalysisJobUpdateMessage>('analysis.job.cancelled', listener)
    ]
    
    return () => {
      unsubscribers.forEach(unsub => unsub())
    }
  }

  public onPredictionExtracted(listener: (message: PredictionExtractedMessage) => void): () => void {
    return this.on<PredictionExtractedMessage>('prediction.extracted', listener)
  }
}

// Legacy WebSocket service for backward compatibility
export class HealthWebSocketService extends WebSocketService {
  constructor() {
    super('/websocket/jobs')
  }

  public onHealthUpdate(listener: (message: HealthUpdateMessage) => void): () => void {
    return super.onHealthUpdate(listener)
  }
}

// Simple event bus for cross-component communication
class EventBus {
  private listeners: Map<string, ((data: any) => void)[]> = new Map()

  on(event: string, listener: (data: any) => void): () => void {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event)!.push(listener)
    
    return () => {
      const eventListeners = this.listeners.get(event)
      if (eventListeners) {
        const index = eventListeners.indexOf(listener)
        if (index > -1) {
          eventListeners.splice(index, 1)
        }
      }
    }
  }

  off(event: string, listener?: (data: any) => void): void {
    if (!listener) {
      this.listeners.delete(event)
      return
    }
    
    const eventListeners = this.listeners.get(event)
    if (eventListeners) {
      const index = eventListeners.indexOf(listener)
      if (index > -1) {
        eventListeners.splice(index, 1)
      }
    }
  }

  emit(event: string, data?: any): void {
    const eventListeners = this.listeners.get(event)
    if (eventListeners) {
      eventListeners.forEach(listener => {
        try {
          listener(data)
        } catch (error) {
          console.error(`Error in event listener for ${event}:`, error)
        }
      })
    }
  }
}

// Global event bus instance
export const eventBus = new EventBus()

// Singleton instance for the main WebSocket connection
let mainWebSocketInstance: WebSocketService | null = null

export function getWebSocketService(): WebSocketService {
  if (!mainWebSocketInstance) {
    mainWebSocketInstance = new WebSocketService()
    
    // Bridge WebSocket events to the event bus
    mainWebSocketInstance.onHealthUpdate((message) => {
      eventBus.emit('health:updated', message)
    })
    
    mainWebSocketInstance.onJobUpdate((message) => {
      eventBus.emit('job:updated', message)
    })
    
    mainWebSocketInstance.onMetricsUpdate((message) => {
      eventBus.emit('metrics:updated', message)
    })
    
    mainWebSocketInstance.onNotification((message) => {
      eventBus.emit('notification', message)
    })
    
    mainWebSocketInstance.onAnalysisJobUpdate((message) => {
      eventBus.emit('analysis:job:updated', message)
    })
    
    mainWebSocketInstance.onPredictionExtracted((message) => {
      eventBus.emit('prediction:extracted', message)
    })
  }
  return mainWebSocketInstance
}

export function disconnectWebSocket(): void {
  if (mainWebSocketInstance) {
    mainWebSocketInstance.disconnect()
    mainWebSocketInstance = null
  }
}