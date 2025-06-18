import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { userApi } from "@/api";
import type { User } from "@/types";

export const useUserStore = defineStore("user", () => {
  // 状态
  const users = ref<User[]>([]);
  const dashboardUsers = ref<any[]>([]); // 数据看板专用用户数据
  const currentUser = ref<User | null>(null);
  const loading = ref(false);
  const isLoaded = ref(false); // 是否已加载过数据
  const token = ref<string | null>(localStorage.getItem("auth-token")); // 登录令牌

  // 计算属性
  const userCount = computed(() => users.value.length);
  const isLoggedIn = computed(
    () => currentUser.value !== null && token.value !== null
  );
  const isAdmin = computed(
    () => currentUser.value?.username === "barlin.zhang"
  );

  // 获取用户列表（懒加载）
  const fetchUsers = async (force = false) => {
    // 如果已经加载过且不强制刷新，直接返回
    if (isLoaded.value && !force) {
      return users.value;
    }

    try {
      loading.value = true;
      const response = await userApi.getUsers();
      users.value = response.data;
      isLoaded.value = true;
      return response.data;
    } catch (error) {
      console.error("获取用户列表失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取数据看板用户数据（性能优化版）
  const fetchDashboardUsers = async (force = false) => {
    // 如果已经加载过且不强制刷新，直接返回
    if (dashboardUsers.value.length > 0 && !force) {
      return dashboardUsers.value;
    }

    try {
      loading.value = true;
      const response = await userApi.getDashboardUsers();
      dashboardUsers.value = response.data;
      return response.data;
    } catch (error) {
      console.error("获取数据看板用户数据失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 搜索用户（限制最小搜索字符数，避免频繁请求）
  const searchUsers = async (keyword?: string) => {
    // 如果搜索关键字为空或长度小于2，返回空数组
    if (!keyword || keyword.trim().length < 2) {
      return [];
    }

    try {
      loading.value = true;
      const response = await userApi.searchUsers(keyword.trim());
      return response.data;
    } catch (error) {
      console.error("搜索用户失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取当前用户信息
  const fetchCurrentUser = async () => {
    try {
      loading.value = true;
      const response = await userApi.getCurrentUser();
      currentUser.value = response.data;
      return response.data;
    } catch (error) {
      console.error("获取当前用户信息失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 设置当前用户（模拟登录）
  const setCurrentUser = (user: User) => {
    currentUser.value = user;
  };

  // 登出
  const logout = () => {
    currentUser.value = null;
    token.value = null;

    // 清除本地存储
    localStorage.removeItem("auth-token");
    localStorage.removeItem("current-user");
  };

  // 从本地存储恢复用户状态
  const initializeFromStorage = () => {
    const storedUser = localStorage.getItem("current-user");
    const storedToken = localStorage.getItem("auth-token");

    if (storedUser && storedToken) {
      try {
        currentUser.value = JSON.parse(storedUser);
        token.value = storedToken;
      } catch (error) {
        console.error("恢复用户状态失败:", error);
        logout(); // 清除无效数据
      }
    }
  };

  // 根据ID查找用户
  const getUserById = (id: number) => {
    // 先从数据看板用户中查找
    const dashboardUser = dashboardUsers.value.find((user) => user.id === id);
    if (dashboardUser) {
      return dashboardUser;
    }

    // 再从完整用户列表中查找
    return users.value.find((user) => user.id === id);
  };

  // 用户登录
  const login = async (username: string, password: string) => {
    try {
      loading.value = true;

      // 验证密码是否为0000
      if (password !== "0000") {
        return { success: false, message: "密码错误" };
      }

      // 调用登录API
      const response = await userApi.login(username, password);

      if (response.data.success) {
        // 保存用户信息和令牌
        currentUser.value = response.data.user;
        token.value = response.data.token;

        // 保存到本地存储
        localStorage.setItem("auth-token", response.data.token);
        localStorage.setItem(
          "current-user",
          JSON.stringify(response.data.user)
        );

        return { success: true, user: response.data.user };
      } else {
        return { success: false, message: response.data.message || "登录失败" };
      }
    } catch (error: any) {
      console.error("登录失败:", error);

      // 如果是用户不存在的错误，返回特定消息
      if (error.response?.status === 404) {
        return { success: false, message: "用户不存在" };
      }

      return { success: false, message: error.message || "登录失败" };
    } finally {
      loading.value = false;
    }
  };

  return {
    // 状态
    users,
    dashboardUsers,
    currentUser,
    loading,
    isLoaded,

    // 计算属性
    userCount,
    isLoggedIn,
    isAdmin,

    // 方法
    fetchUsers,
    fetchDashboardUsers, // 新增：数据看板专用
    searchUsers,
    fetchCurrentUser,
    setCurrentUser,
    logout,
    initializeFromStorage,
    getUserById,
    login,
  };
});
