/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.info.ProcessInfoContributor.ProcessInfoContributorRuntimeHints;
import org.springframework.boot.info.ProcessInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProcessInfoContributor}.
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class ProcessInfoContributorTests {

	@Test
	void processInfoShouldBeAdded() {
		ProcessInfoContributor processInfoContributor = new ProcessInfoContributor();
		Info.Builder builder = new Info.Builder();
		processInfoContributor.contribute(builder);
		Info info = builder.build();
		assertThat(info.get("process")).isInstanceOf(ProcessInfo.class);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new ProcessInfoContributorRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(ProcessInfo.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_24)
	void shouldRegisterRuntimeHintsForVirtualThreadSchedulerMXBean() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new ProcessInfoContributorRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference.of("jdk.management.VirtualThreadSchedulerMXBean"))
			.withMemberCategories(MemberCategory.INVOKE_PUBLIC_METHODS)).accepts(runtimeHints);
	}

}
