package it.pmcsn.centers;

import it.pmcsn.event.Event;

import java.util.ArrayList;
import java.util.List;

public class SqArea {
    double area = 0.0;                    /* time integrated number in the node  */
    Double[] serverServices;
    Integer[] serverMask;

    private SqArea(){} //should not use this one

    public SqArea(int numOfServers) {
        this.serverServices = new Double[numOfServers + 1]; //Array con capacità pari al numero dei server
        this.serverMask = new Integer[numOfServers + 1];

        for (int i = 0; i <= numOfServers; i++) {
            serverServices[i] = 0.0; // Imposta ogni elemento a zero
            serverMask[i] = 0; //tutti liberi inizialmente
        }
    }

    public int assignJobToFreeServer(double serviceTime) {
        //Se eseguo questa funzione mi sono già assicurato che ci sia almeno un server libero
        for (int i = 0; i < serverMask.length; i++){
            if (serverMask[i] == 0) { //trovo il server libero
                serverMask[i] = 1;
                serverServices[i] += serviceTime;
                return i;

            }
        }
        throw new RuntimeException("Non ho trovato un server libero, ma avrei dovuto!");
    }

    public void setServerFree(Event event) {
        serverMask[event.assignedServer] = 0;//di nuovo libero
    }








}