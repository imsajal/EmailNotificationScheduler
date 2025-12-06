package org.demo;

import java.util.PriorityQueue;

public class NotificationScheduler extends Thread {

    private static final int MAX_RETRIES = 3;

    // PriorityQueue ordered by scheduleAt using lambda comparator
    private final PriorityQueue<Notification> queue =
            new PriorityQueue<>((a, b) -> Long.compare(a.scheduleAt, b.scheduleAt));

    private final EmailExecutor emailExecutor = new EmailExecutor();
    private volatile boolean running = true;

    public void schedule(Notification n) {
        synchronized (queue) {
            queue.add(n);
            queue.notify();
        }
    }

    @Override
    public void run() {
        System.out.println("Scheduler started → " + Thread.currentThread().getName());

        while (running) {
            Notification n = null;

            synchronized (queue) {
                while (queue.isEmpty()) {
                    if (!running) return;
                    try { queue.wait(); } catch (InterruptedException e) {}
                }
                long now = System.currentTimeMillis();

                if (queue.peek().scheduleAt > now) {
                    long wait = queue.peek().scheduleAt - now;
                    try { wait(wait); } catch (InterruptedException e) {}
                    continue; // this is needed as some earlier time job came we would poll that and check the
                    // running flag again in while will start
                    // queue.wait(10000) will exit immediately when another thread calls
                    // queue.notify() (or notifyAll()). It does not continue waiting for the full 10 seconds.
                }

                n = queue.poll();
            }

            Notification job = n;
            emailExecutor.send(
                    job,
                    () -> handleSuccess(job),
                    () -> handleFailure(job)
            );
        }
    }

    private void handleSuccess(Notification n) {
        System.out.println(Thread.currentThread().getName() +
                " → ✔ SUCCESS: " + n.id);
    }

    private void handleFailure(Notification n) {
        if (n.retryCount >= MAX_RETRIES) {
            System.out.println(Thread.currentThread().getName() +
                    " → ❌ GIVE UP (max retries reached): " + n.id);
            return;
        }

        n.retryCount++;
        n.scheduleAt = System.currentTimeMillis() + 3000;

        System.out.println(Thread.currentThread().getName() +
                " → 🔁 RETRY " + n.retryCount + "/" + MAX_RETRIES +
                " for: " + n.id);

        schedule(n);
    }

    public void shutdownScheduler() throws InterruptedException {
        running = false;
        synchronized (queue) {
            queue.notifyAll();  // wake worker so it can exit
        }
        //this.interrupt();
        emailExecutor.shutdown();

    }

    /* ALL production schedulers use BOTH: interuppt ensures wake up from any state can be sleep also not just wait
✔ Flag
✔ Notify
✔ Interrupt
Because interrupt is the final guarantee your thread wakes no matter what it is doing.*/
}



// shorter code with scheduleexecutorservice
// it can store many notifications
//public class NotificationScheduler {
//
//    private final EmailExecutor emailExecutor = new EmailExecutor();
//
//    public void schedule(Notification n) {
//        emailExecutor.schedule(n);
//    }
//
//    public void shutdown() {
//        emailExecutor.shutdown();
//    }
//}
//
//public class EmailExecutor {
//
//    private final ScheduledExecutorService executor =
//            Executors.newScheduledThreadPool(4);
//
//    public void schedule(Notification n) {
//
//        long delay = Math.max(
//                0,
//                n.scheduleAt - System.currentTimeMillis()
//        );
//
//        executor.schedule(
//                () -> send(n),
//                delay,
//                TimeUnit.MILLISECONDS
//        );
//    }
//
//    private void send(Notification n) {
//        System.out.println(
//                Thread.currentThread().getName()
//                        + " → Sending: " + n.id
//        );
//
//        // email provider call
//    }
//
//    public void shutdown() {
//        executor.shutdown();
//    }
//}

// In Hld we can use event bridge scheduler from aws or design it like distributed scheduler with redis+watcher
// + optional dispatcher if scoring needed + email workers