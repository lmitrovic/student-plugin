package tracking.models;

import java.util.List;

public class EventBatchDto {
    private List<StudentEventDto> events;

    public EventBatchDto() {}

    public EventBatchDto(List<StudentEventDto> events) {
        this.events = events;
    }

    public List<StudentEventDto> getEvents() {
        return events;
    }

    public void setEvents(List<StudentEventDto> events) {
        this.events = events;
    }
}