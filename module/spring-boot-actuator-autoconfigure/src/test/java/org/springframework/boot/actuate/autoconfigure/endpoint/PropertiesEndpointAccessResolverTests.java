/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PropertiesEndpointAccessResolver}.
 *
 * @author Andy Wilkinson
 */
class PropertiesEndpointAccessResolverTests {

	private final MockEnvironment environment = new MockEnvironment();

	PropertiesEndpointAccessResolverTests() {
		ConfigurationPropertySources.attach(this.environment);
	}

	@Test
	void whenNoPropertiesAreConfiguredThenAccessForReturnsEndpointsDefaultAccess() {
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void whenDefaultAccessForAllEndpointsIsConfiguredThenAccessForReturnsDefaultForAllEndpoints() {
		this.environment.withProperty("management.endpoints.access.default", Access.UNRESTRICTED.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.UNRESTRICTED);
	}

	@Test
	void whenAccessForEndpointIsConfiguredThenAccessForReturnsIt() {
		this.environment.withProperty("management.endpoint.test.access", Access.UNRESTRICTED.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.UNRESTRICTED);
	}

	@Test
	void whenAccessForEndpointWithCamelCaseIdIsConfiguredThenAccessForReturnsIt() {
		this.environment.withProperty("management.endpoint.alpha-bravo.access", Access.UNRESTRICTED.name());
		assertThat(accessResolver().accessFor(EndpointId.of("alphaBravo"), Access.READ_ONLY))
			.isEqualTo(Access.UNRESTRICTED);
	}

	@Test
	void whenAccessForEndpointAndDefaultAccessForAllEndpointsAreConfiguredAccessForReturnsAccessForEndpoint() {
		this.environment.withProperty("management.endpoint.test.access", Access.NONE.name())
			.withProperty("management.endpoints.access.default", Access.UNRESTRICTED.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

	@Test
	void whenAllEndpointsAreDisabledByDefaultAccessForReturnsNone() {
		this.environment.withProperty("management.endpoints.enabled-by-default", "false");
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

	@Test
	void whenAllEndpointsAreEnabledByDefaultAccessForReturnsUnrestricted() {
		this.environment.withProperty("management.endpoints.enabled-by-default", "true");
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.UNRESTRICTED);
	}

	@Test
	void whenEndpointIsDisabledAccessForReturnsNone() {
		this.environment.withProperty("management.endpoint.test.enabled", "false");
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

	@Test
	void whenEndpointIsEnabledAccessForReturnsUnrestricted() {
		this.environment.withProperty("management.endpoint.test.enabled", "true");
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.UNRESTRICTED);
	}

	@Test
	void whenEndpointWithCamelCaseIdIsEnabledAccessForReturnsUnrestricted() {
		this.environment.withProperty("management.endpoint.alpha-bravo.enabled", "true");
		assertThat(accessResolver().accessFor(EndpointId.of("alphaBravo"), Access.READ_ONLY))
			.isEqualTo(Access.UNRESTRICTED);
	}

	@Test
	void whenEnabledByDefaultAndDefaultAccessAreBothConfiguredResolverCreationThrows() {
		this.environment.withProperty("management.endpoints.enabled-by-default", "true")
			.withProperty("management.endpoints.access.default", Access.READ_ONLY.name());
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
			.isThrownBy(this::accessResolver);
	}

	@Test
	void whenEndpointEnabledAndAccessAreBothConfiguredAccessForThrows() {
		this.environment.withProperty("management.endpoint.test.enabled", "true")
			.withProperty("management.endpoint.test.access", Access.READ_ONLY.name());
		assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
			.isThrownBy(() -> accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY));
	}

	@Test
	void whenAllEndpointsAreEnabledByDefaultAndAccessIsLimitedToReadOnlyAccessForReturnsReadOnly() {
		this.environment.withProperty("management.endpoints.enabled-by-default", "true")
			.withProperty("management.endpoints.access.max-permitted", Access.READ_ONLY.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void whenAllEndpointsHaveUnrestrictedDefaultAccessAndAccessIsLimitedToReadOnlyAccessForReturnsReadOnly() {
		this.environment.withProperty("management.endpoints.access.default", Access.UNRESTRICTED.name())
			.withProperty("management.endpoints.access.max-permitted", Access.READ_ONLY.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.READ_ONLY);
	}

	@Test
	void whenEndpointsIsEnabledAndAccessIsLimitedToNoneAccessForReturnsNone() {
		this.environment.withProperty("management.endpoint.test.enabled", "true")
			.withProperty("management.endpoints.access.max-permitted", Access.NONE.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

	@Test
	void whenEndpointsHasUnrestrictedAccessAndAccessIsLimitedToNoneAccessForReturnsNone() {
		this.environment.withProperty("management.endpoint.test.access", Access.UNRESTRICTED.name())
			.withProperty("management.endpoints.access.max-permitted", Access.NONE.name());
		assertThat(accessResolver().accessFor(EndpointId.of("test"), Access.READ_ONLY)).isEqualTo(Access.NONE);
	}

	private PropertiesEndpointAccessResolver accessResolver() {
		return new PropertiesEndpointAccessResolver(this.environment);
	}

}
