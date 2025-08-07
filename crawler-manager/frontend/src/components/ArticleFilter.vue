<template>
  <!-- Using Sakai Panel for better styling -->
  <Panel class="article-filter">
    <template #header>
      <div class="flex align-items-center">
        <i class="pi pi-filter mr-2 text-primary"></i>
        <span class="font-semibold">Filter Articles</span>
      </div>
    </template>
    
    <!-- Simplified filter layout -->
    <div class="grid">
      <div class="col-12 md:col-6">
        <div class="field">
          <label for="title-search" class="font-semibold text-900">Search Title</label>
          <InputGroup>
            <InputGroupAddon>
              <i class="pi pi-search"></i>
            </InputGroupAddon>
            <InputText
              id="title-search"
              v-model="localFilters.titleSearch"
              placeholder="Search in titles..."
              @input="debouncedEmitChange"
            />
          </InputGroup>
        </div>
      </div>
      
      <div class="col-12 md:col-6">
        <div class="field">
          <label for="author-filter" class="font-semibold text-900">Author</label>
          <Dropdown
            id="author-filter"
            v-model="localFilters.authorId"
            :options="authors"
            optionLabel="name"
            optionValue="id"
            placeholder="All Authors"
            showClear
            class="w-full"
            @change="emitFilterChange"
          >
            <template #option="{ option }">
              <div class="flex align-items-center justify-content-between w-full">
                <span>{{ option.name }}</span>
                <Badge v-if="option.articleCount" :value="option.articleCount" severity="secondary" />
              </div>
            </template>
          </Dropdown>
        </div>
      </div>
      
      <div class="col-12 md:col-6">
        <div class="field">
          <label for="crawler-source" class="font-semibold text-900">Source</label>
          <Dropdown
            id="crawler-source"
            v-model="localFilters.crawlerSource"
            :options="sourceOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="All Sources"
            showClear
            class="w-full"
            @change="emitFilterChange"
          />
        </div>
      </div>
      
      <div class="col-12 md:col-6">
        <div class="field">
          <label for="date-range" class="font-semibold text-900">Date Range</label>
          <Calendar
            id="date-range"
            v-model="localFilters.dateRange"
            selectionMode="range"
            dateFormat="yy-mm-dd"
            showButtonBar
            class="w-full"
            @date-select="emitFilterChange"
          />
        </div>
      </div>
    </div>
    
    <!-- Filter Actions -->
    <div class="flex justify-content-between align-items-center mt-3">
      <div class="flex align-items-center gap-2">
        <Button 
          icon="pi pi-times"
          label="Clear All" 
          @click="clearAllFilters"
          severity="secondary"
          outlined
          size="small"
          :disabled="!hasActiveFilters"
        />
        <span v-if="hasActiveFilters" class="text-600 text-sm">
          {{ activeFilterCount }} filter{{ activeFilterCount > 1 ? 's' : '' }} active
        </span>
      </div>
    </div>
    
    <!-- Active Filters Display using Tags -->
    <div v-if="hasActiveFilters" class="mt-3">
      <div class="flex flex-wrap gap-2">
        <Tag 
          v-if="localFilters.authorId" 
          severity="info"
          @click="clearAuthorFilter"
          class="cursor-pointer filter-tag"
        >
          <template #default>
            <span>Author: {{ getAuthorName(localFilters.authorId) }}</span>
            <i class="pi pi-times ml-2"></i>
          </template>
        </Tag>
        
        <Tag 
          v-if="localFilters.titleSearch" 
          severity="info"
          @click="clearTitleFilter"
          class="cursor-pointer filter-tag"
        >
          <template #default>
            <span>Title: "{{ localFilters.titleSearch }}"</span>
            <i class="pi pi-times ml-2"></i>
          </template>
        </Tag>
        
        <Tag 
          v-if="localFilters.crawlerSource" 
          severity="info"
          @click="clearSourceFilter"
          class="cursor-pointer filter-tag"
        >
          <template #default>
            <span>Source: {{ getSourceLabel(localFilters.crawlerSource) }}</span>
            <i class="pi pi-times ml-2"></i>
          </template>
        </Tag>
        
        <Tag 
          v-if="localFilters.dateRange && localFilters.dateRange.length === 2" 
          severity="info"
          @click="clearDateFilter"
          class="cursor-pointer filter-tag"
        >
          <template #default>
            <span>Date: {{ formatDateRange(localFilters.dateRange) }}</span>
            <i class="pi pi-times ml-2"></i>
          </template>
        </Tag>
      </div>
    </div>
  </Panel>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { debounce } from 'lodash-es'
import Panel from 'primevue/panel'
import Dropdown from 'primevue/dropdown'
import InputText from 'primevue/inputtext'
import InputGroup from 'primevue/inputgroup'
import InputGroupAddon from 'primevue/inputgroupaddon'
import Calendar from 'primevue/calendar'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Badge from 'primevue/badge'

export interface ArticleFilters {
  authorId: number | null
  titleSearch: string
  dateRange: Date[] | null
  crawlerSource: string | null
}

export interface Author {
  id: number
  name: string
  avatarUrl?: string
  articleCount?: number
}

interface Props {
  filters: ArticleFilters
  authors: Author[]
}

interface Emits {
  (e: 'filter-changed', filters: ArticleFilters): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// Local reactive copy of filters for v-model binding
const localFilters = ref<ArticleFilters>({ ...props.filters })

// Source options for dropdown
const sourceOptions = computed(() => [
  { label: 'Drucker', value: 'drucker' },
  { label: 'Caspit', value: 'caspit' }
])

// Watch for external filter changes and sync local state
watch(() => props.filters, (newFilters) => {
  localFilters.value = { ...newFilters }
}, { deep: true })

// Debounced emit for text input
const debouncedEmitChange = debounce(() => {
  emitFilterChange()
}, 300)

// Emit filter changes
const emitFilterChange = () => {
  emit('filter-changed', { ...localFilters.value })
}

// Clear all filters
const clearAllFilters = () => {
  localFilters.value = {
    authorId: null,
    titleSearch: '',
    dateRange: null,
    crawlerSource: null
  }
  emitFilterChange()
}

// Active filters functionality
const hasActiveFilters = computed(() => {
  return localFilters.value.authorId !== null ||
         localFilters.value.titleSearch !== '' ||
         localFilters.value.crawlerSource !== null ||
         (localFilters.value.dateRange && localFilters.value.dateRange.length === 2)
})

const activeFilterCount = computed(() => {
  let count = 0
  if (localFilters.value.authorId !== null) count++
  if (localFilters.value.titleSearch !== '') count++
  if (localFilters.value.crawlerSource !== null) count++
  if (localFilters.value.dateRange && localFilters.value.dateRange.length === 2) count++
  return count
})

const getAuthorName = (authorId: number) => {
  const author = props.authors.find(a => a.id === authorId)
  return author?.name || 'Unknown'
}

const getSourceLabel = (sourceValue: string) => {
  const source = sourceOptions.value.find(s => s.value === sourceValue)
  return source?.label || sourceValue
}

const formatDateRange = (dateRange: Date[]) => {
  if (!dateRange || dateRange.length !== 2) return ''
  const start = dateRange[0].toLocaleDateString()
  const end = dateRange[1].toLocaleDateString()
  return `${start} - ${end}`
}

// Individual filter clear methods
const clearAuthorFilter = () => {
  localFilters.value.authorId = null
  emitFilterChange()
}

const clearTitleFilter = () => {
  localFilters.value.titleSearch = ''
  emitFilterChange()
}

const clearSourceFilter = () => {
  localFilters.value.crawlerSource = null
  emitFilterChange()
}

const clearDateFilter = () => {
  localFilters.value.dateRange = null
  emitFilterChange()
}
</script>

<style lang="scss" scoped>
.article-filter {
  margin-bottom: 1rem;
}

.field {
  margin-bottom: 1rem;

  label {
    display: block;
    margin-bottom: 0.5rem;
    font-size: 0.875rem;
  }
}

// Active filters styling
.filter-tag {
  transition: all var(--transition-duration);
  
  &:hover {
    background-color: var(--primary-600);
    transform: translateY(-1px);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }
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
  padding: 1.25rem;
}

// InputGroup customizations
:deep(.p-inputgroup) {
  .p-inputgroup-addon {
    background: var(--surface-100);
    border-color: var(--surface-border);
    color: var(--text-color-secondary);
  }
}

// Form controls styling
:deep(.p-dropdown) {
  border: 1px solid var(--surface-border);
  
  &:hover {
    border-color: var(--primary-color);
  }
  
  &:focus {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 0.2rem var(--primary-200);
  }
}

:deep(.p-inputtext) {
  border: 1px solid var(--surface-border);
  
  &:hover {
    border-color: var(--primary-color);
  }
  
  &:focus {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 0.2rem var(--primary-200);
  }
}

:deep(.p-calendar) {
  .p-inputtext {
    border: 1px solid var(--surface-border);
    
    &:hover {
      border-color: var(--primary-color);
    }
    
    &:focus {
      border-color: var(--primary-color);
      box-shadow: 0 0 0 0.2rem var(--primary-200);
    }
  }
}

// Responsive design
@media screen and (max-width: 991px) {
  :deep(.p-panel .p-panel-header) {
    padding: 0.75rem 1rem;
  }
  
  :deep(.p-panel .p-panel-content) {
    padding: 1rem;
  }
}

@media screen and (max-width: 575px) {
  :deep(.p-panel .p-panel-header) {
    padding: 0.75rem;
  }
  
  :deep(.p-panel .p-panel-content) {
    padding: 0.75rem;
  }
  
  .field {
    margin-bottom: 0.75rem;
  }
}

// Dark theme adjustments
:root.dark {
  :deep(.p-inputgroup) {
    .p-inputgroup-addon {
      background: var(--surface-700);
      color: var(--text-color-secondary);
    }
  }
}
</style>