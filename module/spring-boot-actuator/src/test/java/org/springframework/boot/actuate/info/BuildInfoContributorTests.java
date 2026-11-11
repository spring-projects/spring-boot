/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.info.BuildInfoContributor.BuildInfoContributorRuntimeHints;
import org.springframework.boot.info.BuildProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BuildInfoContributor}.
 *
 * @author Moritz Halbritter
 */
class BuildInfoContributorTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new BuildInfoContributorRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(BuildProperties.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

}
