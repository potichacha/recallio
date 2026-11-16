package com.recallio.runner.sandbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.recallio.runner.sandbox.RunResult.Status;
import com.recallio.runner.sandbox.RunResult.TestOutcome;
import com.recallio.runner.sandbox.Submission.SourceFile;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Des exercices Quarkus dans le bac à sable Java (ADR 0061) : une ressource REST avec Panache sur H2 et la validation,
 * un bean CDI injecté, un bean remplacé par un mock. Le délai est celui de la production : l'application doit y démarrer
 * et ses tests y passer, compilation comprise, sur un seul processeur et dans 512 Mo.
 */
class QuarkusSandboxTest {

    private static final Duration PRODUCTION_TIMEOUT = Duration.ofSeconds(30);

    private static final String REMISE = """
            package boutique;

            import jakarta.enterprise.context.ApplicationScoped;

            @ApplicationScoped
            public class Remise {
                public int appliquer(int prix, int pourcentage) {
                    return prix - prix * pourcentage / 100;
                }
            }
            """;

    private static final String PRIX_RESOURCE = """
            package boutique;

            import jakarta.ws.rs.GET;
            import jakarta.ws.rs.Path;
            import jakarta.ws.rs.QueryParam;

            @Path("/prix")
            public class PrixResource {

                private final Remise remise;

                PrixResource(Remise remise) {
                    this.remise = remise;
                }

                @GET
                public int prix(@QueryParam("base") int base, @QueryParam("pourcentage") int pourcentage) {
                    return remise.appliquer(base, pourcentage);
                }
            }
            """;

    @Test
    void shouldPassAQuarkusTestOfAPanacheResource() {
        RunResult result = run(
                Map.of("boutique/Produit.java", """
                        package boutique;

                        import io.quarkus.hibernate.orm.panache.PanacheEntity;
                        import jakarta.persistence.Entity;

                        @Entity
                        public class Produit extends PanacheEntity {
                            public String nom;
                            public int prix;
                        }
                        """, "boutique/ProduitResource.java", """
                        package boutique;

                        import jakarta.transaction.Transactional;
                        import jakarta.validation.Valid;
                        import jakarta.validation.constraints.Min;
                        import jakarta.validation.constraints.NotBlank;
                        import jakarta.ws.rs.GET;
                        import jakarta.ws.rs.POST;
                        import jakarta.ws.rs.Path;
                        import jakarta.ws.rs.core.Response;
                        import java.util.List;

                        @Path("/produits")
                        public class ProduitResource {

                            public record NouveauProduit(@NotBlank String nom, @Min(0) int prix) {}

                            @GET
                            public List<Produit> tous() {
                                return Produit.listAll();
                            }

                            @POST
                            @Transactional
                            public Response creer(@Valid NouveauProduit nouveau) {
                                Produit produit = new Produit();
                                produit.nom = nouveau.nom();
                                produit.prix = nouveau.prix();
                                produit.persist();
                                return Response.status(Response.Status.CREATED).entity(produit).build();
                            }
                        }
                        """),
                Map.of("boutique/ProduitResourceTest.java", """
                        package boutique;

                        import static io.restassured.RestAssured.given;
                        import static org.hamcrest.Matchers.hasItem;
                        import static org.hamcrest.Matchers.is;

                        import io.quarkus.test.junit.QuarkusTest;
                        import io.restassured.http.ContentType;
                        import org.junit.jupiter.api.Test;

                        @QuarkusTest
                        class ProduitResourceTest {

                            @Test
                            void creeUnProduit() {
                                given().contentType(ContentType.JSON)
                                        .body("{\\"nom\\": \\"stylo\\", \\"prix\\": 2}")
                                        .when()
                                        .post("/produits")
                                        .then()
                                        .statusCode(201)
                                        .body("nom", is("stylo"));
                            }

                            @Test
                            void refuseUnNomVide() {
                                given().contentType(ContentType.JSON)
                                        .body("{\\"nom\\": \\"\\", \\"prix\\": 2}")
                                        .when()
                                        .post("/produits")
                                        .then()
                                        .statusCode(400);
                            }

                            @Test
                            void listeLesProduits() {
                                given().contentType(ContentType.JSON)
                                        .body("{\\"nom\\": \\"cahier\\", \\"prix\\": 5}")
                                        .post("/produits");

                                given().when().get("/produits").then().statusCode(200).body("nom", hasItem("cahier"));
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.PASSED);
        assertThat(result.tests()).extracting(TestOutcome::name).hasSize(3);
    }

    @Test
    void shouldReportTheFailedAssertionOfAQuarkusTest() {
        RunResult result = run(
                Map.of("boutique/Remise.java", REMISE.replace("prix - prix", "prix + prix")),
                Map.of("boutique/RemiseTest.java", """
                        package boutique;

                        import static org.junit.jupiter.api.Assertions.assertEquals;

                        import io.quarkus.test.junit.QuarkusTest;
                        import jakarta.inject.Inject;
                        import org.junit.jupiter.api.Test;

                        @QuarkusTest
                        class RemiseTest {

                            @Inject
                            Remise remise;

                            @Test
                            void appliqueLaRemise() {
                                assertEquals(180, remise.appliquer(200, 10));
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.FAILED);
        assertThat(result.tests().getFirst().message()).contains("220");
    }

    @Test
    void shouldPassAQuarkusTestThatMocksABean() {
        RunResult result = run(
                Map.of("boutique/Remise.java", REMISE, "boutique/PrixResource.java", PRIX_RESOURCE),
                Map.of("boutique/PrixResourceTest.java", """
                        package boutique;

                        import static io.restassured.RestAssured.given;
                        import static org.hamcrest.Matchers.is;
                        import static org.mockito.Mockito.when;

                        import io.quarkus.test.InjectMock;
                        import io.quarkus.test.junit.QuarkusTest;
                        import org.junit.jupiter.api.Test;

                        @QuarkusTest
                        class PrixResourceTest {

                            @InjectMock
                            Remise remise;

                            @Test
                            void renvoieLePrixRemise() {
                                when(remise.appliquer(200, 10)).thenReturn(150);

                                given().queryParam("base", 200)
                                        .queryParam("pourcentage", 10)
                                        .when()
                                        .get("/prix")
                                        .then()
                                        .statusCode(200)
                                        .body(is("150"));
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.PASSED);
    }

    @Test
    void shouldReportTheCompilationErrorOfAQuarkusExercise() {
        RunResult result = run(
                Map.of("boutique/Remise.java", REMISE.replace("return prix", "return prixx")),
                Map.of("boutique/RemiseTest.java", """
                        package boutique;

                        import io.quarkus.test.junit.QuarkusTest;
                        import org.junit.jupiter.api.Test;

                        @QuarkusTest
                        class RemiseTest {

                            @Test
                            void compile() {}
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output()).contains("src/boutique/Remise.java").contains("prixx");
    }

    @Test
    void shouldReportAnUnsatisfiedDependencyAsACompilationError() {
        RunResult result = run(
                Map.of(
                        "boutique/Remise.java",
                        REMISE.replace("@ApplicationScoped\n", ""),
                        "boutique/PrixResource.java",
                        PRIX_RESOURCE),
                Map.of("boutique/PrixResourceTest.java", """
                        package boutique;

                        import static io.restassured.RestAssured.given;

                        import io.quarkus.test.junit.QuarkusTest;
                        import org.junit.jupiter.api.Test;

                        @QuarkusTest
                        class PrixResourceTest {

                            @Test
                            void renvoieLePrixRemise() {
                                given().queryParam("base", 200)
                                        .queryParam("pourcentage", 10)
                                        .when()
                                        .get("/prix")
                                        .then()
                                        .statusCode(200);
                            }
                        }
                        """));

        assertThat(result.status()).as(result.toString()).isEqualTo(Status.COMPILATION_ERROR);
        assertThat(result.output())
                .contains("Unsatisfied dependency for type boutique.Remise")
                .doesNotContain("\tat ");
    }

    /** Le code d'un exercice et ses tests. */
    private static RunResult run(Map<String, String> code, Map<String, String> tests) {
        List<SourceFile> files = code.entrySet().stream()
                .map(file -> new SourceFile(file.getKey(), file.getValue()))
                .toList();
        List<SourceFile> testFiles = tests.entrySet().stream()
                .map(test -> new SourceFile(test.getKey(), test.getValue()))
                .toList();
        return SandboxImage.sandbox(PRODUCTION_TIMEOUT).run(new Submission(Language.JAVA, files, testFiles));
    }
}
