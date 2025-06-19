import { createPinia } from "pinia";
import {
  useGlobalState,
  setupActivityTracking,
} from "./composables/useGlobalState";

const pinia = createPinia();

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
