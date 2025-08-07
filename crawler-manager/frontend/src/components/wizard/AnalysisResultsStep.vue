<template>
  <div class="analysis-results-step">
    <!-- Placeholder for Analysis Results Step -->
    <Panel>
      <template #header>
        <div class="flex align-items-center">
          <i class="pi pi-chart-bar mr-2 text-primary"></i>
          <span class="font-semibold">Analysis Results</span>
        </div>
      </template>
      
      <div class="p-4">
        <div v-if="!hasResults" class="text-center p-6">
          <div class="mb-4">
            <i class="pi pi-chart-line text-6xl text-400"></i>
          </div>
          <div class="text-900 font-medium text-xl mb-2">Analysis Results Step</div>
          <div class="text-600 mb-4">This component will be implemented in task 5.1</div>
          
          <!-- Generate test results -->
          <Button 
            label="Generate Test Results" 
            @click="generateTestResults"
          />
        </div>
        
        <div v-else>
          <!-- Results Summary -->
          <div class="results-summary mb-4">
            <div class="grid">
              <div class="col-12 md:col-4">
                <div class="surface-card p-3 border-round">
                  <div class="text-600 font-medium mb-2">Total Predictions</div>
                  <div class="text-2xl font-bold text-primary">{{ results.length }}</div>
                </div>
              </div>
              <div class="col-12 md:col-4">
                <div class="surface-card p-3 border-round">
                  <div class="text-600 font-medium mb-2">Articles Analyzed</div>
                  <div class="text-2xl font-bold text-primary">{{ articlesAnalyzed }}</div>
                </div>
              </div>
              <div class="col-12 md:col-4">
                <div class="surface-card p-3 border-round">
                  <div class="text-600 font-medium mb-2">Avg Confidence</div>
                  <div class="text-2xl font-bold text-primary">{{ averageConfidence }}%</div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Sample Results -->
          <div class="results-list">
            <div class="text-lg font-medium mb-3">Sample Predictions</div>
            <div v-for="result in results.slice(0, 3)" :key="result.id" class="result-item mb-3">
              <div class="surface-card p-3 border-round">
                <div class="flex align-items-start justify-content-between">
                  <div class="flex-1">
                    <div class="font-medium mb-2">{{ result.predictionText }}</div>
                    <div class="text-sm text-600 mb-2">
                      From: {{ result.article.title }}
                    </div>
                    <div class="flex align-items-center gap-2">
                      <Badge :value="`${result.confidenceScore}% confidence`" severity="info" />
                      <Badge :value="result.rating" :severity="getRatingSeverity(result.rating)" />
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <!-- Actions -->
          <div class="results-actions mt-4 text-center">
            <Button 
              label="Start New Analysis" 
              icon="pi pi-refresh"
              @click="startNewAnalysis"
              class="mr-2"
            />
            <Button 
              label="View Full Results" 
              icon="pi pi-external-link"
              severity="secondary"
              outlined
              @click="viewFullResults"
            />
          </div>
        </div>
      </div>
    </Panel>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, inject, onMounted } from 'vue'
import Panel from 'primevue/panel'
import Button from 'primevue/button'
import Badge from 'primevue/badge'

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

// Inject wizard context
const wizardContext = inject('wizardContext') as any

// Reactive state
const results = ref<any[]>([])

// Computed
const hasResults = computed(() => results.value.length > 0)

const articlesAnalyzed = computed(() => {
  const uniqueArticles = new Set(results.value.map(r => r.article.id))
  return uniqueArticles.size
})

const averageConfidence = computed(() => {
  if (results.value.length === 0) return 0
  const total = results.value.reduce((sum, r) => sum + r.confidenceScore, 0)
  return Math.round(total / results.value.length)
})

// Methods
const generateTestResults = () => {
  const selectedArticles = props.wizardState?.selectedArticles || []
  
  results.value = selectedArticles.flatMap((article: any, index: number) => [
    {
      id: `pred-${article.id}-1`,
      predictionText: `Prediction ${index + 1}A: This will happen within 6 months`,
      confidenceScore: Math.floor(Math.random() * 40) + 60, // 60-100%
      rating: ['HIGH', 'MEDIUM', 'LOW'][Math.floor(Math.random() * 3)],
      article: {
        id: article.id,
        title: article.title
      },
      extractedAt: new Date().toISOString()
    },
    {
      id: `pred-${article.id}-2`,
      predictionText: `Prediction ${index + 1}B: Market will respond positively`,
      confidenceScore: Math.floor(Math.random() * 40) + 60, // 60-100%
      rating: ['HIGH', 'MEDIUM', 'LOW'][Math.floor(Math.random() * 3)],
      article: {
        id: article.id,
        title: article.title
      },
      extractedAt: new Date().toISOString()
    }
  ])
  
  // Emit step data
  emit('step-data', { results: results.value })
  
  // Mark step as valid and complete
  emit('step-valid', true)
  emit('step-complete', 'analysis-results')
}

const getRatingSeverity = (rating: string) => {
  switch (rating) {
    case 'HIGH': return 'success'
    case 'MEDIUM': return 'warning'
    case 'LOW': return 'danger'
    default: return 'info'
  }
}

const startNewAnalysis = () => {
  // Reset wizard to first step
  if (wizardContext?.goToStep) {
    wizardContext.goToStep(1)
  }
}

const viewFullResults = () => {
  // Navigate to full results view (would be implemented later)
  console.log('Navigate to full results view')
}

// Initialize from wizard state
onMounted(() => {
  if (props.wizardState?.results) {
    results.value = props.wizardState.results
    
    if (results.value.length > 0) {
      emit('step-valid', true)
    }
  }
  
  // Auto-generate results if we have a completed job but no results
  if (props.wizardState?.currentJob?.status === 'COMPLETED' && results.value.length === 0) {
    generateTestResults()
  }
})
</script>

<style lang="scss" scoped>
.analysis-results-step {
  // Styles will be added when implementing the actual component
}

.results-summary {
  .surface-card {
    background: var(--surface-card);
    border: 1px solid var(--surface-border);
  }
}

.result-item {
  .surface-card {
    background: var(--surface-card);
    border: 1px solid var(--surface-border);
    transition: all 0.3s ease;
    
    &:hover {
      border-color: var(--primary-color);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }
  }
}

.results-actions {
  padding-top: 1rem;
  border-top: 1px solid var(--surface-border);
}
</style>