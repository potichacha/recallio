package boutique;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Journal {

    private final List<String> messages = new ArrayList<>();

    public void noter(String message) {
        messages.add(message);
    }

    public List<String> messages() {
        return List.copyOf(messages);
    }
}
