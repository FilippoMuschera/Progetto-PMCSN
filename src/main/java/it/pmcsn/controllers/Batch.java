package it.pmcsn.controllers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Batch {



    public void batchMeansInfHorizon() throws IOException {

        NextEventController nextEventController = new NextEventController();
        nextEventController.initArrivals(Integer.MAX_VALUE); //La simulazione si ferma quando finiscono i batch
        nextEventController.initCentersList();
        nextEventController.initRngs();

        nextEventController.startSimulationInfinite(128, 10000);






    }


    public static void main(String[] args) throws IOException {
        Batch batch = new Batch();
        batch.batchMeansInfHorizon();
    }


}
