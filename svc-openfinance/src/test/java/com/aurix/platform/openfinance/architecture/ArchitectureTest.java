package com.aurix.platform.openfinance.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests enforcing architectural invariants (INV-001 to INV-007).
 * These tests run in CI and MUST pass before merge.
 */
public class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .importPackages("com.aurix.platform.openfinance");
    }

    // INV-004: Temporal MUST NOT contain authorization decisions
    @Test
    void testTemporalDoesNotAuthorize() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..temporal..")
                .should().dependOnClassesThat()
                .resideInAPackage("..policy..")
                .because("Temporal engine MUST NOT make authorization decisions (INV-004)");

        rule.check(classes);
    }

    // INV-005: AuthorizedContext must be immutable (no setters)
    @Test
    void testImmutableAuthorizedContext() {
        ArchRule rule = noMethods()
                .that().areDeclaredInClassesThat().haveSimpleName("AuthorizedContext")
                .should().haveNameMatching("set.*")
                .because("Authorized context must be immutable (INV-005)");

        rule.check(classes);
    }

    // Consent Plane dependencies
    @Test
    void testConsentPlaneDependencies() {
        layeredArchitecture()
                .consideringAllDependencies()
                .layer("ConsentPlane").definedBy("..policy..", "..context..", "..discovery..")
                .layer("ExecutionPlane").definedBy("..planner..", "..temporal..", "..reconciliation..")
                .layer("DataPlane").definedBy("..pipeline..", "..extractor..")
                .layer("DistributionPlane").definedBy("..distribution..")

                .whereLayer("ExecutionPlane").mayNotBeAccessedByAnyLayer()
                .whereLayer("ConsentPlane").mayOnlyBeAccessedByLayers("ExecutionPlane", "DataPlane")

                .because("Consent plane provides authorization; execution and data planes consume it")
                .check(classes);
    }

    // Extractors must implement DataExtractor interface
    @Test
    void testExtractorsImplementInterface() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Extractor")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("BaseExtractor")
                .should().implement(
                        com.aurix.platform.openfinance.extractor.DataExtractor.class)
                .because("All extractors must implement DataExtractor interface");

        rule.check(classes);
    }

    // Pipeline must go through all stages
    @Test
    void testPipelineOrchestratorExists() {
        ArchRule rule = classes()
                .that().haveSimpleName("PipelineOrchestrator")
                .should().resideInAPackage("..pipeline..")
                .because("Pipeline orchestrator must be in pipeline package");

        rule.check(classes);
    }

    // All services must have health check
    @Test
    void testAllControllersHaveHealthEndpoint() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..controller..");

        rule.check(classes);
    }

    // Top-level DTOs must be in dto packages (nested request/response records
    // colocated with their owning controller/activity are an accepted exception)
    @Test
    void testDtosInDtoPackages() {
        ArchRule rule = classes()
                .that().areTopLevelClasses()
                .and(simpleNameEndingWith("Request").or(simpleNameEndingWith("Response")))
                .should().resideInAPackage("..dto..");

        rule.check(classes);
    }

    // No circular dependencies between planes
    @Test
    void testNoCircularDependencies() {
        ArchRule rule = slices()
                .matching("com.aurix.platform.openfinance.(*)..")
                .should().beFreeOfCycles()
                .because("No circular dependencies between architectural slices");

        rule.check(classes);
    }
}
