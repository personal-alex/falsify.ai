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
    }
  ]
})

export default router