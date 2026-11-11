/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GenericReactiveWebApplicationContext}
 *
 * @author Brian Clozel
 */
class GenericReactiveWebApplicationContextTests {

	@Test
	void getResourceByPath() throws Exception {
		GenericReactiveWebApplicationContext context = new GenericReactiveWebApplicationContext();
		Resource rootResource = context.getResourceByPath("/");
		assertThat(rootResource.exists()).isFalse();
		assertThat(rootResource.createRelative("application.properties").exists()).isFalse();
		context.close();
	}

}
