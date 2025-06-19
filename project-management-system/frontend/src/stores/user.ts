import { defineStore } from "pinia";
import { ref, computed, watch } from "vue";
import { userApi } from "@/api";
import { useGlobalState } from "./composables/useGlobalState";
import type { User } from "@/types";

export const useUserStore = defineStore("user", () => {
  // 全局状态
  const globalState = useGlobalState();

  // 状态
  const users = ref<User[]>([]);
  const dashboardUsers = ref<any[]>([]);
  const currentUser = ref<User | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 缓存相关状态
  const isUsersLoaded = ref(false);
  const isDashboardUsersLoaded = ref(false);
  const lastUsersFetch = ref<number | null>(null);
  const lastDashboardFetch = ref<number | null>(null);

  // 认证状态
  const token = ref<string | null>(localStorage.getItem("auth-token"));
  const loginAttempts = ref(0);
  const maxLoginAttempts = 5;
  const lockoutTime = ref<number | null>(null);

  // 计算属性
  const userCount = computed(() => users.value.length);
  const isLoggedIn = computed(
    () => currentUser.value !== null && token.value !== null
  );
  const isAdmin = computed(
    () => currentUser.value?.username === "barlin.zhang"
  );
  const isLocked = computed(() => {
    if (!lockoutTime.value) return false;
    return Date.now() < lockoutTime.value;
  });

  // 缓存配置
  const CACHE_TTL = {
    users: 10 * 60 * 1000, // 10分钟
    dashboard: 5 * 60 * 1000, // 5分钟
    currentUser: 15 * 60 * 1000, // 15分钟
  };

  // 错误处理
  const handleError = (err: any, operation: string) => {
    const message = err.message || `${operation}失败`;
    error.value = message;
    console.error(`[UserStore] ${operation}错误:`, err);
    throw err;
  };

  // 检查缓存是否有效
  const isCacheValid = (lastFetch: number | null, ttl: number) => {
    if (!lastFetch) return false;
    return Date.now() - lastFetch < ttl;
  };

  // 获取用户列表（智能缓存）
  const fetchUsers = async (force = false) => {
    // 检查缓存
    if (
      !force &&
      isUsersLoaded.value &&
      isCacheValid(lastUsersFetch.value, CACHE_TTL.users)
    ) {
      return users.value;
    }

    try {
      loading.value = true;
      error.value = null;

      const response = await userApi.getUsers();
      users.value = response.data;
      isUsersLoaded.value = true;
      lastUsersFetch.value = Date.now();

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "users",
      });

      return response.data;
    } catch (err) {
      handleError(err, "获取用户列表");
    } finally {
      loading.value = false;
    }
  };

  // 获取数据看板用户数据（性能优化版）
  const fetchDashboardUsers = async (force = false) => {
    // 检查缓存
    if (
      !force &&
      isDashboardUsersLoaded.value &&
      isCacheValid(lastDashboardFetch.value, CACHE_TTL.dashboard)
    ) {
      return dashboardUsers.value;
    }

    try {
      loading.value = true;
      error.value = null;

      const response = await userApi.getDashboardUsers();
      dashboardUsers.value = response.data;
      isDashboardUsersLoaded.value = true;
      lastDashboardFetch.value = Date.now();

      return response.data;
    } catch (err) {
      handleError(err, "获取数据看板用户数据");
    } finally {
      loading.value = false;
    }
  };

  // 搜索用户（防抖+缓存）
  let searchTimeout: NodeJS.Timeout;
  const searchCache = new Map<string, { data: User[]; timestamp: number }>();

  const searchUsers = async (keyword?: string) => {
    // 清除之前的搜索
    if (searchTimeout) {
      clearTimeout(searchTimeout);
    }

    return new Promise<User[]>((resolve, reject) => {
      searchTimeout = setTimeout(async () => {
        if (!keyword || keyword.trim().length < 2) {
          resolve([]);
          return;
        }

        const trimmedKeyword = keyword.trim().toLowerCase();

        // 检查搜索缓存
        const cached = searchCache.get(trimmedKeyword);
        if (cached && isCacheValid(cached.timestamp, 2 * 60 * 1000)) {
          // 2分钟缓存
          resolve(cached.data);
          return;
        }

        try {
          loading.value = true;
          error.value = null;

          const response = await userApi.searchUsers(trimmedKeyword);

          // 更新搜索缓存
          searchCache.set(trimmedKeyword, {
            data: response.data,
            timestamp: Date.now(),
          });

          // 限制缓存大小
          if (searchCache.size > 50) {
            const firstKey = searchCache.keys().next().value;
            if (firstKey) {
              searchCache.delete(firstKey);
            }
          }

          resolve(response.data);
        } catch (err) {
          handleError(err, "搜索用户");
          reject(err);
        } finally {
          loading.value = false;
        }
      }, 300); // 300ms防抖
    });
  };

  // 获取当前用户信息
  const fetchCurrentUser = async (force = false) => {
    // 检查本地存储的用户信息
    if (!force && currentUser.value) {
      return currentUser.value;
    }

    try {
      loading.value = true;
      error.value = null;

      const response = await userApi.getCurrentUser();
      currentUser.value = response.data;

      // 更新本地存储
      localStorage.setItem("current-user", JSON.stringify(response.data));

      return response.data;
    } catch (err) {
      handleError(err, "获取当前用户信息");
    } finally {
      loading.value = false;
    }
  };

  // 设置当前用户
  const setCurrentUser = (user: User) => {
    currentUser.value = user;
    localStorage.setItem("current-user", JSON.stringify(user));
  };

  // 用户登录（增强版）
  const login = async (username: string, password: string) => {
    // 检查锁定状态
    if (isLocked.value) {
      const remainingTime = Math.ceil(
        (lockoutTime.value! - Date.now()) / 1000 / 60
      );
      return {
        success: false,
        message: `账户已锁定，请 ${remainingTime} 分钟后重试`,
      };
    }

    try {
      loading.value = true;
      error.value = null;

      // 客户端验证
      if (password !== "0000") {
        loginAttempts.value++;

        // 检查是否需要锁定
        if (loginAttempts.value >= maxLoginAttempts) {
          lockoutTime.value = Date.now() + 15 * 60 * 1000; // 锁定15分钟
          return {
            success: false,
            message: `密码连续错误次数过多，账户已锁定15分钟`,
          };
        }

        return {
          success: false,
          message: `密码错误，还可尝试 ${maxLoginAttempts - loginAttempts.value} 次`,
        };
      }

      // 调用登录API
      const response = await userApi.login(username, password);

      if (response.data.success) {
        // 重置登录尝试次数
        loginAttempts.value = 0;
        lockoutTime.value = null;

        // 保存用户信息和令牌
        currentUser.value = response.data.user;
        token.value = response.data.token;

        // 保存到本地存储
        localStorage.setItem("auth-token", response.data.token);
        localStorage.setItem(
          "current-user",
          JSON.stringify(response.data.user)
        );

        // 添加同步事件
        globalState.addSyncEvent({
          type: "update",
          entity: "auth",
          data: { action: "login", user: response.data.user },
        });

        return { success: true, user: response.data.user };
      } else {
        loginAttempts.value++;
        return { success: false, message: response.data.message || "登录失败" };
      }
    } catch (error: any) {
      console.error("登录失败:", error);
      loginAttempts.value++;

      if (error.response?.status === 404) {
        return { success: false, message: "用户不存在" };
      }

      return { success: false, message: error.message || "登录失败" };
    } finally {
      loading.value = false;
    }
  };

  // 登出（增强版）
  const logout = () => {
    // 清除状态
    currentUser.value = null;
    token.value = null;
    loginAttempts.value = 0;
    lockoutTime.value = null;

    // 清除本地存储
    localStorage.removeItem("auth-token");
    localStorage.removeItem("current-user");

    // 清除缓存
    clearAllCache();

    // 添加同步事件
    globalState.addSyncEvent({
      type: "update",
      entity: "auth",
      data: { action: "logout" },
    });
  };

  // 从本地存储恢复用户状态
  const initializeFromStorage = () => {
    const storedUser = localStorage.getItem("current-user");
    const storedToken = localStorage.getItem("auth-token");

    if (storedUser && storedToken) {
      try {
        const parsedUser = JSON.parse(storedUser);
        currentUser.value = parsedUser;
        token.value = storedToken;
      } catch (error) {
        console.error("恢复用户状态失败:", error);
        logout(); // 清除无效数据
      }
    }
  };

  // 根据ID查找用户（优化查找逻辑）
  const getUserById = (id: number) => {
    // 优先从dashboard用户中查找（通常数据更新）
    let user = dashboardUsers.value.find((user) => user.id === id);
    if (user) return user;

    // 再从完整用户列表中查找
    user = users.value.find((user) => user.id === id);
    if (user) return user;

    return null;
  };

  // 批量获取用户信息
  const getUsersByIds = (ids: number[]) => {
    const result: User[] = [];
    const notFound: number[] = [];

    ids.forEach((id) => {
      const user = getUserById(id);
      if (user) {
        result.push(user);
      } else {
        notFound.push(id);
      }
    });

    return { found: result, notFound };
  };

  // 清除所有缓存
  const clearAllCache = () => {
    isUsersLoaded.value = false;
    isDashboardUsersLoaded.value = false;
    lastUsersFetch.value = null;
    lastDashboardFetch.value = null;
    searchCache.clear();
  };

  // 重置状态
  const reset = () => {
    users.value = [];
    dashboardUsers.value = [];
    currentUser.value = null;
    loading.value = false;
    error.value = null;
    clearAllCache();
  };

  // 监听网络状态，在重新连接时更新数据
  watch(
    () => globalState.isOnline,
    (online) => {
      if (online && currentUser.value) {
        // 重新连接时刷新关键数据
        fetchCurrentUser(true);
        if (isUsersLoaded.value) {
          fetchUsers(true);
        }
      }
    }
  );

  return {
    // 状态
    users,
    dashboardUsers,
    currentUser,
    loading,
    error,
    isUsersLoaded,
    isDashboardUsersLoaded,

    // 计算属性
    userCount,
    isLoggedIn,
    isAdmin,
    isLocked,

    // 方法
    fetchUsers,
    fetchDashboardUsers,
    searchUsers,
    fetchCurrentUser,
    setCurrentUser,
    login,
    logout,
    initializeFromStorage,
    getUserById,
    getUsersByIds,
    clearAllCache,
    reset,
  };
});
