import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import AuthorInfo from '../AuthorInfo.vue'

describe('AuthorInfo', () => {
  it('renders author name and avatar when provided', () => {
    const wrapper = mount(AuthorInfo, {
      props: {
        authorName: 'Test Author',
        authorAvatarUrl: 'https://example.com/avatar.jpg'
      }
    })

    expect(wrapper.find('.author-name').text()).toBe('Test Author')
    expect(wrapper.find('.avatar-image').attributes('src')).toBe('https://example.com/avatar.jpg')
    expect(wrapper.find('.avatar-image').attributes('alt')).toBe('Test Author avatar')
  })

  it('renders default avatar when no avatar URL provided', () => {
    const wrapper = mount(AuthorInfo, {
      props: {
        authorName: 'Test Author'
      }
    })

    expect(wrapper.find('.author-name').text()).toBe('Test Author')
    expect(wrapper.find('.default-avatar').exists()).toBe(true)
    expect(wrapper.find('.default-avatar i.pi-user').exists()).toBe(true)
    expect(wrapper.find('.avatar-image').exists()).toBe(false)
  })

  it('renders "Unknown Author" when no author name provided', () => {
    const wrapper = mount(AuthorInfo, {
      props: {}
    })

    expect(wrapper.find('.author-name').text()).toBe('Unknown Author')
    expect(wrapper.find('.default-avatar').exists()).toBe(true)
  })

  it('renders default avatar when avatar URL is empty', () => {
    const wrapper = mount(AuthorInfo, {
      props: {
        authorName: 'Test Author',
        authorAvatarUrl: ''
      }
    })

    expect(wrapper.find('.author-name').text()).toBe('Test Author')
    expect(wrapper.find('.default-avatar').exists()).toBe(true)
    expect(wrapper.find('.avatar-image').exists()).toBe(false)
  })

  it('has proper styling classes', () => {
    const wrapper = mount(AuthorInfo, {
      props: {
        authorName: 'Test Author',
        authorAvatarUrl: 'https://example.com/avatar.jpg'
      }
    })

    expect(wrapper.find('.author-info').exists()).toBe(true)
    expect(wrapper.find('.author-avatar').exists()).toBe(true)
    expect(wrapper.find('.author-details').exists()).toBe(true)
    expect(wrapper.find('.author-label').text()).toBe('Author')
  })
})