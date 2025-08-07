<template>
  <Dialog
    v-model:visible="visible"
    modal
    :closable="false"
    :draggable="false"
    class="wizard-recovery-dialog"
    style="width: 500px"
  >
    <template #header>
      <div class="flex align-items-center">
        <i class="pi pi-history mr-2 text-primary"></i>
        <span class="font-semibold">Restore Previous Session</span>
      </div>
    </template>
    
    <div class="recovery-content">
      <div v-if="loading" class="text-center p-4">
        <ProgressSpinner size="50" />
        <div class="mt-3 text-600">Checking for previous session...</div>
      </div>
      
      <div v-else-if="summary" class="recovery-info">
        <div class="mb-4">
          <div class="text-900 font-medium mb-2">
            We found a previous analysis session that was interrupted.
          </div>
          <div class="text-600">
            Would you like to continue where you left off?
          </div>
        </div>
        
        <!-- Session Details -->
        <div class="session-details surface-card p-3 border-round mb-4">
          <div class="grid">
            <div class="col-12 md:col-6">
              <div class="detail-item mb-3">
                <div class="detail-label text-600 text-sm">Last Step</div>
                <div class="detail-value font-medium">{{ summary.stepName }}</div>
              </div>
            </div>
            <div class="col-12 md:col-6">
              <div class="detail-item mb-3">
                <div class="detail-label text-600 text-sm">Last Saved</div>
                <div class="detail-value font-medium">{{ formatLastSaved }}</div>
              </div>
            </div>
            <div class="col-12 md:col-6">
              <div class="detail-item">
                <div class="detail-label text-600 text-sm">Data Size</div>
                <div class="detail-value font-medium">{{ summary.dataSize }}</div>
              </div>
            </div>
            <div class="col-12 md:col-6">
              <div class="detail-item">
                <div class="detail-label text-600 text-sm">Status</div>
                <div class="detail-value">
                  <Badge 
                    :value="summary.isExpired ? 'Expired' : 'Valid'" 
                    :severity="summary.isExpired ? 'danger' : 'success'"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Warning for expired sessions -->
        <div v-if="summary.isExpired" class="expired-warning mb-4">
          <Message severity="warn" :closable="false">
            <div class="flex align-items-center">
              <i class="pi pi-exclamation-triangle mr-2"></i>
              <div>
                <div class="font-medium">Session Expired</div>
                <div class="text-sm">
                  This session is older than 24 hours and may contain outdated data.
                </div>
              </div>
            </div>
          </Message>
        </div>
        
        <!-- Recovery Options -->
        <div class="recovery-options">
          <div class="text-900 font-medium mb-3">What would you like to do?</div>
          
          <div class="option-cards">
            <div 
              class="option-card p-3 border-round cursor-pointer"
              :class="{ 'option-selected': selectedOption === 'restore' }"
              @click="selectedOption = 'restore'"
            >
              <div class="flex align-items-start">
                <RadioButton 
                  v-model="selectedOption" 
                  value="restore" 
                  class="mr-3 mt-1"
                />
                <div class="flex-1">
                  <div class="font-medium text-900 mb-1">Continue Previous Session</div>
                  <div class="text-600 text-sm">
                    Restore your progress and continue from where you left off.
                  </div>
                </div>
              </div>
            </div>
            
            <div 
              class="option-card p-3 border-round cursor-pointer mt-2"
              :class="{ 'option-selected': selectedOption === 'fresh' }"
              @click="selectedOption = 'fresh'"
            >
              <div class="flex align-items-start">
                <RadioButton 
                  v-model="selectedOption" 
                  value="fresh" 
                  class="mr-3 mt-1"
                />
                <div class="flex-1">
                  <div class="font-medium text-900 mb-1">Start Fresh</div>
                  <div class="text-600 text-sm">
                    Begin a new analysis session and discard the previous one.
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <template #footer>
      <div class="flex align-items-center justify-content-between w-full">
        <div class="recovery-tip text-600 text-sm">
          <i class="pi pi-info-circle mr-1"></i>
          Your progress is automatically saved every 30 seconds
        </div>
        
        <div class="flex align-items-center gap-2">
          <Button 
            label="Start Fresh"
            severity="secondary"
            outlined
            :disabled="loading || processingChoice"
            @click="handleStartFresh"
          />
          
          <Button 
            label="Continue Session"
            :disabled="loading || processingChoice || selectedOption !== 'restore'"
            :loading="processingChoice && selectedOption === 'restore'"
            @click="handleContinueSession"
          />
        </div>
      </div>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import RadioButton from 'primevue/radiobutton'
import Badge from 'primevue/badge'
import Message from 'primevue/message'
import ProgressSpinner from 'primevue/progressspinner'

// Props
interface Props {
  visible: boolean
  summary: {
    stepName: string
    lastSaved: Date
    dataSize: string
    isExpired: boolean
  } | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

// Emits
const emit = defineEmits<{
  'update:visible': [visible: boolean]
  'continue-session': []
  'start-fresh': []
}>()

// Reactive state
const selectedOption = ref<'restore' | 'fresh'>('restore')
const processingChoice = ref(false)

// Computed
const formatLastSaved = computed(() => {
  if (!props.summary?.lastSaved) return 'Unknown'
  
  const now = new Date()
  const saved = props.summary.lastSaved
  const diffMs = now.getTime() - saved.getTime()
  const diffMinutes = Math.floor(diffMs / (1000 * 60))
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60))
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24))
  
  if (diffMinutes < 1) {
    return 'Just now'
  } else if (diffMinutes < 60) {
    return `${diffMinutes} minute${diffMinutes > 1 ? 's' : ''} ago`
  } else if (diffHours < 24) {
    return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
  } else {
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
  }
})

// Methods
const handleContinueSession = async () => {
  processingChoice.value = true
  
  try {
    emit('continue-session')
    emit('update:visible', false)
  } catch (error) {
    console.error('Error continuing session:', error)
  } finally {
    processingChoice.value = false
  }
}

const handleStartFresh = async () => {
  processingChoice.value = true
  
  try {
    emit('start-fresh')
    emit('update:visible', false)
  } catch (error) {
    console.error('Error starting fresh:', error)
  } finally {
    processingChoice.value = false
  }
}

// Watch for expired sessions to auto-select fresh start
watch(() => props.summary?.isExpired, (isExpired) => {
  if (isExpired) {
    selectedOption.value = 'fresh'
  }
})
</script>

<style lang="scss" scoped>
.wizard-recovery-dialog {
  :deep(.p-dialog-header) {
    background: var(--surface-card);
    border-bottom: 1px solid var(--surface-border);
  }
  
  :deep(.p-dialog-content) {
    padding: 1.5rem;
  }
  
  :deep(.p-dialog-footer) {
    background: var(--surface-card);
    border-top: 1px solid var(--surface-border);
    padding: 1rem 1.5rem;
  }
}

.recovery-content {
  min-height: 200px;
}

.session-details {
  background: var(--surface-ground);
  border: 1px solid var(--surface-border);
}

.detail-item {
  .detail-label {
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 0.25rem;
  }
  
  .detail-value {
    font-size: 0.875rem;
  }
}

.expired-warning {
  :deep(.p-message) {
    background: var(--yellow-50);
    border: 1px solid var(--yellow-200);
    color: var(--yellow-800);
  }
}

.option-cards {
  .option-card {
    border: 2px solid var(--surface-border);
    transition: all 0.3s ease;
    
    &:hover {
      border-color: var(--primary-200);
      background: var(--primary-50);
    }
    
    &.option-selected {
      border-color: var(--primary-color);
      background: var(--primary-50);
    }
  }
}

.recovery-tip {
  display: flex;
  align-items: center;
  font-style: italic;
}

// Responsive design
@media screen and (max-width: 768px) {
  .wizard-recovery-dialog {
    :deep(.p-dialog) {
      width: 95vw !important;
      margin: 1rem;
    }
  }
  
  .session-details {
    .grid {
      .col-12 {
        padding: 0.5rem;
      }
    }
  }
  
  .recovery-tip {
    display: none; // Hide on mobile to save space
  }
}
</style>