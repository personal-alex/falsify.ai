import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '@/views/Dashboard.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: Dashboard
    },
    {
      path: '/crawler/:id',
      name: 'crawler-detail',
      component: () => import('@/views/CrawlerDetail.vue'),
      props: true
    },
    // Placeholder routes for menu items (to be implemented later)
    {
      path: '/crawlers',
      name: 'crawlers-overview',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/crawlers/active',
      name: 'crawlers-active',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/crawlers/health',
      name: 'crawlers-health',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/jobs/history',
      name: 'jobs-history',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/jobs/scheduled',
      name: 'jobs-scheduled',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/jobs/failed',
      name: 'jobs-failed',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/monitoring/metrics',
      name: 'monitoring-metrics',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/monitoring/logs',
      name: 'monitoring-logs',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/monitoring/alerts',
      name: 'monitoring-alerts',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/config/crawlers',
      name: 'config-crawlers',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/config/system',
      name: 'config-system',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/config/preferences',
      name: 'config-preferences',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/help/docs',
      name: 'help-docs',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/help/api',
      name: 'help-api',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/help/about',
      name: 'help-about',
      component: Dashboard // Temporary redirect to dashboard
    },
    {
      path: '/theme-demo',
      name: 'theme-demo',
      component: () => import('@/views/ThemeDemo.vue')
    }
  ]
})

export default router