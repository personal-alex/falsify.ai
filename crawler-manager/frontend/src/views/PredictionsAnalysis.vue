<template>
  <div class="predictions-analysis">
    <!-- Header -->
    <!-- div class="page-header mb-4">
      <div class="flex align-items-center justify-content-between">
        <div class="flex align-items-center">
          <i class="pi pi-search mr-3 text-primary text-2xl"></i>
          <div>
            <h1 class="text-2xl font-semibold text-900 m-0">Analyze articles</h1>
            <p class="text-600 m-0 mt-1">Select articles and extract predictions using AI analysis</p>
          </div>
        </div>
      </div>
    </div -->

    <!-- Steps Component -->
    <div class="steps-container mb-4">
      <Steps 
        :model="stepItems" 
        :activeIndex="activeStep" 
        :readonly="false"
        class="custom-steps"
      />
    </div>

    <!-- Step Content -->
    <div class="step-content">
      <!-- Step 1: Articles Selection -->
      <div v-if="activeStep === 0" class="step-panel">
        <Panel>
          <template #header>
            <div class="flex align-items-center">
              <i class="pi pi-list mr-2"></i>
              <span class="font-semibold">Select Articles for Analysis</span>
            </div>
          </template>

          <!-- Article Filters -->
          <div class="mb-4">
            <ArticleFilter 
              :filters="filters"
              :authors="authors"
              @filter-changed="onFilterChanged"
            />
          </div>

          <!-- Article Selection Table -->
          <ArticleSelectionTable 
            :articles="filteredArticles"
            :selected-articles="selectedArticles"
            :loading="isLoadingArticles"
            @selection-changed="onSelectionChanged"
            @refresh-articles="refreshArticles"
          />

          <!-- Step Actions -->
          <div class="flex justify-content-between align-items-center mt-4 pt-3 border-top-1 surface-border">
            <div class="selection-summary">
              <span v-if="selectedArticles.length > 0" class="text-primary font-medium">
                {{ selectedArticles.length }} articles selected
              </span>
              <span v-else class="text-500">
                No articles selected
              </span>
            </div>
            
            <div class="flex gap-2">
              <!-- Actions moved to topbar -->
            </div>
          </div>
        </Panel>
      </div>

      <!-- Step 2: Extracted Predictions -->
      <div v-if="activeStep === 1" class="step-panel">
        <Panel>
          <template #header>
            <div class="flex align-items-center">
              <i class="pi pi-eye mr-2"></i>
              <span class="font-semibold">Extracted Predictions</span>
            </div>
          </template>

          <!-- Analysis Progress -->
          <div v-if="currentAnalysisJob && currentAnalysisJob.status === 'RUNNING'" class="mb-4">
            <AnalysisProgress 
              :job="currentAnalysisJob"
              @cancel-analysis="cancelAnalysis"
              @retry-analysis="retryAnalysis"
              @job-updated="onJobUpdated"
            />
          </div>

          <!-- Analysis Results -->
          <div v-if="analysisResults.length > 0">
            <PredictionResults 
              :predictions="analysisResults"
              :loading="isLoadingResults"
              @export-results="exportResults"
              @refresh-results="refreshResults"
              @prediction-selected="onPredictionSelected"
            />
          </div>

          <!-- Empty State -->
          <div v-if="!currentAnalysisJob && analysisResults.length === 0" class="text-center p-6">
            <div class="mb-4">
              <i class="pi pi-search text-6xl text-400"></i>
            </div>
            <div class="text-900 font-medium text-xl mb-2">No Analysis Results</div>
            <div class="text-600 mb-4">
              Analysis results will appear here once the analysis is complete.
            </div>
          </div>

          <!-- Step Actions -->
          <div class="flex justify-content-between align-items-center mt-4 pt-3 border-top-1 surface-border">
            <Button 
              icon="pi pi-arrow-left"
              label="Back to Selection"
              @click="goToStep(0)"
              severity="secondary"
              outlined
            />
            
            <div class="flex gap-2">
              <Button 
                icon="pi pi-history" 
                label="View History"
                @click="viewHistory"
                severity="secondary"
                outlined
              />
              
              <Button 
                v-if="analysisResults.length > 0"
                icon="pi pi-download" 
                label="Export Results"
                @click="showExportMenu"
                severity="primary"
                outlined
              />
            </div>
          </div>
        </Panel>
      </div>
    </div>

    <!-- Export Menu -->
    <Menu ref="exportMenu" :model="exportMenuItems" :popup="true" />
    
    <!-- Confirm Dialog -->
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useTopBarStore } from '@/stores/topbar'
import { getWebSocketService, type AnalysisJobUpdateMessage, type PredictionExtractedMessage } from '@/services/websocket'
import { ApiService, ApiServiceError } from '@/services/api'
import Button from 'primevue/button'
import Panel from 'primevue/panel'
import Steps from 'primevue/steps'
import Menu from 'primevue/menu'
import ConfirmDialog from 'primevue/confirmdialog'

import ArticleFilter, { type ArticleFilters, type Author } from '@/components/ArticleFilter.vue'
import ArticleSelectionTable, { type Article } from '@/components/ArticleSelectionTable.vue'
import AnalysisProgress from '@/components/AnalysisProgress.vue'
import PredictionResults from '@/components/PredictionResults.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const topBarStore = useTopBarStore()

// Reactive state
const activeStep = ref(0)
const isLoadingArticles = ref(false)
const isRefreshing = ref(false)
const isAnalyzing = ref(false)
const isLoadingResults = ref(false)
const selectedArticles = ref<Article[]>([])
const currentAnalysisJob = ref<any>(null)
const analysisResults = ref<any[]>([])
const analysisConfig = ref<any>(null)

// Steps configuration
const stepItems = ref([
  {
    label: 'Articles Selection',
    icon: 'pi pi-list'
  },
  {
    label: 'Extracted Predictions',
    icon: 'pi pi-eye'
  }
])

// Export menu
const exportMenu = ref<any>(null)
const exportMenuItems = ref([
  {
    label: 'Export as CSV',
    icon: 'pi pi-file-excel',
    command: () => exportResults('csv')
  },
  {
    label: 'Export as JSON',
    icon: 'pi pi-file',
    command: () => exportResults('json')
  }
])

// Real data from API
const articles = ref<Article[]>([])
const authors = ref<Author[]>([])
const totalArticles = ref(0)
const currentPage = ref(0)
const pageSize = ref(20)

// Filters with URL state management
const filters = ref<ArticleFilters>({
  authorId: null,
  titleSearch: '',
  dateRange: null,
  crawlerSource: null
})

// Initialize filters from URL query parameters
const initializeFiltersFromURL = () => {
  const query = route.query
  
  if (query.authorId) {
    filters.value.authorId = parseInt(query.authorId as string)
  }
  
  if (query.titleSearch) {
    filters.value.titleSearch = query.titleSearch as string
  }
  
  if (query.crawlerSource) {
    filters.value.crawlerSource = query.crawlerSource as string
  }
  
  if (query.startDate && query.endDate) {
    filters.value.dateRange = [
      new Date(query.startDate as string),
      new Date(query.endDate as string)
    ]
  }
}

// Update URL when filters change
const updateURLFromFilters = () => {
  const query: Record<string, string> = {}
  
  if (filters.value.authorId) {
    query.authorId = filters.value.authorId.toString()
  }
  
  if (filters.value.titleSearch) {
    query.titleSearch = filters.value.titleSearch
  }
  
  if (filters.value.crawlerSource) {
    query.crawlerSource = filters.value.crawlerSource
  }
  
  if (filters.value.dateRange && filters.value.dateRange.length === 2) {
    query.startDate = filters.value.dateRange[0].toISOString().split('T')[0]
    query.endDate = filters.value.dateRange[1].toISOString().split('T')[0]
  }
  
  router.replace({ query })
}

// Watch filters for URL state persistence and reload articles
watch(filters, async () => {
  updateURLFromFilters()
  // Reset to first page when filters change
  currentPage.value = 0
  // Reload articles with new filters
  await loadArticles()
}, { deep: true })

// Computed properties
const filteredArticles = computed(() => {
  let filtered = [...articles.value]

  if (filters.value.authorId) {
    filtered = filtered.filter(article => article.author?.id === filters.value.authorId)
  }

  if (filters.value.titleSearch) {
    const search = filters.value.titleSearch.toLowerCase()
    filtered = filtered.filter(article => 
      article.title.toLowerCase().includes(search)
    )
  }

  if (filters.value.dateRange && filters.value.dateRange.length === 2) {
    const [startDate, endDate] = filters.value.dateRange
    filtered = filtered.filter(article => {
      const articleDate = new Date(article.createdAt)
      return articleDate >= startDate && articleDate <= endDate
    })
  }

  if (filters.value.crawlerSource) {
    filtered = filtered.filter(article => 
      article.crawlerSource === filters.value.crawlerSource
    )
  }

  return filtered
})

// Update topbar actions based on current state
const updateTopBarActions = () => {
  if (activeStep.value === 0) {
    // Article selection step - show refresh and analyze actions
    topBarStore.setPredictionAnalysisActions({
      onRefreshArticles: refreshArticles,
      onAnalyzeArticles: startAnalysis
    }, {
      hasSelectedArticles: selectedArticles.value.length > 0,
      isRefreshing: isRefreshing.value,
      isAnalyzing: isAnalyzing.value
    })
  } else {
    // Results step - clear actions or show different actions
    topBarStore.clearContextActions()
  }
}

// Event handlers
const onFilterChanged = (newFilters: ArticleFilters) => {
  filters.value = { ...newFilters }
}

const onSelectionChanged = (newSelection: Article[]) => {
  selectedArticles.value = [...newSelection]
  updateTopBarActions() // Update actions when selection changes
}

// Step navigation
const goToStep = (step: number) => {
  activeStep.value = step
  updateTopBarActions() // Update actions when step changes
}

const showExportMenu = (event: Event) => {
  if (exportMenu.value) {
    exportMenu.value.toggle(event)
  }
}

// Methods
const refreshArticles = async () => {
  isRefreshing.value = true
  updateTopBarActions() // Update button states
  
  try {
    await loadArticles()
    
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Articles refreshed successfully',
      life: 3000
    })
  } catch (error) {
    console.error('Error refreshing articles:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to refresh articles'
    
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
      life: 5000
    })
  } finally {
    isRefreshing.value = false
    updateTopBarActions() // Update button states
  }
}

const loadArticles = async () => {
  isLoadingArticles.value = true
  
  try {
    // Build filter parameters
    const filterParams: Record<string, any> = {
      authorId: filters.value.authorId,
      titleSearch: filters.value.titleSearch || undefined,
      fromDate: filters.value.dateRange?.[0]?.toISOString(),
      toDate: filters.value.dateRange?.[1]?.toISOString(),
      page: currentPage.value,
      size: pageSize.value
    }

    // Remove undefined values
    Object.keys(filterParams).forEach(key => {
      if (filterParams[key] === undefined || filterParams[key] === null) {
        delete filterParams[key]
      }
    })

    const response = await ApiService.getArticlesForAnalysis(filterParams, { retries: 2 })
    
    articles.value = response.articles.map(article => ({
      id: article.id,
      title: article.title,
      url: article.url,
      text: article.text,
      crawlerSource: article.crawlerSource,
      createdAt: article.createdAt,
      author: article.author ? {
        id: article.author.id,
        name: article.author.name,
        avatarUrl: article.author.avatarUrl
      } : undefined,
      predictions: article.predictionInstances || []
    }))
    
    totalArticles.value = response.pagination.totalElements
    
  } catch (error) {
    console.error('Error loading articles:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to load articles'
    
    toast.add({
      severity: 'error',
      summary: 'Error Loading Articles',
      detail: errorMessage,
      life: 5000
    })
    
    // Set empty state on error
    articles.value = []
    totalArticles.value = 0
  } finally {
    isLoadingArticles.value = false
  }
}

const startAnalysis = async () => {
  if (selectedArticles.value.length === 0) return

  isAnalyzing.value = true
  updateTopBarActions() // Update button states
  
  // Navigate to results step
  activeStep.value = 1
  updateTopBarActions() // Update actions for new step
  
  try {
    const articleIds = selectedArticles.value.map(article => article.id)
    
    // Start the analysis job using configured analysis type
    const analysisType = analysisConfig.value?.preferredAnalysisType || 'llm'
    const job = await ApiService.startAnalysis(articleIds, analysisType, { retries: 1 })
    
    currentAnalysisJob.value = {
      id: job.id,
      jobId: job.jobId,
      totalArticles: job.totalArticles,
      processedArticles: job.processedArticles || 0,
      predictionsFound: job.predictionsFound || 0,
      status: job.status,
      startedAt: job.startedAt
    }
    
    toast.add({
      severity: 'info',
      summary: 'Analysis Started',
      detail: `Started analysis of ${selectedArticles.value.length} articles`,
      life: 3000
    })
    
    // Poll for job status updates (fallback if WebSocket doesn't work)
    pollJobStatus(job.jobId)
    
  } catch (error) {
    console.error('Error starting analysis:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to start prediction analysis'
    
    toast.add({
      severity: 'error',
      summary: 'Analysis Failed',
      detail: errorMessage,
      life: 5000
    })
    
    isAnalyzing.value = false
    currentAnalysisJob.value = null
    updateTopBarActions() // Update button states
    // Stay on results step to show error
  }
}

// Job status polling
let statusPollingInterval: NodeJS.Timeout | null = null

const pollJobStatus = (jobId: string) => {
  if (statusPollingInterval) {
    clearInterval(statusPollingInterval)
  }
  
  statusPollingInterval = setInterval(async () => {
    try {
      const job = await ApiService.getAnalysisJobStatus(jobId)
      
      if (currentAnalysisJob.value && currentAnalysisJob.value.jobId === jobId) {
        currentAnalysisJob.value = {
          ...currentAnalysisJob.value,
          ...job,
          processedArticles: job.processedArticles || 0,
          predictionsFound: job.predictionsFound || 0
        }
        
        // Check if job is complete
        if (job.status === 'COMPLETED') {
          isAnalyzing.value = false
          clearInterval(statusPollingInterval!)
          statusPollingInterval = null
          
          // Load results
          await loadAnalysisResults(jobId)
          
          toast.add({
            severity: 'success',
            summary: 'Analysis Complete',
            detail: `Found ${job.predictionsFound || 0} predictions`,
            life: 3000
          })
        } else if (job.status === 'FAILED') {
          isAnalyzing.value = false
          clearInterval(statusPollingInterval!)
          statusPollingInterval = null
          
          toast.add({
            severity: 'error',
            summary: 'Analysis Failed',
            detail: job.errorMessage || 'Analysis failed due to an error',
            life: 5000
          })
        }
      }
    } catch (error) {
      console.error('Error polling job status:', error)
      // Continue polling unless it's a critical error
      if (error instanceof ApiServiceError && error.status === 404) {
        // Job not found, stop polling
        clearInterval(statusPollingInterval!)
        statusPollingInterval = null
        isAnalyzing.value = false
        currentAnalysisJob.value = null
      }
    }
  }, 2000) // Poll every 2 seconds
}

const loadAnalysisResults = async (jobId: string) => {
  isLoadingResults.value = true
  
  try {
    const response = await ApiService.getAnalysisResults(jobId, 0, 100) // Load first 100 results
    
    analysisResults.value = response.results.map(result => ({
      id: result.id,
      predictionText: result.prediction.predictionText,
      rating: result.rating,
      confidenceScore: result.confidenceScore,
      context: result.context,
      article: {
        id: result.article.id,
        title: result.article.title,
        url: result.article.url,
        author: result.article.author
      },
      extractedAt: result.extractedAt
    }))
    
  } catch (error) {
    console.error('Error loading analysis results:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to load analysis results'
    
    toast.add({
      severity: 'error',
      summary: 'Error Loading Results',
      detail: errorMessage,
      life: 5000
    })
  } finally {
    isLoadingResults.value = false
  }
}

const cancelAnalysis = async () => {
  if (!currentAnalysisJob.value?.jobId) return
  
  try {
    await ApiService.cancelAnalysisJob(currentAnalysisJob.value.jobId)
    
    // Stop polling
    if (statusPollingInterval) {
      clearInterval(statusPollingInterval)
      statusPollingInterval = null
    }
    
    isAnalyzing.value = false
    currentAnalysisJob.value = null
    
    toast.add({
      severity: 'info',
      summary: 'Analysis Cancelled',
      detail: 'Prediction analysis has been cancelled',
      life: 3000
    })
  } catch (error) {
    console.error('Error cancelling analysis:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to cancel analysis'
    
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
      life: 5000
    })
  }
}

const retryAnalysis = () => {
  if (currentAnalysisJob.value) {
    // Reset job status and restart
    currentAnalysisJob.value.status = 'PENDING'
    currentAnalysisJob.value.processedArticles = 0
    currentAnalysisJob.value.predictionsFound = 0
    currentAnalysisJob.value.errorMessage = undefined
    startAnalysis()
  }
}

const onJobUpdated = (updatedJob: any) => {
  currentAnalysisJob.value = updatedJob
  
  // If job is completed, load results
  if (updatedJob.status === 'COMPLETED') {
    isAnalyzing.value = false
    // Results should already be in analysisResults from real-time updates
  } else if (updatedJob.status === 'FAILED') {
    isAnalyzing.value = false
    toast.add({
      severity: 'error',
      summary: 'Analysis Failed',
      detail: updatedJob.errorMessage || 'Analysis failed due to an error',
      life: 5000
    })
  }
}

const refreshResults = () => {
  isLoadingResults.value = true
  
  // Simulate refreshing results
  setTimeout(() => {
    isLoadingResults.value = false
    toast.add({
      severity: 'success',
      summary: 'Results Refreshed',
      detail: 'Analysis results have been refreshed',
      life: 2000
    })
  }, 1000)
}

const onPredictionSelected = (prediction: any) => {
  // Handle prediction selection - could emit event or navigate
  console.log('Prediction selected:', prediction)
}

// WebSocket setup for real-time updates
const setupWebSocketListeners = () => {
  const wsService = getWebSocketService()
  
  // Listen for analysis job updates
  const unsubscribeAnalysisJob = wsService.onAnalysisJobUpdate((message: AnalysisJobUpdateMessage) => {
    if (currentAnalysisJob.value && message.jobId === currentAnalysisJob.value.jobId) {
      // Update current job with real-time data
      currentAnalysisJob.value = {
        ...currentAnalysisJob.value,
        status: message.status,
        processedArticles: message.progress?.articlesProcessed || currentAnalysisJob.value.processedArticles,
        predictionsFound: message.progress?.predictionsFound || currentAnalysisJob.value.predictionsFound,
        currentActivity: message.progress?.currentActivity,
        completedAt: message.type === 'analysis.job.completed' || message.type === 'analysis.job.failed' 
          ? message.timestamp 
          : currentAnalysisJob.value.completedAt
      }
      
      // Handle job completion
      if (message.type === 'analysis.job.completed') {
        isAnalyzing.value = false
        toast.add({
          severity: 'success',
          summary: 'Analysis Complete',
          detail: `Found ${currentAnalysisJob.value.predictionsFound} predictions`,
          life: 3000
        })
      } else if (message.type === 'analysis.job.failed') {
        isAnalyzing.value = false
        toast.add({
          severity: 'error',
          summary: 'Analysis Failed',
          detail: message.details?.errorMessage || 'Analysis failed due to an error',
          life: 5000
        })
      }
    }
  })
  
  // Listen for individual prediction extractions
  const unsubscribePredictionExtracted = wsService.onPredictionExtracted((message: PredictionExtractedMessage) => {
    if (currentAnalysisJob.value && message.jobId === currentAnalysisJob.value.jobId) {
      // Add new prediction to results in real-time
      const newPrediction = {
        id: message.predictionId,
        predictionText: message.predictionText,
        rating: message.rating,
        confidenceScore: message.confidenceScore,
        article: selectedArticles.value.find(a => a.id === message.articleId),
        extractedAt: message.timestamp,
        context: 'Real-time extracted prediction'
      }
      
      analysisResults.value.push(newPrediction)
    }
  })
  
  // Store unsubscribe functions for cleanup
  onUnmounted(() => {
    unsubscribeAnalysisJob()
    unsubscribePredictionExtracted()
  })
}

const exportResults = async (format: string) => {
  if (!currentAnalysisJob.value?.jobId) return

  try {
    const blob = await ApiService.exportAnalysisResults(
      currentAnalysisJob.value.jobId, 
      format as 'csv' | 'json'
    )
    
    // Create download link
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `analysis-${currentAnalysisJob.value.jobId}.${format}`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)

    toast.add({
      severity: 'success',
      summary: 'Export Complete',
      detail: `Results exported as ${format.toUpperCase()}`,
      life: 3000
    })
  } catch (error) {
    console.error('Error exporting results:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to export results'
    
    toast.add({
      severity: 'error',
      summary: 'Export Failed',
      detail: errorMessage,
      life: 5000
    })
  }
}

// Load analysis configuration
const loadAnalysisConfiguration = async () => {
  try {
    analysisConfig.value = await ApiService.getConfiguration({ retries: 2 })
    console.log('Loaded analysis configuration:', analysisConfig.value)
  } catch (error) {
    console.error('Error loading analysis configuration:', error)
    // Use default configuration if loading fails
    analysisConfig.value = {
      preferredAnalysisType: 'llm',
      defaultExtractorType: 'gemini'
    }
  }
}

const loadAuthors = async () => {
  try {
    const authorsWithCounts = await ApiService.getAllAuthors({ retries: 2 })
    
    authors.value = authorsWithCounts.map(item => ({
      id: item.author.id,
      name: item.author.name,
      avatarUrl: item.author.avatarUrl,
      articleCount: item.articleCount
    }))
    
  } catch (error) {
    console.error('Error loading authors:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to load authors'
    
    toast.add({
      severity: 'warn',
      summary: 'Warning',
      detail: errorMessage,
      life: 3000
    })
    
    // Set empty authors on error
    authors.value = []
  }
}

const viewHistory = () => {
  router.push('/predictions/history')
}

// Initialize data and URL state
onMounted(async () => {
  try {
    // Initialize filters from URL
    initializeFiltersFromURL()
    
    // Load configuration first
    await loadAnalysisConfiguration()
    
    // Load authors first (needed for filters)
    await loadAuthors()
    
    // Load articles
    await loadArticles()
    
    // Set up WebSocket listeners for real-time updates
    setupWebSocketListeners()
    
    // Set up initial topbar actions
    updateTopBarActions()
    
  } catch (error) {
    console.error('Error initializing prediction analysis page:', error)
    toast.add({
      severity: 'error',
      summary: 'Initialization Error',
      detail: 'Failed to initialize the prediction analysis page',
      life: 5000
    })
  }
})

onUnmounted(() => {
  // Clean up polling interval
  if (statusPollingInterval) {
    clearInterval(statusPollingInterval)
    statusPollingInterval = null
  }
  
  // Clear topbar actions when leaving the view
  topBarStore.clearContextActions()
})
</script>

<style lang="scss" scoped>
@import "@/assets/themes/_variables.scss";

.predictions-analysis {
  padding: 1.5rem;
  min-height: calc(100vh - 7rem);
  background-color: var(--surface-ground);
}

.page-header {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  padding: 2rem;
  margin-bottom: 1.5rem;
}

.steps-container {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  padding: 1.5rem;
  
  :deep(.p-steps) {
    .p-steps-item {
      .p-steps-number {
        background: var(--surface-100);
        color: var(--text-color-secondary);
        border: 2px solid var(--surface-200);
        width: 2.5rem;
        height: 2.5rem;
        font-size: 1rem;
        font-weight: 600;
      }
      
      .p-steps-title {
        color: var(--text-color-secondary);
        font-weight: 500;
        margin-top: 0.5rem;
      }
      
      &.p-highlight {
        .p-steps-number {
          background: var(--primary-color);
          color: var(--primary-color-text);
          border-color: var(--primary-color);
        }
        
        .p-steps-title {
          color: var(--primary-color);
          font-weight: 600;
        }
      }
    }
  }
}

.step-content {
  .step-panel {
    :deep(.p-panel) {
      .p-panel-header {
        background: var(--surface-card);
        border: 1px solid var(--surface-border);
        border-bottom: none;
        padding: 1.25rem 1.5rem;
        
        .p-panel-title {
          font-size: 1.1rem;
          font-weight: 600;
        }
      }
      
      .p-panel-content {
        background: var(--surface-card);
        border: 1px solid var(--surface-border);
        border-top: none;
        padding: 1.5rem;
      }
    }
  }
}

.selection-summary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
}

// Steps customization
:deep(.custom-steps) {
  .p-steps-item {
    flex: 1;
    
    &:not(:last-child)::before {
      content: '';
      position: absolute;
      top: 1.25rem;
      left: calc(50% + 1.25rem);
      width: calc(100% - 2.5rem);
      height: 2px;
      background: var(--surface-200);
      z-index: 1;
    }
    
    &.p-highlight:not(:last-child)::before {
      background: var(--primary-color);
    }
  }
}

// Panel customizations
:deep(.p-panel) {
  .p-panel-header {
    background: var(--surface-card);
    border: 1px solid var(--surface-border);
    border-bottom: none;
    padding: 1rem 1.25rem;
  }

  .p-panel-content {
    background: var(--surface-card);
    border: 1px solid var(--surface-border);
    border-top: none;
    padding: 1.25rem;
  }
}

// Button spacing
.flex.gap-2 {
  gap: 0.5rem;
}

// Responsive design
@media screen and (max-width: 991px) {
  .predictions-analysis {
    padding: 1rem;
  }
  
  .page-header {
    padding: 1.5rem;
    
    h1 {
      font-size: 1.5rem;
    }
  }
  
  .steps-container {
    padding: 1rem;
  }
  
  .step-content {
    :deep(.p-panel-content) {
      padding: 1rem;
    }
  }
  
  .flex.justify-content-between {
    flex-direction: column;
    gap: 1rem;
    
    .flex.gap-2 {
      justify-content: center;
    }
  }
}

@media screen and (max-width: 576px) {
  .predictions-analysis {
    padding: 0.75rem;
  }
  
  .page-header {
    padding: 1rem;
    
    .flex.align-items-center {
      flex-direction: column;
      text-align: center;
      gap: 1rem;
    }
  }
  
  :deep(.custom-steps) {
    .p-steps-item {
      .p-steps-title {
        font-size: 0.875rem;
      }
    }
  }
}

// Dark theme adjustments
:global([data-theme="dark"]) {
  .page-header,
  .steps-container {
    background-color: var(--surface-card);
    border-color: var(--surface-border);
  }
}
</style>