import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { projectApi } from "@/api";
import type { Project, ProjectDTO } from "@/types";

export const useProjectStore = defineStore("projects", () => {
  // 状态
  const projects = ref<Project[]>([]);
  const loading = ref(false);
  const currentProject = ref<Project | null>(null);

  // 计算属性
  const projectCount = computed(() => projects.value.length);
  const activeProjects = computed(() =>
    projects.value.filter((p) => p.status === "PROGRESS")
  );
  const completedProjects = computed(() =>
    projects.value.filter((p) => p.status === "COMPLETED")
  );

  // 获取项目列表
  const fetchProjects = async () => {
    try {
      loading.value = true;
      const response = await projectApi.getProjects();
      projects.value = response.data;
    } catch (error) {
      console.error("获取项目列表失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取项目详情
  const fetchProject = async (id: number) => {
    try {
      loading.value = true;
      const response = await projectApi.getProject(id);
      currentProject.value = response.data;
      return response.data;
    } catch (error) {
      console.error("获取项目详情失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 创建项目
  const createProject = async (data: ProjectDTO) => {
    try {
      loading.value = true;
      const response = await projectApi.createProject(data);
      projects.value.push(response.data);
      return response.data;
    } catch (error) {
      console.error("创建项目失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 更新项目
  const updateProject = async (id: number, data: ProjectDTO) => {
    try {
      loading.value = true;
      const response = await projectApi.updateProject(id, data);
      const index = projects.value.findIndex((p) => p.id === id);
      if (index !== -1) {
        projects.value[index] = response.data;
      }
      if (currentProject.value?.id === id) {
        currentProject.value = response.data;
      }
      return response.data;
    } catch (error) {
      console.error("更新项目失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 删除项目
  const deleteProject = async (id: number) => {
    try {
      loading.value = true;
      await projectApi.deleteProject(id);
      projects.value = projects.value.filter((p) => p.id !== id);
      if (currentProject.value?.id === id) {
        currentProject.value = null;
      }
    } catch (error) {
      console.error("删除项目失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 更新项目进度
  const updateProgress = async (id: number, progress: number) => {
    try {
      await projectApi.updateProgress(id, progress);
      const project = projects.value.find((p) => p.id === id);
      if (project) {
        project.progress = progress;
      }
      if (currentProject.value?.id === id) {
        currentProject.value.progress = progress;
      }
    } catch (error) {
      console.error("更新项目进度失败:", error);
      throw error;
    }
  };

  return {
    // 状态
    projects,
    loading,
    currentProject,

    // 计算属性
    projectCount,
    activeProjects,
    completedProjects,

    // 方法
    fetchProjects,
    fetchProject,
    createProject,
    updateProject,
    deleteProject,
    updateProgress,
  };
});
