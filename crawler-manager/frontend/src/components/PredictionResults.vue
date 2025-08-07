<template>
  <Panel class="prediction-results-panel">
    <template #header>
      <div class="flex align-items-center justify-content-between w-full">
        <div class="flex align-items-center">
          <i class="pi pi-chart-bar mr-2 text-primary"></i>
          <span class="font-semibold">Analysis Results</span>
        </div>
        <div class="flex align-items-center gap-2">
          <Tag :value="`${predictions.length} predictions`" severity="info" />
          <Chip :label="`Avg: ${averageRating.toFixed(1)} ⭐`" />
          <Button 
            icon="pi pi-download"
            label="Export"
            @click="showExportMenu"
            severity="secondary"
            size="small"
            outlined
          />
        </div>
      </div>
    </template>
    
    <div class="p-panel-content">
      <!-- Results Summary Cards -->
      <div class="grid mb-4">
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center summary-card">
            <div class="text-2xl font-bold text-900 mb-1">{{ predictions.length }}</div>
            <div class="text-600 text-sm">Total Predictions</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center summary-card">
            <div class="mb-1">
              <Rating :modelValue="averageRating" readonly />
            </div>
            <div class="text-600 text-sm">Average Rating</div>
          </div>
        </div>
        <div class="col-12 md:col-3">
          <div class="surface-card p-3 border-round text-center summary-card">
            <div class="text-2xl font-bold text-green-500 mb-1">{{ highConfidenceCount }}</div>
            <div class="text-600 text-sm">High Confidence (>80%)</div>
          </div>
        </div>
        <div class="col-12 md:col-4">
          <div class="surface-card p-3 border-round text-center summary-card">
            <div class="text-2xl font-bold text-orange-500 mb-1">{{ uniqueArticlesCount }}</div>
            <div class="text-600 text-sm">Articles Analyzed</div>
          </div>
        </div>
      </div>
      
      <!-- Filters and Controls -->
      <div class="flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
        <div class="flex flex-wrap align-items-center gap-3">
          <!-- Rating Filter -->
          <div class="flex align-items-center gap-2">
            <label class="text-sm font-medium">Min Rating:</label>
            <Dropdown 
              v-model="filters.minRating"
              :options="ratingOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="Any"
              class="w-8rem"
              showClear
            />
          </div>
          
          <!-- Confidence Filter -->
          <div class="flex align-items-center gap-2">
            <label class="text-sm font-medium">Min Confidence:</label>
            <Slider 
              v-model="filters.minConfidence"
              :min="0"
              :max="100"
              :step="10"
              class="w-8rem"
            />
            <span class="text-sm text-600 w-3rem">{{ filters.minConfidence }}%</span>
          </div>
          
          <!-- Search -->
          <div class="flex align-items-center gap-2">
            <IconField iconPosition="left">
              <InputIcon class="pi pi-search" />
              <InputText 
                v-model="filters.searchText"
                placeholder="Search predictions..."
                class="w-12rem"
              />
            </IconField>
          </div>
        </div>
        
        <div class="flex align-items-center gap-2">
          <Button 
            icon="pi pi-filter-slash"
            label="Clear Filters"
            @click="clearFilters"
            severity="secondary"
            size="small"
            outlined
          />
          
          <Button 
            icon="pi pi-refresh"
            label="Refresh"
            @click="$emit('refresh-results')"
            severity="secondary"
            size="small"
          />
        </div>
      </div>
      
      <!-- Results Table -->
      <DataTable 
        :value="filteredPredictions"
        :paginator="true"
        :rows="rowsPerPage"
        :rowsPerPageOptions="[10, 25, 50]"
        :sortField="sortField"
        :sortOrder="sortOrder"
        @sort="onSort"
        class="results-table"
        responsiveLayout="scroll"
        stripedRows
        :loading="loading"
        paginatorTemplate="RowsPerPageDropdown FirstPageLink PrevPageLink CurrentPageReport NextPageLink LastPageLink"
        currentPageReportTemplate="{first} to {last} of {totalRecords}"
      >
        <template #empty>
          <div class="text-center p-4">
            <i class="pi pi-search text-4xl text-400 mb-3"></i>
            <div class="text-900 font-medium text-lg mb-2">No Predictions Found</div>
            <div class="text-600">Try adjusting your filters or search criteria.</div>
          </div>
        </template>
        
        <template #loading>
          <div class="text-center p-4">
            <ProgressSpinner />
            <div class="mt-2">Loading results...</div>
          </div>
        </template>
        
        <!-- Prediction Text Column -->
        <Column field="predictionText" header="Prediction" sortable class="prediction-column">
          <template #body="{ data }">
            <div class="prediction-content">
              <p class="font-medium mb-2 line-height-3">{{ data.predictionText }}</p>
              <div class="flex align-items-center gap-2 mb-2">
                <Tag 
                  v-if="data.predictionType" 
                  :label="data.predictionType" 
                  severity="secondary" 
                  class="text-xs"
                />
                <Chip 
                  :label="`ID: ${data.id}`" 
                  class="text-xs"
                />
              </div>
              <Accordion v-if="data.context" class="context-accordion">
                <AccordionTab header="Context">
                  <div class="text-600 text-sm line-height-3">{{ data.context }}</div>
                </AccordionTab>
              </Accordion>
            </div>
          </template>
        </Column>
        
        <!-- Rating Column -->
        <Column field="rating" header="Rating" sortable class="rating-column">
          <template #body="{ data }">
            <div class="text-center">
              <Rating :modelValue="data.rating" readonly class="mb-1" />
              <div class="text-600 text-xs">{{ data.rating }}/5 stars</div>
            </div>
          </template>
        </Column>
        
        <!-- Confidence Column -->
        <Column field="confidenceScore" header="Confidence" sortable class="confidence-column">
          <template #body="{ data }">
            <div class="confidence-display">
              <ProgressBar 
                :value="data.confidenceScore * 100"
                :showValue="false"
                class="confidence-bar mb-1"
                :class="getConfidenceClass(data.confidenceScore)"
              />
              <div class="text-center text-xs text-600">
                {{ Math.round(data.confidenceScore * 100) }}%
              </div>
            </div>
          </template>
        </Column>
        
        <!-- Source Article Column -->
        <Column field="article.title" header="Source Article" sortable class="article-column">
          <template #body="{ data }">
            <div class="source-info">
              <div class="font-medium text-900 mb-1 line-height-3">
                <a 
                  :href="data.article.url" 
                  target="_blank" 
                  class="text-primary no-underline hover:underline"
                  :title="data.article.title"
                >
                  {{ truncateText(data.article.title, 60) }}
                  <i class="pi pi-external-link text-xs ml-1"></i>
                </a>
              </div>
              <div class="flex align-items-center gap-2 mb-1">
                <Avatar 
                  v-if="data.article.author?.avatarUrl"
                  :image="data.article.author.avatarUrl"
                  size="small"
                  class="author-avatar"
                />
                <Avatar 
                  v-else
                  :label="getAuthorInitials(data.article.author?.name)"
                  size="small"
                  class="author-avatar"
                />
                <span class="text-sm">{{ data.article.author?.name || 'Unknown' }}</span>
              </div>
              <div class="flex align-items-center gap-2">
                <Badge :value="data.article.crawlerSource" class="text-xs" />
                <small class="text-500">{{ formatDate(data.article.createdAt) }}</small>
              </div>
            </div>
          </template>
        </Column>
        
        <!-- Actions Column -->
        <Column header="Actions" class="actions-column">
          <template #body="{ data }">
            <div class="flex align-items-center gap-1">
              <Button 
                icon="pi pi-eye"
                @click="viewPredictionDetail(data)"
                severity="secondary"
                size="small"
                text
                v-tooltip.top="'View Details'"
              />
              <Button 
                icon="pi pi-copy"
                @click="copyPrediction(data)"
                severity="secondary"
                size="small"
                text
                v-tooltip.top="'Copy Prediction'"
              />
              <Button 
                icon="pi pi-share-alt"
                @click="sharePrediction(data)"
                severity="secondary"
                size="small"
                text
                v-tooltip.top="'Share'"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>
  </Panel>
  
  <!-- Export Menu -->
  <Menu ref="exportMenu" :model="exportMenuItems" :popup="true" />
  
  <!-- Prediction Detail Dialog -->
  <Dialog 
    v-model:visible="showDetailDialog"
    :header="`Prediction Details - ID: ${selectedPrediction?.id}`"
    :modal="true"
    :closable="true"
    :style="{ width: '50vw' }"
    :breakpoints="{ '960px': '75vw', '641px': '90vw' }"
  >
    <div v-if="selectedPrediction" class="prediction-detail">
      <div class="grid">
        <div class="col-12">
          <h4>Prediction Text</h4>
          <p class="line-height-3">{{ selectedPrediction.predictionText }}</p>
        </div>
        
        <div class="col-6">
          <h4>Rating</h4>
          <Rating :modelValue="selectedPrediction.rating" readonly />
        </div>
        
        <div class="col-6">
          <h4>Confidence Score</h4>
          <ProgressBar 
            :value="selectedPrediction.confidenceScore * 100"
            :showValue="true"
          />
        </div>
        
        <div class="col-12" v-if="selectedPrediction.context">
          <h4>Context</h4>
          <p class="text-600 line-height-3">{{ selectedPrediction.context }}</p>
        </div>
        
        <div class="col-12">
          <h4>Source Article</h4>
          <div class="surface-card p-3 border-round">
            <div class="font-medium mb-2">{{ selectedPrediction.article.title }}</div>
            <div class="flex align-items-center gap-2 mb-2">
              <span>By: {{ selectedPrediction.article.author?.name || 'Unknown' }}</span>
              <Badge :value="selectedPrediction.article.crawlerSource" />
            </div>
            <a 
              :href="selectedPrediction.article.url" 
              target="_blank"
              class="text-primary"
            >
              View Original Article <i class="pi pi-external-link"></i>
            </a>
          </div>
        </div>
      </div>
    </div>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useToast } from 'primevue/usetoast'
import Panel from 'primevue/panel'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Rating from 'primevue/rating'
import ProgressBar from 'primevue/progressbar'
import ProgressSpinner from 'primevue/progressspinner'
import Tag from 'primevue/tag'
import Chip from 'primevue/chip'
import Badge from 'primevue/badge'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import Slider from 'primevue/slider'
import InputText from 'primevue/inputtext'
import IconField from 'primevue/iconfield'
import InputIcon from 'primevue/inputicon'
import Avatar from 'primevue/avatar'
import Menu from 'primevue/menu'
import Dialog from 'primevue/dialog'
import Accordion from 'primevue/accordion'
import AccordionTab from 'primevue/accordiontab'

// Types
interface Author {
  id: number
  name: string
  avatarUrl?: string
}

interface Article {
  id: number
  title: string
  url: string
  crawlerSource: string
  createdAt: string
  author?: Author
}

interface PredictionResult {
  id: string
  predictionText: string
  predictionType?: string
  rating: number
  confidenceScore: number
  context?: string
  article: Article
  extractedAt: string
}

interface Props {
  predictions: PredictionResult[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// Emits
const emit = defineEmits<{
  'export-results': [format: string]
  'refresh-results': []
  'prediction-selected': [prediction: PredictionResult]
}>()

// Reactive state
const toast = useToast()
const exportMenu = ref<any>(null)
const showDetailDialog = ref(false)
const selectedPrediction = ref<PredictionResult | null>(null)

// Table state
const rowsPerPage = ref(10)
const sortField = ref('rating')
const sortOrder = ref(-1)

// Filters
const filters = ref({
  minRating: null as number | null,
  minConfidence: 0,
  searchText: ''
})

// Export menu items
const exportMenuItems = ref([
  {
    label: 'Export as CSV',
    icon: 'pi pi-file-excel',
    command: () => emit('export-results', 'csv')
  },
  {
    label: 'Export as JSON',
    icon: 'pi pi-file',
    command: () => emit('export-results', 'json')
  },
  {
    label: 'Export as PDF Report',
    icon: 'pi pi-file-pdf',
    command: () => emit('export-results', 'pdf')
  }
])

// Rating filter options
const ratingOptions = [
  { label: '5 Stars', value: 5 },
  { label: '4+ Stars', value: 4 },
  { label: '3+ Stars', value: 3 },
  { label: '2+ Stars', value: 2 },
  { label: '1+ Stars', value: 1 }
]

// Computed properties
const averageRating = computed(() => {
  if (props.predictions.length === 0) return 0
  const sum = props.predictions.reduce((acc, pred) => acc + pred.rating, 0)
  return sum / props.predictions.length
})

const highConfidenceCount = computed(() => {
  return props.predictions.filter(pred => pred.confidenceScore > 0.8).length
})

const uniqueArticlesCount = computed(() => {
  const articleIds = new Set(props.predictions.map(pred => pred.article.id))
  return articleIds.size
})

const filteredPredictions = computed(() => {
  let filtered = [...props.predictions]
  
  // Rating filter
  if (filters.value.minRating !== null) {
    filtered = filtered.filter(pred => pred.rating >= filters.value.minRating!)
  }
  
  // Confidence filter
  if (filters.value.minConfidence > 0) {
    filtered = filtered.filter(pred => pred.confidenceScore >= filters.value.minConfidence / 100)
  }
  
  // Search filter
  if (filters.value.searchText) {
    const search = filters.value.searchText.toLowerCase()
    filtered = filtered.filter(pred => 
      pred.predictionText.toLowerCase().includes(search) ||
      pred.article.title.toLowerCase().includes(search) ||
      pred.article.author?.name.toLowerCase().includes(search)
    )
  }
  
  return filtered
})

// Methods
const showExportMenu = (event: Event) => {
  if (exportMenu.value) {
    exportMenu.value.toggle(event)
  }
}

const clearFilters = () => {
  filters.value = {
    minRating: null,
    minConfidence: 0,
    searchText: ''
  }
}

const onSort = (event: any) => {
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder
}

const getConfidenceClass = (confidence: number) => {
  if (confidence >= 0.8) return 'confidence-high'
  if (confidence >= 0.6) return 'confidence-medium'
  return 'confidence-low'
}

const truncateText = (text: string, maxLength: number) => {
  if (text.length <= maxLength) return text
  return text.substring(0, maxLength) + '...'
}

const getAuthorInitials = (name?: string) => {
  if (!name) return '?'
  return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2)
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}

const viewPredictionDetail = (prediction: PredictionResult) => {
  selectedPrediction.value = prediction
  showDetailDialog.value = true
  emit('prediction-selected', prediction)
}

const copyPrediction = async (prediction: PredictionResult) => {
  try {
    await navigator.clipboard.writeText(prediction.predictionText)
    toast.add({
      severity: 'success',
      summary: 'Copied',
      detail: 'Prediction text copied to clipboard',
      life: 2000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Copy Failed',
      detail: 'Failed to copy prediction text',
      life: 3000
    })
  }
}

const sharePrediction = (prediction: PredictionResult) => {
  const shareData = {
    title: 'Prediction Analysis Result',
    text: `"${prediction.predictionText}" - Rating: ${prediction.rating}/5 stars`,
    url: prediction.article.url
  }
  
  if (navigator.share) {
    navigator.share(shareData).catch(console.error)
  } else {
    // Fallback: copy to clipboard
    const shareText = `${shareData.text}\nSource: ${shareData.url}`
    navigator.clipboard.writeText(shareText).then(() => {
      toast.add({
        severity: 'info',
        summary: 'Share Link Copied',
        detail: 'Share information copied to clipboard',
        life: 3000
      })
    })
  }
}

// Watch for predictions changes to reset filters if needed
watch(() => props.predictions.length, (newLength, oldLength) => {
  if (newLength !== oldLength && newLength === 0) {
    clearFilters()
  }
})
</script>

<style lang="scss" scoped>
.prediction-results-panel {
  :deep(.p-panel-content) {
    padding: 1.5rem;
  }
}

.summary-card {
  transition: transform 0.2s, box-shadow 0.2s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
}

.results-table {
  :deep(.p-datatable-thead > tr > th) {
    background-color: var(--surface-section);
    border-bottom: 1px solid var(--surface-border);
    font-weight: 600;
  }
  
  :deep(.p-datatable-tbody > tr) {
    transition: background-color var(--transition-duration);
    
    &:hover {
      background-color: var(--surface-hover);
    }
  }
  
  // Column-specific styling
  :deep(.prediction-column) {
    min-width: 300px;
    max-width: 400px;
  }
  
  :deep(.rating-column) {
    width: 120px;
    text-align: center;
  }
  
  :deep(.confidence-column) {
    width: 140px;
  }
  
  :deep(.article-column) {
    min-width: 250px;
    max-width: 350px;
  }
  
  :deep(.actions-column) {
    width: 120px;
    text-align: center;
  }
}

.prediction-content {
  .line-height-3 {
    line-height: 1.5;
  }
}

.context-accordion {
  :deep(.p-accordion-header) {
    padding: 0.5rem 0.75rem;
    
    .p-accordion-header-link {
      padding: 0;
      font-size: 0.875rem;
    }
  }
  
  :deep(.p-accordion-content) {
    padding: 0.75rem;
  }
}

.confidence-display {
  width: 100px;
}

.confidence-bar {
  height: 0.5rem;
  
  &.confidence-high :deep(.p-progressbar-value) {
    background: var(--green-500);
  }
  
  &.confidence-medium :deep(.p-progressbar-value) {
    background: var(--orange-500);
  }
  
  &.confidence-low :deep(.p-progressbar-value) {
    background: var(--red-500);
  }
}

.source-info {
  .author-avatar {
    width: 1.5rem;
    height: 1.5rem;
    font-size: 0.75rem;
  }
  
  .line-height-3 {
    line-height: 1.4;
  }
}

.prediction-detail {
  h4 {
    color: var(--text-color);
    margin-bottom: 0.5rem;
    font-size: 1rem;
    font-weight: 600;
  }
  
  .line-height-3 {
    line-height: 1.6;
  }
}

// Responsive design
@media screen and (max-width: 768px) {
  .prediction-results-panel {
    :deep(.p-panel-content) {
      padding: 1rem;
    }
  }
  
  .results-table {
    :deep(.prediction-column) {
      min-width: 250px;
    }
    
    :deep(.article-column) {
      min-width: 200px;
    }
  }
  
  .summary-card {
    margin-bottom: 1rem;
  }
}

@media screen and (max-width: 576px) {
  .results-table {
    :deep(.p-datatable-wrapper) {
      overflow-x: auto;
    }
    
    :deep(.prediction-column) {
      min-width: 200px;
    }
    
    :deep(.article-column) {
      min-width: 180px;
    }
    
    :deep(.actions-column) {
      width: 80px;
      
      .p-button {
        padding: 0.25rem;
        
        .p-button-label {
          display: none;
        }
      }
    }
  }
}

// Dark theme adjustments
:root.dark {
  .summary-card {
    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }
  }
}
</style>