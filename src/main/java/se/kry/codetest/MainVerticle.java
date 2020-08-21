package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private Map<String, String> services = new HashMap<>();
    private DBConnector connector;
    private BackgroundPoller poller;

    private String queue = "service-status-update";

    public void onMessage(Message<JsonObject> message) {
        JsonObject body = message.body();
        String url = body.getString(BackgroundPoller.URL);
        if (services.containsKey(url)) {
            services.put(url, body.getString(BackgroundPoller.STATUS));
        }
    }

    @Override
    public void start(Future<Void> startFuture) {
        this.poller = new BackgroundPoller(vertx);
        this.connector = new DBConnector(vertx);
        connector.query("CREATE TABLE IF NOT EXISTS service ("
                + "url VARCHAR(128) NOT NULL PRIMARY KEY,"
                + "name VARCHAR(128),"
                + "timestamp TIMESTAMP"
                + ")").setHandler(done -> {
            System.out.println("in mig");
            if(done.succeeded()){
                System.out.println("completed db migrations");
                System.out.println("in mig suc");
            } else {
                done.cause().printStackTrace();
                System.out.println("in mig fail");
            }
        });
        connector.getAllServices().setHandler(ar -> {
            if (ar.succeeded()) {
                services = ar.result().stream()
                        .collect(Collectors.toMap(
                                s -> s.url,
                                s -> "UNKNOWN"));
            }
        });
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        vertx.eventBus().consumer(queue, this::onMessage);
        vertx.setPeriodic(1000 * 6, timerId -> connector.getAllServices().setHandler(ar -> {
            if (ar.succeeded()) {
                ar.result().forEach(
                        serviceInfo -> services.putIfAbsent(serviceInfo.url, "UNKNOWN"));
                poller.pollServices(services.keySet(), queue);
            }
        }));
        setRoutes(router);
        vertx
                .createHttpServer()
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
            connector.getAllServices().setHandler(ar -> {
                if(ar.succeeded()){
                    List<Service> result = ar.result().stream().map(si -> {
                        si.status = services.getOrDefault(si.url, "UNKNOWN");
                        return si;
                    }).collect(Collectors.toList());
                    req.response()
                            .putHeader("content-type", "application/json")
                            .end(Json.encode(result));
                } else {
                    req.fail(ar.cause());
                }
            });
        });
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            String url = jsonBody.getString("url");
            String name = jsonBody.getString("name");
            if(true){
                connector.addService(url, name);
                req.response()
                        .putHeader("content-type", "text/plain")
                        .end("OK");
            } else {
                req.fail(400);
            }
        });
        router.delete("/service").handler(req -> {
            String url = req.getBodyAsJson().getString("url");
            connector.removeService(url);
            services.remove(url);
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
        router.post("/rename").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            connector.renameService(jsonBody.getString("url"), jsonBody.getString("name"));
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
        router.options("/service").handler(req -> {
            req.response()
                    .putHeader("Access-Control-Allow-Origin", "null")
                    .putHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
                    .putHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With").end("OK");
            ;
        });
        router.options("/rename").handler(req -> {
            req.response()
                    .putHeader("Access-Control-Allow-Origin", "null")
                    .putHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
                    .putHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With").end("OK");
            ;
        });
    }

}



