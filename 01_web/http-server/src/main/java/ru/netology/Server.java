package ru.netology;

import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final List<String> VALID_PATHS = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private static final int PORT = 9999;
    private static final int THREADS_COUNT = 64;

    private final Map<String, Handler> handlerMap = new ConcurrentHashMap<>();

    @SuppressWarnings("InfiniteLoopStatement")
    public void start() {
        final ExecutorService executorService = Executors.newFixedThreadPool(THREADS_COUNT);
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try {
                    final Socket socket = serverSocket.accept();
                    executorService.submit(getServerTask(socket));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        handlerMap.put(method + " " + path, handler);
    }

    private Runnable getServerTask(final Socket socket) {
        return () -> {
            try {
                handleConnection(socket);
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    private void handleConnection(final Socket socket) throws IOException {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            final Request request = getRequest(in);
            if (request == null) {
                // just close socket
                return;
            }

            final Handler handler = handlerMap.get(request.getMethod() + " " + request.getPath());
            if (handler == null) {
                if (!VALID_PATHS.contains(request.getPath())) {
                    makeNotFoundResponse(out);
                } else {
                    makeResponseWithContent(out, request.getPath());
                }
            } else {
                handler.handle(request, out);
            }
            out.flush();
        }
    }

    private Request getRequest(BufferedReader in) throws IOException {
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
        String contentType = "";

        String inputLine = in.readLine();
        int bodyLen = 0;
        while (inputLine.length() > 0) {
            headers.append(inputLine);
            if (inputLine.startsWith("Content-Length: ")) {
                int index = inputLine.indexOf(':') + 1;
                bodyLen = Integer.parseInt(inputLine.substring(index).trim());
                if (bodyLen > 0) {
                    hasBody = true;
                }
            } else if (inputLine.startsWith("Content-Type: ")) {
                int index = inputLine.indexOf(':') + 1;
                contentType = inputLine.substring(index).trim();
            }
            inputLine = in.readLine();
        }

        if (hasBody) {
            final char[] bodyBuff = new char[bodyLen];
            in.read(bodyBuff);
            body.append(bodyBuff);
        }

        final String path = parts[1];
        final String bodyStr = body.toString();
        return new Request(parts[0], getCleanPath(path), getQueryParams(path), headers.toString(), bodyStr, getPostParams(bodyStr, contentType));
    }

    private String getCleanPath(String path) {
        if (path.contains("?")) {
            return path.substring(0, path.indexOf("?"));
        }
        return path;
    }

    private Map<String, List<String>> getQueryParams(String path) {
        if (!path.contains("?")) {
            return Collections.emptyMap();
        }
        final String params = path.substring(path.indexOf("?") + 1);
        return getParams(params);
    }

    private HashMap<String, List<String>> getParams(String params) {
        final HashMap<String, List<String>> paramsMap = new HashMap<>();
        URLEncodedUtils.parse(params, StandardCharsets.UTF_8)
                .forEach(param -> paramsMap.computeIfAbsent(param.getName(), anything -> new ArrayList<>()).add(param.getValue()));
        return paramsMap;
    }

    private Map<String, List<String>> getPostParams(String body, String contentType) {
        if (!"application/x-www-form-urlencoded".equals(contentType)) {
            return new HashMap<>();
        }
        return getParams(body);
    }

    private void makeNotFoundResponse(BufferedOutputStream out) throws IOException {
        final ResponseData responseData = new ResponseData(404, "Not Found", null, 0);
        writeStatusAndHeaders(responseData, out);
    }

    private void writeStatusAndHeaders(final ResponseData data, final BufferedOutputStream out) throws IOException {
        final StringBuilder respBuilder = new StringBuilder();
        respBuilder.append("HTTP/1.1 ").append(data.getCode()).append(" ").append(data.getDescription()).append("\r\n");
        if (data.getContentType() != null) {
            respBuilder.append("Content-Type: ").append(data.getContentType()).append("\r\n");
        }
        respBuilder.append("Content-Length: ").append(data.getContentLength()).append("\r\n");
        respBuilder.append("Connection: close\r\n");
        respBuilder.append("\r\n");

        out.write(respBuilder.toString().getBytes());
    }

    private void makeResponseWithContent(BufferedOutputStream out, String path) throws IOException {
        final Path filePath = Path.of(".", "public", path);
        final String mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (path.equals("/classic.html")) {
            final String template = Files.readString(filePath);
            final byte[] content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            final ResponseData responseData = new ResponseData(200, "OK", mimeType, content.length);
            writeStatusAndHeaders(responseData, out);
            out.write(content);
        } else {
            final long length = Files.size(filePath);
            final ResponseData responseData = new ResponseData(200, "OK", mimeType, length);
            writeStatusAndHeaders(responseData, out);
            Files.copy(filePath, out);
        }
    }
}