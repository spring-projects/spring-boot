/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.classpath;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModifiedClassPathExtension} overriding entries on the class path via
 * parameters.
 *
 * @author Phillip Webb
 */
class ModifiedClassPathExtensionOverridesParameterizedTests {

	@ParameterizedTest
	@ForkedClassPath
	@MethodSource("parameter")
	void classesAreLoadedFromParameter(Class<?> type) {
		assertThat(ApplicationContext.class.getProtectionDomain().getCodeSource().getLocation().toString())
			.endsWith("spring-context-4.1.0.RELEASE.jar");
	}

	static Class<?>[] parameter() {
		return new Class<?>[] { ClassWithOverride.class };
	}

	@ParameterizedTest
	@ForkedClassPath
	@MethodSource("arrayParameter")
	void classesAreLoadedFromParameterInArray(Object[] types) {
		assertThat(ApplicationContext.class.getProtectionDomain().getCodeSource().getLocation().toString())
			.endsWith("spring-context-4.1.0.RELEASE.jar");
	}

	static Stream<Arguments> arrayParameter() {
		Object[] types = new Object[] { ClassWithOverride.class };
		return Stream.of(Arguments.of(new Object[] { types }));
	}

	@ClassPathOverrides("org.springframework:spring-context:4.1.0.RELEASE")
	static class ClassWithOverride {

	}

}
