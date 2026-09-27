package com.testdigital.engine;

import com.testdigital.model.TestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 测试用例生成器，根据功能点描述自动生成一组模板化的测试用例。
 * <p>
 * 默认生成 8 个覆盖正向、反向、边界值、安全、压力、性能、UI 等类型的基础用例；
 * 若 {@link LLMService} 已启用，则进一步调用 LLM 对用例进行增强和补充。
 * </p>
 */
@Component
public class CaseGenerator {

    @Autowired
    private LLMService llmService;

    /**
     * 根据功能点生成测试用例列表。
     *
     * @param featurePoint 功能点描述，如「登录」「商品搜索」
     * @param targetUrl    被测页面 URL（当前未直接使用，保留供后续扩展）
     * @return 生成的测试用例列表
     */
    public List<TestCase> generate(String featurePoint, String targetUrl) {
        List<TestCase> baseCases = generateBaseCases(featurePoint);
        if (llmService.isEnabled()) {
            return llmService.enhanceCases(baseCases, featurePoint);
        }
        return baseCases;
    }

    /**
     * 根据功能点生成 8 个基础模板用例，覆盖常见测试类型。
     *
     * @param featurePoint 功能点描述
     * @return 基础用例列表
     */
    private List<TestCase> generateBaseCases(String featurePoint) {
        String fp = featurePoint.trim();
        List<TestCase> cases = new ArrayList<>();
        int id = 1;

        cases.add(new TestCase(
                tcId(id++), fp + " - 基本功能验证", "正向测试", "P0",
                "验证" + fp + "的核心功能是否正常工作",
                Arrays.asList("打开目标页面", "执行" + fp + "的主要操作", "验证操作结果符合预期"),
                fp + "功能正常运行，无报错", "functional"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 页面加载验证", "正向测试", "P0",
                "验证包含" + fp + "的页面能正常加载",
                Arrays.asList("打开目标页面", "等待页面完全加载", "检查页面是否有 JavaScript 错误"),
                "页面正常加载，无控制台错误", "functional"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 空输入测试", "反向测试", "P1",
                "验证" + fp + "在空输入情况下的处理",
                Arrays.asList("打开目标页面", "不输入任何内容直接提交", "验证系统给出合理提示"),
                "系统提示用户输入必要信息，不会崩溃", "boundary"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 特殊字符输入", "反向测试", "P1",
                "验证" + fp + "对特殊字符的处理",
                Arrays.asList("打开目标页面", "输入特殊字符（<script>, SQL注入, emoji等）", "验证系统正确处理"),
                "系统正确处理特殊字符，无 XSS 或注入漏洞", "security"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 超长输入测试", "边界值测试", "P2",
                "验证" + fp + "对超长输入的处理",
                Arrays.asList("打开目标页面", "输入超长字符串（1000+字符）", "验证系统正确处理"),
                "系统能正常处理超长输入，不会崩溃或出现异常", "boundary"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 重复操作测试", "异常测试", "P2",
                "验证" + fp + "在重复操作下的稳定性",
                Arrays.asList("打开目标页面", "快速重复执行同一操作多次", "验证系统状态正确"),
                "系统能正确处理重复操作，无内存泄漏或状态异常", "stress"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 页面响应时间", "性能测试", "P2",
                "验证" + fp + "相关页面的响应时间",
                Arrays.asList("记录页面加载开始时间", "打开目标页面", "等待页面完全加载", "计算加载耗时"),
                "页面加载时间在合理范围内（< 3秒）", "performance"
        ));

        cases.add(new TestCase(
                tcId(id++), fp + " - 元素可见性验证", "UI测试", "P1",
                "验证" + fp + "相关UI元素正确显示",
                Arrays.asList("打开目标页面", "检查关键UI元素是否可见", "验证元素样式是否正确"),
                "所有关键UI元素可见且样式正确", "ui"
        ));

        return cases;
    }

    /**
     * 生成三位递增序号的用例编号，如 {@code TC-001}、{@code TC-002}。
     *
     * @param n 序号
     * @return 格式化的用例编号字符串
     */
    private String tcId(int n) {
        return "TC-" + String.format("%03d", n);
    }
}
