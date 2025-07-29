// UI-related types

export interface LoadingState {
  isLoading: boolean
  message?: string
}

export interface ErrorState {
  hasError: boolean
  message?: string
  code?: string
  details?: any
}

export interface TableColumn {
  field: string
  header: string
  sortable?: boolean
  filterable?: boolean
  width?: string
  align?: 'left' | 'center' | 'right'
  format?: (value: any) => string
}

export interface FilterOption {
  label: string
  value: any
  icon?: string
}

export interface ChartDataPoint {
  label: string
  value: number
  timestamp?: Date
}

export interface ChartConfig {
  type: 'line' | 'bar' | 'pie' | 'doughnut'
  responsive?: boolean
  maintainAspectRatio?: boolean
  plugins?: any
  scales?: any
}

export interface ModalConfig {
  title: string
  width?: string
  height?: string
  closable?: boolean
  modal?: boolean
  dismissableMask?: boolean
}

export interface ToastMessage {
  severity: 'success' | 'info' | 'warn' | 'error'
  summary: string
  detail?: string
  life?: number
  sticky?: boolean
  closable?: boolean
}

export interface ConfirmationConfig {
  message: string
  header?: string
  icon?: string
  acceptLabel?: string
  rejectLabel?: string
  acceptClass?: string
  rejectClass?: string
}

// Theme and styling types
export type ThemeMode = 'light' | 'dark' | 'auto'

export interface ThemeConfig {
  mode: ThemeMode
  primaryColor?: string
  accentColor?: string
  customCss?: string
}

// Layout types
export interface LayoutConfig {
  sidebar: {
    visible: boolean
    collapsed: boolean
    width: string
  }
  header: {
    visible: boolean
    height: string
  }
  footer: {
    visible: boolean
    height: string
  }
}

// Metrics types
export interface MetricPoint {
  timestamp: string
  articlesProcessed: number
  successRate: number
  processingTimeMs: number
  errorCount: number
}

export interface CrawlerMetrics {
  crawlerId: string
  articlesProcessed: number
  successRate: number
  averageProcessingTimeMs: number
  errorCount: number
  lastCrawlTime?: string
  lastUpdated: string
  trendsData?: MetricPoint[]
  totalCrawlsExecuted: number
  totalExecutionTimeMs: number
  activeCrawls: number
}

export interface MetricsStatus {
  cachedMetricsCount: number
  collectionEnabled: boolean
  lastUpdate: number
}