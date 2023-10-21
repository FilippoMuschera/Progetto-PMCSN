package it.pmcsn.event;


public class Event {

    public EventType eventType;
    public int eventID;
    public double eventTime;


    public Event(EventType type, int id) {
        this.eventID = id;
        this.eventType = type;
    }

}

