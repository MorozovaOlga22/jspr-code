package ru.netology;

import java.util.List;
import java.util.Map;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, List<String>> queryParams;
    private final String headers;
    private final String body;
    private final Map<String, List<String>> postParams;

    public Request(String method, String path, Map<String, List<String>> queryParams, String headers, String body, Map<String, List<String>> postParams) {
        this.method = method;
        this.path = path;
        this.queryParams = queryParams;
        this.headers = headers;
        this.body = body;
        this.postParams = postParams;
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

    public Map<String, List<String>> getPostParams() {
        return postParams;
    }

    public List<String> getQueryParam(String name) {
        return queryParams.get(name);
    }

    public List<String> getPostParam(String name) {
        return postParams.get(name);
    }
}