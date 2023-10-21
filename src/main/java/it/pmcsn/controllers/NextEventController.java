package it.pmcsn.controllers;

import it.pmcsn.centers.AbstractCenter;
import it.pmcsn.event.Event;
import it.pmcsn.rngs.Rngs;

import java.util.ArrayList;
import java.util.List;

public class NextEventController {

    public Rngs rngs;
    public List<Event> eventList = new ArrayList<>();
    public List<AbstractCenter> centerList = new ArrayList<>();

    private int STOP_TIME; //"Gate closed" time

    private int totalArrivals = 0;

    private boolean isGateOpen = true;









}
