package it.pmcsn.controllers;

import it.pmcsn.centers.*;
import it.pmcsn.event.Event;
import it.pmcsn.event.EventType;
import it.pmcsn.rngs.Acs;
import it.pmcsn.rngs.Estimate;
import it.pmcsn.rngs.Rngs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class NextEventController {

    public Rngs rngs;
    public List<Event> eventList = new ArrayList<>();
    public List<AbstractCenter> centerList = new ArrayList<>();

    public ArrivalsController arrivalsController;

    private int STOP_TIME; //"Gate closed" time

    private int jobsProcessed = 0;

    private boolean isGateOpen = true;



    public void initCentersList() {

        CarVisualControlCenter center1 = new CarVisualControlCenter(2, 18.0, this);
        centerList.add(center1);

        CamionVisualControlCenter center2 = new CamionVisualControlCenter(4, 48.0, this);
        centerList.add(center2);

        CarDocCheck center3 = new CarDocCheck(4, 50.0, this, 0.01);
        centerList.add(center3);

        CamionWeightCenterV3 center4 = new CamionWeightCenterV3(6, 100.0, this);
        centerList.add(center4);

        GoodsControlCenter center5 = new GoodsControlCenter(9, 175.0, this, 0.04);
        centerList.add(center5);

        AdvancedChecksCenter center6 = new AdvancedChecksCenter(3, 1000.0, this);
        centerList.add(center6);


    }

    public void initRngs() {
        this.rngs = new Rngs();
        this.rngs.plantSeeds(999L);
    }

    public void initArrivals(int stopTime) {
        //this.arrivalsController = new ArrivalsController(12.76, 15.873, this);
        //this.arrivalsController = new ArrivalsController(24.76, 30.1, this);
        this.arrivalsController = new ArrivalsController(62.5, 26.31579, this);

        this.STOP_TIME = stopTime; //Secondi in un giorno * giorni di simulazione
    }

    public void startSimulationInfinite(int batchNum, int jobsPerBatch) throws IOException {
        jobsProcessed = 0;
        int currentBatch = 0;
        Double[][][] dataMatrix = new Double[6][batchNum][6]; //6 righe, batchNums colonne, ogni elemento è un array di 6 double
        /*
         * La DataMatrix contiene una riga per ogni centro, e una colonna per ogni batch. Ogni elemento ij è una lista
         * con tutte le statistiche del centro i per il batch j.
         */

        while (isGateOpen || !eventList.isEmpty()) {
            if (jobsProcessed % jobsPerBatch == 0 && jobsProcessed > 0) {

                int i = 0;
                for (AbstractCenter center : this.centerList) {
                    dataMatrix[i][currentBatch] = center.getBatchCenterStats().toArray(new Double[0]); //set elemento ij
                    center.notifyStatsReset();
                    i++;
                }

                //Batch terminata, si passa alla prossima
                currentBatch++;
            }
            if (currentBatch == batchNum)
                break;

            if (isGateOpen && this.arrivalIsNeeded(this.eventList)) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate

            }

            eventList.sort(Comparator.comparingDouble(event -> event.eventTime)); //ordino gli eventi


            Event nextEvent = eventList.get(0);
            eventList.remove(0); //pop
            jobsProcessed++; //aggiorno il conteggio dei job processati, dato che da qui in poi il codice si occuperà
            //proprio di processare il job di cui ho appena fatto la pop.


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
                        int currentEventListSize = this.eventList.size();
                        center.handleCompletion(nextEvent);
                        if ((center.ID == 3 || center.ID == 6) && currentEventListSize == this.eventList.size()) {
                            /*
                             * Questo if è true se il centro 3 o il centro 6 (quelli finali) hanno processato un completamento,
                             * e il job è uscito dal sistema.
                             * Per il centro 6 questo è sempre vero: un completamento esce SEMPRE dal sistema. Per il centro 3 un
                             * job può uscire dal sistema, o finire nel centro 6 per controlli più approfonditi. In questo caso
                             * quindi, controllando che la size della eventList non sia cambiata, mi assicuro che non è stato generato
                             * un nuovo arrivo. Infatti, nel caso in cui il completamento è del centro 3, e questo invece di far uscire il
                             * job dal sistema lo invii al centro 6, vedrei la lista degli eventi avere una size aumentata di uno, dato
                             * che avrei un nuovo arrivo.
                             */
                        }
                        break;
                    }
                }
            }

        }
        //Simulazione finita, stampo le statistiche
        for (AbstractCenter center : centerList) {
            center.printStats(center.getClass().getSimpleName());
        }

        /*
         * Per ogni centro batch, per ogni metrica, scrivo in un file .dat tutti i valori medi della metrica.
         * Per esempio, per il centro CarVisualCheck avrò un file .dat con il E[Tq] di ogni batch, uno con il
         * E[Ts] di ogni batch, e così via, per ogni metrica, per ogni centro.
         *
         */

        this.generateDatFiles(batchNum, dataMatrix);
        this.estimateDatFiles();

    }

    private void estimateDatFiles() throws IOException {
        List<String> centerNames = Arrays.asList("CarVisualCenter", "CamionVisualCenter", "CarDocCheckCenter", "CamionWeightCenterV3",
                "GoodsControlCenter", "AdvancedCheckCenter");
        List<String> statNames = Arrays.asList("Lambda", "E[Ts]", "E[Ns]", "E[Tq]", "E[Nq]", "meanRho");
        String directory = "batch_files";
        for (String cn : centerNames) {
            for (String sn : statNames) {
                Estimate estimate = new Estimate();
                Acs acs = new Acs();
                estimate.createInterval(directory, cn + "_" + sn);
                acs.autocorrelation(directory, cn + "_" + sn);
            }
        }
    }

    private boolean arrivalIsNeeded(List<Event> eventList) {
        /*
         * Per verificare se serve un nuovo arrivo "esterno" controllo se ne ho già uno nella lista.
         * "Esterno" nel senso che deve arrivare dall'esterno del sistema, e quindi essere un evento indirizzato
         * al centro 1 (arrivo auto) o al centro 2 (arrivo camion). Per esempio: un arrivo con centerID = 5 è un arrivo, ma per
         * il centro numero 5, e significa che arriva necessariamente dal centro 4, quindi non è un arrivo "esterno".
         * Gli arrivi al centro 1 o 2 possono avvenire solo dall'esterno, dato che non ci sono feedback nella rete.
         * (Vedi CentersID.txt per maggiore chiarezza).
         */
        for (Event e : eventList) {
            if (e.eventType == EventType.ARRIVAL && (e.centerID == 1 || e.centerID == 2))
                return false;
        }
        return true;
    }

    private void generateDatFiles(int batchNums, Double[][][] dataMatrix) {
        int i = 0;
        List<String> centerNames = Arrays.asList("CarVisualCenter", "CamionVisualCenter", "CarDocCheckCenter", "CamionWeightCenterV3",
                "GoodsControlCenter", "AdvancedCheckCenter");
        List<String> statNames = Arrays.asList("Lambda", "E[Ts]", "E[Ns]", "E[Tq]", "E[Nq]", "meanRho");
        String directory = "batch_files";
        for (int stat = 0; stat < statNames.size(); stat++) {
            for (i = 0; i < centerList.size(); i++) {
                File dir = new File(directory);
                BufferedWriter bw = null;
                try {
                if (!dir.exists())
                    dir.mkdirs();

                //creo (o apro) il file .dat per la statistica #stat del centro i-simo, in cui raccolgo i dati di ogni batch j

                File file = new File(dir, centerNames.get(i) + "_" + statNames.get(stat) + ".dat");
                if (!file.exists())
                    file.createNewFile();
                FileWriter writer = new FileWriter(file);
                bw = new BufferedWriter(writer);
                for (int j = 0; j < batchNums; j++) {
                    //Scrivo nel file .dat il valore della statistica corrente, per la j-sima batch


                        bw.append(String.valueOf(dataMatrix[i][j][stat]));
                        bw.append("\n");
                        bw.flush();



                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        bw.flush();
                        bw.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }



    public void startSimulationWithTimeSlots(int timeSlotDuration, Double[] carRates, Double[] camionRates){
        jobsProcessed = 0;
        int currentTimeSlot = 1;
        int slotsNumber = carRates.length;
        Event nextEvent = null;

        this.arrivalsController = new ArrivalsController(1/carRates[0], 1/camionRates[0], this);
        this.STOP_TIME = timeSlotDuration * slotsNumber;

        while (isGateOpen || (!eventList.isEmpty() && currentTimeSlot == slotsNumber)) { //seconda condizione: solo l'ultima fascia svuota il sistema

            if (isGateOpen) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate
                jobsProcessed++; //stats for the sim

                //Handling delle fasce orarie. Se cambia la fascia oraria devo aggiornare il tasso di arrivo
                /*TODO è giusto cambiare la fascia oraria su un arrivo GENERATO? Forse andrebbe fatto su un arrivo EFFETTIVO, ovvero quando entra nel sistema, non quando
                viene messo nella coda dei "nextEvent"*/
                if ((nextArrival.eventTime > (timeSlotDuration * currentTimeSlot)) && isGateOpen) { //se il gate è chiuso non devo aggiornare più nulla qui
                    //Devo cambiare fascia oraria
                    currentTimeSlot++;
                    this.arrivalsController.carArrivalRate = 1/carRates[currentTimeSlot - 1]; //-1 perchè currentTimeSlot parte da 1
                    this.arrivalsController.camionArrivalRate = 1/camionRates[currentTimeSlot - 1];//anche qui come sopra

                    //Se sto cambiando fascia oraria -> faccio un dump delle statistiche della fascia oraria appena terminata
                    System.out.println("+++++++ STATS DUMP FASCIA ORARIA #" + (currentTimeSlot - 1) + " +++++++\n"); //-1 perchè è appena stato incrementato
                    //ma le stats sono della fascia appena finita
                    for (AbstractCenter center : centerList) {
                        center.statsDumpForLastTimeSlot(center.getClass().getSimpleName());
                        center.notifyStatsReset();
                    }
                    System.out.println("++++++++++++++++++++++++++++++++++++\n");



                }

            }


            eventList.sort(Comparator.comparingDouble(event -> event.eventTime)); //ordino gli eventi


            nextEvent = eventList.get(0);
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
        System.out.println("+++++++ STATS DUMP FASCIA ORARIA #" + (currentTimeSlot) + " +++++++\n");
        System.out.println("NextEvent time of last event: " + ((nextEvent.eventTime - 86400)/3600) + "hours after gate closed");
        for (AbstractCenter center : centerList) {
            center.statsDumpForLastTimeSlot(center.getClass().getSimpleName());
        }

        System.out.println("+++++++ STATISTICHE FINALI COMPLESSIVE +++++++\n");
        for (AbstractCenter center : centerList) {
            center.printStats(center.getClass().getSimpleName());
        }

        System.out.println("***   DEBUG  ***\nCar arrivals = " + arrivalsController.counterCars +
                "\nCamion arrivals = " + arrivalsController.counterCamion);
        if (arrivalsController.counterCamion + arrivalsController.counterCars != jobsProcessed + 1)
            //-1 perchè genero un arrivo che non userò mai: io ho sempre due arrivi pronti (una macchina e un camion). Appena uno dei due triggera lo
            //stop time, chiudo il gate. A quel punto, l'arrivo rimasto nell'arrivalController è stato generato, ma sarebbe arrivato dopo quello che ha
            //triggerato la chiusura del gate. In pratica è un arrivo che trova il cancello chiuso, e non entra nel sistema, ma viene generato, quindi
            //non lo considero nel computo degli arrivi perchè è corretto che non arrivi mai nel sistema. Se lo facesse vuol dire che ho consentito
            //a un job l'arrivo dopo la chiusura del cancello.
            throw new RuntimeException("Numero Arrivi generati != Numero arrivi nel sistema"); //Non dovrebbe mai accadere, ma è un check

    }

    public void starSimulation() {

        jobsProcessed = 0;
        while (isGateOpen || !eventList.isEmpty()) {

            if (isGateOpen) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate
                jobsProcessed++; //stats for the sim

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
        nextEventController.initArrivals(86400*1);

        nextEventController.starSimulation();

        System.out.println("***   DEBUG  ***\nCar arrivals = " + nextEventController.arrivalsController.counterCars +
                "\nCamion arrivals = " + nextEventController.arrivalsController.counterCamion);
        if (nextEventController.arrivalsController.counterCamion + nextEventController.arrivalsController.counterCars != nextEventController.jobsProcessed + 1)
            //-1 perchè genero un arrivo che non userò mai: io ho sempre due arrivi pronti (una macchina e un camion). Appena uno dei due triggera lo
            //stop time, chiudo il gate. A quel punto, l'arrivo rimasto nell'arrivalController è stato generato, ma sarebbe arrivato dopo quello che ha
            //triggerato la chiusura del gate. In pratica è un arrivo che trova il cancello chiuso, e non entra nel sistema, ma viene generato, quindi
            //non lo considero nel computo degli arrivi perchè è corretto che non arrivi mai nel sistema. Se lo facesse vuol dire che ho consentito
            //a un job l'arrivo dopo la chiusura del cancello.
            throw new RuntimeException("Numero Arrivi generati != Numero arrivi nel sistema"); //Non dovrebbe mai accadere, ma è un check
    }






}
