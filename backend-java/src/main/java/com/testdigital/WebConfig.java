package com.testdigital;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Web 配置类，处理 CORS 跨域、静态资源映射及根路径转发。
 * <p>
 * 将 {@code /api/**} 开放跨域访问，将 {@code public/}、{@code reports/}、
 * {@code sample-app/}、{@code generated-scripts/} 等目录映射为静态资源，
 * 使前端可直接通过 HTTP 访问测试报告及被测应用。
 * </p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 开放 {@code /api/**} 接口的 CORS 跨域访问 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("*").allowedMethods("*");
    }

    /** 将根路径 {@code /} 转发到前端入口页 {@code index.html} */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
    }

    /**
     * 配置静态资源处理器，将多个目录映射为 HTTP 可访问路径。
     * <ul>
     *   <li>{@code /} → {@code public/}（前端页面）</li>
     *   <li>{@code /reports/**} → {@code reports/}（测试报告）</li>
     *   <li>{@code /sample-app/**} → {@code sample-app/}（被测应用）</li>
     *   <li>{@code /generated-scripts/**} → {@code generated-scripts/}（生成的脚本）</li>
     * </ul>
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path baseDir = Path.of(System.getProperty("user.dir"));
        String publicDir = "file:" + baseDir.resolve("../public").toAbsolutePath() + "/";
        String reportsDir = "file:" + baseDir.resolve("reports").toAbsolutePath() + "/";
        String sampleAppDir = "file:" + baseDir.resolve("../sample-app").toAbsolutePath() + "/";
        String scriptsDir = "file:" + baseDir.resolve("generated-scripts").toAbsolutePath() + "/";

        registry.addResourceHandler("/*.html", "/*.js", "/*.css", "/*.ico").addResourceLocations(publicDir);
        registry.addResourceHandler("/reports/**").addResourceLocations(reportsDir);
        registry.addResourceHandler("/sample-app/**").addResourceLocations(sampleAppDir);
        registry.addResourceHandler("/generated-scripts/**").addResourceLocations(scriptsDir);
    }
}
