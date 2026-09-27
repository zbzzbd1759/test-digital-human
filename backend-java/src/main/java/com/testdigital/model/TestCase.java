package com.testdigital.model;

import java.util.List;

/**
 * 测试用例数据对象，描述一个待执行的自动化测试用例的全部属性。
 * <p>
 * 由 {@code CaseGenerator} 根据功能点模板生成或经 LLM 增强后产出，
 * 后续传递给 {@code ScriptGenerator} 渲染为 Playwright 测试代码。
 * </p>
 */
public class TestCase {
    /** 用例编号，格式为 {@code TC-001}、{@code TC-002} 等三位递增序号 */
    private String id;
    /** 用例名称，格式为「功能点 - 场景描述」 */
    private String name;
    /** 用例分类，如「正向测试」「反向测试」「边界值测试」「性能测试」等 */
    private String category;
    /** 优先级，取值 {@code P0}（最高）~ {@code P2}（最低） */
    private String priority;
    /** 用例描述，说明测试目的 */
    private String description;
    /** 测试步骤列表，按执行顺序排列 */
    private List<String> steps;
    /** 预期结果描述 */
    private String expected;
    /**
     * 用例类型，决定 {@code ScriptGenerator} 使用哪套代码模板渲染。
     * 可选值：{@code functional}、{@code boundary}、{@code security}、
     * {@code stress}、{@code performance}、{@code ui}
     */
    private String type;
    /** 用例来源：{@code local} 表示本地模板生成，{@code llm} 表示 AI 模型生成 */
    private String source;

    /** 默认无参构造，供 JSON 反序列化或手动构建使用 */
    public TestCase() {}

    /**
     * 全参构造，一次性初始化用例的全部属性。
     *
     * @param id          用例编号
     * @param name        用例名称
     * @param category    分类
     * @param priority    优先级
     * @param description 描述
     * @param steps       测试步骤
     * @param expected    预期结果
     * @param type        用例类型
     */
    public TestCase(String id, String name, String category, String priority,
                    String description, List<String> steps, String expected, String type) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.priority = priority;
        this.description = description;
        this.steps = steps;
        this.expected = expected;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
