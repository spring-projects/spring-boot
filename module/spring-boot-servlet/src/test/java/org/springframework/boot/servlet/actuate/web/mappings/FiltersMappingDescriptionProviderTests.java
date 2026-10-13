/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.actuate.web.mappings;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.servlet.actuate.web.mappings.FiltersMappingDescriptionProvider.FiltersMappingDescriptionProviderRuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FiltersMappingDescriptionProvider}.
 *
 * @author Moritz Halbritter
 */
class FiltersMappingDescriptionProviderTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new FiltersMappingDescriptionProviderRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(FilterRegistrationMappingDescription.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

}
