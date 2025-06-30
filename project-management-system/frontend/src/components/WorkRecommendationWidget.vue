<template>
  <div class="work-recommendation-widget" :class="[{ 'expanded': isExpanded }, `${themeStore.actualTheme}-theme`]">
    <!-- 浮动按钮 -->
    <div class="widget-trigger" @click="toggleExpanded" v-if="!isExpanded">
      <div class="trigger-content">
        <div class="trigger-icon">💡</div>
        <div class="trigger-text">工作推荐</div>
        <div class="trigger-badge" v-if="totalCount > 0">{{ totalCount }}</div>
      </div>
    </div>

    <!-- 展开的推荐面板 -->
    <div class="widget-panel" v-if="isExpanded">
      <div class="panel-header">
        <div class="panel-title">
          <span class="title-icon">💡</span>
          <span class="title-text">智能工作推荐</span>
          <span class="total-count" v-if="totalCount > 0">({{ totalCount }})</span>
        </div>
        <div class="panel-actions">
          <a-button size="small" @click="refreshRecommendations" :loading="loading">
            <template #icon><icon-refresh /></template>
          </a-button>
          <a-button size="small" @click="toggleExpanded">
            <template #icon><icon-close /></template>
          </a-button>
        </div>
      </div>

      <div class="panel-content">
        <div class="recommendation-sections">
          <!-- 紧急推进 -->
          <div class="recommendation-section" v-if="recommendations.urgent.length > 0">
            <div class="section-header">
              <span class="section-icon">🔥</span>
              <span class="section-title">紧急推进</span>
              <span class="section-count">({{ recommendations.urgent.length }})</span>
            </div>
            <div class="recommendation-list">
              <div class="recommendation-item urgent" v-for="item in recommendations.urgent" :key="item.id"
                @click="handleRecommendationClick(item)">
                <div class="item-content">
                  <div class="item-title">{{ item.title }}</div>
                  <div class="item-description">{{ item.description }}</div>
                </div>
                <div class="item-actions">
                  <a-button size="mini" type="primary" @click.stop="executeRecommendation(item)">
                    立即处理
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 风险预警 -->
          <div class="recommendation-section" v-if="recommendations.risk.length > 0">
            <div class="section-header">
              <span class="section-icon">⚠️</span>
              <span class="section-title">风险预警</span>
              <span class="section-count">({{ recommendations.risk.length }})</span>
            </div>
            <div class="recommendation-list">
              <div class="recommendation-item risk" v-for="item in recommendations.risk" :key="item.id"
                @click="handleRecommendationClick(item)">
                <div class="item-content">
                  <div class="item-title risk-title">
                    <span class="time-badge" 
                          :class="getTimeBadgeClass(extractTimeBadge(item.title))"
                          v-if="extractTimeBadge(item.title)">{{ extractTimeBadge(item.title) }}</span>
                    <span class="title-text">{{ extractTitleText(item.title) }}</span>
                  </div>
                  <div class="item-description">{{ item.description }}</div>
                </div>
                <div class="item-actions">
                  <a-button size="mini" type="primary" status="warning" @click.stop="executeRecommendation(item)">
                    立即查看
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 项目停滞功能已移除 -->
          <!-- <div class="recommendation-section" v-if="recommendations.stagnant.length > 0">
            <div class="section-header">
              <span class="section-icon">🛑</span>
              <span class="section-title">项目停滞</span>
              <span class="section-count">({{ recommendations.stagnant.length }})</span>
            </div>
            <div class="recommendation-list">
              <div class="recommendation-item stagnant" v-for="item in recommendations.stagnant" :key="item.id"
                @click="handleRecommendationClick(item)">
                <div class="item-content">
                  <div class="item-title">{{ item.title }}</div>
                  <div class="item-description">{{ item.description }}</div>
                </div>
                <div class="item-actions">
                  <a-button size="mini" type="outline" @click.stop="executeRecommendation(item)">
                    去推进
                  </a-button>
                </div>
              </div>
            </div>
          </div> -->

          <!-- 协作推进 -->
          <div class="recommendation-section" v-if="recommendations.collaboration.length > 0">
            <div class="section-header">
              <span class="section-icon">🤝</span>
              <span class="section-title">协作推进</span>
              <span class="section-count">({{ recommendations.collaboration.length }})</span>
            </div>
            <div class="recommendation-list">
              <div class="recommendation-item collaboration" v-for="item in recommendations.collaboration" :key="item.id"
                @click="handleRecommendationClick(item)">
                <div class="item-content">
                  <div class="item-title">{{ item.title }}</div>
                  <div class="item-description">{{ item.description }}</div>
                </div>
                <div class="item-actions">
                  <a-button size="mini" type="outline" @click.stop="executeRecommendation(item)">
                    去协作
                  </a-button>
                </div>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div class="empty-state" v-if="totalCount === 0">
            <div class="empty-icon">✨</div>
            <div class="empty-text">暂无工作推荐</div>
            <div class="empty-description">你的项目管理状态良好</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Message } from '@arco-design/web-vue'
import { IconRefresh, IconClose } from '@arco-design/web-vue/es/icon'
import { workRecommendationApi } from '@/api/workRecommendations'
import type { WorkRecommendationSummary, WorkRecommendation } from '@/types'
import { useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/theme'

// 状态管理
const isExpanded = ref(false)
const loading = ref(false)
const recommendations = ref<WorkRecommendationSummary>({
  urgent: [],
  stagnant: [],
  progress: [],
  collaboration: [],
  risk: [],
  suggestions: [],
  totalCount: 0
})

// 路由和主题
const router = useRouter()
const themeStore = useThemeStore()

// 计算属性
const totalCount = computed(() => recommendations.value.totalCount)

// 切换展开状态
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value
  if (isExpanded.value) {
    loadRecommendations()
  }
}

// 加载推荐数据
const loadRecommendations = async () => {
  loading.value = true
  try {
    const response = await workRecommendationApi.getRecommendations()
    if (response.code === 0) {
      recommendations.value = response.data
    } else {
      Message.error(response.msg || '加载推荐失败')
    }
  } catch (error) {
    console.error('加载推荐失败:', error)
    Message.error('加载推荐失败')
  } finally {
    loading.value = false
  }
}

// 刷新推荐
const refreshRecommendations = () => {
  loadRecommendations()
}

// 处理推荐项点击
const handleRecommendationClick = (item: WorkRecommendation) => {
  if (item.actionType === 'VIEW_PROJECT' && item.projectId) {
    // 跳转到项目页面，并尝试定位到具体项目
    router.push('/projects')
    // 延迟一点时间后触发页面内搜索或定位（如果项目页面支持的话）
    setTimeout(() => {
      const event = new CustomEvent('locateProject', { detail: { projectId: item.projectId } })
      window.dispatchEvent(event)
    }, 500)
  } else if (item.actionType === 'VIEW_TODO' && item.todoId) {
    // 跳转到待办页面，并尝试定位到具体待办
    router.push('/todos')
    setTimeout(() => {
      const event = new CustomEvent('locateTodo', { detail: { todoId: item.todoId } })
      window.dispatchEvent(event)
    }, 500)
  } else if (item.actionType === 'CREATE_TODO') {
    // 跳转到待办页面创建新任务
    router.push('/todos')
  } else {
    // 默认跳转到项目页面
    router.push('/projects')
  }
}

// 执行推荐操作
const executeRecommendation = async (item: WorkRecommendation) => {
  try {
    const response = await workRecommendationApi.executeRecommendation(item.id, item.actionData)
    if (response.code === 0) {
      // Message.success('操作成功')
      handleRecommendationClick(item)
      loadRecommendations()
    } else {
      Message.error(response.msg || '操作失败')
    }
  } catch (error) {
    console.error('执行推荐操作失败:', error)
    Message.error('操作失败')
  }
}

// 提取时间标识
const extractTimeBadge = (title: string) => {
  const match = title.match(/^\[([^\]]+)\]/)
  return match ? match[1] : null
}

// 提取标题文本（去除时间标识）
const extractTitleText = (title: string) => {
  return title.replace(/^\[([^\]]+)\]\s*/, '')
}

// 获取时间标识的样式类
const getTimeBadgeClass = (timeBadge: string | null) => {
  if (!timeBadge) return ''
  
  if (timeBadge.includes('逾期')) {
    return 'overdue'
  } else if (timeBadge.includes('今日')) {
    return 'today'
  } else if (timeBadge.includes('剩余')) {
    const days = parseInt(timeBadge.match(/\d+/)?.[0] || '0')
    if (days <= 1) {
      return 'urgent'
    } else if (days <= 3) {
      return 'warning' 
    } else {
      return 'normal'
    }
  }
  return 'normal'
}

// 生命周期
onMounted(() => {
  loadRecommendations()
})
</script>

<style scoped>
.work-recommendation-widget {
  position: fixed;
  top: 50%;
  right: 20px;
  transform: translateY(-50%);
  z-index: 1000;
}

.widget-trigger {
  background: var(--primary-color);
  color: white;
  border-radius: 25px;
  padding: 12px 20px;
  cursor: pointer;
  box-shadow: 0 4px 20px var(--primary-color-light);
  transition: all 0.3s ease;
}

.widget-trigger:hover {
  transform: translateY(-2px);
  background: var(--theme-primary-hover);
  box-shadow: 0 6px 25px var(--primary-color-light);
}

.trigger-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.trigger-badge {
  background: #ff4757;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: bold;
}

.widget-panel {
  background: var(--card-bg-color);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: 0 12px 40px var(--shadow-color);
  width: 380px;
  max-height: 70vh;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: var(--primary-color);
  color: white;
}

.panel-content {
  max-height: calc(70vh - 60px);
  overflow-y: auto;
}

.recommendation-section {
  border-bottom: 1px solid var(--border-color);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: var(--theme-bg-secondary);
  color: var(--text-color);
  font-weight: 600;
}

.recommendation-item {
  display: flex;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--card-bg-color);
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.recommendation-item:hover {
  background: var(--hover-bg-color);
}

.recommendation-item.urgent {
  border-left: 4px solid #ff4757;
}

.recommendation-item.stagnant {
  border-left: 4px solid #ffa502;
}

.recommendation-item.risk {
  border-left: 4px solid #ff6b35;
}

.recommendation-item.collaboration {
  border-left: 4px solid #3742fa;
}

.item-content {
  flex: 1;
  margin-right: 12px;
}

.item-title {
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--text-color);
}

.risk-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.time-badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
  color: white;
  white-space: nowrap;
  transition: all 0.3s ease;
}

.time-badge.overdue {
  background: linear-gradient(135deg, #ff3742, #ff1744);
  box-shadow: 0 2px 6px rgba(255, 23, 68, 0.4);
  animation: pulse-urgent 1.5s infinite;
}

.time-badge.today {
  background: linear-gradient(135deg, #ff6348, #ff4757);
  box-shadow: 0 2px 6px rgba(255, 71, 87, 0.4);
  animation: pulse-glow 2s infinite;
}

.time-badge.urgent {
  background: linear-gradient(135deg, #ff7675, #e84393);
  box-shadow: 0 2px 4px rgba(255, 118, 117, 0.3);
}

.time-badge.warning {
  background: linear-gradient(135deg, #fdcb6e, #e17055);
  box-shadow: 0 2px 4px rgba(253, 203, 110, 0.3);
}

.time-badge.normal {
  background: linear-gradient(135deg, #74b9ff, #0984e3);
  box-shadow: 0 2px 4px rgba(116, 185, 255, 0.3);
}

.title-text {
  flex: 1;
  line-height: 1.4;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 2px 6px rgba(255, 71, 87, 0.4);
  }
  50% {
    box-shadow: 0 2px 12px rgba(255, 71, 87, 0.6), 0 0 20px rgba(255, 71, 87, 0.3);
  }
}

@keyframes pulse-urgent {
  0%, 100% {
    box-shadow: 0 2px 6px rgba(255, 23, 68, 0.4);
    transform: scale(1);
  }
  50% {
    box-shadow: 0 2px 16px rgba(255, 23, 68, 0.8), 0 0 24px rgba(255, 23, 68, 0.4);
    transform: scale(1.05);
  }
}

.item-description {
  font-size: 12px;
  color: var(--text-muted);
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  color: var(--text-muted);
}
</style>