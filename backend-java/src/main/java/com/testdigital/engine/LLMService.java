package com.testdigital.engine;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.testdigital.model.TestCase;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LLM 服务组件，提供基于大语言模型的测试用例增强能力。
 * <p>
 * 通过 OkHttp 调用 OpenAI 兼容 API，将基础用例和功能点描述发送给 LLM，
 * 请求其优化步骤、补充场景并返回 JSON 格式的增强用例列表。
 * 当未配置 API Key 或 {@code llm.enabled=false} 时自动降级，返回原始基础用例。
 * </p>
 */
@Component
public class LLMService {

    /** LLM API 密钥，通过 {@code llm.api.key} 配置，未配置时为空 */
    @Value("${llm.api.key:}")
    private String apiKey;

    /** LLM API 地址，默认 OpenAI 端点 */
    @Value("${llm.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    /** 使用的模型名称，默认 {@code gpt-4o-mini} */
    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    /** 是否启用 LLM 增强，默认关闭 */
    @Value("${llm.enabled:false}")
    private boolean enabled;

    private final OkHttpClient client;
    private final Gson gson = new Gson();

    /** 初始化 OkHttp 客户端，设置连接 / 读 / 写超时 */
    public LLMService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 判断 LLM 服务是否可用（已启用且配置了 API Key）。
     *
     * @return 可用时返回 {@code true}
     */
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    /**
     * 调用 LLM 对基础用例进行增强。失败时降级返回原始用例。
     *
     * @param baseCases    基础用例列表
     * @param featurePoint 功能点描述
     * @return 增强后的用例列表，或原始列表（降级时）
     */
    public List<TestCase> enhanceCases(List<TestCase> baseCases, String featurePoint) {
        if (!isEnabled()) {
            return baseCases;
        }

        try {
            String prompt = buildPrompt(baseCases, featurePoint);
            String response = callLLM(prompt);
            return parseEnhancedCases(response, baseCases);
        } catch (Exception e) {
            System.err.println("LLM增强失败，使用基础用例: " + e.getMessage());
            return baseCases;
        }
    }

    /** 构建发送给 LLM 的提示词，包含功能点和基础用例信息 */
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

    /** 调用 LLM API 并返回响应文本 */
    private String callLLM(String prompt) throws IOException {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);
        requestBody.add("messages", gson.toJsonTree(List.of(message)));
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 4000);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM API返回错误: " + response.code() + " " + response.body().string());
            }
            JsonObject responseBody = gson.fromJson(response.body().string(), JsonObject.class);
            return responseBody.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
        }
    }

    /** 解析 LLM 返回的 JSON 文本为 TestCase 列表 */
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

    /**
     * 从 LLM 响应文本中提取 JSON 数组。
     * 支持处理 Markdown 代码块包裹的格式。
     *
     * @param response LLM 原始响应
     * @return 提取后的 JSON 数组字符串
     */
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
