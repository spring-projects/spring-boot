/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.generate.InMemoryGeneratedFiles;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.SpringBootJoranConfigurator.GeneratedConfiguratorAotContribution;
import org.springframework.boot.logging.logback.SpringBootJoranConfigurator.LogbackConfigurationAotContribution;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.env.Environment;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GeneratedConfiguratorAotContribution}.
 *
 * @author Ceki Gülcü
 */
class GeneratedConfiguratorAotContributionTests {

	private static final String GENERATED_SOURCE_PATH = ClassUtils
		.convertClassNameToResourcePath(SpringBootTylerConfiguratorBase.GENERATED_CLASS_NAME) + ".java";

	private static final String CONFIGURATION = """
			<configuration>
				<springProperty scope="context" name="contextProperty" source="app.context" defaultValue="fallback"/>
				<springProfile name="dev">
					<logger name="dev" level="DEBUG"/>
				</springProfile>
			</configuration>
			""";

	@TempDir
	@SuppressWarnings("NullAway.Init")
	Path temp;

	@BeforeEach
	void enableAotProcessing() {
		System.setProperty(AbstractAotProcessor.AOT_PROCESSING, "true");
	}

	@AfterEach
	void disableAotProcessing() {
		System.clearProperty(AbstractAotProcessor.AOT_PROCESSING);
	}

	@Test
	void contributesGeneratedConfiguratorSourceAndConstructorHint() throws Exception {
		TestGenerationContext generationContext = new TestGenerationContext();
		applyTo(generationContext, configure(CONFIGURATION));
		InMemoryGeneratedFiles generatedFiles = generationContext.getGeneratedFiles();
		assertThat(generatedFiles.getGeneratedFileContent(Kind.SOURCE, GENERATED_SOURCE_PATH))
			.contains("extends SpringBootTylerConfiguratorBase");
		assertThat(generatedFiles.getGeneratedFiles(Kind.RESOURCE)).isEmpty();
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference.of(SpringBootTylerConfiguratorBase.GENERATED_CLASS_NAME))
			.withMemberCategory(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS))
			.accepts(generationContext.getRuntimeHints());
		assertThat(generationContext.getRuntimeHints().reflection().typeHints()).singleElement()
			.satisfies((hint) -> assertThat(hint.getType().getName())
				.isEqualTo(SpringBootTylerConfiguratorBase.GENERATED_CLASS_NAME));
	}

	@Test
	void generatedConfiguratorCompilesAndConfiguresContext() throws Exception {
		TestGenerationContext generationContext = new TestGenerationContext();
		applyTo(generationContext, configure(CONFIGURATION));
		TestCompiler.forSystem().with(generationContext).compile((compiled) -> {
			Configurator configurator = compiled.getInstance(Configurator.class,
					SpringBootTylerConfiguratorBase.GENERATED_CLASS_NAME);
			MockEnvironment environment = new MockEnvironment();
			environment.setActiveProfiles("dev");
			LoggerContext context = new LoggerContext();
			context.putObject(Environment.class.getName(), environment);
			configurator.configure(context);
			assertThat(context.getProperty("contextProperty")).isEqualTo("fallback");
			assertThat(context.getLogger("dev").getLevel()).hasToString("DEBUG");
		});
	}

	@Test
	void contributionThatMatchesExistingContributionIsAccepted() throws Exception {
		TestGenerationContext generationContext = new TestGenerationContext();
		applyTo(generationContext, configure(CONFIGURATION));
		applyTo(generationContext, configure(CONFIGURATION));
		assertThat(generationContext.getGeneratedFiles().getGeneratedFiles(Kind.SOURCE)).hasSize(1);
	}

	@Test
	void contributionThatDiffersFromExistingContributionThrows() throws Exception {
		TestGenerationContext generationContext = new TestGenerationContext();
		applyTo(generationContext, configure(CONFIGURATION));
		BeanFactoryInitializationAotContribution other = configure(CONFIGURATION.replace("DEBUG", "TRACE"));
		assertThatIllegalStateException().isThrownBy(() -> applyTo(generationContext, other))
			.withMessage("Logging configuration differs from the configuration that has already been written. "
					+ "Update your logging configuration so that it is the same for each context");
	}

	@Test
	void whenTylerIsNotPresentFallsBackToSerializedModel() throws Exception {
		SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(
				new LoggingInitializationContext(new MockEnvironment())) {

			@Override
			boolean isTylerPresent() {
				return false;
			}

		};
		BeanFactoryInitializationAotContribution contribution = configure(configurator, CONFIGURATION);
		assertThat(contribution).isInstanceOf(LogbackConfigurationAotContribution.class);
		TestGenerationContext generationContext = new TestGenerationContext();
		applyTo(generationContext, contribution);
		assertThat(generationContext.getGeneratedFiles().getGeneratedFiles(Kind.SOURCE)).isEmpty();
		assertThat(generationContext.getGeneratedFiles().getGeneratedFiles(Kind.RESOURCE))
			.containsKeys("META-INF/spring/logback-model", "META-INF/spring/logback-pattern-rules");
	}

	@Test
	void whenConfigurationCannotBeTranslatedAotProcessingFails() {
		String configuration = """
				<configuration>
					<logger name="test" level="INFO">
						<springProfile name="dev"/>
					</logger>
				</configuration>
				""";
		assertThatRuntimeException().isThrownBy(() -> configure(configuration))
			.withStackTraceContaining("<springProfile> elements cannot be nested");
	}

	private BeanFactoryInitializationAotContribution configure(String configuration)
			throws IOException, JoranException {
		return configure(new SpringBootJoranConfigurator(new LoggingInitializationContext(new MockEnvironment())),
				configuration);
	}

	private BeanFactoryInitializationAotContribution configure(SpringBootJoranConfigurator configurator,
			String configuration) throws IOException, JoranException {
		Path file = Files.createTempFile(this.temp, "logback", ".xml");
		Files.writeString(file, configuration);
		URL url = file.toUri().toURL();
		LoggerContext context = new LoggerContext();
		configurator.setContext(context);
		configurator.doConfigure(url);
		BeanFactoryInitializationAotContribution contribution = (BeanFactoryInitializationAotContribution) context
			.getObject(BeanFactoryInitializationAotContribution.class.getName());
		assertThat(contribution).isNotNull();
		return contribution;
	}

	private void applyTo(TestGenerationContext generationContext,
			BeanFactoryInitializationAotContribution contribution) {
		contribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
	}

}
