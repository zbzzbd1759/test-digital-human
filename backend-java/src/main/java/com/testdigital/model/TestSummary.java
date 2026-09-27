package com.testdigital.model;

/**
 * 测试结果汇总数据对象，记录一次测试执行中各状态用例的数量及控制台错误数。
 * 被 {@link ExecutionResult} 和 {@link ReportInfo} 引用，用于承载子进程返回的统计信息。
 */
public class TestSummary {
    /** 测试用例总数 */
    private int total;
    /** 通过的用例数 */
    private int passed;
    /** 失败的用例数 */
    private int failed;
    /** 跳过的用例数 */
    private int skipped;
    /** 控制台错误数量 */
    private int consoleErrors;

    /** 默认无参构造，供 JSON 反序列化使用 */
    public TestSummary() {}

    /**
     * 全参构造，根据子进程返回的汇总数据创建实例。
     *
     * @param total         测试用例总数
     * @param passed        通过数
     * @param failed        失败数
     * @param skipped       跳过数
     * @param consoleErrors 控制台错误数
     */
    public TestSummary(int total, int passed, int failed, int skipped, int consoleErrors) {
        this.total = total;
        this.passed = passed;
        this.failed = failed;
        this.skipped = skipped;
        this.consoleErrors = consoleErrors;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getConsoleErrors() { return consoleErrors; }
    public void setConsoleErrors(int consoleErrors) { this.consoleErrors = consoleErrors; }
}
