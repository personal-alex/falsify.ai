<template>
  <div class="predictions-history">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">Predictions History</h1>
        <p class="page-description">View prediction analysis history and results</p>
      </div>
      <div class="header-actions">
        <Button 
          icon="pi pi-search" 
          label="New Analysis"
          @click="startNewAnalysis"
          severity="primary"
        />
      </div>
    </div>

    <!-- Analysis History Component -->
    <AnalysisHistory 
      :history="analysisJobs"
      :auto-refresh="true"
      :refresh-interval="30000"
      :page-size="20"
      @view-results="viewResults"
      @refresh="refreshHistory"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Button from 'primevue/button'
import AnalysisHistory from '@/components/AnalysisHistory.vue'

const router = useRouter()
const toast = useToast()

// Reactive state
const analysisJobs = ref<any[]>([])

// Methods
const startNewAnalysis = () => {
  router.push('/predictions/analysis')
}

const viewResults = (job: any) => {
  // Emit event to view results or navigate to results page
  console.log('Viewing results for job:', job.jobId)
  // Could navigate to a dedicated results page or show modal
}

const refreshHistory = async () => {
  try {
    // Mock API call - replace with actual implementation
    await loadAnalysisHistory()
    toast.add({
      severity: 'success',
      summary: 'Success',
      detail: 'Analysis history refreshed successfully',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to refresh analysis history',
      life: 5000
    })
  }
}

const loadAnalysisHistory = async () => {
  // Mock data - replace with actual API call
  analysisJobs.value = Array.from({ length: 25 }, (_, i) => {
    const statuses = ['COMPLETED', 'RUNNING', 'FAILED', 'CANCELLED', 'PENDING']
    const status = statuses[Math.floor(Math.random() * statuses.length)]
    const startedAt = new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString()
    const completedAt = status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED' 
      ? new Date(new Date(startedAt).getTime() + Math.random() * 60 * 60 * 1000).toISOString()
      : null

    return {
      id: i + 1,
      jobId: `job-${Date.now()}-${i}`,
      status,
      startedAt,
      completedAt,
      totalArticles: Math.floor(Math.random() * 100) + 10,
      processedArticles: status === 'RUNNING' ? Math.floor(Math.random() * 50) : 
                        status === 'COMPLETED' ? Math.floor(Math.random() * 100) + 10 : 0,
      predictionsFound: status === 'COMPLETED' ? Math.floor(Math.random() * 50) : 0,
      analysisType: Math.random() > 0.5 ? 'mock' : 'llm'
    }
  })
}

// Initialize data
onMounted(async () => {
  await loadAnalysisHistory()
})
</script>

<style lang="scss" scoped>
.predictions-history {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 2rem;
  gap: 2rem;

  .header-content {
    flex: 1;
  }

  .page-title {
    font-size: 2rem;
    font-weight: 600;
    color: var(--text-color);
    margin: 0 0 0.5rem 0;
  }

  .page-description {
    color: var(--text-color-secondary);
    margin: 0;
    font-size: 1.1rem;
  }

  .header-actions {
    display: flex;
    gap: 1rem;
    flex-shrink: 0;
  }
}

// Responsive design
@media (max-width: 768px) {
  .predictions-history {
    padding: 1rem;
  }

  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 1rem;

    .header-actions {
      justify-content: stretch;

      :deep(.p-button) {
        flex: 1;
      }
    }
  }
}
</style>