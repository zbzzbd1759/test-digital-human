package com.testdigital.engine;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.testdigital.model.TestCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class LLMService {

    @Value("${llm.api.key:}")
    private String apiKey;

    @Value("${llm.model:qwen-plus}")
    private String model;

    @Value("${llm.enabled:false}")
    private boolean enabled;

    private final Gson gson = new Gson();

    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public List<TestCase> enhanceCases(List<TestCase> baseCases, String featurePoint) {
        if (!isEnabled()) {
            return baseCases;
        }

        try {
            String prompt = buildPrompt(baseCases, featurePoint);
            String response = callDashScope(prompt);
            return parseEnhancedCases(response, baseCases);
        } catch (Exception e) {
            System.err.println("DashScope调用失败，使用基础用例: " + e.getMessage());
            return baseCases;
        }
    }

    private String buildPrompt(List<TestCase> baseCases, String featurePoint) {
        String casesText = baseCases.stream()
                .map(tc -> "- %s: %s [%s]\n  描述: %s".formatted(tc.getId(), tc.getName(), tc.getType(), tc.getDescription()))
                .collect(Collectors.joining("\n"));

        return """
                你是一个资深的软件测试工程师。请基于以下功能点和初始测试用例，优化并增强测试用例。

                功能点: %s

                初始测试用例:
                %s

                请:
                1. 优化现有用例的步骤和预期结果，使其更具体、更可执行
                2. 补充遗漏的重要测试场景（如异常处理、并发、兼容性等）
                3. 返回JSON数组格式，每个用例包含: id, name, category, priority, description, steps(数组), expected, type
                4. type必须是以下之一: functional, boundary, security, stress, performance, ui
                5. 保持用例数量在8-12个之间

                只返回JSON，不要其他内容。
                """.formatted(featurePoint, casesText);
    }

    private String callDashScope(String prompt) throws Exception {
        Generation gen = new Generation();

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

        GenerationResult result = gen.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent();
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
