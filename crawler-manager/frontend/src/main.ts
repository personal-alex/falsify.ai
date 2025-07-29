import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import Tooltip from 'primevue/tooltip'

import App from './App.vue'
import router from './router'

// PrimeVue CSS imports
import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'

// Sakai theme styles
import './assets/main.scss'

const app = createApp(App)

// Configure Pinia store
const pinia = createPinia()
app.use(pinia)

// Configure Vue Router
app.use(router)

// Configure PrimeVue with Sakai theme
app.use(PrimeVue, {
  theme: {
    preset: 'Aura',
    options: {
      darkModeSelector: '[data-theme="dark"]',
      cssLayer: {
        name: 'primevue',
        order: 'primevue-base, primevue-theme, primevue-utilities'
      }
    }
  },
  ripple: true
})

// Configure PrimeVue services
app.use(ToastService)
app.use(ConfirmationService)

// Configure PrimeVue directives
app.directive('tooltip', Tooltip)

// Initialize theme system early
import { useThemeStore } from './stores/theme'
const themeStore = useThemeStore(pinia)
themeStore.initialize()

app.mount('#app')