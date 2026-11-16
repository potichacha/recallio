package boutique;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

// Des contextes réduits à LivraisonProperties : rapides, et un par configuration à essayer.
class LivraisonPropertiesTest {

    @EnableConfigurationProperties(LivraisonProperties.class)
    static class Configuration {
    }

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(Configuration.class);

    @Test
    void prendDeuxJoursDeDelaiQuandLaProprieteEstAbsente() {
        contexte.withPropertyValues("boutique.livraison.frais=4.90", "boutique.livraison.seuil-gratuite=50")
                .run(context -> assertThat(context.getBean(LivraisonProperties.class).delai())
                        .isEqualTo(Duration.ofDays(2)));
    }

    @Test
    void refuseDeDemarrerAvecDesFraisNegatifs() {
        contexte.withPropertyValues("boutique.livraison.frais=-1", "boutique.livraison.seuil-gratuite=50")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().isInstanceOf(BindValidationException.class));
    }

    @Test
    void refuseDeDemarrerSansSeuilDeGratuite() {
        contexte.withPropertyValues("boutique.livraison.frais=4.90")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().isInstanceOf(BindValidationException.class));
    }

    @Test
    void refuseDeDemarrerAvecUnSeuilNul() {
        contexte.withPropertyValues("boutique.livraison.frais=4.90", "boutique.livraison.seuil-gratuite=0")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().rootCause().isInstanceOf(BindValidationException.class));
    }
}
