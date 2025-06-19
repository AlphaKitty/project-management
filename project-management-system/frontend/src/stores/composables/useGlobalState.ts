import { ref, computed, watch } from "vue";
import { defineStore } from "pinia";

// 全局状态类型
interface GlobalState {
  isOnline: boolean;
  lastActivity: number;
  syncStatus: "idle" | "syncing" | "error";
  pendingOperations: Map<string, any>;
}

// 数据同步事件类型
export interface SyncEvent {
  type: "create" | "update" | "delete";
  entity: string;
  id?: number;
  data?: any;
}

export const useGlobalState = defineStore("global", () => {
  // 基础状态
  const isOnline = ref(navigator.onLine);
  const lastActivity = ref(Date.now());
  const syncStatus = ref<"idle" | "syncing" | "error">("idle");
  const pendingOperations = ref(new Map<string, any>());

  // 同步事件队列
  const syncEvents = ref<SyncEvent[]>([]);
  const maxSyncEvents = 100;

  // 计算属性
  const hasPendingOperations = computed(() => pendingOperations.value.size > 0);
  const canSync = computed(
    () => isOnline.value && syncStatus.value !== "syncing"
  );

  // 网络状态监听
  const setupNetworkListeners = () => {
    window.addEventListener("online", () => {
      isOnline.value = true;
      processPendingOperations();
    });

    window.addEventListener("offline", () => {
      isOnline.value = false;
    });
  };

  // 更新活动时间
  const updateActivity = () => {
    lastActivity.value = Date.now();
  };

  // 添加同步事件
  const addSyncEvent = (event: SyncEvent) => {
    syncEvents.value.push(event);

    // 限制事件队列大小
    if (syncEvents.value.length > maxSyncEvents) {
      syncEvents.value = syncEvents.value.slice(-maxSyncEvents);
    }

    // 如果在线，立即处理
    if (canSync.value) {
      processSyncEvents();
    }
  };

  // 处理同步事件
  const processSyncEvents = async () => {
    if (!canSync.value || syncEvents.value.length === 0) return;

    syncStatus.value = "syncing";

    try {
      // 这里可以实现具体的同步逻辑
      // 比如批量处理事件，或者通知相关的Store更新数据

      // 模拟同步处理
      await new Promise((resolve) => setTimeout(resolve, 100));

      // 清空已处理的事件
      syncEvents.value = [];
      syncStatus.value = "idle";
    } catch (error) {
      console.error("同步失败:", error);
      syncStatus.value = "error";
    }
  };

  // 添加待处理操作
  const addPendingOperation = (key: string, operation: any) => {
    pendingOperations.value.set(key, operation);
  };

  // 移除待处理操作
  const removePendingOperation = (key: string) => {
    pendingOperations.value.delete(key);
  };

  // 处理待处理操作
  const processPendingOperations = async () => {
    if (!canSync.value || !hasPendingOperations.value) return;

    syncStatus.value = "syncing";

    try {
      for (const [key, operation] of pendingOperations.value.entries()) {
        try {
          await operation();
          pendingOperations.value.delete(key);
        } catch (error) {
          console.error(`处理待处理操作失败 ${key}:`, error);
        }
      }
      syncStatus.value = "idle";
    } catch (error) {
      console.error("处理待处理操作失败:", error);
      syncStatus.value = "error";
    }
  };

  // 清除所有状态
  const clear = () => {
    syncEvents.value = [];
    pendingOperations.value.clear();
    syncStatus.value = "idle";
  };

  // 初始化
  const initialize = () => {
    setupNetworkListeners();
    updateActivity();

    // 定期清理过期事件
    setInterval(
      () => {
        const now = Date.now();
        const expiredTime = 24 * 60 * 60 * 1000; // 24小时

        syncEvents.value = syncEvents.value.filter((event) => {
          return (now - (event as any).timestamp || 0) < expiredTime;
        });
      },
      60 * 60 * 1000
    ); // 每小时清理一次
  };

  return {
    // 状态
    isOnline,
    lastActivity,
    syncStatus,
    pendingOperations,
    syncEvents,

    // 计算属性
    hasPendingOperations,
    canSync,

    // 方法
    updateActivity,
    addSyncEvent,
    processSyncEvents,
    addPendingOperation,
    removePendingOperation,
    processPendingOperations,
    clear,
    initialize,
  };
});

// 监听用户活动
export const setupActivityTracking = () => {
  const globalState = useGlobalState();

  const events = ["mousedown", "mousemove", "keypress", "scroll", "touchstart"];
  const throttle = 5000; // 5秒节流
  let lastUpdate = 0;

  const handleActivity = () => {
    const now = Date.now();
    if (now - lastUpdate > throttle) {
      globalState.updateActivity();
      lastUpdate = now;
    }
  };

  events.forEach((event) => {
    document.addEventListener(event, handleActivity, true);
  });
};
