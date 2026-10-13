/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.actuate.endpoint.web;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.webflux.actuate.endpoint.web.AbstractWebFluxEndpointHandlerMapping.AbstractWebFluxEndpointHandlerMappingRuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractWebFluxEndpointHandlerMapping}.
 *
 * @author Moritz Halbritter
 */
class AbstractWebFluxEndpointHandlerMappingTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new AbstractWebFluxEndpointHandlerMappingRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference
				.of("org.springframework.boot.webflux.actuate.endpoint.web.AbstractWebFluxEndpointHandlerMapping.WriteOperationHandler")))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference
				.of("org.springframework.boot.webflux.actuate.endpoint.web.AbstractWebFluxEndpointHandlerMapping.ReadOperationHandler")))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection()
			.onType(TypeReference
				.of("org.springframework.boot.webflux.actuate.endpoint.web.AbstractWebFluxEndpointHandlerMapping.CatchAllHandler")))
			.accepts(runtimeHints);
	}

}
