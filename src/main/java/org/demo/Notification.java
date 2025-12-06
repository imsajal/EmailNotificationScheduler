package org.demo;

public class Notification {
    public final String id;
    public final String email;
    public long scheduleAt;
    public int retryCount = 0;

    public Notification(String id, String email, long scheduleAt) {
        this.id = id;
        this.email = email;
        this.scheduleAt = scheduleAt;
    }
}
