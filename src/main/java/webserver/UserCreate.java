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

public class UserCreate extends AbstractController {

    private int contentLength;
    private final OutputStream out;

    public UserCreate(OutputStream outputStream) {
        this.out = outputStream;
    }

    public void setContentLength(int length) {
        contentLength = length;
    }
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {

        HttpResponse httpResponse = new HttpResponse(out);
        BufferedReader br = request.getBuffedReader();

        String data = IOUtils.readData(br, contentLength);
        createUser(data);

        DataOutputStream dos = new DataOutputStream(out);
        response302Header(dos);
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

    private void response302Header(DataOutputStream dos) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: http://localhost:8080/index.html\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
