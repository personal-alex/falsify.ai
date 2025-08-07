<template>
  <div class="crawlers-management">
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">Crawlers Management</h1>
        <p class="page-description">Start, stop, and configure crawlers</p>
      </div>
      <!-- Header actions removed as per UI facelift requirements -->
    </div>

    <div class="content-grid">
      <!-- Crawler Status Cards -->
      <div class="crawler-cards">
        <div class="grid">
          <div class="col-12 md:col-6 lg:col-4" v-for="crawler in crawlers" :key="crawler.id">
            <CrawlerHealthCard 
              :crawler="crawler"
              :health="{ status: 'HEALTHY', lastCheck: new Date().toISOString(), message: 'OK', responseTimeMs: 100, crawlerId: crawler.id }"
              @start="startCrawler"
              @stop="stopCrawler"
              @configure="configureCrawler"
              @view-details="viewCrawlerDetails"
            />
          </div>
        </div>
      </div>

      <!-- Quick Actions Panel -->
      <!-- Card class="quick-actions-panel">
        <template #header>
          <h3>Quick Actions</h3>
        </template>
        <template #content>
          <div class="actions-grid">
            <Button 
              icon="pi pi-play" 
              label="Start All Healthy"
              @click="startAllHealthy"
              :disabled="!hasHealthyCrawlers"
              severity="success"
              class="action-button"
            />
            <Button 
              icon="pi pi-stop" 
              label="Stop All Running"
              @click="stopAllRunning"
              :disabled="!hasRunningCrawlers"
              severity="warning"
              class="action-button"
            />
            <Button 
              icon="pi pi-sync" 
              label="Sync Configuration"
              @click="syncConfiguration"
              :loading="isSyncing"
              severity="info"
              class="action-button"
            />
            <Button 
              icon="pi pi-chart-line" 
              label="View Metrics"
              @click="viewMetrics"
              severity="help"
              class="action-button"
            />
          </div>
        </template>
      </Card -->

      <!-- System Status section removed as per UI facelift requirements -->
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCrawlerStore } from '@/stores/crawler'
import { useTopBarStore } from '@/stores/topbar'
import { useToast } from 'primevue/usetoast'
import CrawlerHealthCard from '@/components/CrawlerHealthCard.vue'

const router = useRouter()
const crawlerStore = useCrawlerStore()
const topBarStore = useTopBarStore()
const toast = useToast()

// Reactive state
const isSyncing = ref(false)

// Computed properties
const crawlers = computed(() => crawlerStore.crawlers)

// Methods

const startCrawler = async (crawlerId: string) => {
  try {
    // Mock implementation - replace with actual store method
    await new Promise(resolve => setTimeout(resolve, 500))
    toast.add({
      severity: 'success',
      summary: 'Crawler Started',
      detail: `Crawler ${crawlerId} started successfully`,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Start Failed',
      detail: `Failed to start crawler ${crawlerId}`,
      life: 5000
    })
  }
}

const stopCrawler = async (crawlerId: string) => {
  try {
    // Mock implementation - replace with actual store method
    await new Promise(resolve => setTimeout(resolve, 500))
    toast.add({
      severity: 'success',
      summary: 'Crawler Stopped',
      detail: `Crawler ${crawlerId} stopped successfully`,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Stop Failed',
      detail: `Failed to stop crawler ${crawlerId}`,
      life: 5000
    })
  }
}

const configureCrawler = (crawlerId: string) => {
  // Navigate to crawler configuration
  router.push(`/crawler/${crawlerId}/config`)
}

const viewCrawlerDetails = (crawlerId: string) => {
  router.push(`/crawler/${crawlerId}`)
}

const startAllHealthy = async () => {
  const healthyCrawlers = crawlers.value.filter(c => 
    (c as any).status === 'healthy' && (c as any).state !== 'running'
  )
  
  try {
    // Mock implementation - replace with actual store method
    await Promise.all(healthyCrawlers.map(() => new Promise(resolve => setTimeout(resolve, 500))))
    toast.add({
      severity: 'success',
      summary: 'Crawlers Started',
      detail: `Started ${healthyCrawlers.length} healthy crawlers`,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Start Failed',
      detail: 'Failed to start some crawlers',
      life: 5000
    })
  }
}

const stopAllRunning = async () => {
  const runningCrawlers = crawlers.value.filter(c => (c as any).state === 'running')
  
  try {
    // Mock implementation - replace with actual store method
    await Promise.all(runningCrawlers.map(() => new Promise(resolve => setTimeout(resolve, 500))))
    toast.add({
      severity: 'success',
      summary: 'Crawlers Stopped',
      detail: `Stopped ${runningCrawlers.length} running crawlers`,
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Stop Failed',
      detail: 'Failed to stop some crawlers',
      life: 5000
    })
  }
}

const syncConfiguration = async () => {
  isSyncing.value = true
  try {
    // Mock implementation - replace with actual store method
    await new Promise(resolve => setTimeout(resolve, 1000))
    toast.add({
      severity: 'success',
      summary: 'Configuration Synced',
      detail: 'Crawler configurations synchronized',
      life: 3000
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Sync Failed',
      detail: 'Failed to sync configuration',
      life: 5000
    })
  } finally {
    isSyncing.value = false
  }
}

const viewMetrics = () => {
  router.push('/monitoring/metrics')
}

// Initialize data
onMounted(async () => {
  try {
    await crawlerStore.loadCrawlerData()
    
    // Set up topbar actions for crawler management
    topBarStore.setCrawlerManagementActions({
      onStartAllHealthy: startAllHealthy,
      onStopAllRunning: stopAllRunning,
      onSyncConfiguration: syncConfiguration,
      onViewMetrics: viewMetrics
    })
  } catch (error) {
    toast.add({
      severity: 'error',
      summary: 'Load Failed',
      detail: 'Failed to load crawler data',
      life: 5000
    })
  }
})

// Clean up topbar actions when leaving the view
onUnmounted(() => {
  topBarStore.clearContextActions()
})
</script>

<style lang="scss" scoped>
.crawlers-management {
  padding: 2rem;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 2rem;

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
}

.content-grid {
  display: grid;
  gap: 2rem;
  grid-template-columns: 1fr;
}

.crawler-cards {
  .grid {
    margin: 0;
  }
}

.quick-actions-panel {
  :deep(.p-card-header) {
    padding-bottom: 1rem;
    border-bottom: 1px solid var(--surface-border);
  }

  :deep(.p-card-content) {
    padding-top: 1.5rem;
  }
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;

  .action-button {
    width: 100%;
    justify-content: flex-start;
    padding: 1rem;
    height: auto;

    :deep(.p-button-label) {
      flex: 1;
      text-align: left;
    }
  }
}

// Status metrics styles removed as System Status section was removed

// Responsive design
@media (max-width: 768px) {
  .crawlers-management {
    padding: 1rem;
  }

  .page-header {
    // Header actions responsive styles removed
  }

  .actions-grid {
    grid-template-columns: 1fr;
  }

  // Status metrics responsive styles removed
}

// Mobile status metrics styles removed as System Status section was removed
</style>