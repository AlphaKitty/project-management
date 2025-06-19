import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { reportApi } from "@/api";
import { useBaseStore } from "./composables/useBaseStore";
import { useGlobalState } from "./composables/useGlobalState";
import type { Report, ReportDTO } from "@/types";

// 报表API适配器
const reportApiAdapter = {
  getList: () => reportApi.getReports(),
  getDetail: (id: number) => reportApi.getReport(id),
  create: (data: ReportDTO) => reportApi.generateReport(data),
  update: () => Promise.reject(new Error("报表不支持更新操作")),
  delete: (id: number) => reportApi.deleteReport(id),
};

export const useReportStore = defineStore("reports", () => {
  // 使用基础Store功能
  const baseStore = useBaseStore<Report>(
    "report",
    reportApiAdapter,
    { ttl: 15 * 60 * 1000, maxSize: 30 } // 15分钟缓存，最多30个报表
  );

  // 全局状态
  const globalState = useGlobalState();

  // 扩展状态
  const currentReport = ref<Report | null>(null);
  const generatingReport = ref(false);

  // 计算属性（基于baseStore的数据）
  const reports = computed(() => baseStore.data.value);
  const reportCount = computed(() => baseStore.count.value);
  const loading = computed(
    () => baseStore.loading.value || generatingReport.value
  );

  const weeklyReports = computed(() =>
    reports.value.filter((r) => r.type === "WEEKLY")
  );

  const monthlyReports = computed(() =>
    reports.value.filter((r) => r.type === "MONTHLY")
  );

  const stageReports = computed(() =>
    reports.value.filter((r) => r.type === "STAGE")
  );

  // 按类型分组的报表
  const reportsByType = computed(() => {
    return reports.value.reduce(
      (acc, report) => {
        if (!acc[report.type]) {
          acc[report.type] = [];
        }
        acc[report.type].push(report);
        return acc;
      },
      {} as Record<string, Report[]>
    );
  });

  // 最近的报表
  const recentReports = computed(() => {
    return reports.value
      .slice()
      .sort(
        (a, b) =>
          new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      )
      .slice(0, 10);
  });

  // 获取报告列表（增强版）
  const fetchReports = async (force = false) => {
    try {
      const result = await baseStore.fetchList(force);

      // 按创建时间排序（最新的在前）
      result.sort(
        (a, b) =>
          new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
      );

      // 添加同步事件
      globalState.addSyncEvent({
        type: "update",
        entity: "reports",
      });

      return result;
    } catch (error) {
      console.error("获取报告列表失败:", error);
      throw error;
    }
  };

  // 获取报告详情（增强版）
  const fetchReport = async (id: number, useCache = true) => {
    try {
      const result = await baseStore.fetchDetail(id, useCache);
      currentReport.value = result;
      return result;
    } catch (error) {
      console.error("获取报告详情失败:", error);
      throw error;
    }
  };

  // 生成报告（增强版）
  const generateReport = async (data: ReportDTO) => {
    try {
      generatingReport.value = true;

      const response = await reportApi.generateReport(data);

      // 新报告添加到前面
      reports.value.unshift(response.data);

      // 清除列表缓存
      baseStore.setCachedData("list", null);

      // 添加同步事件
      globalState.addSyncEvent({
        type: "create",
        entity: "reports",
        data: response.data,
      });

      return response.data;
    } catch (error) {
      console.error("生成报告失败:", error);
      throw error;
    } finally {
      generatingReport.value = false;
    }
  };

  // 删除报告（增强版）
  const deleteReport = async (id: number) => {
    try {
      await baseStore.remove(id);

      // 清理相关状态
      if (currentReport.value?.id === id) {
        currentReport.value = null;
      }

      // 添加同步事件
      globalState.addSyncEvent({
        type: "delete",
        entity: "reports",
        id,
      });
    } catch (error) {
      console.error("删除报告失败:", error);
      throw error;
    }
  };

  // 批量删除报告
  const batchDeleteReports = async (ids: number[]) => {
    const operations = ids.map((id) => deleteReport(id));

    try {
      await Promise.all(operations);
    } catch (error) {
      console.error("批量删除报告失败:", error);
      // 重新获取数据以确保一致性
      await fetchReports(true);
      throw error;
    }
  };

  // 按类型获取报告
  const getReportsByType = (type: string) => {
    return reports.value.filter((report) => report.type === type);
  };

  // 按日期范围获取报告
  const getReportsByDateRange = (startDate: string, endDate: string) => {
    const start = new Date(startDate);
    const end = new Date(endDate);

    return reports.value.filter((report) => {
      const reportDate = new Date(report.createTime);
      return reportDate >= start && reportDate <= end;
    });
  };

  // 搜索报告
  const searchReports = (keyword: string) => {
    if (!keyword.trim()) return reports.value;

    const lowerKeyword = keyword.toLowerCase();
    return reports.value.filter(
      (report) =>
        report.title?.toLowerCase().includes(lowerKeyword) ||
        report.content?.toLowerCase().includes(lowerKeyword)
    );
  };

  // 获取报告统计信息
  const getReportStats = computed(() => {
    const now = new Date();
    const thisMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const thisWeek = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);

    const monthlyCount = reports.value.filter(
      (r) => new Date(r.createTime) >= thisMonth
    ).length;

    const weeklyCount = reports.value.filter(
      (r) => new Date(r.createTime) >= thisWeek
    ).length;

    return {
      total: reportCount.value,
      weekly: weeklyReports.value.length,
      monthly: monthlyReports.value.length,
      stage: stageReports.value.length,
      thisMonth: monthlyCount,
      thisWeek: weeklyCount,
    };
  });

  // 清理方法
  const cleanup = () => {
    baseStore.reset();
    currentReport.value = null;
    generatingReport.value = false;
  };

  return {
    // 状态
    reports,
    loading,
    currentReport,
    generatingReport,

    // 计算属性
    reportCount,
    weeklyReports,
    monthlyReports,
    stageReports,
    reportsByType,
    recentReports,
    getReportStats,

    // 方法
    fetchReports,
    fetchReport,
    generateReport,
    deleteReport,
    batchDeleteReports,
    getReportsByType,
    getReportsByDateRange,
    searchReports,
    cleanup,

    // 基础Store方法
    findById: baseStore.findById,
    clearCache: baseStore.clearCache,
    reset: baseStore.reset,
  };
});
