<template>
  <div class="system-status-panel">
    <!-- System Overview Card -->
    <Card class="mb-4">
      <template #title>
        <div class="flex align-items-center">
          <i class="pi pi-cog mr-2 text-primary"></i>
          <span>System Status</span>
        </div>
      </template>
      
      <template #content>
        <div class="grid">
          <div class="col-12 md:col-6 lg:col-3">
            <div class="surface-card p-3 border-round">
              <div class="flex justify-content-between align-items-start">
                <div>
                  <span class="block text-500 font-medium mb-1">Running Jobs</span>
                  <div class="text-900 font-medium text-xl">{{ systemStatus?.runningJobs || 0 }}</div>
                </div>
                <div class="flex align-items-center justify-content-center bg-blue-100 border-round" style="width:2.5rem;height:2.5rem">
                  <i class="pi pi-play text-blue-500 text-xl"></i>
                </div>
              </div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-3">
            <div class="surface-card p-3 border-round">
              <div class="flex justify-content-between align-items-start">
                <div>
                  <span class="block text-500 font-medium mb-1">Available Slots</span>
                  <div class="text-900 font-medium text-xl">{{ systemStatus?.availableSlots || 0 }}</div>
                </div>
                <div class="flex align-items-center justify-content-center bg-green-100 border-round" style="width:2.5rem;height:2.5rem">
                  <i class="pi pi-check-circle text-green-500 text-xl"></i>
                </div>
              </div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-3">
            <div class="surface-card p-3 border-round">
              <div class="flex justify-content-between align-items-start">
                <div>
                  <span class="block text-500 font-medium mb-1">Max Concurrent</span>
                  <div class="text-900 font-medium text-xl">{{ systemStatus?.maxConcurrentJobs || 0 }}</div>
                </div>
                <div class="flex align-items-center justify-content-center bg-orange-100 border-round" style="width:2.5rem;height:2.5rem">
                  <i class="pi pi-server text-orange-500 text-xl"></i>
                </div>
              </div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-3">
            <div class="surface-card p-3 border-round">
              <div class="flex justify-content-between align-items-start">
                <div>
                  <span class="block text-500 font-medium mb-1">System Load</span>
                  <div class="text-900 font-medium text-xl">
                    {{ Math.round(((systemStatus?.runningJobs || 0) / (systemStatus?.maxConcurrentJobs || 1)) * 100) }}%
                  </div>
                </div>
                <div class="flex align-items-center justify-content-center bg-purple-100 border-round" style="width:2.5rem;height:2.5rem">
                  <i class="pi pi-chart-bar text-purple-500 text-xl"></i>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </Card>

    <!-- Extractors Status Card -->
    <Card class="mb-4">
      <template #title>
        <div class="flex align-items-center justify-content-between">
          <div class="flex align-items-center">
            <i class="pi pi-microchip mr-2 text-primary"></i>
            <span>Prediction Extractors</span>
          </div>
          <Button 
            icon="pi pi-refresh" 
            size="small" 
            text 
            @click="refreshStatus"
            :loading="loading"
            v-tooltip.bottom="'Refresh extractor status'"
          />
        </div>
      </template>
      
      <template #content>
        <div class="grid">
          <!-- Primary Extractor -->
          <div class="col-12 lg:col-6">
            <div class="surface-card p-4 border-round border-left-3" 
                 :class="extractorStatus?.primaryExtractor?.available ? 'border-green-500' : 'border-red-500'">
              <div class="flex align-items-center justify-content-between mb-3">
                <div class="flex align-items-center">
                  <i class="pi pi-star-fill text-yellow-500 mr-2"></i>
                  <span class="font-semibold">Primary Extractor</span>
                </div>
                <Tag 
                  :value="extractorStatus?.primaryExtractor?.available ? 'Available' : 'Unavailable'"
                  :severity="extractorStatus?.primaryExtractor?.available ? 'success' : 'danger'"
                />
              </div>
              
              <div class="mb-2">
                <span class="text-500">Type:</span>
                <span class="ml-2 font-medium">{{ extractorStatus?.primaryExtractor?.type || 'Unknown' }}</span>
              </div>
              
              <div class="text-sm text-600">
                {{ extractorStatus?.primaryExtractor?.configuration || 'No configuration available' }}
              </div>
            </div>
          </div>
          
          <!-- Best Available Extractor -->
          <div class="col-12 lg:col-6">
            <div class="surface-card p-4 border-round border-left-3" 
                 :class="extractorStatus?.bestAvailableExtractor?.available ? 'border-blue-500' : 'border-orange-500'">
              <div class="flex align-items-center justify-content-between mb-3">
                <div class="flex align-items-center">
                  <i class="pi pi-thumbs-up text-blue-500 mr-2"></i>
                  <span class="font-semibold">Best Available</span>
                </div>
                <Tag 
                  :value="extractorStatus?.bestAvailableExtractor?.available ? 'Available' : 'Fallback'"
                  :severity="extractorStatus?.bestAvailableExtractor?.available ? 'info' : 'warning'"
                />
              </div>
              
              <div class="mb-2">
                <span class="text-500">Type:</span>
                <span class="ml-2 font-medium">{{ extractorStatus?.bestAvailableExtractor?.type || 'Unknown' }}</span>
              </div>
              
              <div class="text-sm text-600">
                {{ extractorStatus?.bestAvailableExtractor?.configuration || 'No configuration available' }}
              </div>
            </div>
          </div>
        </div>
        
        <!-- All Extractors Table -->
        <div class="mt-4">
          <h5>All Extractors</h5>
          <DataTable 
            :value="extractorList" 
            :loading="loading"
            responsiveLayout="scroll"
            class="p-datatable-sm"
          >
            <Column field="name" header="Name" style="min-width: 120px">
              <template #body="{ data }">
                <div class="flex align-items-center">
                  <i :class="getExtractorIcon(data.name)" class="mr-2"></i>
                  <span class="font-medium">{{ data.name }}</span>
                </div>
              </template>
            </Column>
            
            <Column field="available" header="Status" style="min-width: 100px">
              <template #body="{ data }">
                <Tag 
                  :value="data.available ? 'Available' : 'Unavailable'"
                  :severity="data.available ? 'success' : 'danger'"
                />
              </template>
            </Column>
            
            <Column field="configuration" header="Configuration" style="min-width: 300px">
              <template #body="{ data }">
                <div class="text-sm">{{ data.configuration }}</div>
              </template>
            </Column>
          </DataTable>
        </div>
      </template>
    </Card>

    <!-- Job Statistics Card -->
    <Card>
      <template #title>
        <div class="flex align-items-center">
          <i class="pi pi-chart-line mr-2 text-primary"></i>
          <span>Job Statistics</span>
        </div>
      </template>
      
      <template #content>
        <div class="grid">
          <div class="col-12 md:col-6 lg:col-2">
            <div class="text-center">
              <div class="text-900 font-medium text-2xl mb-2">{{ statistics?.jobs?.total || 0 }}</div>
              <div class="text-500">Total Jobs</div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-2">
            <div class="text-center">
              <div class="text-green-500 font-medium text-2xl mb-2">{{ statistics?.jobs?.completed || 0 }}</div>
              <div class="text-500">Completed</div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-2">
            <div class="text-center">
              <div class="text-red-500 font-medium text-2xl mb-2">{{ statistics?.jobs?.failed || 0 }}</div>
              <div class="text-500">Failed</div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-2">
            <div class="text-center">
              <div class="text-blue-500 font-medium text-2xl mb-2">{{ statistics?.jobs?.running || 0 }}</div>
              <div class="text-500">Running</div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-2">
            <div class="text-center">
              <div class="text-orange-500 font-medium text-2xl mb-2">{{ statistics?.jobs?.pending || 0 }}</div>
              <div class="text-500">Pending</div>
            </div>
          </div>
          
          <div class="col-12 md:col-6 lg:col-2">
            <div class="text-center">
              <div class="text-purple-500 font-medium text-2xl mb-2">
                {{ Math.round((statistics?.jobs?.successRate || 0) * 100) }}%
              </div>
              <div class="text-500">Success Rate</div>
            </div>
          </div>
        </div>
        
        <!-- Performance Metrics -->
        <Divider />
        
        <div class="grid">
          <div class="col-12 md:col-4">
            <div class="text-center">
              <div class="text-900 font-medium text-xl mb-2">
                {{ Math.round(statistics?.performance?.averageProcessingTimeMs || 0) }}ms
              </div>
              <div class="text-500">Avg Processing Time</div>
            </div>
          </div>
          
          <div class="col-12 md:col-4">
            <div class="text-center">
              <div class="text-900 font-medium text-xl mb-2">
                {{ statistics?.performance?.totalPredictionsFound || 0 }}
              </div>
              <div class="text-500">Total Predictions</div>
            </div>
          </div>
          
          <div class="col-12 md:col-4">
            <div class="text-center">
              <div class="text-900 font-medium text-xl mb-2">
                {{ Math.round((statistics?.performance?.averagePredictionsPerJob || 0) * 10) / 10 }}
              </div>
              <div class="text-500">Avg Predictions/Job</div>
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useToast } from 'primevue/usetoast'
import { ApiService, ApiServiceError } from '@/services/api'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Divider from 'primevue/divider'

const toast = useToast()

// Reactive state
const loading = ref(false)
const systemStatus = ref<any>(null)
const extractorStatus = ref<any>(null)
const statistics = ref<any>(null)

// Computed properties
const extractorList = computed(() => {
  if (!extractorStatus.value?.extractors) return []
  
  return Object.entries(extractorStatus.value.extractors).map(([name, config]: [string, any]) => ({
    name: name.charAt(0).toUpperCase() + name.slice(1),
    available: config.available,
    configuration: config.configuration
  }))
})

// Methods
const getExtractorIcon = (name: string) => {
  switch (name.toLowerCase()) {
    case 'mock':
      return 'pi pi-code text-gray-500'
    case 'gemini':
      return 'pi pi-google text-blue-500'
    case 'llm':
      return 'pi pi-brain text-purple-500'
    default:
      return 'pi pi-microchip text-gray-500'
  }
}

const loadSystemStatus = async () => {
  try {
    const response = await ApiService.getAnalysisStatus()
    systemStatus.value = response.system
    extractorStatus.value = response.extractors
  } catch (error) {
    console.error('Error loading system status:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to load system status'
    
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
      life: 5000
    })
  }
}

const loadStatistics = async () => {
  try {
    const response = await ApiService.getJobStatistics()
    statistics.value = response
  } catch (error) {
    console.error('Error loading statistics:', error)
    const errorMessage = error instanceof ApiServiceError 
      ? error.message 
      : 'Failed to load statistics'
    
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: errorMessage,
      life: 5000
    })
  }
}

const refreshStatus = async () => {
  loading.value = true
  
  try {
    await Promise.all([
      loadSystemStatus(),
      loadStatistics()
    ])
    
    toast.add({
      severity: 'success',
      summary: 'Refreshed',
      detail: 'System status updated successfully',
      life: 3000
    })
  } catch (error) {
    // Error handling is done in individual load methods
  } finally {
    loading.value = false
  }
}

// Initialize
onMounted(async () => {
  loading.value = true
  
  try {
    await Promise.all([
      loadSystemStatus(),
      loadStatistics()
    ])
  } catch (error) {
    // Error handling is done in individual load methods
  } finally {
    loading.value = false
  }
})
</script>

<style lang="scss" scoped>
.system-status-panel {
  .surface-card {
    transition: all 0.2s;
    
    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 25px 0 rgba(0, 0, 0, 0.1);
    }
  }
}

:deep(.p-card .p-card-title) {
  margin-bottom: 1rem;
}

:deep(.p-datatable) {
  .p-datatable-thead > tr > th {
    background-color: var(--surface-section);
    border-bottom: 1px solid var(--surface-border);
  }
}
</style>