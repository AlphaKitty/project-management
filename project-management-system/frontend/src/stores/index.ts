import { createPinia } from "pinia";
import {
  useGlobalState,
  setupActivityTracking,
} from "./composables/useGlobalState";

const pinia = createPinia();

// 全局状态清理方法
export const clearAllStoresData = () => {
  console.log("🧹 开始清理所有stores数据...");

  try {
    // 动态导入所有stores并调用其cleanup/reset方法
    // 使用动态import避免循环依赖
    Promise.all([
      import("./projects").then(({ useProjectStore }) => {
        const projectStore = useProjectStore();
        projectStore.cleanup();
        console.log("✅ 项目数据已清理");
      }),
      import("./todos").then(({ useTodoStore }) => {
        const todoStore = useTodoStore();
        todoStore.cleanup();
        console.log("✅ 待办数据已清理");
      }),
      import("./reports").then(({ useReportStore }) => {
        const reportStore = useReportStore();
        reportStore.cleanup();
        console.log("✅ 报告数据已清理");
      }),
    ])
      .then(() => {
        console.log("🎉 所有stores数据清理完成");
      })
      .catch((error) => {
        console.error("❌ 清理stores数据时出错:", error);
      });
  } catch (error) {
    console.error("❌ 动态清理stores失败:", error);
  }
};

// 初始化全局状态
export const initializeStores = () => {
  const globalState = useGlobalState();

  // 初始化全局状态
  globalState.initialize();

  // 设置用户活动追踪
  setupActivityTracking();

  console.log("[Stores] 状态管理初始化完成");
};

export default pinia;

// 导出所有stores
export * from "./projects";
export * from "./todos";
export * from "./reports";
export * from "./user";
export * from "./composables/useGlobalState";
export * from "./composables/useBaseStore";
