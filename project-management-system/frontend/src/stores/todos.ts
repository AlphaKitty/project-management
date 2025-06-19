import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { todoApi } from "@/api";
import { useBaseStore } from "./composables/useBaseStore";
import { useGlobalState } from "./composables/useGlobalState";
import type { Todo, TodoDTO } from "@/types";

// Todo API适配器
const todoApiAdapter = {
  getList: () => todoApi.getTodos(),
  getDetail: (id: number) => todoApi.getTodo(id),
  create: (data: TodoDTO) => todoApi.createTodo(data),
  update: (id: number, data: TodoDTO) => todoApi.updateTodo(id, data),
  delete: (id: number) => todoApi.deleteTodo(id),
};

export const useTodoStore = defineStore("todos", () => {
  // 使用基础Store功能
  const baseStore = useBaseStore<Todo>(
    "todo",
    todoApiAdapter,
    { ttl: 5 * 60 * 1000, maxSize: 200 } // 5分钟缓存，最多200个待办
  );

  // 全局状态
  const globalState = useGlobalState();

  // 扩展状态
  const todayTodos = ref<Todo[]>([]);
  const weekTodos = ref<Todo[]>([]);
  const highTodos = ref<Todo[]>([]);
  const currentTodo = ref<Todo | null>(null);

  // 计算属性（基于baseStore的数据）
  const todos = computed(() => baseStore.data.value);
  const todoCount = computed(() => baseStore.count.value);
  const loading = computed(() => baseStore.loading.value);

  const pendingTodos = computed(() =>
    todos.value.filter((t) => t.status === "TODO")
  );

  const completedTodos = computed(() =>
    todos.value.filter((t) => t.status === "DONE")
  );

  const inProgressTodos = computed(() =>
    todos.value.filter((t) => t.status === "PROGRESS")
  );

  const highPriorityTodos = computed(() =>
    todos.value.filter((t) => t.priority === "HIGH")
  );

  const overdueTodos = computed(() =>
    todos.value.filter((t) => {
      if (!t.dueDate || t.status === "DONE") return false;

      // 获取今天的日期（只比较年月日，忽略时间）
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      // 解析截止日期（只比较年月日）
      const dueDate = new Date(t.dueDate);
      dueDate.setHours(23, 59, 59, 999); // 设置为当天的最后一刻

      // 逾期任务：截止日期小于今天
      return dueDate < today;
    })
  );

  // 按状态分组的待办
  const todosByStatus = computed(() => {
    return todos.value.reduce(
      (acc, todo) => {
        if (!acc[todo.status]) {
          acc[todo.status] = [];
        }
        acc[todo.status].push(todo);
        return acc;
      },
      {} as Record<string, Todo[]>
    );
  });

  // 按优先级分组的待办
  const todosByPriority = computed(() => {
    return todos.value.reduce(
      (acc, todo) => {
        if (!acc[todo.priority]) {
          acc[todo.priority] = [];
        }
        acc[todo.priority].push(todo);
        return acc;
      },
      {} as Record<string, Todo[]>
    );
  });

  // 获取待办任务列表（增强版）
  const fetchTodos = async (force = false) => {
    try {
      const result = await baseStore.fetchList(force);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "todos",
      });

      return result;
    } catch (error) {
      console.error("获取待办任务列表失败:", error);
      throw error;
    }
  };

  // 获取今日待办任务（优化缓存）
  const fetchTodayTodos = async (force = false) => {
    const cacheKey = "today";

    // 检查缓存
    if (!force) {
      const cached = baseStore.getCachedData(cacheKey);
      if (cached) {
        todayTodos.value = cached;
        return cached;
      }
    }

    try {
      const response = await todoApi.getTodayTodos();
      todayTodos.value = response.data;

      // 更新缓存
      baseStore.setCachedData(cacheKey, response.data);

      return response.data;
    } catch (error) {
      console.error("获取今日待办任务失败:", error);
      throw error;
    }
  };

  // 获取本周待办任务（优化缓存）
  const fetchWeekTodos = async (force = false) => {
    const cacheKey = "week";

    // 检查缓存
    if (!force) {
      const cached = baseStore.getCachedData(cacheKey);
      if (cached) {
        weekTodos.value = cached;
        return cached;
      }
    }

    try {
      const response = await todoApi.getWeekTodos();
      weekTodos.value = response.data;

      // 更新缓存
      baseStore.setCachedData(cacheKey, response.data);

      return response.data;
    } catch (error) {
      console.error("获取本周待办任务失败:", error);
      throw error;
    }
  };

  // 获取高优先级待办任务
  const fetchHighPriorityTodos = async (force = false) => {
    const cacheKey = "high";

    // 检查缓存
    if (!force) {
      const cached = baseStore.getCachedData(cacheKey);
      if (cached) {
        highTodos.value = cached;
        return cached;
      }
    }

    try {
      const response = await todoApi.getHighPriorityTodos();
      highTodos.value = response.data;

      // 更新缓存
      baseStore.setCachedData(cacheKey, response.data);

      return response.data;
    } catch (error) {
      console.error("获取高优先级待办任务失败:", error);
      throw error;
    }
  };

  // 获取待办任务详情（增强版）
  const fetchTodo = async (id: number, useCache = true) => {
    try {
      const result = await baseStore.fetchDetail(id, useCache);
      currentTodo.value = result;
      return result;
    } catch (error) {
      console.error("获取待办任务详情失败:", error);
      throw error;
    }
  };

  // 创建待办任务（增强版）
  const createTodo = async (data: TodoDTO) => {
    try {
      const result = await baseStore.create(data);

      // 清除相关缓存
      baseStore.setCachedData("today", null);
      baseStore.setCachedData("week", null);
      baseStore.setCachedData("high", null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "create",
        entity: "todos",
        data: result,
      });

      return result;
    } catch (error) {
      console.error("创建待办任务失败:", error);
      throw error;
    }
  };

  // 更新待办任务（增强版）
  const updateTodo = async (id: number, data: TodoDTO) => {
    try {
      const result = await baseStore.update(id, data);

      // 更新当前任务
      if (currentTodo.value?.id === id) {
        currentTodo.value = result;
      }

      // 更新相关列表
      updateRelatedLists(id, result);

      // 清除相关缓存
      clearRelatedCache();

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "todos",
        id,
        data: result,
      });

      return result;
    } catch (error) {
      console.error("更新待办任务失败:", error);
      throw error;
    }
  };

  // 删除待办任务（增强版）
  const deleteTodo = async (id: number) => {
    try {
      await baseStore.remove(id);

      // 清理相关状态
      if (currentTodo.value?.id === id) {
        currentTodo.value = null;
      }

      // 从相关列表中删除
      todayTodos.value = todayTodos.value.filter((t) => t.id !== id);
      weekTodos.value = weekTodos.value.filter((t) => t.id !== id);
      highTodos.value = highTodos.value.filter((t) => t.id !== id);

      // 清除相关缓存
      clearRelatedCache();

      // 添加同步事件
      globalState.addSyncEvent({
        type: "delete",
        entity: "todos",
        id,
      });
    } catch (error) {
      console.error("删除待办任务失败:", error);
      throw error;
    }
  };

  // 更新任务状态（优化版）
  const updateStatus = async (id: number, status: string) => {
    try {
      await todoApi.updateStatus(id, status);

      // 乐观更新本地状态
      const todo = todos.value.find((t) => t.id === id);
      if (todo) {
        todo.status = status as any;
        if (status === "DONE") {
          todo.completedTime = new Date().toISOString();
        }
      }

      if (currentTodo.value?.id === id) {
        currentTodo.value.status = status as any;
        if (status === "DONE") {
          currentTodo.value.completedTime = new Date().toISOString();
        }
      }

      // 更新相关列表
      updateRelatedLists(id, todo);

      // 清除详情缓存
      baseStore.setCachedData(`detail_${id}`, null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "todos",
        id,
        data: { status },
      });
    } catch (error) {
      // 错误时回滚本地状态
      await fetchTodo(id, false);
      console.error("更新任务状态失败:", error);
      throw error;
    }
  };

  // 发送邮件（保持原有功能）
  const sendEmail = async (email: string, userId?: number) => {
    try {
      const response = await todoApi.sendEmail(email, userId);
      return response;
    } catch (error) {
      console.error("发送邮件失败:", error);
      throw error;
    }
  };

  // 辅助方法：更新相关列表
  const updateRelatedLists = (id: number, updatedTodo: Todo | undefined) => {
    if (!updatedTodo) return;

    // 更新今日任务列表
    const todayIndex = todayTodos.value.findIndex((t) => t.id === id);
    if (todayIndex !== -1) {
      todayTodos.value[todayIndex] = updatedTodo;
    }

    // 更新本周任务列表
    const weekIndex = weekTodos.value.findIndex((t) => t.id === id);
    if (weekIndex !== -1) {
      weekTodos.value[weekIndex] = updatedTodo;
    }

    // 更新高优先级任务列表
    const highIndex = highTodos.value.findIndex((t) => t.id === id);
    if (highIndex !== -1) {
      highTodos.value[highIndex] = updatedTodo;
    }
  };

  // 辅助方法：清除相关缓存
  const clearRelatedCache = () => {
    baseStore.setCachedData("today", null);
    baseStore.setCachedData("week", null);
    baseStore.setCachedData("high", null);
  };

  // 批量操作
  const batchUpdateStatus = async (ids: number[], status: string) => {
    const operations = ids.map((id) => updateStatus(id, status));

    try {
      await Promise.all(operations);
    } catch (error) {
      console.error("批量更新状态失败:", error);
      // 重新获取数据以确保一致性
      await fetchTodos(true);
      throw error;
    }
  };

  // 搜索待办（本地搜索）
  const searchTodos = (keyword: string) => {
    if (!keyword.trim()) return todos.value;

    const lowerKeyword = keyword.toLowerCase();
    return todos.value.filter(
      (todo) =>
        todo.title?.toLowerCase().includes(lowerKeyword) ||
        todo.description?.toLowerCase().includes(lowerKeyword)
    );
  };

  // 按条件筛选待办
  const filterTodos = (filters: {
    status?: string[];
    priority?: string[];
    assigneeId?: number;
    projectId?: number;
    overdue?: boolean;
  }) => {
    return todos.value.filter((todo) => {
      if (filters.status && !filters.status.includes(todo.status)) return false;
      if (filters.priority && !filters.priority.includes(todo.priority))
        return false;
      if (filters.assigneeId && todo.assignee?.id !== filters.assigneeId)
        return false;
      if (filters.projectId && todo.projectId !== filters.projectId)
        return false;
      if (filters.overdue) {
        const isOverdue = overdueTodos.value.some((t) => t.id === todo.id);
        if (!isOverdue) return false;
      }
      return true;
    });
  };

  // 获取待办统计信息
  const getTodoStats = computed(() => ({
    total: todoCount.value,
    pending: pendingTodos.value.length,
    inProgress: inProgressTodos.value.length,
    completed: completedTodos.value.length,
    high: highPriorityTodos.value.length,
    overdue: overdueTodos.value.length,
    completionRate:
      todoCount.value > 0
        ? Math.round((completedTodos.value.length / todoCount.value) * 100)
        : 0,
  }));

  // 清理方法
  const cleanup = () => {
    baseStore.reset();
    todayTodos.value = [];
    weekTodos.value = [];
    highTodos.value = [];
    currentTodo.value = null;
  };

  return {
    // 状态
    todos,
    todayTodos,
    weekTodos,
    highTodos,
    loading,
    currentTodo,

    // 计算属性
    todoCount,
    pendingTodos,
    completedTodos,
    inProgressTodos,
    highPriorityTodos,
    overdueTodos,
    todosByStatus,
    todosByPriority,
    getTodoStats,

    // 方法
    fetchTodos,
    fetchTodayTodos,
    fetchWeekTodos,
    fetchHighPriorityTodos,
    fetchTodo,
    createTodo,
    updateTodo,
    deleteTodo,
    updateStatus,
    sendEmail,
    batchUpdateStatus,
    searchTodos,
    filterTodos,
    cleanup,

    // 基础Store方法
    findById: baseStore.findById,
    clearCache: baseStore.clearCache,
    reset: baseStore.reset,
  };
});
