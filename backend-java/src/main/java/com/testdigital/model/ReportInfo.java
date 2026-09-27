package com.testdigital.model;

/**
 * 测试报告信息模型，封装单次测试执行后生成的 HTML 报告元数据。
 * <p>
 * 由 {@code Reporter} 组件在渲染 HTML 报告后构建，作为 REST API 响应体的一部分返回给调用方，
 * 包含报告文件定位信息与测试通过率的汇总数据。
 * </p>
 */
public class ReportInfo {
    /** 报告文件名，例如 {@code report-1790265098470.html} */
    private String fileName;
    /** 报告文件在服务器上的绝对路径 */
    private String filePath;
    /** 报告文件的 HTTP 访问 URL，供前端直接打开查看 */
    private String url;
    /** 测试汇总信息，包含总用例数、通过数、失败数等统计 */
    private TestSummary summary;
    /** 测试通过率，取值范围 [0.0, 100.0]，保留一位小数 */
    private double passRate;

    /** 无参构造，供框架反序列化或手动构建使用 */
    public ReportInfo() {}

    /**
     * 全参构造，一次性初始化报告的全部元数据。
     *
     * @param fileName 报告文件名
     * @param filePath 报告文件绝对路径
     * @param url      报告 HTTP 访问地址
     * @param summary  测试汇总统计
     * @param passRate 通过率（0.0 ~ 100.0）
     */
    public ReportInfo(String fileName, String filePath, String url, TestSummary summary, double passRate) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.url = url;
        this.summary = summary;
        this.passRate = passRate;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public TestSummary getSummary() { return summary; }
    public void setSummary(TestSummary summary) { this.summary = summary; }

    public double getPassRate() { return passRate; }
    public void setPassRate(double passRate) { this.passRate = passRate; }
}
