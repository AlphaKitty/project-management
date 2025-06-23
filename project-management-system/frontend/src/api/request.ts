import axios from "axios";
import { Message } from "@arco-design/web-vue";

// API响应数据结构
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

// 标志位，防止多次触发登录过期处理
let isHandlingAuth = false;

// 创建axios实例
const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    console.log("请求发送:", config.method?.toUpperCase(), config.url);

    // 添加认证token到请求头
    const token = localStorage.getItem("auth-token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    console.error("请求错误:", error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const { data } = response;

    // 检查业务状态码
    if (data.code === 0) {
      return data;
    } else if (data.code === 401) {
      // 登录过期，清除本地存储并跳转到登录页

      // 防止多次触发
      if (!isHandlingAuth) {
        isHandlingAuth = true;

        // 清理本地存储
        localStorage.removeItem("auth-token");
        localStorage.removeItem("current-user");

        // 清理所有stores数据
        try {
          import("@/stores").then(({ clearAllStoresData }) => {
            clearAllStoresData();
          });
        } catch (error) {
          console.warn("清理stores数据失败:", error);
        }

        // 显示登录过期提示
        Message.error({
          content: "登录过期，请重新登录",
          duration: 2000,
        });

        // 延迟跳转到登录页
        setTimeout(() => {
          window.location.href = "/login";
        }, 200);
      }

      throw new Error("登录过期，请重新登录");
    } else {
      console.error("业务错误:", data.msg);
      throw new Error(data.msg || "请求失败");
    }
  },
  (error) => {
    console.error("响应错误:", error);

    let message = "请求失败";
    if (error.response) {
      switch (error.response.status) {
        case 400:
          message = "请求参数错误";
          break;
        case 401:
          // HTTP 401状态码，也是登录过期
          message = "登录过期，请重新登录";

          // 防止多次触发
          if (!isHandlingAuth) {
            isHandlingAuth = true;

            // 清理本地存储
            localStorage.removeItem("auth-token");
            localStorage.removeItem("current-user");

            // 清理所有stores数据
            try {
              import("@/stores").then(({ clearAllStoresData }) => {
                clearAllStoresData();
              });
            } catch (error) {
              console.warn("清理stores数据失败:", error);
            }

            // 显示登录过期提示
            Message.error({
              content: "登录过期，请重新登录",
              duration: 2000,
            });

            // 延迟跳转到登录页
            setTimeout(() => {
              window.location.href = "/login";
            }, 2000);
          }
          break;
        case 403:
          message = "禁止访问";
          break;
        case 404:
          message = "请求地址不存在";
          break;
        case 500:
          message = "服务器内部错误";
          break;
        default:
          message = `连接错误${error.response.status}`;
      }
    } else if (error.request) {
      message = "网络连接错误";
    }

    throw new Error(message);
  }
);

export default request;
