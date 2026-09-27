package com.testdigital.engine;

import com.testdigital.model.ExecutionResult;
import com.testdigital.model.ReportInfo;
import com.testdigital.model.TestResult;
import com.testdigital.model.TestSummary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 测试报告生成器，将 {@link ExecutionResult} 渲染为带进度条与错误列表的 HTML 报告。
 * <p>
 * 报告文件以毫秒级时间戳命名（如 {@code report-1790265098470.html}），写入 {@code reports/} 目录，
 * 同时返回 {@link ReportInfo} 供前端直接访问。
 * </p>
 */
@Component
public class Reporter {

    /** 报告输出目录 */
    private static final String REPORT_DIR = "reports";

    /**
     * 根据执行结果生成 HTML 测试报告并写入文件。
     *
     * @param results      测试执行结果
     * @param featurePoint 功能点描述，显示在报告标题中
     * @return 报告元数据，包含文件名、路径、URL 及通过率
     */
    public ReportInfo generate(ExecutionResult results, String featurePoint) {
        long timestamp = System.currentTimeMillis();
        String fileName = "report-" + timestamp + ".html";

        Path dir = Paths.get(REPORT_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建报告目录", e);
        }

        Path filePath = dir.resolve(fileName);

        TestSummary summary = results.getSummary();
        double passRate = summary.getTotal() > 0
                ? ((double) summary.getPassed() / summary.getTotal()) * 100
                : 0;

        String html = buildHtml(featurePoint, results, summary, passRate);

        try {
            Files.writeString(filePath, html);
        } catch (IOException e) {
            throw new RuntimeException("无法写入报告文件", e);
        }

        return new ReportInfo(fileName, filePath.toString(), "/reports/" + fileName, summary, passRate);
    }

    /**
     * 构建完整的 HTML 报告内容，包含摘要卡片、通过率进度条、用例详情表格及控制台错误列表。
     *
     * @param featurePoint 功能点描述
     * @param results      执行结果
     * @param summary      汇总统计
     * @param passRate     通过率（0.0 ~ 100.0）
     * @return 完整的 HTML 字符串
     */
    private String buildHtml(String featurePoint, ExecutionResult results, TestSummary summary, double passRate) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
        String progressClass = passRate >= 80 ? "#10b981" : passRate >= 50 ? "#f59e0b" : "#ef4444";

        StringBuilder rows = new StringBuilder();
        for (TestResult r : results.getResults()) {
            String badge = "passed".equals(r.getStatus())
                    ? "<span class=\"status-badge status-passed\">通过</span>"
                    : "<span class=\"status-badge status-failed\">失败</span>";
            rows.append("<tr>")
                    .append("<td>").append(esc(r.getId())).append("</td>")
                    .append("<td>").append(esc(r.getName())).append("</td>")
                    .append("<td>").append(badge).append("</td>")
                    .append("<td>").append(r.getDuration() > 0 ? r.getDuration() + "ms" : "-").append("</td>")
                    .append("<td>").append(esc(r.getDetails() != null ? r.getDetails() : "")).append("</td>")
                    .append("</tr>\n");
        }

        StringBuilder errors = new StringBuilder();
        if (results.getConsoleErrors() != null && !results.getConsoleErrors().isEmpty()) {
            errors.append("<div class=\"section\"><h2>控制台错误 (").append(results.getConsoleErrors().size()).append(")</h2><ul class=\"error-list\">");
            for (String e : results.getConsoleErrors()) {
                errors.append("<li>").append(esc(e)).append("</li>");
            }
            errors.append("</ul></div>");
        }

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>测试报告 - %s</title>
                  <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #f0f2f5; color: #333; }
                    .container { max-width: 1000px; margin: 0 auto; padding: 24px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 32px; border-radius: 12px; margin-bottom: 24px; }
                    .header h1 { font-size: 24px; margin-bottom: 8px; }
                    .header p { opacity: 0.9; font-size: 14px; }
                    .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 16px; margin-bottom: 24px; }
                    .summary-card { background: white; padding: 20px; border-radius: 10px; text-align: center; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
                    .summary-card .number { font-size: 32px; font-weight: 700; }
                    .summary-card .label { font-size: 13px; color: #666; margin-top: 4px; }
                    .passed { color: #10b981; } .failed { color: #ef4444; } .skipped { color: #f59e0b; } .total { color: #667eea; }
                    .section { background: white; border-radius: 10px; padding: 24px; margin-bottom: 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
                    .section h2 { font-size: 18px; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 2px solid #f0f2f5; }
                    table { width: 100%%; border-collapse: collapse; }
                    th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #f0f2f5; font-size: 14px; }
                    th { background: #f8f9fa; font-weight: 600; color: #555; }
                    .status-badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; }
                    .status-passed { background: #d1fae5; color: #065f46; }
                    .status-failed { background: #fee2e2; color: #991b1b; }
                    .progress-bar { height: 8px; background: #e5e7eb; border-radius: 4px; overflow: hidden; margin-top: 12px; }
                    .progress-fill { height: 100%%; border-radius: 4px; }
                    .footer { text-align: center; padding: 20px; color: #999; font-size: 13px; }
                    .error-list { list-style: none; }
                    .error-list li { padding: 8px 12px; background: #fef2f2; border-left: 3px solid #ef4444; margin-bottom: 8px; font-size: 13px; border-radius: 0 6px 6px 0; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h1>测试报告</h1>
                      <p>功能点: %s</p>
                      <p>生成时间: %s</p>
                    </div>
                    <div class="summary-grid">
                      <div class="summary-card"><div class="number total">%d</div><div class="label">总用例数</div></div>
                      <div class="summary-card"><div class="number passed">%d</div><div class="label">通过</div></div>
                      <div class="summary-card"><div class="number failed">%d</div><div class="label">失败</div></div>
                      <div class="summary-card"><div class="number" style="color:#667eea">%.1f%%</div><div class="label">通过率</div></div>
                    </div>
                    <div class="section">
                      <h2>通过率</h2>
                      <div class="progress-bar">
                        <div class="progress-fill" style="width:%.1f%%;background:%s"></div>
                      </div>
                    </div>
                    <div class="section">
                      <h2>测试详情</h2>
                      <table>
                        <thead><tr><th>编号</th><th>用例名称</th><th>状态</th><th>耗时</th><th>详情</th></tr></thead>
                        <tbody>%s</tbody>
                      </table>
                    </div>
                    %s
                    <div class="footer">测试数字人 - 自动化测试报告 (Java) | %s</div>
                  </div>
                </body>
                </html>
                """.formatted(
                esc(featurePoint),
                esc(featurePoint), time,
                summary.getTotal(), summary.getPassed(), summary.getFailed(), passRate,
                passRate, progressClass,
                rows, errors, time
        );
    }

    /**
     * HTML 转义，防止用户输入内容引发 XSS。
     * 转义字符包括：{@code &}、{@code <}、{@code >}、{@code "}。
     *
     * @param s 原始字符串
     * @return 转义后的字符串
     */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
