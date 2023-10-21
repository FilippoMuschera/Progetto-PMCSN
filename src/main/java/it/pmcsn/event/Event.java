package it.pmcsn.event;


public class Event {

    public EventType eventType;
    public int centerID; //ID del centro a cui è destinato l'evento
    public double eventTime;


    public Event(EventType type, int id) {
        this.centerID = id;
        this.eventType = type;
    }


}

