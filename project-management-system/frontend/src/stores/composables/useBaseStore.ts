import { ref, computed, type Ref } from "vue";
import { Message } from "@arco-design/web-vue";

// 基础状态管理接口
export interface BaseState<T> {
  data: T[];
  loading: boolean;
  error: string | null;
  lastFetch: number | null;
  cache: Map<string, { data: any; timestamp: number }>;
}

// 缓存配置
export interface CacheConfig {
  ttl: number; // 缓存时间（毫秒）
  maxSize: number; // 最大缓存数量
}

// 基础Store工具类
export function useBaseStore<T extends { id: number }>(
  name: string,
  apiService: any,
  cacheConfig: CacheConfig = { ttl: 5 * 60 * 1000, maxSize: 100 }
) {
  // 基础状态
  const data = ref<T[]>([]) as Ref<T[]>;
  const loading = ref(false);
  const error = ref<string | null>(null);
  const lastFetch = ref<number | null>(null);
  const cache = ref(new Map<string, { data: any; timestamp: number }>());

  // 计算属性
  const count = computed(() => data.value.length);
  const hasData = computed(() => data.value.length > 0);
  const isEmpty = computed(() => data.value.length === 0);

  // 缓存管理
  const clearExpiredCache = () => {
    const now = Date.now();
    for (const [key, value] of cache.value.entries()) {
      if (now - value.timestamp > cacheConfig.ttl) {
        cache.value.delete(key);
      }
    }
  };

  const getCachedData = (key: string) => {
    clearExpiredCache();
    const cached = cache.value.get(key);
    if (cached && Date.now() - cached.timestamp < cacheConfig.ttl) {
      return cached.data;
    }
    return null;
  };

  const setCachedData = (key: string, data: any) => {
    // 限制缓存大小
    if (cache.value.size >= cacheConfig.maxSize) {
      const firstKey = cache.value.keys().next().value;
      if (firstKey) {
        cache.value.delete(firstKey);
      }
    }
    cache.value.set(key, { data, timestamp: Date.now() });
  };

  // 错误处理
  const handleError = (err: any, operation: string) => {
    const message = err.message || `${operation}失败`;
    error.value = message;
    console.error(`[${name}Store] ${operation}错误:`, err);

    // 显示用户友好的错误信息
    Message.error({
      content: message,
      duration: 3000,
    });

    throw err;
  };

  // 通用获取列表方法
  const fetchList = async (force = false, useCache = true) => {
    const cacheKey = "list";

    // 检查缓存
    if (!force && useCache) {
      const cached = getCachedData(cacheKey);
      if (cached) {
        data.value = cached;
        return cached;
      }
    }

    try {
      loading.value = true;
      error.value = null;

      const response = await apiService.getList();
      data.value = response.data;
      lastFetch.value = Date.now();

      // 更新缓存
      if (useCache) {
        setCachedData(cacheKey, response.data);
      }

      return response.data;
    } catch (err) {
      handleError(err, "获取列表");
    } finally {
      loading.value = false;
    }
  };

  // 通用获取详情方法
  const fetchDetail = async (id: number, useCache = true) => {
    const cacheKey = `detail_${id}`;

    // 检查缓存
    if (useCache) {
      const cached = getCachedData(cacheKey);
      if (cached) {
        return cached;
      }
    }

    try {
      loading.value = true;
      error.value = null;

      const response = await apiService.getDetail(id);

      // 更新缓存
      if (useCache) {
        setCachedData(cacheKey, response.data);
      }

      return response.data;
    } catch (err) {
      handleError(err, "获取详情");
    } finally {
      loading.value = false;
    }
  };

  // 通用创建方法
  const create = async (payload: any) => {
    try {
      loading.value = true;
      error.value = null;

      const response = await apiService.create(payload);

      // 更新本地数据
      data.value.unshift(response.data);

      // 清除相关缓存
      cache.value.delete("list");

      Message.success("创建成功");
      return response.data;
    } catch (err) {
      handleError(err, "创建");
    } finally {
      loading.value = false;
    }
  };

  // 通用更新方法
  const update = async (id: number, payload: any) => {
    try {
      loading.value = true;
      error.value = null;

      const response = await apiService.update(id, payload);

      // 更新本地数据
      const index = data.value.findIndex((item) => item.id === id);
      if (index !== -1) {
        data.value[index] = response.data;
      }

      // 清除相关缓存
      cache.value.delete("list");
      cache.value.delete(`detail_${id}`);

      Message.success("更新成功");
      return response.data;
    } catch (err) {
      handleError(err, "更新");
    } finally {
      loading.value = false;
    }
  };

  // 通用删除方法
  const remove = async (id: number) => {
    try {
      loading.value = true;
      error.value = null;

      await apiService.delete(id);

      // 更新本地数据
      data.value = data.value.filter((item) => item.id !== id);

      // 清除相关缓存
      cache.value.delete("list");
      cache.value.delete(`detail_${id}`);

      Message.success("删除成功");
    } catch (err) {
      handleError(err, "删除");
    } finally {
      loading.value = false;
    }
  };

  // 根据ID查找项目
  const findById = (id: number) => {
    return data.value.find((item) => item.id === id) || null;
  };

  // 清除所有缓存
  const clearCache = () => {
    cache.value.clear();
  };

  // 重置状态
  const reset = () => {
    data.value = [];
    loading.value = false;
    error.value = null;
    lastFetch.value = null;
    clearCache();
  };

  return {
    // 状态
    data,
    loading,
    error,
    lastFetch,

    // 计算属性
    count,
    hasData,
    isEmpty,

    // 方法
    fetchList,
    fetchDetail,
    create,
    update,
    remove,
    findById,
    clearCache,
    reset,

    // 缓存方法
    getCachedData,
    setCachedData,
  };
}
