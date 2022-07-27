/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.template;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TemplateRuntimeHints}.$
 *
 * @author Stephane Nicoll
 */
class TemplateRuntimeHintsTests {

	public static final Predicate<RuntimeHints> TEST_PREDICATE = RuntimeHintsPredicates.resource()
			.forResource("templates/something/hello.html");

	@Test
	void templateRuntimeHintsIsRegistered() {
		Iterable<RuntimeHintsRegistrar> registrar = AotServices.factories().load(RuntimeHintsRegistrar.class);
		assertThat(registrar).anyMatch(TemplateRuntimeHints.class::isInstance);
	}

	@Test
	void contributeWhenTemplateLocationExists() {
		RuntimeHints runtimeHints = contribute(getClass().getClassLoader());
		assertThat(TEST_PREDICATE.test(runtimeHints)).isTrue();
	}

	@Test
	void contributeWhenTemplateLocationDoesNotExist() {
		FilteredClassLoader classLoader = new FilteredClassLoader(new ClassPathResource("templates"));
		RuntimeHints runtimeHints = contribute(classLoader);
		assertThat(TEST_PREDICATE.test(runtimeHints)).isFalse();
	}

	private RuntimeHints contribute(ClassLoader classLoader) {
		RuntimeHints runtimeHints = new RuntimeHints();
		new TemplateRuntimeHints().registerHints(runtimeHints, classLoader);
		return runtimeHints;
	}

}
