package io.github.oumaimacoco.helpdesk;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;

@AnalyzeClasses(
        packages = "io.github.oumaimacoco.helpdesk",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule featurePackagesHaveNoCycles =
            slices().matching("io.github.oumaimacoco.helpdesk.(*)..")
                    .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule basePackagesDoNotDependOnTickets =
            noClasses().that().resideInAnyPackage("..team..", "..user..", "..category..")
                    .should().dependOnClassesThat().resideInAPackage("..ticket..");

    @ArchTest
    static final ArchRule entityFieldsArePrivate =
            fields().that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class)
                    .should().bePrivate();
}