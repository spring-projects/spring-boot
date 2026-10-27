/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.pattern.PatternLayoutBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.test.tools.SourceFile;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringBootModelToJava} and {@link SpringBootTylerConfiguratorBase}.
 *
 * @author Ceki Gülcü
 */
class SpringBootModelToJavaTests {

	private static final String CONFIGURATION = """
			<configuration>
				<include resource="org/springframework/boot/logging/logback/base.xml"/>
				<springProperty scope="context" name="contextProperty" source="app.context" defaultValue="fallback"/>
				<springProperty scope="context" name="noDefaultProperty" source="app.no-default"/>
				<springProperty name="localLevel" source="app.level" defaultValue="INFO"/>
				<logger name="local" level="${localLevel}"/>
				<property name="profileVariable" value="qa"/>
				<logger name="dev" level="INFO"/>
				<springProfile name="dev">
					<logger name="dev" level="DEBUG"/>
					<springProfile name="!eu">
						<logger name="dev.not-eu" level="TRACE"/>
					</springProfile>
					<appender name="DEV" class="ch.qos.logback.core.ConsoleAppender">
						<encoder>
							<pattern>DEV %m%n</pattern>
						</encoder>
					</appender>
					<logger name="dev.appender" level="INFO">
						<appender-ref ref="DEV"/>
					</logger>
				</springProfile>
				<springProfile name="prod, staging">
					<logger name="prod" level="WARN"/>
				</springProfile>
				<springProfile name="${profileVariable}">
					<logger name="qa" level="ERROR"/>
				</springProfile>
			</configuration>
			""";

	@TempDir
	@SuppressWarnings("NullAway.Init")
	Path temp;

	@Test
	void generatedConfiguratorMatchesJoranConfigurator() {
		String source = new SpringBootModelToJava().translate(CONFIGURATION);
		List<MockEnvironment> environments = List.of(new MockEnvironment(), environment("dev"),
				environment("dev", "eu"), environment("staging"), environment("qa"),
				environment().withProperty("app.context", "custom")
					.withProperty("app.no-default", "set")
					.withProperty("app.level", "WARN"));
		compile(source, (configurator) -> {
			for (MockEnvironment environment : environments) {
				Map<String, String> expected = describe(configureWithJoran(CONFIGURATION, environment));
				Map<String, String> actual = describe(configureWith(configurator, environment));
				assertThat(actual).as("profiles %s", List.of(environment.getActiveProfiles())).isEqualTo(expected);
			}
		});
	}

	@Test
	void springPropertiesAndProfilesAreResolvedAtRuntime() {
		String source = new SpringBootModelToJava().translate(CONFIGURATION);
		compile(source, (configurator) -> {
			LoggerContext withDefaults = configureWith(configurator, new MockEnvironment());
			assertThat(withDefaults.getProperty("contextProperty")).isEqualTo("fallback");
			assertThat(withDefaults.getLogger("local").getLevel()).hasToString("INFO");
			assertThat(withDefaults.getLogger("dev").getLevel()).hasToString("INFO");
			MockEnvironment environment = environment("dev").withProperty("app.context", "custom")
				.withProperty("app.level", "WARN");
			LoggerContext customized = configureWith(configurator, environment);
			assertThat(customized.getProperty("contextProperty")).isEqualTo("custom");
			assertThat(customized.getLogger("local").getLevel()).hasToString("WARN");
			assertThat(customized.getLogger("dev").getLevel()).hasToString("DEBUG");
			assertThat(customized.getLogger("dev.appender").getAppender("DEV")).isNotNull();
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void conversionRulesAreRegistered() {
		String source = new SpringBootModelToJava().translate(CONFIGURATION);
		compile(source, (configurator) -> {
			LoggerContext context = configureWith(configurator, new MockEnvironment());
			Map<String, ?> registry = (Map<String, ?>) context
				.getObject(CoreConstants.PATTERN_RULE_REGISTRY_FOR_SUPPLIERS);
			assertThat(registry).containsKeys("clr", "correlationId", "esb", "wex", "wEx");
		});
	}

	@Test
	void translateWhenSpringProfileIsNestedInLoggerThrowsException() {
		String configuration = """
				<configuration>
					<logger name="test" level="INFO">
						<springProfile name="dev"/>
					</logger>
				</configuration>
				""";
		assertThatIllegalStateException().isThrownBy(() -> new SpringBootModelToJava().translate(configuration))
			.withMessageContaining(
					"<springProfile> elements cannot be nested within an <appender>, <logger> or <root> element");
	}

	@Test
	void translateWhenSpringPropertyHasNoSourceThrowsException() {
		String configuration = """
				<configuration>
					<springProperty name="test"/>
				</configuration>
				""";
		assertThatIllegalStateException().isThrownBy(() -> new SpringBootModelToJava().translate(configuration))
			.withMessageContaining("The \"name\" and \"source\" attributes of <springProperty> must be set");
	}

	@Test
	void translateWhenIncludeCannotBeInlinedThrowsException() {
		String configuration = """
				<configuration>
					<include resource="does/not/exist.xml"/>
				</configuration>
				""";
		assertThatIllegalStateException().isThrownBy(() -> new SpringBootModelToJava().translate(configuration))
			.withMessageContaining("Logback configuration problems detected");
	}

	private MockEnvironment environment(String... profiles) {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles(profiles);
		return environment;
	}

	private void compile(String source, Consumer<Supplier<Configurator>> configurator) {
		TestCompiler.forSystem()
			.withCompilerOptions("-Xlint:deprecation,removal", "-Werror")
			.withSources(SourceFile.of(source))
			.compile((compiled) -> configurator.accept(() -> compiled.getInstance(Configurator.class,
					SpringBootTylerConfiguratorBase.GENERATED_CLASS_NAME)));
	}

	private LoggerContext configureWith(Supplier<Configurator> configurator, Environment environment) {
		LoggerContext context = createContext(environment);
		configurator.get().configure(context);
		return context;
	}

	private LoggerContext configureWithJoran(String configuration, MockEnvironment environment) {
		LoggerContext context = createContext(environment);
		SpringBootJoranConfigurator configurator = new SpringBootJoranConfigurator(
				new LoggingInitializationContext(environment));
		configurator.setContext(context);
		try {
			configurator.doConfigure(new ByteArrayInputStream(configuration.getBytes(StandardCharsets.UTF_8)));
		}
		catch (JoranException ex) {
			throw new IllegalStateException(ex);
		}
		return context;
	}

	private LoggerContext createContext(Environment environment) {
		LoggerContext context = new LoggerContext();
		context.putObject(Environment.class.getName(), environment);
		context.putProperty("LOG_FILE", this.temp.resolve("spring.log").toString());
		return context;
	}

	private Map<String, String> describe(LoggerContext context) {
		Map<String, String> description = new TreeMap<>();
		context.getLoggerList()
			.stream()
			.filter((logger) -> logger.getLevel() != null || logger.iteratorForAppenders().hasNext())
			.sorted(Comparator.comparing(Logger::getName))
			.forEach((logger) -> description.put("logger " + logger.getName(), describe(logger)));
		context.getCopyOfPropertyMap().forEach((name, value) -> description.put("property " + name, value));
		context.stop();
		return description;
	}

	private String describe(Logger logger) {
		List<String> appenders = new ArrayList<>();
		logger.iteratorForAppenders().forEachRemaining((appender) -> appenders.add(describe(appender)));
		return "level=" + logger.getLevel() + " additive=" + logger.isAdditive() + " appenders=" + appenders;
	}

	private String describe(Appender<ILoggingEvent> appender) {
		StringBuilder description = new StringBuilder(appender.getName() + ":" + appender.getClass().getName());
		description.append(" started=").append(appender.isStarted());
		description.append(" filters=")
			.append(appender.getCopyOfAttachedFiltersList()
				.stream()
				.map((filter) -> filter.getClass().getName() + ReflectionTestUtils.getField(filter, "level"))
				.toList());
		if (appender instanceof OutputStreamAppender<ILoggingEvent> outputStreamAppender) {
			Encoder<ILoggingEvent> encoder = outputStreamAppender.getEncoder();
			description.append(" encoder=").append(encoder.getClass().getName());
			if (encoder instanceof LayoutWrappingEncoder<ILoggingEvent> layoutWrappingEncoder) {
				description.append(" charset=").append(layoutWrappingEncoder.getCharset());
				if (layoutWrappingEncoder.getLayout() instanceof PatternLayoutBase<?> layout) {
					description.append(" pattern=").append(layout.getPattern());
				}
			}
		}
		if (appender instanceof RollingFileAppender<ILoggingEvent> rollingFileAppender) {
			description.append(" file=").append(rollingFileAppender.getFile());
			RollingPolicy rollingPolicy = rollingFileAppender.getRollingPolicy();
			if (rollingPolicy instanceof SizeAndTimeBasedRollingPolicy<?> policy) {
				description.append(" fileNamePattern=").append(policy.getFileNamePattern());
				description.append(" maxHistory=").append(policy.getMaxHistory());
				description.append(" cleanHistoryOnStart=").append(policy.isCleanHistoryOnStart());
				description.append(" maxFileSize=").append(ReflectionTestUtils.getField(policy, "maxFileSize"));
				description.append(" totalSizeCap=").append(ReflectionTestUtils.getField(policy, "totalSizeCap"));
			}
		}
		return description.toString();
	}

}
