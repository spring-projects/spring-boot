/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.reactive;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PathRequest}.
 *
 * @author Madhura Bhave
 */
class PathRequestTests {

	@Test
	void toStaticResourcesShouldReturnStaticResourceRequest() {
		assertThat(PathRequest.toStaticResources()).isInstanceOf(StaticResourceRequest.class);
	}

}
