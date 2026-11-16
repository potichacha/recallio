package inscription;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class InscriptionService {

    private final AtomicLong sequence = new AtomicLong();

    public Inscrit inscrire(Inscription inscription) {
        return new Inscrit(sequence.incrementAndGet(), inscription.pseudo());
    }
}
