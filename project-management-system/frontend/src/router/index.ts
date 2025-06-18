import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordRaw } from "vue-router";
import Layout from "@/components/Layout.vue";
import { useUserStore } from "@/stores/user";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("@/views/Login.vue"),
    meta: {
      title: "登录",
      requiresAuth: false,
    },
  },
  {
    path: "/",
    redirect: "/dashboard",
  },
  {
    path: "/",
    component: Layout,
    meta: {
      requiresAuth: true,
    },
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("@/views/Dashboard.vue"),
        meta: {
          title: "仪表盘",
        },
      },
      {
        path: "projects",
        name: "Projects",
        component: () => import("@/views/Projects.vue"),
        meta: {
          title: "项目管理",
        },
      },
      {
        path: "todos",
        name: "Todos",
        component: () => import("@/views/Todos.vue"),
        meta: {
          title: "待办任务",
        },
      },
      {
        path: "reports",
        name: "Reports",
        component: () => import("@/views/Reports.vue"),
        meta: {
          title: "项目报告",
          showInMenu: false,
        },
      },
      {
        path: "email-rules",
        name: "EmailRules",
        component: () => import("@/views/EmailRules.vue"),
        meta: {
          title: "邮件规则管理",
          showInMenu: false,
        },
      },
      {
        path: "data-dashboard",
        name: "DataDashboard",
        component: () => import("@/views/DataDashboard.vue"),
        meta: {
          title: "数据看板",
          showInMenu: true,
        },
      },
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to, from, next) => {
  const userStore = useUserStore();

  // 恢复用户状态
  userStore.initializeFromStorage();

  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - 项目管理系统`;
  }

  // 检查特殊权限访问（数据看板等）
  if (
    to.meta?.requiresSpecialAccess &&
    userStore.currentUser?.username !== "barlin.zhang"
  ) {
    // 权限不足，跳转到仪表盘并显示错误
    next("/dashboard");
    // 注意：这里我们不能在路由守卫中直接调用Message，需要在组件中处理
    return;
  }

  // 检查是否需要登录
  if (to.meta?.requiresAuth !== false && !userStore.isLoggedIn) {
    // 需要登录但未登录，跳转到登录页面
    next("/login");
  } else if (to.path === "/login" && userStore.isLoggedIn) {
    // 已登录用户访问登录页面，跳转到首页
    next("/dashboard");
  } else {
    next();
  }
});

export default router;
