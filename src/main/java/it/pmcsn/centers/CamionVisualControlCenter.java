package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;

public class CamionVisualControlCenter extends AbstractCenter{
    public CamionVisualControlCenter(int servers, double serviceTime, NextEventController controller) {
        super(servers, 2, serviceTime, controller);
    }

    public CamionVisualControlCenter(int[] servers, double serviceTime, NextEventController controller) {
        super(servers, 2, serviceTime, controller);
    }

    @Override
    int getNextCenterId() {
        return 4;
    }

    @Override
    void generateEventAfterCompl(Event event) {

        nextEventController.rngs.selectStream(102);
        double decision = nextEventController.rngs.random();
        if (decision < 0.005) //vedi commento in CarVisualControlCenter dello stesso metodo
            return;
        Event newEvent = new Event(EventType.ARRIVAL, this.getNextCenterId());
        newEvent.eventTime = currentEvent.eventTime; //istantaneo
        nextEventController.eventList.add(newEvent);


    }
}
