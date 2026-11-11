/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.info.OsInfoContributor.OsInfoContributorRuntimeHints;
import org.springframework.boot.info.OsInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OsInfoContributor}
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class OsInfoContributorTests {

	@Test
	void osInfoShouldBeAdded() {
		OsInfoContributor osInfoContributor = new OsInfoContributor();
		Info.Builder builder = new Info.Builder();
		osInfoContributor.contribute(builder);
		Info info = builder.build();
		assertThat(info.getDetails().get("os")).isInstanceOf(OsInfo.class);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new OsInfoContributorRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(OsInfo.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

}
