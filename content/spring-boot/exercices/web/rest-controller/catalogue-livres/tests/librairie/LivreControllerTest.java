package librairie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LivreController.class)
class LivreControllerTest {

    private static final Livre DUNE = new Livre("9782266320481", "Dune", "Frank Herbert");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CatalogueService catalogue;

    @Test
    void listeTousLesLivresEnJson() throws Exception {
        when(catalogue.tous()).thenReturn(List.of(DUNE));

        mvc.perform(get("/api/livres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].titre").value("Dune"));
    }

    @Test
    void renvoieUnLivreParSonIsbn() throws Exception {
        when(catalogue.trouver("9782266320481")).thenReturn(DUNE);

        mvc.perform(get("/api/livres/9782266320481"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auteur").value("Frank Herbert"));
    }

    @Test
    void repond404QuandLeLivreNExistePas() throws Exception {
        when(catalogue.trouver("0000000000000")).thenThrow(new LivreIntrouvableException("0000000000000"));

        mvc.perform(get("/api/livres/0000000000000")).andExpect(status().isNotFound());
    }

    @Test
    void ajouteLeLivreRecuEnJsonEtRepond201() throws Exception {
        when(catalogue.ajouter(any(Livre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post("/api/livres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "9782070360024", "titre": "L'Étranger", "auteur": "Albert Camus"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("L'Étranger"));

        verify(catalogue).ajouter(new Livre("9782070360024", "L'Étranger", "Albert Camus"));
    }
}
