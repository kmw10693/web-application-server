package webserver;

import db.DataBase;
import http.HttpRequest;
import http.HttpResponse;
import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class UserLogin extends AbstractController {

    private int contentLength;
    private final OutputStream out;

    public UserLogin(OutputStream out) {
        this.out = out;
    }

    public void setContentLength(int length) {
        contentLength = length;
    }

    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {

        BufferedReader br = request.getBuffedReader();

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
    }
    private void response302FailHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: http://localhost:8080/user/login_failed.html\r\n");
            dos.writeBytes("Content-Type: text/html\r\n");
            dos.writeBytes("Set-Cookie: logined=false; Path=/;\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
