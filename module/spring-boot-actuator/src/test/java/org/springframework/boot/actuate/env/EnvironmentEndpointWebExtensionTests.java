/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.env;

import java.security.Principal;
import java.util.Collections;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.EnvironmentEntryDescriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EnvironmentEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
class EnvironmentEndpointWebExtensionTests {

	private @Nullable EnvironmentEndpointWebExtension webExtension;

	private EnvironmentEndpoint delegate;

	@BeforeEach
	void setup() {
		this.delegate = mock(EnvironmentEndpoint.class);
	}

	@Test
	void whenShowValuesIsNever() {
		this.webExtension = new EnvironmentEndpointWebExtension(this.delegate, Show.NEVER, Collections.emptySet());
		this.webExtension.environment(SecurityContext.NONE, null);
		then(this.delegate).should().getEnvironmentDescriptor(null, false);
		verifyPrefixed(SecurityContext.NONE, false);
	}

	@Test
	void whenShowValuesIsAlways() {
		this.webExtension = new EnvironmentEndpointWebExtension(this.delegate, Show.ALWAYS, Collections.emptySet());
		this.webExtension.environment(SecurityContext.NONE, null);
		then(this.delegate).should().getEnvironmentDescriptor(null, true);
		verifyPrefixed(SecurityContext.NONE, true);
	}

	@Test
	void whenShowValuesIsWhenAuthorizedAndSecurityContextIsAuthorized() {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		this.webExtension = new EnvironmentEndpointWebExtension(this.delegate, Show.WHEN_AUTHORIZED,
				Collections.emptySet());
		this.webExtension.environment(securityContext, null);
		then(this.delegate).should().getEnvironmentDescriptor(null, true);
		verifyPrefixed(securityContext, true);

	}

	@Test
	void whenShowValuesIsWhenAuthorizedAndSecurityContextIsNotAuthorized() {
		SecurityContext securityContext = mock(SecurityContext.class);
		this.webExtension = new EnvironmentEndpointWebExtension(this.delegate, Show.WHEN_AUTHORIZED,
				Collections.emptySet());
		this.webExtension.environment(securityContext, null);
		then(this.delegate).should().getEnvironmentDescriptor(null, false);
		verifyPrefixed(securityContext, false);
	}

	private void verifyPrefixed(SecurityContext securityContext, boolean showUnsanitized) {
		given(this.delegate.getEnvironmentEntryDescriptor("test", showUnsanitized))
			.willReturn(new EnvironmentEntryDescriptor(null, Collections.emptyList(), Collections.emptyList(),
					Collections.emptyList()));
		assertThat(this.webExtension).isNotNull();
		this.webExtension.environmentEntry(securityContext, "test");
		then(this.delegate).should().getEnvironmentEntryDescriptor("test", showUnsanitized);
	}

}
