package boutique;

public class PaiementRefuseException extends RuntimeException {

    public PaiementRefuseException(String motif) {
        super(motif);
    }
}
