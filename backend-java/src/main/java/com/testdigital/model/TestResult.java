package com.testdigital.model;
/**
 * 单条测试用例的执行结果数据对象。
 * <p>
 * 记录用例编号、名称、执行状态、耗时及详情，由 {@code Executor} 解析子进程 stdout JSON 后构建，
 * 最终由 {@code Reporter} 渲染进 HTML 报告。
 * </p>
 */
public class TestResult {
    /** 用例编号，如 {@code TC-001} */
    private String id;
    /** 用例名称 */
    private String name;
    /** 执行状态：{@code passed}、{@code failed} 或 {@code skipped} */
    private String status;
    /** 执行耗时（毫秒） */
    private long duration;
    /** 执行详情，成功时为摘要信息，失败时为异常消息 */
    private String details;

    /** 默认无参构造，供 JSON 反序列化使用 */
    public TestResult() {}

    /**
     * 全参构造，根据子进程返回的单条结果创建实例。
     *
     * @param id       用例编号
     * @param name     用例名称
     * @param status   执行状态
     * @param duration 耗时（ms）
     * @param details  详情
     */
    public TestResult(String id, String name, String status, long duration, String details) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.duration = duration;
        this.details = details;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
