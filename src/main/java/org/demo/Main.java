package org.demo;


public class Main {
    public static void main(String[] args) throws InterruptedException {
        NotificationScheduler scheduler = new NotificationScheduler();
        scheduler.start();

        scheduler.schedule(new Notification("A", "a@mail.com", System.currentTimeMillis() + 2000));
        scheduler.schedule(new Notification("B", "b@mail.com", System.currentTimeMillis() + 2000));
        scheduler.schedule(new Notification("C", "c@mail.com", System.currentTimeMillis() + 2000));
        scheduler.schedule(new Notification("D", "d@mail.com", System.currentTimeMillis() + 5000));

        Thread.sleep(20000);
        scheduler.shutdownScheduler();
        // simple ver without any threads would be we add into the queue first and then start run
    }
}