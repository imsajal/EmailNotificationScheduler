package org.demo;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EmailExecutor {

    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final Random random = new Random();

    public void send(Notification n, Runnable onSuccess, Runnable onFailure) {
        pool.submit(() -> {
            System.out.println(Thread.currentThread().getName() +
                    " → Sending email: " + n.id + " to " + n.email);

            boolean fail = random.nextInt(100) < 30;

            if (fail) {
                System.out.println(Thread.currentThread().getName() +
                        " → ❌ FAILED: " + n.id);
                onFailure.run();
            } else {
                System.out.println(Thread.currentThread().getName() +
                        " → 📨 SENT: " + n.id);
                onSuccess.run();
            }
        });
    }

    public void shutdown() throws InterruptedException {
        pool.shutdownNow();
       // pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}