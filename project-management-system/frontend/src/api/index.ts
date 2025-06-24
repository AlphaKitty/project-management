import request from "./request";

// 项目API
export const projectApi = {
  // 获取项目列表
  getProjects() {
    return request.get("/projects");
  },

  // 获取项目概览
  getProjectOverview() {
    return request.get("/projects/overview");
  },

  // 获取项目详情
  getProject(id: number) {
    return request.get(`/projects/${id}`);
  },

  // 创建项目
  createProject(data: any) {
    return request.post("/projects", data);
  },

  // 更新项目
  updateProject(id: number, data: any) {
    return request.put(`/projects/${id}`, data);
  },

  // 删除项目
  deleteProject(id: number) {
    return request.delete(`/projects/${id}`);
  },

  // 获取用户参与的项目
  getUserProjects(userId: number) {
    return request.get(`/projects/user/${userId}`);
  },

  // 更新项目进度
  updateProgress(id: number, progress: number) {
    return request.put(`/projects/${id}/progress`, null, {
      params: { progress },
    });
  },
};

// 待办任务API
export const todoApi = {
  // 获取待办任务列表
  getTodos() {
    return request.get("/todos");
  },

  // 获取今日待办任务
  getTodayTodos() {
    return request.get("/todos/today");
  },

  // 获取本周待办任务
  getWeekTodos() {
    return request.get("/todos/week");
  },

  // 获取高优先级待办任务
  getHighPriorityTodos() {
    return request.get("/todos/high");
  },

  // 获取用户的待办任务
  getUserTodos(userId: number) {
    return request.get(`/todos/user/${userId}`);
  },

  // 获取待办任务详情
  getTodo(id: number) {
    return request.get(`/todos/${id}`);
  },

  // 创建待办任务
  createTodo(data: any) {
    return request.post("/todos", data);
  },

  // 更新待办任务
  updateTodo(id: number, data: any) {
    return request.put(`/todos/${id}`, data);
  },

  // 删除待办任务
  deleteTodo(id: number) {
    return request.delete(`/todos/${id}`);
  },

  // 更新任务状态
  updateStatus(id: number, status: string) {
    return request.put(`/todos/${id}/status`, null, {
      params: { status },
    });
  },

  // 发送待办任务邮件
  sendEmail(email: string, userId?: number) {
    const params: any = { email };
    if (userId) {
      params.userId = userId;
    }
    return request.post("/todos/send-email", null, { params });
  },
};

// 报告API
export const reportApi = {
  // 获取报告列表
  getReports() {
    return request.get("/reports");
  },

  // 获取报告详情
  getReport(id: number) {
    return request.get(`/reports/${id}`);
  },

  // 生成项目报告
  generateReport(data: any) {
    return request.post("/reports/generate", data);
  },

  // 删除报告
  deleteReport(id: number) {
    return request.delete(`/reports/${id}`);
  },
};

// 用户API
export const userApi = {
  // 用户登录
  login(username: string, password: string) {
    return request.post("/auth/login", { username, password });
  },

  // 用户登出
  logout() {
    return request.post("/auth/logout");
  },

  // 获取用户列表
  getUsers() {
    return request.get("/users");
  },

  // 获取数据看板用户数据（性能优化版）
  // 只返回有任务的活跃用户和必要字段
  getDashboardUsers() {
    return request.get("/users/dashboard");
  },

  // 根据ID获取用户
  getUser(id: number) {
    return request.get(`/users/${id}`);
  },

  // 搜索用户
  searchUsers(keyword?: string) {
    return request.get("/users/search", {
      params: { keyword },
    });
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request.get("/auth/current");
  },
};

export * from "./request";
export * from "./projects";
export * from "./todos";
export * from "./operationLogs";
export * from "./emailRules";
export * from "./workRecommendations";
