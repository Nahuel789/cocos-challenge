package com.cocos.challenge.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.cocos.challenge", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
	static final ArchRule domain_is_independent = noClasses()
			.that().resideInAPackage("com.cocos.challenge.domain..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"com.cocos.challenge.application..",
					"com.cocos.challenge.infrastructure..",
					"com.cocos.challenge.interfaces..",
					"org.springframework..",
					"jakarta.persistence..",
					"org.hibernate..")
			.because("el dominio no depende de otras capas ni de JPA o Spring");

	@ArchTest
	static final ArchRule application_depends_only_on_domain = noClasses()
			.that().resideInAPackage("com.cocos.challenge.application..")
			.should().dependOnClassesThat().resideInAnyPackage(
					"com.cocos.challenge.infrastructure..",
					"com.cocos.challenge.interfaces..",
					"org.springframework..")
			.orShould().beAnnotatedWith(Service.class)
			.because("application solo depende del dominio y no declara @Service");

	@ArchTest
	static final ArchRule controllers_do_not_use_adapters = noClasses()
			.that().haveSimpleNameEndingWith("Controller")
			.or().areAnnotatedWith(RestController.class)
			.should().dependOnClassesThat().resideInAPackage("..adapter..")
			.orShould().dependOnClassesThat().haveSimpleNameEndingWith("Adapter")
			.because("los controllers dependen de casos de uso, no de adapters de infraestructura");
}
