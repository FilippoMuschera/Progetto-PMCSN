package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;
import it.pmcsn.rngs.Exponential;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCenter {

    public int SERVERS; //Numero di server nel centro
    public SqArea area; //Tiene traccia dello stato del centro ad ogni aggiornamento dell'evento
    public SqArea currentSlotArea; //serve a calcolare le statistiche solamente per la fascia di tempo corrente, o per il batch corrente
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
        this.currentSlotArea = new SqArea(servers - 1);
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
        int sumJobProcessed = 0;
        for (int i = 0; i < area.servedByServer.length; i++) {
            sumJobProcessed += area.servedByServer[i];
        } //Check, just for debug, should never fail
        if (sumJobProcessed != this.totalJobsProcessed)
            throw new RuntimeException("sumJobProcessed = " + sumJobProcessed + ", totalJobProcessed = " + totalJobsProcessed);

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\n*** Stats for center: " + centerName + " ***");

        System.out.println("\nfor " + totalJobsProcessed + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(this.lastArrival / totalJobsProcessed));
        System.out.println("  avg wait ........... =   " + f.format(area.area / totalJobsProcessed) + " s = " + f.format((area.area / totalJobsProcessed)/60) + " min");
        System.out.println("  avg # in node ...... =   " + f.format(area.area / this.currentEvent.eventTime));

        double tempArea = this.area.area; //non uso il vero valore di area.area, sennò sottraendoci i tempi di servizio invalido tutto
        for (int s = 0; s <= SERVERS - 1; s++)          /* adjust area to calculate */
            tempArea -= area.serverServices[s];        /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(tempArea / totalJobsProcessed)+ " s = " + f.format((tempArea / totalJobsProcessed)/60) + " min");
        System.out.println("  avg # in queue ..... =   " + f.format(tempArea / this.currentEvent.eventTime));
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service");
        double avgUtilization = 0;
        for (int s = 0; s <= SERVERS - 1; s++) {
            System.out.print("       " + s + "          " + g.format(area.serverServices[s] / this.currentEvent.eventTime) + "          " +
                    g.format(area.serverServices[s]/area.servedByServer[s]) + "\n");
            avgUtilization += area.serverServices[s] / this.currentEvent.eventTime;


        }
        System.out.println("  avg utilization .......... =   " + g.format(avgUtilization/SERVERS) + "\n");

        System.out.println("---------------------------------------------------------------------");

    }


    public void notifyStatsReset() {

        //Copio i valori attuali dell'area, ma poi non li aggiornerò. Finita la fascia oraria calcolo i "delta" tra le
        //varie statistiche: quelle saranno le aree da usare per le stats della singola fascia oraria.
        this.currentSlotArea = new SqArea(this.SERVERS - 1);
        System.arraycopy(this.area.servedByServer, 0, this.currentSlotArea.servedByServer, 0, this.currentSlotArea.servedByServer.length);
        System.arraycopy(this.area.serverServices, 0, this.currentSlotArea.serverServices, 0, this.currentSlotArea.servedByServer.length);
        this.currentSlotArea.area = this.area.area;

    }

    public List<Double> getBatchCenterStats() {

        List<Double> returnList = new ArrayList<>();
        /*
         * La struttura di questo array è la seguente:
         * [Lambda, E[Ts], E[Ns], E[Tq], E[Nq], meanRho]
         */

        //calcolo i "delta" delle aree. Per ogni dato: valoreAFineBatch - valoreAInizioBatch
        this.currentSlotArea.area = this.area.area - this.currentSlotArea.area;
        for (int i = 0; i < this.currentSlotArea.servedByServer.length; i++) {
            this.currentSlotArea.servedByServer[i] = this.area.servedByServer[i] - this.currentSlotArea.servedByServer[i];
            this.currentSlotArea.serverServices[i] = this.area.serverServices[i] - this.currentSlotArea.serverServices[i];
        }

        int jobsOfBatch = 0;//Mi serve perchè in queste stats divido per il numero di job del batch, non per il numero di job "assoluto" (dall'inizio della simulazione)
        for (int i = 0; i < area.servedByServer.length; i++) {
            jobsOfBatch += this.currentSlotArea.servedByServer[i];
        }



        returnList.add(this.lastArrival / jobsOfBatch);
        returnList.add(currentSlotArea.area/jobsOfBatch);
        returnList.add(currentSlotArea.area / this.currentEvent.eventTime);


        double tempArea = this.currentSlotArea.area; //non uso il vero valore di area.area, sennò sottraendoci i tempi di servizio invalido tutto
        for (int s = 0; s <= SERVERS - 1; s++)          /* adjust area to calculate */
            tempArea -= currentSlotArea.serverServices[s];

        returnList.add(tempArea / jobsOfBatch);
        returnList.add(tempArea / this.currentEvent.eventTime);

        double avgUtilization = 0;
        for (int s = 0; s <= SERVERS - 1; s++) {
            avgUtilization += currentSlotArea.serverServices[s] / this.currentEvent.eventTime;

        }
        returnList.add(avgUtilization);

        return returnList;


    }

    public void statsDumpForLastTimeSlot(String centerName) {

        //calcolo i "delta" delle aree. Per ogni dato: valoreAFineFasciaOraria - valoreAInizioFasciaOraria
        this.currentSlotArea.area = this.area.area - this.currentSlotArea.area;
        for (int i = 0; i < this.currentSlotArea.servedByServer.length; i++) {
            this.currentSlotArea.servedByServer[i] = this.area.servedByServer[i] - this.currentSlotArea.servedByServer[i];
            this.currentSlotArea.serverServices[i] = this.area.serverServices[i] - this.currentSlotArea.serverServices[i];
        }




        int sumJobProcessed = 0;
        int jobsOfTimeSlot = 0;//Mi serve perchè in queste stats divido per il numero di job della fascia, non per il numero di job "assoluto" (dall'inizio della simulazione)
        for (int i = 0; i < area.servedByServer.length; i++) {
            sumJobProcessed += area.servedByServer[i];
            jobsOfTimeSlot += this.currentSlotArea.servedByServer[i];
        } //Check, just for debug, should never fail
        if (sumJobProcessed != this.totalJobsProcessed)
            throw new RuntimeException("sumJobProcessed = " + sumJobProcessed + ", totalJobProcessed = " + totalJobsProcessed);

        DecimalFormat f = new DecimalFormat("###0.00");
        DecimalFormat g = new DecimalFormat("###0.000");

        System.out.println("\n*** Stats for center: " + centerName + " ***");

        System.out.println("\nfor " + jobsOfTimeSlot + " jobs the service node statistics are:\n");
        System.out.println("  avg interarrivals .. =   " + f.format(this.lastArrival / jobsOfTimeSlot));
        System.out.println("  avg wait ........... =   " + f.format(currentSlotArea.area / jobsOfTimeSlot) + " s = " + f.format((currentSlotArea.area / jobsOfTimeSlot)/60) + " min");
        System.out.println("  avg # in node ...... =   " + f.format(currentSlotArea.area / this.currentEvent.eventTime));

        double tempArea = this.currentSlotArea.area; //non uso il vero valore di area.area, sennò sottraendoci i tempi di servizio invalido tutto
        for (int s = 0; s <= SERVERS - 1; s++)          /* adjust area to calculate */
            tempArea -= currentSlotArea.serverServices[s];        /* averages for the queue   */

        System.out.println("  avg delay .......... =   " + f.format(tempArea / jobsOfTimeSlot)+ " s = " + f.format((tempArea / jobsOfTimeSlot)/60) + " min");
        System.out.println("  avg # in queue ..... =   " + f.format(tempArea / this.currentEvent.eventTime));
        System.out.println("\nthe server statistics are:\n");
        System.out.println("    server     utilization     avg service");
        double avgUtilization = 0;
        for (int s = 0; s <= SERVERS - 1; s++) {
            System.out.print("       " + s + "          " + g.format(currentSlotArea.serverServices[s] / this.currentEvent.eventTime) + "          " +
                    g.format(currentSlotArea.serverServices[s]/ currentSlotArea.servedByServer[s]) + "\n");
            avgUtilization += currentSlotArea.serverServices[s] / this.currentEvent.eventTime;


        }
        System.out.println("  avg utilization .......... =   " + g.format(avgUtilization/SERVERS) + "\n");

        System.out.println("---------------------------------------------------------------------");
    }


}
