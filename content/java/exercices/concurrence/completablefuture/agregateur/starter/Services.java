import java.util.List;

public interface Services {

    String nom(String id);

    List<String> commandes(String id);

    int points(String id);
}
