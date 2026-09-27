package com.testdigital;

import com.testdigital.engine.RequestLogService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class RequestLogFilter implements Filter {

    @Autowired
    private RequestLogService logService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;
        String path = httpReq.getRequestURI();

        if (!path.startsWith("/api/") || path.equals("/api/logs")) {
            chain.doFilter(request, response);
            return;
        }

        ServletRequest wrappedRequest = request;
        String body = "";

        if ("POST".equalsIgnoreCase(httpReq.getMethod()) || "PUT".equalsIgnoreCase(httpReq.getMethod())) {
            try {
                CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(httpReq);
                wrappedRequest = wrapper;
                body = wrapper.getCachedBody();
                if (body.length() > 2000) {
                    body = body.substring(0, 2000) + "...(truncated)";
                }
            } catch (Exception e) {
                body = "(read error)";
            }
        }

        RequestLogService.RequestLog log = logService.startRequest(httpReq.getMethod(), path, body);

        try {
            chain.doFilter(wrappedRequest, response);
        } finally {
            logService.finishRequest(log, httpRes.getStatus(), "");
        }
    }
}
