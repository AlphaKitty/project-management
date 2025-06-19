import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { projectApi } from "@/api";
import { useBaseStore } from "./composables/useBaseStore";
import { useGlobalState } from "./composables/useGlobalState";
import type { Project, ProjectDTO } from "@/types";

// 项目API适配器
const projectApiAdapter = {
  getList: () => projectApi.getProjects(),
  getDetail: (id: number) => projectApi.getProject(id),
  create: (data: ProjectDTO) => projectApi.createProject(data),
  update: (id: number, data: ProjectDTO) => projectApi.updateProject(id, data),
  delete: (id: number) => projectApi.deleteProject(id),
};

export const useProjectStore = defineStore("projects", () => {
  // 使用基础Store功能
  const baseStore = useBaseStore<Project>(
    "project",
    projectApiAdapter,
    { ttl: 10 * 60 * 1000, maxSize: 50 } // 10分钟缓存，最多50个项目
  );

  // 全局状态
  const globalState = useGlobalState();

  // 扩展状态
  const overviewProjects = ref<Project[]>([]);
  const currentProject = ref<Project | null>(null);
  const overviewLoading = ref(false);

  // 计算属性（基于baseStore的数据）
  const projects = computed(() => baseStore.data.value);
  const projectCount = computed(() => baseStore.count.value);
  const loading = computed(() => baseStore.loading.value);

  const activeProjects = computed(() =>
    projects.value.filter((p) => p.status === "PROGRESS")
  );

  const completedProjects = computed(() =>
    projects.value.filter((p) => p.status === "COMPLETED")
  );

  const projectsByStatus = computed(() => {
    return projects.value.reduce(
      (acc, project) => {
        if (!acc[project.status]) {
          acc[project.status] = [];
        }
        acc[project.status].push(project);
        return acc;
      },
      {} as Record<string, Project[]>
    );
  });

  // 获取项目列表（增强版）
  const fetchProjects = async (force = false) => {
    try {
      const result = await baseStore.fetchList(force);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "projects",
      });

      return result;
    } catch (error) {
      console.error("获取项目列表失败:", error);
      throw error;
    }
  };

  // 获取项目概览（优化缓存）
  const fetchProjectOverview = async (force = false) => {
    const cacheKey = "overview";

    // 检查缓存
    if (!force) {
      const cached = baseStore.getCachedData(cacheKey);
      if (cached) {
        overviewProjects.value = cached;
        return cached;
      }
    }

    try {
      overviewLoading.value = true;
      const response = await projectApi.getProjectOverview();
      overviewProjects.value = response.data;

      // 更新缓存
      baseStore.setCachedData(cacheKey, response.data);

      return response.data;
    } catch (error) {
      console.error("获取项目概览失败:", error);
      throw error;
    } finally {
      overviewLoading.value = false;
    }
  };

  // 获取项目详情（增强版）
  const fetchProject = async (id: number, useCache = true) => {
    try {
      const result = await baseStore.fetchDetail(id, useCache);
      currentProject.value = result;
      return result;
    } catch (error) {
      console.error("获取项目详情失败:", error);
      throw error;
    }
  };

  // 创建项目（增强版）
  const createProject = async (data: ProjectDTO) => {
    try {
      const result = await baseStore.create(data);

      // 清除概览缓存
      baseStore.setCachedData("overview", null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "create",
        entity: "projects",
        data: result,
      });

      return result;
    } catch (error) {
      console.error("创建项目失败:", error);
      throw error;
    }
  };

  // 更新项目（增强版）
  const updateProject = async (id: number, data: ProjectDTO) => {
    try {
      const result = await baseStore.update(id, data);

      // 更新当前项目
      if (currentProject.value?.id === id) {
        currentProject.value = result;
      }

      // 清除概览缓存
      baseStore.setCachedData("overview", null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "projects",
        id,
        data: result,
      });

      return result;
    } catch (error) {
      console.error("更新项目失败:", error);
      throw error;
    }
  };

  // 删除项目（增强版）
  const deleteProject = async (id: number) => {
    try {
      await baseStore.remove(id);

      // 清理相关状态
      if (currentProject.value?.id === id) {
        currentProject.value = null;
      }

      overviewProjects.value = overviewProjects.value.filter(
        (p) => p.id !== id
      );

      // 清除概览缓存
      baseStore.setCachedData("overview", null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "delete",
        entity: "projects",
        id,
      });
    } catch (error) {
      console.error("删除项目失败:", error);
      throw error;
    }
  };

  // 更新项目进度（优化版）
  const updateProgress = async (id: number, progress: number) => {
    try {
      await projectApi.updateProgress(id, progress);

      // 乐观更新本地状态
      const project = projects.value.find((p) => p.id === id);
      if (project) {
        project.progress = progress;
      }

      if (currentProject.value?.id === id) {
        currentProject.value.progress = progress;
      }

      // 更新概览中的项目
      const overviewProject = overviewProjects.value.find((p) => p.id === id);
      if (overviewProject) {
        overviewProject.progress = progress;
      }

      // 清除详情缓存
      baseStore.setCachedData(`detail_${id}`, null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "projects",
        id,
        data: { progress },
      });
    } catch (error) {
      // 错误时回滚本地状态
      await fetchProject(id, false);
      console.error("更新项目进度失败:", error);
      throw error;
    }
  };

  // 批量操作
  const batchUpdateStatus = async (ids: number[], status: string) => {
    const operations = ids.map((id) =>
      updateProject(id, { status } as ProjectDTO)
    );

    try {
      await Promise.all(operations);
    } catch (error) {
      console.error("批量更新状态失败:", error);
      // 重新获取数据以确保一致性
      await fetchProjects(true);
      throw error;
    }
  };

  // 搜索项目（本地搜索，性能优化）
  const searchProjects = (keyword: string) => {
    if (!keyword.trim()) return projects.value;

    const lowerKeyword = keyword.toLowerCase();
    return projects.value.filter(
      (project) =>
        project.name?.toLowerCase().includes(lowerKeyword) ||
        project.description?.toLowerCase().includes(lowerKeyword)
    );
  };

  // 获取项目统计信息
  const getProjectStats = computed(() => ({
    total: projectCount.value,
    active: activeProjects.value.length,
    completed: completedProjects.value.length,
    planning: projects.value.filter((p) => p.status === "PENDING").length,
    avgProgress:
      projects.value.length > 0
        ? Math.round(
            projects.value.reduce((sum, p) => sum + (p.progress || 0), 0) /
              projects.value.length
          )
        : 0,
  }));

  // 清理方法
  const cleanup = () => {
    baseStore.reset();
    overviewProjects.value = [];
    currentProject.value = null;
    overviewLoading.value = false;
  };

  return {
    // 状态
    projects,
    overviewProjects,
    loading: computed(() => loading.value || overviewLoading.value),
    currentProject,

    // 计算属性
    projectCount,
    activeProjects,
    completedProjects,
    projectsByStatus,
    getProjectStats,

    // 方法
    fetchProjects,
    fetchProjectOverview,
    fetchProject,
    createProject,
    updateProject,
    deleteProject,
    updateProgress,
    batchUpdateStatus,
    searchProjects,
    cleanup,

    // 基础Store方法
    findById: baseStore.findById,
    clearCache: baseStore.clearCache,
    reset: baseStore.reset,
  };
});
