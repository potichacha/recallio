import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LivraisonTest {

    @Test
    void unParticulierPaieLesFraisEnDessousDe50Euros() {
        assertEquals(490, Livraison.frais(new Commande(TypeClient.PARTICULIER, 4_999, false)));
    }

    @Test
    void unParticulierNePaieRienAPartirDe50Euros() {
        assertEquals(0, Livraison.frais(new Commande(TypeClient.PARTICULIER, 5_000, false)));
    }

    @Test
    void unProfessionnelNePaieJamaisDeFraisStandard() {
        assertEquals(0, Livraison.frais(new Commande(TypeClient.PROFESSIONNEL, 100, false)));
    }

    @Test
    void unClientInternationalPaieToujoursLesFrais() {
        assertEquals(990, Livraison.frais(new Commande(TypeClient.INTERNATIONAL, 100_000, false)));
    }

    @Test
    void lExpressAjouteDixEuros() {
        assertEquals(1_490, Livraison.frais(new Commande(TypeClient.PARTICULIER, 1_000, true)));
        assertEquals(1_000, Livraison.frais(new Commande(TypeClient.PROFESSIONNEL, 1_000, true)));
        assertEquals(1_990, Livraison.frais(new Commande(TypeClient.INTERNATIONAL, 1_000, true)));
    }
}
