<template>
  <div class="projects-page">

    <!-- 项目统计卡片 -->
    <div class="stats-cards">
      <a-card class="stat-card">
        <a-statistic title="总项目数" :value="projectStore.projectCount" />
      </a-card>
      <a-card class="stat-card">
        <a-statistic title="进行中" :value="projectStore.activeProjects.length" />
      </a-card>
      <a-card class="stat-card">
        <a-statistic title="已完成" :value="projectStore.completedProjects.length" />
      </a-card>
    </div>

    <!-- 操作按钮区域 -->
    <div class="action-section">
      <a-button type="primary" @click="showOverviewModal">
        <template #icon><icon-eye /></template>
        项目概览
      </a-button>
    </div>

    <!-- 项目列表 -->
    <a-card title="项目列表" class="project-list-card">
      <a-table :columns="columns" :data="projectStore.projects" :loading="projectStore.loading"
        :pagination="{ pageSize: 50 }">
        <template #status="{ record }">
          <a-tag :color="getStatusColor(record.status)">
            {{ getStatusLabel(record.status) }}
          </a-tag>
        </template>

        <template #progress="{ record }">
          <a-progress :percent="record.progress / 100 || 0" size="small" />
        </template>

        <template #creator="{ record }">
          {{ getCreatorName(record) }}
        </template>

        <template #assignee="{ record }">
          {{ getAssigneeName(record) }}
        </template>

        <template #actions="{ record }">
          <a-button-group size="small">
            <a-button @click="editProject(record)">编辑</a-button>
            <a-button status="danger" @click="deleteProject(record)">删除</a-button>
          </a-button-group>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/编辑项目模态框 -->
    <a-modal v-model:visible="modalVisible" :title="isEdit ? '编辑项目' : '新建项目'" @ok="handleSubmit" @cancel="handleCancel"
      width="900px">
      <a-form :model="formData" layout="vertical">
        <a-form-item label="项目名称" required>
          <a-input v-model="formData.name" placeholder="请输入项目名称" />
        </a-form-item>

        <a-form-item label="项目描述">
          <a-textarea v-model="formData.description" placeholder="请输入项目描述" />
        </a-form-item>

        <a-form-item label="项目状态">
          <a-select v-model="formData.status" placeholder="请选择项目状态">
            <a-option value="PENDING">待启动</a-option>
            <a-option value="PROGRESS">进行中</a-option>
            <a-option value="COMPLETED">已完成</a-option>
            <a-option value="CANCELLED">已取消</a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="开始日期">
          <a-date-picker v-model="formData.startDate" style="width: 100%" />
        </a-form-item>

        <a-form-item label="结束日期">
          <a-date-picker v-model="formData.endDate" style="width: 100%" />
        </a-form-item>

        <a-form-item label="责任人">
          <a-select v-model="formData.assigneeId" placeholder="请输入关键字搜索责任人" allow-search allow-clear
            :filter-option="false" :loading="userStore.loading" @search="handleUserSearch">
            <template #empty>
              <div style="text-align: center; padding: 20px; color: #999;">
                {{ userSearchText
                  ? (userSearchText.length < 2 ? '请输入至少2个字符进行搜索' : '无搜索结果') : '请输入关键字搜索用户' }} </div>
            </template>
            <a-option v-for="user in searchResultUsers" :key="user.id" :value="user.id">
              <div class="user-option-inline">{{ user.username }}-{{ user.id }}-{{ user.nickname }}</div>
            </a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="项目进度">
          <a-slider v-model="formData.progress" :max="100" :show-tooltip="true" />
          <div class="progress-display">{{ formData.progress }}%</div>
        </a-form-item>

        <a-form-item label="项目里程碑">
          <div class="milestones-editor">
            <div v-for="(milestone, index) in currentMilestones" :key="index" class="milestone-item">
              <a-row :gutter="12" align="center">
                <a-col :span="6">
                  <a-input v-model="milestone.name" placeholder="里程碑名称" size="small" />
                </a-col>
                <a-col :span="4">
                  <a-select v-model="milestone.status" size="small">
                    <a-option value="PENDING">待开始</a-option>
                    <a-option value="PROGRESS">进行中</a-option>
                    <a-option value="COMPLETED">已完成</a-option>
                  </a-select>
                </a-col>
                <a-col :span="5">
                  <a-date-picker v-model="milestone.dueDate" size="small" style="width: 100%" />
                </a-col>
                <a-col :span="6">
                  <a-input v-model="milestone.description" placeholder="描述" size="small" />
                </a-col>
                <a-col :span="3">
                  <a-button size="small" status="danger" @click="removeMilestone(index)">删除</a-button>
                </a-col>
              </a-row>
            </div>
            <a-button type="dashed" @click="addMilestone" style="width: 100%; margin-top: 8px;">
              <template #icon><icon-plus /></template>
              添加里程碑
            </a-button>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 项目概览模态框 -->
    <a-modal v-model:visible="overviewModalVisible" title="项目概览" width="1200px" :footer="false">
      <div class="overview-content">
        <a-table :columns="overviewColumns" :data="projectStore.overviewProjects" :loading="projectStore.loading"
          :pagination="{ pageSize: 1000 }">
          <template #status="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusLabel(record.status) }}
            </a-tag>
          </template>

          <template #progress="{ record }">
            <a-progress :percent="record.progress / 100 || 0" size="small" />
          </template>

          <template #creator="{ record }">
            {{ getCreatorName(record) }}
          </template>

          <template #assignee="{ record }">
            {{ getAssigneeName(record) }}
          </template>

          <template #milestones="{ record }">
            <div class="milestones-display" v-if="getMilestones(record).length > 0">
              <div v-for="(milestone, index) in getMilestones(record)" :key="index" class="milestone-row">
                <a-tag :color="getMilestoneColor(milestone.status)" size="small" class="milestone-tag">
                  {{ milestone.name }}
                </a-tag>
                <span class="milestone-date" v-if="milestone.dueDate">
                  {{ milestone.dueDate }}
                </span>
              </div>
            </div>
            <span v-else class="text-gray-400">暂无里程碑</span>
          </template>

          <template #actions="{ record }">
            <a-button size="small" @click="editProjectWithMilestones(record)">编辑</a-button>
          </template>
        </a-table>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconEye } from '@arco-design/web-vue/es/icon'
import { useProjectStore } from '@/stores/projects'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todos'
import { StatusLabels, StatusColors } from '@/types'
import type { Project, ProjectDTO, User, Milestone } from '@/types'

// Store
const projectStore = useProjectStore()
const userStore = useUserStore()
const todoStore = useTodoStore()

// 响应式数据
const modalVisible = ref(false)
const overviewModalVisible = ref(false)
const isEdit = ref(false)
const formData = ref<ProjectDTO>({
  name: '',
  description: '',
  status: 'PROGRESS',
  startDate: '',
  endDate: '',
  progress: 0,
  assigneeId: undefined
})

// 里程碑相关数据
const currentMilestones = ref<Milestone[]>([])

// 用户搜索相关的响应式数据
const userSearchText = ref('')
const searchResultUsers = ref<User[]>([])

// 用户搜索缓存
const userSearchCache = new Map<string, User[]>()
let searchTimer: NodeJS.Timeout | null = null

// 表格列配置
const columns = [
  { title: '项目名称', dataIndex: 'name', key: 'name', width: 180, align: 'center', sortable: { sortDirections: ['ascend', 'descend'] } },
  {
    title: '待办数', dataIndex: 'todoCount', key: 'todoCount', width: 90, align: 'center',
    render: ({ record }: { record: Project }) => getTodoCount(record),
  },
  { title: '状态', dataIndex: 'status', key: 'status', slotName: 'status', width: 90, align: 'center' },
  { title: '进度', dataIndex: 'progress', key: 'progress', slotName: 'progress', width: 100, align: 'center', sortable: { sortDirections: ['ascend', 'descend'] } },
  { title: '创建人', dataIndex: 'creator', key: 'creator', slotName: 'creator', width: 90, align: 'center' },
  { title: '责任人', dataIndex: 'assignee', key: 'assignee', slotName: 'assignee', width: 90, align: 'center' },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110, align: 'center', sortable: { sortDirections: ['ascend', 'descend'] } },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110, align: 'center', sortable: { sortDirections: ['ascend', 'descend'] } },
  { title: '操作', key: 'actions', slotName: 'actions', width: 130, align: 'center', fixed: 'right' }
]

// 项目概览表格列配置
const overviewColumns = [
  { title: '项目名称', dataIndex: 'name', key: 'name', width: 150, align: 'center' },
  { title: '状态', dataIndex: 'status', key: 'status', slotName: 'status', width: 80, align: 'center' },
  { title: '进度', dataIndex: 'progress', key: 'progress', slotName: 'progress', width: 120, align: 'center' },
  { title: '创建人', dataIndex: 'creator', key: 'creator', slotName: 'creator', width: 80, align: 'center' },
  { title: '责任人', dataIndex: 'assignee', key: 'assignee', slotName: 'assignee', width: 80, align: 'center' },
  { title: '里程碑', dataIndex: 'milestones', key: 'milestones', slotName: 'milestones', width: 100, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 100, align: 'center' },
  { title: '操作', key: 'actions', slotName: 'actions', width: 80, align: 'center' }
]

// 获取状态标签
const getStatusLabel = (status: string) => {
  return StatusLabels[status as keyof typeof StatusLabels] || status
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  return StatusColors[status as keyof typeof StatusColors] || 'gray'
}

// 获取创建人名称
const getCreatorName = (project: Project) => {
  // 优先使用后端返回的creator对象
  if (project.creator) {
    return project.creator.nickname || project.creator.username || '未知'
  }

  // 如果没有creator对象，显示创建人ID
  if (project.creatorId) {
    return `用户${project.creatorId}`
  }

  return '未知'
}

// 获取责任人名称
const getAssigneeName = (project: Project) => {
  // 优先使用后端返回的assignee对象
  if (project.assignee) {
    return project.assignee.nickname || project.assignee.username || '未知'
  }

  // 如果没有assignee对象但有assigneeId，显示责任人ID
  if (project.assigneeId) {
    return `用户${project.assigneeId}`
  }

  return '未分配'
}

// 统计每个项目的待办数
const getTodoCount = (project: Project) => {
  return todoStore.todos.filter(todo =>
    todo.projectId === project.id &&
    (todo.status === 'TODO' || todo.status === 'PROGRESS')
  ).length
}

// 用户搜索处理（防抖 + 缓存优化）
const handleUserSearch = (searchText: string) => {
  userSearchText.value = searchText

  // 清除之前的定时器
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  // 如果搜索文本为空，清空结果
  if (!searchText.trim()) {
    searchResultUsers.value = []
    return
  }

  // 检查缓存
  if (userSearchCache.has(searchText)) {
    searchResultUsers.value = userSearchCache.get(searchText) || []
    return
  }

  // 防抖：500ms 后执行搜索
  searchTimer = setTimeout(async () => {
    try {
      console.log('🔍 执行用户搜索:', searchText)
      const users = await userStore.searchUsers(searchText)
      searchResultUsers.value = users || []

      // 缓存结果（最多缓存 50 个搜索结果）
      if (userSearchCache.size >= 50) {
        const firstKey = userSearchCache.keys().next().value
        if (firstKey) {
          userSearchCache.delete(firstKey)
        }
      }
      userSearchCache.set(searchText, users || [])

    } catch (error) {
      console.error('搜索用户失败:', error)
      Message.error('搜索用户失败')
    }
  }, 500)
}

// 显示创建模态框
const showCreateModal = () => {
  console.log('🆕 显示新建项目模态框')
  isEdit.value = false
  formData.value = {
    name: '',
    description: '',
    status: 'PROGRESS',
    startDate: '',
    endDate: '',
    progress: 0,
    assigneeId: undefined
  }

  // 重置里程碑数据
  currentMilestones.value = []

  // 重置搜索状态
  userSearchText.value = ''
  searchResultUsers.value = []

  modalVisible.value = true
}

// 监听头部按钮点击事件
const handleHeaderCreate = (event: any) => {
  console.log('🔔 Projects页面收到头部按钮点击事件:', event.detail)
  if (event.detail.type === 'projects') {
    console.log('✅ 触发新建项目模态框')
    showCreateModal()
  }
}

// 编辑项目
const editProject = (project: Project) => {
  isEdit.value = true
  formData.value = {
    id: project.id,
    name: project.name,
    description: project.description,
    status: project.status,
    startDate: project.startDate,
    endDate: project.endDate,
    progress: project.progress,
    assigneeId: project.assigneeId
  }

  // 加载里程碑数据
  currentMilestones.value = getMilestones(project)

  // 重置搜索状态
  userSearchText.value = ''
  searchResultUsers.value = []

  modalVisible.value = true
}

// 删除项目
const deleteProject = async (project: Project) => {
  try {
    await projectStore.deleteProject(project.id)
    Message.success('项目删除成功')
  } catch (error) {
    Message.error('项目删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    // 将里程碑数据序列化为JSON字符串
    const milestonesJson = JSON.stringify(currentMilestones.value.filter(m => m.name.trim()))
    const projectData = {
      ...formData.value,
      milestones: milestonesJson
    }

    if (isEdit.value && formData.value.id) {
      await projectStore.updateProject(formData.value.id, projectData)
      Message.success('项目更新成功')
    } else {
      await projectStore.createProject(projectData)
      Message.success('项目创建成功')
    }
    modalVisible.value = false

    // 如果项目概览模态框是打开的，重新加载数据
    if (overviewModalVisible.value) {
      await projectStore.fetchProjectOverview()
    }
  } catch (error) {
    Message.error(isEdit.value ? '项目更新失败' : '项目创建失败')
  }
}

// 取消操作
const handleCancel = () => {
  modalVisible.value = false
}

// 显示项目概览模态框
const showOverviewModal = async () => {
  try {
    await projectStore.fetchProjectOverview()
    overviewModalVisible.value = true
  } catch (error) {
    Message.error('获取项目概览失败')
  }
}

// 里程碑管理方法
const addMilestone = () => {
  currentMilestones.value.push({
    name: '',
    status: 'PENDING',
    dueDate: '',
    description: ''
  })
}

const removeMilestone = (index: number) => {
  currentMilestones.value.splice(index, 1)
}

// 解析里程碑数据
const getMilestones = (project: Project): Milestone[] => {
  if (!project.milestones) return []
  try {
    return JSON.parse(project.milestones)
  } catch (error) {
    console.error('解析里程碑数据失败:', error)
    return []
  }
}

// 获取里程碑状态颜色
const getMilestoneColor = (status: string) => {
  const colors = {
    'PENDING': 'gray',
    'PROGRESS': 'blue',
    'COMPLETED': 'green'
  }
  return colors[status as keyof typeof colors] || 'gray'
}

// 编辑带里程碑的项目
const editProjectWithMilestones = (project: Project) => {
  isEdit.value = true
  formData.value = {
    id: project.id,
    name: project.name,
    description: project.description,
    status: project.status,
    startDate: project.startDate,
    endDate: project.endDate,
    progress: project.progress,
    assigneeId: project.assigneeId
  }

  // 加载里程碑数据
  currentMilestones.value = getMilestones(project)

  // 重置搜索状态
  userSearchText.value = ''
  searchResultUsers.value = []

  modalVisible.value = true
  overviewModalVisible.value = false
}

// 页面加载时获取数据
onMounted(async () => {
  console.log('📋 Projects页面开始挂载')
  try {
    await Promise.all([
      projectStore.fetchProjects(),
      todoStore.fetchTodos()
    ])
    console.log('✅ Projects页面数据加载完成')
  } catch (error) {
    console.error('❌ Projects页面数据加载失败:', error)
    Message.error('数据加载失败')
  }

  // 添加头部按钮事件监听
  console.log('🎧 添加头部按钮事件监听')
  window.addEventListener('header-create-click', handleHeaderCreate)
})

onUnmounted(() => {
  // 移除事件监听
  window.removeEventListener('header-create-click', handleHeaderCreate)
})
</script>

<style scoped>
.projects-page {
  padding: 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.project-list-card {
  margin-top: 0;
}

/* 表格样式优化 */
:deep(.arco-table) {
  border-collapse: separate;
  border-spacing: 0;
}

:deep(.arco-table-th) {
  text-align: center;
  font-weight: 600;
}

:deep(.arco-table-td) {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 项目名称列允许换行 */
:deep(.arco-table-td:first-child) {
  white-space: normal;
}

/* 进度显示样式 */
.progress-display {
  text-align: center;
  margin-top: 8px;
  font-weight: 500;
  color: #1890ff;
}

/* 用户选项样式 */
.user-option-inline {
  display: flex;
  align-items: center;
  font-size: 14px;
}

/* 操作按钮区域 */
.action-section {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 里程碑编辑器样式 */
.milestones-editor {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.milestone-item {
  margin-bottom: 8px;
  padding: 8px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e5e5e5;
}

.milestone-item:last-child {
  margin-bottom: 0;
}

/* 里程碑显示样式 */
.milestones-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.milestone-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 2px 4px;
  border-radius: 4px;
  background: #f8f9fa;
}

.milestone-tag {
  flex: 1;
  margin-right: 8px;
  font-size: 13px;
  padding: 2px 8px;
  border-radius: 4px;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
  transition: all 0.2s ease;
  border: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  font-weight: 500;
}

.milestone-tag:hover {
  border-color: rgba(0, 0, 0, 0.2);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.milestone-date {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}

/* 概览内容样式 */
.overview-content {
  max-height: 600px;
  overflow-y: auto;
}
</style>