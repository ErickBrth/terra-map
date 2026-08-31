package com.terramap.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces the dependency rule from the project guide, section 3.1:
 * arrows always point toward the domain. {@code domain} must never import
 * Spring, JPA, or the {@code adapter} package.
 */
class HexagonalArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.terramap");
    }

    @Test
    void domainMustNotDependOnSpringFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnJakartaPersistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..");

        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnAdapterLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..adapter..");

        rule.check(classes);
    }

    @Test
    void applicationMustNotDependOnAdapterLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..adapter..");

        rule.check(classes);
    }

    @Test
    void applicationPortsMustNotDependOnSpringFramework() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application.port..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    void adaptersMustNotDependOnEachOtherDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in..")
                .should().dependOnClassesThat().resideInAnyPackage("..adapter.out..");

        rule.check(classes);
    }

    @Test
    void repositoryImplementationsResideInPersistenceAdapter() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("PersistenceAdapter")
                .should().resideInAPackage("..adapter.out.persistence..");

        rule.check(classes);
    }
}
