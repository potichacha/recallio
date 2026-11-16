package inscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(InscriptionController.class)
class InscriptionControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    InscriptionService service;

    private ResultActions inscrire(String json) throws Exception {
        return mvc.perform(post("/api/inscriptions").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void refuse(String json) throws Exception {
        inscrire(json).andExpect(status().isBadRequest());
        verify(service, never()).inscrire(any());
    }

    @Test
    void accepteUneInscriptionValide() throws Exception {
        when(service.inscrire(any())).thenReturn(new Inscrit(1, "ada"));

        inscrire("""
                {"pseudo": "ada", "email": "ada@exemple.fr", "age": 36}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pseudo").value("ada"));
    }

    @Test
    void accepteUnJoueurDeSeizeAns() throws Exception {
        when(service.inscrire(any())).thenReturn(new Inscrit(2, "lea"));

        inscrire("""
                {"pseudo": "lea", "email": "lea@exemple.fr", "age": 16}
                """).andExpect(status().isCreated());
    }

    @Test
    void refuseUnPseudoFaitDEspaces() throws Exception {
        refuse("""
                {"pseudo": "   ", "email": "ada@exemple.fr", "age": 36}
                """);
    }

    @Test
    void refuseUnPseudoDeDeuxCaracteres() throws Exception {
        refuse("""
                {"pseudo": "ad", "email": "ada@exemple.fr", "age": 36}
                """);
    }

    @Test
    void refuseUnPseudoDePlusDeVingtCaracteres() throws Exception {
        refuse("""
                {"pseudo": "ada-lovelace-la-premiere", "email": "ada@exemple.fr", "age": 36}
                """);
    }

    @Test
    void refuseUnEmailMalForme() throws Exception {
        refuse("""
                {"pseudo": "ada", "email": "ada.exemple.fr", "age": 36}
                """);
    }

    @Test
    void refuseUneInscriptionSansEmail() throws Exception {
        refuse("""
                {"pseudo": "ada", "age": 36}
                """);
    }

    @Test
    void refuseUnJoueurDeQuinzeAns() throws Exception {
        refuse("""
                {"pseudo": "ada", "email": "ada@exemple.fr", "age": 15}
                """);
    }
}
