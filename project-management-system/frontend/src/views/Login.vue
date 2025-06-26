<template>
    <div class="login-container" :class="[`${themeStore.actualTheme}-theme`]">
        <div class="login-card">
            <div class="login-header">
                <h1>项目管理系统</h1>
                <p>请输入您的登录信息</p>
            </div>

            <a-form :model="loginForm" :rules="loginRules" ref="loginFormRef" layout="vertical" @submit="handleLogin">
                <a-form-item label="用户名" field="username" required>
                    <a-input v-model="loginForm.username" placeholder="请输入用户名" size="large"
                        :prefix="() => h(IconUser)" />
                </a-form-item>

                <a-form-item label="密码" field="password" required>
                    <a-input-password v-model="loginForm.password" placeholder="请输入密码（默认：0000）" size="large"
                        :prefix="() => h(IconLock)" />
                </a-form-item>

                <a-form-item>
                    <a-button type="primary" html-type="submit" size="large" long :loading="loading">
                        登录
                    </a-button>
                </a-form-item>
            </a-form>

            <div class="login-tips">
                <a-alert type="info" show-icon>
                    <template #title>登录提示</template>
                    <div>
                        <p>• 所有用户的默认密码为：<strong>0000</strong></p>
                        <p>• 管理员账户：<strong>barlin.zhang</strong></p>
                        <p>• 普通用户只能查看与自己相关的待办</p>
                    </div>
                </a-alert>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, h } from 'vue'
import { useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { IconUser, IconLock } from '@arco-design/web-vue/es/icon'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

// 路由实例
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

// 响应式数据
const loading = ref(false)
const loginFormRef = ref()

const loginForm = ref({
    username: '',
    password: ''
})

// 表单验证规则
const loginRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 2, message: '用户名至少2个字符', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { len: 4, message: '密码为4位数字', trigger: 'blur' }
    ]
}

// 登录处理
const handleLogin = async (data: any) => {
    console.log('🚀 开始登录处理:', data.values)

    try {
        loading.value = true

        // 验证密码是否为0000
        if (loginForm.value.password !== '0000') {
            Message.error('密码错误，请输入：0000')
            return
        }

        // 调用登录API
        const result = await userStore.login(loginForm.value.username, loginForm.value.password)

        if (result.success) {
            Message.success('登录成功！')
            // 跳转到主页面
            router.push('/dashboard')
        } else {
            Message.error(result.message || '登录失败')
        }

    } catch (error: any) {
        console.error('登录失败:', error)
        Message.error(error.message || '登录失败，请重试')
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.login-container {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--bg-color);
    padding: 20px;
    position: relative;
}

.login-container::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, var(--primary-color) 0%, var(--theme-primary-dark) 100%);
    opacity: 0.1;
}

.login-card {
    width: 100%;
    max-width: 400px;
    background: var(--card-bg-color);
    border-radius: 12px;
    box-shadow: var(--card-shadow);
    border: 1px solid var(--border-color);
    padding: 40px;
    position: relative;
    z-index: 1;
}

.login-header {
    text-align: center;
    margin-bottom: 32px;
}

.login-header h1 {
    margin: 0 0 8px 0;
    color: var(--text-color);
    font-size: 28px;
    font-weight: 600;
}

.login-header p {
    margin: 0;
    color: var(--text-muted);
    font-size: 14px;
}

.login-tips {
    margin-top: 24px;
}

.login-tips p {
    margin: 4px 0;
    font-size: 13px;
}

:deep(.arco-form-item-label) {
    color: var(--text-color);
    font-weight: 500;
}

:deep(.arco-input-wrapper) {
    border-radius: 8px;
    background-color: var(--theme-bg-secondary) !important;
    border-color: var(--border-color);
    color: var(--text-color);
}

:deep(.arco-btn-primary) {
    border-radius: 8px;
    height: 44px;
    font-size: 16px;
    font-weight: 500;
    background-color: var(--primary-color);
    border-color: var(--primary-color);
}

:deep(.arco-btn-primary:hover) {
    background-color: var(--theme-primary-hover);
    border-color: var(--theme-primary-hover);
}

:deep(.arco-input-wrapper:hover) {
    border-color: var(--border-hover-color);
}

:deep(.arco-input-wrapper.arco-input-focus) {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 2px var(--primary-color-light);
}

:deep(.arco-input) {
    color: var(--text-color) !important;
    background-color: var(--theme-bg-secondary) !important;
}

:deep(.arco-input-password .arco-input) {
    color: var(--text-color) !important;
    background-color: var(--theme-bg-secondary) !important;
}

:deep(.arco-input::placeholder) {
    color: var(--text-muted);
}

:deep(.arco-alert) {
    background-color: var(--theme-info-bg);
    border-color: var(--theme-info);
}

:deep(.arco-alert-info .arco-alert-title) {
    color: var(--text-color);
}

:deep(.arco-alert-info .arco-alert-content) {
    color: var(--text-muted);
}

/* 强制覆盖输入框样式 */
:deep(.arco-input-wrapper .arco-input) {
    background-color: var(--theme-bg-secondary) !important;
    color: var(--text-color) !important;
}

:deep(.arco-input-password-wrapper .arco-input) {
    background-color: var(--theme-bg-secondary) !important;
    color: var(--text-color) !important;
}

/* 修复输入框内容区域 */
:deep(.arco-input-inner) {
    background-color: var(--theme-bg-secondary) !important;
    color: var(--text-color) !important;
}

/* 修复浏览器自动填充样式 */
:deep(.arco-input:-webkit-autofill) {
    -webkit-box-shadow: 0 0 0 1000px var(--theme-bg-secondary) inset !important;
    -webkit-text-fill-color: var(--text-color) !important;
    background-color: var(--theme-bg-secondary) !important;
    transition: background-color 5000s ease-in-out 0s;
}

:deep(.arco-input:-webkit-autofill:hover) {
    -webkit-box-shadow: 0 0 0 1000px var(--theme-bg-secondary) inset !important;
    -webkit-text-fill-color: var(--text-color) !important;
    background-color: var(--theme-bg-secondary) !important;
}

:deep(.arco-input:-webkit-autofill:focus) {
    -webkit-box-shadow: 0 0 0 1000px var(--theme-bg-secondary) inset !important;
    -webkit-text-fill-color: var(--text-color) !important;
    background-color: var(--theme-bg-secondary) !important;
}

:deep(.arco-input:-webkit-autofill:active) {
    -webkit-box-shadow: 0 0 0 1000px var(--theme-bg-secondary) inset !important;
    -webkit-text-fill-color: var(--text-color) !important;
    background-color: var(--theme-bg-secondary) !important;
}
</style>