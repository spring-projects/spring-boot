/*
 * Copyright 2012-2019 the original author or authors.
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
