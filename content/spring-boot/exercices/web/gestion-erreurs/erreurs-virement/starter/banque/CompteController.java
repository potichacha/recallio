package banque;

import banque.Modeles.Compte;
import banque.Modeles.Virement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CompteController {

    private final CompteService service;

    public CompteController(CompteService service) {
        this.service = service;
    }

    @GetMapping("/comptes/{numero}")
    public Compte compte(@PathVariable String numero) {
        return service.compte(numero);
    }

    @PostMapping("/virements")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void virer(@RequestBody Virement virement) {
        service.virer(virement);
    }
}
