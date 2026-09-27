package com.testdigital.engine;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class RequestLogService {

    private static final int MAX_LOGS = 500;
    private final List<RequestLog> logs = new CopyOnWriteArrayList<>();

    private static final ThreadLocal<RequestLog> CURRENT_LOG = new ThreadLocal<>();

    public RequestLog startRequest(String method, String path, String requestBody) {
        RequestLog log = new RequestLog();
        log.setId(UUID.randomUUID().toString().substring(0, 8));
        log.setMethod(method);
        log.setPath(path);
        log.setRequestBody(requestBody);
        log.setStartTime(Instant.now());
        log.setStatus("processing");
        logs.add(0, log);
        if (logs.size() > MAX_LOGS) {
            logs.remove(logs.size() - 1);
        }
        CURRENT_LOG.set(log);
        return log;
    }

    public void finishRequest(RequestLog log, int statusCode, String responseBody) {
        if (log == null) return;
        log.setStatusCode(statusCode);
        log.setResponseBody(responseBody);
        log.setEndTime(Instant.now());
        log.setDuration(log.getEndTime().toEpochMilli() - log.getStartTime().toEpochMilli());
        log.setStatus(statusCode >= 400 ? "error" : "success");
        CURRENT_LOG.remove();
    }

    public RequestLog getCurrentLog() {
        return CURRENT_LOG.get();
    }

    public List<RequestLog> getLogs() {
        return Collections.unmodifiableList(logs);
    }

    public RequestLog getLog(String id) {
        return logs.stream().filter(l -> l.getId().equals(id)).findFirst().orElse(null);
    }

    public void clearLogs() {
        logs.clear();
    }

    public static class RequestLog {
        private String id;
        private String method;
        private String path;
        private String requestBody;
        private String responseBody;
        private Instant startTime;
        private Instant endTime;
        private long duration;
        private int statusCode;
        private String status;
        private Map<String, Object> details = new LinkedHashMap<>();

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
        public String getResponseBody() { return responseBody; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        public Instant getStartTime() { return startTime; }
        public void setStartTime(Instant startTime) { this.startTime = startTime; }
        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
