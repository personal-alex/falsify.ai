// API-related types

export interface ApiResponse<T = any> {
  data: T
  success: boolean
  message?: string
  timestamp: string
}

export interface ApiError {
  message: string
  code?: string
  details?: any
  timestamp: string
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface ApiRequestOptions {
  timeout?: number
  retries?: number
  headers?: Record<string, string>
}

// WebSocket message types
export interface WebSocketMessage<T = any> {
  type: string
  data: T
  timestamp: string
}

export interface WebSocketConnectionStatus {
  connected: boolean
  reconnectAttempts: number
  lastConnected?: Date
  lastDisconnected?: Date
}