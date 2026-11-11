/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.info.JavaInfoContributor.JavaInfoContributorRuntimeHints;
import org.springframework.boot.info.JavaInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaInfoContributor}
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class JavaInfoContributorTests {

	@Test
	void javaInfoShouldBeAdded() {
		JavaInfoContributor javaInfoContributor = new JavaInfoContributor();
		Info.Builder builder = new Info.Builder();
		javaInfoContributor.contribute(builder);
		Info info = builder.build();
		assertThat(info.getDetails().get("java")).isInstanceOf(JavaInfo.class);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new JavaInfoContributorRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(JavaInfo.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

}
