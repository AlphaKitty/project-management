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
              <div class="empty-projects">
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
    <a-modal v-model:visible="overviewModalVisible" title="项目概览" width="1800px" :footer="false">
      <div class="overview-header">
        <a-button type="primary" @click="updateWorkPlans" :loading="workUpdateLoading">
          <template #icon><icon-refresh /></template>
          工作更新
        </a-button>
        <a-button type="primary" @click="exportToExcel">
          <template #icon><icon-download /></template>
          导出Excel
        </a-button>
      </div>
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
              <div v-for="(milestone, index) in getMilestones(record)" :key="index" class="milestone-card">
                <div class="milestone-row">
                  <span class="milestone-name">{{ milestone.name }}</span>
                  <a-tag :color="getMilestoneColor(milestone.status)" size="small" class="milestone-status">
                    {{ getMilestoneStatusLabel(milestone.status) }}
                  </a-tag>
                  <span class="milestone-date" v-if="milestone.dueDate">
                    {{ formatDate(milestone.dueDate) }}
                  </span>
                </div>
              </div>
            </div>
            <div v-else class="no-milestones">
              <span class="no-milestones-text">暂无里程碑</span>
            </div>
          </template>

          <template #thisWeekWork="{ record }">
            <div class="work-plan-display">
              <div v-if="record.thisWeekWork" class="work-plan-content">
                {{ record.thisWeekWork }}
              </div>
              <div v-else class="no-work-plan">
                <span class="no-work-plan-text">暂无本周工作</span>
              </div>
            </div>
          </template>

          <template #nextWeekPlan="{ record }">
            <div class="work-plan-display">
              <div v-if="record.nextWeekPlan" class="work-plan-content">
                {{ record.nextWeekPlan }}
              </div>
              <div v-else class="no-work-plan">
                <span class="no-work-plan-text">暂无下周计划</span>
              </div>
            </div>
          </template>

          <template #todos="{ record }">
            <div class="todos-display">
              <div v-for="todo in getUncompletedTodos(record)" :key="todo.id" class="todo-detail-item">
                <div class="todo-header">
                  <span class="todo-title">{{ todo.title }}</span>
                </div>
                <div class="todo-info">
                  <span class="todo-assignee">责任人: {{ getTodoAssigneeName(todo) }}</span>
                  <span class="todo-dates">
                    创建: {{ formatDate(todo.createTime) }}
                    <span v-if="todo.dueDate"> | 截止: {{ formatDate(todo.dueDate) }}</span>
                  </span>
                  <div class="todo-status-row">
                    <a-tag :color="getTodoStatusColor(todo.status)" size="small" class="todo-status-tag">
                      {{ getStatusLabel(todo.status) }}
                    </a-tag>
                    <span class="todo-remaining" :class="getTodoRemainingClass(todo)">
                      {{ getTodoRemainingText(todo) }}
                    </span>
                  </div>
                </div>

              </div>
              <span v-if="getUncompletedTodos(record).length === 0" class="text-gray-400">正常进行中</span>
            </div>
          </template>

          <template #createTime="{ record }">
            {{ formatDateTime(record.createTime) }}
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
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus, IconEye, IconDownload, IconRefresh } from '@arco-design/web-vue/es/icon'
import * as XLSX from 'xlsx'
import { useProjectStore } from '@/stores/projects'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todos'
import { StatusLabels, StatusColors } from '@/types'
import type { Project, ProjectDTO, User, Milestone } from '@/types'
import dayjs from 'dayjs'

// Store
const projectStore = useProjectStore()
const userStore = useUserStore()
const todoStore = useTodoStore()

// 响应式数据
const modalVisible = ref(false)
const overviewModalVisible = ref(false)
const isEdit = ref(false)
const workUpdateLoading = ref(false)
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
  { title: '总进度', dataIndex: 'progress', key: 'progress', slotName: 'progress', width: 100, align: 'center', sortable: { sortDirections: ['ascend', 'descend'] } },
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
  { title: '责任人', dataIndex: 'assignee', key: 'assignee', slotName: 'assignee', width: 80, align: 'center' },
  { title: '里程碑', dataIndex: 'milestones', key: 'milestones', slotName: 'milestones', width: 100, align: 'center' },
  { title: '本周工作', dataIndex: 'thisWeekWork', key: 'thisWeekWork', slotName: 'thisWeekWork', width: 200, align: 'center' },
  { title: '下周计划', dataIndex: 'nextWeekPlan', key: 'nextWeekPlan', slotName: 'nextWeekPlan', width: 200, align: 'center' },
  { title: '待办事项', dataIndex: 'todos', key: 'todos', slotName: 'todos', width: 180, align: 'center' },
  { title: '创建人', dataIndex: 'creator', key: 'creator', slotName: 'creator', width: 80, align: 'center' },
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
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除项目 "${project.name}" 吗？删除后无法恢复。`,
    okText: '确认删除',
    cancelText: '取消',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        await projectStore.deleteProject(project.id)
        Message.success('项目删除成功')
      } catch (error) {
        Message.error('项目删除失败')
      }
    }
  })
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

// 获取里程碑状态标签
const getMilestoneStatusLabel = (status: string) => {
  const labels = {
    'PENDING': '-待开始-',
    'PROGRESS': '-进行中-',
    'COMPLETED': '-已完成-'
  }
  return labels[status as keyof typeof labels] || status
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

// 获取项目未完成的待办事项
const getUncompletedTodos = (project: Project) => {
  return todoStore.todos.filter(todo =>
    todo.projectId === project.id &&
    (todo.status === 'TODO' || todo.status === 'PROGRESS')
  )
}

// 获取待办状态颜色
const getTodoStatusColor = (status: string) => {
  const colors = {
    'TODO': 'orange',
    'PROGRESS': 'blue',
    'COMPLETED': 'green'
  }
  return colors[status as keyof typeof colors] || 'gray'
}

// 获取待办责任人名称
const getTodoAssigneeName = (todo: any) => {
  if (todo.assignee) {
    return todo.assignee.nickname || todo.assignee.username || '未知'
  }
  if (todo.assigneeId) {
    return `用户${todo.assigneeId}`
  }
  return '未分配'
}

// 格式化日期（简化版）
const formatDate = (date: string | Date | null | undefined) => {
  if (!date) return ''
  return dayjs(date).format('MM-DD')
}

// 获取待办剩余时间文本
const getTodoRemainingText = (todo: any) => {
  if (!todo.dueDate) return ''

  const today = dayjs().startOf('day')
  const dueDate = dayjs(todo.dueDate).startOf('day')
  const diffDays = dueDate.diff(today, 'day')

  if (diffDays < 0) {
    return `逾期${Math.abs(diffDays)}天`
  } else if (diffDays === 0) {
    return '今日到期'
  } else {
    return `剩余${diffDays}天`
  }
}

// 获取待办剩余时间样式类
const getTodoRemainingClass = (todo: any) => {
  if (!todo.dueDate) return ''

  const today = dayjs().startOf('day')
  const dueDate = dayjs(todo.dueDate).startOf('day')
  const diffDays = dueDate.diff(today, 'day')

  if (diffDays < 0) {
    return 'todo-overdue'
  } else if (diffDays <= 3) {
    return 'todo-urgent'
  } else {
    return 'todo-normal'
  }
}

// 添加 formatDateTime 函数
const formatDateTime = (date: string | Date | null | undefined) => {
  if (!date) return '';
  return dayjs(date).format('YYYY-MM-DD');
};

// 工作更新功能
const updateWorkPlans = async () => {
  try {
    workUpdateLoading.value = true

    // 调用后端API更新所有项目的工作计划
    const response = await fetch('/api/projects/update-work-plans', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      // 重新加载项目概览数据
      await projectStore.fetchProjectOverview()
      Message.success('工作计划更新成功')
    } else {
      throw new Error('更新失败')
    }
  } catch (error) {
    console.error('工作计划更新失败:', error)
    Message.error('工作计划更新失败')
  } finally {
    workUpdateLoading.value = false
  }
}



// 导出Excel功能
const exportToExcel = async () => {
  try {
    // 动态导入ExcelJS
    const ExcelJS = await import('exceljs')

    // 创建工作簿
    const workbook = new ExcelJS.Workbook()
    const worksheet = workbook.addWorksheet('项目概览')

    // 定义列
    worksheet.columns = [
      { header: '项目名称', key: 'name', width: 25 },
      { header: '状态', key: 'status', width: 12 },
      { header: '进度', key: 'progress', width: 10 },
      { header: '责任人', key: 'assignee', width: 12 },
      { header: '里程碑', key: 'milestones', width: 60 },
      { header: '本周工作', key: 'thisWeek', width: 40 },
      { header: '下周计划', key: 'nextWeek', width: 40 },
      { header: '待办事项', key: 'todos', width: 50 },
      { header: '风险', key: 'risk', width: 15 }
    ]

    // 设置表头样式
    const headerRow = worksheet.getRow(1)
    headerRow.font = {
      name: '微软雅黑',
      size: 16,
      bold: true,
      color: { argb: 'FFFFFFFF' }
    }
    headerRow.fill = {
      type: 'pattern',
      pattern: 'solid',
      fgColor: { argb: 'FF52C41A' }
    }
    headerRow.alignment = {
      vertical: 'middle',
      horizontal: 'center'
    }
    headerRow.height = 40

    // 添加表头边框
    headerRow.eachCell((cell) => {
      cell.border = {
        top: { style: 'thick', color: { argb: 'FF000000' } },
        bottom: { style: 'thick', color: { argb: 'FF000000' } },
        left: { style: 'thick', color: { argb: 'FF000000' } },
        right: { style: 'thick', color: { argb: 'FF000000' } }
      }
    })

    // 添加数据
    projectStore.overviewProjects.forEach((project, index) => {
      const milestones = getMilestones(project)
      const todos = getUncompletedTodos(project)

      const rowData = {
        name: project.name,
        status: getStatusLabel(project.status),
        progress: `${project.progress}%`,
        assignee: getAssigneeName(project),
        milestones: milestones.map(m => {
          let milestoneText = `${m.name}(${getMilestoneStatusLabel(m.status)})`
          if (m.dueDate) {
            milestoneText += ` - ${m.dueDate}`
          }
          return milestoneText
        }).join('\n'),
        thisWeek: project.thisWeekWork || '暂无本周工作',
        nextWeek: project.nextWeekPlan || '暂无下周计划',
        todos: todos.map(t => {
          let todoText = `${t.title}(${getStatusLabel(t.status)})`

          // 添加责任人
          if (t.assignee) {
            todoText += ` - 责任人：${t.assignee.nickname}`
          } else {
            todoText += ` - 责任人：未分配`
          }

          // 添加截止日期和剩余天数
          if (t.dueDate) {
            const dueDate = new Date(t.dueDate)
            const today = new Date()
            const timeDiff = dueDate.getTime() - today.getTime()
            const daysDiff = Math.ceil(timeDiff / (1000 * 3600 * 24))

            todoText += ` - 截止：${t.dueDate}`

            if (daysDiff > 0) {
              todoText += ` - 剩余${daysDiff}天`
            } else if (daysDiff === 0) {
              todoText += ` - 今日截止`
            } else {
              todoText += ` - 已逾期${Math.abs(daysDiff)}天`
            }
          } else {
            todoText += ` - 截止：无期限`
          }

          return todoText
        }).join('\n'),
        risk: '暂无'
      }

      const row = worksheet.addRow(rowData)
      const rowNumber = index + 2

      // 设置数据行基础样式
      row.font = { name: '微软雅黑', size: 10, color: { argb: 'FF333333' } }
      row.alignment = {
        vertical: 'top',
        horizontal: 'left',
        wrapText: true
      }
      row.height = 80

      // 交替行背景色
      if (rowNumber % 2 === 0) {
        row.eachCell((cell) => {
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: { argb: 'FFF8F9FA' }
          }
        })
      }

      // 添加边框
      row.eachCell((cell) => {
        cell.border = {
          top: { style: 'thick', color: { argb: 'FF000000' } },
          bottom: { style: 'thick', color: { argb: 'FF000000' } },
          left: { style: 'thick', color: { argb: 'FF000000' } },
          right: { style: 'thick', color: { argb: 'FF000000' } }
        }
      })

      // 项目名称列居中
      const nameCell = row.getCell(1)
      nameCell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true }

      // 状态列特殊颜色和居中
      const statusCell = row.getCell(2)
      statusCell.alignment = { vertical: 'middle', horizontal: 'center' }
      const statusValue = rowData.status
      if (statusValue === '进行中') {
        statusCell.font = { name: '微软雅黑', size: 10, bold: true, color: { argb: 'FF52C41A' } }
      } else if (statusValue === '已完成') {
        statusCell.font = { name: '微软雅黑', size: 10, bold: true, color: { argb: 'FF1890FF' } }
      } else if (statusValue === '待启动') {
        statusCell.font = { name: '微软雅黑', size: 10, bold: true, color: { argb: 'FFFAAD14' } }
      } else if (statusValue === '已取消') {
        statusCell.font = { name: '微软雅黑', size: 10, bold: true, color: { argb: 'FFFF4D4F' } }
      }

      // 进度列样式和居中
      const progressCell = row.getCell(3)
      progressCell.alignment = { vertical: 'middle', horizontal: 'center' }
      progressCell.font = { name: '微软雅黑', size: 10, bold: true, color: { argb: 'FF1890FF' } }

      // 责任人列居中
      const assigneeCell = row.getCell(4)
      assigneeCell.alignment = { vertical: 'middle', horizontal: 'center' }

      // 里程碑列居中
      const milestoneCell = row.getCell(5)
      milestoneCell.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true }

      // 风险列居中
      const riskCell = row.getCell(9)
      riskCell.alignment = { vertical: 'middle', horizontal: 'center' }
    })

    // 冻结表头
    worksheet.views = [{ state: 'frozen', ySplit: 1 }]

    // 添加自动筛选
    worksheet.autoFilter = 'A1:I1'

    // 生成文件
    const fileName = `项目概览_${dayjs().format('YYYY-MM-DD_HH-mm-ss')}.xlsx`
    const buffer = await workbook.xlsx.writeBuffer()

    // 创建下载链接
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    link.click()
    window.URL.revokeObjectURL(url)

    Message.success('Excel导出成功')
  } catch (error) {
    console.error('导出Excel失败:', error)
    Message.error('Excel导出失败')
  }
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
  color: var(--primary-color);
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
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 12px;
  background: var(--tag-bg-color);
}

.milestone-item {
  margin-bottom: 8px;
  padding: 8px;
  background: var(--card-bg-color);
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

.milestone-item:last-child {
  margin-bottom: 0;
}

/* 里程碑显示样式 */
.milestones-display {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.milestone-card {
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 4px 6px;
  transition: all 0.2s ease;
  box-shadow: var(--theme-shadow-light);
}

.milestone-card:hover {
  border-color: var(--theme-success);
  box-shadow: var(--theme-shadow-medium);
}

.milestone-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.milestone-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-color);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.milestone-status {
  flex-shrink: 0;
  font-size: 10px;
  height: 16px;
  line-height: 14px;
  padding: 0 4px;
}

.milestone-date {
  font-size: 11px;
  color: var(--text-muted);
  font-family: monospace;
  flex-shrink: 0;
}

.no-milestones {
  text-align: center;
  padding: 12px;
  color: var(--text-muted);
  font-style: italic;
}

.no-milestones-text {
  font-size: 12px;
}

/* 工作计划显示样式 */
.work-plan-display {
  padding: 8px;
  min-height: 40px;
}

.work-plan-content {
  font-size: 12px;
  line-height: 1.4;
  color: var(--text-color);
  text-align: left;
  word-break: break-word;
  white-space: pre-wrap;
}

.no-work-plan {
  text-align: center;
  padding: 12px;
  color: var(--text-muted);
  font-style: italic;
}

.no-work-plan-text {
  font-size: 12px;
}

/* 概览头部样式 */
.overview-header {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 概览内容样式 */
.overview-content {
  max-height: 600px;
  overflow-y: auto;
}

/* 待办事项显示样式 */
.todos-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.todo-detail-item {
  padding: 8px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  background: var(--tag-bg-color);
}

.todo-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.todo-status-tag {
  flex-shrink: 0;
}

.todo-title {
  font-weight: 500;
  color: var(--text-color);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  color: var(--text-muted);
}

.todo-assignee {
  font-weight: 500;
}

.todo-dates {
  color: var(--text-muted);
}

.todo-remaining {
  font-weight: 500;
}

.todo-overdue {
  color: #f53f3f;
}

.todo-urgent {
  color: #ff7d00;
}

.todo-normal {
  color: #00b42a;
}

.empty-projects {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
}
</style>
