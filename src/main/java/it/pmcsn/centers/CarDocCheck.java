package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;

public class CarDocCheck extends AbstractCenter{

    public void setProbFurtherCheck(double probFurtherCheck) {
        this.probFurtherCheck = probFurtherCheck;
    }

    double probFurtherCheck; //sarebbe la P_CA della relazione/schema
    public CarDocCheck(int servers, double serviceTime, NextEventController controller, double probFurtherChecks) {
        super(servers, 3, serviceTime, controller);
        this.probFurtherCheck = probFurtherChecks;
    }

    public CarDocCheck(int[] servers, double serviceTime, NextEventController controller, double probFurtherChecks) {
        super(servers, 3, serviceTime, controller);
        this.probFurtherCheck = probFurtherChecks;
    }

    @Override
    int getNextCenterId() {
        return 6;
    }

    @Override
    void generateEventAfterCompl(Event event) {

        nextEventController.rngs.selectStream(103);
        double decision = nextEventController.rngs.random();
        if (decision > probFurtherCheck)
            return;
        //Qui siamo nel caso in cui decision cade nel range [0, P_CA] quindi dobbiamo schedulare l'evento per cui
        //questo job (un'auto) sia sottoposta a controlli aggiuntivi nel centro #6 della frontiera

        Event newEvent = new Event(EventType.ARRIVAL, this.getNextCenterId());
        newEvent.eventTime = currentEvent.eventTime;
        nextEventController.eventList.add(newEvent);


    }
}
