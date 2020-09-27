package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

public class DBConnector {

    private final String DB_PATH = "poller.db";
    // SQL statement for creating a new table
    public static final String SQL = "CREATE TABLE IF NOT EXISTS services (\n"
            + "	name text NOT NULL,\n"
            + "	url text PRIMARY KEY,\n"
            + "	createdAt text NOT NULL\n"
            + ");";
    private final SQLClient client;

    public DBConnector(Vertx vertx) {
        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + DB_PATH)
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30);

        client = JDBCClient.createShared(vertx, config);
        setupTables();
    }

    public void setupTables() {
        client.getConnection(event -> {
            if (event.succeeded()) {
                SQLConnection connection = event.result();
                connection.execute(SQL, sqlEvent -> {
                    if (sqlEvent.succeeded()) {
                        System.out.println("Database setup done");
                    } else {
                        System.out.println("Error while DB setup" + sqlEvent.cause());
                    }
                });
                connection.close();
            }
        });
    }

    public Future<ResultSet> query(String query) {
        return query(query, new JsonArray());
    }

    public Future<UpdateResult> update(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Query can't be empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }
        Future<UpdateResult> queryResultFuture = Future.future();
        client.updateWithParams(query, params, result -> {
            if (result.failed()) {
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
                System.out.println("Database update successful");
            }
        });
        return queryResultFuture;
    }

    public Future<ResultSet> query(String query, JsonArray params) {
        if (query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if (!query.endsWith(";")) {
            query = query + ";";
        }
        Future<ResultSet> queryResultFuture = Future.future();
        client.queryWithParams(query, params, result -> {
            if (result.failed()) {
                System.out.println("query failed " + result.cause());
                queryResultFuture.fail(result.cause());
            } else {
                queryResultFuture.complete(result.result());
            }
        });
        return queryResultFuture;
    }


}
