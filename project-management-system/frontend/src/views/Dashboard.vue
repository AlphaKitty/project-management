<template>
  <div class="dashboard-content">
    <!-- 统计卡片 -->
    <a-row :gutter="24" class="stats-row">
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic title="总项目数" :value="projectStore.projectCount" :value-style="{ color: '#1890ff' }">
            <template #prefix>
              <icon-folder />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic title="进行中项目" :value="projectStore.activeProjects.length" :value-style="{ color: '#52c41a' }">
            <template #prefix>
              <icon-play-circle />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic title="待办任务" :value="todoStore.inProgressTodos.length" :value-style="{ color: '#faad14' }">
            <template #prefix>
              <icon-calendar />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card class="stats-card">
          <a-statistic title="已完成任务" :value="todoStore.completedTodos.length" :value-style="{ color: '#52c41a' }">
            <template #prefix>
              <icon-check-circle />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 项目列表和待办任务 -->
    <a-row :gutter="24" class="content-row">
      <a-col :span="16">
        <a-card title="项目概览" class="project-overview">
          <div class="project-item" v-for="project in projectStore.projects.slice(0, 5)" :key="project.id">
            <div class="project-info">
              <h3>{{ project.name }}</h3>
              <p>{{ project.description || '暂无描述' }}</p>
              <a-progress :percent="project.progress / 100" />
            </div>
            <div class="project-status">
              <a-tag :color="getStatusColor(project.status)">
                {{ getStatusText(project.status) }}
              </a-tag>
            </div>
          </div>

          <div v-if="projectStore.projects.length === 0" class="empty-state">
            <icon-folder style="font-size: 48px; color: #c0c4cc;" />
            <p>暂无项目数据</p>
          </div>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card :title="`待办任务 (${sortedPendingTodos.length})`" class="todo-card">
          <div class="todo-item" v-for="todo in sortedPendingTodos.slice(0, 8)" :key="todo.id">
            <div class="todo-content">
              <div class="todo-header">
                <span class="todo-title">{{ todo.title }}</span>
                <a-button size="small" :type="getStatusButtonType(todo.status)" @click="toggleTodoStatus(todo)"
                  class="status-button">
                  {{ getStatusText(todo.status) }}
                </a-button>
              </div>

              <div class="todo-details">
                <div class="todo-project-description">
                  <span class="todo-project">{{ getProjectName(todo.projectId) }}</span>
                  <span v-if="todo.description" class="todo-description">{{ todo.description }}</span>
                </div>

                <div class="todo-meta">
                  <a-tag size="small" :color="getPriorityColor(todo.priority)">
                    {{ getPriorityText(todo.priority) }}
                  </a-tag>
                  <span v-if="todo.dueDate" class="todo-due-date" :class="{ 'overdue': isOverdue(todo.dueDate) }">
                    {{ formatDueDate(todo.dueDate) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div v-if="sortedPendingTodos.length === 0" class="empty-state">
            <icon-calendar style="font-size: 48px; color: #c0c4cc;" />
            <p>暂无待办任务</p>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import {
  IconFolder,
  IconPlayCircle,
  IconCheckCircle,
  IconCalendar
} from '@arco-design/web-vue/es/icon'
import { useProjectStore } from '@/stores/projects'
import { useTodoStore } from '@/stores/todos'
import { useUserStore } from '@/stores/user'
import { StatusLabels, StatusColors } from '@/types'
import type { Todo } from '@/types'

// Store
const projectStore = useProjectStore()
const todoStore = useTodoStore()
const userStore = useUserStore()

// 计算属性：排序后的待办任务
const sortedPendingTodos = computed(() => {
  // 获取所有未完成的任务（TODO 和 PROGRESS 状态）
  const pendingTodos = todoStore.todos.filter(
    todo => todo.status === 'TODO' || todo.status === 'PROGRESS'
  )

  // 按优先级和截止时间排序
  return pendingTodos.sort((a, b) => {
    // 优先级排序：HIGH > MEDIUM > LOW
    const priorityOrder = { HIGH: 3, MEDIUM: 2, LOW: 1 }
    const aPriority = priorityOrder[a.priority as keyof typeof priorityOrder] || 0
    const bPriority = priorityOrder[b.priority as keyof typeof priorityOrder] || 0

    if (aPriority !== bPriority) {
      return bPriority - aPriority // 高优先级在前
    }

    // 优先级相同时按截止时间排序
    if (a.dueDate && b.dueDate) {
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime() // 截止时间近的在前
    }

    // 有截止时间的在前
    if (a.dueDate && !b.dueDate) return -1
    if (!a.dueDate && b.dueDate) return 1

    // 都没有截止时间时按创建时间排序
    return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
  })
})

// 格式化截止日期
const formatDueDate = (dueDate: string) => {
  const date = new Date(dueDate)
  const now = new Date()
  const diffTime = date.getTime() - now.getTime()
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays < 0) {
    return `逾期${Math.abs(diffDays)}天`
  } else if (diffDays === 0) {
    return '今天截止'
  } else if (diffDays === 1) {
    return '明天截止'
  } else if (diffDays <= 7) {
    return `${diffDays}天后`
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}

// 判断是否逾期
const isOverdue = (dueDate: string) => {
  const date = new Date(dueDate)
  const now = new Date()
  return date.getTime() < now.getTime()
}

// 获取项目名称
const getProjectName = (projectId: number | null | undefined) => {
  if (!projectId) return '无项目'
  const project = projectStore.projects.find(p => p.id === projectId)
  return project ? project.name : '未知项目'
}

// 获取状态按钮类型
const getStatusButtonType = (status: string) => {
  switch (status) {
    case 'TODO': return 'outline'
    case 'PROGRESS': return 'primary'
    case 'DONE': return 'secondary'
    default: return 'outline'
  }
}

// 状态映射
const getStatusColor = (status: string) => {
  return StatusColors[status as keyof typeof StatusColors] || 'gray'
}

const getStatusText = (status: string) => {
  return StatusLabels[status as keyof typeof StatusLabels] || status
}

const getPriorityColor = (priority: string) => {
  return StatusColors[priority as keyof typeof StatusColors] || 'gray'
}

const getPriorityText = (priority: string) => {
  return StatusLabels[priority as keyof typeof StatusLabels] || priority
}

// 切换待办状态
const toggleTodoStatus = async (todo: Todo) => {
  let newStatus: string
  // 循环状态：TODO -> PROGRESS -> DONE -> TODO
  switch (todo.status) {
    case 'TODO':
      newStatus = 'PROGRESS'
      break
    case 'PROGRESS':
      newStatus = 'DONE'
      break
    case 'DONE':
      newStatus = 'TODO'
      break
    default:
      newStatus = 'PROGRESS'
  }

  // 显示确认对话框
  Modal.confirm({
    title: '确认状态变更',
    content: `确定要将任务「${todo.title}」的状态更改为「${getStatusText(newStatus)}」吗？`,
    okText: '确认',
    cancelText: '取消',
    onOk: async () => {
      try {
        await todoStore.updateStatus(todo.id, newStatus)
        Message.success(`任务状态已更新为：${getStatusText(newStatus)}`)
      } catch (error) {
        Message.error('任务状态更新失败')
      }
    }
  })
}

// 页面加载时获取数据
onMounted(async () => {
  try {
    await Promise.all([
      projectStore.fetchProjects(),
      todoStore.fetchTodos()
      // 移除 fetchTodayTodos()，因为我们现在使用 sortedPendingTodos 计算属性
    ])
  } catch (error) {
    Message.error('数据加载失败')
  }
})
</script>

<style scoped>
.dashboard-content {
  padding: 0;
}

.stats-row {
  margin-bottom: 24px;
}

.stats-card {
  text-align: center;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  transition: all 0.3s;
}

.stats-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.content-row {
  margin-bottom: 24px;
}

.project-overview {
  min-height: 600px;
  max-height: 600px;
}

/* 为项目概览卡片内容区域添加滚动条 */
.project-overview :deep(.arco-card-body) {
  max-height: 600px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条样式 */
.project-overview :deep(.arco-card-body)::-webkit-scrollbar {
  width: 6px;
}

.project-overview :deep(.arco-card-body)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.project-overview :deep(.arco-card-body)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.project-overview :deep(.arco-card-body)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.project-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.project-item:last-child {
  border-bottom: none;
}

.project-info {
  flex: 1;
}

.project-info h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
}

.project-info p {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
}

.project-status {
  margin-left: 16px;
}

.todo-card {
  min-height: 600px;
  max-height: 600px;
}

/* 为待办任务卡片内容区域添加滚动条 */
.todo-card :deep(.arco-card-body) {
  max-height: 600px;
  overflow-y: auto;
  padding-right: 8px;
}

/* 自定义滚动条样式 */
.todo-card :deep(.arco-card-body)::-webkit-scrollbar {
  width: 6px;
}

.todo-card :deep(.arco-card-body)::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.todo-card :deep(.arco-card-body)::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.todo-card :deep(.arco-card-body)::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

.todo-item {
  padding: 16px;
  margin-bottom: 12px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
  transition: all 0.2s ease;
}

.todo-item:hover {
  background: #f5f5f5;
  border-color: #d9d9d9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.todo-item:last-child {
  margin-bottom: 0;
}

.todo-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.todo-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.todo-title {
  font-weight: 500;
  color: #333;
  flex: 1;
}

.status-button {
  margin-left: 12px;
}

.todo-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 4px;
}

.todo-project-description {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.todo-project {
  font-size: 12px;
  color: #1890ff;
  font-weight: 500;
}

.todo-description {
  font-size: 12px;
  color: #666;
  line-height: 1.4;
}

.todo-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.todo-due-date {
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.todo-due-date.overdue {
  color: #ff4d4f;
  background: #fff2f0;
  font-weight: 500;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.empty-state p {
  margin: 12px 0 0 0;
  font-size: 14px;
}
</style>