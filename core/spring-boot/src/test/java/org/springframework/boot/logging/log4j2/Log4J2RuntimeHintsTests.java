/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.log4j2;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.apache.logging.log4j.jul.LogManager;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.ReflectionHintsPredicates;
import org.springframework.aot.hint.predicate.ResourceHintsPredicates;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2RuntimeHints}.
 *
 * @author Piotr P. Karwasz
 * @author Stephane Nicoll
 */
class Log4J2RuntimeHintsTests {

	private static final ReflectionHintsPredicates reflectionHints = RuntimeHintsPredicates.reflection();

	private static final ResourceHintsPredicates resourceHints = RuntimeHintsPredicates.resource();

	@Test
	void registersHintsForTypesCheckedByLog4J2LoggingSystem() {
		RuntimeHints runtimeHints = registerHints();
		assertThat(reflectionHints.onType(Log4jContextFactory.class)).accepts(runtimeHints);
		assertThat(reflectionHints.onType(Log4jBridgeHandler.class)).accepts(runtimeHints);
		assertThat(reflectionHints.onType(LogManager.class)).accepts(runtimeHints);
	}

	@Test
	void registersHintsForLog4j2DefaultConfigurationFiles() {
		RuntimeHints runtimeHints = registerHints();
		assertThat(resourceHints.forResource("org/springframework/boot/logging/log4j2/log4j2.xml"))
			.accepts(runtimeHints);
		assertThat(resourceHints.forResource("org/springframework/boot/logging/log4j2/log4j2-file.xml"))
			.accepts(runtimeHints);
		assertThat(resourceHints.forResource("log4j2.springboot")).accepts(runtimeHints);
	}

	@Test
	void doesNotRegisterHintsWhenLog4jCoreIsNotAvailable() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new Log4J2RuntimeHints().registerHints(runtimeHints, new HidePackagesClassLoader("org.apache.logging.log4j"));
		assertThat(runtimeHints.reflection().typeHints()).isEmpty();
	}

	private RuntimeHints registerHints() {
		RuntimeHints hints = new RuntimeHints();
		new Log4J2RuntimeHints().registerHints(hints, getClass().getClassLoader());
		return hints;
	}

	static final class HidePackagesClassLoader extends URLClassLoader {

		private final String[] hiddenPackages;

		HidePackagesClassLoader(String... hiddenPackages) {
			super(new URL[0], HidePackagesClassLoader.class.getClassLoader());
			this.hiddenPackages = hiddenPackages;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (Arrays.stream(this.hiddenPackages).anyMatch(name::startsWith)) {
				throw new ClassNotFoundException();
			}
			return super.loadClass(name, resolve);
		}

	}

}
