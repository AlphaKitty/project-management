import axios from "axios";

// API响应数据结构
export interface ApiResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

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
          message = "未授权，请登录";
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
