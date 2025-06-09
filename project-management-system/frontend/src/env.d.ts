/// <reference types="vite/client" />

declare module "*.vue" {
  import type { DefineComponent } from "vue";
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

// 编译时常量声明
declare const __DEV__: boolean;
declare const __ENABLE_DEV_TOOLS__: boolean;

// stagewise 工具栏类型声明 - 仅开发环境
declare module "@stagewise/toolbar-vue" {
  import type { DefineComponent } from "vue";

  export interface StagewiseConfig {
    plugins: any[];
  }

  export const StagewiseToolbar: DefineComponent<{
    config: StagewiseConfig;
  }>;
}

// 环境变量类型扩展
interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string;
  readonly DEV: boolean;
  readonly PROD: boolean;
}
