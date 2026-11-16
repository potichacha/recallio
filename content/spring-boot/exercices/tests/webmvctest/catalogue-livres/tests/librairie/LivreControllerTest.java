package librairie;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Seule la couche web démarre : le contrôleur, Jackson, la validation. Le service est un mock.
@WebMvcTest(LivreController.class)
class LivreControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CatalogueService catalogue;

    @Test
    void renvoieLeLivreQuandIlExiste() throws Exception {
        given(catalogue.trouver("9782070360024"))
                .willReturn(Optional.of(new Livre("9782070360024", "L'Étranger", "Albert Camus")));

        mvc.perform(get("/api/livres/9782070360024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("L'Étranger"))
                .andExpect(jsonPath("$.auteur").value("Albert Camus"));
    }

    @Test
    void repond404QuandLeLivreEstInconnu() throws Exception {
        given(catalogue.trouver("9780000000000")).willReturn(Optional.empty());

        mvc.perform(get("/api/livres/9780000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ajouteUnLivreEtRepond201AvecSonAdresse() throws Exception {
        mvc.perform(post("/api/livres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "9782070360536", "titre": "La Peste", "auteur": "Albert Camus"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/livres/9782070360536")))
                .andExpect(jsonPath("$.titre").value("La Peste"));

        verify(catalogue).ajouter(new Livre("9782070360536", "La Peste", "Albert Camus"));
    }

    @Test
    void refuseUnLivreSansTitreAvec400() throws Exception {
        mvc.perform(post("/api/livres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "9782070360536", "titre": "", "auteur": "Albert Camus"}
                                """))
                .andExpect(status().isBadRequest());

        verify(catalogue, never()).ajouter(any());
    }

    @Test
    void refuseUnIsbnMalFormeAvec400() throws Exception {
        mvc.perform(post("/api/livres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"isbn": "12-34", "titre": "La Peste", "auteur": "Albert Camus"}
                                """))
                .andExpect(status().isBadRequest());

        verify(catalogue, never()).ajouter(any());
    }
}
