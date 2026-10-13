/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthEndpointWebExtensionRuntimeHints}.
 *
 * @author Moritz Halbritter
 */
class HealthEndpointWebExtensionRuntimeHintsTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new HealthEndpointWebExtensionRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		Set<Class<?>> bindingTypes = Set.of(IndicatedHealthDescriptor.class, SystemHealthDescriptor.class,
				CompositeHealthDescriptor.class);
		for (Class<?> bindingType : bindingTypes) {
			assertThat(RuntimeHintsPredicates.reflection()
				.onType(bindingType)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
		}
	}

}
