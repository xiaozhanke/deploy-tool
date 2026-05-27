<script setup>
import { ref } from 'vue'

const app = ref({
    name: 'sample-app-frontend',
    version: '1.0.0',
    message: 'Hello, deployed by deploy-tool',
})
const buildTime = new Date().toISOString()
</script>

<template>
    <main class="container">
        <h1>{{ app.message }}</h1>
        <ul class="meta">
            <li><strong>name:</strong> {{ app.name }}</li>
            <li><strong>version:</strong> {{ app.version }}</li>
            <li><strong>buildTime:</strong> {{ buildTime }}</li>
        </ul>
    </main>
</template>

<style scoped>
.container {
    max-width: 640px;
    margin: 4rem auto;
    padding: 2rem;
    font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, sans-serif;
    color: #222;
    background: #f7f8fa;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}
h1 {
    margin: 0 0 1rem;
    font-size: 1.6rem;
}
.meta {
    list-style: none;
    padding: 0;
    line-height: 1.8;
}
.meta li strong {
    display: inline-block;
    width: 6em;
    color: #555;
}
</style>
