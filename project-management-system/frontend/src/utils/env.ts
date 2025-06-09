/**
 * 环境检查工具
 */

// 检查是否为开发环境
export const isDevelopment = import.meta.env.DEV;

// 检查是否为生产环境
export const isProduction = import.meta.env.PROD;

// 检查是否启用开发工具
export const isDevToolsEnabled = isDevelopment;

// 动态导入开发工具的辅助函数
export async function loadDevTool<T = any>(
  moduleName: string
): Promise<T | null> {
  if (!isDevToolsEnabled) {
    return null;
  }

  try {
    const module = await import(/* @vite-ignore */ moduleName);
    console.log(`🔧 开发工具 ${moduleName} 已加载`);
    return module;
  } catch (error) {
    console.warn(
      `⚠️ 开发工具 ${moduleName} 加载失败:`,
      (error as Error).message
    );
    console.info(`💡 如需使用此开发工具，请安装: npm install ${moduleName}`);
    return null;
  }
}

// 环境信息
export const envInfo = {
  isDevelopment,
  isProduction,
  isDevToolsEnabled,
  nodeEnv: import.meta.env.MODE,
  baseUrl: import.meta.env.BASE_URL,
} as const;
