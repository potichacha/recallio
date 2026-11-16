package bibliotheque;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;

public class EmpruntService {

    private final Clock horloge;
    private final Duration duree;

    public EmpruntService(Clock horloge, @Value("${bibliotheque.duree-emprunt}") Duration duree) {
        this.horloge = horloge;
        this.duree = duree;
    }

    public Emprunt emprunter(String isbn, String lecteur) {
        throw new UnsupportedOperationException("à écrire");
    }

    public void rendre(String isbn) {
        throw new UnsupportedOperationException("à écrire");
    }
}
