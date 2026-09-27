# 自动化测试数字人

基于 Spring Boot + Playwright 的端到端自动化测试平台。输入功能点描述，自动生成测试用例、编译执行 Playwright 脚本，并输出 HTML 可视化测试报告。

## 功能特性

- **测试用例生成** — 根据功能点描述自动生成多维度测试用例（功能、边界、安全、性能、UI）
- **脚本自动生成** — 将测试用例编译为可执行的 Playwright Java 脚本
- **自动化执行** — 调用 Playwright Chromium 引擎执行测试脚本
- **HTML 报告** — 生成可视化测试报告，含通过率、耗时、控制台错误等
- **AI 增强（可选）** — 接入大语言模型（通义千问/DeepSeek/Moonshot 等）增强测试用例质量

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.5 |
| 浏览器自动化 | Playwright Java 1.44.0 |
| 构建工具 | Maven |
| HTTP 客户端 | OkHttp 4.12.0 |
| JSON | Gson 2.10.1 |
| Java 版本 | 17+ |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 启动服务

```bash
cd backend-java
mvn spring-boot:run
```

服务启动在 `http://localhost:3456`

### API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/generate-cases` | POST | 根据功能点生成测试用例 |
| `/api/generate-scripts` | POST | 将用例编译为 Playwright 脚本 |
| `/api/execute-scripts` | POST | 执行测试脚本 |
| `/api/generate-report` | POST | 生成 HTML 测试报告 |
| `/api/status` | GET | 查看 LLM 增强状态 |

### 请求示例

```bash
# 生成测试用例
curl -X POST http://localhost:3456/api/generate-cases \
  -H "Content-Type: application/json" \
  -d '{"featurePoint": "用户登录", "targetUrl": "http://localhost:8080"}'
```

## AI 增强模式

在 `application.properties` 中配置：

```properties
llm.enabled=true
llm.api.key=your-api-key
llm.api.url=https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
llm.model=qwen-plus
```

支持的模型服务：
- **通义千问** (DashScope) — 阿里云
- **DeepSeek** — deepseek.com
- **Moonshot (Kimi)** — moonshot.cn
- 任何 OpenAI 兼容 API

## 项目结构

```
├── backend-java/
│   ├── src/main/java/com/testdigital/
│   │   ├── Application.java          # 启动入口
│   │   ├── WebConfig.java            # 静态资源配置
│   │   ├── controller/
│   │   │   └── TestController.java   # REST API
│   │   ├── engine/
│   │   │   ├── CaseGenerator.java    # 用例生成引擎
│   │   │   ├── ScriptGenerator.java  # 脚本生成器
│   │   │   ├── Executor.java         # 脚本执行引擎
│   │   │   ├── Reporter.java         # 报告生成器
│   │   │   └── LLMService.java       # AI 增强服务
│   │   └── model/                    # 数据模型
│   └── pom.xml
├── public/
│   └── index.html                    # 前端界面
└── sample-app/
    └── index.html                    # 示例应用
```

## License

MIT
