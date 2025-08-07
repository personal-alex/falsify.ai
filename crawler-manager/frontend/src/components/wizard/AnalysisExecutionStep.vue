<template>
  <div class="analysis-execution-step">
    <Panel>
      <template #header>
        <div class="flex align-items-center">
          <i class="pi pi-cog mr-2 text-primary"></i>
          <span class="font-semibold">Execute Analysis</span>
        </div>
      </template>
      
      <!-- Analysis Configuration Section -->
      <div v-if="!currentJob" class="analysis-config p-4">
        <div class="grid">
          <div class="col-12 md:col-6">
            <div class="field">
              <label for="analysisType" class="block text-900 font-medium mb-2">Analysis Type</label>
              <Dropdown
                id="analysisType"
                v-model="analysisConfig.analysisType"
                :options="analysisTypeOptions"
                optionLabel="label"
                optionValue="value"
                placeholder="Select analysis type"
                class="w-full"
              />
            </div>
          </div>
          
          <div class="col-12 md:col-6">
            <div class="field">
              <label for="batchSize" class="block text-900 font-medium mb-2">Batch Size</label>
              <InputNumber
                id="batchSize"
                v-model="analysisConfig.batchSize"
                :min="1"
                :max="50"
                placeholder="Articles per batch"
                class="w-full"
              />
            </div>
          </div>
        </div>
        
        <div class="field-checkbox mb-4">
          <Checkbox
            id="enableNotifications"
            v-model="analysisConfig.enableNotifications"
            :binary="true"
          />
          <label for="enableNotifications" class="ml-2">Enable real-time notifications</label>
        </div>
        
        <!-- Selected Articles Summary -->
        <div class="selected-articles-summary mb-4">
          <div class="text-900 font-medium mb-2">Selected Articles</div>
          <div class="text-600">
            {{ selectedArticlesCount }} articles selected for analysis
          </div>
          <div v-if="selectedArticlesCount === 0" class="text-red-500 text-sm mt-1">
            <i class="pi pi-exclamation-triangle mr-1"></i>
            No articles selected. Please go back to select articles.
          </div>
        </div>
        
        <!-- Start Analysis Button -->
        <div class="text-center">
          <Button
            label="Start Analysis"
            icon="pi pi-play"
            @click="startAnalysis"
            :loading="isStarting"
            :disabled="selectedArticlesCount === 0"
            class="p-button-lg"
          />
        </div>
      </div>
      
      <!-- Analysis Progress Section -->
      <div v-else class="analysis-progress p-4">
        <div class="text-center mb-4">
          <div class="text-2xl font-semibold text-900 mb-2">
            {{ getStatusDisplayText() }}
          </div>
          <div class="text-600 mb-2">Job ID: {{ currentJob.jobId }}</div>
          <div class="text-sm text-500">
            Started: {{ formatDateTime(currentJob.startedAt) }}
          </div>
        </div>
        
        <!-- Progress Indicator -->
        <div v-if="currentJob.status === 'RUNNING'" class="progress-section mb-4">
          <div class="flex justify-content-between align-items-center mb-2">
            <span class="text-900 font-medium">Progress</span>
            <span class="text-600">{{ progressPercentage }}%</span>
          </div>
          <ProgressBar :value="progressPercentage" class="mb-3" />
          
          <div class="grid text-center">
            <div class="col-4">
              <div class="text-2xl font-bold text-primary">{{ currentJob.processedArticles }}</div>
              <div class="text-sm text-600">Processed</div>
            </div>
            <div class="col-4">
              <div class="text-2xl font-bold text-green-500">{{ currentJob.predictionsFound || 0 }}</div>
              <div class="text-sm text-600">Predictions Found</div>
            </div>
            <div class="col-4">
              <div class="text-2xl font-bold text-600">{{ currentJob.totalArticles }}</div>
              <div class="text-sm text-600">Total Articles</div>
            </div>
          </div>
          
          <!-- Current Activity -->
          <div v-if="currentActivity" class="current-activity mt-3 p-3 border-round surface-100">
            <div class="flex align-items-center">
              <ProgressSpinner style="width: 20px; height: 20px" strokeWidth="4" class="mr-2" />
              <span class="text-sm">{{ currentActivity }}</span>
            </div>
          </div>
        </div>
        
        <!-- Success State -->
        <div v-if="currentJob.status === 'COMPLETED'" class="success-state text-center mb-4">
          <div class="text-green-500 mb-3">
            <i class="pi pi-check-circle text-6xl"></i>
          </div>
          <div class="text-xl font-semibold text-900 mb-2">Analysis Completed Successfully!</div>
          <div class="text-600 mb-3">
            Found {{ currentJob.predictionsFound || 0 }} predictions in {{ currentJob.processedArticles }} articles
          </div>
          <div class="text-sm text-500">
            Completed: {{ formatDateTime(currentJob.completedAt) }}
          </div>
        </div>
        
        <!-- Error State -->
        <div v-if="currentJob.status === 'FAILED'" class="error-state text-center mb-4">
          <div class="text-red-500 mb-3">
            <i class="pi pi-times-circle text-6xl"></i>
          </div>
          <div class="text-xl font-semibold text-900 mb-2">Analysis Failed</div>
          <div class="text-600 mb-3" v-if="errorMessage">
            {{ errorMessage }}
          </div>
          <div class="flex justify-content-center gap-2">
            <Button
              label="Retry Analysis"
              icon="pi pi-refresh"
              @click="retryAnalysis"
              :loading="isRetrying"
              class="p-button-outlined"
            />
            <Button
              label="Start New Analysis"
              icon="pi pi-plus"
              @click="resetAnalysis"
              class="p-button-outlined p-button-secondary"
            />
          </div>
        </div>
        
        <!-- Cancelled State -->
        <div v-if="currentJob.status === 'CANCELLED'" class="cancelled-state text-center mb-4">
          <div class="text-orange-500 mb-3">
            <i class="pi pi-ban text-6xl"></i>
          </div>
          <div class="text-xl font-semibold text-900 mb-2">Analysis Cancelled</div>
          <div class="text-600 mb-3">
            The analysis was cancelled by user request
          </div>
          <Button
            label="Start New Analysis"
            icon="pi pi-plus"
            @click="resetAnalysis"
            class="p-button-outlined"
          />
        </div>
        
        <!-- Action Buttons -->
        <div class="action-buttons text-center">
          <Button
            v-if="currentJob.status === 'RUNNING'"
            label="Cancel Analysis"
            icon="pi pi-times"
            @click="showCancelDialog = true"
            class="p-button-outlined p-button-danger"
            :loading="isCancelling"
          />
          
          <Button
            v-if="currentJob.status === 'COMPLETED'"
            label="View Results"
            icon="pi pi-eye"
            @click="viewResults"
            class="p-button-outlined p-button-success"
          />
        </div>
      </div>
    </Panel>
    
    <!-- Cancel Confirmation Dialog -->
    <Dialog
      v-model:visible="showCancelDialog"
      modal
      header="Cancel Analysis"
      :style="{ width: '450px' }"
    >
      <div class="flex align-items-center mb-3">
        <i class="pi pi-exclamation-triangle text-orange-500 text-2xl mr-3"></i>
        <span>Are you sure you want to cancel the running analysis?</span>
      </div>
      <div class="text-600 text-sm mb-4">
        This action cannot be undone. Any progress will be lost.
      </div>
      
      <template #footer>
        <Button
          label="Keep Running"
          @click="showCancelDialog = false"
          class="p-button-outlined"
        />
        <Button
          label="Cancel Analysis"
          @click="cancelAnalysis"
          class="p-button-danger"
          :loading="isCancelling"
        />
      </template>
    </Dialog>
    
    <!-- Error Dialog -->
    <Dialog
      v-model:visible="showErrorDialog"
      modal
      header="Analysis Error"
      :style="{ width: '500px' }"
    >
      <div class="flex align-items-start mb-3">
        <i class="pi pi-times-circle text-red-500 text-2xl mr-3 mt-1"></i>
        <div>
          <div class="font-semibold mb-2">Analysis Failed</div>
          <div class="text-600">{{ errorMessage }}</div>
        </div>
      </div>
      
      <template #footer>
        <Button
          label="Close"
          @click="showErrorDialog = false"
          class="p-button-outlined"
        />
        <Button
          label="Retry"
          @click="retryAnalysis"
          :loading="isRetrying"
        />
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import Panel from 'primevue/panel'
import Button from 'primevue/button'
import ProgressBar from 'primevue/progressbar'
import Dropdown from 'primevue/dropdown'
import InputNumber from 'primevue/inputnumber'
import Checkbox from 'primevue/checkbox'
import Dialog from 'primevue/dialog'
import ProgressSpinner from 'primevue/progressspinner'
import { useToast } from 'primevue/usetoast'
import { ApiService } from '@/services/api'

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
  'navigate-to': [stepId: string]
}>()

// Composables
const toast = useToast()

// Reactive state
const currentJob = ref<any>(null)
const isStarting = ref(false)
const isCancelling = ref(false)
const isRetrying = ref(false)
const showCancelDialog = ref(false)
const showErrorDialog = ref(false)
const errorMessage = ref('')
const currentActivity = ref('')
const websocket = ref<WebSocket | null>(null)
const progressInterval = ref<NodeJS.Timeout | null>(null)

// Analysis configuration
const analysisConfig = ref({
  analysisType: 'gemini',
  batchSize: 10,
  enableNotifications: true
})

// Analysis type options
const analysisTypeOptions = [
  { label: 'Gemini AI (Recommended)', value: 'gemini' },
  { label: 'Mock Analysis (Testing)', value: 'mock' }
]

// Computed
const selectedArticlesCount = computed(() => {
  return props.wizardState?.selectedArticles?.length || 0
})

const progressPercentage = computed(() => {
  if (!currentJob.value || !currentJob.value.totalArticles) return 0
  return Math.round((currentJob.value.processedArticles / currentJob.value.totalArticles) * 100)
})

// Methods
const startAnalysis = async () => {
  if (selectedArticlesCount.value === 0) {
    toast.add({
      severity: 'warn',
      summary: 'No Articles Selected',
      detail: 'Please select articles before starting analysis',
      life: 3000
    })
    return
  }

  isStarting.value = true
  errorMessage.value = ''

  try {
    // Get selected article IDs
    const articleIds = props.wizardState.selectedArticles.map((article: any) => article.id)
    
    // Start analysis via API
    const response = await ApiService.startAnalysis(
      articleIds,
      analysisConfig.value.analysisType
    )

    if (response) {
      currentJob.value = {
        id: response.id,
        jobId: response.jobId,
        status: response.status,
        totalArticles: response.totalArticles,
        processedArticles: response.processedArticles || 0,
        predictionsFound: response.predictionsFound || 0,
        startedAt: response.startedAt || new Date().toISOString()
      }

      // Emit step data
      emit('step-data', { 
        currentJob: currentJob.value,
        analysisConfig: analysisConfig.value
      })

      // Start progress monitoring
      startProgressMonitoring()

      // Setup WebSocket if enabled
      if (analysisConfig.value.enableNotifications) {
        setupWebSocket()
      }

      toast.add({
        severity: 'success',
        summary: 'Analysis Started',
        detail: `Analysis job ${currentJob.value.jobId} has been started`,
        life: 3000
      })
    }
  } catch (error: any) {
    console.error('Failed to start analysis:', error)
    errorMessage.value = error.response?.data?.error || 'Failed to start analysis'
    showErrorDialog.value = true
    
    toast.add({
      severity: 'error',
      summary: 'Analysis Failed',
      detail: errorMessage.value,
      life: 5000
    })
  } finally {
    isStarting.value = false
  }
}

const startProgressMonitoring = () => {
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
  }

  progressInterval.value = setInterval(async () => {
    if (!currentJob.value || currentJob.value.status !== 'RUNNING') {
      clearInterval(progressInterval.value!)
      return
    }

    try {
      const updatedJob = await ApiService.getAnalysisJobStatus(currentJob.value.jobId)
      
      if (updatedJob) {
        currentJob.value = {
          ...currentJob.value,
          status: updatedJob.status,
          processedArticles: updatedJob.processedArticles || 0,
          predictionsFound: updatedJob.predictionsFound || 0,
          completedAt: updatedJob.completedAt,
          errorMessage: updatedJob.errorMessage
        }

        // Update current activity
        if (updatedJob.status === 'RUNNING') {
          currentActivity.value = `Processing article ${currentJob.value.processedArticles + 1} of ${currentJob.value.totalArticles}...`
        } else {
          currentActivity.value = ''
        }

        // Emit updated data
        emit('step-data', { 
          currentJob: currentJob.value,
          analysisConfig: analysisConfig.value
        })

        // Handle completion
        if (updatedJob.status === 'COMPLETED') {
          clearInterval(progressInterval.value!)
          emit('step-valid', true)
          emit('step-complete', 'analysis-execution')
          
          toast.add({
            severity: 'success',
            summary: 'Analysis Completed',
            detail: `Found ${currentJob.value.predictionsFound} predictions`,
            life: 5000
          })
        } else if (updatedJob.status === 'FAILED') {
          clearInterval(progressInterval.value!)
          errorMessage.value = updatedJob.errorMessage || 'Analysis failed'
          
          toast.add({
            severity: 'error',
            summary: 'Analysis Failed',
            detail: errorMessage.value,
            life: 5000
          })
        }
      }
    } catch (error) {
      console.error('Failed to fetch job status:', error)
    }
  }, 2000) // Check every 2 seconds
}

const setupWebSocket = () => {
  try {
    // Note: WebSocket URL would need to be configured based on the actual WebSocket endpoint
    // For now, we'll rely on polling for progress updates
    console.log('WebSocket notifications would be set up here')
  } catch (error) {
    console.error('Failed to setup WebSocket:', error)
  }
}

const cancelAnalysis = async () => {
  if (!currentJob.value) return

  isCancelling.value = true
  showCancelDialog.value = false

  try {
    await ApiService.cancelAnalysisJob(currentJob.value.jobId)
    
    currentJob.value.status = 'CANCELLED'
    currentJob.value.cancelledAt = new Date().toISOString()
    
    // Clear progress monitoring
    if (progressInterval.value) {
      clearInterval(progressInterval.value)
    }
    
    // Emit updated data
    emit('step-data', { 
      currentJob: currentJob.value,
      analysisConfig: analysisConfig.value
    })

    toast.add({
      severity: 'info',
      summary: 'Analysis Cancelled',
      detail: 'The analysis job has been cancelled',
      life: 3000
    })
  } catch (error: any) {
    console.error('Failed to cancel analysis:', error)
    toast.add({
      severity: 'error',
      summary: 'Cancellation Failed',
      detail: error.response?.data?.error || 'Failed to cancel analysis',
      life: 5000
    })
  } finally {
    isCancelling.value = false
  }
}

const retryAnalysis = async () => {
  isRetrying.value = true
  showErrorDialog.value = false
  
  // Reset current job
  currentJob.value = null
  errorMessage.value = ''
  
  // Restart analysis
  await startAnalysis()
  
  isRetrying.value = false
}

const resetAnalysis = () => {
  // Clear current job and reset to configuration state
  currentJob.value = null
  errorMessage.value = ''
  currentActivity.value = ''
  
  // Clear any intervals
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
  }
  
  // Close WebSocket
  if (websocket.value) {
    websocket.value.close()
    websocket.value = null
  }
  
  // Emit reset data
  emit('step-data', { 
    currentJob: null,
    analysisConfig: analysisConfig.value
  })
  
  // Mark step as invalid
  emit('step-valid', false)
}

const viewResults = () => {
  // Navigate to results step
  emit('navigate-to', 'analysis-results')
}

const getStatusDisplayText = () => {
  switch (currentJob.value?.status) {
    case 'RUNNING':
      return 'Analysis in Progress'
    case 'COMPLETED':
      return 'Analysis Completed'
    case 'FAILED':
      return 'Analysis Failed'
    case 'CANCELLED':
      return 'Analysis Cancelled'
    default:
      return 'Unknown Status'
  }
}

const formatDateTime = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString()
}

// Watch for job status changes
watch(() => currentJob.value?.status, (newStatus) => {
  const isValid = newStatus === 'COMPLETED'
  emit('step-valid', isValid)
})

// Watch for active state changes
watch(() => props.isActive, (isActive) => {
  if (isActive && currentJob.value?.status === 'RUNNING') {
    // Resume progress monitoring if step becomes active
    startProgressMonitoring()
  } else if (!isActive && progressInterval.value) {
    // Pause progress monitoring if step becomes inactive
    clearInterval(progressInterval.value)
  }
})

// Initialize from wizard state
onMounted(() => {
  if (props.wizardState?.currentJob) {
    currentJob.value = props.wizardState.currentJob
    
    // Check if job is already completed
    if (currentJob.value.status === 'COMPLETED') {
      emit('step-valid', true)
    } else if (currentJob.value.status === 'RUNNING') {
      // Resume progress monitoring for running jobs
      startProgressMonitoring()
    }
  }
  
  if (props.wizardState?.analysisConfig) {
    analysisConfig.value = { ...analysisConfig.value, ...props.wizardState.analysisConfig }
  }
})

// Cleanup on unmount
onUnmounted(() => {
  if (progressInterval.value) {
    clearInterval(progressInterval.value)
  }
  
  if (websocket.value) {
    websocket.value.close()
  }
})
</script>

<style lang="scss" scoped>
@import "@/assets/themes/_variables.scss";

.analysis-execution-step {
  .analysis-config {
    max-width: 600px;
    margin: 0 auto;
  }
  
  .analysis-progress {
    max-width: 700px;
    margin: 0 auto;
  }
  
  .progress-section {
    .grid {
      margin-top: 1rem;
      
      .col-4 {
        padding: 0.5rem;
      }
    }
  }
  
  .current-activity {
    background: var(--surface-100);
    border: 1px solid var(--surface-200);
    border-radius: var(--border-radius);
    
    .p-progressspinner {
      width: 20px !important;
      height: 20px !important;
    }
  }
  
  .success-state,
  .error-state,
  .cancelled-state {
    .pi {
      margin-bottom: 1rem;
    }
  }
  
  .selected-articles-summary {
    background: var(--surface-50);
    border: 1px solid var(--surface-200);
    border-radius: var(--border-radius);
    padding: 1rem;
    margin-bottom: 1.5rem;
  }
  
  .action-buttons {
    margin-top: 2rem;
    
    .p-button {
      margin: 0 0.5rem;
    }
  }
  
  // Responsive design
  @media (max-width: 768px) {
    .analysis-config,
    .analysis-progress {
      max-width: 100%;
      padding: 1rem;
    }
    
    .grid .col-4 {
      margin-bottom: 1rem;
    }
    
    .action-buttons .p-button {
      margin: 0.25rem;
      width: 100%;
    }
  }
}

// Dialog customizations
:deep(.p-dialog) {
  .p-dialog-header {
    background: var(--surface-0);
    border-bottom: 1px solid var(--surface-200);
  }
  
  .p-dialog-content {
    padding: 1.5rem;
  }
  
  .p-dialog-footer {
    background: var(--surface-50);
    border-top: 1px solid var(--surface-200);
    padding: 1rem 1.5rem;
    
    .p-button {
      margin-left: 0.5rem;
    }
  }
}

// Progress bar customizations
:deep(.p-progressbar) {
  height: 0.75rem;
  background: var(--surface-200);
  border-radius: var(--border-radius);
  
  .p-progressbar-value {
    background: linear-gradient(90deg, var(--primary-color), var(--primary-color-text));
    border-radius: var(--border-radius);
  }
}

// Button customizations for different states
:deep(.p-button) {
  &.p-button-lg {
    padding: 0.75rem 2rem;
    font-size: 1.1rem;
  }
  
  &.p-button-danger {
    background: var(--red-500);
    border-color: var(--red-500);
    
    &:hover {
      background: var(--red-600);
      border-color: var(--red-600);
    }
  }
  
  &.p-button-success {
    background: var(--green-500);
    border-color: var(--green-500);
    
    &:hover {
      background: var(--green-600);
      border-color: var(--green-600);
    }
  }
}

// Input field customizations
:deep(.p-dropdown),
:deep(.p-inputnumber) {
  width: 100%;
}

:deep(.p-checkbox) {
  .p-checkbox-box {
    border-color: var(--surface-400);
    
    &.p-highlight {
      background: var(--primary-color);
      border-color: var(--primary-color);
    }
  }
}
</style>