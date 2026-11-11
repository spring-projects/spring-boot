/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OAuth2ClientProperties}.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 */
class OAuth2ClientPropertiesTests {

	private final OAuth2ClientProperties properties = new OAuth2ClientProperties();

	@Test
	void clientIdAbsentThrowsException() {
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setClientSecret("secret");
		registration.setProvider("google");
		this.properties.getRegistration().put("foo", registration);
		assertThatIllegalStateException().isThrownBy(this.properties::validate)
			.withMessageContaining("Client id of registration 'foo' must not be empty.");
	}

	@Test
	void clientSecretAbsentShouldNotThrowException() {
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setClientId("foo");
		registration.setProvider("google");
		this.properties.getRegistration().put("foo", registration);
		this.properties.validate();
	}

}
