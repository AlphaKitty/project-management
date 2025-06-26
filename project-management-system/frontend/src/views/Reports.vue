<template>
  <div class="reports-page">
    <!-- 报告统计卡片 -->
    <div class="stats-cards">
      <a-card class="stat-card">
        <a-statistic title="总报告数" :value="reportStore.reportCount" />
      </a-card>
      <a-card class="stat-card">
        <a-statistic title="周报" :value="weeklyReports.length" />
      </a-card>
      <a-card class="stat-card">
        <a-statistic title="双周报" :value="biweeklyReports.length" />
      </a-card>
      <a-card class="stat-card">
        <a-statistic title="月报" :value="monthlyReports.length" />
      </a-card>
    </div>

    <!-- 筛选条件 -->
    <div class="filter-section">
      <a-row :gutter="16" align="center">
        <a-col :span="6">
          <a-select v-model="filterType" placeholder="报告类型" allow-clear>
            <a-option value="">全部类型</a-option>
            <a-option value="WEEKLY">周报</a-option>
            <a-option value="BIWEEKLY">双周报</a-option>
            <a-option value="MONTHLY">月报</a-option>
            <a-option value="STAGE">阶段报告</a-option>
          </a-select>
        </a-col>
        <a-col :span="6">
          <a-select v-model="filterProject" placeholder="所属项目" allow-clear>
            <a-option value="">全部项目</a-option>
            <a-option v-for="project in projectStore.projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </a-option>
          </a-select>
        </a-col>
      </a-row>
    </div>

    <!-- 报告列表 -->
    <a-card title="报告列表" class="report-list-card">
      <a-table :columns="columns" :data="filteredReports" :loading="reportStore.loading" :pagination="{ pageSize: 10 }">
        <template #type="{ record }">
          <a-tag :color="getTypeColor(record.type)">
            {{ getTypeLabel(record.type) }}
          </a-tag>
        </template>

        <template #project="{ record }">
          {{ record.project?.name || '无' }}
        </template>

        <template #creator="{ record }">
          {{ record.creator?.nickname || '未知' }}
        </template>

        <template #actions="{ record }">
          <a-button-group size="small">
            <a-button @click="viewReport(record)">查看</a-button>
            <a-button status="danger" @click="deleteReport(record)">删除</a-button>
          </a-button-group>
        </template>
      </a-table>
    </a-card>

    <!-- 生成报告模态框 -->
    <a-modal v-model:visible="generateModalVisible" title="生成项目报告" @ok="handleGenerate"
      @cancel="generateModalVisible = false">
      <a-form :model="generateForm" layout="vertical">
        <a-form-item label="报告标题" required>
          <a-input v-model="generateForm.title" placeholder="请输入报告标题" />
        </a-form-item>

        <a-form-item label="报告类型" required>
          <a-select v-model="generateForm.type" placeholder="请选择报告类型">
            <a-option value="WEEKLY">周报</a-option>
            <a-option value="BIWEEKLY">双周报</a-option>
            <a-option value="MONTHLY">月报</a-option>
            <a-option value="STAGE">阶段报告</a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="所属项目" required>
          <a-select v-model="generateForm.projectIds" placeholder="请选择项目（可多选）" multiple @change="handleProjectChange">
            <a-option value="selectAll" @click="toggleSelectAll">
              {{ isAllSelected ? '取消全选' : '全选' }}
            </a-option>
            <a-option v-for="project in projectStore.projects" :key="project.id" :value="project.id">
              {{ project.name }}
            </a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="报告日期" required>
          <a-date-picker v-model="generateForm.reportDate" style="width: 100%" />
        </a-form-item>

        <a-form-item label="报告模式">
          <a-switch v-model="generateForm.fuzzyMode" checked-text="模糊" unchecked-text="严谨" :default-checked="true">
            <template #checked-icon>
              <icon-eye-invisible />
            </template>
            <template #unchecked-icon>
              <icon-eye />
            </template>
          </a-switch>
          <div style="color: #86909c; font-size: 12px; margin-top: 4px;">
            模糊模式：已完成任务不显示按时/逾期状态，项目待办数大于3个时只显示时间范围内的任务
          </div>
        </a-form-item>

        <a-form-item label="报告内容">
          <a-textarea v-model="generateForm.content" placeholder="留空将根据待办任务自动生成报告内容" :rows="6" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 报告详情模态框 -->
    <a-modal v-model:visible="detailModalVisible" :title="currentReport?.title" :footer="false" :width="800">
      <div v-if="currentReport" class="report-detail">
        <div class="report-meta">
          <a-descriptions :column="2" bordered>
            <a-descriptions-item label="报告类型">
              <a-tag :color="getTypeColor(currentReport.type)">
                {{ getTypeLabel(currentReport.type) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="所属项目">
              {{ currentReport.project?.name || '无' }}
            </a-descriptions-item>
            <a-descriptions-item label="创建人">
              {{ currentReport.creator?.nickname || '未知' }}
            </a-descriptions-item>
            <a-descriptions-item label="报告日期">
              {{ currentReport.reportDate }}
            </a-descriptions-item>
            <a-descriptions-item label="创建时间">
              {{ currentReport.createTime }}
            </a-descriptions-item>
            <a-descriptions-item label="更新时间">
              {{ currentReport.updateTime }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <div class="report-content">
          <h3>报告内容</h3>
          <div style="display: flex; justify-content: flex-end; gap: 8px; margin-bottom: 8px;">
            <a-button type="primary" size="small" @click="copyReportRaw">复制原码</a-button>
            <a-button type="primary" size="small" @click="copyReportContent">复制Markdown</a-button>
          </div>
          <div class="content-preview" ref="reportContent" v-html="formatContent(currentReport.content)"></div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import {
  IconEye,
  IconEyeInvisible
} from '@arco-design/web-vue/es/icon'
import { useReportStore } from '@/stores/reports'
import { useProjectStore } from '@/stores/projects'
import type { Report, ReportDTO } from '@/types'
import { marked } from 'marked'

// Store
const reportStore = useReportStore()
const projectStore = useProjectStore()

// 响应式数据
const generateModalVisible = ref(false)
const detailModalVisible = ref(false)
const filterType = ref('')
const filterProject = ref('')
const currentReport = ref<Report | null>(null)
const reportContent = ref<HTMLElement | null>(null)

const generateForm = ref<ReportDTO>({
  title: '',
  type: 'WEEKLY',
  projectIds: [],
  reportDate: '',
  content: '',
  creatorId: 1
})

// 表格列配置
const columns = [
  { title: '报告标题', dataIndex: 'title', key: 'title', align: 'center' },
  { title: '类型', dataIndex: 'type', key: 'type', slotName: 'type', align: 'center' },
  { title: '所属项目', dataIndex: 'project', key: 'project', slotName: 'project', align: 'center' },
  { title: '创建人', dataIndex: 'creator', key: 'creator', slotName: 'creator', align: 'center' },
  { title: '报告日期', dataIndex: 'reportDate', key: 'reportDate', align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', align: 'center' },
  { title: '操作', key: 'actions', slotName: 'actions', width: 200, align: 'center' }
]

// 按类型分类的报告
const weeklyReports = computed(() =>
  reportStore.reports.filter(report => report.type === 'WEEKLY')
)

const biweeklyReports = computed(() =>
  reportStore.reports.filter(report => report.type === 'BIWEEKLY')
)

const monthlyReports = computed(() =>
  reportStore.reports.filter(report => report.type === 'MONTHLY')
)

const stageReports = computed(() =>
  reportStore.reports.filter(report => report.type === 'STAGE')
)

// 筛选后的报告列表
const filteredReports = computed(() => {
  let reports = reportStore.reports

  if (filterType.value) {
    reports = reports.filter(report => report.type === filterType.value)
  }

  if (filterProject.value) {
    reports = reports.filter(report => report.projectId === Number(filterProject.value))
  }

  return reports
})

// 获取类型标签
const getTypeLabel = (type: string) => {
  const labels: Record<string, string> = {
    'WEEKLY': '周报',
    'BIWEEKLY': '双周报',
    'MONTHLY': '月报',
    'STAGE': '阶段报告'
  }
  return labels[type] || type
}

// 获取类型颜色
const getTypeColor = (type: string) => {
  const colors: Record<string, string> = {
    'WEEKLY': 'blue',
    'BIWEEKLY': 'cyan',
    'MONTHLY': 'green',
    'STAGE': 'orange'
  }
  return colors[type] || 'gray'
}

// 格式化报告内容为markdown
const formatContent = (content: string | undefined) => {
  return content ? marked.parse(content) : ''
}

// 判断是否全选
const isAllSelected = computed(() => {
  const allProjectIds = projectStore.projects.map(p => p.id)
  const selectedIds = generateForm.value.projectIds || []
  return allProjectIds.length > 0 &&
    allProjectIds.every(id => selectedIds.includes(id))
})

// 全选/取消全选
const toggleSelectAll = () => {
  if (isAllSelected.value) {
    // 取消全选
    generateForm.value.projectIds = []
  } else {
    // 全选
    generateForm.value.projectIds = projectStore.projects.map(p => p.id)
  }
}

// 处理项目选择变化
const handleProjectChange = (value: (number | string)[]) => {
  // 过滤掉 selectAll 值，只保留实际的项目ID
  generateForm.value.projectIds = value.filter(v => typeof v === 'number') as number[]
}

// 显示生成模态框
const showGenerateModal = () => {
  generateForm.value = {
    title: '',
    type: 'WEEKLY',
    projectIds: [],
    reportDate: '',
    content: '',
    creatorId: 1,
    fuzzyMode: true
  }
  generateModalVisible.value = true
}

// 监听头部按钮点击事件
const handleHeaderCreate = (event: any) => {
  if (event.detail.type === 'reports') {
    showGenerateModal()
  }
}

// 查看报告详情
const viewReport = (report: Report) => {
  currentReport.value = report
  detailModalVisible.value = true
}

// 编辑报告
const editReport = (report: Report) => {
  Message.info(`编辑报告: ${report.title}`)
}

// 删除报告
const deleteReport = async (report: Report) => {
  try {
    await reportStore.deleteReport(report.id)
    Message.success('报告删除成功')
  } catch (error) {
    Message.error('报告删除失败')
  }
}

// 生成报告
const handleGenerate = async () => {
  try {
    await reportStore.generateReport(generateForm.value)
    Message.success('报告生成成功')
    generateModalVisible.value = false
  } catch (error) {
    Message.error('报告生成失败')
  }
}

function copyReportRaw() {
  if (currentReport.value && currentReport.value.content) {
    navigator.clipboard.writeText(currentReport.value.content).then(() => {
      Message.success('原始Markdown已复制到剪贴板')
    })
  }
}

function copyReportContent() {
  const el = reportContent.value
  if (el) {
    const text = el.innerText || el.textContent || ''
    navigator.clipboard.writeText(text).then(() => {
      Message.success('报告内容已复制到剪贴板')
    })
  }
}

// 页面加载时获取数据
onMounted(async () => {
  try {
    await Promise.all([
      reportStore.fetchReports(),
      projectStore.fetchProjects()
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
.reports-page {
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

.report-list-card {
  margin-top: 0;
}

.report-detail {
  padding: 16px 0;
}

.report-meta {
  margin-bottom: 24px;
}

.report-content h3 {
  margin: 0 0 16px 0;
  color: #262626;
  font-size: 16px;
  font-weight: 500;
}

.content-preview {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  line-height: 1.7;
  white-space: normal;
  font-size: 15px;
  color: #1d2129;
}

/* Markdown优化样式 */
.content-preview h1,
.content-preview h2,
.content-preview h3,
.content-preview h4 {
  font-weight: 700;
  margin: 18px 0 10px 0;
  color: #1765ad;
}

.content-preview h1 {
  font-size: 2em;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 6px;
}

.content-preview h2 {
  font-size: 1.5em;
  border-bottom: 1px solid #e9ecef;
  padding-bottom: 4px;
}

.content-preview h3 {
  font-size: 1.2em;
}

.content-preview h4 {
  font-size: 1em;
}

.content-preview ul,
.content-preview ol {
  margin: 10px 0 10px 24px;
  padding-left: 20px;
}

.content-preview li {
  margin: 6px 0;
  line-height: 1.7;
}

.content-preview blockquote {
  border-left: 4px solid #b5c7e3;
  background: #f4f8fb;
  color: #666;
  margin: 12px 0;
  padding: 8px 16px;
  border-radius: 4px;
}

.content-preview pre {
  background: #23272e;
  color: #fff;
  border-radius: 6px;
  padding: 12px;
  overflow-x: auto;
  font-size: 14px;
  margin: 12px 0;
}

.content-preview code {
  background: #f4f4f4;
  color: #c7254e;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 14px;
}

.content-preview table {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
}

.content-preview th,
.content-preview td {
  border: 1px solid #e9ecef;
  padding: 8px 12px;
  text-align: left;
}

.content-preview th {
  background: #f4f8fb;
  font-weight: 600;
}

.content-preview tr:nth-child(even) {
  background: #fafbfc;
}

.content-preview a {
  color: #1765ad;
  text-decoration: underline;
}
</style>