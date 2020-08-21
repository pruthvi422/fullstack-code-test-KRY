package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class DBConnector {

    private final String DB_PATH = "poller.db";
    private final SQLClient client;

    public DBConnector(Vertx vertx){
        JsonObject config = new JsonObject()
                .put("url", "jdbc:sqlite:" + DB_PATH)
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30);

        client = JDBCClient.createShared(vertx, config);
    }

    public Future<ResultSet> query(String query) {
        return query(query, new JsonArray());
    }


    public Future<ResultSet> query(String query, JsonArray params) {
        if(query == null || query.isEmpty()) {
            return Future.failedFuture("Query is null or empty");
        }
        if(!query.endsWith(";")) {
            query = query + ";";
        }

        Future<ResultSet> queryResultFuture = Future.future();

        client.queryWithParams(query, params, result -> {
            if(result.failed()){
                queryResultFuture.fail(result.cause());
                System.out.println(result.cause());
                System.out.println("dsaffads");
            } else {
                queryResultFuture.complete(result.result());
                System.out.println("success");
            }
        });
        System.out.println("here");
        return queryResultFuture;
    }

    public Future<List<Service>> getAllServices(){
        Future<List<Service>> future = Future.future();
        query("SELECT * FROM service").setHandler(ar -> {
            if(ar.succeeded()){
                future.complete(
                        ar.result()
                                .getResults()
                                .stream()
                                .map(json -> new Service(
                                        json.getString(0), json.getString(1), Instant.ofEpochMilli(json.getLong(2))
                                ))
                                .collect(Collectors.toList())
                );
            } else {
                future.fail(ar.cause());
            }
        });
        return future;
    }

    public Future<Void> addService(String service, String name){
        Future<Void> future = Future.future();
        query("INSERT INTO service VALUES (?, ?, ?)", new JsonArray().add(service).add(name).add(Instant.now()) )
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        future.complete();
                    } else {
                        System.out.println(ar.cause());
                        future.fail(ar.cause());
                    }
                });
        return future;
    }

    public Future<Void> renameService(String url, String name) {
        Future<Void> future = Future.future();
        query("UPDATE service SET name = ? WHERE url = ?", new JsonArray().add(name).add(url))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        future.complete();
                    } else {
                        System.out.println(ar.cause());
                        future.fail(ar.cause());
                    }
                });
        return future;
    }

    public Future<Void> removeService(String service){
        Future<Void> future = Future.future();
        query("DELETE FROM service WHERE url = ?", new JsonArray().add(service))
                .setHandler(ar -> {
                    if (ar.succeeded()) {
                        future.complete();
                    } else {
                        System.out.println(ar.cause());
                        future.fail(ar.cause());
                    }
                });
        return future;
    }


}
