package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;

public class GoodsControlCenter extends AbstractCenter{

    double probOfAdvancedInspection;

    public GoodsControlCenter(int servers, double serviceTime, NextEventController controller, double probCenter6) {
        super(servers, 5, serviceTime, controller);
        this.probOfAdvancedInspection = probCenter6;
    }

    public GoodsControlCenter(int[] servers, double serviceTime, NextEventController controller, double probCenter6) {
        super(servers, 5, serviceTime, controller);
        this.probOfAdvancedInspection = probCenter6;
    }

    @Override
    int getNextCenterId() {
        return 6;
    }

    @Override
    void generateEventAfterCompl(Event event) {
        nextEventController.rngs.selectStream(105);
        double decision = nextEventController.rngs.random(); //[0.0, 1.0]
        if (decision >= probOfAdvancedInspection)
            return; //Il job esce dal sistema -> nessun nuovo arrivo

        //Generiamo arrivo al centro 6
        Event newEvent = new Event(EventType.ARRIVAL, this.getNextCenterId());
        newEvent.eventTime = currentEvent.eventTime; //stesso istante
        nextEventController.eventList.add(newEvent);


    }
}
