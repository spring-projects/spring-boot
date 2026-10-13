/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.actuate.endpoint.web.Link;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.reactive.CloudFoundryWebFluxEndpointHandlerMapping.CloudFoundryLinksHandler;
import org.springframework.boot.cloudfoundry.autoconfigure.actuate.endpoint.reactive.CloudFoundryWebFluxEndpointHandlerMapping.CloudFoundryWebFluxEndpointHandlerMappingRuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryWebFluxEndpointHandlerMapping}.
 *
 * @author Moritz Halbritter
 */
class CloudFoundryWebFluxEndpointHandlerMappingTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new CloudFoundryWebFluxEndpointHandlerMappingRuntimeHints().registerHints(runtimeHints,
				getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(CloudFoundryLinksHandler.class, "links"))
			.accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.reflection().onType(Link.class)).accepts(runtimeHints);
	}

}
