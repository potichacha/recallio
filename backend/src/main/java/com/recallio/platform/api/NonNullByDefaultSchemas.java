package com.recallio.platform.api;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * Applique au contrat OpenAPI la règle du code Java (ADR 0004) : tout est non nul par défaut. Chaque propriété d'un
 * schéma objet devient obligatoire, sauf si elle accepte {@code null}. Le client TypeScript généré n'a ainsi pas à
 * tester {@code undefined} sur des champs toujours présents.
 *
 * <p>Un schéma qui déclare déjà ses propriétés obligatoires ({@code @Schema(requiredProperties = ...)}) est laissé tel
 * quel.
 *
 * <p>Exécuté au build ({@link OpenApiFilter.RunStage#BUILD}) : c'est ce document qui est exporté dans le dépôt.
 */
@OpenApiFilter(stages = OpenApiFilter.RunStage.BUILD)
public class NonNullByDefaultSchemas implements OASFilter {

    @Override
    public Schema filterSchema(Schema schema) {
        Map<String, Schema> properties = schema.getProperties();
        boolean alreadyDeclared =
                schema.getRequired() != null && !schema.getRequired().isEmpty();
        if (properties == null || properties.isEmpty() || alreadyDeclared) {
            return schema;
        }
        List<String> required = new ArrayList<>();
        properties.forEach((name, property) -> {
            if (!acceptsNull(property)) {
                required.add(name);
            }
        });
        schema.setRequired(required);
        return schema;
    }

    private static boolean acceptsNull(Schema property) {
        List<Schema.SchemaType> types = property.getType();
        return types != null && types.contains(Schema.SchemaType.NULL);
    }
}
