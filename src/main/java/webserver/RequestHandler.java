package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

import javax.xml.crypto.Data;

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

            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            InputStreamReader reader = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(reader);

            List<String> strings = getLines(br);

            String line = strings.get(0);
            String[] tokens = line.split(" ");

            // POST 방식의 회원가입`
            if (tokens[1].equals("/user/create")) {
                String[] split = strings.get(3).split(": ");
                String s = IOUtils.readData(br, Integer.parseInt(split[1]));
                createUser(s);

                DataOutputStream dos = new DataOutputStream(out);
                response302Header(dos);
            }

            else if (tokens[1].equals("/user/login")) {
                String[] split = strings.get(3).split(": ");
                String s = IOUtils.readData(br, Integer.parseInt(split[1]));

                Map<String, String> map = HttpRequestUtils.parseQueryString(s);

                try {
                    User user = DataBase.findUserById(map.get("userId"));
                    if (user.getPassword().equals(map.get("password"))) {
                        DataOutputStream dos = new DataOutputStream(out);
                        response302LoginHeader(dos);
                    } else {
                        DataOutputStream dos = new DataOutputStream(out);
                        response302FailHeader(dos);
                    }

                } catch (NullPointerException e) {
                    log.error(e.getMessage());
                    DataOutputStream dos = new DataOutputStream(out);
                    response302FailHeader(dos);
                }

            }
            // 사용자 목록 출력
            else if (tokens[1].equals("/user/list")) {
                try {

                    Map<String, String> rawTokens = null;
                    for (String s : strings) {
                        System.out.println(s);
                        String[] token = s.split(": ");
                        if (token[0].equals("Cookie")) {
                            rawTokens = HttpRequestUtils.parseCookies(token[1]);
                        }
                    }
                    String token = rawTokens.get("logined");

                    if (Boolean.parseBoolean(token)) {
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
                    log.error(e.getMessage());
                    DataOutputStream dos = new DataOutputStream(out);
                    responseNotLoginHeader(dos);
                }

            }

            for (String s : strings) {
                System.out.println(s);

                String[] restTokens = s.split(" ");
                if(restTokens[0].endsWith("GET") && restTokens[1].endsWith("css")) {
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] filebody = getFilebody(restTokens);
                    response200cssHeader(dos, filebody.length);
                    responseBody(dos, filebody);
                }
                else if(restTokens[0].endsWith("GET")){
                    System.out.println("=================");
                    System.out.println(restTokens[1]);
                    DataOutputStream dos = new DataOutputStream(out);
                    byte[] filebody = getFilebody(restTokens);
                    response200Header(dos, filebody.length);
                    responseBody(dos, filebody);
                }
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
        body = Files.readAllBytes(new File(LinuxDir + tokens[1]).toPath());
        return body;
    }

    private List<String> getLines(BufferedReader br) {
        List<String> strings = new ArrayList<>();
        try {
            String str;

            while (!(str = br.readLine()).equals("")) {
                strings.add(str);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return strings;
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
