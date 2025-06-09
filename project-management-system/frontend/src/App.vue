<template>
  <div id="app">
    <router-view />
    <!-- stagewise 开发工具栏 -->
    <StagewiseDevTool />
  </div>
</template>

<script setup lang="ts">
import StagewiseDevTool from './components/StagewiseDevTool.vue'

// 主应用组件
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  background-color: #f5f5f5;
}
</style>