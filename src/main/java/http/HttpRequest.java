package http;

import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private final Map<String, String> header = new HashMap<>();
    private String requestLine;
    private int contentLength;
    private final BufferedReader br;

    public HttpRequest(InputStream in) throws IOException {

        InputStreamReader reader = new InputStreamReader(in);
        br = new BufferedReader(reader);
        String line = br.readLine();
        if (line == null) return;
        requestLine = line;

        while (!(line.equals(""))) {
            line = br.readLine();
            if (line == null || line.isEmpty()) return;

            String[] tokens = line.split(":");
            if (tokens[0].equals("Content-Length")) {
                contentLength = Integer.parseInt(tokens[1].trim());
            }
            header.put(tokens[0], tokens[1].trim());
        }
    }

    public BufferedReader getBuffedReader() {
        return br;
    }

    public String getMethod() {
        String[] tokens = requestLine.split(" ");
        return tokens[0];
    }

    public String getPath() {
        String[] tokens = requestLine.split(" ");
        String[] queryTokens = tokens[1].split("\\?");
        return queryTokens[0];
    }

    public String getHeader(String fieldName) {
        return header.get(fieldName);
    }

    public String getParameter(String fieldName) throws IOException {

        String[] queryTokens;

        if (getMethod().equals("POST")) {
            String data = IOUtils.readData(br, contentLength);
            return HttpRequestUtils.parseQueryString(data).get(fieldName);
        } else {
            String[] tokens = requestLine.split(" ");
            queryTokens = tokens[1].split("\\?");
        }
        return HttpRequestUtils.parseQueryString(queryTokens[1]).get(fieldName);
    }

}
