package banque;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class VirementService {

    @Transactional
    public Long ouvrir(String titulaire, int solde) {
        Compte compte = new Compte();
        compte.titulaire = titulaire;
        compte.solde = solde;
        compte.persist();
        return compte.id;
    }

    public void virer(Long de, Long vers, int montant) {
        Compte cible = Compte.findById(vers);
        cible.crediter(montant);

        Compte source = Compte.findById(de);
        source.debiter(montant);
    }

    public void virerLot(List<Virement> virements) {
        for (Virement virement : virements) {
            virer(virement.de(), virement.vers(), virement.montant());
        }
    }
}
