package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;
import it.pmcsn.rngs.Exponential;

public abstract class AbstractCenter {

    public int SERVERS; //Numero di server nel centro
    public SqArea area; //Tiene traccia dello stato del centro ad ogni aggiornamento dell'evento

    public int ID; //ID del centro, per poter essere identificato dal controller
    public int jobsInQueue;
    public int jobsInService;
    public int totalJobsProcessed;
    public Event currentEvent; //Ultimo evento processato, serve per le stats
    protected final double MEAN_SERVICE_TIME; //E[S], va passato ad Exponential, così calcola un valore di exp con questa media

    protected NextEventController nextEventController; //il controller che sta gestendo la simulazione e la lista degli eventi


    public AbstractCenter(int servers, int id, double serviceTime, NextEventController controller) {
        this.SERVERS = servers;
        this.ID = id;
        this.MEAN_SERVICE_TIME = serviceTime;
        this.area = new SqArea();
        this.jobsInQueue = 0;
        this.jobsInService = 0;
        this.totalJobsProcessed = 0;
        this.currentEvent = null;
        this.nextEventController = controller;


    }


    abstract int getNextCenterId(); //deve restituire l'ID del centro destinatario del nuovo evento, sarà diversa per ogni centro

    abstract void generateEventAfterCompl(Event event); //Dopo un completamento ogni centro deve generare un nuovo
    //arrivo per il centro successivo, a meno che non sia un centro "finale" dove i job escono sempre dalla rete.
    //Va implementata diversamente per ogni centro, perchè dipende dalla sua posizione nella rete.



    protected void updateStats(Event event) {  //Aggiorna l'area per le statistiche
        if (jobsInService + jobsInQueue > 0) {
            //Se nel sistema ci sono job, aggiorno le stats (source: esempio libro Ssq3.java)
            double deltaTime = event.eventTime - currentEvent.eventTime;
            //TODO assicurarsi che per single server e multi-server le stats si calcolino alla stessa maniera
            //mi pare di si, però meglio controllare
            area.node += deltaTime * (jobsInQueue + jobsInService);
            area.queue += deltaTime * jobsInQueue;
            area.service += deltaTime;
        }
        this.currentEvent = event;
    }

    protected double getService() { //calcola il tempo a cui finirà il job che sta entrando in servizio

        nextEventController.rngs.selectStream(this.ID); //Ogni centro usa stream = ID. Gli arrivi useranno stream > ID più grande
        return Exponential.exponential(this.MEAN_SERVICE_TIME, nextEventController.rngs); //Tempo servizio del job.

    }
    public void handleNewArrival(Event event) {

        updateStats(event);
        //Se sono un arrivo, in coda non c'è nessuno, e c'è un server libero -> vado in servizio
        if (jobsInQueue == 0 && jobsInService < SERVERS) {
            jobsInService++;
            this.currentEvent = event;
            Event completionForThisArrival = new Event(EventType.COMPLETION, this.ID);
            completionForThisArrival.eventTime = this.getService() + event.eventTime; //+event.eventTime perchè getService calcola solo il tempo di servizio
            //ma bisogna aggiungere il tempo della simulazione a cui si è arrivati.
            nextEventController.eventList.add(completionForThisArrival);
        } else {
            //metto il job in coda
            jobsInQueue++;
        }

    }


    public void handleCompletion(Event event) {
        this.updateStats(event);
        jobsInService--;
        totalJobsProcessed++;

        //Se ho avuto un completamento, e c'è un job in coda -> va in servizio
        if (jobsInQueue > 0) {
            jobsInService++;
            Event complOfNewJob = new Event(EventType.COMPLETION, this.ID);
            complOfNewJob.eventTime = this.getService() + event.eventTime;

            nextEventController.eventList.add(complOfNewJob);
            jobsInQueue--;
        }

        this.generateEventAfterCompl(event); //Genero eventuali nuovi eventi per questo centro. Es.: completamento nel centro
        //3 (Pesa dei Camion) genera un arrivo nel centro 4 (Controllo Merce Camion). La creazione di questo evento avviene
        //con questa invocazione.


    }


}
