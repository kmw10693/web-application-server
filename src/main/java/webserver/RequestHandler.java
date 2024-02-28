package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.*;

import db.DataBase;
import http.HttpRequest;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final String LinuxDir = "./webapp";

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {

            HttpRequest httpRequest = new HttpRequest(in);
            BufferedReader br = httpRequest.getBuffedReader();

            String line = br.readLine();

            if (line == null) {
                return;
            }

            String[] tokens = line.split(" ");
            int contentLength = 0;
            boolean isChecked = false;

            while (!line.equals("")) {
                line = br.readLine();

                if (line.contains("Content-Length")) {
                    String[] contents = line.split(":");
                    contentLength = Integer.parseInt(contents[1].trim());
                }

                if (line.contains("Cookie")) {
                    String[] contents = line.split(":");
                    Map<String, String> cookies = HttpRequestUtils.parseCookies(contents[1].trim());
                    String value = cookies.get("logined");
                    if (value == null) {
                        isChecked = false;
                    } else isChecked = Boolean.parseBoolean(value);
                }
            }
            String url = tokens[1];

            if (url.equals("/user/create")) {
                String data = IOUtils.readData(br, contentLength);
                createUser(data);

                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos);
            } else if (url.equals("/user/login")) {
                String data = IOUtils.readData(br, contentLength);
                Map<String, String> userQuery = HttpRequestUtils.parseQueryString(data);

                try {
                    User user = DataBase.findUserById(userQuery.get("userId"));

                    if (user.getPassword().equals(userQuery.get("password"))) {

                        DataOutputStream dos = new DataOutputStream(out);
                        response302LoginHeader(dos);

                    } else {
                        DataOutputStream dos = new DataOutputStream(out);
                        response302FailHeader(dos);
                    }

                } catch (NullPointerException e) {
                    DataOutputStream dos = new DataOutputStream(out);
                    response302FailHeader(dos);
                }
            } else if (url.equals("/user/list")) {
                try {

                    if (isChecked) {
                        StringBuilder sb = new StringBuilder();
                        Collection<User> users = DataBase.findAll();

                        sb.append("<table border='1'>");
                        for (User user : users) {
                            sb.append("<tr>");
                            sb.append("<td>" + user.getUserId() + "</td>");
                            sb.append("<td>" + user.getName() + "</td>");
                            sb.append("<td>" + user.getEmail() + "</td>");
                            sb.append("</tr>");
                        }
                        sb.append("</table>");
                        byte[] body = sb.toString().getBytes();
                        DataOutputStream dos = new DataOutputStream(out);
                        response200Header(dos, body.length);
                        responseBody(dos, body);
                    }

                    DataOutputStream dos = new DataOutputStream(out);
                    responseNotLoginHeader(dos);

                } catch (NullPointerException e) {
                    DataOutputStream dos = new DataOutputStream(out);
                    responseNotLoginHeader(dos);
                }

            } else if (url.endsWith("css")) {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = getFilebody(tokens);
                response200cssHeader(dos, body.length);
                responseBody(dos, body);
            } else {
                DataOutputStream dos = new DataOutputStream(out);
                byte[] body = getFilebody(tokens);
                response200Header(dos, body.length);
                responseBody(dos, body);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void createUser(String params) {
        Map<String, String> stringStringMap = HttpRequestUtils.parseQueryString(params);

        User user = new User(
                stringStringMap.get("userId"),
                stringStringMap.get("password"),
                stringStringMap.get("name"),
                stringStringMap.get("email")
        );
        DataBase.addUser(user);
    }

    private String getParams(String[] tokens) {
        int index = tokens[1].indexOf("?");
        String requestPath = tokens[1].substring(0, index);
        return tokens[1].substring(index + 1);
    }

    private byte[] getFilebody(String[] tokens) throws IOException {
        byte[] body;
        body = Files.readAllBytes(new File("./webapp" + tokens[1]).toPath());
        return body;
    }


    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response200cssHeader(DataOutputStream dos, int lengthOfBodyContent) {


        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/css;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: http://localhost:8080/index.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302LoginHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: http://localhost:8080/index.html\r\n");
            dos.writeBytes("Content-Type: text/html\r\n");
            dos.writeBytes("Set-Cookie: logined=true; Path=/; \r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302FailHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: http://localhost:8080/user/login_failed.html\r\n");
            dos.writeBytes("Content-Type: text/html\r\n");
            dos.writeBytes("Set-Cookie: logined=false; Path=/;\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseNotLoginHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: http://localhost:8080/user/login.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
