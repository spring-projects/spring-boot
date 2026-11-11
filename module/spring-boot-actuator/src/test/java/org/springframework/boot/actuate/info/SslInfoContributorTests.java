/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

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
		SslInfo sslInfo = new SslInfo(sslBundles);
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
			.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(runtimeHints);
	}

}
