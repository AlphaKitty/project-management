package com.projectmanagement;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Gson JSON转换测试
 */
public class GsonTest {
    
    public static void main(String[] args) {
        Gson gson = new Gson();
        
        // 测试Map转JSON
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("userName", "张三");
        testMap.put("taskCount", 5);
        testMap.put("currentDate", "2025-05-25");
        testMap.put("taskListHtml", "<ul><li>任务1</li><li>任务2</li></ul>");
        
        String json = gson.toJson(testMap);
        System.out.println("Map转JSON: " + json);
        
        // 测试JSON转Map
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> resultMap = gson.fromJson(json, type);
        
        System.out.println("JSON转Map:");
        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
        }
        
        // 测试模板变量替换
        String template = "您好 ${userName}！您有 ${taskCount} 个任务需要处理。${taskListHtml}";
        String result = template;
        for (Map.Entry<String, Object> entry : resultMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace("${" + key + "}", value);
        }
        
        System.out.println("\n模板渲染结果:");
        System.out.println(result);
    }
} 