/*
 * Copyright 2012-2024 the original author or authors.
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

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.info.SslInfoContributor.SslInfoContributorRuntimeHints;
import org.springframework.boot.info.SslInfo;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SslInfoContributor}.
 *
 * @author Jonatan Ivanov
 */
class SslInfoContributorTests {

	@Test
	void sslInfoShouldBeAdded() {
		SslBundles sslBundles = new DefaultSslBundleRegistry("test", mock(SslBundle.class));
		SslInfo sslInfo = new SslInfo(sslBundles, Duration.ofDays(14));
		SslInfoContributor sslInfoContributor = new SslInfoContributor(sslInfo);
		Info.Builder builder = new Info.Builder();
		sslInfoContributor.contribute(builder);
		Info info = builder.build();
		assertThat(info.getDetails().get("ssl")).isInstanceOf(SslInfo.class);
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new SslInfoContributorRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(SslInfo.class)
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS))
			.accepts(runtimeHints);
	}

}
