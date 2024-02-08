package it.pmcsn.controllers;

import it.pmcsn.rngs.Estimate;
import it.pmcsn.rngs.Rngs;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FiniteHorizonSim {
    private static final int REPLICATIONS = 128;
    /*Lo scopo di questa classe è quello di eseguire una simulazione con le 4 possibili fasce orarie, eseguite in modo
     * consecutivo una all'altra. Lo stato finale della seconda fascia oraria sarà dunque quello iniziale della terza
     * fascia, e così via.
     */


    Double[] carsArrivals = new Double[] {0.048, 0.063, 0.032, 0.016}; //lambda delle auto per le 4 fasce orarie
    Double[] camionArrivals = new Double[] {0.064, 0.076, 0.076, 0.038}; //lambda dei camion nelle fasce orarie
    int[][] config = new int[][] {{1,2,1,1}, {4,4,4,2}, {3,4,2,1}, {7,8,8,4}, {11,13,13,7}, {4,4,4,2}}; //final config
//    int[][] config = new int[][] {{1,2,1,1}, {4,4,4,2}, {3,4,2,1}, {7,8,8,4}, {11,13,13,7}, {4,4,4,4}}; //new QoS

    double[] p_ca = {0.03, 0.01, 0.02, 0.02};
    double[] p_cc = {0.04, 0.04, 0.04, 0.04}; //basic
//    double[] p_cc = {0.05, 0.05, 0.05, 0.05}; //new QoS



    int timeSlotDuration = 21600; //6 ore, durata fascia oraria

    public void simFiniteHorizon() throws IOException {
        System.out.println("** Starting TimeSlots next event simulation **\n");
        long seed = 333L;

        for (int n = 0; n < REPLICATIONS; n++) {
            System.out.println("Replica " + n);
            NextEventController nextEventController = new NextEventController();
            nextEventController.initArrivals(24*60*60); //24h in s
            nextEventController.rngs = new Rngs();
            nextEventController.rngs.plantSeeds(seed);
            nextEventController.startFiniteHorizonSim(config, n, p_ca, p_cc, carsArrivals, camionArrivals);

            //genero il seed per la prossima iterazione
            nextEventController.rngs.selectStream(255);
            seed = nextEventController.rngs.getSeed();
        }

        //Creo File con valori mediati sulle repliche
        int numberOfFiles = REPLICATIONS;

        List<String> centerNames = Arrays.asList("CarVisualCenter", "CamionVisualCenter", "CarDocCheckCenter", "CamionWeightCenterV3",
                "GoodsControlCenter", "AdvancedCheckCenter");
        for (String statName : Arrays.asList("E[Ts]", "meanRho")){
            for (String cn : centerNames) {
                // Ciclo per scorrere le righe
                for (int lineNumber = 1; lineNumber < 73; lineNumber++) { //Per ogni riga
                    List<Object> dataPoints = new ArrayList<>();
                    // Ciclo per scorrere i file
                    for (int fileNumber = 0; fileNumber < numberOfFiles; fileNumber++) { //Per ogni file
                        String fileName = cn + "_" + statName + fileNumber + ".dat";

                        // Leggi la riga corrente dal file
                        String line = readLineFromFile(fileName, lineNumber);

                        // Stampa o elabora il valore della riga
                        if (line != null) {
                            if (line.equals("Infinity") || line.equals("NaN"))
                                dataPoints.add(0.0);
                            else
                                dataPoints.add(Double.parseDouble(line));
                        }
                    }

                    Estimate estimate = new Estimate();
                    double[] dataPointsArray = new double[dataPoints.size()];
                    for (int i = 0; i < dataPoints.size(); i++) {
                        dataPointsArray[i] = (double) dataPoints.get(i);
                    }
                    double[] meanArray = estimate.evalMean(dataPointsArray);
                    double mean = meanArray == null ? 0.0 : meanArray[0]; //se è null <-> 0 job processati in quell'intervallo -> E[Ts] = 0

                    //creo (o apro) il file .dat per la statistica #stat del centro i-simo, in cui raccolgo i dati di ogni batch j

                    File file = new File("replications", cn + "_" + statName + "_AGGREGATE" + ".dat");
                    if (!file.exists())
                        file.createNewFile();
                    FileWriter writer = new FileWriter(file, true);
                    BufferedWriter bw = new BufferedWriter(writer);

                    bw.append(String.valueOf(mean));
                    bw.append("\n");
                    bw.flush();


                }
            }
        }






        System.out.println("Done!");
    }

    public static void main(String[] args) throws IOException {
        FiniteHorizonSim finiteHorizonSim = new FiniteHorizonSim();
        finiteHorizonSim.simFiniteHorizon();
    }

    // Metodo per leggere una specifica riga da un file
    private static String readLineFromFile(String fileName, int lineNumber) {
        try (BufferedReader reader = new BufferedReader(new FileReader("replications/"+fileName))) {
            // Utilizza il metodo lines() per ottenere uno stream di linee e usa skip() per andare alla riga desiderata
            return reader.lines().skip(lineNumber - 1).findFirst().orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
