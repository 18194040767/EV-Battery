<template>
  <span class="nav-icon" aria-hidden="true">
    <svg v-if="name === 'home'" viewBox="0 0 24 24"><path d="M4 11.5 12 5l8 6.5V20a1 1 0 0 1-1 1h-4.5v-5h-5v5H5a1 1 0 0 1-1-1z"/></svg>
    <svg v-else-if="name === 'battery'" viewBox="0 0 24 24"><rect x="4" y="7" width="14" height="10" rx="2.2"/><path d="M18 10h2.2v4H18"/><path d="M7.2 13.4h2.6l1.1-3 1.8 5 1-2h2.1"/></svg>
    <svg v-else-if="name === 'assessment'" viewBox="0 0 24 24"><path d="M5 18h14"/><path d="M7 15l3-4 3 2 4-6"/></svg>
    <svg v-else-if="name === 'market'" viewBox="0 0 24 24"><path d="M6 7h12l-1 12H7z"/><path d="M9 7a3 3 0 1 1 6 0"/></svg>
    <svg v-else-if="name === 'logistics'" viewBox="0 0 24 24"><path d="M3 8h11v8H3z"/><path d="M14 11h3l2 2v3h-5z"/><circle cx="8" cy="18" r="1.5"/><circle cx="18" cy="18" r="1.5"/></svg>
    <svg v-else-if="name === 'dashboard'" viewBox="0 0 24 24"><path d="M4 13h6V4H4z"/><path d="M14 20h6v-9h-6z"/><path d="M14 10h6V4h-6z"/><path d="M4 20h6v-5H4z"/></svg>
    <svg v-else-if="name === 'users'" viewBox="0 0 24 24"><circle cx="9" cy="9" r="3"/><circle cx="17" cy="10" r="2.5"/><path d="M4 19a5 5 0 0 1 10 0"/><path d="M14 19a4 4 0 0 1 6 0"/></svg>
    <svg v-else-if="name === 'products'" viewBox="0 0 24 24"><path d="M5 7h14v12H5z"/><path d="M5 10h14"/><path d="M9 5h6v4H9z"/></svg>
    <svg v-else-if="name === 'orders'" viewBox="0 0 24 24"><path d="M6 5h12v14H6z"/><path d="M9 9h6"/><path d="M9 13h6"/><path d="M9 17h4"/></svg>
    <svg v-else-if="name === 'contracts'" viewBox="0 0 24 24"><path d="M7 4h8l4 4v12H7z"/><path d="M15 4v4h4"/><path d="M9 13h6"/><path d="M9 17h6"/></svg>
    <svg v-else-if="name === 'statistics'" viewBox="0 0 24 24"><path d="M5 19h14"/><path d="M8 17v-5"/><path d="M12 17V8"/><path d="M16 17v-8"/></svg>
    <svg v-else-if="name === 'settings'" viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.04.04a2 2 0 0 1-2.83 2.83l-.04-.04A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 1.55V21a2 2 0 0 1-4 0v-.05A1.7 1.7 0 0 0 9 19.4a1.7 1.7 0 0 0-1.88.34l-.04.04a2 2 0 1 1-2.83-2.83l.04-.04A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-1.55-1H3a2 2 0 0 1 0-4h.05A1.7 1.7 0 0 0 4.6 9a1.7 1.7 0 0 0-.34-1.88l-.04-.04a2 2 0 1 1 2.83-2.83l.04.04A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-1.55V3a2 2 0 0 1 4 0v.05A1.7 1.7 0 0 0 15 4.6a1.7 1.7 0 0 0 1.88-.34l.04-.04a2 2 0 1 1 2.83 2.83l-.04.04A1.7 1.7 0 0 0 19.4 9a1.7 1.7 0 0 0 1.55 1H21a2 2 0 0 1 0 4h-.05A1.7 1.7 0 0 0 19.4 15z"/></svg>
    <svg v-else-if="name === 'bell'" viewBox="0 0 24 24"><path d="M18 9a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/></svg>
    <svg v-else-if="name === 'assistant'" viewBox="0 0 24 24"><path d="M12 4v3"/><rect x="5" y="7" width="14" height="11" rx="4"/><circle cx="9.5" cy="12.5" r="1"/><circle cx="14.5" cy="12.5" r="1"/><path d="M9.5 16h5"/><path d="M4 12h1"/><path d="M19 12h1"/></svg>
    <svg v-else viewBox="0 0 24 24"><circle cx="12" cy="12" r="7"/></svg>
  </span>
</template>

<script setup>
defineProps({
  name: {
    type: String,
    default: 'dot'
  }
})
</script>

<style scoped>
.nav-icon {
  display: inline-flex;
  width: 1.25em;
  height: 1.25em;
  align-items: center;
  justify-content: center;
}

.nav-icon svg {
  width: 100%;
  height: 100%;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
