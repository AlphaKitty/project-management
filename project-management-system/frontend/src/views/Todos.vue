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
                        <a-option :value="undefined">全部项目</a-option>
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
                    </a-space>
                </a-col>
            </a-row>
        </div>

        <!-- 快捷筛选 -->
        <div class="filter-tabs">
            <a-tabs v-model:active-key="activeTab" @change="handleTabChange">
                <a-tab-pane key="all" title="全部任务" />
                <a-tab-pane key="today" title="今日任务" />
                <a-tab-pane key="week" title="本周任务" />
                <a-tab-pane key="high" title="高优先级" />
                <a-tab-pane key="overdue" title="已逾期任务" />
                <a-tab-pane key="completed" title="已完成任务" />
            </a-tabs>
        </div>

        <!-- 任务列表 -->
        <a-card title="任务列表" class="todo-list-card">
            <a-table :columns="columns" :data="filteredTodos" :loading="todoStore.loading"
                :pagination="{ pageSize: 10 }">
                <template #title="{ record }">
                    <div class="task-title">
                        <a-checkbox :model-value="record.status === 'DONE'" @change="toggleTaskStatus(record)" />
                        <span :class="{ 'completed': record.status === 'DONE' }">
                            {{ record.title }}
                        </span>
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
            </a-form>
        </a-modal>

        <!-- 发送邮件模态框 --> <a-modal v-model:visible="sendEmailModal" title="发送待办任务邮件" @before-ok="handleSendEmail"
            @cancel="sendEmailModal = false" width="800px"> <a-form layout="vertical"> <a-form-item label="邮件发送说明">
                    <a-alert type="info" show-icon> <template #title>邮件将自动发送给待办任务的责任人</template>
                        系统将根据所选范围内的待办任务，自动向每个任务的责任人发送邮件提醒。
                    </a-alert> </a-form-item> <a-form-item label="发送范围"> <a-radio-group v-model="emailForm.scope"
                        @change="updateEmailTodoList"> <a-radio value="all">所有待办任务</a-radio> <a-radio
                            value="today">今日任务</a-radio> <a-radio value="week">本周任务</a-radio> </a-radio-group>
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
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconPlus, IconEmail } from '@arco-design/web-vue/es/icon'
import { useTodoStore } from '@/stores/todos'
import { useProjectStore } from '@/stores/projects'
import { useUserStore } from '@/stores/user'
import { StatusLabels, StatusColors } from '@/types'
import type { Todo, TodoDTO, User } from '@/types'

// Store
const todoStore = useTodoStore()
const projectStore = useProjectStore()
const userStore = useUserStore()

// 响应式数据
const modalVisible = ref(false)
const sendEmailModal = ref(false)
const isEdit = ref(false)
const activeTab = ref('all')
const selectedProjectId = ref<number | undefined>(undefined)
const formRef = ref()
const userSearchText = ref('')
const searchResultUsers = ref<User[]>([])
const submitting = ref(false)
const formErrors = ref<Record<string, string>>({})

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
    creatorId: 1 // 暂时固定为管理员
})

const emailForm = ref({
    scope: 'all'
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
const columns = [
    { title: '任务标题', dataIndex: 'title', key: 'title', slotName: 'title', width: 200 },
    { title: '优先级', dataIndex: 'priority', key: 'priority', slotName: 'priority' },
    { title: '状态', dataIndex: 'status', key: 'status', slotName: 'status' },
    { title: '所属项目', dataIndex: 'project', key: 'project', slotName: 'project', width: 200 },
    { title: '负责人', dataIndex: 'assignee', key: 'assignee', slotName: 'assignee' },
    { title: '截止日期', dataIndex: 'dueDate', key: 'dueDate' },
    { title: '操作', key: 'actions', slotName: 'actions', width: 200 }
]

// 当前显示的任务列表（结合标签页和项目筛选）
const currentTodos = computed(() => {
    switch (activeTab.value) {
        case 'today':
            return todoStore.todayTodos
        case 'week':
            return todoStore.weekTodos
        case 'high':
            return todoStore.highPriorityTodos
        case 'overdue':
            return todoStore.overdueTodos
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
    console.log('🔍 表单验证检查 - 当前表单数据:', formData.value)

    // 检查必填字段
    if (!formData.value.title?.trim()) {
        console.log('❌ title 验证失败:', formData.value.title)
        return false
    }
    if (!formData.value.projectId) {
        console.log('❌ projectId 验证失败:', formData.value.projectId)
        return false
    }
    if (!formData.value.assigneeId) {
        console.log('❌ assigneeId 验证失败:', formData.value.assigneeId)
        return false
    }
    if (!formData.value.priority) {
        console.log('❌ priority 验证失败:', formData.value.priority)
        return false
    }
    if (!formData.value.status) {
        console.log('❌ status 验证失败:', formData.value.status)
        return false
    }

    // 检查字段长度
    if (formData.value.title.length < 2 || formData.value.title.length > 100) {
        console.log('❌ title 长度验证失败:', formData.value.title.length)
        return false
    }

    console.log('✅ 表单验证通过')
    return true
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
        // 未完成任务：检查是否逾期
        const today = new Date()
        const dueDate = new Date(todo.dueDate)
        if (dueDate < today) {
            return '已逾期'
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
        // 未完成任务：检查是否逾期
        const today = new Date()
        const dueDate = new Date(todo.dueDate)
        if (dueDate < today) {
            return 'red' // 逾期显示红色
        }
    }
    return StatusColors[todo.status as keyof typeof StatusColors] || 'gray'
}

// 项目筛选处理
const handleProjectFilter = (projectId: number | undefined) => {
    selectedProjectId.value = projectId
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
        case 'today':
            emailTodoList.value = todoStore.todayTodos.filter(todo => todo.status !== 'DONE')
            break
        case 'week':
            emailTodoList.value = todoStore.weekTodos.filter(todo => todo.status !== 'DONE')
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
    background: #fff;
    border-radius: 6px;
    border: 1px solid #f0f0f0;
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
    color: #999;
}

.user-option {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.user-main {
    font-weight: 500;
    color: #1d2129;
}

.user-sub {
    font-size: 12px;
    color: #86909c;
}

.user-option-inline {
    white-space: nowrap;
}

.email-preview {
    border: 1px solid #f0f0f0;
    border-radius: 6px;
    padding: 16px;
    background: #fafafa;
}

.email-header {
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e5e5e5;
}

.assignee-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 8px 0;
}

.email-header p {
    margin: 4px 0;
    color: #666;
}

.email-table {
    background: white;
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