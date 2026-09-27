package com.testdigital.controller;

import com.testdigital.engine.CaseGenerator;
import com.testdigital.engine.Executor;
import com.testdigital.engine.LLMService;
import com.testdigital.engine.Reporter;
import com.testdigital.engine.RequestLogService;
import com.testdigital.engine.ScriptGenerator;
import com.testdigital.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试数字人 REST 控制器，编排自动化测试全流程并对外暴露 API。
 * <p>
 * 提供以下接口：
 * <ul>
 *   <li>{@code GET /api/status} — 查询服务状态及 LLM 是否启用</li>
 *   <li>{@code POST /api/generate-cases} — 根据功能点生成测试用例</li>
 *   <li>{@code POST /api/generate-scripts} — 将用例渲染为 Playwright 测试脚本</li>
 *   <li>{@code POST /api/execute} — 编译并执行测试脚本</li>
 *   <li>{@code POST /api/generate-report} — 根据执行结果生成 HTML 报告</li>
 *   <li>{@code POST /api/run-full} — 一键执行全流程（用例生成 → 脚本生成 → 执行 → 报告）</li>
 * </ul>
 * 所有接口入参统一使用 {@code Map} 接收，并在方法开头进行空值校验。
 * </p>
 */
@RestController
@CrossOrigin
public class TestController {

    @Autowired
    private CaseGenerator caseGenerator;

    @Autowired
    private ScriptGenerator scriptGenerator;

    @Autowired
    private Executor executor;

    @Autowired
    private Reporter reporter;

    @Autowired
    private LLMService llmService;

    @Autowired
    private RequestLogService requestLogService;

    /** 查询服务状态，返回 LLM 启用情况 */
    @GetMapping("/api/status")
    public ResponseEntity<?> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("llmEnabled", llmService.isEnabled());
        return ResponseEntity.ok(status);
    }

    /** 根据功能点生成测试用例，支持 LLM 增强 */
    @PostMapping("/api/generate-cases")
    public ResponseEntity<?> generateCases(@RequestBody Map<String, String> body) {
        String featurePoint = body.get("featurePoint");
        if (featurePoint == null || featurePoint.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供功能点描述"));
        }
        String targetUrl = body.get("targetUrl");
        List<TestCase> cases = caseGenerator.generate(featurePoint, targetUrl);
        Map<String, Object> response = new HashMap<>();
        response.put("cases", cases);
        response.put("llmEnhanced", llmService.isEnabled());
        return ResponseEntity.ok(response);
    }

    /** 将测试用例渲染为 Playwright Java 测试脚本 */
    @PostMapping("/api/generate-scripts")
    public ResponseEntity<?> generateScripts(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawCases = (List<Map<String, Object>>) body.get("cases");
        if (rawCases == null || rawCases.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供测试用例"));
        }
        String targetUrl = (String) body.get("targetUrl");
        List<TestCase> cases = convertCases(rawCases);
        ScriptResult result = scriptGenerator.generate(cases, targetUrl);
        return ResponseEntity.ok(result);
    }

    /** 编译并执行指定的测试脚本，返回执行结果 */
    @PostMapping("/api/execute")
    public ResponseEntity<?> execute(@RequestBody Map<String, String> body) {
        String scriptFile = body.get("scriptFile");
        if (scriptFile == null || scriptFile.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供脚本文件路径"));
        }
        String targetUrl = body.get("targetUrl");
        try {
            ExecutionResult results = executor.execute(scriptFile, targetUrl);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** 根据测试结果生成 HTML 报告 */
    @PostMapping("/api/generate-report")
    public ResponseEntity<?> generateReport(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, Object> rawResults = (Map<String, Object>) body.get("results");
        if (rawResults == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供测试结果"));
        }
        String featurePoint = (String) body.get("featurePoint");
        ExecutionResult execResult = convertExecutionResult(rawResults);
        ReportInfo report = reporter.generate(execResult, featurePoint);
        return ResponseEntity.ok(report);
    }

    /** 一键执行全流程：用例生成 → 脚本生成 → 执行 → 报告 */
    @PostMapping("/api/run-full")
    public ResponseEntity<?> runFull(@RequestBody Map<String, String> body) {
        String featurePoint = body.get("featurePoint");
        if (featurePoint == null || featurePoint.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请提供功能点描述"));
        }
        String targetUrl = body.get("targetUrl");

        try {
            List<TestCase> cases = caseGenerator.generate(featurePoint, targetUrl);
            ScriptResult scriptResult = scriptGenerator.generate(cases, targetUrl);
            ExecutionResult execResults = executor.execute(scriptResult.getFile(), targetUrl);
            ReportInfo report = reporter.generate(execResults, featurePoint);

            Map<String, Object> response = new HashMap<>();
            response.put("cases", cases);
            response.put("scriptFile", scriptResult.getFile());
            response.put("scriptContent", scriptResult.getContent());
            response.put("execResults", execResults);
            response.put("report", report);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** 查询请求日志列表 */
    @GetMapping("/api/logs")
    public ResponseEntity<?> getLogs() {
        return ResponseEntity.ok(Map.of("logs", requestLogService.getLogs()));
    }

    /** 查询单条请求日志详情 */
    @GetMapping("/api/logs/{id}")
    public ResponseEntity<?> getLogDetail(@PathVariable String id) {
        RequestLogService.RequestLog log = requestLogService.getLog(id);
        if (log == null) {
            return ResponseEntity.status(404).body(Map.of("error", "日志不存在"));
        }
        return ResponseEntity.ok(log);
    }

    /** 清空请求日志 */
    @DeleteMapping("/api/logs")
    public ResponseEntity<?> clearLogs() {
        requestLogService.clearLogs();
        return ResponseEntity.ok(Map.of("message", "日志已清空"));
    }

    /** 将前端传入的原始 Map 结构转换为 TestCase 对象列表 */
    private List<TestCase> convertCases(List<Map<String, Object>> rawCases) {
        return rawCases.stream().map(raw -> {
            TestCase tc = new TestCase();
            tc.setId((String) raw.get("id"));
            tc.setName((String) raw.get("name"));
            tc.setCategory((String) raw.get("category"));
            tc.setPriority((String) raw.get("priority"));
            tc.setDescription((String) raw.get("description"));
            @SuppressWarnings("unchecked")
            List<String> steps = (List<String>) raw.get("steps");
            tc.setSteps(steps);
            tc.setExpected((String) raw.get("expected"));
            tc.setType((String) raw.get("type"));
            return tc;
        }).toList();
    }

    /** 将前端传入的原始 Map 结构转换为 ExecutionResult 对象 */
    @SuppressWarnings("unchecked")
    private ExecutionResult convertExecutionResult(Map<String, Object> raw) {
        List<Map<String, Object>> rawResults = (List<Map<String, Object>>) raw.get("results");
        List<TestResult> results = rawResults.stream().map(r -> new TestResult(
                (String) r.get("id"),
                (String) r.get("name"),
                (String) r.get("status"),
                ((Number) r.get("duration")).longValue(),
                (String) r.get("details")
        )).toList();

        Map<String, Object> rawSummary = (Map<String, Object>) raw.get("summary");
        TestSummary summary = new TestSummary(
                ((Number) rawSummary.get("total")).intValue(),
                ((Number) rawSummary.get("passed")).intValue(),
                ((Number) rawSummary.get("failed")).intValue(),
                ((Number) rawSummary.get("skipped")).intValue(),
                ((Number) rawSummary.get("consoleErrors")).intValue()
        );

        List<String> consoleErrors = (List<String>) raw.get("consoleErrors");
        return new ExecutionResult(results, summary, consoleErrors);
    }
}
