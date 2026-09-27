package com.testdigital.model;

import java.util.List;
/**
 * 测试执行结果数据对象，封装一次完整执行后的全部输出。
 * <p>
 * 包含每条用例的详细结果、汇总统计及控制台错误列表，
 * 由 {@code Executor} 构建并传递给 {@code Reporter} 用于生成 HTML 报告。
 * </p>
 */
public class ExecutionResult {
    /** 各用例的详细执行结果列表 */
    private List<TestResult> results;
    /** 测试结果汇总（总数、通过数、失败数等） */
    private TestSummary summary;
    /** 浏览器控制台及页面错误信息列表 */
    private List<String> consoleErrors;

    /** 默认无参构造，供 JSON 反序列化使用 */
    public ExecutionResult() {}

    /**
     * 全参构造，一次性初始化执行结果。
     *
     * @param results       用例结果列表
     * @param summary       汇总统计
     * @param consoleErrors 控制台错误列表
     */
    public ExecutionResult(List<TestResult> results, TestSummary summary, List<String> consoleErrors) {
        this.results = results;
        this.summary = summary;
        this.consoleErrors = consoleErrors;
    }

    public List<TestResult> getResults() { return results; }
    public void setResults(List<TestResult> results) { this.results = results; }
    public TestSummary getSummary() { return summary; }
    public void setSummary(TestSummary summary) { this.summary = summary; }
    public List<String> getConsoleErrors() { return consoleErrors; }
    public void setConsoleErrors(List<String> consoleErrors) { this.consoleErrors = consoleErrors; }
}
