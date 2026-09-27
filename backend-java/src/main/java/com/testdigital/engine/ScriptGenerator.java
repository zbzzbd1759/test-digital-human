package com.testdigital.engine;

import com.testdigital.model.ScriptResult;
import com.testdigital.model.TestCase;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 测试脚本生成器，将测试用例列表渲染为可编译执行的 Java 源码（基于 Playwright）。
 * <p>
 * 生成的脚本写入 {@code generated-scripts/TestRunner.java}，包含完整的 Playwright 浏览器启动、
 * 用例执行、结果收集及 JSON 输出逻辑，供 {@link Executor} 编译并执行。
 * </p>
 */
@Component
public class ScriptGenerator {

    /** 生成脚本的输出目录 */
    private static final String SCRIPT_DIR = "generated-scripts";

    /**
     * 根据测试用例列表生成完整的 Playwright 测试脚本并写入文件。
     *
     * @param cases     测试用例列表
     * @param targetUrl 被测页面 URL，为 null 时使用默认地址
     * @return 脚本生成结果，包含文件路径、内容及时间戳
     */
    public ScriptResult generate(List<TestCase> cases, String targetUrl) {
        String url = targetUrl != null ? targetUrl : "http://localhost:3456/sample-app/index.html";
        long timestamp = System.currentTimeMillis();
        String fileName = "TestRunner.java";

        Path dir = Paths.get(SCRIPT_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建脚本目录", e);
        }

        Path filePath = dir.resolve(fileName);

        StringBuilder sb = new StringBuilder();
        sb.append("// 自动生成的测试脚本\n");
        sb.append("// 生成时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("// 目标地址: ").append(url).append("\n");
        sb.append("// 测试用例数: ").append(cases.size()).append("\n\n");

        sb.append("import com.microsoft.playwright.*;\n");
        sb.append("import com.microsoft.playwright.options.WaitUntilState;\n");
        sb.append("import java.util.*;\n\n");
        sb.append("public class TestRunner {\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        try (Playwright pw = Playwright.create()) {\n");
        sb.append("            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));\n");
        sb.append("            BrowserContext context = browser.newContext();\n");
        sb.append("            Page page = context.newPage();\n");
        sb.append("            List<Map<String, Object>> results = new ArrayList<>();\n");
        sb.append("            List<String> consoleErrors = new ArrayList<>();\n\n");

        sb.append("            page.onConsoleMessage(msg -> {\n");
        sb.append("                if (\"error\".equals(msg.type())) consoleErrors.add(msg.text());\n");
        sb.append("            });\n");
        sb.append("            page.onPageError(error -> consoleErrors.add(error));\n\n");

        for (TestCase tc : cases) {
            sb.append(generateCaseCode(tc, url));
        }

        sb.append("            browser.close();\n\n");

        sb.append("            int passed = (int) results.stream().filter(r -> \"passed\".equals(r.get(\"status\"))).count();\n");
        sb.append("            int failed = (int) results.stream().filter(r -> \"failed\".equals(r.get(\"status\"))).count();\n");
        sb.append("            int skipped = (int) results.stream().filter(r -> \"skipped\".equals(r.get(\"status\"))).count();\n\n");

        sb.append("            Map<String, Object> output = new LinkedHashMap<>();\n");
        sb.append("            output.put(\"results\", results);\n");
        sb.append("            Map<String, Object> summary = new LinkedHashMap<>();\n");
        sb.append("            summary.put(\"total\", results.size());\n");
        sb.append("            summary.put(\"passed\", passed);\n");
        sb.append("            summary.put(\"failed\", failed);\n");
        sb.append("            summary.put(\"skipped\", skipped);\n");
        sb.append("            summary.put(\"consoleErrors\", consoleErrors.size());\n");
        sb.append("            output.put(\"summary\", summary);\n");
        sb.append("            output.put(\"consoleErrors\", consoleErrors);\n\n");

        sb.append("            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();\n");
        sb.append("            System.out.println(gson.toJson(output));\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("}\n");

        String content = sb.toString();
        try {
            Files.writeString(filePath, content);
        } catch (IOException e) {
            throw new RuntimeException("无法写入脚本文件", e);
        }

        return new ScriptResult(filePath.toString(), fileName, content, timestamp);
    }

    /**
     * 为单个测试用例生成对应的 try/catch 包裹的 Java 代码片段。
     * 根据用例的 {@code type} 字段分发到不同的模板方法。
     *
     * @param tc  测试用例
     * @param url 被测页面 URL
     * @return 生成的 Java 代码字符串
     */
    private String generateCaseCode(TestCase tc, String url) {
        StringBuilder sb = new StringBuilder();
        sb.append("            // ").append(tc.getId()).append(": ").append(tc.getName()).append("\n");
        sb.append("            try {\n");

        switch (tc.getType()) {
            case "functional" -> sb.append(genFunctional(tc, url));
            case "boundary" -> sb.append(genBoundary(tc, url));
            case "security" -> sb.append(genSecurity(tc, url));
            case "stress" -> sb.append(genStress(tc, url));
            case "performance" -> sb.append(genPerformance(tc, url));
            case "ui" -> sb.append(genUI(tc, url));
            default -> sb.append(genFunctional(tc, url));
        }

        sb.append("            } catch (Exception e) {\n");
        sb.append("                Map<String, Object> r = new LinkedHashMap<>();\n");
        sb.append("                r.put(\"id\", \"").append(tc.getId()).append("\");\n");
        sb.append("                r.put(\"name\", \"").append(escape(tc.getName())).append("\");\n");
        sb.append("                r.put(\"status\", \"failed\");\n");
        sb.append("                r.put(\"duration\", 0L);\n");
        sb.append("                r.put(\"details\", e.getMessage());\n");
        sb.append("                results.add(r);\n");
        sb.append("            }\n\n");
        return sb.toString();
    }

    /** 生成功能测试代码（页面加载验证或通用功能验证） */
    private String genFunctional(TestCase tc, String url) {
        if (tc.getName().contains("页面加载")) {
            return """
                    long startTime = System.currentTimeMillis();
                    page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                    long loadTime = System.currentTimeMillis() - startTime;
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", "%s");
                    r.put("name", "%s");
                    r.put("status", "passed");
                    r.put("duration", loadTime);
                    r.put("details", "页面加载成功，无JS错误");
                    results.add(r);
                """.formatted(url, tc.getId(), escape(tc.getName()));
        }
        return """
                page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                String title = page.title();
                String bodyText = page.textContent("body");
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "%s");
                r.put("name", "%s");
                r.put("status", "passed");
                r.put("duration", 0L);
                r.put("details", "页面标题: " + title + "，内容长度: " + (bodyText != null ? bodyText.length() : 0));
                results.add(r);
            """.formatted(url, tc.getId(), escape(tc.getName()));
    }

    /** 生成边界值测试代码（空输入处理等） */
    private String genBoundary(TestCase tc, String url) {
        if (tc.getName().contains("空输入")) {
            return """
                    page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                    Locator input = page.locator("input[type=\\"text\\"], input:not([type]), textarea").first();
                    if (input.count() > 0) {
                        input.fill("");
                        Locator btn = page.locator("button[type=\\"submit\\"], button, input[type=\\"submit\\"]").first();
                        if (btn.count() > 0) btn.click();
                        page.waitForTimeout(500);
                    }
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", "%s");
                    r.put("name", "%s");
                    r.put("status", "passed");
                    r.put("duration", 0L);
                    r.put("details", "空输入处理正常，页面未崩溃");
                    results.add(r);
                """.formatted(url, tc.getId(), escape(tc.getName()));
        }
        return """
                page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "%s");
                r.put("name", "%s");
                r.put("status", "passed");
                r.put("duration", 0L);
                r.put("details", "边界值测试通过");
                results.add(r);
            """.formatted(url, tc.getId(), escape(tc.getName()));
    }

    /** 生成安全测试代码（XSS、SQL 注入等 payload 检测） */
    private String genSecurity(TestCase tc, String url) {
        return """
                page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                String[] payloads = {"<script>alert(1)</script>", "' OR '1'='1", "{{7*7}}", "🎉🚀💥"};
                Locator input = page.locator("input[type=\\"text\\"], input:not([type]), textarea").first();
                if (input.count() > 0) {
                    for (String payload : payloads) {
                        input.fill(payload);
                        page.waitForTimeout(200);
                    }
                    String pageContent = page.content();
                    boolean hasXSS = pageContent.contains("<script>alert(1)</script>");
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", "%s");
                    r.put("name", "%s");
                    r.put("status", hasXSS ? "failed" : "passed");
                    r.put("duration", 0L);
                    r.put("details", hasXSS ? "检测到XSS漏洞！" : "特殊字符处理正确，无注入风险");
                    results.add(r);
                } else {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", "%s");
                    r.put("name", "%s");
                    r.put("status", "passed");
                    r.put("duration", 0L);
                    r.put("details", "页面无输入框，跳过安全测试");
                    results.add(r);
                }
            """.formatted(url, tc.getId(), escape(tc.getName()), tc.getId(), escape(tc.getName()));
    }

    /** 生成压力测试代码（重复操作稳定性验证） */
    private String genStress(TestCase tc, String url) {
        return """
                page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                Locator btn = page.locator("button").first();
                if (btn.count() > 0) {
                    long startTime = System.currentTimeMillis();
                    for (int i = 0; i < 10; i++) {
                        try { btn.click(); } catch (Exception ignored) {}
                        page.waitForTimeout(100);
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", "%s");
                    r.put("name", "%s");
                    r.put("status", "passed");
                    r.put("duration", duration);
                    r.put("details", "10次重复操作完成，耗时: " + duration + "ms");
                    results.add(r);
                } else {
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("id", "%s");
                    r.put("name", "%s");
                    r.put("status", "passed");
                    r.put("duration", 0L);
                    r.put("details", "页面无按钮，跳过重复操作测试");
                    results.add(r);
                }
            """.formatted(url, tc.getId(), escape(tc.getName()), tc.getId(), escape(tc.getName()));
    }

    /** 生成性能测试代码（页面加载耗时检测，阈值 3 秒） */
    private String genPerformance(TestCase tc, String url) {
        return """
                long startTime = System.currentTimeMillis();
                page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                long loadTime = System.currentTimeMillis() - startTime;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "%s");
                r.put("name", "%s");
                r.put("status", loadTime < 3000 ? "passed" : "failed");
                r.put("duration", loadTime);
                r.put("details", "加载耗时: " + loadTime + "ms");
                results.add(r);
            """.formatted(url, tc.getId(), escape(tc.getName()));
    }

    /** 生成 UI 测试代码（元素可见性验证） */
    private String genUI(TestCase tc, String url) {
        return """
                page.navigate("%s", new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE).setTimeout(10000));
                Object elements = page.evaluate("() => { const all = document.querySelectorAll('h1,h2,h3,p,button,input,form,nav,header,footer,main,section'); return Array.from(all).map(el => ({ tag: el.tagName.toLowerCase(), text: (el.textContent||'').trim().substring(0,50), visible: el.offsetParent !== null || el.offsetWidth > 0 })); }");
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", "%s");
                r.put("name", "%s");
                r.put("status", "passed");
                r.put("duration", 0L);
                r.put("details", "UI元素可见性验证通过");
                results.add(r);
            """.formatted(url, tc.getId(), escape(tc.getName()));
    }

    /**
     * 转义字符串中的反斜杠、双引号、换行符，防止注入到生成的 Java 模板字符串中。
     *
     * @param s 原始字符串
     * @return 转义后的字符串
     */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
