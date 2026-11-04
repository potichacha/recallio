import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

public final class Journal {

    private Journal() {
    }

    public static Map<String, Long> compterParNiveau(Reader source) throws IOException {
        BufferedReader lecteur = new BufferedReader(source);
        Map<String, Long> compteurs = new TreeMap<>();
        String ligne;
        while ((ligne = lecteur.readLine()) != null) {
            if (!ligne.isBlank()) {
                compteurs.merge(niveau(ligne), 1L, Long::sum);
            }
        }
        return compteurs;
    }

    public static int copierErreurs(Reader source, Writer cible) throws IOException {
        BufferedReader lecteur = new BufferedReader(source);
        BufferedWriter ecrivain = new BufferedWriter(cible);
        int copiees = 0;
        String ligne;
        while ((ligne = lecteur.readLine()) != null) {
            if (!ligne.isBlank() && niveau(ligne).equals("ERROR")) {
                ecrivain.write(ligne);
                ecrivain.newLine();
                copiees++;
            }
        }
        ecrivain.flush();
        return copiees;
    }

    private static String niveau(String ligne) {
        String nettoyee = ligne.strip();
        int espace = nettoyee.indexOf(' ');
        return espace < 0 ? nettoyee : nettoyee.substring(0, espace);
    }
}
