package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import se.kry.codetest.domain.ServiceDetail;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private Map<ServiceDetail, String> services = new ConcurrentHashMap<>();
    private DBConnector connector;
    private ServiceDao serviceDao;

    @Override
    public void start(Future<Void> startFuture) {
        connector = new DBConnector(vertx);
        serviceDao = new ServiceDao(connector);

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        loadServices();
        WebClient webClient = WebClient.create(vertx);
        vertx.setPeriodic(1000 * 60, timerId -> BackgroundPoller.pollServices(services, webClient));
        setRoutes(router);
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, result -> {
                    if (result.succeeded()) {
                        System.out.println("KRY code test service started");
                        startFuture.complete();
                    } else {
                        startFuture.fail(result.cause());
                    }
                });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(req -> {
            List<JsonObject> jsonServices = services
                    .entrySet()
                    .stream()
                    .map(service ->
                            new JsonObject()
                                    .put("url", service.getKey().getUrl())
                                    .put("name", service.getKey().getServiceName())
                                    .put("status", service.getValue())
                                    .put("addedOn", service.getKey().getAddedOn()))
                    .collect(Collectors.toList());
            req.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonArray(jsonServices).encode());
        });
        router.delete("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            String url = jsonBody.getString("url");
            for (ServiceDetail serviceDetail : services.keySet()) {
                if(serviceDetail.getUrl().equals(url)){
                    services.remove(serviceDetail);
                    break;
                }
            }
            serviceDao.deleteService(url);
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            String url = jsonBody.getString("url");
            String name = jsonBody.getString("name");
            String addedOn = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
            ServiceDetail serviceDetail = new ServiceDetail(name, url, addedOn);
            services.put(serviceDetail, "UNKNOWN");
            serviceDao.storeService(serviceDetail);
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
    }

    private void loadServices() {
        serviceDao.loadServices().setHandler(event -> {
            if (event.succeeded()) {
                System.out.println("*** Loaded services");
                services.putAll(event.result());
            } else if (event.failed()) {
                System.out.println("*** Unable to load services: " + event.cause().getMessage());
            }
        });
    }

}



