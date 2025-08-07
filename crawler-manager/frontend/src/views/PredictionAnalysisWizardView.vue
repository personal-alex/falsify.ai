<template>
  <div class="prediction-analysis-wizard-view">
    <PredictionAnalysisWizard />
  </div>
</template>

<script setup lang="ts">
import PredictionAnalysisWizard from '@/components/PredictionAnalysisWizard.vue'
</script>

<style lang="scss" scoped>
.prediction-analysis-wizard-view {
  height: 100%;
  display: flex;
  flex-direction: column;
}
</style>