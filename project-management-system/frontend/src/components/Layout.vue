<template>
  <div class="layout-container page-container">
    <a-layout class="layout-basic">
      <a-layout-sider :width="240" class="layout-sider">
        <div class="logo">
          <h2>项目管理系统</h2>
        </div>
        <a-menu :selected-keys="[currentRoute]" mode="vertical"
          :style="{ height: 'calc(100vh - 60px)', borderRight: 'none' }" @menu-item-click="handleMenuClick">
          <a-menu-item key="dashboard">
            <template #icon>
              <icon-dashboard />
            </template>
            仪表盘
          </a-menu-item>
          <a-menu-item key="projects">
            <template #icon>
              <icon-folder />
            </template>
            项目列表
          </a-menu-item>
          <a-menu-item key="todos">
            <template #icon>
              <icon-calendar />
            </template>
            待办任务
          </a-menu-item>
          <a-menu-item key="reports" v-if="shouldShowMenuItem('reports')">
            <template #icon>
              <icon-file />
            </template>
            项目报告
          </a-menu-item>
          <a-menu-item key="email-rules" v-if="shouldShowMenuItem('email-rules')">
            <template #icon>
              <icon-email />
            </template>
            邮件规则管理
          </a-menu-item>
          <a-menu-item key="data-dashboard" v-if="shouldShowMenuItem('data-dashboard')">
            <template #icon>
              <icon-bar-chart />
            </template>
            数据看板
          </a-menu-item>
          <a-menu-item key="operation-logs" v-if="shouldShowMenuItem('operation-logs')">
            <template #icon>
              <icon-history />
            </template>
            操作日志
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <a-layout>
        <a-layout-header class="layout-header">
          <div class="header-content">
            <h1>{{ pageTitle }}</h1>
            <a-space>
              <a-button type="primary" v-if="showCreateButton" @click="handleCreate">
                <template #icon>
                  <icon-plus />
                </template>
                {{ createButtonText }}
              </a-button>

              <!-- 主题切换按钮 -->
              <theme-toggle mode="dropdown" />

              <!-- 用户信息和登出 -->
              <a-dropdown>
                <a-avatar>
                  {{ userStore.currentUser?.username?.charAt(0).toUpperCase() || 'U' }}
                </a-avatar>
                <template #content>
                  <a-doption>
                    <div style="padding: 8px;">
                      <div style="font-weight: 500;">{{ userStore.currentUser?.nickname ||
                        userStore.currentUser?.username }}</div>
                      <div style="font-size: 12px; color: #999;">{{ userStore.currentUser?.email }}</div>
                      <div v-if="userStore.isAdmin" style="font-size: 12px; color: #1890ff;">管理员</div>
                    </div>
                  </a-doption>
                  <a-doption @click="handleLogout">
                    <template #icon>
                      <icon-poweroff />
                    </template>
                    退出登录
                  </a-doption>
                </template>
              </a-dropdown>
            </a-space>
          </div>
        </a-layout-header>

        <a-layout-content class="layout-content">
          <div class="content-container">
            <router-view />
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  IconDashboard,
  IconFolder,
  IconCalendar,
  IconFile,
  IconPlus,
  IconPoweroff,
  IconEmail,
  IconBarChart,
  IconHistory
} from '@arco-design/web-vue/es/icon'
import { useUserStore } from '../stores/user'
import ThemeToggle from './ThemeToggle.vue'

// 路由实例
const router = useRouter()
const route = useRoute()

// 当前路由
const currentRoute = computed(() => {
  const path = route.path
  if (path === '/' || path === '/dashboard') return 'dashboard'
  if (path.startsWith('/projects')) return 'projects'
  if (path.startsWith('/todos')) return 'todos'
  if (path.startsWith('/reports')) return 'reports'
  if (path.startsWith('/email-rules')) return 'email-rules'
  if (path.startsWith('/data-dashboard')) return 'data-dashboard'
  if (path.startsWith('/operation-logs')) return 'operation-logs'
  return 'dashboard'
})

// 页面标题
const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    'dashboard': '仪表盘',
    'projects': '项目管理',
    'todos': '待办任务',
    'reports': '项目报告',
    'email-rules': '邮件规则管理',
    'data-dashboard': '数据看板',
    'operation-logs': '操作日志'
  }
  return titles[currentRoute.value] || '项目管理系统'
})

// 判断是否为 barlin.zhang 用户
const isBarlinZhang = computed(() => {
  return userStore.currentUser?.username === 'barlin.zhang'
})

// 根据路由配置判断是否显示菜单项
const shouldShowMenuItem = (menuKey: string) => {
  // 查找对应的路由配置
  const matchedRoute = router.getRoutes().find(route => {
    if (route.children) {
      return route.children.some(child => child.path === menuKey)
    }
    return false
  })

  if (matchedRoute) {
    const childRoute = matchedRoute.children?.find(child => child.path === menuKey)
    if (childRoute?.meta?.showInMenu === true) {
      return true // 配置为true时，所有用户都显示
    }
    if (childRoute?.meta?.showInMenu === false) {
      return isBarlinZhang.value // 配置为false时，只有管理员显示
    }
  }

  return true // 默认显示
}

// 创建按钮
const showCreateButton = computed(() => {
  const route = currentRoute.value
  if (['reports', 'email-rules'].includes(route)) {
    // 报告和邮件规则只有 barlin.zhang 能看到
    return isBarlinZhang.value
  }
  return ['projects', 'todos'].includes(route)
})

const createButtonText = computed(() => {
  const texts: Record<string, string> = {
    'projects': '新建项目',
    'todos': '新建任务',
    'reports': '生成报告',
    'email-rules': '新建规则'
  }
  return texts[currentRoute.value] || '新建'
})

// 菜单点击事件
const handleMenuClick = (key: string) => {
  const routes: Record<string, string> = {
    'dashboard': '/dashboard',
    'projects': '/projects',
    'todos': '/todos',
    'reports': '/reports',
    'email-rules': '/email-rules',
    'data-dashboard': '/data-dashboard',
    'operation-logs': '/operation-logs'
  }

  if (routes[key] && route.path !== routes[key]) {
    router.push(routes[key])
  }
}

// 创建按钮点击事件
const handleCreate = () => {
  console.log('🚀 头部新建按钮被点击，当前路由:', currentRoute.value)
  // 向子组件发送事件，由具体页面处理
  const eventDetail = { type: currentRoute.value }
  console.log('📤 发送事件 header-create-click，detail:', eventDetail)
  window.dispatchEvent(new CustomEvent('header-create-click', {
    detail: eventDetail
  }))
}

// 用户商店实例
const userStore = useUserStore()

// 处理登出
const handleLogout = async () => {
  console.log('🚀 用户点击了退出登录')
  try {
    // 调用用户商店的 logout 方法
    await userStore.logout()
    // 跳转到登录页面
    router.push('/login')
  } catch (error) {
    console.error('❌ 登出过程中出错:', error)
    // 即使出错也要跳转到登录页面
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: #f5f5f5;
}

.layout-basic {
  height: 100vh;
}

.layout-sider {
  background: #fff;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.08);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #f0f0f0;
}

.logo h2 {
  color: #1890ff;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 0 24px;
  height: 64px;
  line-height: 64px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.header-content h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #262626;
}

.layout-content {
  background: #f5f5f5;
  padding: 24px;
  overflow: auto;
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
}
</style>