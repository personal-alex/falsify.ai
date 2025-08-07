<template>
  <div class="author-info">
    <div class="flex align-items-center">
      <div class="author-avatar mr-3">
        <img 
          v-if="authorAvatarUrl" 
          :src="authorAvatarUrl" 
          :alt="`${authorName} avatar`"
          class="avatar-image"
          @error="onImageError"
        />
        <div v-else class="default-avatar">
          <i class="pi pi-user"></i>
        </div>
      </div>
      <div class="author-details">
        <div class="author-name text-900 font-medium">{{ authorName || 'Unknown Author' }}</div>
        <div class="author-label text-600 text-sm">Author</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  authorName?: string
  authorAvatarUrl?: string
}

defineProps<Props>()

const imageError = ref(false)

const onImageError = () => {
  imageError.value = true
}
</script>

<style scoped>
.author-info {
  padding: 0.75rem;
  background: var(--surface-50);
  border-radius: 6px;
  border: 1px solid var(--surface-200);
}

.author-avatar {
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--surface-100);
  border: 2px solid var(--surface-200);
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.default-avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-100);
  color: var(--primary-600);
  font-size: 1.25rem;
}

.author-details {
  flex: 1;
}

.author-name {
  font-size: 1rem;
  line-height: 1.2;
  margin-bottom: 0.25rem;
}

.author-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  opacity: 0.8;
}

/* Dark theme adjustments */
@media (prefers-color-scheme: dark) {
  .author-info {
    background: var(--surface-800);
    border-color: var(--surface-700);
  }
  
  .author-avatar {
    background: var(--surface-700);
    border-color: var(--surface-600);
  }
  
  .default-avatar {
    background: var(--primary-900);
    color: var(--primary-300);
  }
}

/* Responsive adjustments */
@media screen and (max-width: 575px) {
  .author-avatar {
    width: 2.5rem;
    height: 2.5rem;
  }
  
  .author-name {
    font-size: 0.875rem;
  }
  
  .author-label {
    font-size: 0.6875rem;
  }
}
</style>