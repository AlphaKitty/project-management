package com.projectmanagement.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectmanagement.annotation.OperationLog;
import com.projectmanagement.entity.User;
import com.projectmanagement.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 * 通过AOP自动记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(com.projectmanagement.annotation.OperationLog)")
    public void operationLogPointCut() {
    }

    @Around("operationLogPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        // 获取注解信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperationLog operationLogAnnotation = method.getAnnotation(OperationLog.class);

        // 创建操作日志对象
        com.projectmanagement.entity.OperationLog operationLogEntity = new com.projectmanagement.entity.OperationLog();

        // 设置基本信息
        operationLogEntity.setOperationType(operationLogAnnotation.type().getCode());
        operationLogEntity.setModule(operationLogAnnotation.module().getCode());
        operationLogEntity.setDescription(operationLogAnnotation.description());
        operationLogEntity.setOperationTime(LocalDateTime.now());

        // 设置请求信息
        if (request != null) {
            operationLogEntity.setRequestUrl(request.getRequestURL().toString());
            operationLogEntity.setRequestMethod(request.getMethod());
            operationLogEntity.setIpAddress(getClientIpAddress(request));
            operationLogEntity.setUserAgent(request.getHeader("User-Agent"));

            // 获取当前用户信息
            HttpSession session = request.getSession(false);
            if (session != null) {
                User currentUser = (User) session.getAttribute("currentUser");
                if (currentUser != null) {
                    operationLogEntity.setUserId(currentUser.getId());
                    operationLogEntity.setUsername(currentUser.getUsername());
                }
            }
        }

        // 记录请求参数
        if (operationLogAnnotation.recordParams()) {
            try {
                Object[] args = point.getArgs();
                // 过滤敏感参数（如HttpSession、HttpServletRequest等）
                Object[] filteredArgs = filterSensitiveArgs(args);
                String paramsJson = objectMapper.writeValueAsString(filteredArgs);
                // 限制参数长度，避免日志过大
                if (paramsJson.length() > 2000) {
                    paramsJson = paramsJson.substring(0, 2000) + "...";
                }
                operationLogEntity.setRequestParams(paramsJson);
            } catch (Exception e) {
                operationLogEntity.setRequestParams("参数序列化失败: " + e.getMessage());
            }
        }

        Object result = null;
        Exception exception = null;

        try {
            // 执行目标方法
            result = point.proceed();
            operationLogEntity.setSuccess(true);
        } catch (Exception e) {
            exception = e;
            operationLogEntity.setSuccess(false);
            operationLogEntity.setErrorMessage(e.getMessage());
            throw e; // 重新抛出异常
        } finally {
            // 计算耗时
            long endTime = System.currentTimeMillis();
            operationLogEntity.setDuration(endTime - startTime);

            // 记录响应结果
            if (operationLogAnnotation.recordResponse() && result != null) {
                try {
                    String responseJson = objectMapper.writeValueAsString(result);
                    if (responseJson.length() > 1000) {
                        responseJson = responseJson.substring(0, 1000) + "...";
                    }
                    operationLogEntity.setResponse(responseJson);
                } catch (Exception e) {
                    operationLogEntity.setResponse("响应序列化失败: " + e.getMessage());
                }
            }

            // 异步记录日志，避免影响主业务
            try {
                operationLogService.recordLogAsync(operationLogEntity);
            } catch (Exception e) {
                log.error("记录操作日志失败: {}", e.getMessage(), e);
            }
        }

        return result;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * 过滤敏感参数
     */
    private Object[] filterSensitiveArgs(Object[] args) {
        if (args == null) {
            return new Object[0];
        }

        Object[] filteredArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            // 过滤掉Servlet相关对象
            if (arg instanceof HttpServletRequest ||
                    arg instanceof HttpSession ||
                    arg instanceof ServletRequestAttributes) {
                filteredArgs[i] = "[FILTERED]";
            } else {
                filteredArgs[i] = arg;
            }
        }
        return filteredArgs;
    }
}