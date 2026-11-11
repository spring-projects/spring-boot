/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.actuate.endpoint.web;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping.WebMvcEndpointHandlerMappingRuntimeHints;
import org.springframework.boot.webmvc.actuate.endpoint.web.WebMvcEndpointHandlerMapping.WebMvcLinksHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcEndpointHandlerMapping}.
 *
 * @author Moritz Halbritter
 */
class WebMvcEndpointHandlerMappingTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new WebMvcEndpointHandlerMappingRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(WebMvcLinksHandler.class, "links"))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Link.class)).accepts(runtimeHints);
	}

}
