package se.kry.codetest;

import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import se.kry.codetest.domain.ServiceDetail;

import java.util.Map;
import java.util.concurrent.Callable;

public class BackgroundPoller {

    private BackgroundPoller() {
    }

    static {
    }

    public static void pollServices(Map<ServiceDetail, String> services, WebClient webClient) {
        System.out.println("Triggered");
        for (Map.Entry<ServiceDetail, String> entry : services.entrySet()) {
            HttpRequest<Buffer> bufferHttpRequest = webClient.getAbs(entry.getKey().getUrl());
            bufferHttpRequest.send(event -> {
                if (event.succeeded()) {
                    int statusCode = event.result().statusCode();
                    System.out.println("URL: " + entry.getKey() + " returned: " + statusCode);
                    if (statusCode >= 200 && statusCode <= 299) {
                        services.put(entry.getKey(), "UP");
                    } else {
                        services.put(entry.getKey(), "DOWN");
                    }
                }else {
                    services.put(entry.getKey(), "DOWN");
                }
            });
        }

    }
}

class PollingResponse {
    private String url;
    private String response;

    public PollingResponse(String url, String response) {
        this.url = url;
        this.response = response;
    }

    public String getUrl() {
        return url;
    }

    public String getResponse() {
        return response;
    }
}

class Poller implements Callable<PollingResponse> {
    private String url;
    private PoolingHttpClientConnectionManager connManager;

    public Poller(String url, PoolingHttpClientConnectionManager connManager) {
        this.url = url;
        this.connManager = connManager;
    }

    @Override
    public PollingResponse call() throws Exception {
        CloseableHttpClient httpclient = HttpClients.custom().setConnectionManager(connManager).build();
        HttpGet httpGet = new HttpGet(url);
        int status = httpclient.execute(httpGet).getStatusLine().getStatusCode();
        System.out.println("Response: " + status + " url: " + url);
        if (status >= 200 && status < 300) {
            return new PollingResponse(url, "SUCCESS");
        }
        return new PollingResponse(url, "UNKNOWN");
    }
}
