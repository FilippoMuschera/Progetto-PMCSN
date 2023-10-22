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

        CarVisualControlCenter center1 = new CarVisualControlCenter(3, 18.0, this);
        centerList.add(center1);

        CamionVisualControlCenter center2 = new CamionVisualControlCenter(3, 45.0, this);
        centerList.add(center2);

        CarDocCheck center3 = new CarDocCheck(5, 60.0, this, 0.2);
        centerList.add(center3);

        CamionWeightCenter center4 = new CamionWeightCenter(4, 120.0, this);
        centerList.add(center4);

        GoodsControlCenter center5 = new GoodsControlCenter(4, 600.0, this, 0.4);
        centerList.add(center5);

        AdvancedChecksCenter center6 = new AdvancedChecksCenter(2, 1080.0, this);
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

    public void starSimulation() {

        totalArrivals = 0;
        while (isGateOpen || !eventList.isEmpty()) {

            if (isGateOpen) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate

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
    }






}
