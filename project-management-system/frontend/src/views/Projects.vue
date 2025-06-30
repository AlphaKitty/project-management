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
      
      <a-button style="margin-left: 10px;" type="primary" @click="showGanttModal">
        <template #icon><icon-calendar /></template>
        甘特图
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
      width="900px" :esc-to-close="true">
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

    <!-- 新增待办模态框 -->
    <a-modal v-model:visible="todoModalVisible" title="新增待办事项" @ok="handleTodoSubmit" @cancel="handleTodoCancel" width="600px" :esc-to-close="true">
      <a-form :model="todoFormData" layout="vertical">
        <a-form-item label="待办标题" required>
          <a-input v-model="todoFormData.title" placeholder="请输入待办标题" />
        </a-form-item>

        <a-form-item label="待办描述">
          <a-textarea v-model="todoFormData.description" placeholder="请输入待办描述" :rows="3" />
        </a-form-item>

        <a-form-item label="任务状态">
          <a-select v-model="todoFormData.status" placeholder="请选择任务状态">
            <a-option value="TODO">待办</a-option>
            <a-option value="PROGRESS">进行中</a-option>
            <a-option value="DONE">已完成</a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="优先级">
          <a-select v-model="todoFormData.priority" placeholder="请选择优先级">
            <a-option value="LOW">低</a-option>
            <a-option value="MEDIUM">中</a-option>
            <a-option value="HIGH">高</a-option>
          </a-select>
        </a-form-item>

        <a-form-item label="截止日期">
          <a-date-picker v-model="todoFormData.dueDate" style="width: 100%" />
        </a-form-item>

        <a-form-item label="责任人">
          <a-select v-model="todoFormData.assigneeId" placeholder="请输入关键字搜索责任人" allow-search allow-clear
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
      </a-form>
    </a-modal>

    <!-- 项目概览模态框 -->
    <a-modal v-model:visible="overviewModalVisible" title="项目概览" width="1800px" :footer="false" :esc-to-close="true">
      <div class="overview-header">
        <div class="overview-controls">
          <div class="work-update-controls">
            <a-switch 
              v-model="weeklyModeEnabled" 
              :checked-text="'周模式'" 
              :unchecked-text="'双周模式'"
              class="work-mode-switch"
            />
            <span class="mode-description">
              {{ weeklyModeEnabled ? '更新7天内进度' : '更新14天内进度' }}
            </span>
          </div>
          <div class="action-buttons">
            <a-button type="primary" @click="updateWorkPlans" :loading="workUpdateLoading">
              <template #icon><icon-refresh /></template>
              工作更新
            </a-button>
            <a-button type="primary" @click="exportToExcel">
              <template #icon><icon-download /></template>
              导出Excel
            </a-button>
          </div>
        </div>
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
                  <div class="todo-title">{{ todo.title }}</div>
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
                <div class="todo-note-section">
                  <div v-if="!isEditingNote[todo.id]" class="todo-note-display">
                    <span class="note-label">备注:</span>
                    <span class="note-content">{{ todo.description || '无备注' }}</span>
                    <a-button size="mini" type="text" @click="editTodoNote(todo)" class="edit-note-btn">
                      <template #icon><icon-edit /></template>
                    </a-button>
                  </div>
                  <div v-else class="todo-note-edit">
                    <a-textarea 
                      v-model="editingNoteContent" 
                      placeholder="请输入备注内容..."
                      :rows="2"
                      :max-length="500"
                      show-word-limit
                      class="note-textarea"
                    />
                    <div class="note-edit-actions">
                      <a-button size="mini" type="primary" @click="saveTodoNote(todo)" :loading="noteSaving">
                        保存
                      </a-button>
                      <a-button size="mini" @click="cancelEditNote(todo.id)">
                        取消
                      </a-button>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="getUncompletedTodos(record).length === 0" class="no-todos-message">
                <span class="text-gray-400">正常进行中</span>
              </div>
            </div>
          </template>

          <template #createTime="{ record }">
            {{ formatDateTime(record.createTime) }}
          </template>

          <template #actions="{ record }">
            <a-button size="small" @click="editProjectWithMilestones(record)">编辑</a-button>
            <a-button size="small" type="primary" @click="showAddTodoModal(record)">新增待办</a-button>
          </template>
        </a-table>
      </div>
    </a-modal>



    <!-- 甘特图模态框 -->
    <a-modal v-model:visible="ganttModalVisible" title="全局项目甘特图" width="95%" :footer="false" :esc-to-close="true">
      <div class="gantt-container">
        <div class="gantt-actions">
          <div class="gantt-info">
            <span class="info-label">📊 当前用户相关的所有项目甘特图</span>
            <a-button @click="loadUserProjectsGantt" :loading="ganttLoading" size="small">
              <template #icon><icon-refresh /></template>
              刷新数据
            </a-button>
          </div>
        </div>
        
        <a-spin :loading="ganttLoading" style="width: 100%;">
          <div v-if="!ganttData.timeRange" class="empty-gantt">
            <icon-calendar style="font-size: 48px; color: #c0c4cc;" />
            <p>暂无项目数据</p>
            <p style="font-size: 12px; color: #999; margin-top: 8px;">
              调试信息: timeRange={{ !!ganttData.timeRange }}, 
              taskTracks={{ ganttData.taskTracks?.length || 0 }}个项目
            </p>
          </div>
          
          <div v-else class="gantt-chart">
            <!-- 项目信息头部 -->
            <div class="gantt-header">
              <div class="gantt-header-content">
                <div class="gantt-header-info">
                  <h3>全局项目甘特图</h3>
                  <div class="project-meta">
                    <span>项目数量: {{ ganttData.taskTracks?.length || 0 }}</span>
                    <span>时间范围: {{ ganttData.timeRange?.startDate }} ~ {{ ganttData.timeRange?.endDate }}</span>
                  </div>
                </div>
                
                <!-- 🔧 状态图例移到header行尾 -->
                <div class="gantt-legend-header">
                  <h4>状态图例</h4>
                  <div class="legend-items-inline">
                    <div class="legend-item">
                      <div class="legend-bar" style="background: #00b42a;"></div>
                      <span>提前完成</span>
                    </div>
                    <div class="legend-item">
                      <div class="legend-bar" style="background: #52c41a;"></div>
                      <span>按时完成</span>
                    </div>
                    <div class="legend-item">
                      <div class="legend-bar" style="background: #f5222d;"></div>
                      <span>延期完成</span>
                    </div>
                    <div class="legend-item">
                      <div class="legend-bar" style="background: #1890ff;"></div>
                      <span>正常进行</span>
                    </div>
                    <div class="legend-item">
                      <div class="legend-bar" style="background: #722ed1;"></div>
                      <span>即将到期</span>
                    </div>
                    <div class="legend-item">
                      <div class="legend-bar" style="background: #a8071a;"></div>
                      <span>逾期进行</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 🔧 重新设计甘特图布局 - 类似Excel的冻结行列 -->
            <div class="gantt-layout">
              <!-- 1. 顶部时间轴区域 -->
              <div class="gantt-header-area">
                <!-- 左上角固定区域 -->
                <div class="corner-cell">项目名称</div>
                <!-- 时间轴滚动区域 -->
                <div class="time-axis-scroll" ref="timeAxisRef">
                  <div class="time-scale">
                    <div 
                      v-for="(date, index) in generateTimeScale(ganttData.timeRange)" 
                      :key="index"
                      class="time-unit"
                      :class="{ 'current-time': isCurrentDate(date) }"
                    >
                      {{ formatDateLabel(date) }}
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 2. 主体内容区域 -->
              <div class="gantt-body-area" v-if="ganttData.taskTracks && ganttData.taskTracks.length > 0">
                <!-- 🔧 左侧固定项目名称列（真正冻结） -->
                <div class="track-labels-frozen" ref="labelsRef" @scroll="onLabelsScroll">
                  <div 
                    v-for="track in ganttData.taskTracks" 
                    :key="track.id"
                    class="track-label-cell"
                    :style="{ height: getTrackHeight(track) + 'px' }"
                  >
                    {{ track.name }}
                  </div>
                </div>
                
                <!-- 🔧 右侧可滚动内容区域 -->
                <div class="gantt-content-scroll" ref="contentRef" @scroll="onContentScroll">
                  <div class="tracks-container">
                      <div 
                        v-for="track in ganttData.taskTracks" 
                        :key="track.id"
                        class="project-track-content"
                        :style="{ height: getTrackHeight(track) + 'px' }"
                      >
                        <!-- 🔧 里程碑水平连线（节点首尾相连的时间轴） -->
                        <div class="milestone-timeline-horizontal">
                          <div 
                            v-for="(milestone, index) in track.milestones" 
                            :key="milestone.id"
                            class="milestone-connection"
                            :style="getMilestoneConnectionStyle(milestone, track.milestones[index + 1], ganttData.timeRange, index, track)"
                            :class="{ 'connection-completed': isMilestoneCompleted(milestone) }"
                          ></div>
                        </div>
                        
                        <!-- 项目里程碑 -->
                        <div 
                          v-for="milestone in track.milestones" 
                          :key="milestone.id"
                          class="milestone-marker"
                          :style="getMilestonePosition(milestone, ganttData.timeRange)"
                          :title="milestone.name + ' - ' + milestone.dueDate"
                        >
                          <div class="milestone-diamond" :style="{ backgroundColor: milestone.color }"></div>
                          <div class="milestone-label">{{ milestone.name }}</div>
                        </div>
                        
                        <!-- 项目任务 -->
                        <div 
                          v-for="(task, taskIndex) in track.tasks" 
                          :key="task.id"
                          class="task-bar"
                          :style="getTaskBarStyle(task, ganttData.timeRange, taskIndex)"
                          :title="getTaskTooltip(task)"
                        >
                          <div class="task-content">
                            <span class="task-title">{{ task.title }}</span>
                            <span v-if="task.progress < 100" class="task-progress">{{ task.progress }}%</span>
                          </div>
                          <div 
                            class="task-progress-fill" 
                            :style="{ width: task.progress + '%', backgroundColor: task.color }"
                          ></div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 无项目数据提示 -->
              <div v-if="ganttData.taskTracks && ganttData.taskTracks.length === 0" class="no-projects-hint" style="text-align: center; padding: 40px; color: #999;">
                <p>当前用户没有相关项目数据</p>
                <p style="font-size: 12px; margin-top: 8px;">
                  taskTracks: {{ ganttData.taskTracks?.length || 0 }}
                </p>
              </div>
          </div>
        </a-spin>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { Message, Modal } from '@arco-design/web-vue'
import { IconPlus, IconEye, IconDownload, IconRefresh, IconEdit, IconClockCircle } from '@arco-design/web-vue/es/icon'
import * as XLSX from 'xlsx'
import { useProjectStore } from '@/stores/projects'
import { useUserStore } from '@/stores/user'
import { useTodoStore } from '@/stores/todos'
import { projectApi } from '@/api/projects'
import { StatusLabels, StatusColors } from '@/types'
import type { Project, ProjectDTO, User, Milestone, TodoDTO } from '@/types'
import dayjs from 'dayjs'

// Store
const projectStore = useProjectStore()
const userStore = useUserStore()
const todoStore = useTodoStore()

// 响应式数据
const modalVisible = ref(false)
const overviewModalVisible = ref(false)
const todoModalVisible = ref(false)
const selectedProject = ref<number | undefined>()

// 甘特图相关数据
const ganttModalVisible = ref(false)
const ganttData = ref<any>({})
const ganttLoading = ref(false)
const isEdit = ref(false)
const workUpdateLoading = ref(false)
const currentProject = ref<Project | null>(null)
const weeklyModeEnabled = ref(true) // 默认开启周模式(7天)
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

// 待办表单数据
const todoFormData = ref<TodoDTO>({
  title: '',
  description: '',
  status: 'PROGRESS',
  priority: 'MEDIUM',
  dueDate: '',
  assigneeId: undefined,
  projectId: undefined,
  creatorId: userStore.currentUser?.id || 1
})

// 备注编辑相关数据
const isEditingNote = ref<Record<number, boolean>>({})
const editingNoteContent = ref('')
const noteSaving = ref(false)

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
  // { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 100, align: 'center' },
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
      await projectStore.fetchProjectOverview(true)
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
    await projectStore.fetchProjectOverview(true)
    overviewModalVisible.value = true
  } catch (error) {
    Message.error('获取项目概览失败')
  }
}



// 显示甘特图模态框
const showGanttModal = async () => {
  ganttData.value = {}
  ganttModalVisible.value = true
  // 自动加载用户全局甘特图数据
  await loadUserProjectsGantt()
}

// 加载项目甘特图数据
const loadProjectGantt = async (projectId: number) => {
  if (!projectId) return
  
  try {
    ganttLoading.value = true
    const response = await projectApi.getProjectGantt(projectId)
    ganttData.value = response.data || {}
  } catch (error) {
    console.error('获取项目甘特图数据失败:', error)
    Message.error('获取甘特图数据失败')
  } finally {
    ganttLoading.value = false
  }
}

// 加载用户全局甘特图数据
const loadUserProjectsGantt = async () => {
  try {
    ganttLoading.value = true
    const response = await projectApi.getUserProjectsGantt()
    console.log('🔍 甘特图接口返回数据:', response.data)
    ganttData.value = response.data || {}
    console.log('🔍 设置后的ganttData.value:', ganttData.value)
    console.log('🔍 timeRange存在:', !!ganttData.value.timeRange)
    console.log('🔍 taskTracks存在:', !!ganttData.value.taskTracks)
    console.log('🔍 taskTracks长度:', ganttData.value.taskTracks?.length || 0)
  } catch (error) {
    console.error('获取用户甘特图数据失败:', error)
    Message.error('获取甘特图数据失败')
  } finally {
    ganttLoading.value = false
  }
}



// 获取优先级文本
const getPriorityText = (priority: string) => {
  const priorityMap: Record<string, string> = {
    'HIGH': '高',
    'URGENT': '紧急',
    'MEDIUM': '中',
    'LOW': '低'
  }
  return priorityMap[priority] || priority
}

// 甘特图相关计算方法

// 生成时间刻度
const generateTimeScale = (timeRange: any) => {
  if (!timeRange) return []
  
  const startDate = dayjs(timeRange.startDate)
  const endDate = dayjs(timeRange.endDate)
  const dates = []
  
  let currentDate = startDate
  while (currentDate.isBefore(endDate) || currentDate.isSame(endDate)) {
    dates.push(currentDate.format('YYYY-MM-DD'))
    currentDate = currentDate.add(1, 'week') // 按周显示
  }
  
  return dates
}

// 判断是否为当前日期
const isCurrentDate = (date: string) => {
  return dayjs(date).isSame(dayjs(), 'week')
}

// 格式化日期标签
const formatDateLabel = (date: string) => {
  return dayjs(date).format('MM/DD')
}

// 获取里程碑位置
const getMilestonePosition = (milestone: any, timeRange: any) => {
  const startDate = dayjs(timeRange.startDate)
  const endDate = dayjs(timeRange.endDate)
  const milestoneDate = dayjs(milestone.dueDate)
  
  const totalDays = endDate.diff(startDate, 'day')
  const offsetDays = milestoneDate.diff(startDate, 'day')
  const leftPercent = (offsetDays / totalDays) * 100
  
  return {
    left: `${Math.max(0, Math.min(100, leftPercent))}%`,
    position: 'absolute' as const
  }
}

// 获取任务条样式（优化版 - 避免重叠）
const getTaskBarStyle = (task: any, timeRange: any, taskIndex: number = 0) => {
  const startDate = dayjs(timeRange.startDate)
  const endDate = dayjs(timeRange.endDate)
  const taskStartDate = dayjs(task.startDate)
  const taskEndDate = dayjs(task.endDate)
  
  const totalDays = endDate.diff(startDate, 'day')
  const taskStartOffset = taskStartDate.diff(startDate, 'day')
  const taskDuration = taskEndDate.diff(taskStartDate, 'day')
  
  const leftPercent = (taskStartOffset / totalDays) * 100
  const widthPercent = (taskDuration / totalDays) * 100
  
  // 🎯 关键优化：为每个任务分配不同的垂直位置，避免重叠
  const taskHeight = 22 // 任务条高度
  const taskSpacing = 4 // 任务间距
  const baseTopOffset = 50 // 🔧 里程碑区域基础高度，与getTrackHeight保持一致
  const topPosition = baseTopOffset + (taskIndex * (taskHeight + taskSpacing))
  
  return {
    left: `${Math.max(0, leftPercent)}%`,
    width: `${Math.max(2, widthPercent)}%`,
    top: `${topPosition}px`, // 🚀 每个任务占据不同的行
    backgroundColor: task.color + '20', // 半透明背景
    border: `2px solid ${task.color}`,
    position: 'absolute' as const,
    height: `${taskHeight}px`,
    borderRadius: '4px',
    overflow: 'hidden' as const,
    zIndex: 5 + taskIndex // 层级递增，避免覆盖
  }
}

// 获取任务提示信息
const getTaskTooltip = (task: any) => {
  let assigneeName = '未分配'
  if (task.assignee) {
    assigneeName = task.assignee.nickname || task.assignee.username || '未知'
  } else if (task.assigneeId) {
    assigneeName = `用户${task.assigneeId}`
  }
  
  // 构建提示信息
  let tooltip = `${task.title}\n状态: ${task.statusText}\n时间: ${task.startDate} ~ ${task.endDate}\n进度: ${task.progress}%\n处理人: ${assigneeName}`
  
  // 添加描述信息（如果存在）
  if (task.description && task.description.trim()) {
    tooltip += `\n描述: ${task.description.trim()}`
  }
  
  // 添加优先级信息（如果存在）
  if (task.priority) {
    const priorityMap: Record<string, string> = {
      'HIGH': '高',
      'URGENT': '紧急', 
      'MEDIUM': '中',
      'LOW': '低'
    }
    tooltip += `\n优先级: ${priorityMap[task.priority] || task.priority}`
  }
  
  return tooltip
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

    // 根据开关状态确定更新天数
    const days = weeklyModeEnabled.value ? 7 : 14

    // 调用后端API更新所有项目的工作计划
    const response = await fetch(`/api/projects/update-work-plans?days=${days}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    if (response.ok) {
      // 重新加载项目概览数据，强制刷新缓存
      await projectStore.fetchProjectOverview(true)
      const modeText = weeklyModeEnabled.value ? '周' : '双周'
      Message.success(`${modeText}工作计划更新成功`)
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
      { header: '项目名称', key: 'name', width: 22 },
      { header: '状态', key: 'status', width: 8 },
      { header: '进度', key: 'progress', width: 8 },
      { header: '责任人', key: 'assignee', width: 12 },
      { header: '里程碑', key: 'milestones', width: 30 },
      { header: '本周工作', key: 'thisWeek', width: 60 },
      { header: '下周计划', key: 'nextWeek', width: 60 },
      // { header: '待办事项', key: 'todos', width: 50 },
      { header: '风险', key: 'risk', width: 15 }
    ]

    // 设置表头样式
    const headerRow = worksheet.getRow(1)
    headerRow.font = {
      name: '微软雅黑',
      size: 12,
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
      horizontal: 'left'
    }
    headerRow.height = 30

    // 添加表头边框
    headerRow.eachCell((cell) => {
      cell.border = {
        top: { style: 'thin', color: { argb: 'FF000000' } },
        bottom: { style: 'thin', color: { argb: 'FF000000' } },
        left: { style: 'thin', color: { argb: 'FF000000' } },
        right: { style: 'thin', color: { argb: 'FF000000' } }
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
        vertical: 'middle',
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
          top: { style: 'thin', color: { argb: 'FF000000' } },
          bottom: { style: 'thin', color: { argb: 'FF000000' } },
          left: { style: 'thin', color: { argb: 'FF000000' } },
          right: { style: 'thin', color: { argb: 'FF000000' } }
        }
      })

      // 项目名称列居中
      const nameCell = row.getCell(1)
      nameCell.alignment = { vertical: 'middle', horizontal: 'left', wrapText: true }

      // 状态列特殊颜色和居中
      const statusCell = row.getCell(2)
      statusCell.alignment = { vertical: 'middle', horizontal: 'left' }
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
      progressCell.alignment = { vertical: 'middle', horizontal: 'left' }
      progressCell.font = { name: '微软雅黑', size: 10, bold: true, color: { argb: 'FF1890FF' } }

      // 责任人列居中
      const assigneeCell = row.getCell(4)
      assigneeCell.alignment = { vertical: 'middle', horizontal: 'center' }

      // 里程碑列居中
      const milestoneCell = row.getCell(5)
      milestoneCell.alignment = { vertical: 'middle', horizontal: 'left', wrapText: true }

      // 风险列居中
      const riskCell = row.getCell(9)
      riskCell.alignment = { vertical: 'middle', horizontal: 'left' }
    })

    // 冻结表头
    worksheet.views = [{ state: 'frozen', ySplit: 1 }]

    // 添加自动筛选
    // worksheet.autoFilter = 'A1:I1'

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

// 显示新增待办模态框
const showAddTodoModal = (project: Project) => {
  currentProject.value = project
  todoFormData.value = {
    title: '',
    description: '',
    status: 'PROGRESS',
    priority: 'MEDIUM',
    dueDate: '',
    assigneeId: undefined,
    projectId: project.id,
    creatorId: userStore.currentUser?.id || 1
  }
  
  // 重置搜索状态
  userSearchText.value = ''
  searchResultUsers.value = []
  
  todoModalVisible.value = true
}

// 提交待办表单
const handleTodoSubmit = async () => {
  try {
    await todoStore.createTodo(todoFormData.value)
    Message.success('待办事项创建成功')
    todoModalVisible.value = false
    
    // 如果项目概览模态框是打开的，重新加载数据
    if (overviewModalVisible.value) {
      await Promise.all([
        projectStore.fetchProjectOverview(true),
        todoStore.fetchTodos()
      ])
    }
  } catch (error) {
    Message.error('待办事项创建失败')
  }
}

// 取消待办操作
const handleTodoCancel = () => {
  todoModalVisible.value = false
}

// 备注编辑功能
const editTodoNote = (todo: any) => {
  isEditingNote.value[todo.id] = true
  editingNoteContent.value = todo.description || ''
}

const cancelEditNote = (todoId: number) => {
  isEditingNote.value[todoId] = false
  editingNoteContent.value = ''
}

const saveTodoNote = async (todo: any) => {
  try {
    noteSaving.value = true
    
    // 调用API更新待办事项备注
    await todoStore.updateTodo(todo.id, {
      ...todo,
      description: editingNoteContent.value
    })
    
    // 重新获取任务列表以确保数据实时更新
    await todoStore.fetchTodos()
    
    // 退出编辑模式
    isEditingNote.value[todo.id] = false
    editingNoteContent.value = ''
    
    Message.success('备注更新成功')
  } catch (error) {
    console.error('更新备注失败:', error)
    Message.error('更新备注失败')
  } finally {
    noteSaving.value = false
  }
}

// 🔧 新增甘特图滚动同步方法
const timeAxisRef = ref<HTMLElement>()
const contentRef = ref<HTMLElement>()
const labelsRef = ref<HTMLElement>()
const isScrollSyncing = ref(false)

// 🔧 内容区域滚动时同步时间轴和项目名称列
const onContentScroll = (event: Event) => {
  if (isScrollSyncing.value) return
  
  const target = event.target as HTMLElement
  isScrollSyncing.value = true
  
  // 同步时间轴的横向滚动
  if (timeAxisRef.value) {
    timeAxisRef.value.scrollLeft = target.scrollLeft
  }
  // 同步项目名称列的纵向滚动
  if (labelsRef.value) {
    labelsRef.value.scrollTop = target.scrollTop
  }
  
  // 延迟重置标记
  setTimeout(() => {
    isScrollSyncing.value = false
  }, 10)
}

// 🔧 新增里程碑水平连线方法（节点首尾相连，支持动态高度）
const getMilestoneConnectionStyle = (currentMilestone: any, nextMilestone: any, timeRange: any, index: number, track: any) => {
  if (!timeRange || !currentMilestone.dueDate) return { display: 'none' }
  
  const startDate = new Date(timeRange.startDate)
  const endDate = new Date(timeRange.endDate)
  const currentDate = new Date(currentMilestone.dueDate)
  
  const totalDays = Math.ceil((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))
  const currentDay = Math.ceil((currentDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))
  
  if (currentDay < 0 || currentDay > totalDays) return { display: 'none' }
  
  let leftPercent = (currentDay / totalDays) * 100
  let widthPercent = 0
  
  // 如果有下一个里程碑，连接到下一个
  if (nextMilestone && nextMilestone.dueDate) {
    const nextDate = new Date(nextMilestone.dueDate)
    const nextDay = Math.ceil((nextDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24))
    
    if (nextDay >= 0 && nextDay <= totalDays) {
      widthPercent = ((nextDay - currentDay) / totalDays) * 100
    }
  } else {
    // 最后一个里程碑，如果没有下一个里程碑但有任务，延伸到最后一个任务
    if (track && track.tasks && track.tasks.length > 0) {
      // 不延伸，避免连线过长
      widthPercent = 0
    }
  }
  
  return {
    left: `${leftPercent}%`,
    width: `${Math.max(0, widthPercent)}%`,
    top: '8px', // 水平线条在上方
    height: '3px',
    position: 'absolute' as const,
    zIndex: 3
  }
}

// 判断里程碑是否已完成
const isMilestoneCompleted = (milestone: any) => {
  const today = new Date()
  const milestoneDate = new Date(milestone.dueDate)
  return milestoneDate <= today
}

// 🔧 项目名称列滚动时同步内容区域
const onLabelsScroll = (event: Event) => {
  if (isScrollSyncing.value) return
  
  const target = event.target as HTMLElement
  isScrollSyncing.value = true
  
  if (contentRef.value) {
    contentRef.value.scrollTop = target.scrollTop
  }
  
  // 延迟重置标记
  setTimeout(() => {
    isScrollSyncing.value = false
  }, 10)
}

// 🔧 动态计算轨道高度（基于待办数量）
const getTrackHeight = (track: any) => {
  const baseMilestoneHeight = 50 // 里程碑区域基础高度
  const taskHeight = 26 // 每个任务的高度（包含间距）
  const taskCount = track.tasks ? track.tasks.length : 0
  const minHeight = 160 // 最小高度
  
  const calculatedHeight = baseMilestoneHeight + (taskCount * taskHeight)
  return Math.max(minHeight, calculatedHeight)
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
}

.overview-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.work-update-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.work-mode-switch {
  flex-shrink: 0;
}

.mode-description {
  font-size: 12px;
  color: var(--theme-text-secondary);
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .overview-controls {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .work-update-controls {
    justify-content: center;
  }
  
  .action-buttons {
    justify-content: center;
  }
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

.todos-header {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}

.add-todo-btn {
  font-size: 12px;
  height: 24px;
  padding: 0 8px;
}

.no-todos-message {
  text-align: center;
  padding: 8px;
  margin-top: 8px;
}

.todo-detail-item {
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  max-width: 350px; /* 限制最大宽度 */
  width: 100%;
  box-sizing: border-box;
}

.todo-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.todo-title {
  font-weight: 500;
  color: var(--text-color);
  flex: 1;
  word-wrap: break-word;
  word-break: break-word;
  overflow-wrap: break-word;
  line-height: 1.4;
  max-width: 100%;
  text-wrap-mode: wrap;
  width: 1000px;
}

.todo-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  margin-bottom: 8px;
}

.todo-assignee,
.todo-dates {
  color: var(--text-muted);
  line-height: 1.3;
}

.todo-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 新增样式 */
.edit-note-btn {
  margin-left: 8px;
  opacity: 0.7;
  transition: opacity 0.2s;
  flex-shrink: 0; /* 防止按钮被压缩 */
}

.edit-note-btn:hover {
  opacity: 1;
}

.todo-note-section {
  margin-top: 8px;
  padding: 8px;
  background: var(--theme-bg-secondary);
  border-radius: 4px;
  border: 1px solid var(--border-color);
}

.todo-note-display {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.note-label {
  font-weight: 500;
  color: var(--text-color);
  min-width: 40px;
  flex-shrink: 0; /* 标签不压缩 */
}

.note-content {
  color: var(--text-muted);
  flex: 1;
  word-wrap: break-word;
  white-space: pre-wrap; /* 保持换行符 */
  line-height: 1.4;
  font-size: 12px;
  text-align: left; /* 明确设置左对齐 */
}

.todo-note-edit {
  width: 100%;
}

.note-textarea {
  width: 100%;
  margin-top: 4px;
}

.note-edit-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

/* 状态相关样式 */
.todo-status-tag {
  flex-shrink: 0;
}

.todo-remaining {
  font-weight: 500;
  font-size: 11px;
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

/* 空状态样式 */
.empty-projects {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
}

/* 🔧 重新设计甘特图样式 - 类似Excel的冻结行列布局 */
.gantt-container {
  height: 80vh; /* 固定高度 */
  overflow: hidden; /* 隐藏外层滚动 */
  width: 100%;
  display: flex;
  flex-direction: column;
  position: relative; /* 为绝对定位做准备 */
}

/* 甘特图整体布局 */
.gantt-layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  position: relative;
  overflow: hidden;
  height: 100%; /* 确保填满父容器 */
}

/* 顶部时间轴区域 */
.gantt-header-area {
  display: flex;
  background: var(--bg-color-2);
  border: 1px solid var(--border-color);
  border-radius: 6px 6px 0 0;
  overflow: hidden;
  z-index: 10; /* 确保时间轴在内容之上 */
  position: absolute; /* 绝对定位固定 */
  top: 0;
  left: 0;
  right: 0;
  height: 56px; /* 固定高度 */
}

/* 左上角固定区域 */
.corner-cell {
  width: 150px;
  min-width: 150px;
  padding: 8px 12px;
  background: var(--primary-color);
  color: white;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid var(--border-color);
  font-size: 13px;
  height: 56px; /* 与时间轴区域高度保持一致 */
  box-sizing: border-box; /* 包含padding在高度内 */
  z-index: 15; /* 确保在最上层 */
}

/* 时间轴滚动区域 */
.time-axis-scroll {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  /* 隐藏滚动条但保持滚动功能 */
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.time-axis-scroll::-webkit-scrollbar {
  display: none;
}

/* 时间刻度容器 */
.time-scale {
  display: flex;
  min-width: 4800px; /* 扩大两倍 */
  height: 56px; /* 与时间轴区域高度保持一致 */
}

.gantt-container .arco-spin {
  width: 100%;
}

.gantt-actions {
  margin-bottom: 16px;
  padding: 12px;
  background: var(--card-bg-color);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  flex-shrink: 0; /* 防止被压缩 */
}

.gantt-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.info-label {
  font-weight: 600;
  color: var(--text-color-1);
  font-size: 14px;
}

.empty-gantt {
  text-align: center;
  padding: 60px 0;
  color: var(--text-color-3);
}

.gantt-chart {
  background: var(--card-bg-color);
  border-radius: 8px;
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 允许子元素缩小 */
  position: relative; /* 为子元素绝对定位提供参考 */
  height: calc(80vh - 120px); /* 给定固定高度 */
}

/* 🔧 重新设计gantt-header支持水平布局 */
.gantt-header {
  margin-bottom: 8px; /* 减少边距 */
  padding: 12px; /* 减少内边距 */
  background: var(--bg-color-2);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  flex-shrink: 0; /* 防止被压缩 */
}

.gantt-header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
}

.gantt-header-info {
  flex: 1;
}

.gantt-header-info h3 {
  margin: 0 0 8px 0;
  color: var(--text-color-1);
  font-size: 18px;
}

.project-meta {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: var(--text-color-2);
  flex-wrap: wrap;
}

/* 🔧 header中的图例样式 */
.gantt-legend-header {
  flex-shrink: 0;
  min-width: 400px;
}

.gantt-legend-header h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: var(--text-color-1);
  font-weight: 600;
  text-align: center;
}

.legend-items-inline {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.legend-items-inline .legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--text-color-2);
  padding: 2px 6px;
  background: var(--bg-color-1);
  border-radius: 3px;
  border: 1px solid var(--border-color);
}

/* 🔧 重新设计主体内容区域 - 真正的冻结列布局 */
.gantt-body-area {
  display: flex;
  position: absolute;
  top: 56px; /* 时间轴高度 */
  left: 0;
  right: 0;
  bottom: 0; /* 填满剩余空间 */
  border: 1px solid var(--border-color);
  border-top: none;
  border-radius: 0 0 6px 6px;
  background: var(--bg-color-1);
  overflow: hidden; /* 保持隐藏，让内部元素处理滚动 */
  z-index: 1; /* 确保内容在上层 */
}

/* 🔧 左侧固定项目名称列（真正冻结） */
.track-labels-frozen {
  width: 150px;
  min-width: 150px;
  background: var(--bg-color-2);
  border-right: 1px solid var(--border-color);
  overflow-y: auto;
  overflow-x: hidden;
  flex-shrink: 0;
  z-index: 5; /* 调整层级，低于时间轴 */
  height: 100%; /* 填满父容器高度 */
}

.track-label-cell {
  /* height 通过 :style 动态设置 */
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  font-weight: 600;
  color: var(--text-color-1);
  font-size: 13px;
  background: var(--bg-color-2);
  word-break: break-word;
  line-height: 1.3;
  /* 🔧 让文本垂直居中 */
  justify-content: flex-start;
  text-align: left;
}

/* 右侧内容滚动区域 */
.gantt-content-scroll {
  flex: 1;
  overflow: auto;
  position: relative;
  height: 100%; /* 填满父容器高度 */
}

/* 轨道容器 */
.tracks-container {
  min-width: 4800px; /* 扩大两倍 */
  position: relative;
  min-height: 100%; /* 确保容器高度充足 */
  height: fit-content; /* 自适应内容高度 */
}

/* 🔧 项目轨道内容 - 动态高度 */
.project-track-content {
  /* height 通过 :style 动态设置 */
  border-bottom: 1px solid var(--border-color);
  position: relative;
  background: var(--bg-color-1);
  min-height: 160px; /* 最小高度保证 */
}

/* 🔧 清理：移除不再使用的旧样式 */

.time-unit {
  flex: 1;
  padding: 8px 4px;
  text-align: center;
  font-size: 12px;
  border-right: 1px solid var(--border-color);
  color: var(--text-color-2);
  height: 56px; /* 与时间轴区域高度保持一致 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-color-1);
  min-width: 0; /* 允许flex收缩 */
}

.time-unit.current-time {
  background: linear-gradient(135deg, #1890ff20, #40a9ff20);
  color: var(--primary-color);
  font-weight: 600;
}

/* 🔧 清理：移除不再使用的旧项目轨道样式 */

/* 🔧 里程碑水平连线（节点首尾相连的时间轴） */
.milestone-timeline-horizontal {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none; /* 不影响其他元素的交互 */
  z-index: 1;
}

.milestone-connection {
  position: absolute;
  background: #d9d9d9; /* 默认灰色 - 未发生 */
  opacity: 0.7;
  transition: all 0.3s ease;
  border-radius: 1px;
}

.milestone-connection.connection-completed {
  background: #1890ff; /* 蓝色 - 已发生 */
  opacity: 0.9;
  box-shadow: 0 1px 3px rgba(24, 144, 255, 0.3);
}

.milestone-marker {
  position: absolute;
  top: 5px; /* 🔧 增加与时间线的间距，防止文字重叠 */
  transform: translateY(0);
  cursor: pointer;
  z-index: 10;
}

.milestone-diamond {
  width: 12px;
  height: 12px;
  transform: rotate(45deg);
  border: 1px solid white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.milestone-label {
  position: absolute;
  top: 15px; /* 🔧 增加间距，防止与时间线重叠 */
  left: 50%;
  transform: translateX(-50%);
  font-size: 9px;
  color: var(--text-color-2);
  white-space: nowrap;
  background: var(--bg-color-1);
  padding: 1px 3px;
  border-radius: 2px;
  border: 1px solid var(--border-color);
  z-index: 11;
}

.task-bar {
  position: absolute;
  /* top值由JavaScript动态计算，避免重叠 */
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  /* z-index由JavaScript动态计算 */
}

.task-bar:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

.task-content {
  position: relative;
  z-index: 2;
  padding: 2px 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.task-title {
  font-size: 11px;
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 2px rgba(0,0,0,0.3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.task-progress {
  font-size: 10px;
  color: rgba(255,255,255,0.9);
}

.task-progress-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  transition: width 0.3s ease;
  border-radius: 2px;
}

/* 🔧 清理：移除不再使用的固定图例样式 */

/* 兼容原有样式（如果有其他地方引用） */
.gantt-legend {
  margin-top: 20px;
  padding: 16px;
  background: var(--bg-color-2);
  border-radius: 6px;
  border: 1px solid var(--border-color);
}

.gantt-legend h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--text-color-1);
}

.gantt-legend .legend-items {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}

.gantt-legend .legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-color-2);
}

.legend-bar {
  width: 16px;
  height: 8px;
  border-radius: 2px;
  flex-shrink: 0;
}
</style>
