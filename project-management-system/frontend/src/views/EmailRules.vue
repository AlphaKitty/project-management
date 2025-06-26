<template>
    <div class="email-rules-container">
        <!-- 页面头部 -->
        <div class="page-header">
            <a-row justify="space-between" align="center">
                <a-col>
                    <h2>邮件规则管理</h2>
                    <p>配置自动邮件发送规则，提升团队协作效率</p>
                </a-col>
                <a-col>
                    <a-space style="margin-top: 10px;">
                        <a-button @click="showPreferencesModal = true">
                            <template #icon>
                                <icon-settings />
                            </template>
                            个人偏好设置
                        </a-button>
                        <a-button type="primary" @click="showCreateModal = true">
                            <template #icon>
                                <icon-plus />
                            </template>
                            新建规则
                        </a-button>
                    </a-space>
                </a-col>
            </a-row>
        </div>

        <!-- 筛选条件 -->
        <div class="filter-section">
            <a-card>
                <a-row :gutter="16">
                    <a-col :span="6">
                        <a-select v-model="queryParams.ruleType" placeholder="规则类型" allow-clear @change="handleSearch">
                            <a-option value="">全部类型</a-option>
                            <a-option value="DEADLINE">截止日期</a-option>
                            <a-option value="OVERDUE">逾期任务</a-option>
                            <a-option value="STATUS_CHANGE">状态变更</a-option>
                            <a-option value="SUMMARY">汇总报告</a-option>
                            <a-option value="INACTIVE">长期未更新</a-option>
                        </a-select>
                    </a-col>
                    <a-col :span="6">
                        <a-select v-model="queryParams.enabled" placeholder="启用状态" allow-clear @change="handleSearch">
                            <a-option :value="undefined">全部状态</a-option>
                            <a-option :value="true">已启用</a-option>
                            <a-option :value="false">已禁用</a-option>
                        </a-select>
                    </a-col>
                    <a-col :span="6">
                        <a-button type="primary" @click="handleSearch">
                            <template #icon>
                                <icon-search />
                            </template>
                            搜索
                        </a-button>
                    </a-col>
                </a-row>
            </a-card>
        </div>

        <!-- 规则列表 -->
        <div class="rules-list">
            <a-card>
                <a-table :data="rulesList" :loading="loading" :pagination="pagination" @page-change="handlePageChange"
                    @page-size-change="handlePageSizeChange">
                    <template #columns>
                        <a-table-column title="规则名称" data-index="ruleName" :width="180">
                            <template #cell="{ record }">
                                <div class="rule-name">
                                    <span class="name">{{ record.ruleName }}</span>
                                </div>
                            </template>
                        </a-table-column>
                        <a-table-column title="规则类型" data-index="ruleType" :width="80" align="center">
                            <template #cell="{ record }">
                                <div class="rule-name">
                                    <a-tag :color="getRuleTypeColor(record.ruleType)" size="small"
                                        style="margin-left: 8px">
                                        {{ getRuleTypeText(record.ruleType) }}
                                    </a-tag>
                                </div>
                            </template>
                        </a-table-column>

                        <a-table-column title="描述" data-index="description" :width="230">
                            <template #cell="{ record }">
                                <div class="description">
                                    {{ record.description || '暂无描述' }}
                                </div>
                            </template>
                        </a-table-column>

                        <a-table-column title="优先级" data-index="priority" :width="80" align="center">
                            <template #cell="{ record }">
                                <a-tag :color="getPriorityColor(record.priority)">
                                    {{ record.priority }}
                                </a-tag>
                            </template>
                        </a-table-column>

                        <a-table-column title="频率限制" data-index="maxFrequency" :width="90" align="center">
                            <template #cell="{ record }">
                                <span class="frequency">{{ record.maxFrequency || '无限制' }}</span>
                            </template>
                        </a-table-column>

                        <a-table-column title="状态" data-index="enabled" :width="60" align="center">
                            <template #cell="{ record }">
                                <a-switch v-model="record.enabled"
                                    @change="(value: string | number | boolean) => handleToggleRule(record.id, Boolean(value))"
                                    :loading="record.switching" />
                            </template>
                        </a-table-column>

                        <a-table-column title="创建时间" data-index="createTime" :width="160" align="center">
                            <template #cell="{ record }">
                                {{ formatDateTime(record.createTime) }}
                            </template>
                        </a-table-column>

                        <a-table-column title="操作" :width="200" align="center">
                            <template #cell="{ record }">
                                <a-space>
                                    <a-button type="text" size="mini" @click="handleEdit(record)">
                                        <template #icon>
                                            <icon-edit />
                                        </template>
                                        编辑
                                    </a-button>
                                    <!-- <a-button type="text" size="mini" status="success" @click="handleTest(record)">
                                        <template #icon>
                                            <icon-play-arrow />
                                        </template>
                                        测试
                                    </a-button> -->
                                    <a-popconfirm content="确定要删除这条规则吗？" @ok="handleDelete(record)">
                                        <a-button type="text" size="mini" status="danger">
                                            <template #icon>
                                                <icon-delete />
                                            </template>
                                            删除
                                        </a-button>
                                    </a-popconfirm>
                                </a-space>
                            </template>
                        </a-table-column>
                    </template>
                </a-table>
            </a-card>
        </div>

        <!-- 创建/编辑规则弹窗 -->
        <a-modal v-model:visible="showCreateModal" :title="editingRule ? '编辑规则' : '新建规则'" width="800px"
            @ok="handleSaveRule" @cancel="handleCancelEdit">
            <a-form :model="ruleForm" layout="vertical">
                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item label="规则名称" required>
                            <a-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="规则类型" required>
                            <a-select v-model="ruleForm.ruleType" placeholder="请选择规则类型">
                                <a-option value="DEADLINE">截止日期提醒</a-option>
                                <a-option value="OVERDUE">逾期任务提醒</a-option>
                                <a-option value="STATUS_CHANGE">状态变更通知</a-option>
                                <a-option value="SUMMARY">汇总报告</a-option>
                                <a-option value="INACTIVE">长期未更新提醒</a-option>
                            </a-select>
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item label="邮件模板">
                            <a-select v-model="ruleForm.emailTemplateCode" placeholder="请选择邮件模板">
                                <a-option v-for="template in emailTemplates" :key="template.code"
                                    :value="template.code">
                                    {{ template.name }}
                                </a-option>
                            </a-select>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="优先级">
                            <a-input-number v-model="ruleForm.priority" :min="0" :max="10"
                                placeholder="0-10，数字越大优先级越高" />
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item label="最大发送频率">
                            <a-input v-model="ruleForm.maxFrequency" placeholder="如：1/hour, 1/day" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="目标角色">
                            <a-input v-model="ruleForm.targetRoles" placeholder="如：ADMIN,USER" />
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item>
                            <a-checkbox v-model="ruleForm.businessHoursOnly">
                                仅工作时间发送
                            </a-checkbox>
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item>
                            <a-checkbox v-model="ruleForm.excludeWeekends">
                                排除周末
                            </a-checkbox>
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-form-item label="触发条件配置">
                    <a-textarea v-model="ruleForm.triggerCondition" placeholder="请输入JSON格式的触发条件配置" :rows="4" />
                </a-form-item>

                <a-form-item label="规则描述">
                    <a-textarea v-model="ruleForm.description" placeholder="请输入规则描述" :rows="3" />
                </a-form-item>
            </a-form>
        </a-modal>

        <!-- 个人偏好设置弹窗 -->
        <a-modal v-model:visible="showPreferencesModal" title="个人邮件偏好设置" width="600px" @ok="handleSavePreferences"
            @cancel="showPreferencesModal = false">
            <a-form :model="preferencesForm" layout="vertical">
                <a-form-item label="基础设置">
                    <a-space direction="vertical" style="width: 100%">
                        <a-checkbox v-model="preferencesForm.enableEmail">
                            启用邮件通知
                        </a-checkbox>
                        <a-checkbox v-model="preferencesForm.urgentOnly">
                            仅接收紧急任务通知
                        </a-checkbox>
                    </a-space>
                </a-form-item>

                <a-form-item label="通知类型">
                    <a-space direction="vertical" style="width: 100%">
                        <a-checkbox v-model="preferencesForm.deadlineReminder">
                            截止日期提醒
                        </a-checkbox>
                        <a-checkbox v-model="preferencesForm.statusChange">
                            状态变更通知
                        </a-checkbox>
                        <a-checkbox v-model="preferencesForm.taskAssignment">
                            任务分配通知
                        </a-checkbox>
                        <a-checkbox v-model="preferencesForm.overdueTask">
                            逾期任务提醒
                        </a-checkbox>
                    </a-space>
                </a-form-item>

                <a-form-item label="汇总报告">
                    <a-space direction="vertical" style="width: 100%">
                        <a-checkbox v-model="preferencesForm.dailySummary">
                            每日汇总
                        </a-checkbox>
                        <a-checkbox v-model="preferencesForm.weeklySummary">
                            每周汇总
                        </a-checkbox>
                        <a-checkbox v-model="preferencesForm.monthlySummary">
                            每月汇总
                        </a-checkbox>
                    </a-space>
                </a-form-item>

                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item label="每日汇总时间">
                            <a-time-picker v-model="preferencesForm.dailySummaryTime" format="HH:mm"
                                placeholder="选择时间" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="每日最大邮件数">
                            <a-input-number v-model="preferencesForm.maxEmailsPerDay" :min="1" :max="50"
                                placeholder="1-50" />
                        </a-form-item>
                    </a-col>
                </a-row>

                <a-row :gutter="16">
                    <a-col :span="12">
                        <a-form-item label="免打扰开始时间">
                            <a-time-picker v-model="preferencesForm.quietStart" format="HH:mm" placeholder="选择时间" />
                        </a-form-item>
                    </a-col>
                    <a-col :span="12">
                        <a-form-item label="免打扰结束时间">
                            <a-time-picker v-model="preferencesForm.quietEnd" format="HH:mm" placeholder="选择时间" />
                        </a-form-item>
                    </a-col>
                </a-row>
            </a-form>
        </a-modal>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
    IconPlus,
    IconSearch,
    IconEdit,
    IconDelete,
    IconSettings,
    IconPlayArrow
} from '@arco-design/web-vue/es/icon'
import { emailRuleApi, type EmailRule, type UserEmailPreference, type EmailRuleQuery } from '@/api/emailRules'

// 扩展EmailRule类型，添加switching属性
interface EmailRuleWithSwitching extends EmailRule {
    switching?: boolean
}

// 响应式数据
const loading = ref(false)
const rulesList = ref<EmailRuleWithSwitching[]>([])
const showCreateModal = ref(false)
const showPreferencesModal = ref(false)
const editingRule = ref<EmailRule | null>(null)
const emailTemplates = ref<{ code: string; name: string }[]>([])

// 查询参数
const queryParams = reactive<EmailRuleQuery>({
    current: 1,
    size: 10,
    ruleType: '',
    enabled: undefined
})

// 分页配置
const pagination = computed(() => ({
    current: queryParams.current || 1,
    pageSize: queryParams.size || 10,
    total: totalCount.value,
    showTotal: true,
    showPageSize: true
}))

const totalCount = ref(0)

// 规则表单
const ruleForm = reactive<Partial<EmailRule>>({
    ruleName: '',
    ruleType: '',
    triggerCondition: '',
    emailTemplateCode: '',
    enabled: true,
    priority: 5,
    maxFrequency: '',
    businessHoursOnly: true,
    excludeWeekends: true,
    targetRoles: '',
    description: ''
})

// 偏好设置表单
const preferencesForm = reactive<Partial<UserEmailPreference>>({
    enableEmail: true,
    deadlineReminder: true,
    statusChange: true,
    taskAssignment: true,
    overdueTask: true,
    dailySummary: false,
    weeklySummary: false,
    monthlySummary: false,
    dailySummaryTime: '08:30',
    weeklyDay: 1,
    monthlyDay: 1,
    urgentOnly: false,
    quietStart: '22:00',
    quietEnd: '08:00',
    maxEmailsPerDay: 10
})

// 获取规则列表
const fetchRules = async () => {
    try {
        loading.value = true
        const response = await emailRuleApi.getEmailRules(queryParams)
        rulesList.value = response.data.records
        totalCount.value = response.data.total
    } catch (error) {
        console.error('获取规则列表失败:', error)
        Message.error('获取规则列表失败')
    } finally {
        loading.value = false
    }
}

// 获取邮件模板列表
const fetchEmailTemplates = async () => {
    try {
        const response = await emailRuleApi.getEmailTemplates()
        emailTemplates.value = response.data.map(item => {
            const [code, name] = item.split(':')
            return { code, name }
        })
    } catch (error) {
        console.error('获取邮件模板失败:', error)
    }
}

// 获取用户偏好设置
const fetchUserPreferences = async () => {
    try {
        const response = await emailRuleApi.getUserEmailPreference()
        Object.assign(preferencesForm, response.data)
    } catch (error) {
        console.error('获取用户偏好设置失败:', error)
    }
}

// 搜索
const handleSearch = () => {
    queryParams.current = 1
    fetchRules()
}

// 分页变化
const handlePageChange = (page: number) => {
    queryParams.current = page
    fetchRules()
}

const handlePageSizeChange = (size: number) => {
    queryParams.size = size
    queryParams.current = 1
    fetchRules()
}

// 切换规则状态
const handleToggleRule = async (id: number, enabled: boolean) => {
    const rule = rulesList.value.find(r => r.id === id)
    if (!rule) return

    try {
        rule.switching = true
        await emailRuleApi.toggleEmailRule(id, enabled)
        Message.success(enabled ? '规则已启用' : '规则已禁用')
    } catch (error) {
        console.error('切换规则状态失败:', error)
        Message.error('操作失败')
        rule.enabled = !enabled // 回滚状态
    } finally {
        rule.switching = false
    }
}

// 编辑规则
const handleEdit = (rule: EmailRule) => {
    editingRule.value = rule
    Object.assign(ruleForm, rule)
    showCreateModal.value = true
}

// 删除规则
const handleDelete = async (rule: EmailRule) => {
    try {
        await emailRuleApi.deleteEmailRule(rule.id!)
        Message.success('删除成功')
        fetchRules()
    } catch (error) {
        console.error('删除规则失败:', error)
        Message.error('删除失败')
    }
}

// 测试规则
const handleTest = async (rule: EmailRule) => {
    try {
        const response = await emailRuleApi.testEmailRule(rule.id!)
        Message.success(response.data || '测试成功')
    } catch (error) {
        console.error('测试规则失败:', error)
        Message.error('测试失败')
    }
}

// 保存规则
const handleSaveRule = async () => {
    try {
        if (editingRule.value) {
            await emailRuleApi.updateEmailRule(editingRule.value.id!, ruleForm as EmailRule)
            Message.success('更新成功')
        } else {
            await emailRuleApi.createEmailRule(ruleForm as EmailRule)
            Message.success('创建成功')
        }
        showCreateModal.value = false
        fetchRules()
    } catch (error) {
        console.error('保存规则失败:', error)
        Message.error('保存失败')
    }
}

// 取消编辑
const handleCancelEdit = () => {
    editingRule.value = null
    // 重置表单
    ruleForm.ruleName = ''
    ruleForm.ruleType = ''
    ruleForm.triggerCondition = ''
    ruleForm.emailTemplateCode = ''
    ruleForm.enabled = true
    ruleForm.priority = 5
    ruleForm.maxFrequency = ''
    ruleForm.businessHoursOnly = true
    ruleForm.excludeWeekends = true
    ruleForm.targetRoles = ''
    ruleForm.description = ''
}

// 保存偏好设置
const handleSavePreferences = async () => {
    try {
        await emailRuleApi.updateUserEmailPreference(preferencesForm as UserEmailPreference)
        Message.success('偏好设置保存成功')
        showPreferencesModal.value = false
    } catch (error) {
        console.error('保存偏好设置失败:', error)
        Message.error('保存失败')
    }
}

// 工具函数
const getRuleTypeColor = (type: string) => {
    const colors: Record<string, string> = {
        DEADLINE: 'red',
        STATUS_CHANGE: 'blue',
        SUMMARY: 'green',
        INACTIVE: 'orange',
        OVERDUE: 'purple'
    }
    return colors[type] || 'gray'
}

const getRuleTypeText = (type: string) => {
    const texts: Record<string, string> = {
        DEADLINE: '截止日期',
        STATUS_CHANGE: '状态变更',
        OVERDUE: '逾期任务',
        SUMMARY: '汇总报告',
        INACTIVE: '长期未更新'
    }
    return texts[type] || type
}

const getPriorityColor = (priority: number) => {
    if (priority >= 8) return 'red'
    if (priority >= 5) return 'orange'
    return 'green'
}

const formatDateTime = (dateTime: string) => {
    if (!dateTime) return '-'
    return new Date(dateTime).toLocaleString('zh-CN')
}

// 监听头部创建按钮点击事件
onMounted(() => {
    fetchRules()
    fetchEmailTemplates()
    fetchUserPreferences()

    // 监听全局创建事件
    const handleHeaderCreate = (event: CustomEvent) => {
        if (event.detail?.type === 'email-rules') {
            showCreateModal.value = true
        }
    }

    window.addEventListener('header-create-click', handleHeaderCreate as EventListener)

    // 组件卸载时移除监听器
    return () => {
        window.removeEventListener('header-create-click', handleHeaderCreate as EventListener)
    }
})
</script>

<style scoped>
.email-rules-container {
    padding: 0;
}

.page-header {
    margin-bottom: 24px;
}

.page-header h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
    color: var(--text-color);
}

.page-header p {
    margin: 0;
    color: var(--text-muted);
    font-size: 14px;
}

.filter-section {
    margin-bottom: 24px;
    background: var(--card-bg-color);
    padding: 16px;
    border-radius: 6px;
    border: 1px solid var(--border-color);
}

.rules-list {
    background: var(--card-bg-color);
    border-radius: 8px;
    border: 1px solid var(--border-color);
}

.rule-name {
    display: flex;
    align-items: center;
}

.rule-name .name {
    font-weight: 500;
    color: var(--text-color);
}

.description {
    color: var(--text-muted);
    font-size: 13px;
    line-height: 1.4;
}

.frequency {
    font-size: 12px;
    color: var(--text-muted);
}

:deep(.arco-table-th) {
    background-color: #f7f8fa;
    font-weight: 600;
}

:deep(.arco-card-body) {
    padding: 20px;
}

:deep(.arco-form-item-label) {
    font-weight: 500;
}
</style>