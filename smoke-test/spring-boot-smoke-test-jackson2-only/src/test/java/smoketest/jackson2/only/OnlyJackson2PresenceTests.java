/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.only;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the presence of mixed versions of Jackson.
 *
 * @author Andy Wilkinson
 */
class OnlyJackson2PresenceTests {

	@Test
	void jackson2IsPresent() {
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null)).isTrue();
	}

	@Test
	void jackson3IsNotPresent() {
		assertThat(ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", null)).isFalse();
	}

}
