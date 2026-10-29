package com.recallio;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Règles d'architecture du monolithe modulaire (CLAUDE.md, section 4.2 et ADR 0001).
 *
 * <p>Un module est un package {@code com.recallio.<module>}. Le package {@code com.recallio.platform} regroupe le
 * transverse (erreurs, temps, sécurité) et peut être utilisé par tous les modules.
 */
class ArchitectureTest {

    private static final String ROOT = "com.recallio";
    private static final String PLATFORM_MODULE = "platform";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(ROOT);

    @Test
    void domainShouldNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "io.quarkus..",
                        "jakarta..",
                        "com.fasterxml..",
                        "org.hibernate..",
                        "io.vertx..",
                        "io.smallrye..",
                        "org.eclipse.microprofile..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void layersShouldDependInwards() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Api")
                .definedBy("..api..")
                .layer("Application")
                .definedBy("..application..")
                .layer("Domain")
                .definedBy("..domain..")
                .layer("Infrastructure")
                .definedBy("..infrastructure..")
                .whereLayer("Api")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("Api", "Infrastructure")
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api")
                .whereLayer("Infrastructure")
                .mayNotBeAccessedByAnyLayer()
                .check(CLASSES);
    }

    @Test
    void modulesShouldBeFreeOfCycles() {
        slices().matching(ROOT + ".(*)..")
                .should()
                .beFreeOfCycles()
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void modulesShouldOnlyUseThePublicApiOfOtherModules() {
        noClasses()
                .should(accessInternalsOfAnotherModule())
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    /**
     * L'API publique d'un module est son package racine ({@code com.recallio.content}) ; ses sous-packages
     * ({@code domain}, {@code infrastructure}...) sont internes.
     */
    private static ArchCondition<JavaClass> accessInternalsOfAnotherModule() {
        return new ArchCondition<>("access internal packages of another module") {
            @Override
            public void check(JavaClass origin, ConditionEvents events) {
                for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    Optional<String> originModule = moduleOf(origin);
                    Optional<String> targetModule = moduleOf(target);
                    boolean crossesModules = originModule.isPresent()
                            && targetModule.isPresent()
                            && !originModule.equals(targetModule)
                            && !targetModule.get().equals(PLATFORM_MODULE);
                    if (crossesModules && isInternal(target, targetModule.get())) {
                        events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
                    }
                }
            }
        };
    }

    private static Optional<String> moduleOf(JavaClass javaClass) {
        String packageName = javaClass.getPackageName();
        if (!packageName.startsWith(ROOT + ".")) {
            return Optional.empty();
        }
        String relative = packageName.substring(ROOT.length() + 1);
        int dot = relative.indexOf('.');
        return Optional.of(dot < 0 ? relative : relative.substring(0, dot));
    }

    private static boolean isInternal(JavaClass target, String module) {
        return !target.getPackageName().equals(ROOT + "." + module);
    }
}
