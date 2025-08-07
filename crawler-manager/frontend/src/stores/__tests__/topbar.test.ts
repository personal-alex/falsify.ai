import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useTopBarStore } from '../topbar'

describe('TopBar Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should initialize with empty context actions', () => {
    const store = useTopBarStore()
    
    expect(store.contextActions).toEqual([])
    expect(store.hasActions).toBe(false)
    expect(store.isLoading).toBe(false)
  })

  it('should set context actions', () => {
    const store = useTopBarStore()
    const mockActions = [
      {
        id: 'test-action',
        label: 'Test Action',
        icon: 'pi pi-test',
        command: () => {}
      }
    ]

    store.setContextActions(mockActions)

    expect(store.contextActions).toEqual(mockActions)
    expect(store.hasActions).toBe(true)
  })

  it('should add context action', () => {
    const store = useTopBarStore()
    const mockAction = {
      id: 'test-action',
      label: 'Test Action',
      icon: 'pi pi-test',
      command: () => {}
    }

    store.addContextAction(mockAction)

    expect(store.contextActions.find(a => a.id === 'test-action')).toBeDefined()
    expect(store.hasActions).toBe(true)
  })

  it('should remove context action', () => {
    const store = useTopBarStore()
    const mockAction = {
      id: 'test-action',
      label: 'Test Action',
      icon: 'pi pi-test',
      command: () => {}
    }

    store.addContextAction(mockAction)
    expect(store.hasActions).toBe(true)

    store.removeContextAction('test-action')
    expect(store.hasActions).toBe(false)
  })

  it('should update action state', () => {
    const store = useTopBarStore()
    const mockAction = {
      id: 'test-action',
      label: 'Test Action',
      icon: 'pi pi-test',
      loading: false,
      command: () => {}
    }

    store.addContextAction(mockAction)
    store.updateActionState('test-action', { loading: true })

    const updatedAction = store.contextActions.find(a => a.id === 'test-action')
    expect(updatedAction?.loading).toBe(true)
  })

  it('should clear context actions', () => {
    const store = useTopBarStore()
    const mockActions = [
      {
        id: 'test-action-1',
        label: 'Test Action 1',
        icon: 'pi pi-test',
        command: () => {}
      },
      {
        id: 'test-action-2',
        label: 'Test Action 2',
        icon: 'pi pi-test',
        command: () => {}
      }
    ]

    store.setContextActions(mockActions)
    expect(store.hasActions).toBe(true)

    store.clearContextActions()
    expect(store.hasActions).toBe(false)
    expect(store.contextActions).toEqual([])
  })

  it('should set crawler management actions', () => {
    const store = useTopBarStore()
    const mockCallbacks = {
      onStartAllHealthy: () => {},
      onStopAllRunning: () => {},
      onSyncConfiguration: () => {},
      onViewMetrics: () => {}
    }

    store.setCrawlerManagementActions(mockCallbacks)

    expect(store.contextActions).toHaveLength(4)
    expect(store.contextActions.find(a => a.id === 'start-all-healthy')).toBeDefined()
    expect(store.contextActions.find(a => a.id === 'stop-all-running')).toBeDefined()
    expect(store.contextActions.find(a => a.id === 'sync-configuration')).toBeDefined()
    expect(store.contextActions.find(a => a.id === 'view-metrics')).toBeDefined()
  })

  it('should set crawler detail actions', () => {
    const store = useTopBarStore()
    const mockCallbacks = {
      onStartCrawl: () => {},
      onHealthCheck: () => {},
      onStatusCheck: () => {}
    }
    const mockOptions = {
      canStartCrawl: true,
      isHealthChecking: false,
      isCrawling: false
    }

    store.setCrawlerDetailActions(mockCallbacks, mockOptions)

    expect(store.contextActions).toHaveLength(3)
    expect(store.contextActions.find(a => a.id === 'start-crawl')).toBeDefined()
    expect(store.contextActions.find(a => a.id === 'health-check')).toBeDefined()
    expect(store.contextActions.find(a => a.id === 'status-check')).toBeDefined()
  })

  it('should set prediction analysis actions', () => {
    const store = useTopBarStore()
    const mockCallbacks = {
      onRefreshArticles: () => {},
      onAnalyzeArticles: () => {}
    }
    const mockOptions = {
      hasSelectedArticles: true,
      isRefreshing: false,
      isAnalyzing: false
    }

    store.setPredictionAnalysisActions(mockCallbacks, mockOptions)

    expect(store.contextActions).toHaveLength(2)
    expect(store.contextActions.find(a => a.id === 'refresh-articles')).toBeDefined()
    expect(store.contextActions.find(a => a.id === 'analyze-articles')).toBeDefined()
  })

  it('should disable analyze articles action when no articles selected', () => {
    const store = useTopBarStore()
    const mockCallbacks = {
      onRefreshArticles: () => {},
      onAnalyzeArticles: () => {}
    }
    const mockOptions = {
      hasSelectedArticles: false,
      isRefreshing: false,
      isAnalyzing: false
    }

    store.setPredictionAnalysisActions(mockCallbacks, mockOptions)

    const analyzeAction = store.contextActions.find(a => a.id === 'analyze-articles')
    expect(analyzeAction?.disabled).toBe(true)
  })
})