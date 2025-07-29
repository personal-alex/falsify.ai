<template>
  <div class="metrics-component">
    <div class="flex justify-content-between align-items-center mb-4">
      <h5 class="flex align-items-center m-0">
        <i class="pi pi-chart-line mr-2"></i>
        Metrics
      </h5>
      <div class="flex align-items-center gap-2">
        <Dropdown
          v-model="selectedTimeRange"
          :options="timeRangeOptions"
          optionLabel="label"
          optionValue="value"
          placeholder="Select time range"
          class="w-10rem"
          @change="onTimeRangeChange"
        />
        <Button
          icon="pi pi-refresh"
          class="p-button-text p-button-sm"
          :loading="isLoading"
          @click="refreshMetrics"
          v-tooltip.top="'Refresh metrics'"
        />
        <Button
          icon="pi pi-download"
          class="p-button-text p-button-sm"
          @click="exportCharts"
          v-tooltip.top="'Export charts'"
          :disabled="!hasMetricsData"
        />
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading && !metrics" class="flex align-items-center justify-content-center p-4">
      <ProgressSpinner size="small" />
      <span class="ml-2 text-600">Loading metrics...</span>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="text-center p-4">
      <Message severity="error" :closable="false">
        <div class="flex align-items-center">
          <i class="pi pi-exclamation-triangle mr-2"></i>
          <div>
            <div class="font-medium">Failed to load metrics</div>
            <div class="text-sm">{{ error }}</div>
          </div>
        </div>
      </Message>
      <Button
        icon="pi pi-refresh"
        label="Retry"
        class="p-button-sm mt-2"
        @click="refreshMetrics"
      />
    </div>

    <!-- No Data State -->
    <div v-else-if="!metrics || !hasMetricsData" class="text-center p-4">
      <div class="text-600">
        <i class="pi pi-chart-line text-4xl mb-3"></i>
        <div class="text-lg font-medium">No metrics data available</div>
        <div class="text-sm">
          {{ lastUpdated ? `Last updated: ${formatTimestamp(lastUpdated)}` : 'No data collected yet' }}
        </div>
      </div>
    </div>

    <!-- Metrics Content -->
    <div v-else class="metrics-content">
      <!-- Key Metrics Cards -->
      <div class="grid mb-4">
        <div class="col-12 md:col-3">
          <div class="metric-card surface-card p-3 border-round shadow-1">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-600 text-sm mb-1">Articles Processed</div>
                <div class="text-900 text-2xl font-semibold">
                  {{ formatNumber(metrics.articlesProcessed) }}
                </div>
                <div class="flex align-items-center mt-1">
                  <i 
                    :class="getTrendIcon(metrics.trends?.articlesProcessed)" 
                    class="text-sm mr-1"
                  ></i>
                  <span 
                    :class="getTrendColor(metrics.trends?.articlesProcessed)"
                    class="text-sm font-medium"
                  >
                    {{ formatTrendValue(metrics.trends?.articlesProcessed) }}
                  </span>
                </div>
              </div>
              <div class="metric-icon">
                <i class="pi pi-file text-2xl text-blue-500"></i>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 md:col-3">
          <div class="metric-card surface-card p-3 border-round shadow-1">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-600 text-sm mb-1">Success Rate</div>
                <div class="text-900 text-2xl font-semibold">
                  {{ formatPercentage(metrics.successRate) }}
                </div>
                <div class="flex align-items-center mt-1">
                  <ProgressBar 
                    :value="metrics.successRate" 
                    class="w-full"
                    :class="getSuccessRateClass(metrics.successRate)"
                    style="height: 4px"
                  />
                </div>
              </div>
              <div class="metric-icon">
                <i class="pi pi-check-circle text-2xl text-green-500"></i>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 md:col-3">
          <div class="metric-card surface-card p-3 border-round shadow-1">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-600 text-sm mb-1">Avg Processing Time</div>
                <div class="text-900 text-2xl font-semibold">
                  {{ formatDuration(metrics.averageProcessingTimeMs) }}
                </div>
                <div class="flex align-items-center mt-1">
                  <i 
                    :class="getTrendIcon(metrics.trends?.averageProcessingTime, true)" 
                    class="text-sm mr-1"
                  ></i>
                  <span 
                    :class="getTrendColor(metrics.trends?.averageProcessingTime, true)"
                    class="text-sm font-medium"
                  >
                    {{ formatTrendValue(metrics.trends?.averageProcessingTime) }}
                  </span>
                </div>
              </div>
              <div class="metric-icon">
                <i class="pi pi-clock text-2xl text-orange-500"></i>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 md:col-3">
          <div class="metric-card surface-card p-3 border-round shadow-1">
            <div class="flex align-items-center justify-content-between">
              <div>
                <div class="text-600 text-sm mb-1">Error Count</div>
                <div class="text-900 text-2xl font-semibold">
                  {{ formatNumber(metrics.errorCount) }}
                </div>
                <div class="flex align-items-center mt-1">
                  <i 
                    :class="getTrendIcon(metrics.trends?.errorCount, true)" 
                    class="text-sm mr-1"
                  ></i>
                  <span 
                    :class="getTrendColor(metrics.trends?.errorCount, true)"
                    class="text-sm font-medium"
                  >
                    {{ formatTrendValue(metrics.trends?.errorCount) }}
                  </span>
                </div>
              </div>
              <div class="metric-icon">
                <i class="pi pi-exclamation-triangle text-2xl text-red-500"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Charts Section -->
      <div class="grid">
        <!-- Articles Processed Chart -->
        <div class="col-12 lg:col-6">
          <Panel class="chart-panel" :toggleable="true">
            <template #header>
              <div class="flex align-items-center">
                <i class="pi pi-chart-line mr-2 text-primary"></i>
                <span class="font-semibold">Articles Processed Over Time</span>
              </div>
            </template>
            <template #icons>
              <Button
                icon="pi pi-download"
                class="p-button-text p-button-sm"
                @click="exportChart('articles')"
                v-tooltip.top="'Export chart'"
              />
            </template>
            <div class="chart-container" style="height: 300px;">
              <Chart
                v-if="articlesChartData"
                type="line"
                :data="articlesChartData"
                :options="articlesChartOptions"
                class="chart-responsive"
              />
              <div v-else class="flex align-items-center justify-content-center h-full text-600">
                <i class="pi pi-chart-line mr-2"></i>
                No chart data available
              </div>
            </div>
          </Panel>
        </div>

        <!-- Success Rate Chart -->
        <div class="col-12 lg:col-6">
          <Panel class="chart-panel" :toggleable="true">
            <template #header>
              <div class="flex align-items-center">
                <i class="pi pi-chart-bar mr-2 text-green-500"></i>
                <span class="font-semibold">Success Rate Trend</span>
              </div>
            </template>
            <template #icons>
              <Button
                icon="pi pi-download"
                class="p-button-text p-button-sm"
                @click="exportChart('success-rate')"
                v-tooltip.top="'Export chart'"
              />
            </template>
            <div class="chart-container" style="height: 300px;">
              <Chart
                v-if="successRateChartData"
                type="line"
                :data="successRateChartData"
                :options="successRateChartOptions"
                class="chart-responsive"
              />
              <div v-else class="flex align-items-center justify-content-center h-full text-600">
                <i class="pi pi-chart-bar mr-2"></i>
                No chart data available
              </div>
            </div>
          </Panel>
        </div>

        <!-- Processing Time Chart -->
        <div class="col-12 lg:col-6">
          <Panel class="chart-panel" :toggleable="true">
            <template #header>
              <div class="flex align-items-center">
                <i class="pi pi-clock mr-2 text-orange-500"></i>
                <span class="font-semibold">Processing Time Trend</span>
              </div>
            </template>
            <template #icons>
              <Button
                icon="pi pi-download"
                class="p-button-text p-button-sm"
                @click="exportChart('processing-time')"
                v-tooltip.top="'Export chart'"
              />
            </template>
            <div class="chart-container" style="height: 300px;">
              <Chart
                v-if="processingTimeChartData"
                type="line"
                :data="processingTimeChartData"
                :options="processingTimeChartOptions"
                class="chart-responsive"
              />
              <div v-else class="flex align-items-center justify-content-center h-full text-600">
                <i class="pi pi-clock mr-2"></i>
                No chart data available
              </div>
            </div>
          </Panel>
        </div>

        <!-- Error Count Chart -->
        <div class="col-12 lg:col-6">
          <Panel class="chart-panel" :toggleable="true">
            <template #header>
              <div class="flex align-items-center">
                <i class="pi pi-exclamation-triangle mr-2 text-red-500"></i>
                <span class="font-semibold">Error Count Trend</span>
              </div>
            </template>
            <template #icons>
              <Button
                icon="pi pi-download"
                class="p-button-text p-button-sm"
                @click="exportChart('error-count')"
                v-tooltip.top="'Export chart'"
              />
            </template>
            <div class="chart-container" style="height: 300px;">
              <Chart
                v-if="errorCountChartData"
                type="bar"
                :data="errorCountChartData"
                :options="errorCountChartOptions"
                class="chart-responsive"
              />
              <div v-else class="flex align-items-center justify-content-center h-full text-600">
                <i class="pi pi-exclamation-triangle mr-2"></i>
                No chart data available
              </div>
            </div>
          </Panel>
        </div>
      </div>

      <!-- Last Updated -->
      <div class="text-center mt-4" v-if="lastUpdated">
        <small class="text-600">
          Last updated: {{ formatTimestamp(lastUpdated) }}
        </small>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import Button from 'primevue/button'
import Dropdown from 'primevue/dropdown'
import ProgressSpinner from 'primevue/progressspinner'
import ProgressBar from 'primevue/progressbar'
import Message from 'primevue/message'
import Panel from 'primevue/panel'
import Chart from 'primevue/chart'
import { useThemeStore } from '@/stores/theme'

interface Props {
  crawlerId: string
  autoRefresh?: boolean
  refreshInterval?: number
}

// Removed unused MetricPoint interface - using the one from CrawlerMetrics instead

interface CrawlerMetrics {
  crawlerId: string
  articlesProcessed: number
  successRate: number
  averageProcessingTimeMs: number
  errorCount: number
  lastCrawlTime?: string
  lastUpdated: string
  trendsData?: Array<{
    timestamp: string
    articlesProcessed: number
    successRate: number
    processingTimeMs: number
    errorCount: number
  }>
  totalCrawlsExecuted: number
  totalExecutionTimeMs: number
  activeCrawls: number
  trends?: {
    articlesProcessed?: number
    successRate?: number
    averageProcessingTime?: number
    errorCount?: number
  }
}

const props = withDefaults(defineProps<Props>(), {
  autoRefresh: true,
  refreshInterval: 30000 // 30 seconds
})

// State
const metrics = ref<CrawlerMetrics | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)
const lastUpdated = ref<Date | null>(null)
const selectedTimeRange = ref('1h')

// Auto-refresh timer
let refreshTimer: NodeJS.Timeout | null = null

// Time range options
const timeRangeOptions = [
  { label: '1 Hour', value: '1h' },
  { label: '6 Hours', value: '6h' },
  { label: '24 Hours', value: '1d' },
  { label: '7 Days', value: '7d' },
  { label: '30 Days', value: '30d' }
]

// Computed properties
const hasMetricsData = computed(() => {
  return metrics.value && (
    metrics.value.articlesProcessed > 0 ||
    metrics.value.errorCount > 0 ||
    (metrics.value.trendsData && metrics.value.trendsData.length > 0)
  )
})

// Theme store for dynamic colors
const themeStore = useThemeStore()

// Sakai color scheme
const getSakaiColors = () => {
  const isDark = themeStore.isDarkMode
  return {
    primary: isDark ? '#60a5fa' : '#3b82f6',
    success: isDark ? '#4ade80' : '#22c55e',
    warning: isDark ? '#fbbf24' : '#f59e0b',
    danger: isDark ? '#f87171' : '#ef4444',
    info: isDark ? '#38bdf8' : '#0ea5e9',
    surface: isDark ? '#374151' : '#f8fafc',
    text: isDark ? '#f9fafb' : '#1f2937',
    textSecondary: isDark ? '#d1d5db' : '#6b7280'
  }
}

// Chart data computed properties with Sakai styling
const articlesChartData = computed(() => {
  if (!metrics.value?.trendsData || metrics.value.trendsData.length === 0) return null
  
  const data = metrics.value.trendsData
  const colors = getSakaiColors()
  
  return {
    labels: data.map(point => formatChartLabel(point.timestamp)),
    datasets: [{
      label: 'Articles Processed',
      data: data.map(point => point.articlesProcessed),
      borderColor: colors.primary,
      backgroundColor: `${colors.primary}20`,
      fill: true,
      tension: 0.4,
      pointBackgroundColor: colors.primary,
      pointBorderColor: colors.primary,
      pointHoverBackgroundColor: colors.primary,
      pointHoverBorderColor: '#ffffff',
      pointRadius: 4,
      pointHoverRadius: 6
    }]
  }
})

const successRateChartData = computed(() => {
  if (!metrics.value?.trendsData || metrics.value.trendsData.length === 0) return null
  
  const data = metrics.value.trendsData
  const colors = getSakaiColors()
  
  return {
    labels: data.map(point => formatChartLabel(point.timestamp)),
    datasets: [{
      label: 'Success Rate (%)',
      data: data.map(point => point.successRate),
      borderColor: colors.success,
      backgroundColor: `${colors.success}20`,
      fill: true,
      tension: 0.4,
      pointBackgroundColor: colors.success,
      pointBorderColor: colors.success,
      pointHoverBackgroundColor: colors.success,
      pointHoverBorderColor: '#ffffff',
      pointRadius: 4,
      pointHoverRadius: 6
    }]
  }
})

const processingTimeChartData = computed(() => {
  if (!metrics.value?.trendsData || metrics.value.trendsData.length === 0) return null
  
  const data = metrics.value.trendsData
  const colors = getSakaiColors()
  
  return {
    labels: data.map(point => formatChartLabel(point.timestamp)),
    datasets: [{
      label: 'Processing Time (ms)',
      data: data.map(point => point.processingTimeMs),
      borderColor: colors.warning,
      backgroundColor: `${colors.warning}20`,
      fill: true,
      tension: 0.4,
      pointBackgroundColor: colors.warning,
      pointBorderColor: colors.warning,
      pointHoverBackgroundColor: colors.warning,
      pointHoverBorderColor: '#ffffff',
      pointRadius: 4,
      pointHoverRadius: 6
    }]
  }
})

const errorCountChartData = computed(() => {
  if (!metrics.value?.trendsData || metrics.value.trendsData.length === 0) return null
  
  const data = metrics.value.trendsData
  const colors = getSakaiColors()
  
  return {
    labels: data.map(point => formatChartLabel(point.timestamp)),
    datasets: [{
      label: 'Error Count',
      data: data.map(point => point.errorCount),
      backgroundColor: `${colors.danger}CC`,
      borderColor: colors.danger,
      borderWidth: 2,
      borderRadius: 4,
      borderSkipped: false,
      hoverBackgroundColor: colors.danger,
      hoverBorderColor: colors.danger
    }]
  }
})

// Base chart options with Sakai styling
const getBaseChartOptions = () => {
  const colors = getSakaiColors()
  
  return {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 750,
      easing: 'easeInOutQuart'
    },
    plugins: {
      legend: {
        display: true,
        position: 'top' as const,
        align: 'end' as const,
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          padding: 20,
          color: colors.text,
          font: {
            size: 12,
            family: 'Inter, system-ui, sans-serif'
          }
        }
      },
      tooltip: {
        enabled: true,
        mode: 'index' as const,
        intersect: false,
        backgroundColor: colors.surface,
        titleColor: colors.text,
        bodyColor: colors.text,
        borderColor: colors.primary,
        borderWidth: 1,
        cornerRadius: 8,
        displayColors: true,
        padding: 12,
        titleFont: {
          size: 13,
          weight: 'bold'
        },
        bodyFont: {
          size: 12
        },
        callbacks: {
          title: (context: any) => {
            return formatTooltipTimestamp(context[0].label)
          }
        }
      }
    },
    scales: {
      x: {
        display: true,
        grid: {
          display: false
        },
        ticks: {
          color: colors.textSecondary,
          font: {
            size: 11
          }
        }
      },
      y: {
        display: true,
        beginAtZero: true,
        grid: {
          color: `${colors.textSecondary}20`,
          drawBorder: false
        },
        ticks: {
          color: colors.textSecondary,
          font: {
            size: 11
          }
        }
      }
    },
    interaction: {
      mode: 'nearest' as const,
      axis: 'x' as const,
      intersect: false
    },
    elements: {
      line: {
        borderWidth: 3
      },
      point: {
        hoverBorderWidth: 3
      }
    }
  }
}

// Specific chart options
const articlesChartOptions = computed(() => ({
  ...getBaseChartOptions(),
  plugins: {
    ...getBaseChartOptions().plugins,
    tooltip: {
      ...getBaseChartOptions().plugins.tooltip,
      callbacks: {
        ...getBaseChartOptions().plugins.tooltip.callbacks,
        label: (context: any) => {
          return `Articles: ${formatNumber(context.parsed.y)}`
        }
      }
    }
  }
}))

const successRateChartOptions = computed(() => ({
  ...getBaseChartOptions(),
  scales: {
    ...getBaseChartOptions().scales,
    y: {
      ...getBaseChartOptions().scales.y,
      max: 100,
      ticks: {
        ...getBaseChartOptions().scales.y.ticks,
        callback: (value: any) => `${value}%`
      }
    }
  },
  plugins: {
    ...getBaseChartOptions().plugins,
    tooltip: {
      ...getBaseChartOptions().plugins.tooltip,
      callbacks: {
        ...getBaseChartOptions().plugins.tooltip.callbacks,
        label: (context: any) => {
          return `Success Rate: ${formatPercentage(context.parsed.y)}`
        }
      }
    }
  }
}))

const processingTimeChartOptions = computed(() => ({
  ...getBaseChartOptions(),
  plugins: {
    ...getBaseChartOptions().plugins,
    tooltip: {
      ...getBaseChartOptions().plugins.tooltip,
      callbacks: {
        ...getBaseChartOptions().plugins.tooltip.callbacks,
        label: (context: any) => {
          return `Processing Time: ${formatDuration(context.parsed.y)}`
        }
      }
    }
  }
}))

const errorCountChartOptions = computed(() => ({
  ...getBaseChartOptions(),
  plugins: {
    ...getBaseChartOptions().plugins,
    tooltip: {
      ...getBaseChartOptions().plugins.tooltip,
      callbacks: {
        ...getBaseChartOptions().plugins.tooltip.callbacks,
        label: (context: any) => {
          return `Errors: ${formatNumber(context.parsed.y)}`
        }
      }
    }
  }
}))

// Methods
const loadMetrics = async () => {
  try {
    isLoading.value = true
    error.value = null

    // Import ApiService dynamically to avoid circular dependencies
    const { ApiService } = await import('@/services/api')
    
    // Get historical metrics if time range is selected
    let data
    if (selectedTimeRange.value !== '1h') {
      const hours = parseTimeRange(selectedTimeRange.value)
      data = await ApiService.getCrawlerMetricsHistory(props.crawlerId, hours)
    } else {
      data = await ApiService.getCrawlerMetrics(props.crawlerId)
    }
    
    metrics.value = data
    lastUpdated.value = new Date()
  } catch (err) {
    console.error('Failed to load metrics:', err)
    error.value = err instanceof Error ? err.message : 'Failed to load metrics'
  } finally {
    isLoading.value = false
  }
}

const parseTimeRange = (timeRange: string): number => {
  switch (timeRange) {
    case '1h': return 1
    case '6h': return 6
    case '1d': return 24
    case '7d': return 168
    case '30d': return 720
    default: return 24
  }
}

const refreshMetrics = async () => {
  await loadMetrics()
}

const onTimeRangeChange = () => {
  loadMetrics()
}

// Export functionality
const exportChart = (chartType: string) => {
  try {
    const chartElement = document.querySelector(`.chart-container canvas`) as HTMLCanvasElement
    if (!chartElement) {
      console.warn('Chart canvas not found for export')
      return
    }

    // Create download link
    const link = document.createElement('a')
    link.download = `${props.crawlerId}-${chartType}-${selectedTimeRange.value}-${new Date().toISOString().split('T')[0]}.png`
    link.href = chartElement.toDataURL('image/png')
    link.click()
  } catch (error) {
    console.error('Failed to export chart:', error)
  }
}

const exportCharts = () => {
  const charts = ['articles', 'success-rate', 'processing-time', 'error-count']
  charts.forEach((chartType, index) => {
    setTimeout(() => {
      exportChart(chartType)
    }, index * 500) // Stagger exports to avoid conflicts
  })
}

const startAutoRefresh = () => {
  if (props.autoRefresh && props.refreshInterval > 0) {
    refreshTimer = setInterval(() => {
      if (!isLoading.value) {
        loadMetrics()
      }
    }, props.refreshInterval)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// Formatting methods
const formatNumber = (value: number): string => {
  if (value >= 1000000) {
    return `${(value / 1000000).toFixed(1)}M`
  } else if (value >= 1000) {
    return `${(value / 1000).toFixed(1)}K`
  }
  return value.toString()
}

const formatPercentage = (value: number): string => {
  return `${value.toFixed(1)}%`
}

const formatDuration = (milliseconds: number): string => {
  if (milliseconds < 1000) {
    return `${milliseconds}ms`
  } else if (milliseconds < 60000) {
    return `${(milliseconds / 1000).toFixed(1)}s`
  } else {
    return `${(milliseconds / 60000).toFixed(1)}m`
  }
}

const formatTimestamp = (timestamp: string | Date): string => {
  const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp
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
}

const formatChartLabel = (timestamp: string): string => {
  const date = new Date(timestamp)
  const timeRange = selectedTimeRange.value
  
  if (timeRange === '1h' || timeRange === '6h') {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else if (timeRange === '1d') {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }
}

const formatTooltipTimestamp = (label: string): string => {
  // This is a simplified version - in a real implementation,
  // you'd want to store the original timestamp and format it properly
  return label
}

const getTrendIcon = (trendValue?: number, inverse = false): string => {
  if (trendValue === undefined || trendValue === null) return 'pi pi-minus'
  
  const isPositive = inverse ? trendValue < 0 : trendValue > 0
  const isNegative = inverse ? trendValue > 0 : trendValue < 0
  
  if (isPositive) return 'pi pi-arrow-up'
  if (isNegative) return 'pi pi-arrow-down'
  return 'pi pi-minus'
}

const getTrendColor = (trendValue?: number, inverse = false): string => {
  if (trendValue === undefined || trendValue === null) return 'text-600'
  
  const isPositive = inverse ? trendValue < 0 : trendValue > 0
  const isNegative = inverse ? trendValue > 0 : trendValue < 0
  
  if (isPositive) return 'text-green-500'
  if (isNegative) return 'text-red-500'
  return 'text-600'
}

const formatTrendValue = (trendValue?: number): string => {
  if (trendValue === undefined || trendValue === null) return 'No change'
  
  const absValue = Math.abs(trendValue)
  const sign = trendValue > 0 ? '+' : ''
  
  if (absValue < 1) {
    return `${sign}${(trendValue * 100).toFixed(1)}%`
  } else {
    return `${sign}${trendValue.toFixed(1)}`
  }
}

const getSuccessRateClass = (rate: number): string => {
  if (rate >= 95) return 'success-rate-excellent'
  if (rate >= 85) return 'success-rate-good'
  if (rate >= 70) return 'success-rate-warning'
  return 'success-rate-poor'
}

// WebSocket integration
let wsUnsubscribe: (() => void) | null = null

const setupWebSocketListeners = async () => {
  try {
    const { getWebSocketService } = await import('@/services/websocket')
    const ws = getWebSocketService()
    
    wsUnsubscribe = ws.onMetricsUpdate((message) => {
      if (message.crawlerId === props.crawlerId) {
        // Update metrics with real-time data
        if (metrics.value) {
          metrics.value.articlesProcessed = message.metrics.articlesProcessed
          metrics.value.successRate = message.metrics.successRate
          metrics.value.averageProcessingTimeMs = message.metrics.averageProcessingTime
          metrics.value.errorCount = message.metrics.errorCount
          lastUpdated.value = new Date()
        }
      }
    })
  } catch (error) {
    console.warn('Failed to setup WebSocket listeners:', error)
  }
}

// Lifecycle
onMounted(() => {
  loadMetrics()
  startAutoRefresh()
  setupWebSocketListeners()
})

onUnmounted(() => {
  stopAutoRefresh()
  if (wsUnsubscribe) {
    wsUnsubscribe()
  }
})

// Watch for crawlerId changes
watch(() => props.crawlerId, () => {
  loadMetrics()
})

// Watch for autoRefresh changes
watch(() => props.autoRefresh, (newValue) => {
  if (newValue) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
})
</script>

<style scoped>
.metrics-component {
  width: 100%;
}

.metric-card {
  transition: all 0.3s ease;
  border: 1px solid var(--surface-border);
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}

.metric-icon {
  opacity: 0.8;
  transition: opacity 0.2s ease;
}

.metric-card:hover .metric-icon {
  opacity: 1;
}

.chart-panel {
  transition: all 0.3s ease;
  border: 1px solid var(--surface-border);
}

.chart-panel:hover {
  box-shadow: 0 4px 25px rgba(0, 0, 0, 0.1);
}

.chart-container {
  position: relative;
  padding: 1rem 0;
}

.chart-responsive {
  width: 100% !important;
  height: 100% !important;
}

/* Panel header styling */
:deep(.p-panel-header) {
  background: var(--surface-50);
  border-bottom: 1px solid var(--surface-border);
  padding: 1rem 1.25rem;
}

:deep(.p-panel-content) {
  padding: 0;
}

/* Chart canvas styling */
:deep(.chart-container canvas) {
  border-radius: 6px;
}

/* Success rate progress bar colors */
:deep(.success-rate-excellent .p-progressbar-value) {
  background: linear-gradient(90deg, var(--green-400), var(--green-600));
}

:deep(.success-rate-good .p-progressbar-value) {
  background: linear-gradient(90deg, var(--blue-400), var(--blue-600));
}

:deep(.success-rate-warning .p-progressbar-value) {
  background: linear-gradient(90deg, var(--orange-400), var(--orange-600));
}

:deep(.success-rate-poor .p-progressbar-value) {
  background: linear-gradient(90deg, var(--red-400), var(--red-600));
}

/* Enhanced metric card styling */
.metric-card .text-2xl {
  font-weight: 700;
  letter-spacing: -0.025em;
}

.metric-card .text-600 {
  font-weight: 500;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
}

/* Chart panel icons */
:deep(.p-panel-icons) {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* Loading and error states */
.text-600 i.text-4xl {
  opacity: 0.3;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .metric-card {
    margin-bottom: 1rem;
  }
  
  .chart-container {
    height: 250px !important;
    padding: 0.5rem 0;
  }
  
  :deep(.p-panel-header) {
    padding: 0.75rem 1rem;
  }
}

@media (max-width: 576px) {
  .chart-container {
    height: 200px !important;
  }
  
  .metric-card .text-2xl {
    font-size: 1.5rem;
  }
  
  .metric-card .text-600 {
    font-size: 0.7rem;
  }
  
  :deep(.p-panel-header) {
    padding: 0.5rem 0.75rem;
  }
  
  :deep(.p-panel-header .font-semibold) {
    font-size: 0.9rem;
  }
}

/* Dark theme adjustments */
[data-theme="dark"] .metric-card {
  background: var(--surface-100);
  border-color: var(--surface-300);
}

[data-theme="dark"] .chart-panel {
  background: var(--surface-100);
  border-color: var(--surface-300);
}

[data-theme="dark"] :deep(.p-panel-header) {
  background: var(--surface-200);
  border-color: var(--surface-300);
}

/* Animation for chart loading */
@keyframes chartFadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chart-container canvas {
  animation: chartFadeIn 0.6s ease-out;
}

/* Hover effects for interactive elements */
.metric-card .pi {
  transition: transform 0.2s ease;
}

.metric-card:hover .pi {
  transform: scale(1.1);
}

/* Export button styling */
:deep(.p-button.p-button-text.p-button-sm) {
  padding: 0.375rem;
  border-radius: 50%;
  transition: all 0.2s ease;
}

:deep(.p-button.p-button-text.p-button-sm:hover) {
  background: var(--primary-50);
  transform: scale(1.1);
}

[data-theme="dark"] :deep(.p-button.p-button-text.p-button-sm:hover) {
  background: var(--primary-900);
}
</style>