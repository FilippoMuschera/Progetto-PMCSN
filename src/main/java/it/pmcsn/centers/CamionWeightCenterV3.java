package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;

public class CamionWeightCenterV3 extends AbstractCenter {


    double pOut = 0.05; //Hard-coded ed è giusto così, è una prob. fissa





    public CamionWeightCenterV3(int servers, double serviceTime, NextEventController controller) {
        super(servers, 4, serviceTime, controller);
    }

    @Override
    int getNextCenterId() {
        return 5;
    }

    @Override
    void generateEventAfterCompl(Event event) {

        nextEventController.rngs.selectStream(104);
        double decision = nextEventController.rngs.random(); //decision compreso tra [0.0, 1.0]
        if (decision < pOut) //Caso in cui il job esce immediatamente dal sistema
            return;
        else { //caso in cui il job prosegue nel centro 5

            Event newEvent = new Event(EventType.ARRIVAL, this.getNextCenterId());
            newEvent.eventTime = currentEvent.eventTime;
            nextEventController.eventList.add(newEvent);
            return;

        }

    }
}
