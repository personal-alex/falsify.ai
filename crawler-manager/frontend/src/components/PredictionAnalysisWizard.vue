<template>
  <div class="prediction-analysis-wizard">
    <!-- Wizard Header with Step Indicator -->
    <Panel class="wizard-header mb-4">
      <template #header>
        <div class="flex align-items-center justify-content-between w-full">
          <div class="flex align-items-center">
            <i class="pi pi-search mr-2 text-primary"></i>
            <span class="font-semibold text-lg">Prediction Analysis</span>
          </div>
          <div class="wizard-progress">
            <span class="text-sm text-600">Step {{ currentStep }} of {{ totalSteps }}</span>
          </div>
        </div>
      </template>
      
      <!-- Step Progress Indicator -->
      <div class="step-indicator">
        <div class="steps-container">
          <div 
            v-for="(step, index) in steps" 
            :key="step.id"
            class="step-item"
            :class="{
              'step-active': index + 1 === currentStep,
              'step-completed': index + 1 < currentStep,
              'step-disabled': index + 1 > currentStep
            }"
          >
            <div class="step-circle">
              <i 
                v-if="index + 1 < currentStep" 
                class="pi pi-check"
              ></i>
              <span v-else>{{ index + 1 }}</span>
            </div>
            <div class="step-content">
              <div class="step-title">{{ step.title }}</div>
              <div class="step-description">{{ step.description }}</div>
            </div>
            <div 
              v-if="index < steps.length - 1" 
              class="step-connector"
              :class="{ 'connector-completed': index + 1 < currentStep }"
            ></div>
          </div>
        </div>
      </div>
    </Panel>

    <!-- Step Content Container -->
    <div class="wizard-content">
      <Transition 
        :name="transitionName" 
        mode="out-in"
        @before-enter="onBeforeEnter"
        @after-enter="onAfterEnter"
      >
        <component 
          :is="currentStepComponent"
          :key="currentStep"
          v-bind="currentStepProps"
          @step-valid="onStepValidityChanged"
          @step-complete="onStepComplete"
          @step-data="onStepDataChanged"
        />
      </Transition>
    </div>

    <!-- Recovery Dialog -->
    <WizardRecoveryDialog
      v-model:visible="recoveryDialog.show"
      :summary="recoveryDialog.summary"
      :loading="recoveryDialog.loading"
      @continue-session="handleContinueSession"
      @start-fresh="handleStartFresh"
    />

    <!-- Navigation Controls -->
    <Panel class="wizard-navigation mt-4">
      <div class="flex align-items-center justify-content-between">
        <div class="flex align-items-center gap-2">
          <Button 
            icon="pi pi-arrow-left"
            label="Previous"
            severity="secondary"
            outlined
            :disabled="!canGoBack"
            @click="goToPreviousStep"
            v-tooltip.top="'Go to previous step'"
          />
        </div>
        
        <div class="flex align-items-center gap-2">
          <Button 
            icon="pi pi-times"
            label="Cancel"
            severity="secondary"
            text
            @click="cancelWizard"
            v-tooltip.top="'Cancel and return to main page'"
          />
          
          <Button 
            v-if="!isLastStep"
            icon="pi pi-arrow-right"
            iconPos="right"
            :label="nextButtonLabel"
            :disabled="!canProceed"
            :loading="isProcessing"
            @click="goToNextStep"
            v-tooltip.top="nextButtonTooltip"
          />
          
          <Button 
            v-else
            icon="pi pi-check"
            label="Complete"
            :disabled="!canProceed"
            :loading="isProcessing"
            @click="completeWizard"
            v-tooltip.top="'Complete the analysis process'"
          />
        </div>
      </div>
    </Panel>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, provide } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import { useConfirm } from 'primevue/useconfirm'
import Panel from 'primevue/panel'
import Button from 'primevue/button'

// Import step components
import ArticleSelectionStep from './wizard/ArticleSelectionStep.vue'
import AnalysisExecutionStep from './wizard/AnalysisExecutionStep.vue'
import AnalysisResultsStep from './wizard/AnalysisResultsStep.vue'

// Import validation system
import {
  WizardStepConfigFactory,
  WizardValidationUtils,
  type WizardStepConfig,
  type WizardStepState
} from './wizard/WizardStepBase'

// Import recovery system
import {
  WizardRecoveryManager,
  WizardAutoSaveManager,
  type WizardRecoveryDialogData
} from './wizard/WizardRecoverySystem'

// Import recovery dialog
import WizardRecoveryDialog from './wizard/WizardRecoveryDialog.vue'

// Types (removed unused WizardStep interface)

interface WizardState {
  currentStep: number
  totalSteps: number
  canProceed: boolean
  canGoBack: boolean
  selectedArticles: any[]
  analysisConfig: any
  currentJob: any | null
  results: any[]
  stepData: Record<string, any>
}

// Props
interface Props {
  initialStep?: number
  autoSave?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  initialStep: 1,
  autoSave: true
})

// Emits
const emit = defineEmits<{
  'wizard-complete': [data: any]
  'wizard-cancelled': []
  'step-changed': [step: number]
}>()

// Composables
const router = useRouter()
const toast = useToast()
const confirm = useConfirm()

// Reactive state
const currentStep = ref(props.initialStep)
const isProcessing = ref(false)
const transitionName = ref('slide-left')

// Wizard steps configuration using the validation system
const stepConfigs = ref<WizardStepConfig[]>([
  { ...WizardStepConfigFactory.createArticleSelectionStep(), component: ArticleSelectionStep },
  { ...WizardStepConfigFactory.createAnalysisExecutionStep(), component: AnalysisExecutionStep },
  { ...WizardStepConfigFactory.createAnalysisResultsStep(), component: AnalysisResultsStep }
])

// Legacy steps format for template compatibility
const steps = computed(() => stepConfigs.value.map(config => ({
  id: config.id,
  title: config.title,
  description: config.description,
  component: config.component,
  isValid: stepStates.value.get(config.id)?.isValid || false,
  isComplete: stepStates.value.get(config.id)?.isComplete || false
})))

// Step states using the validation system
const stepStates = ref<Map<string, WizardStepState>>(new Map())

// Recovery system
const recoveryManager = new WizardRecoveryManager({
  maxAge: 24 * 60 * 60 * 1000, // 24 hours
  enableSessionRecovery: true,
  enableCrossTabRecovery: false
})

const autoSaveManager = new WizardAutoSaveManager(recoveryManager, 30000) // 30 seconds

const recoveryDialog = ref<WizardRecoveryDialogData>({
  show: false,
  summary: null,
  loading: false
})

// Initialize step states
const initializeStepStates = () => {
  stepConfigs.value.forEach(config => {
    stepStates.value.set(config.id, {
      id: config.id,
      isValid: false,
      isComplete: false,
      isDirty: false,
      data: {},
      validation: { isValid: false, errors: [], warnings: [] },
      lastValidated: null
    })
  })
}

// Wizard state
const wizardState = ref<WizardState>({
  currentStep: currentStep.value,
  totalSteps: steps.value.length,
  canProceed: false,
  canGoBack: false,
  selectedArticles: [],
  analysisConfig: null,
  currentJob: null,
  results: [],
  stepData: {}
})

// Computed properties
const totalSteps = computed(() => steps.value.length)

const currentStepData = computed(() => steps.value[currentStep.value - 1])

const currentStepComponent = computed(() => currentStepData.value?.component)

const currentStepProps = computed(() => ({
  wizardState: wizardState.value,
  stepData: wizardState.value.stepData[currentStepData.value?.id] || {},
  isActive: true
}))

const canGoBack = computed(() => {
  return currentStep.value > 1 && !isProcessing.value
})

const canProceed = computed(() => {
  const step = currentStepData.value
  return step?.isValid && !isProcessing.value
})

const isLastStep = computed(() => currentStep.value === totalSteps.value)

const nextButtonLabel = computed(() => {
  if (currentStep.value === 1) return 'Start Analysis'
  if (currentStep.value === 2) return 'View Results'
  return 'Next'
})

const nextButtonTooltip = computed(() => {
  if (!canProceed.value) {
    return 'Complete the current step to proceed'
  }
  return `Proceed to ${steps.value[currentStep.value]?.title || 'next step'}`
})

// Provide wizard context to child components
provide('wizardContext', {
  state: wizardState,
  updateStepData: (stepId: string, data: any) => {
    wizardState.value.stepData[stepId] = { ...wizardState.value.stepData[stepId], ...data }
    saveWizardState()
  },
  getStepData: (stepId: string) => wizardState.value.stepData[stepId] || {},
  goToStep: (step: number) => goToStep(step),
  completeStep: (stepId: string) => {
    const stepIndex = steps.value.findIndex(s => s.id === stepId)
    if (stepIndex !== -1) {
      steps.value[stepIndex].isComplete = true
      steps.value[stepIndex].isValid = true
    }
  }
})

// Methods
const goToStep = (step: number) => {
  if (step < 1 || step > totalSteps.value) return
  
  const direction = step > currentStep.value ? 'slide-left' : 'slide-right'
  transitionName.value = direction
  
  currentStep.value = step
  wizardState.value.currentStep = step
  wizardState.value.canGoBack = canGoBack.value
  wizardState.value.canProceed = canProceed.value
  
  emit('step-changed', step)
  saveWizardState()
}

const goToPreviousStep = () => {
  if (canGoBack.value) {
    goToStep(currentStep.value - 1)
  }
}

const goToNextStep = async () => {
  if (!canProceed.value) return
  
  isProcessing.value = true
  
  try {
    // Validate current step before proceeding
    const currentStepId = currentStepData.value.id
    const isValid = await validateStep(currentStepId)
    
    if (isValid) {
      // Mark current step as complete
      steps.value[currentStep.value - 1].isComplete = true
      
      // Move to next step
      goToStep(currentStep.value + 1)
    }
  } catch (error) {
    console.error('Error proceeding to next step:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to proceed to next step',
      life: 5000
    })
  } finally {
    isProcessing.value = false
  }
}

const validateStep = async (stepId: string): Promise<boolean> => {
  const stepConfig = stepConfigs.value.find(s => s.id === stepId)
  const stepState = stepStates.value.get(stepId)
  
  if (!stepConfig || !stepState) return false
  
  try {
    // Run step validation
    const validation = await stepConfig.validation.validate(stepState.data)
    
    // Update step state with validation results
    stepState.validation = validation
    stepState.isValid = validation.isValid
    stepState.lastValidated = new Date()
    
    // Check navigation guard
    if (stepConfig.navigationGuard) {
      const canLeave = await stepConfig.navigationGuard.canLeave(stepState.data)
      if (!canLeave) {
        return false
      }
    }
    
    // Check dependencies
    const dependenciesMet = WizardValidationUtils.checkStepDependencies(
      stepId, 
      stepConfigs.value.map(config => ({
        id: config.id,
        dependencies: config.dependencies || []
      })), 
      stepStates.value
    )
    
    return validation.isValid && dependenciesMet
  } catch (error) {
    console.error(`Error validating step ${stepId}:`, error)
    return false
  }
}

const cancelWizard = () => {
  confirm.require({
    message: 'Are you sure you want to cancel the analysis wizard? Any unsaved progress will be lost.',
    header: 'Cancel Wizard',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-danger',
    accept: () => {
      clearWizardState()
      emit('wizard-cancelled')
      router.push('/predictions/analysis')
    }
  })
}

const completeWizard = async () => {
  if (!canProceed.value) return
  
  isProcessing.value = true
  
  try {
    // Finalize wizard data
    const wizardData = {
      selectedArticles: wizardState.value.selectedArticles,
      analysisJob: wizardState.value.currentJob,
      results: wizardState.value.results,
      completedAt: new Date().toISOString()
    }
    
    emit('wizard-complete', wizardData)
    
    toast.add({
      severity: 'success',
      summary: 'Analysis Complete',
      detail: 'Prediction analysis wizard completed successfully',
      life: 3000
    })
    
    // Clear wizard state after completion
    clearWizardState()
    
  } catch (error) {
    console.error('Error completing wizard:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to complete wizard',
      life: 5000
    })
  } finally {
    isProcessing.value = false
  }
}

// Event handlers
const onStepValidityChanged = (isValid: boolean) => {
  const stepId = currentStepData.value?.id
  if (stepId) {
    const stepState = stepStates.value.get(stepId)
    if (stepState) {
      stepState.isValid = isValid
      stepState.validation.isValid = isValid
    }
    wizardState.value.canProceed = isValid
  }
}

const onStepComplete = (stepId: string) => {
  const stepState = stepStates.value.get(stepId)
  if (stepState) {
    stepState.isComplete = true
    stepState.isValid = true
  }
}

const onStepDataChanged = (data: any) => {
  const stepId = currentStepData.value?.id
  if (stepId) {
    // Update step state data
    const stepState = stepStates.value.get(stepId)
    if (stepState) {
      stepState.data = { ...stepState.data, ...data }
      stepState.isDirty = true
    }
    
    // Update wizard state based on step data
    wizardState.value.stepData[stepId] = { ...wizardState.value.stepData[stepId], ...data }
    
    if (stepId === 'article-selection' && data.selectedArticles) {
      wizardState.value.selectedArticles = data.selectedArticles
    } else if (stepId === 'analysis-execution' && data.currentJob) {
      wizardState.value.currentJob = data.currentJob
    } else if (stepId === 'analysis-results' && data.results) {
      wizardState.value.results = data.results
    }
    
    saveWizardState()
  }
}

// Transition handlers
const onBeforeEnter = () => {
  isProcessing.value = true
}

const onAfterEnter = () => {
  isProcessing.value = false
}

// State persistence (removed unused constant)

const saveWizardState = async () => {
  if (props.autoSave) {
    try {
      await recoveryManager.saveState(
        currentStep.value,
        stepStates.value,
        wizardState.value
      )
      
      // Mark auto-save manager as dirty for next interval
      autoSaveManager.markDirty()
    } catch (error) {
      console.warn('Failed to save wizard state:', error)
    }
  }
}

const loadWizardState = async (): Promise<boolean> => {
  if (props.autoSave) {
    try {
      const data = await recoveryManager.acceptRecovery()
      if (data) {
        currentStep.value = data.currentStep || 1
        wizardState.value = { ...wizardState.value, ...data.wizardState }
        stepStates.value = data.stepStates
        
        return true
      }
    } catch (error) {
      console.warn('Failed to load wizard state:', error)
    }
  }
  return false
}

const clearWizardState = () => {
  recoveryManager.clearState()
  
  // Reset to initial state
  currentStep.value = 1
  wizardState.value = {
    currentStep: 1,
    totalSteps: stepConfigs.value.length,
    canProceed: false,
    canGoBack: false,
    selectedArticles: [],
    analysisConfig: null,
    currentJob: null,
    results: [],
    stepData: {}
  }
  
  // Reset step states
  initializeStepStates()
}

// Keyboard shortcuts
const handleKeyboardShortcuts = (event: KeyboardEvent) => {
  if (!event.ctrlKey && !event.metaKey) return
  
  switch (event.key.toLowerCase()) {
    case 'arrowleft':
      event.preventDefault()
      if (canGoBack.value) {
        goToPreviousStep()
      }
      break
    case 'arrowright':
      event.preventDefault()
      if (canProceed.value) {
        goToNextStep()
      }
      break
    case 'escape':
      event.preventDefault()
      cancelWizard()
      break
  }
}

// Recovery dialog handlers
const handleContinueSession = async () => {
  try {
    const restored = await loadWizardState()
    if (restored) {
      toast.add({
        severity: 'success',
        summary: 'Session Restored',
        detail: 'Your previous analysis session has been restored',
        life: 3000
      })
    }
  } catch (error) {
    console.error('Error continuing session:', error)
    toast.add({
      severity: 'error',
      summary: 'Restore Failed',
      detail: 'Failed to restore previous session',
      life: 5000
    })
  }
}

const handleStartFresh = async () => {
  try {
    await recoveryManager.declineRecovery()
    initializeStepStates()
    
    toast.add({
      severity: 'info',
      summary: 'Fresh Start',
      detail: 'Starting a new analysis session',
      life: 3000
    })
  } catch (error) {
    console.error('Error starting fresh:', error)
  }
}

const checkForRecovery = async () => {
  try {
    recoveryDialog.value.loading = true
    const dialogData = await recoveryManager.getRecoveryDialogData()
    
    if (dialogData.show && dialogData.summary) {
      recoveryDialog.value = dialogData
    } else {
      // No recovery available, initialize fresh
      initializeStepStates()
      wizardState.value.currentStep = currentStep.value
      wizardState.value.totalSteps = totalSteps.value
    }
  } catch (error) {
    console.error('Error checking for recovery:', error)
    // Fallback to fresh initialization
    initializeStepStates()
    wizardState.value.currentStep = currentStep.value
    wizardState.value.totalSteps = totalSteps.value
  } finally {
    recoveryDialog.value.loading = false
  }
}

// Lifecycle
onMounted(async () => {
  // Initialize step states
  initializeStepStates()
  
  // Check for recovery first
  await checkForRecovery()
  
  // Start auto-save
  autoSaveManager.startAutoSave(
    () => currentStep.value,
    () => stepStates.value,
    () => wizardState.value
  )
  
  // Add keyboard shortcuts
  document.addEventListener('keydown', handleKeyboardShortcuts)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyboardShortcuts)
  
  // Stop auto-save
  autoSaveManager.stopAutoSave()
  
  // Force save current state before unmounting
  if (props.autoSave) {
    autoSaveManager.forceSave(
      () => currentStep.value,
      () => stepStates.value,
      () => wizardState.value
    ).catch(error => {
      console.warn('Failed to save state on unmount:', error)
    })
  }
})

// Watch for step changes to update state
watch(currentStep, (newStep) => {
  wizardState.value.currentStep = newStep
  wizardState.value.canGoBack = canGoBack.value
  wizardState.value.canProceed = canProceed.value
})
</script>

<style lang="scss" scoped>
.prediction-analysis-wizard {
  padding: 1.5rem;
  min-height: calc(100vh - 7rem);
  background-color: var(--surface-ground);
}

.wizard-header {
  :deep(.p-panel-content) {
    padding: 1rem 1.25rem;
  }
}

.step-indicator {
  margin-top: 1rem;
}

.steps-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
  
  &:not(:last-child) {
    margin-right: 2rem;
  }
}

.step-circle {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
  transition: all 0.3s ease;
  
  .step-disabled & {
    background-color: var(--surface-200);
    color: var(--text-color-secondary);
    border: 2px solid var(--surface-300);
  }
  
  .step-active & {
    background-color: var(--primary-color);
    color: var(--primary-color-text);
    border: 2px solid var(--primary-color);
    box-shadow: 0 0 0 4px var(--primary-50);
  }
  
  .step-completed & {
    background-color: var(--green-500);
    color: white;
    border: 2px solid var(--green-500);
  }
}

.step-content {
  text-align: center;
  max-width: 120px;
}

.step-title {
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
  
  .step-disabled & {
    color: var(--text-color-secondary);
  }
  
  .step-active & {
    color: var(--primary-color);
  }
  
  .step-completed & {
    color: var(--text-color);
  }
}

.step-description {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
  line-height: 1.2;
}

.step-connector {
  position: absolute;
  top: 1.25rem;
  left: calc(50% + 1.25rem);
  right: calc(-50% + 1.25rem);
  height: 2px;
  background-color: var(--surface-300);
  transition: background-color 0.3s ease;
  
  &.connector-completed {
    background-color: var(--green-500);
  }
}

.wizard-content {
  min-height: 500px;
  position: relative;
}

.wizard-navigation {
  :deep(.p-panel-content) {
    padding: 1rem 1.25rem;
  }
}

// Transition animations
.slide-left-enter-active,
.slide-left-leave-active,
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s ease;
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

// Responsive design
@media screen and (max-width: 768px) {
  .prediction-analysis-wizard {
    padding: 1rem;
  }
  
  .steps-container {
    flex-direction: column;
    gap: 1rem;
  }
  
  .step-item {
    flex-direction: row;
    align-items: center;
    margin-right: 0;
    
    &:not(:last-child) {
      margin-bottom: 1rem;
    }
  }
  
  .step-circle {
    margin-bottom: 0;
    margin-right: 1rem;
  }
  
  .step-content {
    text-align: left;
    max-width: none;
  }
  
  .step-connector {
    display: none;
  }
}
</style>