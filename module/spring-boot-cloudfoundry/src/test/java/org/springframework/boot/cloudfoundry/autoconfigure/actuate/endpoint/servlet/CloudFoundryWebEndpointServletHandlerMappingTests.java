/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet.CloudFoundryWebEndpointServletHandlerMapping.CloudFoundryLinksHandler;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.servlet.CloudFoundryWebEndpointServletHandlerMapping.CloudFoundryWebEndpointServletHandlerMappingRuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryWebEndpointServletHandlerMapping}.
 *
 * @author Moritz Halbritter
 */
class CloudFoundryWebEndpointServletHandlerMappingTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new CloudFoundryWebEndpointServletHandlerMappingRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(CloudFoundryLinksHandler.class, "links"))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Link.class)).accepts(runtimeHints);
	}

}
