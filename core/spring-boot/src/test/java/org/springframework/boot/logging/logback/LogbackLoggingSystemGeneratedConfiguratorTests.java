/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.util.HashSet;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.aot.AotDetector;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackLoggingSystem} when a Logback configurator has been generated
 * during AOT processing.
 *
 * @author Ceki Gülcü
 */
class LogbackLoggingSystemGeneratedConfiguratorTests {

	private final Set<Object> systemPropertyNames = new HashSet<>(System.getProperties().keySet());

	@BeforeEach
	void setup() {
		resetLoggerContext();
	}

	@AfterEach
	void cleanUp() {
		System.getProperties().keySet().retainAll(this.systemPropertyNames);
		resetLoggerContext();
	}

	private void resetLoggerContext() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.reset();
		context.getStatusManager().clear();
	}

	@Test
	void whenGeneratedConfiguratorIsPresentThenItIsUsedToConfigureLogback() {
		String source = new SpringBootModelToJava().translate("""
				<configuration>
					<springProperty scope="context" name="generated" source="example.generated"/>
					<springProfile name="dev">
						<logger name="generated.dev" level="TRACE"/>
					</springProfile>
				</configuration>
				""");
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("dev");
		environment.setProperty("example.generated", "yes");
		TestCompiler.forSystem().withSources(SourceFile.of(source)).compile((compiled) -> {
			System.setProperty(AotDetector.AOT_ENABLED, "true");
			LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(
					Thread.currentThread().getContextClassLoader());
			loggingSystem.beforeInitialize();
			loggingSystem.initialize(new LoggingInitializationContext(environment), null, null);
			try {
				LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
				assertThat(context.getProperty("generated")).isEqualTo("yes");
				assertThat(context.getLogger("generated.dev").getLevel()).isEqualTo(Level.TRACE);
			}
			finally {
				loggingSystem.cleanUp();
			}
		});
	}

	@Test
	void whenGeneratedConfiguratorIsAbsentThenSerializedModelIsUsed() {
		System.setProperty(AotDetector.AOT_ENABLED, "true");
		LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader());
		loggingSystem.beforeInitialize();
		try {
			// Neither a generated configurator nor a serialized model is available, so
			// initialization falls back to the standard (non-AOT) mechanism
			loggingSystem.initialize(new LoggingInitializationContext(new MockEnvironment()), null, null);
			LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
			assertThat(context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.INFO);
		}
		finally {
			loggingSystem.cleanUp();
		}
	}

}
