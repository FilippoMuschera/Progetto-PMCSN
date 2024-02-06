package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;

public class CarVisualControlCenter extends AbstractCenter{
    public CarVisualControlCenter(int servers, double serviceTime, NextEventController controller) {
        super(servers, 1, serviceTime, controller);
    }


    @Override
    int getNextCenterId() {
        return 3;
    }

    @Override
    void generateEventAfterCompl(Event event) {
            nextEventController.rngs.selectStream(101);
            double decision = nextEventController.rngs.random();
            if (decision < 0.003) //P_out1 sostanzialmente: nello 0.3% dei casi non genero un nuovo arrivo, come se il job
                                  //fosse uscito direttamente dal sistema
                return;
            //Nel 99.5% dei casi il job passa il controllo e resta ancora nel sistema -> generiamo il nuovo arrivo

            Event newEvent = new Event(EventType.ARRIVAL, this.getNextCenterId());
            newEvent.eventTime = this.currentEvent.eventTime; //istantaneo
            nextEventController.eventList.add(newEvent);

    }
}
