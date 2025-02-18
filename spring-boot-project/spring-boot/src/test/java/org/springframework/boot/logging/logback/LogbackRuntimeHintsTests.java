/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.pattern.SyslogStartConverter;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.rolling.helper.DateTokenConverter;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackRuntimeHints}.
 *
 * @author Andy Wilkinson
 */
class LogbackRuntimeHintsTests {

	@Test
	void registersHintsForTypesCheckedByLogbackLoggingSystem() {
		ReflectionHints reflection = registerHints();
		assertThat(reflection.getTypeHint(LoggerContext.class)).isNotNull();
		assertThat(reflection.getTypeHint(SLF4JBridgeHandler.class)).isNotNull();
	}

	@Test
	void registersHintsForBuiltInLogbackConverters() {
		ReflectionHints reflection = registerHints();
		assertThat(logbackConverters()).allSatisfy(registeredForPublicConstructorInvocation(reflection));
	}

	@Test
	void registersHintsForSpringBootConverters() throws IOException {
		ReflectionHints reflection = registerHints();
		assertThat(converterClasses()).allSatisfy(registeredForPublicConstructorInvocation(reflection));
	}

	@SuppressWarnings("unchecked")
	private Stream<Class<Converter<?>>> converterClasses() throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		return Stream.of(resolver.getResources("classpath:org/springframework/boot/logging/logback/*.class"))
			.filter(Resource::isFile)
			.map(this::loadClass)
			.filter(Converter.class::isAssignableFrom)
			.map((type) -> (Class<Converter<?>>) type);
	}

	private Class<?> loadClass(Resource resource) {
		try {
			return getClass().getClassLoader()
				.loadClass("org.springframework.boot.logging.logback." + resource.getFilename().replace(".class", ""));
		}
		catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Test
	void doesNotRegisterHintsWhenLoggerContextIsNotAvailable() {
		RuntimeHints hints = new RuntimeHints();
		new LogbackRuntimeHints().registerHints(hints, ClassLoader.getPlatformClassLoader());
		assertThat(hints.reflection().typeHints()).isEmpty();
	}

	private ReflectionHints registerHints() {
		RuntimeHints hints = new RuntimeHints();
		new LogbackRuntimeHints().registerHints(hints, getClass().getClassLoader());
		ReflectionHints reflection = hints.reflection();
		return reflection;
	}

	private Consumer<Class<?>> registeredForPublicConstructorInvocation(ReflectionHints reflection) {
		return (converter) -> {
			TypeHint typeHint = reflection.getTypeHint(converter);
			assertThat(typeHint).isNotNull();
			assertThat(typeHint.getMemberCategories()).containsExactly(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		};
	}

	private List<Class<?>> logbackConverters() {
		return List.of(DateTokenConverter.class, IntegerTokenConverter.class, SyslogStartConverter.class);
	}

}
