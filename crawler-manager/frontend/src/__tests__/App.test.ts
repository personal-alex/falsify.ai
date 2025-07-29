import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import App from '../App.vue'

describe('App', () => {
  it('renders properly', () => {
    const wrapper = mount(App, {
      global: {
        plugins: [
          createPinia(),
          PrimeVue,
          ToastService,
          ConfirmationService
        ],
        stubs: {
          RouterView: true,
          Toast: true,
          ConfirmDialog: true
        }
      }
    })
    expect(wrapper.exists()).toBe(true)
  })
})