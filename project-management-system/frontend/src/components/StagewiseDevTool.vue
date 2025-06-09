<template>
    <!-- 开发环境：显示开发工具 -->
    <div v-if="showDevTools">
        <component v-if="toolbarComponent" :is="toolbarComponent" :config="stagewiseConfig" />
    </div>
    <!-- 生产环境：什么都不渲染 -->
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import type { Component } from 'vue'

// 开发工具组件引用
const toolbarComponent = ref<Component | null>(null)

// 是否显示开发工具
const showDevTools = computed(() => {
    return typeof __ENABLE_DEV_TOOLS__ !== 'undefined' && __ENABLE_DEV_TOOLS__
})

// stagewise 配置
const stagewiseConfig = {
    plugins: []
}

// 条件编译：只在开发环境包含这段代码
if (typeof __ENABLE_DEV_TOOLS__ !== 'undefined' && __ENABLE_DEV_TOOLS__) {
    onMounted(async () => {
        try {
            // 动态导入开发工具
            const module = await import('@stagewise/toolbar-vue')
            toolbarComponent.value = module.StagewiseToolbar
            console.log('🔧 StagewiseToolbar 开发工具已加载')
        } catch (error) {
            console.warn('⚠️ StagewiseToolbar 加载失败:', (error as Error).message)
            console.info('💡 请安装开发工具: npm install @stagewise/toolbar-vue')
        }
    })
}
</script>

<style scoped>
/* 确保工具栏不影响主应用样式 */
</style>