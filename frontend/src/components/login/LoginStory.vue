<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  lead: string
  ringLabel: string
  ringValue: string
  ringMeta: string
  metaItems: readonly { label: string; value: string }[]
  tags: readonly string[]
}>()
</script>

<template>
  <section class="login-story">
    <div class="story-grid"></div>
    <div class="story-orbit is-a"></div>
    <div class="story-orbit is-b"></div>
    <div class="story-orbit is-c"></div>

    <div class="signal-cluster is-a">
      <span></span>
      <span></span>
      <span></span>
    </div>

    <div class="signal-cluster is-b">
      <span></span>
      <span></span>
    </div>

    <div class="story-content">
      <p class="eyebrow">{{ eyebrow }}</p>
      <h1 class="story-title">{{ title }}</h1>
      <p class="story-lead">{{ lead }}</p>

      <div class="story-panel">
        <div class="story-panel__ring">
          <div>
            <span>{{ ringLabel }}</span>
            <strong>{{ ringValue }}</strong>
            <small>{{ ringMeta }}</small>
          </div>
        </div>

        <div class="story-panel__meta">
          <div v-for="item in metaItems" :key="item.label">
            <label>{{ item.label }}</label>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>

      <div class="story-tags">
        <span v-for="tag in tags" :key="tag">{{ tag }}</span>
      </div>
    </div>
  </section>
</template>
