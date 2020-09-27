package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    private DBConnector dbConnector;
    @BeforeEach
    void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
        dbConnector = new DBConnector(vertx);
        deleteAllService();
    }

    @Test
    @DisplayName("Start a web server on localhost responding to path /service on port 8080")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void start_http_server(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray body = response.result().bodyAsJsonArray();
                    assertEquals(0, body.size());
                    testContext.completeNow();
                }));
    }

    @Test
    @DisplayName("Add a service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void addService(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(new JsonObject()
                        .put("url","https://www.amazon.co.uk")
                        .put("name","Amazon"), event -> {
                    assertEquals(200, event.result().statusCode());
                });
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray body = response.result().bodyAsJsonArray();
                    assertEquals(1, body.size());
                    testContext.completeNow();
                }));

    }

    @Test
    @DisplayName("Delete a service")
    @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
    void deleteService(Vertx vertx, VertxTestContext testContext) {
        //Create service
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(new JsonObject()
                        .put("url","https://www.amazon.co.uk")
                        .put("name","Amazon"), event -> {
                    assertEquals(200, event.result().statusCode());
                });

        //Delete service
        WebClient.create(vertx)
                .delete(8080, "::1", "/service")
                .sendJsonObject(new JsonObject()
                        .put("url","https://www.amazon.co.uk"), event -> {
                    assertEquals(200, event.result().statusCode());
                });

        //Check count
        WebClient.create(vertx)
                .get(8080, "::1", "/service")
                .send(response -> testContext.verify(() -> {
                    assertEquals(200, response.result().statusCode());
                    JsonArray body = response.result().bodyAsJsonArray();
                    assertEquals(0, body.size());
                    testContext.completeNow();
                }));

    }

    public void deleteAllService() {
         dbConnector.update("DELETE FROM services",null);
    }

}
