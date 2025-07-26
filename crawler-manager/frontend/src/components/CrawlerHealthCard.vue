<template>
  <div class="crawler-health-card">
    <Card class="h-full">
      <template #header>
        <div class="flex align-items-center justify-content-between p-3 pb-0">
          <div class="flex align-items-center">
            <div 
              class="health-indicator mr-3"
              :class="healthIndicatorClass"
            >
              <i :class="healthIconClass"></i>
            </div>
            <div>
              <h6 class="m-0 text-900 font-semibold">{{ crawler.name }}</h6>
              <p class="m-0 text-600 text-sm">{{ crawler.id }}</p>
            </div>
          </div>
          <div class="flex align-items-center">
            <Button
              icon="pi pi-refresh"
              class="p-button-text p-button-sm"
              :loading="isRefreshing"
              @click="forceHealthCheck"
              v-tooltip.top="'Force health check'"
            />
            <Button
              icon="pi pi-external-link"
              class="p-button-text p-button-sm ml-2"
              @click="$router.push(`/crawler/${crawler.id}`)"
              v-tooltip.top="'View details'"
            />
          </div>
        </div>
      </template>
      
      <template #content>
        <div class="p-3 pt-0">
          <div class="grid">
            <div class="col-6">
              <div class="text-600 text-sm mb-1">Status</div>
              <div class="flex align-items-center">
                <Tag 
                  :value="health.status" 
                  :severity="statusSeverity"
                  class="text-xs"
                />
              </div>
            </div>
            <div class="col-6">
              <div class="text-600 text-sm mb-1">Response Time</div>
              <div class="text-900 font-medium">
                {{ responseTimeDisplay }}
              </div>
            </div>
            <div class="col-12 mt-2">
              <div class="text-600 text-sm mb-1">Last Check</div>
              <div class="text-900 text-sm">{{ lastCheckDisplay }}</div>
            </div>
            <div class="col-12 mt-2" v-if="health.message">
              <div class="text-600 text-sm mb-1">Message</div>
              <div class="text-900 text-sm">{{ health.message }}</div>
            </div>
          </div>
          
          <div class="mt-3">
            <div class="text-600 text-sm mb-2">Endpoint</div>
            <div class="flex align-items-center">
              <span class="text-900 text-sm mr-2">{{ crawler.baseUrl }}</span>
              <Button
                icon="pi pi-copy"
                class="p-button-text p-button-sm"
                @click="copyToClipboard(crawler.baseUrl)"
                v-tooltip.top="'Copy URL'"
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
import { useRouter } from 'vue-router'
import { useToast } from 'primevue/usetoast'
import Card from 'primevue/card'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import type { CrawlerConfiguration, HealthStatus } from '@/types/health'
import { ApiService } from '@/services/api'

interface Props {
  crawler: CrawlerConfiguration
  health: HealthStatus
}

const props = defineProps<Props>()
const emit = defineEmits<{
  healthUpdated: [health: HealthStatus]
}>()

const router = useRouter()
const toast = useToast()
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
    toast.add({
      severity: 'success',
      summary: 'Health Check',
      detail: `Forced health check for ${props.crawler.name}`,
      life: 3000
    })
  } catch (error) {
    console.error('Failed to force health check:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to perform health check',
      life: 5000
    })
  } finally {
    isRefreshing.value = false
  }
}

const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    toast.add({
      severity: 'success',
      summary: 'Copied',
      detail: 'URL copied to clipboard',
      life: 2000
    })
  } catch (error) {
    console.error('Failed to copy to clipboard:', error)
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: 'Failed to copy URL',
      life: 3000
    })
  }
}
</script>

<style scoped>
.crawler-health-card {
  height: 100%;
}

.health-indicator {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  color: white;
}

.health-healthy {
  background-color: var(--green-500);
}

.health-unhealthy {
  background-color: var(--red-500);
}

.health-unknown {
  background-color: var(--orange-500);
}

:deep(.p-card-body) {
  padding: 0;
}

:deep(.p-card-content) {
  padding: 0;
}
</style>