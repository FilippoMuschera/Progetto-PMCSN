package it.pmcsn.controllers;

import java.io.IOException;

public class Batch {



    public void batchMeansInfHorizon() throws IOException {

        NextEventController nextEventController = new NextEventController();
        nextEventController.initArrivals(Integer.MAX_VALUE); //La simulazione si ferma quando finiscono i batch
        nextEventController.initCentersList();
        nextEventController.initRngs();

        nextEventController.startSimulationInfinite(128, 10000); //batch 10000 consente di avere autocorrelation (lag 1) < 0.2. In verifica utilizzato k = 3000






    }


    public static void main(String[] args) throws IOException {
        Batch batch = new Batch();
        batch.batchMeansInfHorizon();
    }


}
