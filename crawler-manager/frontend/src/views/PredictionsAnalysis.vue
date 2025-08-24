<template>
  <div class="predictions-analysis">
    <!-- Page Header -->
    <div class="page-header">
      <div class="flex align-items-center justify-content-between">
        <div>
          <h1>Analyze Articles</h1>
          <p class="text-color-secondary">Select articles and analyze predictions using AI</p>
        </div>
      </div>
    </div>

    <!-- Article Selection Section -->
    <div class="article-selection-section">
      <Panel header="Article Selection" :toggleable="false">
        <!-- Filters -->
        <div class="filters-section mb-4">
          <div class="grid">
            <div class="col-12 md:col-3">
              <label for="author-filter" class="block text-sm font-medium mb-2">Author</label>
              <Dropdown
                id="author-filter"
                v-model="filters.authorId"
                :options="authors"
                option-label="name"
                option-value="id"
                placeholder="All Authors"
                :show-clear="true"
                class="w-full"
              >
                <template #option="slotProps">
                  <div class="flex align-items-center">
                    <Avatar
                      v-if="slotProps.option.avatarUrl"
                      :image="slotProps.option.avatarUrl"
                      size="small"
                      class="mr-2"
                    />
                    <Avatar
                      v-else
                      :label="slotProps.option.name.charAt(0)"
                      size="small"
                      class="mr-2"
                    />
                    <span>{{ slotProps.option.name }} ({{ slotProps.option.articleCount }})</span>
                  </div>
                </template>
              </Dropdown>
            </div>
            
            <div class="col-12 md:col-3">
              <label for="title-search" class="block text-sm font-medium mb-2">Title Search</label>
              <InputText
                id="title-search"
                v-model="filters.titleSearch"
                placeholder="Search article titles..."
                class="w-full"
              />
            </div>
            
            <div class="col-12 md:col-3">
              <label for="date-range" class="block text-sm font-medium mb-2">Date Range</label>
              <Calendar
                id="date-range"
                v-model="filters.dateRange"
                selection-mode="range"
                :show-icon="true"
                placeholder="Select date range"
                class="w-full"
              />
            </div>
            
            <div class="col-12 md:col-3">
              <label for="crawler-source" class="block text-sm font-medium mb-2">Source</label>
              <Dropdown
                id="crawler-source"
                v-model="filters.crawlerSource"
                :options="['drucker', 'caspit']"
                placeholder="All Sources"
                :show-clear="true"
                class="w-full"
              />
            </div>
          </div>
        </div>

        <!-- Articles Table -->
        <div class="articles-table-container">
          <DataTable
            v-model:selection="selectedArticles"
            :value="filteredArticles"
            :loading="isLoadingArticles"
            selection-mode="multiple"
            :meta-key-selection="false"
            data-key="id"
            :paginator="true"
            :rows="20"
            :total-records="totalArticles"
            :lazy="true"
            @page="onPageChange"
            @selection-change="onSelectionChanged"
            class="articles-table"
            :scroll-height="'400px'"
            :scrollable="true"
          >
            <template #header>
              <div class="flex justify-content-between align-items-center">
                <div class="selection-summary">
                  <span class="font-semibold">{{ selectedArticles.length }} articles selected</span>
                  <span v-if="totalArticles > 0" class="text-color-secondary ml-2">
                    ({{ totalArticles }} total available)
                  </span>
                </div>
                <div class="flex gap-2">
                  <Button
                    icon="pi pi-refresh"
                    label="Refresh"
                    :loading="isRefreshing"
                    @click="refreshArticles"
                    size="small"
                  />
                  <Button
                    icon="pi pi-times"
                    label="Clear Selection"
                    :disabled="selectedArticles.length === 0"
                    @click="clearSelection"
                    severity="secondary"
                    size="small"
                  />
                  <Button
                    icon="pi pi-search"
                    label="Analyze Selected"
                    :disabled="selectedArticles.length === 0 || isAnalyzing"
                    :loading="isAnalyzing"
                    @click="startAnalysis"
                    size="small"
                  />
                </div>
              </div>
            </template>

            <Column selection-mode="multiple" header-style="width: 3rem"></Column>
            
            <Column field="title" header="Title" :sortable="true" style="min-width: 300px">
              <template #body="slotProps">
                <div class="article-title">
                  <a :href="slotProps.data.url" target="_blank" class="text-primary no-underline">
                    {{ slotProps.data.title }}
                  </a>
                </div>
              </template>
            </Column>
            
            <Column field="author.name" header="Author" :sortable="true" style="min-width: 150px">
              <template #body="slotProps">
                <div v-if="slotProps.data.author" class="flex align-items-center">
                  <Avatar
                    v-if="slotProps.data.author.avatarUrl"
                    :image="slotProps.data.author.avatarUrl"
                    size="small"
                    class="mr-2"
                  />
                  <Avatar
                    v-else
                    :label="slotProps.data.author.name.charAt(0)"
                    size="small"
                    class="mr-2"
                  />
                  <span>{{ slotProps.data.author.name }}</span>
                </div>
              </template>
            </Column>
            
            <Column field="createdAt" header="Date" :sortable="true" style="min-width: 120px">
              <template #body="slotProps">
                {{ new Date(slotProps.data.createdAt).toLocaleDateString() }}
              </template>
            </Column>
            
            <Column field="crawlerSource" header="Source" :sortable="true" style="min-width: 100px">
              <template #body="slotProps">
                <Tag :value="slotProps.data.crawlerSource" :severity="slotProps.data.crawlerSource === 'drucker' ? 'info' : 'success'" />
              </template>
            </Column>

            <template #empty>
              <div class="text-center py-4">
                <i class="pi pi-search text-4xl text-color-secondary mb-3"></i>
                <p class="text-color-secondary">No articles found matching your criteria</p>
                <Button label="Clear Filters" @click="clearFilters" size="small" />
              </div>
            </template>
          </DataTable>
        </div>
      </Panel>
    </div>

    <!-- Analysis Progress Section -->
    <div v-if="isAnalyzing || currentAnalysisJob" class="analysis-progress-section mt-4">
      <Panel header="Analysis Progress" :toggleable="false">
        <div v-if="currentAnalysisJob" class="progress-content">
          <div class="grid">
            <div class="col-12 md:col-8">
              <div class="progress-info">
                <div class="flex align-items-center mb-3">
                  <ProgressSpinner v-if="isAnalyzing" size="small" class="mr-2" />
                  <i v-else-if="currentAnalysisJob.status === 'COMPLETED'" class="pi pi-check-circle text-green-500 mr-2"></i>
                  <i v-else-if="currentAnalysisJob.status === 'FAILED'" class="pi pi-times-circle text-red-500 mr-2"></i>
                  <span class="font-semibold">{{ getStatusLabel(currentAnalysisJob.status) }}</span>
                </div>
                
                <div v-if="currentAnalysisJob.totalArticles" class="mb-3">
                  <div class="flex justify-content-between mb-2">
                    <span>Progress: {{ currentAnalysisJob.processedArticles || 0 }} / {{ currentAnalysisJob.totalArticles }}</span>
                    <span>{{ Math.round(((currentAnalysisJob.processedArticles || 0) / currentAnalysisJob.totalArticles) * 100) }}%</span>
                  </div>
                  <ProgressBar 
                    :value="Math.round(((currentAnalysisJob.processedArticles || 0) / currentAnalysisJob.totalArticles) * 100)"
                    class="mb-2"
                  />
                </div>
                
                <div v-if="currentAnalysisJob.currentActivity" class="text-sm text-color-secondary mb-2">
                  Current: {{ currentAnalysisJob.currentActivity }}
                </div>
                
                <div class="text-sm text-color-secondary">
                  Predictions found: {{ currentAnalysisJob.predictionsFound || 0 }}
                </div>
              </div>
            </div>
            
            <div class="col-12 md:col-4">
              <div class="flex flex-column gap-2">
                <Button
                  v-if="isAnalyzing"
                  icon="pi pi-times"
                  label="Cancel Analysis"
                  severity="danger"
                  @click="cancelAnalysis"
                  size="small"
                />
                <Button
                  v-else-if="currentAnalysisJob.status === 'FAILED'"
                  icon="pi pi-refresh"
                  label="Retry Analysis"
                  @click="retryAnalysis"
                  size="small"
                />
              </div>
            </div>
          </div>
        </div>
      </Panel>
    </div>

    <!-- Results Section -->
    <div v-if="analysisResults.length > 0" class="results-section mt-4">
      <Panel header="Analysis Results" :toggleable="false">
        <!-- Results Summary -->
        <div class="results-summary mb-4">
          <div class="grid">
            <div class="col-12 md:col-3">
              <div class="summary-item">
                <div class="text-2xl font-bold text-primary">{{ analysisResults.length }}</div>
                <div class="text-sm text-color-secondary">Total Predictions</div>
              </div>
            </div>
            <div class="col-12 md:col-3">
              <div class="summary-item">
                <div class="text-2xl font-bold text-primary">{{ averageRating.toFixed(1) }}</div>
                <div class="text-sm text-color-secondary">Average Rating</div>
              </div>
            </div>
            <div class="col-12 md:col-3">
              <div class="summary-item">
                <div class="text-2xl font-bold text-primary">{{ highConfidenceCount }}</div>
                <div class="text-sm text-color-secondary">High Confidence</div>
              </div>
            </div>
            <div class="col-12 md:col-3">
              <div class="summary-item">
                <div class="text-2xl font-bold text-primary">{{ uniqueArticlesCount }}</div>
                <div class="text-sm text-color-secondary">Articles Analyzed</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Results Filters -->
        <div class="results-filters mb-4">
          <div class="grid">
            <div class="col-12 md:col-3">
              <label for="min-rating" class="block text-sm font-medium mb-2">Minimum Rating</label>
              <Dropdown
                id="min-rating"
                v-model="resultsFilters.minRating"
                :options="ratingOptions"
                option-label="label"
                option-value="value"
                placeholder="All Ratings"
                :show-clear="true"
                class="w-full"
              />
            </div>
            
            <div class="col-12 md:col-3">
              <label for="min-confidence" class="block text-sm font-medium mb-2">Minimum Confidence (%)</label>
              <Slider
                id="min-confidence"
                v-model="resultsFilters.minConfidence"
                :min="0"
                :max="100"
                class="w-full"
              />
              <div class="text-sm text-color-secondary mt-1">{{ resultsFilters.minConfidence }}%</div>
            </div>
            
            <div class="col-12 md:col-4">
              <label for="search-results" class="block text-sm font-medium mb-2">Search Results</label>
              <InputText
                id="search-results"
                v-model="resultsFilters.searchText"
                placeholder="Search predictions, articles, or authors..."
                class="w-full"
              />
            </div>
            
            <div class="col-12 md:col-2">
              <label class="block text-sm font-medium mb-2">&nbsp;</label>
              <div class="flex gap-2">
                <Button
                  icon="pi pi-filter-slash"
                  label="Clear"
                  @click="clearResultsFilters"
                  severity="secondary"
                  size="small"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- Results Table -->
        <DataTable
          :value="filteredResults"
          :loading="isLoadingResults"
          :paginator="true"
          :rows="10"
          data-key="id"
          class="results-table"
        >
          <template #header>
            <div class="flex justify-content-between align-items-center">
              <span class="font-semibold">{{ filteredResults.length }} predictions found</span>
              <div class="flex gap-2">
                <Button
                  icon="pi pi-refresh"
                  label="Refresh"
                  @click="refreshResults"
                  size="small"
                />
                <Button
                  icon="pi pi-download"
                  label="Export"
                  @click="showExportMenu"
                  size="small"
                />
                <Menu ref="exportMenu" :model="exportMenuItems" :popup="true" />
                <Button
                  icon="pi pi-plus"
                  label="New Analysis"
                  @click="startNewAnalysis"
                  size="small"
                />
              </div>
            </div>
          </template>

          <Column field="predictionText" header="Prediction" style="min-width: 300px">
            <template #body="slotProps">
              <div class="prediction-text">
                {{ slotProps.data.predictionText }}
              </div>
            </template>
          </Column>
          
          <Column field="rating" header="Rating" :sortable="true" style="min-width: 120px">
            <template #body="slotProps">
              <Rating :model-value="slotProps.data.rating" :readonly="true" :cancel="false" />
            </template>
          </Column>
          
          <Column field="confidenceScore" header="Confidence" :sortable="true" style="min-width: 120px">
            <template #body="slotProps">
              <div class="confidence-score">
                <ProgressBar 
                  :value="Math.round((slotProps.data.confidenceScore || 0) * 100)"
                  :show-value="true"
                  class="confidence-bar"
                />
              </div>
            </template>
          </Column>
          
          <Column field="article.title" header="Article" style="min-width: 200px">
            <template #body="slotProps">
              <div v-if="slotProps.data.article">
                <a :href="slotProps.data.article.url" target="_blank" class="text-primary no-underline">
                  {{ slotProps.data.article.title }}
                </a>
                <div v-if="slotProps.data.article.author" class="text-sm text-color-secondary mt-1">
                  by {{ slotProps.data.article.author.name }}
                </div>
              </div>
            </template>
          </Column>
          
          <Column field="extractedAt" header="Extracted" :sortable="true" style="min-width: 120px">
            <template #body="slotProps">
              {{ new Date(slotProps.data.extractedAt).toLocaleDateString() }}
            </template>
          </Column>

          <template #empty>
            <div class="text-center py-4">
              <i class="pi pi-search text-4xl text-color-secondary mb-3"></i>
              <p class="text-color-secondary">No predictions found matching your criteria</p>
            </div>
          </template>
        </DataTable>

        <!-- Results Actions -->
        <div class="results-actions mt-4">
          <div class="flex justify-content-center gap-3">
            <Button
              icon="pi pi-history"
              label="View History"
              @click="viewHistory"
              severity="secondary"
            />
            <Button
              icon="pi pi-eye"
              label="Detailed Results"
              @click="viewDetailedResults"
              severity="secondary"
            />
          </div>
        </div>
      </Panel>
    </div>

    <!-- Empty State -->
    <div v-if="!isAnalyzing && !currentAnalysisJob && analysisResults.length === 0" class="empty-state mt-4">
      <Panel>
        <div class="text-center py-6">
          <i class="pi pi-search text-6xl text-color-secondary mb-4"></i>
          <h3 class="text-color-secondary mb-3">Ready to Analyze Articles</h3>
          <p class="text-color-secondary mb-4">
            Select articles from the table above and click "Analyze Selected" to start prediction analysis.
          </p>
          <div class="flex justify-content-center gap-3">
            <Button
              icon="pi pi-history"
              label="View Previous Results"
              @click="viewHistory"
              severity="secondary"
            />
          </div>
        </div>
      </Panel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from 'primevue/usetoast'

// PrimeVue Components
import Panel from 'primevue/panel'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import Calendar from 'primevue/calendar'
import Avatar from 'primevue/avatar'
import Tag from 'primevue/tag'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import Rating from 'primevue/rating'
import Slider from 'primevue/slider'
import Menu from 'primevue/menu'

// Services and Types
import { ApiService, ApiServiceError } from '@/services/api'
import { getPredictionAnalysisWebSocketService } from '@/services/prediction-websocket'

import type { 
  Article, 
  Author, 
  ArticleFilters
} from '@/types'

const router = useRouter()
const route = useRoute()
const toast = useToast()

// Reactive state
const isLoadingArticles = ref(false)
const isRefreshing = ref(false)
const isAnalyzing = ref(false)
const isLoadingResults = ref(false)
const selectedArticles = ref<Article[]>([])
const currentAnalysisJob = ref<any>(null)
const analysisResults = ref<any[]>([])
const analysisConfig = ref<any>(null)

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

// Results filtering and sorting
const resultsFilters = ref({
  minRating: null as number | null,
  minConfidence: 0,
  searchText: ''
})

const ratingOptions = [
  { label: '5 Stars', value: 5 },
  { label: '4+ Stars', value: 4 },
  { label: '3+ Stars', value: 3 },
  { label: '2+ Stars', value: 2 },
  { label: '1+ Stars', value: 1 }
]

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

// Results computed properties
const averageRating = computed(() => {
  if (analysisResults.value.length === 0) return 0
  const sum = analysisResults.value.reduce((acc, pred) => acc + (pred.rating || 0), 0)
  return sum / analysisResults.value.length
})

const highConfidenceCount = computed(() => {
  return analysisResults.value.filter(pred => (pred.confidenceScore || 0) > 0.8).length
})

const uniqueArticlesCount = computed(() => {
  const articleIds = new Set(analysisResults.value.map(pred => pred.article?.id).filter(Boolean))
  return articleIds.size
})

const filteredResults = computed(() => {
  let filtered = [...analysisResults.value]
  
  // Rating filter
  if (resultsFilters.value.minRating !== null) {
    filtered = filtered.filter(pred => (pred.rating || 0) >= resultsFilters.value.minRating!)
  }
  
  // Confidence filter
  if (resultsFilters.value.minConfidence > 0) {
    filtered = filtered.filter(pred => (pred.confidenceScore || 0) >= resultsFilters.value.minConfidence / 100)
  }
  
  // Search filter
  if (resultsFilters.value.searchText) {
    const search = resultsFilters.value.searchText.toLowerCase()
    filtered = filtered.filter(pred => 
      pred.predictionText?.toLowerCase().includes(search) ||
      pred.article?.title?.toLowerCase().includes(search) ||
      pred.article?.author?.name?.toLowerCase().includes(search)
    )
  }
  
  return filtered
})



// Event handlers
const onSelectionChanged = (event: any) => {
  selectedArticles.value = [...event.value]
}

const onPageChange = (event: any) => {
  currentPage.value = event.page
  loadArticles()
}

const clearSelection = () => {
  selectedArticles.value = []
}

const clearFilters = () => {
  filters.value = {
    authorId: null,
    titleSearch: '',
    dateRange: null,
    crawlerSource: null
  }
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'PENDING': return 'Starting Analysis...'
    case 'RUNNING': return 'Analyzing Articles...'
    case 'COMPLETED': return 'Analysis Complete'
    case 'FAILED': return 'Analysis Failed'
    default: return status
  }
}

const showExportMenu = (event: Event) => {
  if (exportMenu.value) {
    exportMenu.value.toggle(event)
  }
}

// Methods
const refreshArticles = async () => {
  isRefreshing.value = true
  
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
  }
}

// Start prediction analysis
const startAnalysis = async () => {
  console.log('startAnalysis called, selectedArticles:', selectedArticles.value)
  console.log('selectedArticles length:', selectedArticles.value.length)
  console.log('isAnalyzing current value:', isAnalyzing.value)
  
  if (selectedArticles.value.length === 0) {
    toast.add({
      severity: 'warn',
      summary: 'No Articles Selected',
      detail: 'Please select at least one article to analyze',
      life: 3000
    })
    return
  }

  if (isAnalyzing.value) {
    console.log('Analysis already in progress, ignoring duplicate request')
    return
  }

  // Set analyzing flag immediately to prevent duplicate requests
  isAnalyzing.value = true
  console.log('Set isAnalyzing to true, starting analysis...')

  try {
    
    const articleIds = selectedArticles.value.map(article => article.id)
    const analysisType = analysisConfig.value?.preferredAnalysisType || 'llm'
    
    console.log('Starting analysis for articles:', articleIds, 'with type:', analysisType)
    console.log('Selected articles details:', selectedArticles.value.map(a => ({ id: a.id, title: a.title?.substring(0, 50) })))
    
    const job = await ApiService.startAnalysis(articleIds, analysisType)
    console.log('API response for startAnalysis:', job)
    
    currentAnalysisJob.value = {
      jobId: job.jobId,
      status: job.status || 'PENDING',
      startedAt: job.startedAt || new Date().toISOString(),
      totalArticles: selectedArticles.value.length,
      processedArticles: 0,
      predictionsFound: 0,
      analysisType: analysisType
    }
    
    console.log('Analysis job created and assigned to currentAnalysisJob.value:', currentAnalysisJob.value)
    console.log('currentAnalysisJob.value is now:', currentAnalysisJob.value)
    
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
          console.log('Job completed, loading results for job:', jobId)
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
  console.log('Loading analysis results for job:', jobId)
  isLoadingResults.value = true
  
  try {
    console.log('Making API call to get analysis results...')
    const response = await ApiService.getAnalysisResults(jobId, 0, 100) // Load first 100 results
    console.log('API response received:', response)
    
    // Handle different response formats from backend
    // The backend returns List<PredictionInstanceEntity> directly, not wrapped in an object
    const results = Array.isArray(response) ? response : (response.results || [])
    
    analysisResults.value = results.map(result => ({
      id: result.id,
      predictionText: result.prediction?.predictionText || result.predictionText || 'No prediction text',
      rating: result.rating || 0,
      confidenceScore: result.confidenceScore || 0,
      context: result.context || '',
      article: {
        id: result.article?.id || 0,
        title: result.article?.title || 'Unknown Article',
        url: result.article?.url || '#',
        author: result.article?.author || null
      },
      extractedAt: result.extractedAt || new Date().toISOString()
    }))
    
    console.log('Analysis results loaded:', analysisResults.value.length, 'predictions')
    
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

// WebSocket setup for real-time updates
const setupWebSocketListeners = () => {
  try {
    const wsService = getPredictionAnalysisWebSocketService()
    console.log('WebSocket service obtained:', wsService)
    console.log('WebSocket connection status:', wsService.getConnectionStatus())
    
    // Listen for connection status changes
    wsService.onConnectionStatusChange((status) => {
      console.log('WebSocket connection status changed:', status)
    })
    
    // Listen for analysis job updates
    wsService.onAnalysisJobUpdate((message: any) => {
      try {
        console.log('Prediction analysis WebSocket message received:', message)
        console.log('Current job ID:', currentAnalysisJob.value?.jobId || 'null')
        console.log('Message job ID:', message.jobId)
        
        // If we don't have a current job but receive a message, create the job object
        if (!currentAnalysisJob.value && message.jobId) {
          console.log('Creating currentAnalysisJob from WebSocket message')
          currentAnalysisJob.value = {
            jobId: message.jobId,
            status: 'RUNNING',
            startedAt: new Date().toISOString(),
            totalArticles: selectedArticles.value.length || 0,
            processedArticles: 0,
            predictionsFound: 0,
            analysisType: 'llm'
          }
        }
        
        // Process message if we have a job and the IDs match, or if we just created the job
        if (currentAnalysisJob.value && (message.jobId === currentAnalysisJob.value.jobId || !currentAnalysisJob.value.jobId)) {
          // Update job ID if it wasn't set
          if (!currentAnalysisJob.value.jobId && message.jobId) {
            currentAnalysisJob.value.jobId = message.jobId
          }
          
          // Handle different message types from the backend
          if (message.type === 'job.status.update') {
            currentAnalysisJob.value = {
              ...currentAnalysisJob.value,
              status: message.data?.status || 'RUNNING'
            }
            console.log('Updated job status to:', currentAnalysisJob.value.status)
          } else if (message.type === 'job.progress.update') {
            currentAnalysisJob.value = {
              ...currentAnalysisJob.value,
              processedArticles: message.data?.processedArticles || currentAnalysisJob.value.processedArticles,
              predictionsFound: message.data?.predictionsFound || currentAnalysisJob.value.predictionsFound,
              currentActivity: message.data?.currentActivity || currentAnalysisJob.value.currentActivity
            }
            console.log('Updated job progress:', {
              processed: currentAnalysisJob.value.processedArticles,
              predictions: currentAnalysisJob.value.predictionsFound
            })
          } else if (message.type === 'job.completed') {
            console.log('Job completed via WebSocket, loading results for job:', message.jobId)
            isAnalyzing.value = false
            
            // Stop polling since we got WebSocket notification
            if (statusPollingInterval) {
              clearInterval(statusPollingInterval)
              statusPollingInterval = null
            }
            
            // Update job status
            currentAnalysisJob.value = {
              ...currentAnalysisJob.value,
              status: 'COMPLETED',
              processedArticles: message.data?.processedArticles || currentAnalysisJob.value.processedArticles,
              predictionsFound: message.data?.totalPredictions || currentAnalysisJob.value.predictionsFound,
              completedAt: new Date().toISOString()
            }
            
            // Load results
            loadAnalysisResults(message.jobId)
            
            toast.add({
              severity: 'success',
              summary: 'Analysis Complete',
              detail: `Found ${message.data?.totalPredictions || 0} predictions`,
              life: 3000
            })
          } else if (message.type === 'job.failed') {
            isAnalyzing.value = false
            
            // Stop polling
            if (statusPollingInterval) {
              clearInterval(statusPollingInterval)
              statusPollingInterval = null
            }
            
            currentAnalysisJob.value = {
              ...currentAnalysisJob.value,
              status: 'FAILED',
              errorMessage: message.data?.errorMessage
            }
            
            toast.add({
              severity: 'error',
              summary: 'Analysis Failed',
              detail: message.data?.errorMessage || 'Analysis failed due to an error',
              life: 5000
            })
          }
        } else {
          console.log('Ignoring WebSocket message - job ID mismatch or no current job')
        }
      } catch (error) {
        console.error('Error in prediction analysis WebSocket message listener:', error)
      }
    })
    
    console.log('WebSocket listeners set up successfully')
  } catch (error) {
    console.error('Error setting up WebSocket listeners:', error)
  }
}

const viewHistory = () => {
  router.push('/predictions/history')
}

const clearResultsFilters = () => {
  resultsFilters.value = {
    minRating: null,
    minConfidence: 0,
    searchText: ''
  }
}

const startNewAnalysis = () => {
  // Reset state for new analysis
  currentAnalysisJob.value = null
  analysisResults.value = []
  selectedArticles.value = []
  clearResultsFilters()
}

const viewDetailedResults = () => {
  // Navigate to a detailed results page (if it exists)
  router.push('/predictions/results')
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

.article-selection-section,
.analysis-progress-section,
.results-section {
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

.filters-section {
  background: var(--surface-50);
  border: 1px solid var(--surface-200);
  border-radius: var(--border-radius);
  padding: 1rem;
}

.articles-table-container {
  .articles-table {
    :deep(.p-datatable-header) {
      background: var(--surface-100);
      border: 1px solid var(--surface-border);
      padding: 1rem;
    }
  }
}

.selection-summary {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.95rem;
}

.progress-content {
  .progress-info {
    .confidence-bar {
      height: 0.5rem;
    }
  }
}

.results-summary {
  .summary-item {
    text-align: center;
    padding: 1rem;
    background: var(--surface-50);
    border-radius: var(--border-radius);
    border: 1px solid var(--surface-200);
  }
}

.results-filters {
  background: var(--surface-50);
  border: 1px solid var(--surface-200);
  border-radius: var(--border-radius);
  padding: 1rem;
}

.results-table {
  .prediction-text {
    max-width: 300px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .confidence-score {
    .confidence-bar {
      height: 0.75rem;
    }
  }
}

.empty-state {
  background: var(--surface-50);
  border: 1px solid var(--surface-200);
  border-radius: var(--border-radius);
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

// Results styling
.results-summary {
  font-size: 0.875rem;
}

.job-summary {
  .summary-item {
    text-align: center;
    padding: 1rem;
    background: var(--surface-50);
    border-radius: var(--border-radius);
    border: 1px solid var(--surface-200);
  }
}

.empty-state {
  background: var(--surface-50);
  border: 1px solid var(--surface-200);
  border-radius: var(--border-radius);
  margin: 2rem 0;
}

.results-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

// Dark theme adjustments
:global([data-theme="dark"]) {
  .page-header,
  .steps-container {
    background-color: var(--surface-card);
    border-color: var(--surface-border);
  }
  
  .job-summary .summary-item {
    background: var(--surface-700);
    border-color: var(--surface-600);
  }
  
  .empty-state {
    background: var(--surface-700);
    border-color: var(--surface-600);
  }
}
</style>