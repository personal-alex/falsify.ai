<template>
  <div class="theme-configurator">
    <div class="theme-configurator-header">
      <h3 class="theme-configurator-title">
        <i class="pi pi-palette"></i>
        Theme Configuration
      </h3>
      <p class="theme-configurator-description">
        Customize the appearance and behavior of the application
      </p>
    </div>

    <div class="theme-configurator-content">
      <!-- Theme Mode Selection -->
      <div class="config-section">
        <label class="config-label">
          <i class="pi pi-moon"></i>
          Theme Mode
        </label>
        <div class="config-control">
          <div class="theme-mode-selector">
            <button
              v-for="mode in themeModes"
              :key="mode.value"
              class="theme-mode-button"
              :class="{ 
                'active': themeStore.currentMode === mode.value,
                [`theme-${mode.value}`]: true
              }"
              @click="themeStore.setMode(mode.value)"
              :aria-label="`Switch to ${mode.label} theme`"
            >
              <i :class="mode.icon"></i>
              <span>{{ mode.label }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Font Scale Selection -->
      <div class="config-section">
        <label class="config-label">
          <i class="pi pi-search-plus"></i>
          Font Scale
        </label>
        <div class="config-control">
          <div class="scale-selector">
            <button
              v-for="scale in fontScales"
              :key="scale.value"
              class="scale-button"
              :class="{ 'active': themeStore.currentScale === scale.value }"
              @click="themeStore.setScale(scale.value)"
              :aria-label="`Set font scale to ${scale.label}`"
            >
              {{ scale.label }}
            </button>
          </div>
          <div class="scale-preview">
            <span class="scale-preview-text">Preview Text</span>
          </div>
        </div>
      </div>

      <!-- Input Style Selection -->
      <div class="config-section">
        <label class="config-label">
          <i class="pi pi-pencil"></i>
          Input Style
        </label>
        <div class="config-control">
          <div class="input-style-selector">
            <button
              v-for="style in inputStyles"
              :key="style.value"
              class="input-style-button"
              :class="{ 'active': themeStore.currentInputStyle === style.value }"
              @click="themeStore.setInputStyle(style.value)"
              :aria-label="`Set input style to ${style.label}`"
            >
              <div class="input-style-preview" :class="`preview-${style.value}`">
                <input type="text" :placeholder="style.label" readonly />
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Ripple Effect Toggle -->
      <div class="config-section">
        <label class="config-label">
          <i class="pi pi-circle"></i>
          Ripple Effects
        </label>
        <div class="config-control">
          <div class="ripple-toggle">
            <label class="toggle-switch">
              <input
                type="checkbox"
                :checked="themeStore.isRippleEnabled"
                @change="themeStore.setRipple(($event.target as HTMLInputElement).checked)"
              />
              <span class="toggle-slider"></span>
            </label>
            <span class="toggle-label">
              {{ themeStore.isRippleEnabled ? 'Enabled' : 'Disabled' }}
            </span>
          </div>
        </div>
      </div>

      <!-- Theme Colors Preview -->
      <div class="config-section">
        <label class="config-label">
          <i class="pi pi-eye"></i>
          Color Preview
        </label>
        <div class="config-control">
          <div class="color-preview">
            <div class="color-swatch primary" :style="{ backgroundColor: themeStore.colors.primary }">
              <span>Primary</span>
            </div>
            <div class="color-swatch surface" :style="{ backgroundColor: themeStore.colors.surface }">
              <span>Surface</span>
            </div>
            <div class="color-swatch text" :style="{ backgroundColor: themeStore.colors.text }">
              <span>Text</span>
            </div>
            <div class="color-swatch border" :style="{ backgroundColor: themeStore.colors.border }">
              <span>Border</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Actions -->
    <div class="theme-configurator-actions">
      <Button
        label="Reset to Default"
        icon="pi pi-refresh"
        class="p-button-outlined"
        @click="resetTheme"
      />
      <Button
        label="Apply Changes"
        icon="pi pi-check"
        @click="applyChanges"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import Button from 'primevue/button'
import { useThemeStore } from '@/stores/theme'
import type { ThemeMode, ThemeScale, InputStyle } from '@/stores/theme'

const themeStore = useThemeStore()

// Theme mode options
const themeModes = computed(() => [
  {
    value: 'light' as ThemeMode,
    label: 'Light',
    icon: 'pi pi-sun'
  },
  {
    value: 'dark' as ThemeMode,
    label: 'Dark',
    icon: 'pi pi-moon'
  }
])

// Font scale options
const fontScales = computed(() => [
  { value: 12 as ThemeScale, label: 'Small' },
  { value: 13 as ThemeScale, label: 'Medium' },
  { value: 14 as ThemeScale, label: 'Default' },
  { value: 15 as ThemeScale, label: 'Large' },
  { value: 16 as ThemeScale, label: 'Extra Large' }
])

// Input style options
const inputStyles = computed(() => [
  {
    value: 'outlined' as InputStyle,
    label: 'Outlined'
  },
  {
    value: 'filled' as InputStyle,
    label: 'Filled'
  }
])

// Methods
const resetTheme = () => {
  themeStore.reset()
}

const applyChanges = () => {
  // Changes are applied automatically, this is just for user feedback
  console.log('Theme configuration applied')
}
</script>

<style lang="scss" scoped>
.theme-configurator {
  padding: 1.5rem;
  background: var(--surface-card);
  border-radius: var(--border-radius);
  box-shadow: var(--card-shadow);
  max-width: 600px;
  margin: 0 auto;
}

.theme-configurator-header {
  margin-bottom: 2rem;
  text-align: center;
}

.theme-configurator-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  margin: 0 0 0.5rem 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-color);

  i {
    color: var(--primary-color);
  }
}

.theme-configurator-description {
  margin: 0;
  color: var(--text-color-secondary);
  font-size: 0.875rem;
}

.theme-configurator-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.config-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--text-color);
  font-size: 0.875rem;

  i {
    color: var(--primary-color);
    font-size: 1rem;
  }
}

.config-control {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

// Theme Mode Selector
.theme-mode-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.theme-mode-button {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  border: 2px solid var(--surface-border);
  border-radius: var(--border-radius);
  background: var(--surface-section);
  color: var(--text-color);
  cursor: pointer;
  transition: all var(--transition-duration);

  &:hover {
    border-color: var(--primary-color);
    transform: translateY(-2px);
    box-shadow: var(--card-shadow);
  }

  &.active {
    border-color: var(--primary-color);
    background: var(--primary-50);
    color: var(--primary-700);
  }

  &.theme-dark.active {
    background: var(--primary-900);
    color: var(--primary-200);
  }

  i {
    font-size: 1.5rem;
  }

  span {
    font-weight: 500;
  }
}

// Font Scale Selector
.scale-selector {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.scale-button {
  padding: 0.5rem 1rem;
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  background: var(--surface-section);
  color: var(--text-color);
  cursor: pointer;
  transition: all var(--transition-duration);
  font-size: 0.875rem;

  &:hover {
    border-color: var(--primary-color);
    background: var(--surface-hover);
  }

  &.active {
    border-color: var(--primary-color);
    background: var(--primary-color);
    color: white;
  }
}

.scale-preview {
  padding: 1rem;
  border: 1px solid var(--surface-border);
  border-radius: var(--border-radius);
  background: var(--surface-section);
  text-align: center;
}

.scale-preview-text {
  font-size: 1rem;
  color: var(--text-color);
}

// Input Style Selector
.input-style-selector {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.input-style-button {
  padding: 1rem;
  border: 2px solid var(--surface-border);
  border-radius: var(--border-radius);
  background: var(--surface-section);
  cursor: pointer;
  transition: all var(--transition-duration);

  &:hover {
    border-color: var(--primary-color);
    transform: translateY(-2px);
  }

  &.active {
    border-color: var(--primary-color);
    background: var(--primary-50);
  }
}

.input-style-preview {
  input {
    width: 100%;
    padding: 0.75rem;
    border-radius: var(--border-radius);
    font-size: 0.875rem;
    color: var(--text-color);
    background: var(--surface-ground);

    &::placeholder {
      color: var(--text-color-secondary);
    }
  }

  &.preview-outlined input {
    border: 1px solid var(--surface-border);
  }

  &.preview-filled input {
    border: none;
    background: var(--surface-200);
  }
}

// Ripple Toggle
.ripple-toggle {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 3rem;
  height: 1.5rem;

  input {
    opacity: 0;
    width: 0;
    height: 0;
  }
}

.toggle-slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: var(--surface-300);
  transition: var(--transition-duration);
  border-radius: 1.5rem;

  &:before {
    position: absolute;
    content: "";
    height: 1.125rem;
    width: 1.125rem;
    left: 0.1875rem;
    bottom: 0.1875rem;
    background-color: white;
    transition: var(--transition-duration);
    border-radius: 50%;
  }

  input:checked + & {
    background-color: var(--primary-color);
  }

  input:checked + &:before {
    transform: translateX(1.5rem);
  }
}

.toggle-label {
  font-weight: 500;
  color: var(--text-color);
}

// Color Preview
.color-preview {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.75rem;
}

.color-swatch {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 3rem;
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
  position: relative;
  overflow: hidden;

  span {
    font-size: 0.75rem;
    font-weight: 600;
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
    color: white;
    mix-blend-mode: difference;
  }
}

// Actions
.theme-configurator-actions {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--surface-border);
}

// Responsive Design
@media screen and (max-width: 768px) {
  .theme-configurator {
    padding: 1rem;
  }

  .theme-mode-selector,
  .input-style-selector {
    grid-template-columns: 1fr;
  }

  .scale-selector {
    justify-content: center;
  }

  .color-preview {
    grid-template-columns: repeat(2, 1fr);
  }

  .theme-configurator-actions {
    flex-direction: column;
  }
}

// Dark theme adjustments
:global([data-theme="dark"]) {
  .theme-mode-button.theme-light.active {
    background: var(--primary-100);
    color: var(--primary-800);
  }

  .input-style-button.active {
    background: var(--primary-900);
  }

  .toggle-slider:before {
    background-color: var(--surface-100);
  }
}
</style>