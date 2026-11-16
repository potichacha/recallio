package banque;

import banque.Modeles.Compte;
import banque.Modeles.Virement;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CompteService {

    private final Map<String, Long> soldes = new HashMap<>(Map.of("FR01", 10_000L, "FR02", 500L));

    public synchronized Compte compte(String numero) {
        Long solde = soldes.get(numero);
        if (solde == null) {
            throw new CompteIntrouvableException(numero);
        }
        return new Compte(numero, solde);
    }

    public synchronized void virer(Virement virement) {
        long soldeSource = compte(virement.source()).soldeEnCentimes();
        long soldeDestination = compte(virement.destination()).soldeEnCentimes();
        if (soldeSource < virement.montantEnCentimes()) {
            throw new SoldeInsuffisantException(virement.source());
        }
        soldes.put(virement.source(), soldeSource - virement.montantEnCentimes());
        soldes.put(virement.destination(), soldeDestination + virement.montantEnCentimes());
    }
}
