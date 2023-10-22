package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;
import it.pmcsn.rngs.Exponential;

import java.text.DecimalFormat;

public abstract class AbstractCenter {

    public int SERVERS; //Numero di server nel centro
    public SqArea area; //Tiene traccia dello stato del centro ad ogni aggiornamento dell'evento

    public int ID; //ID del centro, per poter essere identificato dal controller
    public int jobsInQueue;
    public int jobsInService;
    public int totalJobsProcessed;
    public double lastArrival = 0.0;
    public Event currentEvent; //Ultimo evento processato, serve per le stats
    protected final double MEAN_SERVICE_TIME; //E[S], va passato ad Exponential, così calcola un valore di exp con questa media

    protected NextEventController nextEventController; //il controller che sta gestendo la simulazione e la lista degli eventi


    public AbstractCenter(int servers, int id, double serviceTime, NextEventController controller) {
        this.SERVERS = servers;
        this.ID = id;
        this.MEAN_SERVICE_TIME = serviceTime;
        this.area = new SqArea(servers - 1); //Il numero di server si conta partendo da 1, la dimensione dell'array -> N-1
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



    protected void updateArea(Event event) {  //Aggiorna l'area per le statistiche

        if (jobsInQueue + jobsInService > 0){
            double deltaTime = event.eventTime - currentEvent.eventTime;
            area.area += deltaTime * (jobsInQueue + jobsInService);
        }

        this.currentEvent = event;
    }

    protected double getService() { //calcola il tempo a cui finirà il job che sta entrando in servizio

        nextEventController.rngs.selectStream(this.ID); //Ogni centro usa stream = ID. Gli arrivi useranno stream > ID più grande
        return Exponential.exponential(this.MEAN_SERVICE_TIME, nextEventController.rngs); //Tempo servizio del job.

    }
    public void handleNewArrival(Event event) {

        updateArea(event);
        if (event.eventType == EventType.ARRIVAL && event.eventTime > lastArrival) //dovrebbe sempre essere vera, ma per sicurezza la metto
            lastArrival = event.eventTime;
        //Se sono un arrivo, in coda non c'è nessuno, e c'è un server libero -> vado in servizio
        if (jobsInQueue == 0 && jobsInService < SERVERS) {
            jobsInService++;
            Event completionForThisArrival = new Event(EventType.COMPLETION, this.ID);
            completionForThisArrival.eventTime = this.getService() + event.eventTime; //+event.eventTime perchè getService calcola solo il tempo di servizio
            //ma bisogna aggiungere il tempo della simulazione a cui si è arrivati.


            //update services time
            double serviceTime = completionForThisArrival.eventTime - event.eventTime; //tempo_fine - tempo_inizio
            int chosenServer = area.assignJobToFreeServer(serviceTime);
            completionForThisArrival.assignedServer = chosenServer;


            nextEventController.eventList.add(completionForThisArrival);


        } else {
            //metto il job in coda
            jobsInQueue++;
        }

    }


    public void handleCompletion(Event event) {
        this.updateArea(event);
        jobsInService--;
        totalJobsProcessed++;
        area.setServerFree(event);

        //Se ho avuto un completamento, e c'è un job in coda -> va in servizio
        if (jobsInQueue > 0) {
            jobsInService++;
            Event complOfNewJob = new Event(EventType.COMPLETION, this.ID);
            complOfNewJob.eventTime = this.getService() + event.eventTime;

            jobsInQueue--;

            //update services time
            double serviceTime = complOfNewJob.eventTime - event.eventTime; //tempo_fine - tempo_inizio
            int chosenServer = area.assignJobToFreeServer(serviceTime);
            complOfNewJob.assignedServer = chosenServer;


            nextEventController.eventList.add(complOfNewJob);

        }

        this.generateEventAfterCompl(event); //Genero eventuali nuovi eventi per questo centro. Es.: completamento nel centro
        //3 (Pesa dei Camion) genera un arrivo nel centro 4 (Controllo Merce Camion). La creazione di questo evento avviene
        //con questa invocazione.


    }

    public void printStats(String centerName) {

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\n*** Stats for center: " + centerName + " ***");

        System.out.println("\nfor " + totalJobsProcessed + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(this.lastArrival / totalJobsProcessed));
        System.out.println("  avg wait ........... =   " + f.format(area.area / totalJobsProcessed));
        System.out.println("  avg # in node ...... =   " + f.format(area.area / this.currentEvent.eventTime));

        for (int s = 0; s <= SERVERS - 1; s++)          /* adjust area to calculate */
            area.area -= area.serverServices[s];              /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(area.area / totalJobsProcessed));
        System.out.println("  avg # in queue ..... =   " + f.format(area.area / this.currentEvent.eventTime));
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     ");
        for (int s = 0; s <= SERVERS - 1; s++) {
            System.out.print("       " + s + "          " + g.format(area.serverServices[s] / this.currentEvent.eventTime) + "            \n");

        }

        System.out.println("---------------------------------------------------------------------");

    }


}
