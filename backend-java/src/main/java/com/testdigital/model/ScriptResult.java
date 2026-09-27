package com.testdigital.model;

/**
 * 脚本生成结果数据对象，封装 {@code ScriptGenerator} 生成 Playwright 测试脚本后的输出。
 * <p>
 * 包含脚本文件路径、文件内容及时间戳，供控制器层返回给前端，
 * 同时作为后续 {@code Executor} 执行脚本的输入依据。
 * </p>
 */
public class ScriptResult {
    /** 生成脚本的完整文件路径，如 {@code generated-scripts/TestRunner.java} */
    private String file;
    /** 脚本文件名，如 {@code TestRunner.java} */
    private String fileName;
    /** 脚本源码内容（Java 文本） */
    private String content;
    /** 生成时的毫秒级时间戳，用于唯一标识本次生成 */
    private long timestamp;

    /** 默认无参构造，供 JSON 反序列化使用 */
    public ScriptResult() {}

    /**
     * 全参构造，一次性初始化脚本生成结果。
     *
     * @param file      脚本文件完整路径
     * @param fileName  脚本文件名
     * @param content   脚本源码内容
     * @param timestamp 生成时间戳（ms）
     */
    public ScriptResult(String file, String fileName, String content, long timestamp) {
        this.file = file;
        this.fileName = fileName;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
