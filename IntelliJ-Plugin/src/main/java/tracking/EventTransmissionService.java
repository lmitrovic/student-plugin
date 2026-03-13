package tracking;


import com.github.nikolajr93.studenttestingintellijplugin.api.RafApiClient;
import com.google.gson.Gson;
import tracking.models.EventBatchDto;
import tracking.models.StudentEventDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class EventTransmissionService {
    private final List<StudentEvent> pendingEvents = new ArrayList<>();
    private final int MAX_BATCH_SIZE = 10; // Reduced batch size for more frequent sends
    private final int MAX_RETRIES = 3;
    private final Gson gson = new Gson();

    public synchronized void addToPendingBatch(StudentEvent event) {
        pendingEvents.add(event);

        System.out.println("=== EVENT TRANSMISSION DEBUG ===");
        System.out.println("Added event to pending batch. Total pending: " + pendingEvents.size());
        System.out.println("MAX_BATCH_SIZE: " + MAX_BATCH_SIZE);
        System.out.println("Event type: " + event.getEventType());

        // Send immediately if batch is full
        if (pendingEvents.size() >= MAX_BATCH_SIZE) {
            System.out.println("*** BATCH SIZE REACHED - SENDING NOW ***");
            sendBatch();
        } else {
            System.out.println("Batch not full yet (" + pendingEvents.size() + "/" + MAX_BATCH_SIZE + ")");
        }
        System.out.println("=================================");
    }

    public synchronized void sendBatch() {
        System.out.println("=== SEND BATCH CALLED ===");
        if (pendingEvents.isEmpty()) {
            System.out.println("No pending events to send - returning");
            return;
        }

        System.out.println("Preparing to send " + pendingEvents.size() + " events");

        List<StudentEvent> batch = new ArrayList<>(pendingEvents);
        pendingEvents.clear();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            System.out.println("Attempt " + (attempt + 1) + " of " + MAX_RETRIES);
            try {
                boolean success = sendToServer(batch);
                System.out.println("sendToServer returned: " + success);
                if (success) {
                    System.out.println("Successfully sent batch of " + batch.size() + " events");
                    break; // Success, exit retry loop
                } else {
                    System.out.println("Failed to send batch, attempt " + (attempt + 1));
                }
            } catch (Exception e) {
                System.err.println("Attempt " + (attempt + 1) + " failed: " + e.getMessage());
                if (attempt == MAX_RETRIES - 1) {
                    // Save to local file as backup
                    saveToLocalFile(batch);
                }
                // Wait before retry
                try {
                    Thread.sleep(1000 * (attempt + 1)); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println("=== SEND BATCH COMPLETE ===");
    }

    private boolean sendToServer(List<StudentEvent> events) {
        System.out.println("=== SEND TO SERVER ===");
        System.out.println("Converting " + events.size() + " events to DTOs");

        try {
            // Convert StudentEvent to StudentEventDto
            List<StudentEventDto> eventDtos = new ArrayList<>();

            for (StudentEvent event : events) {
                System.out.println("Converting event: " + event.getEventType());

                StudentEventDto dto = new StudentEventDto();
                dto.setStudentId(event.getStudentId());
                dto.setSessionId(event.getSessionId());
                dto.setEventType(event.getEventType());

                // Convert timestamp from milliseconds to LocalDateTime
                LocalDateTime timestamp = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.getTimestamp()),
                        ZoneId.systemDefault()
                );
                dto.setTimestamp(timestamp);

                dto.setTaskId(event.getTaskId());
                dto.setEventData(event.getEventData());

                eventDtos.add(dto);
            }

            EventBatchDto batchDto = new EventBatchDto(eventDtos);

            System.out.println("Created EventBatchDto with " + eventDtos.size() + " events");
            System.out.println("Calling RafApiClient.sendTrackingEvents...");

            // Use RafApiClient to send the batch
            String response = RafApiClient.sendTrackingEvents(batchDto);

            System.out.println("RafApiClient response: " + response);

            // Check if response indicates success
            boolean success = response != null && !response.contains("error") && !response.contains("failed");
            System.out.println("Determined success: " + success);

            return success;
        } catch (Exception e) {
            System.err.println("Failed to send events to server: " + e.getMessage());
            return false;
        }
    }

    private void saveToLocalFile(List<StudentEvent> events) {
        try {
            File backupFile = new File("event_backup_" + System.currentTimeMillis() + ".json");

            // Convert events to DTOs for JSON serialization
            List<StudentEventDto> eventDtos = new ArrayList<>();
            for (StudentEvent event : events) {
                StudentEventDto dto = new StudentEventDto();
                dto.setStudentId(event.getStudentId());
                dto.setSessionId(event.getSessionId());
                dto.setEventType(event.getEventType());
                dto.setTimestamp(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(event.getTimestamp()),
                        ZoneId.systemDefault()
                ));
                dto.setTaskId(event.getTaskId());
                dto.setEventData(event.getEventData());
                eventDtos.add(dto);
            }

            EventBatchDto batchDto = new EventBatchDto(eventDtos);
            String json = gson.toJson(batchDto);

            Files.write(backupFile.toPath(), json.getBytes());
            System.out.println("Events saved to backup file: " + backupFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to save events to backup file: " + e.getMessage());
        }
    }

    // Force send any remaining events (call this when plugin shuts down)
    public synchronized void flushPendingEvents() {
        if (!pendingEvents.isEmpty()) {
            sendBatch();
        }
    }
}
