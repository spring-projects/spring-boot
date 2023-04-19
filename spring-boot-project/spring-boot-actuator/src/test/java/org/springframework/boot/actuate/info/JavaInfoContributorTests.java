/*
 * Copyright 2012-2023 the original author or authors.
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
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
			.accepts(runtimeHints);
	}

}
