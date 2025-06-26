import { createApp } from "vue";
import ArcoVue from "@arco-design/web-vue";
import "@arco-design/web-vue/dist/arco.css";

import App from "./App.vue";
import router from "./router";
import pinia, { initializeStores } from "./stores";
import { useThemeStore } from "./stores/theme";

// 导入主题样式
import "./styles/theme.css";
import "./styles/theme-extended.css";

// 创建应用实例
const app = createApp(App);

// 使用插件
app.use(pinia);
app.use(router);
app.use(ArcoVue);

// 初始化状态管理
initializeStores();

// 初始化主题
const themeStore = useThemeStore();
themeStore.initTheme();

// 挂载应用
app.mount("#app");
