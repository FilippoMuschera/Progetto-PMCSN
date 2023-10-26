package it.pmcsn.controllers;

import it.pmcsn.centers.*;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;
import it.pmcsn.rngs.Rngs;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NextEventController {

    public Rngs rngs;
    public List<Event> eventList = new ArrayList<>();
    public List<AbstractCenter> centerList = new ArrayList<>();

    public ArrivalsController arrivalsController;

    private int STOP_TIME; //"Gate closed" time

    private int totalArrivals = 0;

    private boolean isGateOpen = true;



    public void initCentersList() {

        CarVisualControlCenter center1 = new CarVisualControlCenter(2, 18.0, this);
        centerList.add(center1);

        CamionVisualControlCenter center2 = new CamionVisualControlCenter(4, 48.0, this);
        centerList.add(center2);

        CarDocCheck center3 = new CarDocCheck(4, 50.0, this, 0.01);
        centerList.add(center3);

        CamionWeightCenter center4 = new CamionWeightCenter(7, 110.0, this);
        centerList.add(center4);

        GoodsControlCenter center5 = new GoodsControlCenter(9, 175.0, this, 0.04);
        centerList.add(center5);

        AdvancedChecksCenter center6 = new AdvancedChecksCenter(3, 1000.0, this);
        centerList.add(center6);


    }

    public void initRngs() {
        this.rngs = new Rngs();
        this.rngs.plantSeeds(123456789L);
    }

    public void initArrivals() {
        this.arrivalsController = new ArrivalsController(12.76, 15.873, this);
        this.STOP_TIME = 86400 * 1; //Secondi in un giorno * giorni di simulazione
    }


    public void startSimulationWithTimeSlots(int timeSlotDuration, Double[] carRates, Double[] camionRates){
        totalArrivals = 0;
        int currentTimeSlot = 1;
        int slotsNumber = carRates.length;

        this.arrivalsController = new ArrivalsController(1/carRates[0], 1/camionRates[0], this);
        this.STOP_TIME = timeSlotDuration * slotsNumber;

        while (isGateOpen || (!eventList.isEmpty() && currentTimeSlot == slotsNumber)) { //seconda condizione: solo l'ultima fascia svuota il sistema

            if (isGateOpen) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate
                totalArrivals++; //stats for the sim

                //Handling delle fasce orarie. Se cambia la fascia oraria devo aggiornare il tasso di arrivo

                if ((nextArrival.eventTime > (timeSlotDuration * currentTimeSlot)) && isGateOpen) { //se il gate è chiuso non devo aggiornare più nulla qui
                    //Devo cambiare fascia oraria
                    currentTimeSlot++;
                    this.arrivalsController.carArrivalRate = 1/carRates[currentTimeSlot - 1]; //-1 perchè currentTimeSlot parte da 1
                    this.arrivalsController.camionArrivalRate = 1/camionRates[currentTimeSlot - 1];//anche qui come sopra

                    //Se sto cambiando fascia oraria -> faccio un dump delle statistiche della fascia oraria appena terminata
                    System.out.println("+++++++ STATS DUMP FASCIA ORARIA #" + (currentTimeSlot - 1) + " +++++++\n"); //-1 perchè è appena stato incrementato
                    //ma le stats sono della fascia appena finita
                    for (AbstractCenter center : centerList) {
                        center.printStats(center.getClass().getSimpleName());
                    }
                    System.out.println("++++++++++++++++++++++++++++++++++++\n");



                }

            }


            eventList.sort(Comparator.comparingDouble(event -> event.eventTime)); //ordino gli eventi


            Event nextEvent = eventList.get(0);
            eventList.remove(0); //pop




            if (nextEvent.eventType == EventType.ARRIVAL) {


                for (AbstractCenter center : centerList) {
                    if (center.ID == nextEvent.centerID) {
                        center.handleNewArrival(nextEvent);
                        break;
                    }
                }


            }
            else {
                for (AbstractCenter center : centerList) {
                    if (center.ID == nextEvent.centerID) {
                        center.handleCompletion(nextEvent);
                        break;
                    }
                }
            }






        }
        //Simulazione finita, stampo le statistiche
        for (AbstractCenter center : centerList) {
            center.printStats(center.getClass().getSimpleName());
        }

        System.out.println("***   DEBUG  ***\nCar arrivals = " + arrivalsController.counterCars +
                "\nCamion arrivals = " + arrivalsController.counterCamion);
        if (arrivalsController.counterCamion + arrivalsController.counterCars != totalArrivals + 1)
            //-1 perchè genero un arrivo che non userò mai: io ho sempre due arrivi pronti (una macchina e un camion). Appena uno dei due triggera lo
            //stop time, chiudo il gate. A quel punto, l'arrivo rimasto nell'arrivalController è stato generato, ma sarebbe arrivato dopo quello che ha
            //triggerato la chiusura del gate. In pratica è un arrivo che trova il cancello chiuso, e non entra nel sistema, ma viene generato, quindi
            //non lo considero nel computo degli arrivi perchè è corretto che non arrivi mai nel sistema. Se lo facesse vuol dire che ho consentito
            //a un job l'arrivo dopo la chiusura del cancello.
            throw new RuntimeException("Numero Arrivi generati != Numero arrivi nel sistema"); //Non dovrebbe mai accadere, ma è un check

    }

    public void starSimulation() {

        totalArrivals = 0;
        while (isGateOpen || !eventList.isEmpty()) {

            if (isGateOpen) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate
                totalArrivals++; //stats for the sim

            }

            eventList.sort(Comparator.comparingDouble(event -> event.eventTime)); //ordino gli eventi


            Event nextEvent = eventList.get(0);
            eventList.remove(0); //pop

            if (nextEvent.eventType == EventType.ARRIVAL) {


                for (AbstractCenter center : centerList) {
                    if (center.ID == nextEvent.centerID) {
                        center.handleNewArrival(nextEvent);
                        break;
                    }
                }


            }
            else {
                for (AbstractCenter center : centerList) {
                    if (center.ID == nextEvent.centerID) {
                        center.handleCompletion(nextEvent);
                        break;
                    }
                }
            }

        }
        //Simulazione finita, stampo le statistiche
        for (AbstractCenter center : centerList) {
            center.printStats(center.getClass().getSimpleName());
        }


    }

    public static void main(String[] args) {
        NextEventController nextEventController = new NextEventController();
        nextEventController.initCentersList();
        nextEventController.initRngs();
        nextEventController.initArrivals();

        nextEventController.starSimulation();

        System.out.println("***   DEBUG  ***\nCar arrivals = " + nextEventController.arrivalsController.counterCars +
                "\nCamion arrivals = " + nextEventController.arrivalsController.counterCamion);
        if (nextEventController.arrivalsController.counterCamion + nextEventController.arrivalsController.counterCars != nextEventController.totalArrivals + 1)
            //-1 perchè genero un arrivo che non userò mai: io ho sempre due arrivi pronti (una macchina e un camion). Appena uno dei due triggera lo
            //stop time, chiudo il gate. A quel punto, l'arrivo rimasto nell'arrivalController è stato generato, ma sarebbe arrivato dopo quello che ha
            //triggerato la chiusura del gate. In pratica è un arrivo che trova il cancello chiuso, e non entra nel sistema, ma viene generato, quindi
            //non lo considero nel computo degli arrivi perchè è corretto che non arrivi mai nel sistema. Se lo facesse vuol dire che ho consentito
            //a un job l'arrivo dopo la chiusura del cancello.
            throw new RuntimeException("Numero Arrivi generati != Numero arrivi nel sistema"); //Non dovrebbe mai accadere, ma è un check
    }






}
