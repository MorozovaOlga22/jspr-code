package ru.netology;

import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    static Request getRequest(BufferedReader in) throws IOException {
        // read only request line for simplicity
        // must be in form GET /path HTTP/1.1
        final String requestLine = in.readLine();
        final String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            // just close socket
            return null;
        }

        final StringBuilder headers = new StringBuilder();
        final StringBuilder body = new StringBuilder();
        boolean hasBody = false;

        String inputLine = in.readLine();
        while (inputLine.length() > 0) {
            headers.append(inputLine);
            if (inputLine.startsWith("Content-Length: ")) {
                int index = inputLine.indexOf(':') + 1;
                String len = inputLine.substring(index).trim();
                if (Integer.parseInt(len) > 0) {
                    hasBody = true;
                }
            }
            inputLine = in.readLine();
        }

        if (hasBody) {
            inputLine = in.readLine();
            while (inputLine != null && inputLine.length() > 0) {
                body.append(inputLine);
                inputLine = in.readLine();
            }
        }

        final String path = parts[1];
        return new Request(parts[0], getCleanPath(path), getParams(path), headers.toString(), body.toString());
    }

    private static String getCleanPath(String path) {
        if (path.contains("?")) {
            return path.substring(0, path.indexOf("?"));
        }
        return path;
    }

    private static Map<String, List<String>> getParams(String path) {
        if (!path.contains("?")) {
            return Collections.emptyMap();
        }
        final String params = path.substring(path.indexOf("?") + 1);
        final HashMap<String, List<String>> paramsMap = new HashMap<>();
        URLEncodedUtils.parse(params, StandardCharsets.UTF_8)
                .forEach(param -> paramsMap.computeIfAbsent(param.getName(), anything -> new ArrayList<>()).add(param.getValue()));
        return paramsMap;
    }
}