package it.pmcsn.controllers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Batch {



    public void batchMeansInfHorizon() {

        NextEventController nextEventController = new NextEventController();
        nextEventController.initArrivals();
        nextEventController.initCentersList();
        nextEventController.initRngs();

        nextEventController.startSimulationInfinite(4, 256);






    }

    public static void writeFile(Double[] list, String directoryName, String filename) {
        File directory = new File(directoryName);
        BufferedWriter bw = null;

        try {
            if (!directory.exists())
                directory.mkdirs();

            File file = new File(directory, filename + ".dat");

            if (!file.exists())
                file.createNewFile();

            FileWriter writer = new FileWriter(file);
            bw = new BufferedWriter(writer);


            for (Double value : list) {
                bw.append(String.valueOf(value));
                bw.append("\n");
                bw.flush();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                bw.flush();
                bw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        Batch batch = new Batch();
        batch.batchMeansInfHorizon();
    }


}
