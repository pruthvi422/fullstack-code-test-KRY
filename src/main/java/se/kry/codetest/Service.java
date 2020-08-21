package se.kry.codetest;

import java.time.Instant;

public class Service {
    public String url;
    public String name;
    public Instant timestamp;
    public String status;

    public Service(String url, String name, Instant timestamp){
        this(url, name, timestamp, "UNKNOWN");
    }

    public Service(String url, String name, Instant timestamp, String status){
        this.url = url;
        this.name = name;
        this.timestamp = timestamp;
        this.status = status;
    }
}
