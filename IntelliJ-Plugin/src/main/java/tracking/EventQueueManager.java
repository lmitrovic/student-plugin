package tracking;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class EventQueueManager {
    private final BlockingQueue<StudentEvent> eventQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final EventTransmissionService transmissionService;

    public EventQueueManager() {
        this.transmissionService = new EventTransmissionService();
        startEventProcessor();
        startBatchTransmitter();
    }

    public void addEvent(StudentEvent event) {
        eventQueue.offer(event); // Non-blocking add
    }

    private void startEventProcessor() {
        scheduler.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    StudentEvent event = eventQueue.take(); // Blocking
                    processEvent(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void startBatchTransmitter() {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("*** SCHEDULED BATCH TRANSMISSION TRIGGERED ***");
            transmissionService.sendBatch();
        }, 30, 30, TimeUnit.SECONDS);
    }

    private void processEvent(StudentEvent event) {
        // Add event to pending transmission list
        transmissionService.addToPendingBatch(event);

        // Log locally for backup
        System.out.println("Event logged: " + event.toString());
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
