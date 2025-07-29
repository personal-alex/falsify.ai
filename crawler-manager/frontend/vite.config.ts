import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import "@/assets/themes/_variables.scss";`
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://localhost:8082',
        ws: true,
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: '../src/main/resources/META-INF/resources',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia'],
          primevue: [
            'primevue/config', 
            'primevue/button', 
            'primevue/card', 
            'primevue/datatable',
            'primevue/panel',
            'primevue/sidebar',
            'primevue/menubar',
            'primevue/toast',
            'primevue/dialog',
            'primevue/chart'
          ],
          charts: ['chart.js'],
          sakai: ['@/assets/themes/sakai-light.scss', '@/assets/themes/sakai-dark.scss']
        }
      }
    },
    cssCodeSplit: true,
    assetsInlineLimit: 4096
  },
  test: {
    environment: 'jsdom'
  }
})