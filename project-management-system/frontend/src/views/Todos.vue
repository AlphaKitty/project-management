<template>
    <div class="todos-page">
        <!-- 任务统计卡片 -->
        <div class="stats-cards">
            <a-card class="stat-card">
                <a-statistic title="总任务数" :value="todoStore.todoCount" />
            </a-card>
            <a-card class="stat-card">
                <a-statistic title="待办" :value="todoStore.pendingTodos.length" />
            </a-card>
            <a-card class="stat-card">
                <a-statistic title="进行中" :value="todoStore.inProgressTodos.length" />
            </a-card>
            <a-card class="stat-card">
                <a-statistic title="已完成" :value="todoStore.completedTodos.length" />
            </a-card>
        </div>

        <!-- 筛选条件 -->
        <div class="filter-section">
            <a-row :gutter="16" align="center">
                <a-col :span="8">
                    <a-select v-model="selectedProjectId" placeholder="选择项目" allow-clear @change="handleProjectFilter">
                        <a-option v-for="project in projectStore.projects" :key="project.id" :value="project.id">
                            {{ project.name }}
                        </a-option>
                    </a-select>
                </a-col>
                <a-col :span="16">
                    <a-space>
                        <a-button @click="openSendEmailModal">
                            <template #icon><icon-email /></template>
                            发送邮件
                        </a-button>
                        <a-button @click="openImportModal">
                            <template #icon><icon-upload /></template>
                            批量导入
                        </a-button>
                    </a-space>
                </a-col>
            </a-row>
        </div>

        <!-- 快捷筛选 -->
        <div class="filter-tabs">
            <a-tabs v-model:active-key="activeTab" @change="handleTabChange">
                <a-tab-pane key="pending" title="未完成任务" />
                <a-tab-pane key="high" title="高优先级" />
                <a-tab-pane key="overdue" title="已逾期任务" />
                <a-tab-pane key="completed" title="已完成任务" />
                <a-tab-pane key="all" title="全部任务" />
            </a-tabs>
        </div>

        <!-- 任务列表 -->
        <a-card title="任务列表" class="todo-list-card">
            <a-table :columns="columns" :data="filteredTodos" :loading="todoStore.loading"
                :pagination="{ pageSize: 20 }">
                <template #title="{ record }">
                    <div class="task-title">
                        <span :class="{ 'completed': record.status === 'DONE' }">
                            {{ record.title }}
                        </span>
                    </div>
                </template>

                <template #description="{ record }">
                    <div class="description-action">
                        <a-button v-if="record.description && record.description.trim()" size="small" type="text"
                            @click="viewDescription(record)" class="description-btn">
                            <template #icon><icon-eye /></template>
                            查看
                        </a-button>
                        <span v-else class="no-description-text">无描述</span>
                    </div>
                </template>

                <template #priority="{ record }">
                    <a-tag :color="getPriorityColor(record.priority)">
                        {{ getPriorityLabel(record.priority) }}
                    </a-tag>
                </template>

                <template #status="{ record }">
                    <a-tag :color="getStatusColor(record)">
                        {{ getStatusLabel(record) }}
                    </a-tag>
                </template>

                <template #project="{ record }">
                    {{ record.project?.name || '无' }}
                </template>

                <template #assignee="{ record }">
                    {{ record.assignee?.username || '未分配' }}
                </template>

                <template #updateTime="{ record }">
                    {{ formatDateTime(record.updateTime) }}
                </template>

                <template #actions="{ record }">
                    <a-button-group size="small">
                        <a-button @click="editTodo(record)">编辑</a-button>
                        <a-button status="danger" @click="deleteTodo(record)">删除</a-button>
                    </a-button-group>
                </template>
            </a-table>
        </a-card>

        <!-- 创建/编辑任务模态框 -->
        <a-modal v-model:visible="modalVisible" :title="isEdit ? '编辑任务' : '新建任务'" @before-ok="handleSubmit"
            @cancel="handleCancel" :ok-loading="submitting" :ok-button-props="{ disabled: !isFormValid || submitting }">
            <a-form ref="formRef" :model="formData" :rules="formRules" layout="vertical">
                <div v-if="!isFormValid && Object.keys(formErrors).length > 0" class="form-error-tip"> <a-alert
                        type="warning" show-icon> <template #title>请完善以下必填信息</template>
                        <ul>
                            <li v-for="(error, field) in formErrors" :key="field"> {{ error }} </li>
                        </ul>
                    </a-alert> </div>
                <div v-if="!isFormValid && Object.keys(formErrors).length > 0" class="form-error-tip"> <a-alert
                        type="warning" show-icon> <template #title>请完善以下必填信息</template>
                        <ul>
                            <li v-for="(error, field) in formErrors" :key="field"> {{ error }} </li>
                        </ul>
                    </a-alert> </div>
                <a-form-item label="任务标题" field="title" required>
                    <a-input v-model="formData.title" placeholder="请输入任务标题" @blur="() => handleFieldChange('title')" />
                </a-form-item>

                <a-form-item label="任务描述" field="description">
                    <a-textarea v-model="formData.description" placeholder="请输入任务描述" />
                </a-form-item>

                <a-form-item label="所属项目" field="projectId" required>
                    <a-select v-model="formData.projectId" placeholder="请选择项目"
                        @change="() => handleFieldChange('projectId')">
                        <a-option v-for="project in projectStore.projects" :key="project.id" :value="project.id">
                            {{ project.name }}
                        </a-option>
                    </a-select>
                </a-form-item>

                <a-form-item label="分配给" field="assigneeId" required>
                    <a-select v-model="formData.assigneeId" placeholder="请输入关键字搜索负责人" allow-search allow-clear
                        :filter-option="false" :loading="userStore.loading" @search="handleUserSearch"
                        @change="() => handleFieldChange('assigneeId')">
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

                <a-form-item label="优先级" field="priority" required> <a-select v-model="formData.priority"
                        placeholder="请选择优先级" @change="() => handleFieldChange('priority')"> <a-option
                            value="LOW">低</a-option> <a-option value="MEDIUM">中</a-option> <a-option
                            value="HIGH">高</a-option> </a-select> </a-form-item>
                <a-form-item label="任务状态" field="status" required> <a-select v-model="formData.status"
                        placeholder="请选择状态" @change="() => handleFieldChange('status')"> <a-option
                            value="TODO">待办</a-option> <a-option value="PROGRESS">进行中</a-option> <a-option
                            value="DONE">已完成</a-option> </a-select> </a-form-item>

                <a-form-item label="截止日期" field="dueDate">
                    <a-date-picker v-model="formData.dueDate" style="width: 100%" />
                </a-form-item>

                <a-form-item label="邮件通知" field="emailEnabled">
                    <a-switch v-model="formData.emailEnabled" />
                    <span style="margin-left: 8px; color: #666; font-size: 12px;">
                        开启后将向责任人发送邮件通知
                    </span>
                </a-form-item>
            </a-form>
        </a-modal>

        <!-- 查看描述模态框 -->
        <a-modal v-model:visible="descriptionModalVisible" title="任务描述" @cancel="closeDescriptionModal" :footer="false"
            width="600px">
            <div class="description-modal-content">
                <div class="description-header">
                    <h3>{{ currentTask?.title }}</h3>
                    <div class="task-info">
                        <a-tag v-if="currentTask?.priority" :color="getPriorityColor(currentTask.priority)">
                            {{ getPriorityLabel(currentTask.priority) }}
                        </a-tag>
                        <a-tag v-if="currentTask" :color="getStatusColor(currentTask)">
                            {{ getStatusLabel(currentTask) }}
                        </a-tag>
                    </div>
                </div>
                <a-divider />
                <div class="description-content-modal">
                    <div v-if="currentTask?.description" class="description-text">
                        {{ currentTask.description }}
                    </div>
                    <div v-else class="no-description-placeholder">
                        暂无任务描述
                    </div>
                </div>
            </div>
        </a-modal>

        <!-- 发送邮件模态框 --> <a-modal v-model:visible="sendEmailModal" title="发送待办任务邮件" @before-ok="handleSendEmail"
            @cancel="sendEmailModal = false" width="800px"> <a-form layout="vertical"> <a-form-item label="邮件发送说明">
                    <a-alert type="info" show-icon> <template #title>邮件将自动发送给待办任务的责任人</template>
                        系统将根据所选范围内的待办任务，自动向每个任务的责任人发送邮件提醒。
                    </a-alert> </a-form-item> <a-form-item label="发送范围"> <a-radio-group v-model="emailForm.scope"
                        @change="updateEmailTodoList"> <a-radio value="all">所有待办任务</a-radio> <a-radio
                            value="pending">未完成任务</a-radio> <a-radio value="high">高优先级</a-radio> </a-radio-group>
                </a-form-item>
                <a-form-item label="邮件发送预览">
                    <div class="email-preview">
                        <div class="email-header">
                            <p><strong>将要通知的责任人：</strong></p>
                            <div class="assignee-list"> <a-tag v-for="assignee in uniqueAssignees" :key="assignee.id"
                                    size="small" color="blue"> {{ assignee.email }} </a-tag>
                            </div>
                            <p style="margin-top: 12px;"><strong>任务数量：</strong>{{ emailTodoList.length }} 个待办任务</p>
                        </div> <a-table :columns="emailTableColumns" :data="emailTodoList" :pagination="false"
                            size="small" class="email-table"> <template #project="{ record }"> {{ record.project?.name
                                || '无' }} </template>
                            <template #priority="{ record }"> <a-tag :color="getPriorityColor(record.priority)"
                                    size="small"> {{ getPriorityLabel(record.priority) }} </a-tag> </template>
                            <template #assignee="{ record }">
                                <div v-if="record.assignee">
                                    <div>{{ record.assignee.email }}</div>
                                </div>
                                <div v-else>未分配</div>
                            </template>
                        </a-table>
                    </div>
                </a-form-item> </a-form> </a-modal>

        <!-- 批量导入模态框 -->
        <a-modal v-model:visible="importModalVisible" title="批量导入任务" @before-ok="handleImport"
            @cancel="importModalVisible = false" width="800px">
            <a-form layout="vertical">
                <a-form-item label="导入说明">
                    <a-alert type="info" show-icon>
                        <template #title>请按以下格式粘贴任务数据，每行一个任务</template>
                        格式：任务标题|项目名称|负责人ad|优先级|描述|截止日期<br />
                        优先级选项：HIGH（高）、MEDIUM（中）、LOW（低）<br />
                        示例：完成需求分析|项目管理系统|admin|HIGH|详细分析用户需求|2025-01-20
                    </a-alert>
                </a-form-item>
                <a-form-item label="任务数据" required>
                    <a-textarea v-model="importData" placeholder="请粘贴任务数据，每行一个任务"
                        :auto-size="{ minRows: 10, maxRows: 20 }" />
                </a-form-item>
                <a-form-item label="邮件通知">
                    <a-switch v-model="importEmailEnabled" />
                    <span style="margin-left: 8px; color: #666; font-size: 12px;">
                        开启后将向所有责任人发送邮件通知
                    </span>
                </a-form-item>
                <a-form-item label="预览" v-if="parsedTasks.length > 0">
                    <a-table :columns="previewColumns" :data="parsedTasks" :pagination="false" size="small">
                        <template #project="{ record }">
                            <span :class="{ 'text-red-500': !record.projectId }">
                                {{ record.projectName }}
                                <span v-if="!record.projectId" class="text-xs">(未找到)</span>
                            </span>
                        </template>
                        <template #assignee="{ record }">
                            <span :class="{ 'text-red-500': !record.assigneeId }">
                                {{ record.username || '未指定' }}
                                <span v-if="record.username && !record.assigneeId" class="text-xs">(未找到)</span>
                            </span>
                        </template>
                        <template #priority="{ record }">
                            <span :class="{ 'text-red-500': !['HIGH', 'MEDIUM', 'LOW'].includes(record.priority) }">
                                {{ record.priority }}
                                <span v-if="!['HIGH', 'MEDIUM', 'LOW'].includes(record.priority)"
                                    class="text-xs">(无效)</span>
                            </span>
                        </template>
                        <template #dueDate="{ record }">
                            <span
                                :class="{ 'text-red-500': record.dueDate && !/^\d{4}-\d{2}-\d{2}$/.test(record.dueDate) }">
                                {{ record.dueDate }}
                                <span v-if="record.dueDate && !/^\d{4}-\d{2}-\d{2}$/.test(record.dueDate)"
                                    class="text-xs">(格式错误)</span>
                            </span>
                        </template>
                    </a-table>
                </a-form-item>
            </a-form>
        </a-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconEmail, IconUpload, IconEye } from '@arco-design/web-vue/es/icon'
import { useTodoStore } from '@/stores/todos'
import { useProjectStore } from '@/stores/projects'
import { useUserStore } from '@/stores/user'
import { StatusLabels, StatusColors } from '@/types'
import type { Todo, TodoDTO, User } from '@/types'
import dayjs from 'dayjs'

// Store
const todoStore = useTodoStore()
const projectStore = useProjectStore()
const userStore = useUserStore()

// 响应式数据
const modalVisible = ref(false)
const sendEmailModal = ref(false)
const importModalVisible = ref(false)
const importData = ref('')
const importEmailEnabled = ref(true)
const isEdit = ref(false)
const activeTab = ref('pending')
const selectedProjectId = ref<number | undefined>(undefined)
const formRef = ref()
const userSearchText = ref('')
const searchResultUsers = ref<User[]>([])
const submitting = ref(false)
const formErrors = ref<Record<string, string>>({})

// 描述查看相关
const descriptionModalVisible = ref(false)
const currentTask = ref<Todo | null>(null)

const emailTodoList = ref<Todo[]>([])

// 邮件表格列配置
const emailTableColumns = [
    { title: '任务', dataIndex: 'title', key: 'title' },
    { title: '所属项目', dataIndex: 'project', key: 'project', slotName: 'project' },
    { title: '优先级', dataIndex: 'priority', key: 'priority', slotName: 'priority' },
    { title: '负责人', dataIndex: 'assignee', key: 'assignee', slotName: 'assignee' },
    { title: '截止时间', dataIndex: 'dueDate', key: 'dueDate' }
]

const formData = ref<TodoDTO>({
    title: '',
    description: '',
    projectId: undefined,
    assigneeId: undefined,
    priority: 'MEDIUM',
    status: 'PROGRESS',
    dueDate: '',
    emailEnabled: true, // 默认开启邮件通知
    creatorId: 1 // 暂时固定为管理员
})

const emailForm = ref({
    scope: 'pending'
})

// 获取唯一的责任人列表
const uniqueAssignees = computed(() => {
    const assigneeMap = new Map<number, User>()

    emailTodoList.value.forEach(todo => {
        if (todo.assignee && todo.assignee.id) {
            assigneeMap.set(todo.assignee.id, todo.assignee)
        }
    })

    return Array.from(assigneeMap.values())
})

// 表单验证规则
const formRules = {
    title: [
        { required: true, message: '请输入任务标题', trigger: 'blur' },
        { min: 2, max: 100, message: '任务标题长度为2-100个字符', trigger: 'blur' }
    ],
    projectId: [
        { required: true, message: '请选择所属项目', trigger: 'change' }
    ],
    assigneeId: [
        { required: true, message: '请选择负责人', trigger: 'change' }
    ],
    priority: [
        { required: true, message: '请选择优先级', trigger: 'change' }
    ],
    status: [
        { required: true, message: '请选择任务状态', trigger: 'change' }
    ]
}

// 表格列配置
const columns = computed(() => {
    const baseColumns = [
        { title: '任务标题', dataIndex: 'title', key: 'title', slotName: 'title', width: 200 },
        { title: '描述', dataIndex: 'description', key: 'description', slotName: 'description', width: 120, align: 'center' },
        { title: '优先级', dataIndex: 'priority', key: 'priority', slotName: 'priority', align: 'center' },
        { title: '状态', dataIndex: 'status', key: 'status', slotName: 'status', align: 'center' },
        { title: '所属项目', dataIndex: 'project', key: 'project', slotName: 'project', width: 200, align: 'center' },
        { title: '负责人', dataIndex: 'assignee', key: 'assignee', slotName: 'assignee', align: 'center' },
        { title: '截止日期', dataIndex: 'dueDate', key: 'dueDate', align: 'center', sortable: { sortDirections: ['ascend', 'descend'] } },
        { title: '操作', key: 'actions', slotName: 'actions', width: 200, align: 'center' }
    ]

    if (activeTab.value === 'completed') {
        // 在已完成标签页中，在截止日期前插入更新时间列
        const updateTimeColumn = {
            title: '更新时间',
            dataIndex: 'updateTime',
            key: 'updateTime',
            slotName: 'updateTime',
            width: 120,
            align: 'center' as const,
            sortable: { sortDirections: ['descend'] as const }
        }
        const dueDateIndex = baseColumns.findIndex(col => col.dataIndex === 'dueDate')
        baseColumns.splice(dueDateIndex, 0, updateTimeColumn as any)
    }

    return baseColumns
})

// 当前显示的任务列表（结合标签页和项目筛选）
const currentTodos = computed(() => {
    switch (activeTab.value) {
        case 'high':
            // 高优先级：只显示未完成的高优先级任务
            return todoStore.highPriorityTodos.filter(todo => todo.status !== 'DONE')
        case 'overdue':
            return todoStore.overdueTodos
        case 'pending':
            // 未完成任务：合并今日任务和本周任务，包含所有非完成状态的任务
            return todoStore.todos.filter(todo => todo.status !== 'DONE')
        case 'completed':
            return todoStore.completedTodos
        default:
            return todoStore.todos
    }
})

// 筛选后的任务列表
const filteredTodos = computed(() => {
    if (!selectedProjectId.value) {
        return currentTodos.value
    }
    return currentTodos.value.filter(todo => todo.projectId === selectedProjectId.value)
})

// 表单是否有效
const isFormValid = computed(() => {
    // 简化验证逻辑，只检查最基本的必填字段
    const hasTitle = !!(formData.value.title && formData.value.title.trim().length >= 2)
    const hasProject = !!(formData.value.projectId && formData.value.projectId > 0)
    const hasAssignee = !!(formData.value.assigneeId && formData.value.assigneeId > 0)
    const hasPriority = !!(formData.value.priority && formData.value.priority.trim())
    const hasStatus = !!(formData.value.status && formData.value.status.trim())

    const isValid = hasTitle && hasProject && hasAssignee && hasPriority && hasStatus

    console.log('🔍 表单验证:', {
        hasTitle,
        hasProject: `${formData.value.projectId} -> ${hasProject}`,
        hasAssignee: `${formData.value.assigneeId} -> ${hasAssignee}`,
        hasPriority: `${formData.value.priority} -> ${hasPriority}`,
        hasStatus: `${formData.value.status} -> ${hasStatus}`,
        isValid,
        formData: formData.value
    })

    return isValid
})

// 获取优先级标签
const getPriorityLabel = (priority: string) => {
    return StatusLabels[priority as keyof typeof StatusLabels] || priority
}

// 获取优先级颜色
const getPriorityColor = (priority: string) => {
    return StatusColors[priority as keyof typeof StatusColors] || 'gray'
}

// 获取状态标签（支持细化状态）
const getStatusLabel = (todo: Todo) => {
    if (todo.status === 'DONE') {
        // 已完成任务：区分按时完成和逾期完成
        if (todo.dueDate) {
            const dueDate = new Date(todo.dueDate)
            const completedTime = todo.completedTime ? new Date(todo.completedTime) : new Date(todo.updateTime)
            return completedTime <= dueDate ? '按时完成' : '逾期完成'
        }
        return '已完成'
    } else if (todo.dueDate) {
        // 未完成任务：检查是否逾期或即将逾期
        const today = new Date()
        today.setHours(0, 0, 0, 0) // 设置为今天的开始时间
        const dueDate = new Date(todo.dueDate)
        dueDate.setHours(23, 59, 59, 999) // 设置为截止日的结束时间

        if (dueDate < today) {
            return '已逾期'
        } else if (dueDate.toDateString() === today.toDateString()) {
            return '即将逾期'
        }
    }
    return StatusLabels[todo.status as keyof typeof StatusLabels] || todo.status
}

// 获取状态颜色（支持细化状态）
const getStatusColor = (todo: Todo) => {
    if (todo.status === 'DONE') {
        // 已完成任务：区分按时完成和逾期完成的颜色
        if (todo.dueDate) {
            const dueDate = new Date(todo.dueDate)
            const completedTime = todo.completedTime ? new Date(todo.completedTime) : new Date(todo.updateTime)
            return completedTime <= dueDate ? 'green' : 'orange'
        }
        return 'green'
    } else if (todo.dueDate) {
        // 未完成任务：检查是否逾期或即将逾期
        const today = new Date()
        today.setHours(0, 0, 0, 0) // 设置为今天的开始时间
        const dueDate = new Date(todo.dueDate)
        dueDate.setHours(23, 59, 59, 999) // 设置为截止日的结束时间

        if (dueDate < today) {
            return 'red' // 逾期显示红色
        } else if (dueDate.toDateString() === today.toDateString()) {
            return 'orange' // 即将逾期显示橙色
        }
    }
    return StatusColors[todo.status as keyof typeof StatusColors] || 'gray'
}

// 项目筛选处理
const handleProjectFilter = (projectId: number | undefined) => {
    selectedProjectId.value = projectId
}

// 查看描述相关方法
const viewDescription = (todo: Todo) => {
    currentTask.value = todo
    descriptionModalVisible.value = true
}

const closeDescriptionModal = () => {
    descriptionModalVisible.value = false
    currentTask.value = null
}

// 用户搜索缓存
const userSearchCache = new Map<string, User[]>()
let searchTimer: NodeJS.Timeout | null = null

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

// 切换标签页
const handleTabChange = async (key: string) => {
    activeTab.value = key
    try {
        switch (key) {
            case 'today':
                await todoStore.fetchTodayTodos()
                break
            case 'week':
                await todoStore.fetchWeekTodos()
                break
            case 'all':
                await todoStore.fetchTodos()
                break
            case 'high':
                await todoStore.fetchHighPriorityTodos()
                break
            case 'overdue':
                await todoStore.fetchTodos() // 已逾期任务通过计算属性获取
                break
            case 'completed':
                await todoStore.fetchTodos() // 已完成任务通过计算属性获取
                break
        }
    } catch (error) {
        Message.error('数据加载失败')
    }
}

// 切换任务状态
const toggleTaskStatus = async (todo: Todo) => {
    const newStatus = todo.status === 'DONE' ? 'TODO' : 'DONE'
    try {
        await todoStore.updateStatus(todo.id, newStatus)
        Message.success('任务状态更新成功')
    } catch (error) {
        Message.error('任务状态更新失败')
    }
}

// 显示创建模态框
const showCreateModal = () => {
    isEdit.value = false
    formData.value = {
        title: '',
        description: '',
        projectId: undefined,
        assigneeId: undefined,
        priority: 'MEDIUM',
        status: 'PROGRESS',
        dueDate: '',
        emailEnabled: true,
        creatorId: 1
    }
    // 重置搜索状态
    userSearchText.value = ''
    searchResultUsers.value = []
    // 清除表单错误
    formErrors.value = {}
    modalVisible.value = true
}

// 监听头部按钮点击事件
const handleHeaderCreate = (event: any) => {
    if (event.detail.type === 'todos') {
        showCreateModal()
    }
}

// 编辑任务
const editTodo = (todo: Todo) => {
    isEdit.value = true
    formData.value = {
        id: todo.id,
        title: todo.title,
        description: todo.description,
        projectId: todo.projectId,
        assigneeId: todo.assigneeId,
        priority: todo.priority,
        status: todo.status,
        dueDate: todo.dueDate,
        emailEnabled: (todo as any).emailEnabled !== undefined ? (todo as any).emailEnabled : true,
        creatorId: todo.creatorId
    }
    // 重置搜索状态
    userSearchText.value = ''
    searchResultUsers.value = []
    // 清除表单错误
    formErrors.value = {}
    modalVisible.value = true
}

// 查看任务详情
const viewTodo = (todo: Todo) => {
    Message.info(`查看任务: ${todo.title}`)
}

// 删除任务
const deleteTodo = async (todo: Todo) => {
    try {
        await todoStore.deleteTodo(todo.id)
        Message.success('任务删除成功')
    } catch (error) {
        Message.error('任务删除失败')
    }
}

// 添加字段验证和错误处理
const validateField = async (field: string) => {
    try {
        await formRef.value?.validateField(field)
        // 验证成功，清除该字段的错误
        delete formErrors.value[field]
    } catch (error: any) {
        // 验证失败，记录错误信息
        formErrors.value[field] = error.message || '验证失败'
    }
}

// 监听表单字段变化，实时验证
const handleFieldChange = (field: string) => {
    // 延迟验证，避免用户输入时频繁验证
    setTimeout(() => {
        validateField(field)
    }, 300)
}

// 提交表单 - 使用 before-ok 事件
const handleSubmit = async (): Promise<boolean> => {
    console.log('🚀 handleSubmit 开始执行')
    console.log('📊 submitting.value:', submitting.value)
    console.log('📊 isFormValid.value:', isFormValid.value)

    if (submitting.value) {
        console.log('❌ 正在提交中，阻止重复提交')
        return false // 阻止弹框关闭
    }

    if (!isFormValid.value) {
        console.log('❌ 表单验证未通过，阻止提交')
        return false // 阻止弹框关闭
    }

    try {
        submitting.value = true

        // 表单验证
        console.log('开始表单验证，当前表单数据:', formData.value)

        try {
            console.log('📋 formRef.value:', formRef.value)
            const result = await formRef.value?.validate()
            console.log('✅ Arco 表单验证通过, result:', result)
            // 注意：Arco Design 验证成功时可能返回 undefined，这是正常的
        } catch (error: any) {
            console.log('❌ Arco 表单验证失败，错误信息:', error)
            // 显示具体的验证错误
            if (error && typeof error === 'object') {
                const errorFields = Object.keys(error)
                if (errorFields.length > 0) {
                    Message.error(`请检查以下字段：${errorFields.join('、')}`)
                } else {
                    Message.error('请检查表单内容，确保必填项已正确填写')
                }
            } else {
                Message.error('请检查表单内容，确保必填项已正确填写')
            }
            return false // 阻止弹框关闭
        }

        // 数据格式处理
        const submitData = {
            ...formData.value,
            // 确保ID是数字类型
            projectId: formData.value.projectId ? Number(formData.value.projectId) : undefined,
            assigneeId: formData.value.assigneeId ? Number(formData.value.assigneeId) : undefined,
            creatorId: Number(formData.value.creatorId),
            // 日期格式处理 - 只传递年月日
            dueDate: formData.value.dueDate ?
                (typeof formData.value.dueDate === 'string' ?
                    formData.value.dueDate.substring(0, 10) : // 只取年月日部分 YYYY-MM-DD
                    formData.value.dueDate) :
                undefined
        }

        console.log('提交的表单数据:', submitData)

        // 调用后端接口
        let apiResponse
        if (isEdit.value && formData.value.id) {
            console.log('执行更新任务操作...')
            apiResponse = await todoStore.updateTodo(formData.value.id, submitData)
            console.log('任务更新接口返回:', apiResponse)
        } else {
            console.log('执行创建任务操作...')
            apiResponse = await todoStore.createTodo(submitData)
            console.log('任务创建接口返回:', apiResponse)
        }

        // 接口调用成功，执行后续操作
        console.log('后端接口调用成功，准备关闭弹框')

        // 显示成功消息
        Message.success(isEdit.value ? '任务更新成功' : '任务创建成功')

        // 清除表单错误状态
        formErrors.value = {}

        // 重新加载任务列表
        console.log('重新加载任务列表...')
        await todoStore.fetchTodos()
        console.log('任务列表重新加载完成')

        // 返回 true 让 Modal 自动关闭
        return true

    } catch (error: any) {
        console.error('提交失败:', error)

        // 详细的错误处理
        let errorMessage = isEdit.value ? '任务更新失败' : '任务创建失败'

        if (error?.response?.data?.message) {
            errorMessage += `：${error.response.data.message}`
        } else if (error?.message) {
            errorMessage += `：${error.message}`
        } else {
            errorMessage += '，请检查网络连接或联系管理员'
        }

        Message.error(errorMessage)

        // 错误时返回 false，阻止弹框关闭
        console.log('接口调用失败，保持弹框打开状态')
        return false

    } finally {
        submitting.value = false
        console.log('提交操作完成，loading状态已重置')
    }
}

// 取消操作
const handleCancel = () => {
    modalVisible.value = false
    // 重置搜索状态
    userSearchText.value = ''
    searchResultUsers.value = []
    // 清除表单错误
    formErrors.value = {}
}

// 更新邮件任务列表
const updateEmailTodoList = () => {
    const todos = todoStore.todos.filter(todo => todo.status !== 'DONE')
    switch (emailForm.value.scope) {
        case 'pending':
            // 未完成任务：所有非完成状态的任务
            emailTodoList.value = todos
            break
        case 'high':
            // 高优先级未完成任务
            emailTodoList.value = todoStore.highPriorityTodos.filter(todo => todo.status !== 'DONE')
            break
        default:
            emailTodoList.value = todos
    }
}

// 打开发送邮件模态框
const openSendEmailModal = () => {
    updateEmailTodoList()
    sendEmailModal.value = true
}

// 发送邮件 - 使用 before-ok 事件
const handleSendEmail = async (): Promise<boolean> => {
    // 检查是否有待办任务
    if (emailTodoList.value.length === 0) {
        Message.error('没有待办任务可发送')
        return false // 阻止弹框关闭
    }

    // 检查是否所有任务都有负责人
    const tasksWithoutAssignee = emailTodoList.value.filter(todo => !todo.assignee?.email)
    if (tasksWithoutAssignee.length > 0) {
        Message.error(`有 ${tasksWithoutAssignee.length} 个任务未分配责任人，无法发送邮件`)
        return false // 阻止弹框关闭
    }

    try {
        console.log('开始批量发送邮件给责任人')

        // 获取所有唯一的责任人邮箱
        const uniqueEmails = Array.from(new Set(
            emailTodoList.value
                .filter(todo => todo.assignee?.email)
                .map(todo => todo.assignee!.email)
        ))

        console.log('将要发送邮件的收件人:', uniqueEmails)

        // 批量发送邮件 - 需要为每个邮箱找到对应的userId
        const sendPromises = uniqueEmails.map(email => {
            // 找到该邮箱对应的用户ID
            const userTodo = emailTodoList.value.find(todo => todo.assignee?.email === email)
            const userId = userTodo?.assignee?.id
            console.log(`发送邮件给 ${email}，userId: ${userId}`)
            return todoStore.sendEmail(email!, userId)
        })
        await Promise.all(sendPromises)

        // 邮件发送成功
        Message.success(`邮件发送成功，已通知 ${uniqueEmails.length} 位责任人`)
        console.log('邮件批量发送成功，准备关闭弹框')

        // 返回 true 让 Modal 自动关闭
        return true

    } catch (error: any) {
        console.error('邮件发送失败:', error)

        // 详细的错误处理
        let errorMessage = '邮件发送失败'

        if (error?.response?.data?.message) {
            errorMessage += `：${error.response.data.message}`
        } else if (error?.message) {
            errorMessage += `：${error.message}`
        } else {
            errorMessage += '，请检查网络连接或联系管理员'
        }

        Message.error(errorMessage)

        // 邮件发送失败时返回 false，阻止弹框关闭
        console.log('邮件发送失败，保持弹框打开状态')
        return false
    }
}

// 添加 formatDateTime 函数
const formatDateTime = (date: string | Date | null | undefined) => {
    if (!date) return '';
    return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
};

// 批量导入相关
const userCacheVersion = ref(0) // 用于强制刷新计算属性

const parsedTasks = computed(() => {
    // 依赖userCacheVersion来触发重新计算
    userCacheVersion.value

    if (!importData.value.trim()) return []

    return importData.value.trim().split('\n').map((line, index) => {
        const parts = line.split('|')
        if (parts.length < 6) return null

        // 根据项目名称查找项目ID
        const projectName = parts[1]?.trim()
        const project = projectStore.projects.find(p => p.name === projectName)
        const projectId = project ? project.id : null

        // 根据用户名查找用户ID
        const username = parts[2]?.trim()
        let assigneeId = null

        // 从搜索结果缓存中查找用户
        if (username) {
            console.log('查找用户:', username)
            console.log('当前缓存:', userSearchCache)

            // 直接在对应的缓存中查找
            const cachedUsers = userSearchCache.get(username)
            if (cachedUsers) {
                const user = cachedUsers.find(u => u.username === username)
                if (user) {
                    assigneeId = user.id
                    console.log(`找到用户 ${username}, ID: ${assigneeId}`)
                } else {
                    console.log(`缓存中有数据但未找到精确匹配的用户: ${username}`)
                }
            } else {
                console.log(`缓存中没有用户: ${username}`)
            }
        }

        return {
            title: parts[0]?.trim() || '',
            projectId,
            assigneeId: assigneeId || undefined,
            priority: parts[3]?.trim() || 'MEDIUM',
            description: parts[4]?.trim() || '',
            dueDate: parts[5]?.trim() || '',
            status: 'PROGRESS',
            emailEnabled: importEmailEnabled.value,
            creatorId: 1,
            // 保存原始数据用于显示
            projectName,
            username
        }
    }).filter(task => task && task.title)
})

const previewColumns = [
    { title: '任务标题', dataIndex: 'title', width: 150 },
    { title: '项目', dataIndex: 'projectName', slotName: 'project', width: 120 },
    { title: '负责人', dataIndex: 'username', slotName: 'assignee', width: 120, align: 'center' },
    { title: '优先级', dataIndex: 'priority', slotName: 'priority', width: 100, align: 'center' },
    { title: '描述', dataIndex: 'description', slotName: 'description', width: 200 },
    { title: '截止日期', dataIndex: 'dueDate', slotName: 'dueDate', width: 120, align: 'center' }
]

const getProjectNameById = (projectId: number | null) => {
    if (!projectId) return '无项目'
    const project = projectStore.projects.find(p => p.id === projectId)
    return project ? project.name : `项目${projectId}`
}

const openImportModal = () => {
    importData.value = ''
    importEmailEnabled.value = true
    importModalVisible.value = true
}

// 监听导入数据变化，自动搜索用户
let searchUsersTimer: NodeJS.Timeout | null = null
watch(importData, async (newData: string) => {
    if (!newData.trim()) return

    // 清除之前的定时器
    if (searchUsersTimer) {
        clearTimeout(searchUsersTimer)
    }

    // 防抖处理
    searchUsersTimer = setTimeout(async () => {
        const uniqueUsernames = [...new Set(
            newData.trim().split('\n')
                .map((line: string) => line.split('|')[2]?.trim())
                .filter((username: string | undefined) => username && username.length >= 2)
        )]

        // 为每个用户名进行搜索
        for (const username of uniqueUsernames) {
            if (!userSearchCache.has(username)) {
                try {
                    const users = await userStore.searchUsers(username)
                    userSearchCache.set(username, users || [])
                    console.log(`缓存用户 ${username}:`, users)
                    // 触发计算属性重新计算
                    userCacheVersion.value++
                } catch (error) {
                    console.error('搜索用户失败:', username, error)
                    userSearchCache.set(username, [])
                    // 触发计算属性重新计算
                    userCacheVersion.value++
                }
            }
        }
    }, 1000)
})

const handleImport = async (): Promise<boolean> => {
    if (parsedTasks.value.length === 0) {
        Message.error('请输入有效的任务数据')
        return false
    }

    // 先搜索所有需要的用户
    const uniqueUsernames = [...new Set(
        importData.value.trim().split('\n')
            .map(line => line.split('|')[2]?.trim())
            .filter(username => username)
    )]

    // 为每个用户名进行搜索
    for (const username of uniqueUsernames) {
        if (!userSearchCache.has(username)) {
            try {
                console.log('搜索用户:', username)
                const users = await userStore.searchUsers(username)
                userSearchCache.set(username, users || [])
                userCacheVersion.value++
            } catch (error) {
                console.error('搜索用户失败:', username, error)
                userSearchCache.set(username, [])
                userCacheVersion.value++
            }
        }
    }

    // 检查是否有无效的项目、用户、优先级或日期
    const validPriorities = ['HIGH', 'MEDIUM', 'LOW']
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/

    const invalidTasks = parsedTasks.value.filter(task => {
        if (!task) return true

        // 检查项目
        if (!task.projectId) return true

        // 检查负责人
        if (!task.assigneeId) return true

        // 检查优先级
        if (!validPriorities.includes(task.priority)) return true

        // 检查截止日期格式
        if (task.dueDate && !dateRegex.test(task.dueDate)) return true

        return false
    })

    if (invalidTasks.length > 0) {
        Message.error(`有 ${invalidTasks.length} 个任务的项目、负责人、优先级或日期格式不正确，请检查数据`)
        return false
    }

    try {
        // 批量创建任务
        let successCount = 0
        for (const task of parsedTasks.value) {
            if (task && task.projectId && task.assigneeId) {
                const { projectName, username, ...todoData } = task
                await todoStore.createTodo(todoData as TodoDTO)
                successCount++
            }
        }

        Message.success(`成功导入 ${successCount} 个任务`)
        await todoStore.fetchTodos()
        return true
    } catch (error) {
        Message.error('批量导入失败')
        return false
    }
}

// 页面加载时获取数据
onMounted(async () => {
    try {
        await Promise.all([
            todoStore.fetchTodos(),
            projectStore.fetchProjects()
            // 不在页面加载时获取用户数据，改为懒加载
        ])
    } catch (error) {
        Message.error('数据加载失败')
    }

    // 添加头部按钮事件监听
    window.addEventListener('header-create-click', handleHeaderCreate)
})

onUnmounted(() => {
    // 移除事件监听
    window.removeEventListener('header-create-click', handleHeaderCreate)
})
</script>

<style scoped>
.todos-page {
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

.filter-section {
    margin-bottom: 16px;
    padding: 16px;
    background: var(--card-bg-color);
    border-radius: 6px;
    border: 1px solid var(--border-color);
}

.filter-tabs {
    margin-bottom: 20px;
}

.todo-list-card {
    margin-top: 0;
}

.task-title {
    display: flex;
    align-items: center;
    gap: 8px;
}

.task-title .completed {
    text-decoration: line-through;
    color: var(--text-muted);
}

.description-action {
    display: flex;
    align-items: center;
    justify-content: center;
}

.description-btn {
    color: var(--primary-color);
    border: none;
    padding: 4px 8px;
    font-size: 12px;
}

.description-btn:hover {
    background-color: var(--primary-color-light);
}

.no-description-text {
    color: var(--text-muted);
    font-size: 12px;
    font-style: italic;
}

.description-modal-content {
    padding: 4px 0;
}

.description-header {
    margin-bottom: 16px;
}

.description-header h3 {
    margin: 0 0 8px 0;
    font-size: 18px;
    color: var(--text-color);
}

.task-info {
    display: flex;
    gap: 8px;
}

.description-content-modal {
    max-height: 400px;
    overflow-y: auto;
}

.description-text {
    white-space: pre-line;
    line-height: 1.6;
    color: var(--text-color);
    font-size: 14px;
    padding: 16px;
    background-color: var(--card-bg-color);
    border-radius: 6px;
    border: 1px solid var(--border-color);
}

.no-description-placeholder {
    text-align: center;
    color: var(--text-muted);
    font-style: italic;
    padding: 32px;
}

.user-option {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.user-main {
    font-weight: 500;
    color: var(--text-color);
}

.user-sub {
    font-size: 12px;
    color: var(--text-muted);
}

.user-option-inline {
    white-space: nowrap;
}

.email-preview {
    border: 1px solid var(--border-color);
    border-radius: 6px;
    padding: 16px;
    background: var(--card-bg-color);
}

.email-header {
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid var(--border-color);
}

.assignee-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 8px 0;
}

.email-header p {
    margin: 4px 0;
    color: var(--text-muted);
}

.email-table {
    background: var(--card-bg-color);
    border-radius: 4px;
}

.form-error-tip {
    margin-bottom: 16px;
}

.form-error-tip ul {
    margin: 8px 0 0 0;
    padding-left: 20px;
}

.form-error-tip li {
    margin: 4px 0;
    color: #f53f3f;
}

.form-error-tip {
    margin-bottom: 16px;
}

.form-error-tip ul {
    margin: 8px 0 0 0;
    padding-left: 20px;
}

.form-error-tip li {
    margin: 4px 0;
    color: #f53f3f;
}
</style>