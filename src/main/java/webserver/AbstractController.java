package webserver;

import http.HttpRequest;
import http.HttpResponse;

import java.io.IOException;


public abstract class AbstractController implements Controller {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
	// POST 요청시 doPost, GET 요청시 doGet
    }
    public void doGet(HttpRequest request, HttpResponse response) throws IOException {

    }

    public void doPost(HttpRequest request, HttpResponse response) throws IOException {

    }

}
