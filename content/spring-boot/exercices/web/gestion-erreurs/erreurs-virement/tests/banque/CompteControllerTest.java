package banque;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import banque.Modeles.Compte;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CompteController.class)
class CompteControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CompteService service;

    private ResultActions virer(String source, String destination, long montant) throws Exception {
        return mvc.perform(post("/api/virements")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"source\": \"%s\", \"destination\": \"%s\", \"montantEnCentimes\": %d}"
                        .formatted(source, destination, montant)));
    }

    @Test
    void renvoieLeCompteQuandIlExiste() throws Exception {
        when(service.compte("FR01")).thenReturn(new Compte("FR01", 10_000));

        mvc.perform(get("/api/comptes/FR01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldeEnCentimes").value(10_000));
    }

    @Test
    void unCompteInconnuRepond404AvecLeCodeCompteIntrouvable() throws Exception {
        when(service.compte("FR99")).thenThrow(new CompteIntrouvableException("FR99"));

        mvc.perform(get("/api/comptes/FR99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPTE_INTROUVABLE"))
                .andExpect(jsonPath("$.message").value("Compte FR99 introuvable"));
    }

    @Test
    void unVirementReussiRepond204() throws Exception {
        virer("FR01", "FR02", 2_000).andExpect(status().isNoContent());
    }

    @Test
    void unSoldeInsuffisantRepond409AvecLeCodeSoldeInsuffisant() throws Exception {
        doThrow(new SoldeInsuffisantException("FR02")).when(service).virer(any());

        virer("FR02", "FR01", 90_000)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SOLDE_INSUFFISANT"))
                .andExpect(jsonPath("$.message").value("Solde insuffisant sur le compte FR02"));
    }

    @Test
    void unVirementVersUnCompteInconnuRepondAussi404() throws Exception {
        doThrow(new CompteIntrouvableException("FR99")).when(service).virer(any());

        virer("FR01", "FR99", 100)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMPTE_INTROUVABLE"));
    }
}
