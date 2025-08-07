import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface TopBarAction {
  id: string
  label: string
  icon: string
  severity?: 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'danger'
  outlined?: boolean
  loading?: boolean
  disabled?: boolean
  tooltip?: string
  command: () => void
}

export interface TopBarState {
  contextActions: TopBarAction[]
  isLoading: boolean
}

export const useTopBarStore = defineStore('topbar', () => {
  // State
  const state = ref<TopBarState>({
    contextActions: [],
    isLoading: false
  })

  // Getters
  const contextActions = computed(() => state.value.contextActions)
  const isLoading = computed(() => state.value.isLoading)
  const hasActions = computed(() => state.value.contextActions.length > 0)

  // Actions
  const setContextActions = (actions: TopBarAction[]) => {
    state.value.contextActions = [...actions]
  }

  const addContextAction = (action: TopBarAction) => {
    const existingIndex = state.value.contextActions.findIndex(a => a.id === action.id)
    if (existingIndex >= 0) {
      state.value.contextActions[existingIndex] = action
    } else {
      state.value.contextActions.push(action)
    }
  }

  const removeContextAction = (actionId: string) => {
    state.value.contextActions = state.value.contextActions.filter(a => a.id !== actionId)
  }

  const updateActionState = (actionId: string, updates: Partial<TopBarAction>) => {
    const actionIndex = state.value.contextActions.findIndex(a => a.id === actionId)
    if (actionIndex >= 0) {
      state.value.contextActions[actionIndex] = {
        ...state.value.contextActions[actionIndex],
        ...updates
      }
    }
  }

  const clearContextActions = () => {
    state.value.contextActions = []
  }

  const setLoading = (loading: boolean) => {
    state.value.isLoading = loading
  }

  // Predefined action sets for different contexts
  const setCrawlerManagementActions = (callbacks: {
    onStartAllHealthy?: () => void
    onStopAllRunning?: () => void
    onSyncConfiguration?: () => void
    onViewMetrics?: () => void
  }) => {
    const actions: TopBarAction[] = []

    if (callbacks.onStartAllHealthy) {
      actions.push({
        id: 'start-all-healthy',
        label: 'Start All Healthy',
        icon: 'pi pi-play',
        severity: 'success',
        tooltip: 'Start all healthy crawlers',
        command: callbacks.onStartAllHealthy
      })
    }

    if (callbacks.onStopAllRunning) {
      actions.push({
        id: 'stop-all-running',
        label: 'Stop All Running',
        icon: 'pi pi-stop',
        severity: 'warning',
        tooltip: 'Stop all running crawlers',
        command: callbacks.onStopAllRunning
      })
    }

    if (callbacks.onSyncConfiguration) {
      actions.push({
        id: 'sync-configuration',
        label: 'Sync Config',
        icon: 'pi pi-sync',
        severity: 'info',
        tooltip: 'Synchronize crawler configurations',
        command: callbacks.onSyncConfiguration
      })
    }

    if (callbacks.onViewMetrics) {
      actions.push({
        id: 'view-metrics',
        label: 'View Metrics',
        icon: 'pi pi-chart-line',
        severity: 'secondary',
        outlined: true,
        tooltip: 'View system metrics',
        command: callbacks.onViewMetrics
      })
    }

    setContextActions(actions)
  }

  const setCrawlerDetailActions = (callbacks: {
    onStartCrawl?: () => void
    onHealthCheck?: () => void
    onStatusCheck?: () => void
  }, options: {
    canStartCrawl?: boolean
    isHealthChecking?: boolean
    isCrawling?: boolean
  } = {}) => {
    const actions: TopBarAction[] = []

    if (callbacks.onStartCrawl) {
      actions.push({
        id: 'start-crawl',
        label: 'Start Crawl',
        icon: 'pi pi-play',
        severity: 'success',
        disabled: !options.canStartCrawl || options.isCrawling,
        loading: options.isCrawling,
        tooltip: options.canStartCrawl ? 'Start a new crawl' : 'Crawler must be healthy to start crawl',
        command: callbacks.onStartCrawl
      })
    }

    if (callbacks.onHealthCheck) {
      actions.push({
        id: 'health-check',
        label: 'Health Check',
        icon: 'pi pi-refresh',
        severity: 'info',
        loading: options.isHealthChecking,
        tooltip: 'Force a health check for this crawler',
        command: callbacks.onHealthCheck
      })
    }

    if (callbacks.onStatusCheck) {
      actions.push({
        id: 'status-check',
        label: 'Check Status',
        icon: 'pi pi-info-circle',
        severity: 'secondary',
        outlined: true,
        tooltip: 'Check current crawl status',
        command: callbacks.onStatusCheck
      })
    }

    setContextActions(actions)
  }

  const setPredictionAnalysisActions = (callbacks: {
    onRefreshArticles?: () => void
    onAnalyzeArticles?: () => void
  }, options: {
    hasSelectedArticles?: boolean
    isRefreshing?: boolean
    isAnalyzing?: boolean
  } = {}) => {
    const actions: TopBarAction[] = []

    if (callbacks.onRefreshArticles) {
      actions.push({
        id: 'refresh-articles',
        label: 'Refresh',
        icon: 'pi pi-refresh',
        severity: 'secondary',
        outlined: true,
        loading: options.isRefreshing,
        tooltip: 'Refresh articles list',
        command: callbacks.onRefreshArticles
      })
    }

    if (callbacks.onAnalyzeArticles) {
      actions.push({
        id: 'analyze-articles',
        label: 'Analyze Articles',
        icon: 'pi pi-arrow-right',
        severity: 'primary',
        disabled: !options.hasSelectedArticles,
        loading: options.isAnalyzing,
        tooltip: options.hasSelectedArticles ? 'Analyze selected articles' : 'Select articles to analyze',
        command: callbacks.onAnalyzeArticles
      })
    }

    setContextActions(actions)
  }

  return {
    // State
    state,
    
    // Getters
    contextActions,
    isLoading,
    hasActions,
    
    // Actions
    setContextActions,
    addContextAction,
    removeContextAction,
    updateActionState,
    clearContextActions,
    setLoading,
    
    // Predefined action sets
    setCrawlerManagementActions,
    setCrawlerDetailActions,
    setPredictionAnalysisActions
  }
})