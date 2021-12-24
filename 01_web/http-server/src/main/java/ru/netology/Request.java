package ru.netology;

import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, List<String>> queryParams;
    private final String headers;
    private final String body;

    public Request(String method, String path, Map<String, List<String>> queryParams, String headers, String body) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }

    public String getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public List<String> getQueryParam(String name) {
        return queryParams.get(name);
    }
}