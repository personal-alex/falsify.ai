// Job-related types for the frontend

export interface JobStatus {
  jobId: string
  crawlerId: string
  status: 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  startTime: string
  endTime?: string
  articlesProcessed: number
  articlesSkipped: number
  articlesFailed: number
  errorMessage?: string
  requestId?: string
  currentActivity?: string
  lastUpdated?: string
  durationMs?: number
  elapsedTimeMs: number
  successRate: number
  totalArticlesAttempted: number
}

export interface JobHistoryFilter {
  status?: JobStatus['status'][]
  dateFrom?: Date
  dateTo?: Date
  searchTerm?: string
}

export interface JobHistorySort {
  field: keyof JobStatus
  order: 'asc' | 'desc'
}

export interface JobDetail extends JobStatus {
  // Additional details that might be available
  estimatedDuration?: string
  progressPercentage?: number
  averageProcessingTime?: number
  lastActivityTime?: string
}

// WebSocket job update message
export interface JobUpdateMessage {
  type: 'job.started' | 'job.progress' | 'job.completed' | 'job.failed' | 'job.cancelled'
  jobId: string
  crawlerId: string
  status: JobStatus['status']
  progress?: {
    articlesProcessed: number
    articlesSkipped: number
    articlesFailed: number
    currentActivity?: string
    estimatedCompletion?: string
  }
  timestamp: string
  details?: any
}

// Job statistics for display
export interface JobStatistics {
  totalJobs: number
  runningJobs: number
  completedJobs: number
  failedJobs: number
  cancelledJobs: number
  averageSuccessRate: number
  averageDuration: number
}

// Analysis job specific types
export interface AnalysisJob {
  id: number
  jobId: string
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  startedAt: string
  completedAt?: string
  totalArticles: number
  processedArticles: number
  predictionsFound: number
  analysisType: 'mock' | 'llm'
  errorMessage?: string
}

export interface AnalysisJobFilter {
  status?: AnalysisJob['status'][]
  analysisType?: AnalysisJob['analysisType'][]
  dateFrom?: Date
  dateTo?: Date
  minPredictions?: number
  searchTerm?: string
}

export interface PredictionInstance {
  id: number
  predictionText: string
  rating: number
  confidenceScore: number
  context: string
  extractedAt: string
  article: {
    id: number
    title: string
    author?: {
      name: string
      avatarUrl?: string
    }
  }
}

export interface AnalysisStatistics {
  totalJobs: number
  completedJobs: number
  runningJobs: number
  failedJobs: number
  totalPredictions: number
}