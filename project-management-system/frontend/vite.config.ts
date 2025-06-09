import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { resolve } from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const isDev = mode === "development";

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        "@": resolve(__dirname, "src"),
      },
    },
    server: {
      port: 3000,
      open: true,
      proxy: {
        "/api": {
          target: "http://localhost:8080",
          // target: "http://10.10.119.186:8080",
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, "/api"),
        },
      },
    },
    build: {
      outDir: "dist",
      sourcemap: false,
      chunkSizeWarningLimit: 1600,
      rollupOptions: {
        external: (id) => {
          // 生产环境完全排除 stagewise 相关模块
          if (!isDev && id.includes("@stagewise")) {
            return true;
          }
          return false;
        },
      },
    },
    define: {
      // 定义编译时常量
      __DEV__: JSON.stringify(isDev),
      __ENABLE_DEV_TOOLS__: JSON.stringify(isDev),
    },
  };
});
