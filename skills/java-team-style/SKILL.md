---
name: java-team-style
description: 当生成 Java 代码时，自动应用团队编码规范，包括命名规则、注释要求、异常处理、日志规范和返回格式。
TRIGGER when: 用户要求生成、修改或重构 Java 代码（.java 文件）时自动触发。
---

# 团队 Java 编码规范

你在生成任何 Java 代码时，必须严格遵守以下规范。这不是建议，而是强制要求。

## 1. 命名规范

- **类名**：大驼峰（PascalCase），必须是名词或名词短语
  - ✅ `EmployeeExportService`、`OrderQueryController`
  - ❌ `employee_export_service`、`exportService`
- **方法名**：小驼峰（camelCase），必须以动词开头
  - ✅ `exportById()`、`queryUserList()`
  - ❌ `ExportData()`、`get_user_by_id()`
- **常量**：全大写 + 下划线
  - ✅ `MAX_RETRY_COUNT`
  - ❌ `maxRetryCount`
- **变量/参数**：小驼峰，禁止单字母（循环变量 i/j/k 除外）
  - ✅ `employeeId`、`exportResult`
  - ❌ `e`、`data`、`temp`

## 2. 注释规范

- **类注释**：每个类必须有 JavaDoc，包含用途说明和作者

/**
 * 员工数据导出服务
 * 负责将员工信息按指定格式导出为 Excel/CSV 文件
 *
 * @author {{作者名}}
 * @since {{日期}}
 */

- **方法注释**：所有 public 方法必须有 JavaDoc，包含功能说明、参数说明和返回值说明

/**
 * 根据员工ID导出员工信息
 *
 * @param employeeId 员工唯一标识
 * @return 导出文件的下载地址
 * @throws BusinessException 当员工不存在或导出失败时抛出
 */

- **行内注释**：仅在逻辑复杂或不直观处添加，禁止写无意义注释如 `// 获取用户`

## 3. 异常处理规范

- **统一使用自定义业务异常** `BusinessException`
- **禁止直接抛出** `RuntimeException`、`IllegalArgumentException` 等通用异常
- **异常信息格式**：中文描述 + 上下文参数，禁止纯错误码

// ✅ 正确
throw new BusinessException("员工不存在，employeeId: " + employeeId);
throw new BusinessException("导出失败，原因：文件写入异常", e);

// ❌ 错误
throw new RuntimeException("导出失败");
throw new BusinessException("ERR_001");
throw new IllegalArgumentException("error");

- **异常必须记录日志后再抛出**（除非是参数校验类异常）

## 4. 日志规范

- 使用 `@Slf4j` 注解（Lombok），禁止手动创建 Logger
- **日志级别使用规则**：
  - `INFO`：关键业务节点（方法入口、核心操作完成）
  - `WARN`：可恢复的异常情况
  - `ERROR`：不可恢复的异常，必须包含异常堆栈和关键参数
  - `DEBUG`：调试信息，禁止出现在 INFO 级别
- **日志必须包含关键参数**：

// ✅ 正确
log.info("开始导出员工数据, employeeId: {}", employeeId);
log.error("员工数据导出失败, employeeId: {}", employeeId, e);

// ❌ 错误
log.info("开始导出");
log.error("导出失败");

## 5. 返回格式规范

- **所有 Controller 层接口统一返回 `Result<T>` 包装类**
- 禁止直接返回实体对象或 Map

// ✅ 正确
@GetMapping("/export/{id}")
public Result<String> exportEmployee(@PathVariable Long id) {
    String url = employeeExportService.exportById(id);
    return Result.success(url);
}

// ❌ 错误
@GetMapping("/export/{id}")
public Map<String, Object> exportEmployee(@PathVariable Long id) { ... }

## 6. 代码模板

生成 Service 类时，使用以下结构模板：

package com.company.module.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class XxxService {

    private final XxxRepository xxxRepository;

    public ReturnType methodName(ParamType param) {
        log.info("[操作描述], param: {}", param);
        try {
            // 业务逻辑
            log.info("[操作描述]完成, param: {}", param);
            return result;
        } catch (Exception e) {
            log.error("[操作描述]失败, param: {}", param, e);
            throw new BusinessException("[中文错误描述], param: " + param, e);
        }
    }
}