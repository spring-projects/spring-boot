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

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.core.annotation.SynthesizedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActuatorAnnotationsRuntimeHints}.
 *
 * @author Moritz Halbritter
 */
class ActuatorAnnotationsRuntimeHintsTests {

	private final ActuatorAnnotationsRuntimeHints registrar = new ActuatorAnnotationsRuntimeHints();

	private RuntimeHints runtimeHints;

	@BeforeEach
	void setUp() {
		this.runtimeHints = new RuntimeHints();
		this.registrar.registerHints(this.runtimeHints, getClass().getClassLoader());
	}

	@Test
	void shouldRegisterReflectionHints() {
		Set<Class<?>> annotations = Set.of(Endpoint.class, ReadOperation.class, WriteOperation.class,
				DeleteOperation.class, EndpointExtension.class);
		for (Class<?> annotation : annotations) {
			assertThat(RuntimeHintsPredicates.reflection().onType(annotation)
					.withAnyMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.runtimeHints);
		}
	}

	@Test
	void shouldRegisterProxyHints() {
		Set<Class<?>> synthesizedAnnotations = Set.of(Endpoint.class, EndpointExtension.class);
		for (Class<?> synthesizedAnnotation : synthesizedAnnotations) {
			assertThat(
					RuntimeHintsPredicates.proxies().forInterfaces(synthesizedAnnotation, SynthesizedAnnotation.class))
							.accepts(this.runtimeHints);
		}
	}

}
