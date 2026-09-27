package com.testdigital.engine;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.testdigital.model.TestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LLMService {

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.model:qwen-plus}")
    private String model;

    @Value("${llm.enabled:false}")
    private boolean enabled;

    @Autowired
    private RequestLogService requestLogService;

    private final Gson gson = new Gson();

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public List<TestCase> enhanceCases(List<TestCase> baseCases, String featurePoint) {
        if (!isEnabled()) {
            System.out.println("[LLMService] LLM 未启用，使用本地模板用例");
            return baseCases;
        }

        System.out.println("[LLMService] 开始调用 DashScope 增强用例，功能点: " + featurePoint);
        try {
            String prompt = buildPrompt(baseCases, featurePoint);
            String response = callDashScope(prompt);
            List<TestCase> enhanced = parseEnhancedCases(response, baseCases);
            System.out.println("[LLMService] 增强完成，生成 " + enhanced.size() + " 个用例");
            return enhanced;
        } catch (Exception e) {
            System.err.println("[LLMService] DashScope调用失败，使用基础用例: " + e.getMessage());
            e.printStackTrace();
            return baseCases;
        }
    }

    private String buildPrompt(List<TestCase> baseCases, String featurePoint) {
        String casesText = baseCases.stream()
                .map(tc -> "- %s: %s [%s]\n  描述: %s".formatted(tc.getId(), tc.getName(), tc.getType(), tc.getDescription()))
                .collect(Collectors.joining("\n"));

        return """
                你是一名拥有10年以上经验的资深QA工程师，精通黑盒测试方法论（等价类划分、边界值分析、因果图、判定表、正交实验法），
                同时具备安全测试、性能测试和稳定性测试的实战经验。
                请基于以下功能点和初始测试用例，以专业测试思维优化并增强测试用例集。
                
                功能点: %s
                
                初始测试用例:
                %s
                
                【第一步：场景识别】
                请先分析该功能点属于以下哪种场景类型（可多选），然后针对性地设计用例：
                
                A. 表单交互型 — 登录、注册、搜索、数据录入等含输入控件的场景
                B. 页面导航型 — 多页面跳转、路由切换、Tab切换、面包屑导航等
                C. 数据展示型 — 列表、表格、Dashboard、报告、图表渲染等
                D. 业务流程型 — 下单、支付、审批流、多步骤向导等有状态流转的场景
                E. 文件操作型 — 文件上传/下载、导入/导出、图片预览等
                F. 弹窗/交互型 — Modal弹窗、Toast提示、拖拽、右键菜单等交互组件
                
                【第二步：按场景类型针对性设计】
                根据识别出的场景类型，重点覆盖以下对应关注点：
                
                ▶ A. 表单交互型
                  - 正向：各字段有效值输入 → 提交成功 → 结果反馈闭环
                  - 反向：空值、非法格式（邮箱不含@、手机号含字母）、超长度输入
                  - 校验：前端实时校验提示 + 提交后服务端校验兜底
                  - 状态：禁用/只读态是否可交互、表单重置后数据是否清空
                  - 安全：输入框 XSS/SQL 注入 payload 是否被转义
                  - 体验：Tab 键焦点顺序、回车提交、防重复提交
                
                ▶ B. 页面导航型
                  - 正向：各导航路径可达，URL 与页面内容匹配
                  - 后退/前进：浏览器前进后退后状态是否正确
                  - 深链接：直接访问子页面 URL 是否正常加载
                  - 异常：访问不存在的路由是否 404 或重定向
                  - 性能：页面切换耗时、是否有白屏闪烁
                  - 状态保持：导航后返回原页面，表单数据/滚动位置是否保留
                
                ▶ C. 数据展示型
                  - 正向：数据正确渲染，字段完整无截断
                  - 空态：无数据时展示空状态提示而非白屏
                  - 加载态：数据加载中是否有 loading 指示器
                  - 边界：0条/1条/大量数据（100+条）的渲染表现
                  - 排序/筛选：排序方向正确、筛选条件组合有效
                  - 性能：大数据量列表滚动是否流畅、是否有重复请求
                
                ▶ D. 业务流程型
                  - 正向：完整流程从起点到终点每一步均可正常推进
                  - 中断恢复：流程中途刷新页面/关闭浏览器后重新进入是否可继续
                  - 幂等性：重复提交同一操作是否产生重复数据
                  - 状态流转：各状态间的流转路径正确，禁止逆向流转（如已取消不可再审批）
                  - 并发：同一数据被多人同时操作时的一致性
                  - 回滚：流程取消或失败后数据是否正确回滚
                
                ▶ E. 文件操作型
                  - 正向：合法文件上传/下载成功，内容完整
                  - 格式限制：不支持的文件类型是否拒绝并提示
                  - 大小限制：超大文件是否拦截，边界值（恰好等于上限）
                  - 异常：上传中断网、上传同名文件（覆盖/拒绝）
                  - 安全：上传文件含恶意脚本是否被检测
                  - 预览：图片/PDF 预览是否正确渲染
                
                ▶ F. 弹窗/交互型
                  - 正向：触发条件正确 → 弹窗/交互出现 → 操作后关闭
                  - 遮罩层：点击遮罩是否关闭、弹窗下层是否可操作（应不可）
                  - 嵌套：多弹窗叠加时层级是否正确
                  - 键盘：Esc 关闭弹窗、焦点陷阱（Tab 不会跳出弹窗）
                  - 动画：展开/收起动画流畅，无残影
                  - 响应式：不同屏幕尺寸下弹窗布局正确
                
                【第三步：通用维度补充】
                无论哪种场景类型，均需考虑以下通用维度：
                
                - 安全性：XSS 注入、SQL 注入、敏感信息泄露、权限校验
                - 性能：页面加载 < 3s、交互响应 < 500ms、无死循环/重复请求
                - 稳定性：高频操作幂等性、异常中断后恢复、长时间运行无泄漏
                
                【优先级分配原则】
                - P0：主流程功能验证，阻塞性用例
                - P1：重要异常流处理、安全基础校验、关键交互验证
                - P2：边界值、性能指标、稳定性、兼容性
                
                【输出格式】
                返回JSON数组，每个用例包含以下字段：
                {
                  "id": "TC-001",
                  "name": "功能点 - 场景描述",
                  "category": "正向测试",
                  "priority": "P0",
                  "description": "测试目的",
                  "steps": ["步骤1", "步骤2"],
                  "expected": "明确的、可判定的预期结果",
                  "type": "functional"
                }
                
                字段约束：
                - category 可选：正向测试/反向测试/边界值测试/安全测试/性能测试/稳定性测试/UI测试
                - type 可选：functional/boundary/security/stress/performance/ui
                - 用例总数 8-12 个，确保场景针对性 + 通用维度覆盖
                
                只返回JSON数组，不要包含任何解释、Markdown标记或其他文字。
                """.formatted(featurePoint, casesText);
    }

    private String callDashScope(String prompt) throws Exception {
        Generation gen = new Generation();

        System.out.println("========== DashScope API 调用 ==========");
        System.out.println("模型: " + model);
        System.out.println("API Key: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "***");
        System.out.println("Prompt 长度: " + prompt.length() + " 字符");

        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个专业的软件测试工程师AI助手。")
                .build();

        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(prompt)
                .build();

        GenerationParam param = GenerationParam.builder()
                .model(model)
                .messages(List.of(systemMsg, userMsg))
                .apiKey(apiKey)
                .temperature(0.7f)
                .maxTokens(4000)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        long startTime = System.currentTimeMillis();
        GenerationResult result = gen.call(param);
        long duration = System.currentTimeMillis() - startTime;

        String content = result.getOutput().getChoices().get(0).getMessage().getContent();
        String requestId = result.getRequestId();

        System.out.println("Request ID: " + requestId);
        System.out.println("调用耗时: " + duration + "ms");
        if (result.getUsage() != null) {
            System.out.println("Tokens - 输入: " + result.getUsage().getInputTokens()
                    + ", 输出: " + result.getUsage().getOutputTokens()
                    + ", 总计: " + result.getUsage().getTotalTokens());
        }
        System.out.println("响应长度: " + content.length() + " 字符");
        System.out.println("=========================================");

        recordLLMCallDetails(result, duration, requestId);

        return content;
    }

    private void recordLLMCallDetails(GenerationResult result, long duration, String requestId) {
        RequestLogService.RequestLog currentLog = requestLogService.getCurrentLog();
        if (currentLog == null) return;

        Map<String, Object> llmDetails = new LinkedHashMap<>();
        llmDetails.put("model", model);
        llmDetails.put("requestId", requestId);
        llmDetails.put("duration", duration);

        if (result.getUsage() != null) {
            Map<String, Object> tokens = new LinkedHashMap<>();
            tokens.put("input", result.getUsage().getInputTokens());
            tokens.put("output", result.getUsage().getOutputTokens());
            tokens.put("total", result.getUsage().getTotalTokens());
            llmDetails.put("tokens", tokens);
        }

        currentLog.getDetails().put("llm", llmDetails);
    }

    private List<TestCase> parseEnhancedCases(String response, List<TestCase> baseCases) {
        String json = extractJson(response);
        JsonObject[] cases = gson.fromJson(json, JsonObject[].class);

        List<TestCase> result = new ArrayList<>();
        for (int i = 0; i < cases.length; i++) {
            JsonObject obj = cases[i];
            TestCase tc = new TestCase();
            tc.setId(obj.has("id") ? obj.get("id").getAsString() : "TC-" + String.format("%03d", i + 1));
            tc.setName(obj.has("name") ? obj.get("name").getAsString() : "未命名用例");
            tc.setCategory(obj.has("category") ? obj.get("category").getAsString() : "正向测试");
            tc.setPriority(obj.has("priority") ? obj.get("priority").getAsString() : "P1");
            tc.setDescription(obj.has("description") ? obj.get("description").getAsString() : "");
            tc.setExpected(obj.has("expected") ? obj.get("expected").getAsString() : "");
            tc.setType(obj.has("type") ? obj.get("type").getAsString() : "functional");

            List<String> steps = new ArrayList<>();
            if (obj.has("steps")) {
                obj.get("steps").getAsJsonArray().forEach(s -> steps.add(s.getAsString()));
            }
            tc.setSteps(steps);
            tc.setSource("llm");

            result.add(tc);
        }
        return result;
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n") + 1;
            int end = trimmed.lastIndexOf("```");
            if (end > start) {
                trimmed = trimmed.substring(start, end).trim();
            }
        }
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7).trim();
        }
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
