/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.endpoint.web;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.webflux.actuate.endpoint.web.WebFluxEndpointHandlerMapping.WebFluxEndpointHandlerMappingRuntimeHints;
import org.springframework.boot.webflux.actuate.endpoint.web.WebFluxEndpointHandlerMapping.WebFluxLinksHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxEndpointHandlerMapping}.
 *
 * @author Moritz Halbritter
 */
class WebFluxEndpointHandlerMappingTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new WebFluxEndpointHandlerMappingRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(WebFluxLinksHandler.class, "links"))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Link.class)).accepts(runtimeHints);
	}

}
