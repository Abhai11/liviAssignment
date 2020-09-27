package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import se.kry.codetest.domain.ServiceDetail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceDao {

    public static final String INSERT_INTO_SERVICES = "INSERT INTO services(name,url,createdAt) VALUES(?,?,?)";
    public static final String SELECT_ALL_SERVICES = "SELECT * FROM services";
    public static final String DELETE_SERVICE = "DELETE FROM services where url = ?";

    private final DBConnector dbConnector;

    public ServiceDao(DBConnector dbConnector) {
        this.dbConnector = dbConnector;
    }

    public void storeService(ServiceDetail serviceDetail) {
        JsonArray array = new JsonArray();
        array.add(serviceDetail.getServiceName());
        array.add(serviceDetail.getUrl());
        array.add(serviceDetail.getAddedOn());
        dbConnector.update(INSERT_INTO_SERVICES, array);
    }

    public Future<UpdateResult> deleteService(String url) {
        JsonArray array = new JsonArray();
        array.add(url);
        Future<UpdateResult> future = Future.future();
        Future<UpdateResult> resultFuture = dbConnector.update(DELETE_SERVICE, array);
        resultFuture.setHandler(event -> {
            if (event.succeeded()){
                future.complete(event.result());
            }else{
                future.fail(event.cause());
            }
        });
        return future;
    }

    public Future<Map<ServiceDetail, String>> loadServices() {
        Future<Map<ServiceDetail, String>> future = Future.future();
        Map<ServiceDetail, String> services = new HashMap<>();
        Future<ResultSet> resultSetFuture = dbConnector.query(SELECT_ALL_SERVICES);
        resultSetFuture.setHandler(event -> {
            if (event.succeeded()) {
                ResultSet resultSet = event.result();
                List<JsonArray> data = resultSet.getResults();
                for (JsonArray vals : data) {
                    String name = vals.getString(0);
                    String url = vals.getString(1);
                    String addedOn = vals.getString(2);
                    services.put(new ServiceDetail(name, url, addedOn), "UNKNOWN");
                }
                future.complete(services);
            } else {
                future.fail(event.cause().getMessage());
            }
        });
        return future;
    }
}
