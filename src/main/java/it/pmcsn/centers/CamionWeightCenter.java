package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;

public class CamionWeightCenter extends AbstractCenter{


    private int jobsInQueue1 = 0;
    private int jobsInQueue2 = 0;
    double pOut = 0.05; //Hard-coded ed è giusto così, è una prob. fissa
    private double probFirstClass = 0.4; //Prob. di un job di essere di classe 1 (high priority)

    public CamionWeightCenter(int servers, double serviceTime, NextEventController controller) {
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

    @Override
    public void handleNewArrival(Event event) { //Override perchè questo centro ha code di priorità -> la funzione "base" non va bene
        if (event.eventType == EventType.ARRIVAL && event.eventTime > lastArrival) //dovrebbe sempre essere vera, ma per sicurezza la metto
            lastArrival = event.eventTime;

        //decido classe di appartenenza del nuovo arrivo
        nextEventController.rngs.selectStream(141);
        double classDecision = nextEventController.rngs.random();
        updateAreaWrapper(event);
        if (classDecision < this.probFirstClass) {
            //Job di classe 1 (alta priorità)
            if (jobsInQueue1 == 0 && jobsInService < this.SERVERS) {
                //in servizio
                jobsInService++;
                this.currentEvent = event;
                Event completionForThisArrival = new Event(EventType.COMPLETION, this.ID);
                completionForThisArrival.eventTime = this.getService() + event.eventTime; //+event.eventTime perchè getService calcola solo il tempo di servizio
                //ma bisogna aggiungere il tempo della simulazione a cui si è arrivati.

                //update services time
                double serviceTime = completionForThisArrival.eventTime - event.eventTime; //tempo_fine - tempo_inizio
                int chosenServer = area.assignJobToFreeServer(serviceTime);
                completionForThisArrival.assignedServer = chosenServer;


                nextEventController.eventList.add(completionForThisArrival);

            } else {
                //job di classe 1 ma tutti i serventi occupati -> in coda 1
                jobsInQueue1++;
            }
        } else {

            //jobs in class 2 (low priority)
            if (jobsInQueue1 == 0 && jobsInQueue2 == 0 && jobsInService < this.SERVERS) {
                //in servizio
                jobsInService++;
                this.currentEvent = event;
                Event completionForThisArrival = new Event(EventType.COMPLETION, this.ID);
                completionForThisArrival.eventTime = this.getService() + event.eventTime; //+event.eventTime perchè getService calcola solo il tempo di servizio
                //ma bisogna aggiungere il tempo della simulazione a cui si è arrivati.

                //update services time
                double serviceTime = completionForThisArrival.eventTime - event.eventTime; //tempo_fine - tempo_inizio
                int chosenServer = area.assignJobToFreeServer(serviceTime);
                completionForThisArrival.assignedServer = chosenServer;


                nextEventController.eventList.add(completionForThisArrival);
            } else {
                //job di classe 2 in coda 2
                jobsInQueue2++;
            }
        }

    }

    @Override
    public void handleCompletion(Event event) {
        this.updateAreaWrapper(event);
        jobsInService--;
        totalJobsProcessed++;
        area.setServerFree(event);


        if (jobsInQueue1 > 0) {
            //Prima controllo se posso mandare in servizio un job di classe 1 perchè ha priorità maggiore
            jobsInService++;
            Event complOfNewJob = new Event(EventType.COMPLETION, this.ID);
            complOfNewJob.eventTime = this.getService() + event.eventTime;

            jobsInQueue1--;
            //update services time
            double serviceTime = complOfNewJob.eventTime - event.eventTime; //tempo_fine - tempo_inizio
            int chosenServer = area.assignJobToFreeServer(serviceTime);
            complOfNewJob.assignedServer = chosenServer;


            nextEventController.eventList.add(complOfNewJob);

        } else if (jobsInQueue2 > 0) {
            jobsInService++;
            Event complOfNewJob = new Event(EventType.COMPLETION, this.ID);
            complOfNewJob.eventTime = this.getService() + event.eventTime;

            jobsInQueue2--;
            //update services time
            double serviceTime = complOfNewJob.eventTime - event.eventTime; //tempo_fine - tempo_inizio
            int chosenServer = area.assignJobToFreeServer(serviceTime);
            complOfNewJob.assignedServer = chosenServer;


            nextEventController.eventList.add(complOfNewJob);

        }

        this.generateEventAfterCompl(event); //genero arrivo per il prossimo centro (a meno di p_out obv)
    }

    private void updateAreaWrapper(Event event) {
        /*Questa classe ha le code di priorità, quindi usa jobsInQueue1 e jobsInQueue2. Il metodo per il calcolo
        dell'area usa jobsInQueue, che però non viene aggiornato durante l'esecuzione -> imposto il valore corretto e
        poi invoco updateArea(...), senza dover fare l'ovveride del metodo updateArea(...)
         */
        this.jobsInQueue = jobsInQueue1 + jobsInQueue2;
        this.updateArea(event);
    }

}
