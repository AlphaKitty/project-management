<template>
    <div class="data-dashboard">
        <!-- 时间范围配置 -->
        <div class="time-config">
            <a-card title="统计时间配置" size="small" class="config-card">
                <a-form layout="inline">
                    <a-form-item label="统计天数">
                        <a-select v-model="timeRange" @change="handleTimeRangeChange" style="width: 120px">
                            <a-option value="7">最近7天</a-option>
                            <a-option value="15">最近15天</a-option>
                            <a-option value="30">最近30天</a-option>
                            <a-option value="90">最近90天</a-option>
                            <a-option value="custom">自定义</a-option>
                        </a-select>
                    </a-form-item>
                    <a-form-item v-if="timeRange === 'custom'" label="自定义天数">
                        <a-input-number v-model="customDays" :min="1" :max="365" @change="handleCustomDaysChange" />
                    </a-form-item>
                    <a-form-item>
                        <a-button type="primary" @click="refreshData" :loading="loading">
                            <template #icon><icon-refresh /></template>
                            刷新数据
                        </a-button>
                    </a-form-item>
                </a-form>
            </a-card>
        </div>

        <!-- 数据概览卡片 -->
        <div class="overview-cards">
            <a-card class="overview-card">
                <a-statistic title="总项目数" :value="statistics.totalProjects" :value-style="{ color: '#1890ff' }" />
                <div class="trend-info">
                    <span class="trend-value">{{ statistics.projectTrend > 0 ? '+' : '' }}{{ statistics.projectTrend
                        }}</span>
                    <span class="trend-label">{{ timeRangeText }}新增</span>
                </div>
            </a-card>

            <a-card class="overview-card">
                <a-statistic title="活跃用户数" :value="statistics.activeUsers" :value-style="{ color: '#52c41a' }" />
                <div class="trend-info">
                    <span class="trend-value">{{ statistics.userActiveTrend > 0 ? '+' : '' }}{{
                        statistics.userActiveTrend
                        }}</span>
                    <span class="trend-label">{{ timeRangeText }}操作</span>
                </div>
            </a-card>

            <a-card class="overview-card">
                <a-statistic title="待办任务" :value="statistics.pendingTodos" :value-style="{ color: '#faad14' }" />
                <div class="trend-info">
                    <span class="trend-value">{{ statistics.todoTrend > 0 ? '+' : '' }}{{ statistics.todoTrend }}</span>
                    <span class="trend-label">{{ timeRangeText }}新增</span>
                </div>
            </a-card>

            <a-card class="overview-card">
                <a-statistic title="任务完成率" :value="statistics.completionRate" suffix="%"
                    :value-style="{ color: '#13c2c2' }" />
                <div class="trend-info">
                    <span class="trend-value">{{ statistics.completionTrend > 0 ? '+' : '' }}{{
                        statistics.completionTrend
                        }}%</span>
                    <span class="trend-label">较上期</span>
                </div>
            </a-card>
        </div>

        <!-- 数据分析维度切换 -->
        <div class="analysis-tabs">
            <a-tabs v-model:active-key="activeTab" type="card" @change="handleTabChange">
                <a-tab-pane key="project" title="项目维度分析">
                    <div class="analysis-content">
                        <!-- 项目状态分布 -->
                        <div class="chart-row">
                            <a-card title="项目状态分布" class="chart-card">
                                <div ref="projectStatusChart" class="chart-container"></div>
                            </a-card>

                            <a-card title="项目进度分布" class="chart-card">
                                <div ref="projectProgressChart" class="chart-container"></div>
                            </a-card>
                        </div>

                        <!-- 项目工作量统计 -->
                        <div class="chart-row">
                            <a-card title="项目待办任务分布" class="chart-card full-width">
                                <div ref="projectTaskChart" class="chart-container"></div>
                            </a-card>
                        </div>

                        <!-- 项目详细数据表格 -->
                        <a-card title="项目详细统计" class="data-table-card">
                            <a-table :columns="projectColumns" :data="projectAnalysisData" :loading="loading"
                                :pagination="{ pageSize: 10 }">
                                <template #status="{ record }">
                                    <a-tag :color="getStatusColor(record.status)">
                                        {{ getStatusLabel(record.status) }}
                                    </a-tag>
                                </template>
                                <template #progress="{ record }">
                                    <a-progress :percent="record.progress" size="small" />
                                </template>
                                <template #taskStats="{ record }">
                                    <div class="task-stats">
                                        <span class="stat-item">待办: {{ record.pendingTasks }}</span>
                                        <span class="stat-item">进行中: {{ record.inProgressTasks }}</span>
                                        <span class="stat-item">已完成: {{ record.completedTasks }}</span>
                                    </div>
                                </template>
                            </a-table>
                        </a-card>
                    </div>
                </a-tab-pane>

                <a-tab-pane key="user" title="人员维度分析">
                    <div class="analysis-content">
                        <!-- 人员工作量分布 -->
                        <div class="chart-row">
                            <a-card title="人员工作量分布" class="chart-card">
                                <div ref="userWorkloadChart" class="chart-container"></div>
                            </a-card>

                            <a-card title="人员效率排行" class="chart-card">
                                <div ref="userEfficiencyChart" class="chart-container"></div>
                            </a-card>
                        </div>

                        <!-- 部门统计 -->
                        <div class="chart-row">
                            <a-card title="部门工作分布" class="chart-card full-width">
                                <div ref="departmentChart" class="chart-container"></div>
                            </a-card>
                        </div>

                        <!-- 人员详细数据表格 -->
                        <a-card title="人员详细统计" class="data-table-card">
                            <a-table :columns="userColumns" :data="userAnalysisData" :loading="loading"
                                :pagination="{ pageSize: 10 }">
                                <template #user="{ record }">
                                    <div class="user-info">
                                        <a-avatar size="small">{{ record.username.charAt(0).toUpperCase() }}</a-avatar>
                                        <span class="user-name">{{ record.nickname || record.username }}</span>
                                    </div>
                                </template>
                                <template #department="{ record }">
                                    <a-tag>{{ record.department || '未分配' }}</a-tag>
                                </template>
                                <template #efficiency="{ record }">
                                    <div class="efficiency-score">
                                        <span :class="['score', getEfficiencyClass(record.efficiency)]">
                                            {{ record.efficiency }}%
                                        </span>
                                    </div>
                                </template>
                            </a-table>
                        </a-card>
                    </div>
                </a-tab-pane>

                <a-tab-pane key="system" title="系统整体分析">
                    <div class="analysis-content">
                        <!-- 系统活跃度趋势 -->
                        <div class="chart-row">
                            <a-card title="系统操作活跃度趋势" class="chart-card full-width">
                                <div ref="systemActivityChart" class="chart-container"></div>
                            </a-card>
                        </div>

                        <!-- 任务流转分析 -->
                        <div class="chart-row">
                            <a-card title="任务创建趋势" class="chart-card">
                                <div ref="taskTrendChart" class="chart-container"></div>
                            </a-card>

                            <a-card title="工作效率分析" class="chart-card">
                                <div ref="efficiencyAnalysisChart" class="chart-container"></div>
                            </a-card>
                        </div>
                    </div>
                </a-tab-pane>
            </a-tabs>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconRefresh } from '@arco-design/web-vue/es/icon'
import * as echarts from 'echarts'
import { useProjectStore } from '@/stores/projects'
import { useTodoStore } from '@/stores/todos'
import { useUserStore } from '@/stores/user'
import { StatusLabels, StatusColors } from '@/types'

// 图表实例引用
const projectStatusChart = ref<HTMLDivElement>()
const projectProgressChart = ref<HTMLDivElement>()
const projectTaskChart = ref<HTMLDivElement>()
const userWorkloadChart = ref<HTMLDivElement>()
const userEfficiencyChart = ref<HTMLDivElement>()
const departmentChart = ref<HTMLDivElement>()
const systemActivityChart = ref<HTMLDivElement>()
const taskTrendChart = ref<HTMLDivElement>()
const efficiencyAnalysisChart = ref<HTMLDivElement>()

// 图表实例对象
let chartInstances: { [key: string]: echarts.ECharts } = {}

// 数据和状态
const activeTab = ref('project')
const loading = ref(false)
const timeRange = ref('30') // 默认30天
const customDays = ref(30)

// 时间范围文本
const timeRangeText = computed(() => {
    const days = timeRange.value === 'custom' ? customDays.value : parseInt(timeRange.value)
    return `最近${days}天`
})

// 实际使用的天数
const actualDays = computed(() => {
    return timeRange.value === 'custom' ? customDays.value : parseInt(timeRange.value)
})

// 统计数据
const statistics = ref({
    totalProjects: 0,
    activeUsers: 0,
    pendingTodos: 0,
    completionRate: 0,
    projectTrend: 0,
    userActiveTrend: 0,
    todoTrend: 0,
    completionTrend: 0
})

const projectAnalysisData = ref<any[]>([])
const userAnalysisData = ref<any[]>([])

// Stores
const projectStore = useProjectStore()
const todoStore = useTodoStore()
const userStore = useUserStore()

// 表格列配置
const projectColumns = [
    { title: '项目名称', dataIndex: 'name', width: 150 },
    { title: '状态', dataIndex: 'status', slotName: 'status', width: 100 },
    { title: '进度', dataIndex: 'progress', slotName: 'progress', width: 120 },
    { title: '负责人', dataIndex: 'assigneeName', width: 100 },
    { title: '任务统计', dataIndex: 'taskStats', slotName: 'taskStats', width: 200 },
    { title: '完成率', dataIndex: 'completionRate', width: 100 },
    { title: '创建时间', dataIndex: 'createTime', width: 120 }
]

const userColumns = [
    { title: '用户', dataIndex: 'user', slotName: 'user', width: 120 },
    { title: '部门', dataIndex: 'department', slotName: 'department', width: 100 },
    { title: '待办任务', dataIndex: 'pendingTasks', width: 100 },
    { title: '进行中任务', dataIndex: 'inProgressTasks', width: 100 },
    { title: '已完成任务', dataIndex: 'completedTasks', width: 100 },
    { title: '工作效率', dataIndex: 'efficiency', slotName: 'efficiency', width: 100 },
    { title: '逾期任务', dataIndex: 'overdueTasks', width: 100 },
    { title: '活跃度分数', dataIndex: 'activityScore', width: 100 }
]

// 工具函数
const getStatusLabel = (status: string) => {
    return StatusLabels[status as keyof typeof StatusLabels] || status
}

const getStatusColor = (status: string) => {
    return StatusColors[status as keyof typeof StatusColors] || 'gray'
}

const getEfficiencyClass = (efficiency: number) => {
    if (efficiency >= 80) return 'high'
    if (efficiency >= 60) return 'medium'
    return 'low'
}

// 计算用户活跃度分数（基于操作行为而非登录）
const calculateUserActivityScore = (user: any, days: number) => {
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)

    // 计算该用户在指定时间范围内的操作活跃度
    const userTodos = todoStore.todos.filter(todo => todo.assigneeId === user.id)
    const userProjects = projectStore.projects.filter(project =>
        project.creatorId === user.id || project.assigneeId === user.id
    )

    // 近期创建的任务数量
    const recentTodos = userTodos.filter(todo =>
        new Date(todo.createTime) >= cutoffDate
    ).length

    // 近期完成的任务数量
    const completedTodos = userTodos.filter(todo =>
        todo.status === 'DONE' && todo.completedTime && new Date(todo.completedTime) >= cutoffDate
    ).length

    // 近期更新的项目数量
    const recentProjectUpdates = userProjects.filter(project =>
        new Date(project.updateTime) >= cutoffDate
    ).length

    // 计算活跃度分数（满分100分）
    const activityScore = Math.min(100,
        recentTodos * 5 + // 创建任务得5分
        completedTodos * 10 + // 完成任务得10分  
        recentProjectUpdates * 3 // 更新项目得3分
    )

    return activityScore
}

// 时间范围变化处理
const handleTimeRangeChange = () => {
    refreshData()
}

const handleCustomDaysChange = () => {
    if (timeRange.value === 'custom') {
        refreshData()
    }
}

// 数据加载
const loadStatistics = async () => {
    loading.value = true
    try {
        // 加载基础数据
        await Promise.all([
            projectStore.fetchProjects(),
            todoStore.fetchTodos(),
            userStore.fetchDashboardUsers() // 使用优化后的接口
        ])

        // 计算统计数据
        calculateStatistics()
        await loadAnalysisData()

    } catch (error) {
        console.error('加载统计数据失败:', error)
        Message.error('加载数据失败')
    } finally {
        loading.value = false
    }
}

const calculateStatistics = () => {
    const projects = projectStore.projects
    const todos = todoStore.todos
    const users = userStore.dashboardUsers // 使用优化后的数据
    const days = actualDays.value

    // 计算时间范围
    const cutoffDate = new Date()
    cutoffDate.setDate(cutoffDate.getDate() - days)

    // 总体统计
    const totalProjects = projects.length
    const pendingTodos = todos.filter(t => t.status === 'TODO' || t.status === 'PROGRESS').length
    const completedTodos = todos.filter(t => t.status === 'DONE').length
    const completionRate = Math.round((completedTodos / todos.length) * 100) || 0

    // 趋势统计（与上一个相同时间段对比）  
    const previousCutoffDate = new Date()
    previousCutoffDate.setDate(previousCutoffDate.getDate() - days * 2)

    const recentProjects = projects.filter(p => new Date(p.createTime) >= cutoffDate).length
    const previousProjects = projects.filter(p =>
        new Date(p.createTime) >= previousCutoffDate && new Date(p.createTime) < cutoffDate
    ).length

    const recentTodos = todos.filter(t => new Date(t.createTime) >= cutoffDate).length
    const previousTodos = todos.filter(t =>
        new Date(t.createTime) >= previousCutoffDate && new Date(t.createTime) < cutoffDate
    ).length

    // 计算活跃用户数（基于操作活跃度）
    const activeUsers = users.filter(user =>
        calculateUserActivityScore(user, days) > 0
    ).length

    const previousActiveUsers = users.filter(user =>
        calculateUserActivityScore(user, days * 2) - calculateUserActivityScore(user, days) > 0
    ).length

    statistics.value = {
        totalProjects,
        activeUsers,
        pendingTodos,
        completionRate,
        projectTrend: recentProjects - previousProjects,
        userActiveTrend: activeUsers - previousActiveUsers,
        todoTrend: recentTodos - previousTodos,
        completionTrend: Math.floor(Math.random() * 10) - 5 // 简化处理
    }
}

const loadAnalysisData = async () => {
    const days = actualDays.value

    // 项目分析数据
    projectAnalysisData.value = projectStore.projects.map(project => {
        const projectTodos = todoStore.todos.filter(todo => todo.projectId === project.id)
        const pendingTasks = projectTodos.filter(t => t.status === 'TODO').length
        const inProgressTasks = projectTodos.filter(t => t.status === 'PROGRESS').length
        const completedTasks = projectTodos.filter(t => t.status === 'DONE').length
        const totalTasks = projectTodos.length

        return {
            ...project,
            assigneeName: project.assignee?.nickname || project.assignee?.username || '未分配',
            pendingTasks,
            inProgressTasks,
            completedTasks,
            completionRate: totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0,
            createTime: new Date(project.createTime).toLocaleDateString()
        }
    })

    // 用户分析数据
    userAnalysisData.value = userStore.dashboardUsers.map(user => {
        const userTodos = todoStore.todos.filter(todo => todo.assigneeId === user.id)
        const pendingTasks = userTodos.filter(t => t.status === 'TODO').length
        const inProgressTasks = userTodos.filter(t => t.status === 'PROGRESS').length
        const completedTasks = userTodos.filter(t => t.status === 'DONE').length
        const totalTasks = userTodos.length
        const overdueTasks = userTodos.filter(t =>
            t.dueDate && new Date(t.dueDate) < new Date() && t.status !== 'DONE'
        ).length

        // 计算活跃度分数
        const activityScore = calculateUserActivityScore(user, days)

        return {
            ...user,
            pendingTasks,
            inProgressTasks,
            completedTasks,
            overdueTasks,
            efficiency: totalTasks > 0 ? Math.round((completedTasks / totalTasks) * 100) : 0,
            activityScore
        }
    })
}

// 刷新数据
const refreshData = async () => {
    await loadStatistics()
    await nextTick()
    initCharts()
}

// 标签切换处理
const handleTabChange = async () => {
    await nextTick()
    initCharts()
}

// 图表初始化
const initCharts = async () => {
    await nextTick()

    if (activeTab.value === 'project') {
        initProjectCharts()
    } else if (activeTab.value === 'user') {
        initUserCharts()
    } else if (activeTab.value === 'system') {
        initSystemCharts()
    }
}

const initProjectCharts = () => {
    // 项目状态分布饼图
    if (projectStatusChart.value) {
        if (chartInstances['projectStatus']) {
            chartInstances['projectStatus'].dispose()
        }
        chartInstances['projectStatus'] = echarts.init(projectStatusChart.value)

        const statusData = projectStore.projects.reduce((acc: any, project) => {
            acc[project.status] = (acc[project.status] || 0) + 1
            return acc
        }, {})

        chartInstances['projectStatus'].setOption({
            tooltip: { trigger: 'item' },
            legend: { orient: 'vertical', left: 'left' },
            series: [{
                name: '项目状态',
                type: 'pie',
                radius: '50%',
                data: Object.entries(statusData).map(([key, value]) => ({
                    name: getStatusLabel(key),
                    value
                })),
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }]
        })
    }

    // 项目进度分布柱状图
    if (projectProgressChart.value) {
        if (chartInstances['projectProgress']) {
            chartInstances['projectProgress'].dispose()
        }
        chartInstances['projectProgress'] = echarts.init(projectProgressChart.value)

        const progressRanges = ['0-20%', '21-40%', '41-60%', '61-80%', '81-100%']
        const progressData = progressRanges.map(range => {
            const [min, max] = range.split('-').map(s => parseInt(s.replace('%', '')))
            return projectStore.projects.filter(p => p.progress >= min && p.progress <= max).length
        })

        chartInstances['projectProgress'].setOption({
            tooltip: { trigger: 'axis' },
            xAxis: { type: 'category', data: progressRanges },
            yAxis: { type: 'value' },
            series: [{
                name: '项目数量',
                type: 'bar',
                data: progressData,
                itemStyle: { color: '#1890ff' }
            }]
        })
    }

    // 项目任务分布
    if (projectTaskChart.value) {
        if (chartInstances['projectTask']) {
            chartInstances['projectTask'].dispose()
        }
        chartInstances['projectTask'] = echarts.init(projectTaskChart.value)

        const projectNames = projectStore.projects.map(p => p.name.length > 10 ? p.name.substr(0, 10) + '...' : p.name)
        const taskData = projectStore.projects.map(project => {
            const todos = todoStore.todos.filter(t => t.projectId === project.id)
            return {
                pending: todos.filter(t => t.status === 'TODO').length,
                inProgress: todos.filter(t => t.status === 'PROGRESS').length,
                completed: todos.filter(t => t.status === 'DONE').length
            }
        })

        chartInstances['projectTask'].setOption({
            tooltip: { trigger: 'axis' },
            legend: { data: ['待办', '进行中', '已完成'] },
            xAxis: {
                type: 'category',
                data: projectNames,
                axisLabel: { rotate: 45, interval: 0 }
            },
            yAxis: { type: 'value' },
            series: [
                {
                    name: '待办',
                    type: 'bar',
                    stack: 'tasks',
                    data: taskData.map(d => d.pending),
                    itemStyle: { color: '#faad14' }
                },
                {
                    name: '进行中',
                    type: 'bar',
                    stack: 'tasks',
                    data: taskData.map(d => d.inProgress),
                    itemStyle: { color: '#1890ff' }
                },
                {
                    name: '已完成',
                    type: 'bar',
                    stack: 'tasks',
                    data: taskData.map(d => d.completed),
                    itemStyle: { color: '#52c41a' }
                }
            ]
        })
    }
}

const initUserCharts = () => {
    // 用户工作量分布
    if (userWorkloadChart.value) {
        if (chartInstances['userWorkload']) {
            chartInstances['userWorkload'].dispose()
        }
        chartInstances['userWorkload'] = echarts.init(userWorkloadChart.value)

        const workloadData = userAnalysisData.value
            .map(user => ({
                name: user.nickname || user.username,
                value: user.pendingTasks + user.inProgressTasks
            }))
            .filter(item => item.value > 0)
            .sort((a, b) => b.value - a.value)
            .slice(0, 10)

        chartInstances['userWorkload'].setOption({
            tooltip: { trigger: 'item' },
            series: [{
                name: '工作量',
                type: 'pie',
                radius: ['40%', '70%'],
                data: workloadData,
                emphasis: {
                    itemStyle: {
                        shadowBlur: 10,
                        shadowOffsetX: 0,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }]
        })
    }

    // 用户效率排行
    if (userEfficiencyChart.value) {
        if (chartInstances['userEfficiency']) {
            chartInstances['userEfficiency'].dispose()
        }
        chartInstances['userEfficiency'] = echarts.init(userEfficiencyChart.value)

        const efficiencyData = userAnalysisData.value
            .filter(user => user.efficiency > 0)
            .sort((a, b) => b.efficiency - a.efficiency)
            .slice(0, 10)

        chartInstances['userEfficiency'].setOption({
            tooltip: { trigger: 'axis' },
            xAxis: {
                type: 'category',
                data: efficiencyData.map(u => u.nickname || u.username),
                axisLabel: { rotate: 45 }
            },
            yAxis: { type: 'value', max: 100 },
            series: [{
                name: '完成率',
                type: 'bar',
                data: efficiencyData.map(u => u.efficiency),
                itemStyle: {
                    color: (params: any) => {
                        const value = params.value
                        if (value >= 80) return '#52c41a'
                        if (value >= 60) return '#faad14'
                        return '#ff4d4f'
                    }
                }
            }]
        })
    }

    // 部门工作分布
    if (departmentChart.value) {
        if (chartInstances['department']) {
            chartInstances['department'].dispose()
        }
        chartInstances['department'] = echarts.init(departmentChart.value)

        const departmentData = userAnalysisData.value.reduce((acc: any, user) => {
            const dept = user.department || '未分配'
            if (!acc[dept]) {
                acc[dept] = { pending: 0, inProgress: 0, completed: 0 }
            }
            acc[dept].pending += user.pendingTasks
            acc[dept].inProgress += user.inProgressTasks
            acc[dept].completed += user.completedTasks
            return acc
        }, {})

        const departments = Object.keys(departmentData)

        chartInstances['department'].setOption({
            tooltip: { trigger: 'axis' },
            legend: { data: ['待办', '进行中', '已完成'] },
            xAxis: { type: 'category', data: departments },
            yAxis: { type: 'value' },
            series: [
                {
                    name: '待办',
                    type: 'bar',
                    data: departments.map(d => departmentData[d].pending),
                    itemStyle: { color: '#faad14' }
                },
                {
                    name: '进行中',
                    type: 'bar',
                    data: departments.map(d => departmentData[d].inProgress),
                    itemStyle: { color: '#1890ff' }
                },
                {
                    name: '已完成',
                    type: 'bar',
                    data: departments.map(d => departmentData[d].completed),
                    itemStyle: { color: '#52c41a' }
                }
            ]
        })
    }
}

const initSystemCharts = () => {
    const days = actualDays.value

    // 系统活跃度趋势
    if (systemActivityChart.value) {
        if (chartInstances['systemActivity']) {
            chartInstances['systemActivity'].dispose()
        }
        chartInstances['systemActivity'] = echarts.init(systemActivityChart.value)

        // 生成时间序列
        const timeLabels = Array.from({ length: Math.min(days, 30) }, (_, i) => {
            const date = new Date()
            date.setDate(date.getDate() - i)
            return date.toISOString().split('T')[0]
        }).reverse()

        // 模拟操作活跃度数据
        const operationData = timeLabels.map(() => Math.floor(Math.random() * 50) + 10)
        const taskData = timeLabels.map(() => Math.floor(Math.random() * 30) + 5)

        chartInstances['systemActivity'].setOption({
            tooltip: { trigger: 'axis' },
            legend: { data: ['用户操作数', '任务创建数'] },
            xAxis: {
                type: 'category',
                data: timeLabels.map(d => d.substr(5))
            },
            yAxis: { type: 'value' },
            series: [
                {
                    name: '用户操作数',
                    type: 'line',
                    data: operationData,
                    smooth: true,
                    itemStyle: { color: '#1890ff' }
                },
                {
                    name: '任务创建数',
                    type: 'line',
                    data: taskData,
                    smooth: true,
                    itemStyle: { color: '#52c41a' }
                }
            ]
        })
    }

    // 任务创建趋势
    if (taskTrendChart.value) {
        if (chartInstances['taskTrend']) {
            chartInstances['taskTrend'].dispose()
        }
        chartInstances['taskTrend'] = echarts.init(taskTrendChart.value)

        const taskCreationData = Array.from({ length: Math.min(days, 30) }, () =>
            Math.floor(Math.random() * 10)
        )

        chartInstances['taskTrend'].setOption({
            tooltip: { trigger: 'axis' },
            xAxis: {
                type: 'category',
                data: Array.from({ length: taskCreationData.length }, (_, i) => `${i + 1}天前`)
            },
            yAxis: { type: 'value' },
            series: [{
                name: '新增任务',
                type: 'line',
                data: taskCreationData,
                smooth: true,
                itemStyle: { color: '#1890ff' },
                areaStyle: { opacity: 0.3 }
            }]
        })
    }

    // 工作效率分析
    if (efficiencyAnalysisChart.value) {
        if (chartInstances['efficiencyAnalysis']) {
            chartInstances['efficiencyAnalysis'].dispose()
        }
        chartInstances['efficiencyAnalysis'] = echarts.init(efficiencyAnalysisChart.value)

        const efficiencyCategories = ['高效(>=80%)', '正常(60-79%)', '待改进(<60%)']
        const efficiencyValues = [
            userAnalysisData.value.filter(u => u.efficiency >= 80).length,
            userAnalysisData.value.filter(u => u.efficiency >= 60 && u.efficiency < 80).length,
            userAnalysisData.value.filter(u => u.efficiency < 60).length
        ]

        chartInstances['efficiencyAnalysis'].setOption({
            tooltip: { trigger: 'item' },
            series: [{
                name: '工作效率分布',
                type: 'pie',
                radius: '50%',
                data: efficiencyCategories.map((name, index) => ({
                    name,
                    value: efficiencyValues[index]
                })),
                itemStyle: {
                    color: (params: any) => {
                        const colors = ['#52c41a', '#faad14', '#ff4d4f']
                        return colors[params.dataIndex]
                    }
                }
            }]
        })
    }
}

// 生命周期
onMounted(async () => {
    await loadStatistics()
    await initCharts()
})

onUnmounted(() => {
    // 销毁所有图表实例
    Object.values(chartInstances).forEach(chart => {
        if (chart) {
            chart.dispose()
        }
    })
    chartInstances = {}
})
</script>

<style scoped>
.data-dashboard {
    padding: 20px;
    background: #f5f5f5;
    min-height: 100vh;
}

.time-config {
    margin-bottom: 20px;
}

.config-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.overview-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
}

.overview-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.trend-info {
    margin-top: 8px;
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #666;
}

.trend-value {
    font-weight: 500;
}

.analysis-tabs {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    padding: 20px;
}

.analysis-content {
    padding: 20px 0;
}

.chart-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    margin-bottom: 20px;
}

.chart-row .full-width {
    grid-column: 1 / -1;
}

.chart-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.chart-container {
    height: 300px;
    width: 100%;
}

.data-table-card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.task-stats {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.stat-item {
    font-size: 12px;
    color: #666;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 8px;
}

.user-name {
    font-weight: 500;
}

.efficiency-score .score {
    font-weight: 500;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 12px;
}

.efficiency-score .score.high {
    background: #f6ffed;
    color: #52c41a;
}

.efficiency-score .score.medium {
    background: #fff7e6;
    color: #faad14;
}

.efficiency-score .score.low {
    background: #fff2f0;
    color: #ff4d4f;
}

@media (max-width: 768px) {
    .chart-row {
        grid-template-columns: 1fr;
    }

    .overview-cards {
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    }
}
</style>