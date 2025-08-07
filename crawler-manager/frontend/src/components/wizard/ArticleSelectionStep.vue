<template>
  <div class="article-selection-step">
    <!-- Step Header with Selection Summary -->
    <Panel class="mb-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center">
            <i class="pi pi-list mr-2 text-primary"></i>
            <span class="font-semibold">Select Articles for Analysis</span>
          </div>
          <div class="flex align-items-center gap-2">
            <Badge 
              :value="selectedArticles.length" 
              :severity="selectedArticles.length > 0 ? 'success' : 'info'"
            />
            <Chip 
              :label="`${selectedArticles.length} of ${filteredArticles.length} selected`" 
              class="selection-summary"
            />
          </div>
        </div>
      </template>
      
      <div class="step-description">
        <div class="text-600 mb-3">
          Choose articles from the list below to analyze for predictions. You can filter articles by author, title, date range, or source.
        </div>
        
        <!-- Selection validation messages -->
        <div v-if="validationErrors.length > 0" class="validation-messages mb-3">
          <Message 
            v-for="error in validationErrors" 
            :key="error"
            severity="error" 
            :closable="false"
            class="mb-2"
          >
            {{ error }}
          </Message>
        </div>
        
        <div v-if="validationWarnings.length > 0" class="validation-messages mb-3">
          <Message 
            v-for="warning in validationWarnings" 
            :key="warning"
            severity="warn" 
            :closable="false"
            class="mb-2"
          >
            {{ warning }}
          </Message>
        </div>
        
        <!-- Selection info -->
        <div v-if="selectedArticles.length > 0" class="selection-info">
          <div class="flex align-items-center gap-2 text-sm text-600">
            <i class="pi pi-info-circle text-blue-500"></i>
            <span v-if="selectedArticles.length > 100" class="text-orange-600 font-medium">
              Large batch analysis may take several minutes
            </span>
            <span v-else>
              Estimated analysis time: {{ estimatedTime }} minutes
            </span>
          </div>
        </div>
      </div>
    </Panel>

    <!-- Article Filters -->
    <ArticleFilter 
      :filters="filters"
      :authors="authors"
      @filter-changed="onFilterChanged"
      class="mb-4"
    />

    <!-- Article Selection Table with Built-in Controls -->
    <Panel class="article-table-panel">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center gap-2">
            <i class="pi pi-table text-primary"></i>
            <span class="font-semibold">Available Articles</span>
            <Tag 
              v-if="selectedArticles.length > 0"
              :value="`${selectedArticles.length} selected`" 
              severity="success"
            />
          </div>
          <div class="flex align-items-center gap-2">
            <Button 
              icon="pi pi-refresh"
              label="Refresh"
              size="small"
              severity="secondary"
              outlined
              @click="refreshArticles"
              :loading="isRefreshing"
              v-tooltip.top="'Refresh article list'"
            />
            <Button 
              v-if="selectedArticles.length > 0"
              icon="pi pi-times"
              label="Clear Selection"
              size="small"
              severity="secondary"
              outlined
              @click="clearSelection"
              v-tooltip.top="'Clear all selected articles'"
            />
          </div>
        </div>
      </template>
      
      <!-- Enhanced Article Selection Table -->
      <ArticleSelectionTable 
        :articles="filteredArticles"
        :selected-articles="selectedArticles"
        :loading="isLoadingArticles"
        @selection-changed="onSelectionChanged"
        @refresh-articles="refreshArticles"
        class="wizard-article-table"
      />
    </Panel>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import { ApiService, ApiServiceError } from '@/services/api'
import Panel from 'primevue/panel'
import Button from 'primevue/button'
import Badge from 'primevue/badge'
import Chip from 'primevue/chip'
import Tag from 'primevue/tag'
import Message from 'primevue/message'

import ArticleFilter, { type ArticleFilters, type Author } from '@/components/ArticleFilter.vue'
import ArticleSelectionTable, { type Article } from '@/components/ArticleSelectionTable.vue'
import { ArticleSelectionValidator, type WizardStepData } from './WizardStepBase'

// Props
interface Props {
  wizardState: any
  stepData: any
  isActive: boolean
}

const props = defineProps<Props>()

// Emits
const emit = defineEmits<{
  'step-valid': [isValid: boolean]
  'step-complete': [stepId: string]
  'step-data': [data: any]
}>()

const toast = useToast()

// Reactive state
const selectedArticles = ref<Article[]>([])
const articles = ref<Article[]>([])
const authors = ref<Author[]>([])
const isLoadingArticles = ref(false)
const isRefreshing = ref(false)
const validationErrors = ref<string[]>([])
const validationWarnings = ref<string[]>([])

// Filters with persistence
const filters = ref<ArticleFilters>({
  authorId: null,
  titleSearch: '',
  dateRange: null,
  crawlerSource: null
})

// Validator instance
const validator = new ArticleSelectionValidator()

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

const estimatedTime = computed(() => {
  return Math.ceil(selectedArticles.value.length / 10) // Rough estimate: 10 articles per minute
})

// Computed property for checking if articles are selected (used in template)
// const hasSelection = computed(() => selectedArticles.value.length > 0)

// Event handlers
const onFilterChanged = async (newFilters: ArticleFilters) => {
  filters.value = { ...newFilters }
  // Reload articles when filters change
  await loadArticles()
}

const onSelectionChanged = (newSelection: Article[]) => {
  selectedArticles.value = [...newSelection]
  validateStep()
  emitStepData()
}

const clearSelection = () => {
  selectedArticles.value = []
  validateStep()
  emitStepData()
  
  toast.add({
    severity: 'info',
    summary: 'Selection Cleared',
    detail: 'All article selections have been cleared',
    life: 2000
  })
}

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

// Data loading methods
const loadArticles = async () => {
  isLoadingArticles.value = true
  
  try {
    // Build filter parameters
    const filterParams: Record<string, any> = {
      authorId: filters.value.authorId,
      titleSearch: filters.value.titleSearch || undefined,
      fromDate: filters.value.dateRange?.[0]?.toISOString(),
      toDate: filters.value.dateRange?.[1]?.toISOString(),
      page: 0,
      size: 1000 // Load more articles for wizard selection
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
  } finally {
    isLoadingArticles.value = false
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

// Step validation
const validateStep = () => {
  const stepData: WizardStepData = {
    selectedArticles: selectedArticles.value,
    filters: filters.value
  }
  
  const validation = validator.validate(stepData)
  
  validationErrors.value = validation.errors
  validationWarnings.value = validation.warnings
  
  // Emit validation status
  emit('step-valid', validation.isValid)
  
  // Mark step as complete if valid
  if (validation.isValid) {
    emit('step-complete', 'article-selection')
  }
  
  return validation
}

const emitStepData = () => {
  const stepData = {
    selectedArticles: selectedArticles.value,
    filters: filters.value,
    totalAvailableArticles: filteredArticles.value.length
  }
  
  emit('step-data', stepData)
}

// Watch for selection changes
watch(selectedArticles, () => {
  validateStep()
  emitStepData()
}, { deep: true })

// Watch for filter changes
watch(filters, () => {
  emitStepData()
}, { deep: true })

// Initialize from wizard state
const initializeFromWizardState = () => {
  if (props.wizardState?.selectedArticles) {
    selectedArticles.value = [...props.wizardState.selectedArticles]
  }
  
  if (props.wizardState?.filters) {
    filters.value = { ...props.wizardState.filters }
  }
}

// Initialize step
onMounted(async () => {
  try {
    // Initialize from wizard state first
    initializeFromWizardState()
    
    // Load authors first (needed for filters)
    await loadAuthors()
    
    // Load articles
    await loadArticles()
    
    // Validate initial state
    validateStep()
    
  } catch (error) {
    console.error('Error initializing article selection step:', error)
    toast.add({
      severity: 'error',
      summary: 'Initialization Error',
      detail: 'Failed to initialize article selection',
      life: 5000
    })
  }
})

// Watch for wizard state changes
watch(() => props.wizardState, (newState) => {
  if (newState && props.isActive) {
    initializeFromWizardState()
  }
}, { deep: true })
</script>

<style lang="scss" scoped>
.article-selection-step {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  height: 100%;
}

.step-description {
  .selection-info {
    padding: 0.75rem;
    background: var(--surface-100);
    border-radius: var(--border-radius);
    border-left: 4px solid var(--primary-color);
  }
}

.selection-summary {
  font-size: 0.875rem;
}

.validation-messages {
  .p-message {
    margin-bottom: 0.5rem;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
}

.article-table-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  
  :deep(.p-panel-content) {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 1rem;
    overflow: hidden;
  }
}

.wizard-article-table {
  flex: 1;
  min-height: 0;
}

// Panel customizations
:deep(.p-panel .p-panel-header) {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-bottom: none;
  padding: 1rem 1.25rem;
}

:deep(.p-panel .p-panel-content) {
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-top: none;
}

// Message customizations
:deep(.p-message) {
  .p-message-wrapper {
    padding: 0.75rem 1rem;
  }
  
  .p-message-icon {
    font-size: 1rem;
  }
  
  .p-message-text {
    font-size: 0.875rem;
    line-height: 1.4;
  }
}

// Responsive design
@media screen and (max-width: 991px) {
  .article-selection-step {
    gap: 0.75rem;
  }
  
  :deep(.p-panel .p-panel-header) {
    padding: 0.75rem 1rem;
    
    .flex {
      flex-direction: column;
      gap: 0.5rem;
      align-items: stretch;
    }
  }
  
  :deep(.p-panel .p-panel-content) {
    padding: 1rem;
  }
}

@media screen and (max-width: 575px) {
  .article-selection-step {
    gap: 0.5rem;
  }
  
  :deep(.p-panel .p-panel-header) {
    padding: 0.75rem;
  }
  
  :deep(.p-panel .p-panel-content) {
    padding: 0.75rem;
  }
  
  .step-description {
    font-size: 0.875rem;
  }
  
  .selection-summary {
    font-size: 0.75rem;
  }
}

// Dark theme adjustments
:root.dark {
  .selection-info {
    background: var(--surface-700);
    border-left-color: var(--primary-400);
  }
}
</style>