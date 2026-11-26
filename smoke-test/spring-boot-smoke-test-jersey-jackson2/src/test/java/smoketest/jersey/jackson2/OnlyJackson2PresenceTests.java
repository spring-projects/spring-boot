/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the presence of only Jackson 2 and Jersey's Jackson 2 provider.
 *
 * @author Kristoffer Larsen Hopland
 */
class OnlyJackson2PresenceTests {

	@Test
	void jackson2IsPresent() {
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null)).isTrue();
		assertThat(ClassUtils.isPresent("org.glassfish.jersey.jackson.JacksonFeature", null)).isTrue();
	}

	@Test
	void jackson3IsNotPresent() {
		assertThat(ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", null)).isFalse();
		assertThat(ClassUtils.isPresent("org.glassfish.jersey.jackson3.JacksonFeature", null)).isFalse();
	}

}
