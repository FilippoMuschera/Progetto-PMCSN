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
import java.util.*;

public class NextEventController {

    public Rngs rngs;
    public List<Event> eventList = new ArrayList<>();
    public List<AbstractCenter> centerList = new ArrayList<>();

    public ArrivalsController arrivalsController;

    private int STOP_TIME; //"Gate closed" time

    private int jobsProcessed = 0;

    private boolean isGateOpen = true;



    public void initCentersList() {

        CarVisualControlCenter center1 = new CarVisualControlCenter(1, 18.0, this);
        centerList.add(center1);

        CamionVisualControlCenter center2 = new CamionVisualControlCenter(2, 48.0, this);
        centerList.add(center2);

        CarDocCheck center3 = new CarDocCheck(1, 50.0, this, 0.03);
        centerList.add(center3);

        CamionWeightCenterV3 center4 = new CamionWeightCenterV3(4, 100.0, this);
        centerList.add(center4);

        GoodsControlCenter center5 = new GoodsControlCenter(7, 175.0, this, 0.06);
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
//        this.arrivalsController = new ArrivalsController(62.5, 26.31579, this);
        this.arrivalsController = new ArrivalsController(62.5, 26.31579, this);


        this.STOP_TIME = stopTime;
    }


    public void startFiniteHorizonSim(int[][] config, int iteration) throws IOException {
        CarVisualControlCenter center1 = new CarVisualControlCenter(config[0], 18.0, this);
        centerList.add(center1);

        CamionVisualControlCenter center2 = new CamionVisualControlCenter(config[1], 48.0, this);
        centerList.add(center2);

        CarDocCheck center3 = new CarDocCheck(config[2], 50.0, this, 0.03);
        centerList.add(center3);

        CamionWeightCenterV3 center4 = new CamionWeightCenterV3(config[3], 100.0, this);
        centerList.add(center4);

        GoodsControlCenter center5 = new GoodsControlCenter(config[4], 175.0, this, 0.06);
        centerList.add(center5);

        AdvancedChecksCenter center6 = new AdvancedChecksCenter(config[5], 1000.0, this);
        centerList.add(center6);

        for (AbstractCenter center : centerList) center.setTimeSlot(0); //Set di tutti i centri al primo time slot
        for (int t = 1200; t <= 24*60*60; t += 1200) {
            //Creo eventi di sampling delle statistiche e di cambio time slot
            Event samplingEvent = new Event(EventType.SAMPLING, -1);
            samplingEvent.eventTime = t;
            this.eventList.add(samplingEvent);
            if (t % 21600 == 0 && t < 86400) { //uno ogni 6 ore. L'ultimo non mi interessa perché finirà la simulazione.
                Event changeTS = new Event(EventType.CHANGE_TS, -1);
                changeTS.eventTime = t;
                this.eventList.add(changeTS);
            }
        }

        //INIZIO SIMULAZIONE DELLE 24H
        jobsProcessed = 0;
        int currentTimeSlot = 0;
        int samplingElapsed = 0;
        Double[][][] dataMatrix = new Double[6][73][6]; //6 righe, 73 (sampling ogni 20 minuti = 1200 s) colonne, ogni elemento è un array di 6 double
        /*
         * La DataMatrix contiene una riga per ogni centro, e una colonna per ogni batch. Ogni elemento ij è una lista
         * con tutte le statistiche del centro i per il batch j.
         */

        while (isGateOpen || !eventList.isEmpty()) {

            if (isGateOpen && this.arrivalIsNeeded(this.eventList)) {
                Event nextArrival = this.arrivalsController.getNextArrival();
                eventList.add(nextArrival); //Se il gate è open genero il prossimo arrivo
                isGateOpen = nextArrival.eventTime < this.STOP_TIME;//Se l'ultimo arrivo è oltre il closing time -> chiudo il gate

            }

            eventList.sort(Comparator.comparingDouble(event -> event.eventTime)); //ordino gli eventi


            Event nextEvent = eventList.get(0);
            eventList.remove(0); //pop

            if (nextEvent.eventType == EventType.SAMPLING) {
                int i = 0;
                for (AbstractCenter center : this.centerList) {
                    //sfrutto la funzione di raccolta statistiche che uso anche per i batch, ma per gli eventi di sampling
                    dataMatrix[i][samplingElapsed] = center.getBatchCenterStats().toArray(new Double[0]); //set elemento ij
                    center.notifyStatsReset();
                    i++;
                }

                //Sampling terminato, preparo il prossimo
                samplingElapsed++;
                continue;
            }

            if (nextEvent.eventType == EventType.CHANGE_TS) {
                currentTimeSlot++;
                for (AbstractCenter center : centerList) {
                    //center.printStats(center.getClass().getSimpleName());
                    center.setTimeSlot(currentTimeSlot);
                }
                continue;
            }


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
                        break;
                    }
                }
            }

        }
        //Simulazione finita, ultimo sampling
        int i = 0;
        for (AbstractCenter center : this.centerList) {
            //sfrutto la funzione di raccolta statistiche che uso anche per i batch, ma per gli eventi di sampling
            dataMatrix[i][samplingElapsed] = center.getBatchCenterStats().toArray(new Double[0]); //set elemento ij
            center.notifyStatsReset();
            i++;
        }

        /*
         * Per ogni centro, sampling,e per ogni metrica, scrivo in un file .dat tutti i valori medi della metrica.
         * Per esempio, per il centro CarVisualCheck avrò un file .dat con il E[Tq] di ogni sampling, uno con il
         * E[Ts] di ogni sampling, e così via, per ogni metrica, per ogni centro.
         *
         */

        this.generateDatFiles(73, dataMatrix, "replications", iteration); //73 = numero di eventi di sampling
        //this.estimateDatFiles();

        //FINE SIMULAZIONE SINGOLA REPLICAZIONE

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

        this.generateDatFiles(batchNum, dataMatrix, "batch_files", 1);
        this.estimateDatFiles();

    }

    public void estimateDatFiles() throws IOException {
        List<String> centerNames = Arrays.asList("CarVisualCenter", "CamionVisualCenter", "CarDocCheckCenter", "CamionWeightCenterV3",
                "GoodsControlCenter", "AdvancedCheckCenter");
        List<String> statNames = Arrays.asList("E[Ts]", "E[Tq]");
        String directory = "batch_files";
        for (String cn : centerNames) {
            for (String sn : statNames) {
                Estimate estimate = new Estimate();
                Acs acs = new Acs();
                estimate.createInterval(directory, cn + "_" + sn + 1);
                if (sn.equals("E[Tq]"))
                    acs.autocorrelation(directory, cn + "_" + sn + 1);
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

    public void generateDatFiles(int batchNums, Double[][][] dataMatrix, String dirName, int iter) {
        int i = 0;
        List<String> centerNames = Arrays.asList("CarVisualCenter", "CamionVisualCenter", "CarDocCheckCenter", "CamionWeightCenterV3",
                "GoodsControlCenter", "AdvancedCheckCenter");
        List<String> statNames = Arrays.asList("Lambda", "E[Ts]", "E[Ns]", "E[Tq]", "E[Nq]", "meanRho");
        String directory = dirName;
        for (int stat = 0; stat < statNames.size(); stat++) {
            for (i = 0; i < centerList.size(); i++) {
                File dir = new File(directory);
                BufferedWriter bw = null;
                try {
                if (!dir.exists())
                    dir.mkdirs();

                //creo (o apro) il file .dat per la statistica #stat del centro i-simo, in cui raccolgo i dati di ogni batch j

                File file = new File(dir, centerNames.get(i) + "_" + statNames.get(stat) + iter +".dat");
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










}
