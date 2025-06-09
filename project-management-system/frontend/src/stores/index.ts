import { createPinia } from "pinia";

const pinia = createPinia();

export default pinia;

// 导出所有stores
export * from "./projects";
export * from "./todos";
export * from "./reports";
export * from "./user";
