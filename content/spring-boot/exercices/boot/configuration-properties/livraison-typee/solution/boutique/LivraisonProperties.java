package boutique;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("boutique.livraison")
@Validated
public record LivraisonProperties(
        @NotNull @PositiveOrZero BigDecimal frais,
        @NotNull @Positive BigDecimal seuilGratuite,
        @DefaultValue("2d") Duration delai) {
}
