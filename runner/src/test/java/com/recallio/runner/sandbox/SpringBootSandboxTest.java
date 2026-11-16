package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Des exercices Spring Boot dans le bac à sable Java (ADR 0060) : un contexte complet, une couche web avec MockMvc, une
 * couche de persistance sur H2. Le délai est celui de la production : un exercice doit y tenir, compilation comprise,
 * sur un seul processeur et dans 512 Mo.
 */
class SpringBootSandboxTest {

    private static final Duration PRODUCTION_TIMEOUT = Duration.ofSeconds(30);

    private static final String APPLICATION = """
            package boutique;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class BoutiqueApplication {}
            """;

    private static final String REMISE = """
            package boutique;

            import org.springframework.stereotype.Service;

            @Service
            public class Remise {
                public int appliquer(int prix, int pourcentage) {
                    return prix - prix * pourcentage / 100;
                }
            }
            """;

    @Test
    void shouldPassASpringBootTestThatInjectsAService() {
        RunResult result = run(Map.of("boutique/Remise.java", REMISE), Map.of("boutique/RemiseTest.java", """
                        package boutique;

                        import static org.assertj.core.api.Assertions.assertThat;

                        import org.junit.jupiter.api.Test;
                        import org.springframework.beans.factory.annotation.Autowired;
                        import org.springframework.boot.test.context.SpringBootTest;

                        @SpringBootTest
                        class RemiseTest {

                            @Autowired
                            Remise remise;

                            @Test
                            void appliqueLaRemise() {
                                assertThat(remise.appliquer(200, 10)).isEqualTo(180);
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldReportTheFailedAssertionOfASpringBootTest() {
        RunResult result = run(
                Map.of("boutique/Remise.java", REMISE.replace("prix - prix", "prix + prix")),
                Map.of("boutique/RemiseTest.java", """
                        package boutique;

                        import static org.assertj.core.api.Assertions.assertThat;

                        import org.junit.jupiter.api.Test;
                        import org.springframework.beans.factory.annotation.Autowired;
                        import org.springframework.boot.test.context.SpringBootTest;

                        @SpringBootTest
                        class RemiseTest {

                            @Autowired
                            Remise remise;

                            @Test
                            void appliqueLaRemise() {
                                assertThat(remise.appliquer(200, 10)).isEqualTo(180);
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message()).contains("220");
    }

    @Test
    void shouldPassAWebMvcTestWithAMockedService() {
        RunResult result = run(
                Map.of("boutique/Remise.java", REMISE, "boutique/PrixController.java", """
                        package boutique;

                        import org.springframework.web.bind.annotation.GetMapping;
                        import org.springframework.web.bind.annotation.RequestParam;
                        import org.springframework.web.bind.annotation.RestController;

                        @RestController
                        public class PrixController {

                            private final Remise remise;

                            PrixController(Remise remise) {
                                this.remise = remise;
                            }

                            @GetMapping("/prix")
                            public int prix(@RequestParam int base, @RequestParam int pourcentage) {
                                return remise.appliquer(base, pourcentage);
                            }
                        }
                        """),
                Map.of("boutique/PrixControllerTest.java", """
                        package boutique;

                        import static org.mockito.Mockito.when;
                        import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
                        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
                        import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

                        import org.junit.jupiter.api.Test;
                        import org.springframework.beans.factory.annotation.Autowired;
                        import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
                        import org.springframework.test.context.bean.override.mockito.MockitoBean;
                        import org.springframework.test.web.servlet.MockMvc;

                        @WebMvcTest(PrixController.class)
                        class PrixControllerTest {

                            @Autowired
                            MockMvc mvc;

                            @MockitoBean
                            Remise remise;

                            @Test
                            void renvoieLePrixRemise() throws Exception {
                                when(remise.appliquer(200, 10)).thenReturn(180);

                                mvc.perform(get("/prix").param("base", "200").param("pourcentage", "10"))
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("180"));
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldPassADataJpaTestOnAnInMemoryDatabase() {
        RunResult result = run(
                Map.of("boutique/Produit.java", """
                        package boutique;

                        import jakarta.persistence.Entity;
                        import jakarta.persistence.GeneratedValue;
                        import jakarta.persistence.Id;

                        @Entity
                        public class Produit {

                            @Id
                            @GeneratedValue
                            private Long id;

                            private String nom;
                            private int prix;

                            protected Produit() {}

                            public Produit(String nom, int prix) {
                                this.nom = nom;
                                this.prix = prix;
                            }

                            public String getNom() {
                                return nom;
                            }
                        }
                        """, "boutique/ProduitRepository.java", """
                        package boutique;

                        import java.util.List;
                        import org.springframework.data.jpa.repository.JpaRepository;

                        public interface ProduitRepository extends JpaRepository<Produit, Long> {
                            List<Produit> findByPrixLessThan(int prix);
                        }
                        """),
                Map.of("boutique/ProduitRepositoryTest.java", """
                        package boutique;

                        import static org.assertj.core.api.Assertions.assertThat;

                        import org.junit.jupiter.api.Test;
                        import org.springframework.beans.factory.annotation.Autowired;
                        import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

                        @DataJpaTest
                        class ProduitRepositoryTest {

                            @Autowired
                            ProduitRepository produits;

                            @Test
                            void trouveLesProduitsMoinsChers() {
                                produits.save(new Produit("stylo", 2));
                                produits.save(new Produit("cartable", 40));

                                assertThat(produits.findByPrixLessThan(10))
                                        .extracting(Produit::getNom)
                                        .containsExactly("stylo");
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldRunAProgramOfTheTourThatImportsALibrary() {
        ProgramResult result = SandboxImage.sandbox(PRODUCTION_TIMEOUT)
                .run(new Program(Language.JAVA, List.of(new SourceFile(Program.JAVA_ENTRY_POINT, """
                        import org.springframework.util.StringUtils;

                        void main() {
                            IO.println(StringUtils.capitalize("recallio"));
                        }
                        """))));

        assertThat(result.stdout()).as(result.toString()).isEqualTo("Recallio\n");
    }

    @Test
    void shouldKeepTheLibrariesOutOfCodeThatDoesNotImportThem() {
        ProgramResult result = SandboxImage.sandbox(PRODUCTION_TIMEOUT)
                .run(new Program(Language.JAVA, List.of(new SourceFile(Program.JAVA_ENTRY_POINT, """
                        void main() throws Exception {
                            try {
                                Class.forName("org.springframework.util.StringUtils");
                                IO.println("chargée");
                            } catch (ClassNotFoundException e) {
                                IO.println("absente");
                            }
                        }
                        """))));

        assertThat(result.stdout()).as(result.toString()).isEqualTo("absente\n");
    }

    /** Le code d'un exercice, avec la classe de l'application Spring Boot, et ses tests. */
    private static RunResult run(Map<String, String> code, Map<String, String> tests) {
        List<SourceFile> files =
                new java.util.ArrayList<>(List.of(new SourceFile("boutique/BoutiqueApplication.java", APPLICATION)));
        code.forEach((path, content) -> files.add(new SourceFile(path, content)));
        List<SourceFile> testFiles = tests.entrySet().stream()
                .map(test -> new SourceFile(test.getKey(), test.getValue()))
                .toList();
        return SandboxImage.sandbox(PRODUCTION_TIMEOUT).run(new Submission(Language.JAVA, files, testFiles));
    }
}
