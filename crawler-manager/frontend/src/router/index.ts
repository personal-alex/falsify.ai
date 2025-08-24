import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard,
      meta: {
        title: 'Dashboard',
        breadcrumb: [{ label: 'Dashboard' }]
      }
    },
    {
      path: '/crawler/:id',
      name: 'crawler-detail',
      component: () => import('@/views/CrawlerDetail.vue'),
      props: true,
      meta: {
        title: 'Crawler Detail',
        breadcrumb: [
          { label: 'Dashboard', to: '/' },
          { label: 'Crawlers Management', to: '/crawlers/management' },
          { label: 'Crawler Detail' }
        ]
      }
    },
    // Crawlers Management routes
    {
      path: '/crawlers/management',
      name: 'crawlers-management',
      component: () => import('@/views/CrawlersManagement.vue'),
      meta: {
        title: 'Crawlers Management',
        breadcrumb: [
          { label: 'Dashboard', to: '/' },
          { label: 'Crawlers Management' }
        ]
      }
    },
    {
      path: '/crawlers/history',
      name: 'crawlers-history',
      component: () => import('@/views/CrawlersHistory.vue'),
      meta: {
        title: 'Crawlers History',
        breadcrumb: [
          { label: 'Dashboard', to: '/' },
          { label: 'Crawlers Management', to: '/crawlers/management' },
          { label: 'History' }
        ]
      }
    },
    // Predictions routes
    {
      path: '/predictions/analysis',
      name: 'predictions-analysis',
      component: () => import('@/views/PredictionsAnalysis.vue'),
      meta: {
        title: 'Analyze Articles',
        breadcrumb: [
          { label: 'Dashboard', to: '/' },
          { label: 'Predictions Management', to: '/predictions/analysis' },
          { label: 'Analyze Articles' }
        ]
      }
    },

    {
      path: '/predictions/history',
      name: 'predictions-history',
      component: () => import('@/views/PredictionsHistory.vue'),
      meta: {
        title: 'Prediction History',
        breadcrumb: [
          { label: 'Dashboard', to: '/' },
          { label: 'Predictions Management', to: '/predictions/analysis' },
          { label: 'Prediction History' }
        ]
      }
    },
    // Analysis Status route
    {
      path: '/analysis/status',
      name: 'analysis-status',
      component: () => import('@/views/AnalysisStatus.vue'),
      meta: {
        title: 'Analysis Status',
        breadcrumb: [
          { label: 'Dashboard', to: '/' },
          { label: 'Analysis Status' }
        ]
      }
    },
    // Theme demo (for development)
    {
      path: '/theme-demo',
      name: 'theme-demo',
      component: () => import('@/views/ThemeDemo.vue'),
      meta: {
        title: 'Theme Demo',
        breadcrumb: [{ label: 'Theme Demo' }]
      }
    }
  ]
})

export default router