import { defineStore } from "pinia";
import { ref, computed } from "vue";

export type ThemeMode = "light" | "dark" | "auto";

export const useThemeStore = defineStore("theme", () => {
  // 当前主题模式
  const themeMode = ref<ThemeMode>("auto");

  // 系统是否为深色模式
  const systemIsDark = ref(false);

  // 计算实际应用的主题
  const actualTheme = computed(() => {
    if (themeMode.value === "auto") {
      return systemIsDark.value ? "dark" : "light";
    }
    return themeMode.value;
  });

  // 初始化主题
  const initTheme = () => {
    // 从localStorage获取保存的主题偏好
    const savedTheme = localStorage.getItem("theme-mode") as ThemeMode;
    if (savedTheme && ["light", "dark", "auto"].includes(savedTheme)) {
      themeMode.value = savedTheme;
    }

    // 监听系统主题变化
    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");
    systemIsDark.value = mediaQuery.matches;

    mediaQuery.addEventListener("change", (e) => {
      systemIsDark.value = e.matches;
      applyTheme();
    });

    // 应用主题
    applyTheme();
  };

  // 应用主题到DOM
  const applyTheme = () => {
    const theme = actualTheme.value;
    const html = document.documentElement;

    // 移除现有主题类
    html.classList.remove("light-theme", "dark-theme");

    // 添加新主题类
    html.classList.add(`${theme}-theme`);

    // 为Arco Design设置主题
    if (theme === "dark") {
      document.body.setAttribute("arco-theme", "dark");
    } else {
      document.body.removeAttribute("arco-theme");
    }
  };

  // 切换主题
  const setTheme = (mode: ThemeMode) => {
    themeMode.value = mode;
    localStorage.setItem("theme-mode", mode);
    applyTheme();
  };

  // 循环切换主题
  const toggleTheme = () => {
    const modes: ThemeMode[] = ["light", "dark", "auto"];
    const currentIndex = modes.indexOf(themeMode.value);
    const nextIndex = (currentIndex + 1) % modes.length;
    setTheme(modes[nextIndex]);
  };

  // 获取主题显示文本
  const getThemeLabel = computed(() => {
    const labels = {
      light: "浅色模式",
      dark: "深色模式",
      auto: "跟随系统",
    };
    return labels[themeMode.value];
  });

  // 获取主题图标
  const getThemeIcon = computed(() => {
    const icons = {
      light: "icon-sun",
      dark: "icon-moon",
      auto: "icon-computer",
    };
    return icons[themeMode.value];
  });

  return {
    themeMode,
    actualTheme,
    systemIsDark,
    initTheme,
    setTheme,
    toggleTheme,
    getThemeLabel,
    getThemeIcon,
  };
});
