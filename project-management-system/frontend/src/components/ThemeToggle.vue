<template>
    <div class="theme-toggle">
        <!-- 简单切换按钮模式 -->
        <a-button v-if="mode === 'button'" type="text" shape="circle" size="medium" @click="themeStore.toggleTheme()"
            class="theme-toggle-btn" :title="themeStore.getThemeLabel">
            <template #icon>
                <icon-sun v-if="themeStore.themeMode === 'light'" />
                <icon-moon v-else-if="themeStore.themeMode === 'dark'" />
                <icon-computer v-else />
            </template>
        </a-button>

        <!-- 下拉菜单模式 -->
        <a-dropdown v-else trigger="click" position="bottom">
            <a-button type="text" shape="circle" size="medium" class="theme-toggle-btn">
                <template #icon>
                    <icon-sun v-if="themeStore.themeMode === 'light'" />
                    <icon-moon v-else-if="themeStore.themeMode === 'dark'" />
                    <icon-computer v-else />
                </template>
            </a-button>

            <template #content>
                <a-doption v-for="themeOption in themeOptions" :key="themeOption.value"
                    @click="themeStore.setTheme(themeOption.value)"
                    :class="{ 'is-active': themeStore.themeMode === themeOption.value }">
                    <template #icon>
                        <component :is="themeOption.icon" />
                    </template>
                    <template #default>
                        {{ themeOption.label }}
                        <icon-check v-if="themeStore.themeMode === themeOption.value" class="check-icon" />
                    </template>
                </a-doption>
            </template>
        </a-dropdown>
    </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { IconSun, IconMoon, IconComputer, IconCheck } from '@arco-design/web-vue/es/icon'
import { useThemeStore, type ThemeMode } from '@/stores/theme'

interface Props {
    mode?: 'button' | 'dropdown'
}

const props = withDefaults(defineProps<Props>(), {
    mode: 'dropdown'
})

const themeStore = useThemeStore()

const themeOptions = computed(() => [
    {
        value: 'light' as ThemeMode,
        label: '浅色模式',
        icon: IconSun
    },
    {
        value: 'dark' as ThemeMode,
        label: '深色模式',
        icon: IconMoon
    },
    {
        value: 'auto' as ThemeMode,
        label: '跟随系统',
        icon: IconComputer
    }
])
</script>

<style scoped>
.theme-toggle {
    display: inline-block;
}

.theme-toggle-btn {
    transition: all 0.3s ease;
}

.theme-toggle-btn:hover {
    background-color: var(--color-fill-2);
}

:deep(.arco-dropdown-option.is-active) {
    background-color: var(--color-primary-light-1);
    color: var(--color-primary-6);
}

:deep(.arco-dropdown-option) {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 12px;
}

.check-icon {
    margin-left: 8px;
    color: var(--color-primary-6);
    font-size: 14px;
}

.theme-toggle-btn {
    position: relative;
    overflow: hidden;
}

.theme-toggle-btn::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: radial-gradient(circle, var(--color-primary-light-3) 0%, transparent 70%);
    transition: all 0.6s ease;
    transform: translate(-50%, -50%);
    border-radius: 50%;
    opacity: 0;
    z-index: 0;
}

.theme-toggle-btn:active::before {
    width: 200px;
    height: 200px;
    opacity: 1;
}

:deep(.arco-btn-icon) {
    position: relative;
    z-index: 1;
    transition: transform 0.3s ease;
}

.theme-toggle-btn:active :deep(.arco-btn-icon) {
    transform: scale(0.9);
}
</style>