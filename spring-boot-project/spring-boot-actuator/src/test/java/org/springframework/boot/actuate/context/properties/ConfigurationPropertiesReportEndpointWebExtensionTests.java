/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.context.properties;

import java.security.Principal;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesDescriptor;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.Show;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigurationPropertiesReportEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
class ConfigurationPropertiesReportEndpointWebExtensionTests {

	private ConfigurationPropertiesReportEndpointWebExtension webExtension;

	private ConfigurationPropertiesReportEndpoint delegate;

	@BeforeEach
	void setup() {
		this.delegate = mock(ConfigurationPropertiesReportEndpoint.class);
	}

	@Test
	void whenShowValuesIsNever() {
		this.webExtension = new ConfigurationPropertiesReportEndpointWebExtension(this.delegate, Show.NEVER,
				Collections.emptySet());
		this.webExtension.configurationProperties(null);
		then(this.delegate).should().getConfigurationProperties(false);
		verifyPrefixed(null, false);
	}

	@Test
	void whenShowValuesIsAlways() {
		this.webExtension = new ConfigurationPropertiesReportEndpointWebExtension(this.delegate, Show.ALWAYS,
				Collections.emptySet());
		this.webExtension.configurationProperties(null);
		then(this.delegate).should().getConfigurationProperties(true);
		verifyPrefixed(null, true);
	}

	@Test
	void whenShowValuesIsWhenAuthorizedAndSecurityContextIsAuthorized() {
		SecurityContext securityContext = mock(SecurityContext.class);
		given(securityContext.getPrincipal()).willReturn(mock(Principal.class));
		this.webExtension = new ConfigurationPropertiesReportEndpointWebExtension(this.delegate, Show.WHEN_AUTHORIZED,
				Collections.emptySet());
		this.webExtension.configurationProperties(securityContext);
		then(this.delegate).should().getConfigurationProperties(true);
		verifyPrefixed(securityContext, true);

	}

	@Test
	void whenShowValuesIsWhenAuthorizedAndSecurityContextIsNotAuthorized() {
		SecurityContext securityContext = mock(SecurityContext.class);
		this.webExtension = new ConfigurationPropertiesReportEndpointWebExtension(this.delegate, Show.WHEN_AUTHORIZED,
				Collections.emptySet());
		this.webExtension.configurationProperties(securityContext);
		then(this.delegate).should().getConfigurationProperties(false);
		verifyPrefixed(securityContext, false);
	}

	private void verifyPrefixed(SecurityContext securityContext, boolean showUnsanitized) {
		given(this.delegate.getConfigurationProperties("test", showUnsanitized))
			.willReturn(new ConfigurationPropertiesDescriptor(Collections.emptyMap()));
		this.webExtension.configurationPropertiesWithPrefix(securityContext, "test");
		then(this.delegate).should().getConfigurationProperties("test", showUnsanitized);
	}

}
