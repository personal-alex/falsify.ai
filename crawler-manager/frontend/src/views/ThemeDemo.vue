<template>
  <div class="theme-demo">
    <div class="demo-header">
      <h1>Theme System Demo</h1>
      <p>Test the Sakai theme integration and configuration</p>
    </div>

    <div class="demo-content">
      <!-- Current Theme Info -->
      <Panel header="Current Theme Configuration" class="demo-panel">
        <div class="theme-info">
          <div class="info-item">
            <strong>Mode:</strong> {{ themeStore.currentMode }}
            <Tag :value="themeStore.currentMode" :severity="themeStore.isDarkMode ? 'info' : 'warning'" />
          </div>
          <div class="info-item">
            <strong>Scale:</strong> {{ themeStore.currentScale }}px
          </div>
          <div class="info-item">
            <strong>Input Style:</strong> {{ themeStore.currentInputStyle }}
          </div>
          <div class="info-item">
            <strong>Ripple:</strong> {{ themeStore.isRippleEnabled ? 'Enabled' : 'Disabled' }}
          </div>
        </div>
      </Panel>

      <!-- Quick Theme Controls -->
      <Panel header="Quick Controls" class="demo-panel">
        <div class="quick-controls">
          <Button
            :label="`Switch to ${themeStore.currentMode === 'light' ? 'Dark' : 'Light'}`"
            :icon="themeStore.currentMode === 'light' ? 'pi pi-moon' : 'pi pi-sun'"
            @click="themeStore.toggleMode()"
            class="p-button-outlined"
          />
          <Button
            label="Reset Theme"
            icon="pi pi-refresh"
            @click="themeStore.reset()"
            class="p-button-secondary"
          />
        </div>
      </Panel>

      <!-- Theme Configurator -->
      <Panel header="Theme Configurator" class="demo-panel">
        <ThemeConfigurator />
      </Panel>

      <!-- Notification Demo -->
      <NotificationDemo />

      <!-- Component Showcase -->
      <Panel header="Component Showcase" class="demo-panel">
        <div class="component-showcase">
          <!-- Buttons -->
          <div class="showcase-section">
            <h3>Buttons</h3>
            <div class="button-group">
              <Button label="Primary" />
              <Button label="Secondary" class="p-button-secondary" />
              <Button label="Success" class="p-button-success" />
              <Button label="Warning" class="p-button-warning" />
              <Button label="Danger" class="p-button-danger" />
              <Button label="Outlined" class="p-button-outlined" />
            </div>
          </div>

          <!-- Inputs -->
          <div class="showcase-section">
            <h3>Input Components</h3>
            <div class="input-group">
              <InputText v-model="demoText" placeholder="Text Input" />
              <InputNumber v-model="demoNumber" placeholder="Number Input" />
              <Dropdown v-model="demoDropdown" :options="dropdownOptions" placeholder="Select Option" />
              <Calendar v-model="demoDate" placeholder="Select Date" />
            </div>
          </div>

          <!-- Cards -->
          <div class="showcase-section">
            <h3>Cards</h3>
            <div class="card-group">
              <Card>
                <template #title>Sample Card</template>
                <template #content>
                  <p>This is a sample card with some content to demonstrate the theme styling.</p>
                </template>
              </Card>
              <Card>
                <template #title>Another Card</template>
                <template #content>
                  <p>Cards automatically adapt to the current theme colors and styling.</p>
                </template>
              </Card>
            </div>
          </div>

          <!-- Data Table -->
          <div class="showcase-section">
            <h3>Data Table</h3>
            <DataTable :value="demoTableData" class="demo-table">
              <Column field="name" header="Name" />
              <Column field="status" header="Status">
                <template #body="{ data }">
                  <Tag :value="data.status" :severity="getStatusSeverity(data.status)" />
                </template>
              </Column>
              <Column field="date" header="Date" />
            </DataTable>
          </div>
        </div>
      </Panel>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useThemeStore } from '@/stores/theme'
import Panel from 'primevue/panel'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Dropdown from 'primevue/dropdown'
import Calendar from 'primevue/calendar'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import ThemeConfigurator from '@/components/ThemeConfigurator.vue'
import NotificationDemo from '@/components/NotificationDemo.vue'

const themeStore = useThemeStore()

// Demo data
const demoText = ref('Sample text')
const demoNumber = ref(42)
const demoDropdown = ref(null)
const demoDate = ref(new Date())

const dropdownOptions = ref([
  { label: 'Option 1', value: 'opt1' },
  { label: 'Option 2', value: 'opt2' },
  { label: 'Option 3', value: 'opt3' }
])

const demoTableData = ref([
  { name: 'Crawler 1', status: 'Active', date: '2024-01-15' },
  { name: 'Crawler 2', status: 'Inactive', date: '2024-01-14' },
  { name: 'Crawler 3', status: 'Error', date: '2024-01-13' }
])

const getStatusSeverity = (status: string) => {
  switch (status) {
    case 'Active': return 'success'
    case 'Inactive': return 'warning'
    case 'Error': return 'danger'
    default: return 'info'
  }
}
</script>

<style lang="scss" scoped>
.theme-demo {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.demo-header {
  text-align: center;
  margin-bottom: 2rem;

  h1 {
    color: var(--text-color);
    margin-bottom: 0.5rem;
  }

  p {
    color: var(--text-color-secondary);
    font-size: 1.125rem;
  }
}

.demo-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.demo-panel {
  box-shadow: var(--card-shadow);
  transition: box-shadow var(--transition-duration);

  &:hover {
    box-shadow: var(--card-shadow-hover);
  }
}

.theme-info {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
}

.info-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem;
  background: var(--surface-100);
  border-radius: var(--border-radius);
  border: 1px solid var(--surface-border);
}

.quick-controls {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.component-showcase {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.showcase-section {
  h3 {
    color: var(--text-color);
    margin-bottom: 1rem;
    padding-bottom: 0.5rem;
    border-bottom: 2px solid var(--primary-color);
  }
}

.button-group {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.input-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.card-group {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.demo-table {
  margin-top: 1rem;
}

// Responsive design
@media screen and (max-width: 768px) {
  .theme-demo {
    padding: 1rem;
  }

  .theme-info {
    grid-template-columns: 1fr;
  }

  .button-group {
    justify-content: center;
  }

  .input-group {
    grid-template-columns: 1fr;
  }

  .card-group {
    grid-template-columns: 1fr;
  }
}
</style>