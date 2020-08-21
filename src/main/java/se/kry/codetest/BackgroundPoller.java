package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import java.util.Collection;

public class BackgroundPoller {

    public static final String URL = "url";
    public static final String STATUS = "status";

    private final WebClient client;
    private final Vertx vertx;

    public BackgroundPoller(Vertx vertx) {
        this.vertx = vertx;
        this.client = WebClient.create(vertx);
    }

    public void pollServices(Collection<String> services, String address) {
        services.forEach(
                (service) -> client.getAbs(service)
                        .send(ar -> {
                            JsonObject reply = new JsonObject().put(URL, service);
                            if(ar.succeeded()){
                                vertx.eventBus().send(address, reply.put(STATUS, "OK"));
                            }
                            else {
                                System.out.println(ar.cause().getMessage());
                                vertx.eventBus().send(address, reply.put(STATUS, "FAIL"));
                            }
                        })
        );
    }
}
