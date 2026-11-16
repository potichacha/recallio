package bibliotheque;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmpruntService {

    private final Clock horloge;
    private final Duration duree;
    private final Map<String, Emprunt> enCours = new ConcurrentHashMap<>();

    public EmpruntService(Clock horloge, @Value("${bibliotheque.duree-emprunt:21d}") Duration duree) {
        this.horloge = horloge;
        this.duree = duree;
    }

    public Emprunt emprunter(String isbn, String lecteur) {
        Emprunt emprunt = new Emprunt(isbn, lecteur, LocalDate.now(horloge).plusDays(duree.toDays()));
        if (enCours.putIfAbsent(isbn, emprunt) != null) {
            throw new LivreIndisponibleException(isbn);
        }
        return emprunt;
    }

    public void rendre(String isbn) {
        enCours.remove(isbn);
    }
}
