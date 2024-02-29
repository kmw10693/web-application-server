package http;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private final Map<String, String> header = new HashMap<>();
    private final FileOutputStream outputStream;

    public HttpResponse(OutputStream out) {
        outputStream = (FileOutputStream) out;
    }

    public void forward(String location) {
        try {
            outputStream.write("HTTP/1.1 200 OK \r\n".getBytes());
            outputStream.write("Content-Type: text/html;charset=utf-8\r\n".getBytes());
            outputStream.write(("Content-Length: " + location.length() + "\r\n").getBytes());
            outputStream.write("\r\n".getBytes());
            outputStream.write(location.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendRedirect(String location) {
        try {
            outputStream.write("HTTP/1.1 302 Found \r\n".getBytes());
            outputStream.write(("Location: http://localhost:8080" + location + "\r\n").getBytes());
            if(!header.isEmpty()) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    outputStream.write((entry.getKey() + ": " + entry.getValue() + "\r\n").getBytes());
                }
            }
            outputStream.write("\r\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHeader(String name, String value) {
        header.put(name, value);
    }


}
