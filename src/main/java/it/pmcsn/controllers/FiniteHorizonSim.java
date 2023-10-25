package it.pmcsn.controllers;

import java.util.List;

public class FiniteHorizonSim {
    /*Lo scopo di questa classe è quello di eseguire una simulazione con le 4 possibili fasce orarie, eseguite in modo
     * consecutivo una all'altra. Lo stato iniziale della seconda fascia oraria sarà dunque quello iniziale della terza
     * fascia, e così via.
     */


    Double[] carsArrivals = new Double[] {0.048, 0.063, 0.032, 0.016}; //lambda delle auto per le 4 fasce orarie
    Double[] camionArrivals = new Double[] {0.064, 0.076, 0.076, 0.038}; //lambda dei camion nelle fasce orarie

    int timeSlotDuration = 21600; //6 ore, durata fascia oraria

    public void simFiniteHorizon() {
        System.out.println("** Starting TimeSlots next event simulation **\n");


        NextEventController nextEventController = new NextEventController();
        nextEventController.initRngs();
        nextEventController.initCentersList();
        nextEventController.startSimulationWithTimeSlots(timeSlotDuration, carsArrivals, camionArrivals);

        System.out.println("Done!");
    }

    public static void main(String[] args) {
        FiniteHorizonSim finiteHorizonSim = new FiniteHorizonSim();
        finiteHorizonSim.simFiniteHorizon();
    }


}
