import type { WebSocketConnectionStatus } from '@/types/api'
import type { PredictionExtractedMessage } from './websocket'

// Prediction Analysis WebSocket service class
export class PredictionAnalysisWebSocketService {
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

  constructor(private endpoint: string = '/ws/analysis') {
    this.connect()
  }

  private connect() {
    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      // Connect directly to the prediction analysis service on port 8083
      const wsUrl = `${protocol}//localhost:8083${this.endpoint}`

      console.log('Connecting to prediction analysis WebSocket:', wsUrl)
      this.socket = new WebSocket(wsUrl)
      this.setupEventHandlers()
    } catch (error) {
      console.error('Failed to create prediction analysis WebSocket connection:', error)
      this.handleConnectionError()
    }
  }

  private setupEventHandlers() {
    if (!this.socket) return

    this.socket.onopen = () => {
      console.log('Prediction analysis WebSocket connected')
      this.connectionStatus = {
        connected: true,
        reconnectAttempts: 0,
        lastConnected: new Date()
      }
      this.notifyStatusListeners()

      // Clear reconnect timer if it exists
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        console.log('Prediction analysis WebSocket service handling the message:', message)
        this.handleMessage(message)
      } catch (error) {
        console.error('Failed to parse prediction analysis WebSocket message:', error)
      }
    }

    this.socket.onclose = (event) => {
      console.log('Prediction analysis WebSocket disconnected:', event.code, event.reason)
      this.connectionStatus = {
        connected: false,
        reconnectAttempts: this.connectionStatus.reconnectAttempts,
        lastDisconnected: new Date()
      }
      this.notifyStatusListeners()
      this.handleConnectionError()
    }

    this.socket.onerror = (error) => {
      console.error('Prediction analysis WebSocket error:', error)
      this.handleConnectionError()
    }
  }

  private handleMessage(message: any) {
    const messageType = message.type
    const listeners = this.messageListeners.get(messageType) || []

    listeners.forEach(listener => {
      try {
        listener(message)
      } catch (error) {
        console.error('Error in prediction analysis WebSocket message listener:', error)
      }
    })

    // Also notify listeners for all messages
    const allListeners = this.messageListeners.get('*') || []
    allListeners.forEach(listener => {
      try {
        listener(message)
      } catch (error) {
        console.error('Error in prediction analysis WebSocket all-message listener:', error)
      }
    })
  }

  private handleConnectionError() {
    if (this.connectionStatus.reconnectAttempts < this.maxReconnectAttempts) {
      const delay = this.reconnectDelay * Math.pow(2, this.connectionStatus.reconnectAttempts)
      console.log(`Attempting to reconnect prediction analysis WebSocket in ${delay}ms (attempt ${this.connectionStatus.reconnectAttempts + 1}/${this.maxReconnectAttempts})`)

      this.reconnectTimer = setTimeout(() => {
        this.connectionStatus.reconnectAttempts++
        this.connect()
      }, delay)
    } else {
      console.error('Max reconnection attempts reached for prediction analysis WebSocket')
    }
  }

  private notifyStatusListeners() {
    this.statusListeners.forEach(listener => {
      try {
        listener(this.connectionStatus)
      } catch (error) {
        console.error('Error in prediction analysis WebSocket status listener:', error)
      }
    })
  }

  // Public methods
  public getConnectionStatus(): WebSocketConnectionStatus {
    return { ...this.connectionStatus }
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

  public onMessage(messageType: string, listener: (message: any) => void): () => void {
    if (!this.messageListeners.has(messageType)) {
      this.messageListeners.set(messageType, [])
    }
    this.messageListeners.get(messageType)!.push(listener)

    return () => {
      const listeners = this.messageListeners.get(messageType)
      if (listeners) {
        const index = listeners.indexOf(listener)
        if (index > -1) {
          listeners.splice(index, 1)
        }
      }
    }
  }

  // Specific methods for prediction analysis messages
  public onAnalysisJobUpdate(listener: (message: any) => void): () => void {
    const unsubscribers = [
      this.onMessage('job.status.update', (message: any) => {
        console.log('WebSocket job.status.update received:', message)
        listener(message)
      }),
      this.onMessage('job.progress.update', (message: any) => {
        console.log('WebSocket job.progress.update received:', message)
        listener(message)
      }),
      this.onMessage('job.completed', (message: any) => {
        console.log('WebSocket job.completed received:', message)
        listener(message)
      }),
      this.onMessage('job.failed', (message: any) => {
        console.log('WebSocket job.failed received:', message)
        listener(message)
      }),
      this.onMessage('job.cancelled', (message: any) => {
        console.log('WebSocket job.cancelled received:', message)
        listener(message)
      })
    ]

    return () => {
      unsubscribers.forEach(unsub => unsub())
    }
  }

  public onPredictionExtracted(listener: (message: PredictionExtractedMessage) => void): () => void {
    return this.onMessage('prediction.extracted', listener)
  }

  public disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }

    if (this.socket) {
      this.socket.close()
      this.socket = null
    }

    this.connectionStatus = {
      connected: false,
      reconnectAttempts: 0
    }
    this.notifyStatusListeners()
  }

  public reconnect() {
    this.disconnect()
    this.connectionStatus.reconnectAttempts = 0
    this.connect()
  }
}

// Singleton instance
let predictionAnalysisWebSocketInstance: PredictionAnalysisWebSocketService | null = null

export function getPredictionAnalysisWebSocketService(): PredictionAnalysisWebSocketService {
  if (!predictionAnalysisWebSocketInstance) {
    predictionAnalysisWebSocketInstance = new PredictionAnalysisWebSocketService()
  }
  return predictionAnalysisWebSocketInstance
}