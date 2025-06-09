import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { todoApi } from "@/api";
import type { Todo, TodoDTO } from "@/types";

export const useTodoStore = defineStore("todos", () => {
  // 状态
  const todos = ref<Todo[]>([]);
  const todayTodos = ref<Todo[]>([]);
  const weekTodos = ref<Todo[]>([]);
  const highTodos = ref<Todo[]>([]);
  const loading = ref(false);
  const currentTodo = ref<Todo | null>(null);

  // 计算属性
  const todoCount = computed(() => todos.value.length);
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

  // 获取待办任务列表
  const fetchTodos = async () => {
    try {
      loading.value = true;
      const response = await todoApi.getTodos();
      todos.value = response.data;
    } catch (error) {
      console.error("获取待办任务列表失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取今日待办任务
  const fetchTodayTodos = async () => {
    try {
      loading.value = true;
      const response = await todoApi.getTodayTodos();
      todayTodos.value = response.data;
    } catch (error) {
      console.error("获取今日待办任务失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取本周待办任务
  const fetchWeekTodos = async () => {
    try {
      loading.value = true;
      const response = await todoApi.getWeekTodos();
      weekTodos.value = response.data;
    } catch (error) {
      console.error("获取本周待办任务失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取本周待办任务
  const fetchHighPriorityTodos = async () => {
    try {
      loading.value = true;
      const response = await todoApi.getHighPriorityTodos();
      highTodos.value = response.data;
    } catch (error) {
      console.error("获取高优先级待办任务失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取待办任务详情
  const fetchTodo = async (id: number) => {
    try {
      loading.value = true;
      const response = await todoApi.getTodo(id);
      currentTodo.value = response.data;
      return response.data;
    } catch (error) {
      console.error("获取待办任务详情失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 创建待办任务
  const createTodo = async (data: TodoDTO) => {
    try {
      loading.value = true;
      const response = await todoApi.createTodo(data);
      todos.value.push(response.data);
      return response.data;
    } catch (error) {
      console.error("创建待办任务失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 更新待办任务
  const updateTodo = async (id: number, data: TodoDTO) => {
    try {
      loading.value = true;
      const response = await todoApi.updateTodo(id, data);
      const index = todos.value.findIndex((t) => t.id === id);
      if (index !== -1) {
        todos.value[index] = response.data;
      }
      if (currentTodo.value?.id === id) {
        currentTodo.value = response.data;
      }
      return response.data;
    } catch (error) {
      console.error("更新待办任务失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 删除待办任务
  const deleteTodo = async (id: number) => {
    try {
      loading.value = true;
      await todoApi.deleteTodo(id);
      todos.value = todos.value.filter((t) => t.id !== id);
      todayTodos.value = todayTodos.value.filter((t) => t.id !== id);
      weekTodos.value = weekTodos.value.filter((t) => t.id !== id);
      if (currentTodo.value?.id === id) {
        currentTodo.value = null;
      }
    } catch (error) {
      console.error("删除待办任务失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 更新任务状态
  const updateStatus = async (id: number, status: string) => {
    try {
      await todoApi.updateStatus(id, status);
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
    } catch (error) {
      console.error("更新任务状态失败:", error);
      throw error;
    }
  };

  // 发送邮件
  const sendEmail = async (email: string, userId?: number) => {
    try {
      loading.value = true;
      const response = await todoApi.sendEmail(email, userId);
      return response;
    } catch (error) {
      console.error("发送邮件失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  return {
    // 状态
    todos,
    todayTodos,
    weekTodos,
    loading,
    currentTodo,

    // 计算属性
    todoCount,
    pendingTodos,
    completedTodos,
    inProgressTodos,
    highPriorityTodos,
    overdueTodos,

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
  };
});
