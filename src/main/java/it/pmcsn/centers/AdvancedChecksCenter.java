package it.pmcsn.centers;

import it.pmcsn.controllers.NextEventController;
import it.pmcsn.event.Event;

public class AdvancedChecksCenter extends AbstractCenter{
    public AdvancedChecksCenter(int servers, double serviceTime, NextEventController controller) {
        super(servers, 6, serviceTime, controller);
    }

    public AdvancedChecksCenter(int[] servers, double serviceTime, NextEventController controller) {
        super(servers, 6, serviceTime, controller);
    }

    @Override
    int getNextCenterId() {
        throw new RuntimeException("Should not be calling this method in the last center of the network");
    }

    @Override
    void generateEventAfterCompl(Event event) {
        /*
         * Metodo vuoto perchè questo è sempre l'ultimo centro della rete. Una volta raggiunto questo centro, qualsiasi
         * sia il risultato del controllo avanzato sulla merce/oggetti trasportati, il job esce comunque dal sistema:
         * - Se il controllo avanzato va a buon fine -> Camion/auto entra nel paese.
         * - Se il controllo avanzato NON va a buon fine la dogana respinge il veicolo, che quindi esce comunque dal sistema.
         */

    }
}
