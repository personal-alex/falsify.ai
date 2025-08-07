<template>
  <div class="article-selection-table-container">
    <!-- Integrated Table Controls Header -->
    <div class="table-controls-header">
      <div class="flex align-items-center justify-content-between w-full">
        <div class="flex align-items-center gap-2">
          <i class="pi pi-table text-primary"></i>
          <span class="font-semibold">Articles</span>
          <Badge 
            :value="localSelection.length" 
            :severity="localSelection.length > 0 ? 'success' : 'info'"
          />
          <Chip :label="`${localSelection.length} of ${filteredArticles.length} selected`" />
        </div>
        <div class="flex align-items-center gap-2 table-action-controls">
          <Button 
            icon="pi pi-refresh"
            label="Refresh"
            size="small"
            severity="secondary"
            outlined
            @click="handleRefresh"
            :loading="isRefreshing"
            v-tooltip.top="'Refresh article list'"
          />
          <Button 
            icon="pi pi-check-square"
            :label="isAllSelected ? 'Deselect All' : 'Select All'"
            size="small"
            :severity="isAllSelected ? 'danger' : 'primary'"
            outlined
            @click="toggleSelectAll"
            :disabled="filteredArticles.length === 0"
            v-tooltip.top="isAllSelected ? 'Deselect all articles' : 'Select all articles'"
          />
          <Button 
            v-if="localSelection.length > 0"
            icon="pi pi-times"
            label="Clear Selection"
            size="small"
            severity="secondary"
            outlined
            @click="handleClearSelection"
            v-tooltip.top="'Clear current selection'"
          />
        </div>
      </div>
    </div>

    <!-- Table Content Area -->
    <div class="table-content-area">
      <!-- Loading State with Skeleton -->
      <div v-if="loading" class="loading-state">
        <div v-for="n in 5" :key="n" class="mb-3">
          <div class="flex align-items-center">
            <Skeleton shape="circle" size="2rem" class="mr-3"></Skeleton>
            <div class="flex-1">
              <Skeleton width="80%" height="1.2rem" class="mb-2"></Skeleton>
              <Skeleton width="60%" height="1rem"></Skeleton>
            </div>
            <Skeleton width="4rem" height="1rem"></Skeleton>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!loading && articles.length === 0" class="empty-state">
        <div class="text-center p-6">
          <div class="mb-4">
            <i class="pi pi-inbox text-6xl text-400"></i>
          </div>
          <div class="text-900 font-medium text-xl mb-2">No Articles Found</div>
          <div class="text-600 mb-4">No articles match your current filter criteria. Try adjusting your filters or refresh the data.</div>
          <Button 
            icon="pi pi-refresh"
            label="Refresh Articles"
            severity="secondary"
            @click="handleRefresh"
          />
        </div>
      </div>

      <!-- Enhanced DataTable with Fixed Viewport -->
      <DataTable 
        v-else
        v-model:selection="localSelection"
        :value="filteredArticles"
        dataKey="id"
        :paginator="!shouldUseVirtualScrolling"
        :rows="shouldUseVirtualScrolling ? 0 : 25"
        :rowsPerPageOptions="[10, 25, 50, 100]"
        selectionMode="multiple"
        class="article-selection-table"
        responsiveLayout="scroll"
        stripedRows
        showGridlines
        :scrollable="true"
        :scrollHeight="tableViewportHeight"
        :virtualScrollerOptions="virtualScrollConfig"
        resizableColumns
        columnResizeMode="expand"
        :loading="loading"
        loadingIcon="pi pi-spinner"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
        @column-resize-end="onColumnResize"
        role="table"
        :aria-label="`Article selection table with ${filteredArticles.length} articles`"
      >
        <Column 
          selectionMode="multiple" 
          :style="columnStyles.selection"
          :resizable="false"
          frozen
          alignFrozen="left"
          headerClass="selection-header"
        >
          <template #header>
            <div class="text-center" role="columnheader" aria-label="Select all articles">
              <Checkbox 
                :modelValue="isAllSelected"
                :indeterminate="isPartiallySelected"
                @update:modelValue="toggleSelectAll"
                :disabled="filteredArticles.length === 0"
                v-tooltip.top="'Select/deselect all articles'"
                :aria-label="isAllSelected ? 'Deselect all articles' : 'Select all articles'"
              />
            </div>
          </template>
        </Column>
        
        <Column 
          field="title" 
          header="Title" 
          sortable 
          :style="columnStyles.title"
          class="title-column"
          headerClass="title-header"
        >
          <template #body="{ data }">
            <div class="article-title" role="cell">
              <div 
                class="font-medium text-900 mb-1 line-height-3 title-text" 
                :title="data.title"
                :aria-label="`Article title: ${data.title}`"
              >
                {{ data.title }}
              </div>
              <div class="flex align-items-center gap-2">
                <Tag label="URL" severity="secondary" />
                <small 
                  class="text-600 url-text" 
                  :title="data.url"
                  :aria-label="`Article URL: ${data.url}`"
                >
                  {{ data.url }}
                </small>
              </div>
            </div>
          </template>
        </Column>
        
        <Column 
          field="author.name" 
          header="Author" 
          sortable 
          :style="columnStyles.author"
          class="author-column"
          headerClass="author-header"
        >
          <template #body="{ data }">
            <div class="flex align-items-center gap-2" role="cell">
              <Avatar 
                :image="data.author?.avatarUrl"
                :label="data.author?.name?.charAt(0) || 'U'"
                size="small"
                class="author-avatar"
                :aria-label="`Author avatar for ${data.author?.name || 'Unknown'}`"
              />
              <Chip 
                :label="data.author?.name || 'Unknown'" 
                class="author-chip"
                :aria-label="`Author: ${data.author?.name || 'Unknown'}`"
              />
            </div>
          </template>
        </Column>
        
        <Column 
          field="createdAt" 
          header="Scraped" 
          sortable 
          :style="columnStyles.date"
          class="date-column"
          headerClass="date-header"
        >
          <template #body="{ data }">
            <div class="text-center" role="cell">
              <div 
                class="font-medium text-900"
                :aria-label="`Scraped on ${formatDate(data.createdAt)} at ${formatTime(data.createdAt)}`"
              >
                {{ formatDate(data.createdAt) }}
              </div>
              <small class="text-600">{{ formatTime(data.createdAt) }}</small>
            </div>
          </template>
        </Column>
        
        <Column 
          field="crawlerSource" 
          header="Source" 
          sortable 
          :style="columnStyles.source"
          class="source-column"
          headerClass="source-header"
        >
          <template #body="{ data }">
            <div class="text-center" role="cell">
              <Badge 
                :value="data.crawlerSource" 
                :severity="data.crawlerSource === 'drucker' ? 'info' : 'warning'"
                :aria-label="`Crawler source: ${data.crawlerSource}`"
              />
            </div>
          </template>
        </Column>
        
        <Column 
          header="Predictions" 
          :style="columnStyles.predictions"
          class="predictions-column"
          headerClass="predictions-header"
        >
          <template #body="{ data }">
            <div class="text-center" role="cell">
              <Badge 
                v-if="data.predictions && data.predictions.length > 0"
                :value="`${data.predictions.length} found`"
                severity="success"
                :aria-label="`${data.predictions.length} predictions found`"
              />
              <Tag 
                v-else 
                value="Not analyzed" 
                severity="secondary"
                aria-label="Article not yet analyzed for predictions"
              />
            </div>
          </template>
        </Column>

        <Column 
          header="Actions" 
          :style="columnStyles.actions"
          :resizable="false"
          frozen
          alignFrozen="right"
          class="actions-column"
          headerClass="actions-header"
        >
          <template #body="{ data }">
            <div class="flex justify-content-center" role="cell">
              <Button 
                icon="pi pi-eye"
                severity="secondary"
                text
                size="small"
                @click="showPreview(data)"
                v-tooltip.top="'Preview article'"
                :aria-label="`Preview article: ${data.title}`"
              />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Article Preview Dialog -->
    <Dialog 
      v-model:visible="previewVisible"
      :header="previewArticle?.title"
      modal
      :style="{ width: '80vw', maxWidth: '800px' }"
      class="article-preview-dialog"
    >
      <div v-if="previewArticle" class="preview-content">
        <div class="preview-meta">
          <div class="meta-item">
            <strong>Author:</strong> {{ previewArticle.author?.name || 'Unknown' }}
          </div>
          <div class="meta-item">
            <strong>Source:</strong> {{ previewArticle.crawlerSource }}
          </div>
          <div class="meta-item">
            <strong>Scraped:</strong> {{ formatDate(previewArticle.createdAt) }}
          </div>
          <div class="meta-item">
            <strong>URL:</strong> 
            <a :href="previewArticle.url" target="_blank" class="article-link">
              {{ previewArticle.url }}
              <i class="pi pi-external-link"></i>
            </a>
          </div>
        </div>
        
        <div class="preview-text">
          <h4>Article Content</h4>
          <p>{{ previewArticle.text || 'No content available' }}</p>
        </div>
      </div>
      
      <template #footer>
        <Button 
          label="Close" 
          @click="previewVisible = false"
          severity="secondary"
        />
        <Button 
          :label="isArticleSelected(previewArticle) ? 'Remove from Selection' : 'Add to Selection'"
          @click="toggleArticleSelection(previewArticle)"
          :severity="isArticleSelected(previewArticle) ? 'danger' : 'primary'"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Badge from 'primevue/badge'
import Chip from 'primevue/chip'
import Tag from 'primevue/tag'
import Avatar from 'primevue/avatar'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Skeleton from 'primevue/skeleton'
import Checkbox from 'primevue/checkbox'

export interface Article {
  id: number
  title: string
  url: string
  text: string
  crawlerSource: string
  createdAt: string
  author?: {
    id: number
    name: string
    avatarUrl?: string
  }
  predictions?: any[]
}

interface Props {
  articles: Article[]
  selectedArticles: Article[]
  loading?: boolean
}

interface Emits {
  (e: 'selection-changed', articles: Article[]): void
  (e: 'refresh-articles'): void
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<Emits>()

// Reactive state for refresh handling
const isRefreshing = ref(false)

// Local selection state for DataTable v-model
const localSelection = ref<Article[]>([...props.selectedArticles])

// Preview dialog state
const previewVisible = ref(false)
const previewArticle = ref<Article | null>(null)

// Selection persistence across filtering and pagination
const selectedArticleIds = ref<Set<number>>(new Set(props.selectedArticles.map(a => a.id)))

// Viewport height management
const viewportHeight = ref(window.innerHeight)

// Performance optimization for large datasets
const shouldUseVirtualScrolling = computed(() => props.articles.length > 100)

// Dynamic table viewport height calculation
const tableViewportHeight = computed(() => {
  // Calculate available height: viewport - header - controls - padding
  const availableHeight = viewportHeight.value - 300 // Reserve space for other UI elements
  const minHeight = 400
  const maxHeight = 800
  
  return `${Math.max(minHeight, Math.min(maxHeight, availableHeight))}px`
})

// Virtual scroll configuration
const virtualScrollConfig = computed(() => 
  shouldUseVirtualScrolling.value 
    ? { 
        itemSize: 73, 
        showLoader: true, 
        delay: 150,
        lazy: true,
        numToleratedItems: 10
      }
    : undefined
)

// Responsive column styles
const columnStyles = computed(() => {
  const isMobile = viewportHeight.value < 768
  const isTablet = viewportHeight.value < 1024
  
  return {
    selection: {
      width: isMobile ? '50px' : '60px',
      minWidth: isMobile ? '50px' : '60px'
    },
    title: {
      minWidth: isMobile ? '200px' : isTablet ? '250px' : '300px',
      width: isMobile ? '40%' : isTablet ? '35%' : '30%'
    },
    author: {
      minWidth: isMobile ? '120px' : isTablet ? '150px' : '180px',
      width: isMobile ? '150px' : isTablet ? '180px' : '200px'
    },
    date: {
      minWidth: isMobile ? '100px' : isTablet ? '120px' : '140px',
      width: isMobile ? '120px' : isTablet ? '140px' : '160px'
    },
    source: {
      minWidth: isMobile ? '80px' : isTablet ? '100px' : '120px',
      width: isMobile ? '100px' : isTablet ? '120px' : '140px'
    },
    predictions: {
      minWidth: isMobile ? '100px' : isTablet ? '120px' : '140px',
      width: isMobile ? '120px' : isTablet ? '140px' : '160px'
    },
    actions: {
      width: isMobile ? '60px' : '80px',
      minWidth: isMobile ? '60px' : '80px'
    }
  }
})

// Computed properties for selection state
const filteredArticles = computed(() => props.articles)

const isAllSelected = computed(() => {
  return filteredArticles.value.length > 0 && 
         filteredArticles.value.every(article => selectedArticleIds.value.has(article.id))
})

const isPartiallySelected = computed(() => {
  const selectedCount = filteredArticles.value.filter(article => 
    selectedArticleIds.value.has(article.id)
  ).length
  return selectedCount > 0 && selectedCount < filteredArticles.value.length
})

// Watch for external selection changes and sync local state
watch(() => props.selectedArticles, (newSelection) => {
  localSelection.value = [...newSelection]
  selectedArticleIds.value = new Set(newSelection.map(a => a.id))
}, { deep: true })

// Watch for articles changes and update local selection to maintain persistence
watch(() => props.articles, (newArticles) => {
  // Update local selection based on persisted IDs
  localSelection.value = newArticles.filter(article => 
    selectedArticleIds.value.has(article.id)
  )
}, { deep: true })

// Watch local selection changes and emit selection change
watch(localSelection, () => {
  emitSelectionChangeDebounced()
}, { deep: true })

// Enhanced selection change handling
const handleSelectionChange = (event: any) => {
  // Update local selection from DataTable event
  localSelection.value = [...event.value]
  
  // Update persisted IDs
  selectedArticleIds.value = new Set(event.value.map((article: Article) => article.id))
  
  // Emit change immediately for user actions
  emit('selection-changed', [...localSelection.value])
}

// Handle row click for better selection UX
const handleRowClick = (event: any) => {
  const article = event.data as Article
  const isCurrentlySelected = selectedArticleIds.value.has(article.id)
  
  if (isCurrentlySelected) {
    // Remove from selection
    selectedArticleIds.value.delete(article.id)
    const index = localSelection.value.findIndex(selected => selected.id === article.id)
    if (index >= 0) {
      localSelection.value.splice(index, 1)
    }
  } else {
    // Add to selection
    selectedArticleIds.value.add(article.id)
    localSelection.value.push(article)
  }
  
  // Emit change
  emit('selection-changed', [...localSelection.value])
}

// Integrated refresh handler
const handleRefresh = async () => {
  isRefreshing.value = true
  try {
    emit('refresh-articles')
    // Wait a bit for the refresh to complete
    await new Promise(resolve => setTimeout(resolve, 500))
  } finally {
    isRefreshing.value = false
  }
}

// Integrated clear selection handler
const handleClearSelection = () => {
  localSelection.value = []
  selectedArticleIds.value.clear()
  emit('selection-changed', [])
}

// Format date helper
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString()
}

// Format time helper
const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Select/Deselect all articles
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    // Deselect all current articles (remove from persisted IDs)
    filteredArticles.value.forEach(article => {
      selectedArticleIds.value.delete(article.id)
    })
    // Update local selection to exclude deselected articles
    localSelection.value = localSelection.value.filter(article => 
      selectedArticleIds.value.has(article.id)
    )
  } else {
    // Select all current articles (add to persisted IDs)
    filteredArticles.value.forEach(article => {
      selectedArticleIds.value.add(article.id)
    })
    // Add new articles to selection, avoiding duplicates
    const existingIds = new Set(localSelection.value.map(a => a.id))
    const newArticles = filteredArticles.value.filter(article => 
      !existingIds.has(article.id)
    )
    localSelection.value = [...localSelection.value, ...newArticles]
  }
}

// Show article preview
const showPreview = (article: Article) => {
  previewArticle.value = article
  previewVisible.value = true
}

// Check if article is selected
const isArticleSelected = (article: Article | null) => {
  if (!article) return false
  return localSelection.value.some(selected => selected.id === article.id)
}

// Toggle article selection from preview
const toggleArticleSelection = (article: Article | null) => {
  if (!article) return
  
  if (selectedArticleIds.value.has(article.id)) {
    // Remove from selection
    selectedArticleIds.value.delete(article.id)
    const index = localSelection.value.findIndex(selected => selected.id === article.id)
    if (index >= 0) {
      localSelection.value.splice(index, 1)
    }
  } else {
    // Add to selection
    selectedArticleIds.value.add(article.id)
    localSelection.value.push(article)
  }
}

// Column resize handler
const onColumnResize = (event: any) => {
  // Store column widths in localStorage for persistence
  const columnWidths = JSON.parse(localStorage.getItem('articleTableColumnWidths') || '{}')
  columnWidths[event.element.dataset.field || event.element.textContent] = event.element.style.width
  localStorage.setItem('articleTableColumnWidths', JSON.stringify(columnWidths))
}

// Debounced search for better performance
const debouncedEmitSelection = debounce(() => {
  emit('selection-changed', [...localSelection.value])
}, 100)

// Replace immediate emit with debounced version for better performance
const emitSelectionChangeDebounced = () => {
  debouncedEmitSelection()
}

// Viewport resize handling
const handleResize = () => {
  viewportHeight.value = window.innerHeight
}

// Lifecycle hooks for viewport management
onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize() // Initial calculation
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// Simple debounce function
function debounce(func: Function, wait: number) {
  let timeout: NodeJS.Timeout
  return function executedFunction(...args: any[]) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}
</script>

<style lang="scss" scoped>
.article-selection-table-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  overflow: hidden;
}

.table-controls-header {
  background: var(--surface-section);
  border-bottom: 1px solid var(--surface-border);
  padding: 1rem 1.25rem;
  flex-shrink: 0;
  
  .table-action-controls {
    flex-wrap: wrap;
    gap: 0.5rem;
  }
}

.table-content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 1rem;
}

.loading-state {
  padding: 1rem;
}

.empty-state {
  padding: 2rem;
  background: var(--surface-card);
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
}

.article-selection-table {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  
  // Main table wrapper with fixed viewport
  :deep(.p-datatable-wrapper) {
    flex: 1;
    overflow: auto;
    border: 1px solid var(--surface-border);
    border-radius: var(--border-radius);
    height: 100%;
    
    // Custom scrollbar styling
    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
    }
    
    &::-webkit-scrollbar-track {
      background: var(--surface-100);
      border-radius: 4px;
    }
    
    &::-webkit-scrollbar-thumb {
      background: var(--surface-400);
      border-radius: 4px;
      
      &:hover {
        background: var(--surface-500);
      }
    }
    
    &::-webkit-scrollbar-corner {
      background: var(--surface-100);
    }
  }
  
  // Table structure improvements for responsive design
  :deep(.p-datatable-table) {
    min-width: 800px; // Reduced minimum width for better mobile support
    table-layout: fixed; // Fixed layout for better column control
    width: 100%;
  }
  
  // Header styling with sticky positioning
  :deep(.p-datatable-thead) {
    position: sticky;
    top: 0;
    z-index: 10;
    
    > tr > th {
      background: var(--surface-100);
      border-bottom: 2px solid var(--surface-border);
      font-weight: 600;
      color: var(--text-color);
      padding: 1rem 0.75rem;
      white-space: nowrap;
      position: relative;
      
      // Resizer styling
      .p-column-resizer {
        position: absolute;
        top: 0;
        right: 0;
        width: 4px;
        height: 100%;
        cursor: col-resize;
        background: transparent;
        border-right: 2px solid transparent;
        transition: border-color var(--transition-duration);
        
        &:hover {
          border-right-color: var(--primary-color);
        }
      }
      
      // Sort icon positioning
      .p-sortable-column-icon {
        margin-left: 0.5rem;
        color: var(--text-color-secondary);
      }
      
      &.p-highlight {
        background: var(--primary-50);
        color: var(--primary-600);
        
        .p-sortable-column-icon {
          color: var(--primary-600);
        }
      }
    }
  }
  
  // Body row styling
  :deep(.p-datatable-tbody) {
    > tr {
      transition: all var(--transition-duration);
      border-bottom: 1px solid var(--surface-border);
      
      &:hover {
        background-color: var(--surface-hover);
      }
      
      &.p-highlight {
        background-color: var(--primary-50);
        color: var(--primary-600);
        
        &:hover {
          background-color: var(--primary-100);
        }
      }
      
      > td {
        padding: 0.75rem;
        border-right: 1px solid var(--surface-border);
        vertical-align: top;
        
        &:last-child {
          border-right: none;
        }
        
        // Frozen column styling
        &.p-frozen-column {
          background: var(--surface-card);
          z-index: 5;
          box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);
          
          &.p-frozen-column-left {
            border-right: 2px solid var(--surface-border);
          }
          
          &.p-frozen-column-right {
            border-left: 2px solid var(--surface-border);
            box-shadow: -2px 0 4px rgba(0, 0, 0, 0.1);
          }
        }
      }
    }
  }
  
  // Paginator styling
  :deep(.p-paginator) {
    background: var(--surface-section);
    border-top: 1px solid var(--surface-border);
    padding: 0.75rem 1rem;
    border-radius: 0 0 var(--border-radius) var(--border-radius);
    
    .p-paginator-pages {
      .p-paginator-page {
        min-width: 2.5rem;
        height: 2.5rem;
        
        &.p-highlight {
          background: var(--primary-color);
          color: var(--primary-color-text);
        }
      }
    }
  }
  
  // Loading state improvements
  &.p-datatable-loading {
    position: relative;
    
    .p-datatable-loading-overlay {
      background: rgba(var(--surface-card-rgb), 0.8);
      backdrop-filter: blur(2px);
      
      .p-datatable-loading-icon {
        font-size: 2rem;
        color: var(--primary-color);
      }
    }
  }
  
  // Virtual scroller loading
  :deep(.p-virtualscroller-loader) {
    background: var(--surface-section);
    border: 1px solid var(--surface-border);
    border-radius: var(--border-radius);
    padding: 1rem;
    text-align: center;
    
    .p-virtualscroller-loading-icon {
      color: var(--primary-color);
      font-size: 1.5rem;
    }
  }
  
  // Performance optimization: GPU acceleration for smooth scrolling
  :deep(.p-datatable-scrollable-body) {
    transform: translateZ(0);
    will-change: scroll-position;
  }
}

// Column-specific styling
.article-title {
  .line-height-3 {
    line-height: 1.3;
  }
  
  .title-text {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    max-height: 2.6em;
    word-break: break-word;
  }
  
  .url-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
    display: block;
  }
}

.author-avatar {
  flex-shrink: 0;
}

.author-chip {
  max-width: 120px;
  
  :deep(.p-chip-text) {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.article-preview-dialog {
  .preview-content {
    .preview-meta {
      background: var(--surface-section);
      padding: 1.5rem;
      border-radius: var(--border-radius);
      margin-bottom: 1.5rem;
      border: 1px solid var(--surface-border);

      .meta-item {
        margin-bottom: 1rem;
        
        &:last-child {
          margin-bottom: 0;
        }

        strong {
          color: var(--text-color);
          margin-right: 0.5rem;
          font-weight: 600;
        }

        .article-link {
          color: var(--primary-color);
          text-decoration: none;
          transition: color var(--transition-duration);
          
          &:hover {
            color: var(--primary-600);
            text-decoration: underline;
          }

          .pi-external-link {
            margin-left: 0.25rem;
            font-size: 0.875rem;
          }
        }
      }
    }

    .preview-text {
      h4 {
        color: var(--text-color);
        margin-bottom: 1rem;
        font-weight: 600;
      }

      p {
        color: var(--text-color-secondary);
        line-height: 1.6;
        max-height: 300px;
        overflow-y: auto;
        padding-right: 0.5rem;
        
        // Custom scrollbar
        &::-webkit-scrollbar {
          width: 6px;
        }
        
        &::-webkit-scrollbar-track {
          background: var(--surface-100);
          border-radius: 3px;
        }
        
        &::-webkit-scrollbar-thumb {
          background: var(--surface-400);
          border-radius: 3px;
          
          &:hover {
            background: var(--surface-500);
          }
        }
      }
    }
  }
}

// Skeleton customizations
:deep(.p-skeleton) {
  background: linear-gradient(90deg, var(--surface-200) 25%, var(--surface-100) 50%, var(--surface-200) 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

// Responsive design improvements
@media screen and (max-width: 1200px) {
  .table-controls-header {
    padding: 0.875rem 1rem;
    
    .table-action-controls {
      gap: 0.375rem;
    }
  }
  
  .table-content-area {
    padding: 0.875rem;
  }
  
  .article-selection-table {
    :deep(.p-datatable-table) {
      min-width: 700px;
    }
    
    .author-chip {
      max-width: 100px;
    }
  }
}

@media screen and (max-width: 991px) {
  .table-controls-header {
    padding: 0.75rem;
    
    .flex {
      flex-direction: column;
      gap: 0.75rem;
      align-items: stretch;
      
      .table-action-controls {
        justify-content: center;
      }
    }
  }
  
  .table-content-area {
    padding: 0.75rem;
  }
  
  .article-selection-table {
    :deep(.p-datatable-table) {
      min-width: 600px;
    }
    
    :deep(.p-datatable-thead > tr > th) {
      padding: 0.75rem 0.5rem;
      font-size: 0.875rem;
    }
    
    :deep(.p-datatable-tbody > tr > td) {
      padding: 0.5rem;
      font-size: 0.875rem;
    }
  }
  
  .article-title {
    .title-text {
      -webkit-line-clamp: 1;
      max-height: 1.3em;
    }
    
    .url-text {
      font-size: 0.75rem;
    }
  }
  
  .author-chip {
    max-width: 80px;
    
    :deep(.p-chip-text) {
      font-size: 0.75rem;
    }
  }
}

@media screen and (max-width: 575px) {
  .table-controls-header {
    padding: 0.5rem;
  }
  
  .table-content-area {
    padding: 0.5rem;
  }
  
  .article-selection-table {
    :deep(.p-datatable-table) {
      min-width: 500px;
    }
    
    :deep(.p-datatable-thead > tr > th) {
      padding: 0.5rem 0.25rem;
      font-size: 0.75rem;
    }
    
    :deep(.p-datatable-tbody > tr > td) {
      padding: 0.375rem 0.25rem;
      font-size: 0.75rem;
    }
  }
  
  .article-title {
    .title-text {
      font-size: 0.875rem;
      line-height: 1.2;
    }
    
    .url-text {
      font-size: 0.625rem;
    }
  }
  
  .author-chip {
    max-width: 60px;
    
    :deep(.p-chip-text) {
      font-size: 0.625rem;
    }
  }

  .article-preview-dialog {
    :deep(.p-dialog) {
      width: 95vw !important;
      margin: 1rem;
    }
  }
}
</style>