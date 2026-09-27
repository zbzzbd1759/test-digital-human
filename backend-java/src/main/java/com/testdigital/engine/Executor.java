package com.testdigital.engine;

import com.testdigital.model.ExecutionResult;
import com.testdigital.model.TestResult;
import com.testdigital.model.TestSummary;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 测试脚本执行器，负责编译并运行 {@code ScriptGenerator} 生成的 Java 测试脚本。
 * <p>
 * 执行流程：先调用 {@code javac} 编译脚本（30 秒超时），再启动 {@code java} 子进程执行
 * {@code TestRunner}（120 秒超时）。通过线程池并行读取 stdout/stderr 避免死锁，
 * 最终解析 stdout 中的 JSON 结果并封装为 {@link ExecutionResult}。
 * </p>
 */
@Component
public class Executor {

    /**
     * 编译并执行指定的测试脚本文件。
     *
     * @param scriptFile 脚本文件路径（相对于工作目录或绝对路径）
     * @param targetUrl  被测页面 URL（传递给子进程环境变量）
     * @return 执行结果，包含各用例结果、汇总统计及控制台错误
     * @throws Exception 编译失败、执行超时或脚本不存在时抛出
     */
    @SuppressWarnings("unchecked")
    public ExecutionResult execute(String scriptFile, String targetUrl) throws Exception {
        Path scriptPath = Paths.get(scriptFile);
        if (!Files.exists(scriptPath)) {
            throw new RuntimeException("脚本文件不存在: " + scriptFile);
        }

        Path scriptDir = scriptPath.getParent().toAbsolutePath();

        String classpath = System.getProperty("java.class.path");

        ProcessBuilder compilePb = new ProcessBuilder(
                "javac",
                "-cp", classpath,
                "-d", scriptDir.toString(),
                scriptPath.toAbsolutePath().toString()
        );
        compilePb.redirectErrorStream(true);
        Process compileProcess = compilePb.start();
        boolean compileFinished = compileProcess.waitFor(30, TimeUnit.SECONDS);
        if (!compileFinished) {
            compileProcess.destroyForcibly();
            throw new RuntimeException("脚本编译超时");
        }

        String compileOutput;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(compileProcess.getInputStream()))) {
            compileOutput = reader.lines().collect(Collectors.joining("\n")).trim();
        }
        if (!compileOutput.isEmpty()) {
            throw new RuntimeException("脚本编译失败: " + compileOutput);
        }

        ProcessBuilder runPb = new ProcessBuilder(
                "java",
                "-cp", classpath + ":" + scriptDir.toString(),
                "TestRunner"
        );
        runPb.redirectErrorStream(false);
        runPb.directory(scriptDir.toFile());
        runPb.environment().putAll(System.getenv());

        Process runProcess = runPb.start();

        ExecutorService streamExecutor = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = streamExecutor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()))) {
                return reader.lines().collect(Collectors.joining("\n")).trim();
            }
        });
        Future<String> stderrFuture = streamExecutor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()))) {
                return reader.lines().collect(Collectors.joining("\n")).trim();
            }
        });

        boolean runFinished = runProcess.waitFor(120, TimeUnit.SECONDS);
        if (!runFinished) {
            runProcess.destroyForcibly();
            streamExecutor.shutdownNow();
            throw new RuntimeException("脚本执行超时");
        }

        String stdout = stdoutFuture.get();
        String stderr = stderrFuture.get();
        streamExecutor.shutdown();

        if (stdout.isEmpty() && !stderr.isEmpty()) {
            throw new RuntimeException("脚本执行失败: " + stderr);
        }

        Gson gson = new Gson();
        Map<String, Object> data = gson.fromJson(stdout, new TypeToken<Map<String, Object>>() {}.getType());

        List<TestResult> results = new ArrayList<>();
        List<Map<String, Object>> rawResults = (List<Map<String, Object>>) data.get("results");
        if (rawResults != null) {
            for (Map<String, Object> raw : rawResults) {
                results.add(new TestResult(
                        (String) raw.get("id"),
                        (String) raw.get("name"),
                        (String) raw.get("status"),
                        ((Number) raw.get("duration")).longValue(),
                        (String) raw.get("details")
                ));
            }
        }

        Map<String, Object> rawSummary = (Map<String, Object>) data.get("summary");
        TestSummary summary = new TestSummary(
                ((Number) rawSummary.get("total")).intValue(),
                ((Number) rawSummary.get("passed")).intValue(),
                ((Number) rawSummary.get("failed")).intValue(),
                ((Number) rawSummary.get("skipped")).intValue(),
                ((Number) rawSummary.get("consoleErrors")).intValue()
        );

        List<String> consoleErrors = (List<String>) data.get("consoleErrors");
        if (consoleErrors == null) consoleErrors = new ArrayList<>();

        return new ExecutionResult(results, summary, consoleErrors);
    }
}
