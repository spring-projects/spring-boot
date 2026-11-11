/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SimpleInfoContributor}.
 *
 * @author Stephane Nicoll
 */
class SimpleInfoContributorTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void prefixIsMandatory() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SimpleInfoContributor(null, new Object()));
	}

	@Test
	void mapSimpleObject() {
		Object o = new Object();
		Info info = contributeFrom("test", o);
		assertThat(info.get("test")).isSameAs(o);
	}

	private static Info contributeFrom(String prefix, Object detail) {
		SimpleInfoContributor contributor = new SimpleInfoContributor(prefix, detail);
		Info.Builder builder = new Info.Builder();
		contributor.contribute(builder);
		return builder.build();
	}

}
