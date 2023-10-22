package it.pmcsn.controllers;

import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;
import it.pmcsn.rngs.Exponential;
import it.pmcsn.rngs.Rngs;

public class ArrivalsController {


    private double carArrivalRate; //tasso di interarrivo auto
    private double camionArrivalRate; //tasso interarrivo camion

    private NextEventController controller;
    private Event carOnHold; //"onHold" nel senso che l'arrivo è stato generato ma non ancora inserito nella coda perchè
                             //non era il più imminente
    private Event camionOnHold; //come sopra

    private final int carStream = 7;
    private  final int camionStream = 8;

    private double carSumArrivals = 0.0; //Somma dei tempi di arrivo delle auto. Così calcolo tempo interarrivo della prossima auto,
                                       // e sommandolo a questo valore ottengo l'istante di arrivo

    private double camionSumArrivals = 0.0; //come per le auto
    public int counterCars = 0;
    public int counterCamion = 0;


    public ArrivalsController(double carInterarrivalRate, double camionInterarrivalRate, NextEventController controller) {
        this.carOnHold = null;
        this.camionOnHold = null;
        this.carArrivalRate = carInterarrivalRate;
        this.camionArrivalRate = camionInterarrivalRate;
        this.controller = controller;
    }

    private Event generateCarArrival() {
        Event e = new Event(EventType.ARRIVAL, 1 /*VisualControlCarCenter*/);
        controller.rngs.selectStream(carStream);
        carSumArrivals += Exponential.exponential(carArrivalRate, controller.rngs);
        e.eventTime = carSumArrivals;
        counterCars++; //debug
        return e;
    }

    private Event generateCamionArrival() {
        Event e = new Event(EventType.ARRIVAL, 2 /*VisualControlCamionCenter*/);
        controller.rngs.selectStream(camionStream);
        camionSumArrivals += Exponential.exponential(camionArrivalRate, controller.rngs);
        e.eventTime = camionSumArrivals;
        counterCamion++; //debug
        return e;
    }


    public Event getNextArrival() {
        if (carOnHold == null)
            carOnHold = generateCarArrival();
        if (camionOnHold == null)
            camionOnHold = generateCamionArrival();

        if (carOnHold.eventTime <= camionOnHold.eventTime) {
            Event toReturn = carOnHold;
            carOnHold = null; //lo metto null così alla prossima iterazione so che devo generare solo un auto e non un camion
            return  toReturn;
        } else { //carOnHold.eventTime > camionOnHold.eventTime -> il camion arriva prima, letteralmente
            Event toReturn = camionOnHold;
            camionOnHold = null;
            return toReturn;
        }

    }

}
