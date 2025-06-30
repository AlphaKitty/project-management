<template>
    <div class="operation-logs-container">
        <!-- 筛选区域 -->
        <div class="filter-section">
            <a-card title="查询条件" :bordered="false">
                <a-form :model="queryForm" layout="vertical">
                    <!-- 第一行：基础查询条件 -->
                    <a-row :gutter="16">
                        <a-col :span="6">
                            <a-form-item label="用户名">
                                <a-input v-model="queryForm.username" placeholder="请输入用户名" allow-clear />
                            </a-form-item>
                        </a-col>
                        <a-col :span="6">
                            <a-form-item label="模块">
                                <a-select v-model="queryForm.module" placeholder="请选择模块" allow-clear>
                                    <a-option value="USER">用户管理</a-option>
                                    <a-option value="PROJECT">项目管理</a-option>
                                    <a-option value="TODO">待办任务</a-option>
                                    <a-option value="REPORT">报告管理</a-option>
                                    <a-option value="EMAIL_RULE">邮件规则</a-option>
                                    <a-option value="AUTH">认证授权</a-option>
                                    <a-option value="SYSTEM">系统管理</a-option>
                                </a-select>
                            </a-form-item>
                        </a-col>
                        <a-col :span="6">
                            <a-form-item label="操作类型">
                                <a-select v-model="queryForm.operationType" placeholder="请选择操作类型" allow-clear>
                                    <a-option value="CREATE">新增</a-option>
                                    <a-option value="UPDATE">修改</a-option>
                                    <a-option value="DELETE">删除</a-option>
                                    <a-option value="QUERY">查询</a-option>
                                    <a-option value="LOGIN">登录</a-option>
                                    <a-option value="LOGOUT">退出</a-option>
                                    <a-option value="EXPORT">导出</a-option>
                                    <a-option value="IMPORT">导入</a-option>
                                </a-select>
                            </a-form-item>
                        </a-col>
                        <a-col :span="6">
                            <a-form-item label="执行状态">
                                <a-select v-model="queryForm.success" placeholder="请选择执行状态" allow-clear>
                                    <a-option :value="true">成功</a-option>
                                    <a-option :value="false">失败</a-option>
                                </a-select>
                            </a-form-item>
                        </a-col>
                    </a-row>

                    <!-- 第二行：时间范围和操作按钮 -->
                    <a-row :gutter="16" style="margin-top: 16px">
                        <a-col :span="8">
                            <a-form-item label="时间范围">
                                <a-range-picker v-model="queryForm.timeRange" style="width: 100%"
                                    format="YYYY-MM-DD HH:mm:ss" show-time />
                            </a-form-item>
                        </a-col>
                        <a-col :span="16">
                            <a-form-item label="操作">
                                <a-space>
                                    <a-button type="primary" @click="handleSearch" :loading="loading">
                                        <template #icon>
                                            <icon-search />
                                        </template>
                                        查询
                                    </a-button>
                                    <a-button @click="handleReset">
                                        <template #icon>
                                            <icon-refresh />
                                        </template>
                                        重置
                                    </a-button>
                                    <a-button type="outline" @click="handleStatsClick" :loading="statsLoading"
                                        v-if="userStore.isAdmin">
                                        <template #icon>
                                            <icon-bar-chart />
                                        </template>
                                        {{ showStats ? '隐藏统计' : '显示统计' }}
                                    </a-button>
                                    <a-button type="outline" status="danger" @click="showCleanDialog"
                                        v-if="userStore.isAdmin">
                                        <template #icon>
                                            <icon-delete />
                                        </template>
                                        清理日志
                                    </a-button>
                                </a-space>
                            </a-form-item>
                        </a-col>
                    </a-row>
                </a-form>
            </a-card>
        </div>

        <!-- 统计信息（管理员可见） -->
        <div class="stats-section" v-if="userStore.isAdmin && showStats">
            <a-card title="统计信息" :bordered="false" :loading="statsLoading">
                <a-row :gutter="16" v-if="stats">
                    <a-col :span="6">
                        <a-statistic title="总记录数" :value="stats.totalCount" :value-style="{ color: '#1890ff' }" />
                    </a-col>
                    <a-col :span="6">
                        <a-statistic title="成功记录" :value="stats.successCount" :value-style="{ color: '#52c41a' }" />
                    </a-col>
                    <a-col :span="6">
                        <a-statistic title="失败记录" :value="stats.failCount" :value-style="{ color: '#ff4d4f' }" />
                    </a-col>
                    <a-col :span="6">
                        <a-statistic title="成功率"
                            :value="stats.totalCount > 0 ? Number(((stats.successCount / stats.totalCount) * 100).toFixed(2)) : 0"
                            suffix="%"
                            :value-style="{ color: stats.totalCount > 0 && (stats.successCount / stats.totalCount) >= 0.9 ? '#52c41a' : '#faad14' }" />
                    </a-col>
                </a-row>
                <a-empty v-else description="暂无统计数据" />
            </a-card>
        </div>

        <!-- 数据表格 -->
        <div class="table-section">
            <a-card :bordered="false">
                <template #title>
                    <span v-if="userStore.isAdmin">操作日志</span>
                    <span v-else>我的操作记录</span>
                </template>

                <a-table :columns="columns" :data="logData" :loading="loading" :pagination="pagination"
                    @page-change="handlePageChange" @page-size-change="handlePageSizeChange" :scroll="{ x: 1800 }"
                    row-key="id">
                    <!-- 操作类型插槽 -->
                    <template #operationType="{ record }">
                        <a-tag :color="getOperationTypeColor(record.operationType)">
                            {{ getOperationTypeText(record.operationType) }}
                        </a-tag>
                    </template>

                    <!-- 模块插槽 -->
                    <template #module="{ record }">
                        <a-tag color="blue">{{ getModuleText(record.module) }}</a-tag>
                    </template>

                    <!-- 请求方式插槽 -->
                    <template #requestMethod="{ record }">
                        <a-tag :color="getMethodColor(record.requestMethod)">
                            {{ record.requestMethod }}
                        </a-tag>
                    </template>

                    <!-- 执行状态插槽 -->
                    <template #success="{ record }">
                        <a-tag :color="record.success ? 'green' : 'red'">
                            {{ record.success ? '成功' : '失败' }}
                        </a-tag>
                    </template>

                    <!-- 操作时间插槽 -->
                    <template #operationTime="{ record }">
                        {{ formatDateTime(record.operationTime) }}
                    </template>

                    <!-- 操作插槽 -->
                    <template #actions="{ record }">
                        <a-button type="text" size="small" @click="showDetail(record)">
                            <template #icon>
                                <icon-eye />
                            </template>
                            详情
                        </a-button>
                    </template>
                </a-table>
            </a-card>
        </div>

        <!-- 详情弹窗 -->
        <a-modal v-model:visible="detailVisible" title="操作详情" width="800px" :footer="false" :esc-to-close="true">
            <div class="detail-content" v-if="currentLog">
                <a-descriptions :data="detailData" layout="vertical" :column="2" />

                <a-divider>请求参数</a-divider>
                <pre class="json-viewer">{{ formatJson(currentLog.requestParams) }}</pre>

                <a-divider>响应结果</a-divider>
                <pre class="json-viewer">{{ formatJson(currentLog.response) }}</pre>

                <div v-if="!currentLog.success && currentLog.errorMessage">
                    <a-divider>错误信息</a-divider>
                    <a-alert type="error" :message="currentLog.errorMessage" />
                </div>
            </div>
        </a-modal>

        <!-- 清理日志确认弹窗 -->
        <a-modal v-model:visible="cleanVisible" title="清理过期日志" @ok="handleCleanLogs" ok-text="确认清理" cancel-text="取消" :esc-to-close="true">
            <a-form :model="cleanForm" layout="vertical">
                <a-form-item label="保留天数">
                    <a-input-number v-model="cleanForm.keepDays" :min="1" :max="365" placeholder="请输入保留天数"
                        style="width: 200px" />
                    <div style="color: #999; font-size: 12px; margin-top: 4px;">
                        将删除 {{ cleanForm.keepDays }} 天前的所有日志记录
                    </div>
                </a-form-item>
            </a-form>
        </a-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
    IconSearch,
    IconRefresh,
    IconBarChart,
    IconDelete,
    IconEye
} from '@arco-design/web-vue/es/icon'
import { useUserStore } from '@/stores/user'
import {
    getOperationLogs,
    getMyOperationLogs,
    getOperationStats,
    cleanExpiredLogs,
    type OperationLog,
    type OperationLogQuery,
    type OperationStats,
    type IPage
} from '@/api/operationLogs'

const userStore = useUserStore()

// 响应式数据
const loading = ref(false)
const statsLoading = ref(false)
const showStats = ref(false)
const logData = ref<OperationLog[]>([])
const stats = ref<OperationStats | null>(null)
const detailVisible = ref(false)
const cleanVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

// 查询表单
const queryForm = reactive<{
    username?: string
    module?: string
    operationType?: string
    success?: boolean
    timeRange?: [string, string]
}>({})

// 清理表单
const cleanForm = reactive({
    keepDays: 30
})

// 分页配置
const pagination = reactive({
    current: 1,
    pageSize: 20,
    total: 0,
    showTotal: true,
    showPageSize: true,
    pageSizeOptions: ['10', '20', '50', '100']
})

// 表格列配置
const columns = [
    {
        title: '用户',
        dataIndex: 'username',
        width: 100
    },
    {
        title: '操作类型',
        dataIndex: 'operationType',
        width: 100,
        slotName: 'operationType'
    },
    {
        title: '模块',
        dataIndex: 'module',
        width: 120,
        slotName: 'module'
    },
    {
        title: '操作描述',
        dataIndex: 'description',
        width: 200,
        ellipsis: true,
        tooltip: true
    },
    {
        title: '请求URL',
        dataIndex: 'requestUrl',
        width: 200,
        ellipsis: true,
        tooltip: true
    },
    {
        title: '请求方式',
        dataIndex: 'requestMethod',
        width: 80,
        slotName: 'requestMethod'
    },
    {
        title: '执行状态',
        dataIndex: 'success',
        width: 100,
        slotName: 'success'
    },
    {
        title: '耗时(ms)',
        dataIndex: 'duration',
        width: 100
    },
    {
        title: 'IP地址',
        dataIndex: 'ipAddress',
        width: 120
    },
    {
        title: '操作时间',
        dataIndex: 'operationTime',
        width: 160,
        slotName: 'operationTime'
    },
    {
        title: '操作',
        width: 120,
        fixed: 'right',
        slotName: 'actions'
    }
]

// 详情数据
const detailData = computed(() => {
    if (!currentLog.value) return []

    return [
        {
            label: '用户名',
            value: currentLog.value.username
        },
        {
            label: '操作类型',
            value: getOperationTypeText(currentLog.value.operationType)
        },
        {
            label: '模块',
            value: getModuleText(currentLog.value.module)
        },
        {
            label: '操作描述',
            value: currentLog.value.description
        },
        {
            label: '请求URL',
            value: currentLog.value.requestUrl
        },
        {
            label: '请求方式',
            value: currentLog.value.requestMethod
        },
        {
            label: '执行状态',
            value: currentLog.value.success ? '成功' : '失败'
        },
        {
            label: '耗时',
            value: `${currentLog.value.duration}ms`
        },
        {
            label: 'IP地址',
            value: currentLog.value.ipAddress
        },
        {
            label: '用户代理',
            value: currentLog.value.userAgent
        },
        {
            label: '操作时间',
            value: formatDateTime(currentLog.value.operationTime)
        }
    ]
})

// 加载数据
const loadData = async () => {
    try {
        loading.value = true

        if (userStore.isAdmin) {
            // 管理员查看所有日志
            const params: OperationLogQuery = {
                current: pagination.current,
                size: pagination.pageSize,
                ...queryForm
            }

            if (queryForm.timeRange) {
                params.startTime = queryForm.timeRange[0]
                params.endTime = queryForm.timeRange[1]
            }

            const response = await getOperationLogs(params)
            logData.value = response.data.records || []
            pagination.total = response.data.total || 0
        } else {
            // 普通用户只查看自己的日志
            const response = await getMyOperationLogs(50)
            logData.value = response.data || []
            pagination.total = response.data?.length || 0
        }
    } catch (error) {
        console.error('加载操作日志失败:', error)
        Message.error('加载操作日志失败')
    } finally {
        loading.value = false
    }
}

// 加载统计数据
const loadStats = async () => {
    if (!userStore.isAdmin) return

    try {
        statsLoading.value = true
        const params: any = {}
        if (queryForm.timeRange) {
            params.startTime = queryForm.timeRange[0]
            params.endTime = queryForm.timeRange[1]
        }

        const response = await getOperationStats(params)
        stats.value = response.data
    } catch (error) {
        console.error('加载统计数据失败:', error)
        Message.error('加载统计数据失败')
    } finally {
        statsLoading.value = false
    }
}

// 处理统计按钮点击
const handleStatsClick = async () => {
    if (showStats.value) {
        // 如果当前显示统计，则隐藏
        showStats.value = false
    } else {
        // 如果当前隐藏统计，则显示并加载数据
        showStats.value = true
        await loadStats()
    }
}

// 搜索
const handleSearch = () => {
    pagination.current = 1
    loadData()

    // 如果统计信息正在显示，也更新统计数据
    if (showStats.value) {
        loadStats()
    }
}

// 重置
const handleReset = () => {
    Object.keys(queryForm).forEach(key => {
        delete (queryForm as any)[key]
    })
    pagination.current = 1
    loadData()

    // 如果统计信息正在显示，也更新统计数据
    if (showStats.value) {
        loadStats()
    }
}

// 分页处理
const handlePageChange = (page: number) => {
    pagination.current = page
    loadData()
}

const handlePageSizeChange = (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.current = 1
    loadData()
}

// 显示详情
const showDetail = (log: OperationLog) => {
    currentLog.value = log
    detailVisible.value = true
}

// 显示清理对话框
const showCleanDialog = () => {
    cleanVisible.value = true
}

// 清理日志
const handleCleanLogs = async () => {
    try {
        const response = await cleanExpiredLogs(cleanForm.keepDays)
        Message.success(response.data)
        cleanVisible.value = false
        loadData()
    } catch (error) {
        console.error('清理日志失败:', error)
        Message.error('清理日志失败')
    }
}

// 工具函数
const getOperationTypeText = (type: string) => {
    const map: Record<string, string> = {
        'CREATE': '新增',
        'UPDATE': '修改',
        'DELETE': '删除',
        'QUERY': '查询',
        'LOGIN': '登录',
        'LOGOUT': '退出',
        'EXPORT': '导出',
        'IMPORT': '导入'
    }
    return map[type] || type
}

const getOperationTypeColor = (type: string) => {
    const map: Record<string, string> = {
        'CREATE': 'green',
        'UPDATE': 'orange',
        'DELETE': 'red',
        'QUERY': 'blue',
        'LOGIN': 'cyan',
        'LOGOUT': 'gray',
        'EXPORT': 'purple',
        'IMPORT': 'magenta'
    }
    return map[type] || 'blue'
}

const getModuleText = (module: string) => {
    const map: Record<string, string> = {
        'USER': '用户管理',
        'PROJECT': '项目管理',
        'TODO': '待办任务',
        'REPORT': '报告管理',
        'EMAIL_RULE': '邮件规则',
        'EMAIL_TEMPLATE': '邮件模板',
        'AUTH': '认证授权',
        'SYSTEM': '系统管理'
    }
    return map[module] || module
}

const getMethodColor = (method: string) => {
    const map: Record<string, string> = {
        'GET': 'blue',
        'POST': 'green',
        'PUT': 'orange',
        'DELETE': 'red',
        'PATCH': 'purple'
    }
    return map[method] || 'gray'
}

const formatDateTime = (dateTime: string) => {
    if (!dateTime) return ''
    return new Date(dateTime).toLocaleString('zh-CN')
}

const formatJson = (jsonStr?: string) => {
    if (!jsonStr) return '无'
    try {
        const obj = JSON.parse(jsonStr)
        return JSON.stringify(obj, null, 2)
    } catch {
        return jsonStr
    }
}

// 生命周期
onMounted(() => {
    loadData()
    if (userStore.isAdmin) {
        loadStats()
    }
})
</script>

<style scoped>
.operation-logs-container {
    padding: 20px;
}

.filter-section {
    margin-bottom: 20px;
    background: var(--card-bg-color);
    padding: 16px;
    border-radius: 6px;
    border: 1px solid var(--border-color);
}

.stats-section {
    margin-bottom: 20px;
}

.table-section {
    background: var(--card-bg-color);
    border-radius: 6px;
    box-shadow: var(--card-shadow);
    border: 1px solid var(--border-color);
}

.detail-content {
    max-height: 600px;
    overflow-y: auto;
}

.json-viewer {
    background: #f6f8fa;
    border: 1px solid #e1e4e8;
    border-radius: 6px;
    padding: 12px;
    font-size: 12px;
    line-height: 1.5;
    overflow-x: auto;
    max-height: 200px;
    white-space: pre-wrap;
    word-break: break-all;
}

/* 优化表单布局 */
.arco-form-item {
    margin-bottom: 16px;
}

.arco-form-item-label {
    font-weight: 500;
    color: #1d2129;
}

/* 统计卡片样式 */
.arco-statistic-title {
    font-size: 14px;
    color: #86909c;
    margin-bottom: 8px;
}

.arco-statistic-content {
    font-size: 24px;
    font-weight: 600;
    line-height: 32px;
}
</style>