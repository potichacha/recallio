package banque;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VirementService {

    private final CompteRepository comptes;

    public VirementService(CompteRepository comptes) {
        this.comptes = comptes;
    }

    @Transactional
    public void virer(Long de, Long vers, int montant) {
        Compte cible = comptes.findById(vers).orElseThrow();
        cible.crediter(montant);
        comptes.save(cible);

        Compte source = comptes.findById(de).orElseThrow();
        source.debiter(montant);
        comptes.save(source);
    }

    @Transactional
    public void virerLot(List<Virement> virements) {
        for (Virement virement : virements) {
            virer(virement.de(), virement.vers(), virement.montant());
        }
    }
}
