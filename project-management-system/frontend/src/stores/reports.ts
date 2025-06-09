import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { reportApi } from "@/api";
import type { Report, ReportDTO } from "@/types";

export const useReportStore = defineStore("reports", () => {
  // 状态
  const reports = ref<Report[]>([]);
  const loading = ref(false);
  const currentReport = ref<Report | null>(null);

  // 计算属性
  const reportCount = computed(() => reports.value.length);
  const weeklyReports = computed(() =>
    reports.value.filter((r) => r.type === "WEEKLY")
  );
  const monthlyReports = computed(() =>
    reports.value.filter((r) => r.type === "MONTHLY")
  );
  const stageReports = computed(() =>
    reports.value.filter((r) => r.type === "STAGE")
  );

  // 获取报告列表
  const fetchReports = async () => {
    try {
      loading.value = true;
      const response = await reportApi.getReports();
      reports.value = response.data;
    } catch (error) {
      console.error("获取报告列表失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 获取报告详情
  const fetchReport = async (id: number) => {
    try {
      loading.value = true;
      const response = await reportApi.getReport(id);
      currentReport.value = response.data;
      return response.data;
    } catch (error) {
      console.error("获取报告详情失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 生成报告
  const generateReport = async (data: ReportDTO) => {
    try {
      loading.value = true;
      const response = await reportApi.generateReport(data);
      reports.value.unshift(response.data); // 新报告添加到前面
      return response.data;
    } catch (error) {
      console.error("生成报告失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  // 删除报告
  const deleteReport = async (id: number) => {
    try {
      loading.value = true;
      await reportApi.deleteReport(id);
      reports.value = reports.value.filter((r) => r.id !== id);
      if (currentReport.value?.id === id) {
        currentReport.value = null;
      }
    } catch (error) {
      console.error("删除报告失败:", error);
      throw error;
    } finally {
      loading.value = false;
    }
  };

  return {
    // 状态
    reports,
    loading,
    currentReport,

    // 计算属性
    reportCount,
    weeklyReports,
    monthlyReports,
    stageReports,

    // 方法
    fetchReports,
    fetchReport,
    generateReport,
    deleteReport,
  };
});
