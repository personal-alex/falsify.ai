<template>
  <div class="crawler-health-card">
    <Card class="h-full crawler-card" :class="cardStatusClass">
      <template #header>
        <div class="card-header">
          <div class="flex align-items-center">
            <div 
              class="health-indicator mr-3"
              :class="healthIndicatorClass"
            >
              <i :class="healthIconClass"></i>
            </div>
            <div class="flex-1">
              <h6 class="m-0 text-900 font-semibold text-lg">{{ crawler.name }}</h6>
              <p class="m-0 text-600 text-sm">{{ crawler.id }}</p>
            </div>
          </div>
          <div class="flex align-items-center card-actions">
            <Button
              icon="pi pi-refresh"
              text
              rounded
              size="small"
              :loading="isRefreshing"
              @click="forceHealthCheck"
              v-tooltip.top="'Force health check'"
              class="action-button"
            />
            <Button
              icon="pi pi-external-link"
              text
              rounded
              size="small"
              @click="$router.push(`/crawler/${crawler.id}`)"
              v-tooltip.top="'View details'"
              data-testid="detail-button"
              class="action-button ml-1"
            />
          </div>
        </div>
      </template>
      
      <template #content>
        <div class="card-content">
          <!-- Status and Response Time Row -->
          <div class="grid mb-3">
            <div class="col-6">
              <div class="metric-item">
                <div class="metric-label">Status</div>
                <Tag 
                  :value="health.status" 
                  :severity="statusSeverity"
                  class="metric-tag"
                />
              </div>
            </div>
            <div class="col-6">
              <div class="metric-item">
                <div class="metric-label">Response Time</div>
                <div class="metric-value" :class="responseTimeClass">
                  {{ responseTimeDisplay }}
                </div>
              </div>
            </div>
          </div>

          <!-- Last Check -->
          <div class="metric-item mb-3">
            <div class="metric-label">Last Check</div>
            <div class="metric-value">{{ lastCheckDisplay }}</div>
          </div>

          <!-- Message (if exists) -->
          <div v-if="health.message" class="metric-item mb-3">
            <div class="metric-label">Message</div>
            <div class="metric-value text-sm" :class="messageClass">{{ health.message }}</div>
          </div>
          
          <!-- Endpoint -->
          <div class="endpoint-section">
            <div class="metric-label mb-2">Endpoint</div>
            <div class="flex align-items-center endpoint-container">
              <span class="endpoint-url flex-1">{{ crawler.baseUrl }}</span>
              <Button
                icon="pi pi-copy"
                text
                rounded
                size="small"
                @click="copyToClipboard(crawler.baseUrl)"
                v-tooltip.top="'Copy URL'"
                class="copy-button"
              />
            </div>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { CrawlerConfiguration, HealthStatus } from '@/types/health'
import { ApiService } from '@/services/api'
import { useNotifications } from '@/composables/useNotifications'

interface Props {
  crawler: CrawlerConfiguration
  health: HealthStatus
}

const props = defineProps<Props>()
const emit = defineEmits<{
  healthUpdated: [health: HealthStatus]
}>()

const { showToast, handleApiError } = useNotifications()
const isRefreshing = ref(false)

const healthIndicatorClass = computed(() => {
  switch (props.health.status) {
    case 'HEALTHY':
      return 'health-healthy'
    case 'UNHEALTHY':
      return 'health-unhealthy'
    default:
      return 'health-unknown'
  }
})

const healthIconClass = computed(() => {
  switch (props.health.status) {
    case 'HEALTHY':
      return 'pi pi-check-circle'
    case 'UNHEALTHY':
      return 'pi pi-times-circle'
    default:
      return 'pi pi-question-circle'
  }
})

const statusSeverity = computed(() => {
  switch (props.health.status) {
    case 'HEALTHY':
      return 'success'
    case 'UNHEALTHY':
      return 'danger'
    default:
      return 'warning'
  }
})

const cardStatusClass = computed(() => {
  switch (props.health.status) {
    case 'HEALTHY':
      return 'card-healthy'
    case 'UNHEALTHY':
      return 'card-unhealthy'
    default:
      return 'card-unknown'
  }
})

const responseTimeClass = computed(() => {
  if (props.health.responseTimeMs === null) return 'text-600'
  if (props.health.responseTimeMs < 500) return 'text-green-600'
  if (props.health.responseTimeMs < 1000) return 'text-orange-600'
  return 'text-red-600'
})

const messageClass = computed(() => {
  switch (props.health.status) {
    case 'HEALTHY':
      return 'text-green-700'
    case 'UNHEALTHY':
      return 'text-red-700'
    default:
      return 'text-orange-700'
  }
})

const responseTimeDisplay = computed(() => {
  if (props.health.responseTimeMs === null) {
    return 'N/A'
  }
  return `${props.health.responseTimeMs}ms`
})

const lastCheckDisplay = computed(() => {
  if (!props.health.lastCheck) {
    return 'Never'
  }
  
  const date = new Date(props.health.lastCheck)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffSeconds = Math.floor(diffMs / 1000)
  const diffMinutes = Math.floor(diffSeconds / 60)
  
  if (diffSeconds < 60) {
    return `${diffSeconds}s ago`
  } else if (diffMinutes < 60) {
    return `${diffMinutes}m ago`
  } else {
    return date.toLocaleString()
  }
})

const forceHealthCheck = async () => {
  isRefreshing.value = true
  try {
    const updatedHealth = await ApiService.forceHealthCheck(props.crawler.id)
    emit('healthUpdated', updatedHealth)
    showToast.success(
      'Health Check',
      `Forced health check for ${props.crawler.name}`
    )
  } catch (error) {
    console.error('Failed to force health check:', error)
    handleApiError(error, 'Health check')
  } finally {
    isRefreshing.value = false
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    showToast.success(
      'Copied',
      'URL copied to clipboard',
      { life: 2000 }
    )
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
    showToast.error(
      'Copy Failed',
      'Failed to copy URL to clipboard'
    )
  }
}
</script>

<style scoped>
.crawler-health-card {
  height: 100%;
}

.crawler-card {
  height: 100%;
  transition: all 0.3s ease;
  border: 1px solid var(--surface-border);
  position: relative;
  overflow: hidden;
}

.crawler-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px 0 rgba(0, 0, 0, 0.15);
}

/* Card status indicators */
.card-healthy {
  border-left: 4px solid var(--green-500);
}

.card-unhealthy {
  border-left: 4px solid var(--red-500);
}

.card-unknown {
  border-left: 4px solid var(--orange-500);
}

/* Header styling */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.25rem 0.75rem 1.25rem;
}

.card-actions {
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.crawler-card:hover .card-actions {
  opacity: 1;
}

.action-button {
  width: 2rem;
  height: 2rem;
}

/* Health indicator */
.health-indicator {
  width: 3rem;
  height: 3rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.25rem;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.health-healthy {
  background: linear-gradient(135deg, var(--green-500), var(--green-600));
}

.health-unhealthy {
  background: linear-gradient(135deg, var(--red-500), var(--red-600));
}

.health-unknown {
  background: linear-gradient(135deg, var(--orange-500), var(--orange-600));
}

/* Content styling */
.card-content {
  padding: 0 1.25rem 1.25rem 1.25rem;
}

.metric-item {
  margin-bottom: 0.5rem;
}

.metric-label {
  font-size: 0.875rem;
  color: var(--text-color-secondary);
  font-weight: 500;
  margin-bottom: 0.25rem;
}

.metric-value {
  font-size: 0.95rem;
  color: var(--text-color);
  font-weight: 600;
}

.metric-tag {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 0.25rem 0.5rem;
}

/* Endpoint section */
.endpoint-section {
  background: var(--surface-50);
  border: 1px solid var(--surface-200);
  border-radius: 8px;
  padding: 0.75rem;
  margin-top: 0.5rem;
}

.endpoint-container {
  background: var(--surface-0);
  border: 1px solid var(--surface-300);
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
}

.endpoint-url {
  font-size: 0.875rem;
  color: var(--text-color);
  font-family: 'Courier New', monospace;
  word-break: break-all;
}

.copy-button {
  width: 1.75rem;
  height: 1.75rem;
  margin-left: 0.5rem;
}

/* Card customizations */
:deep(.p-card-body) {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.p-card-content) {
  padding: 0;
  flex: 1;
}

:deep(.p-card-header) {
  padding: 0;
}

/* Responsive adjustments */
@media screen and (max-width: 768px) {
  .card-header {
    padding: 1rem 1rem 0.5rem 1rem;
  }
  
  .card-content {
    padding: 0 1rem 1rem 1rem;
  }
  
  .health-indicator {
    width: 2.5rem;
    height: 2.5rem;
    font-size: 1rem;
  }
  
  .endpoint-section {
    padding: 0.5rem;
  }
  
  .endpoint-container {
    padding: 0.375rem 0.5rem;
  }
}

/* Dark theme adjustments */
:root.dark {
  .endpoint-section {
    background: var(--surface-800);
    border-color: var(--surface-700);
  }
  
  .endpoint-container {
    background: var(--surface-900);
    border-color: var(--surface-600);
  }
}
</style>