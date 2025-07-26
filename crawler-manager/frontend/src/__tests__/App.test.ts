import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import App from '../App.vue'

describe('App', () => {
  it('renders properly', () => {
    const wrapper = mount(App, {
      global: {
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