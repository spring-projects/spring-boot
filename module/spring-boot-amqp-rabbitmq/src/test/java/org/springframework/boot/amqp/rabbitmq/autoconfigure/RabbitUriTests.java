/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RabbitUri}.
 *
 * @author Phillip Webb
 */
class RabbitUriTests {

	@Test
	void ofWhenNullReturnsNone() {
		assertThat(RabbitUri.of(null)).isEqualTo(RabbitUri.NONE);
	}

	@Test
	void ofWhenStringWithoutTextReturnsNone() {
		assertThat(RabbitUri.of("")).isEqualTo(RabbitUri.NONE);
		assertThat(RabbitUri.of(" ")).isEqualTo(RabbitUri.NONE);
	}

	@Test
	void ofWhenSimpleHost() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", null, null, null, null));
	}

	@Test
	void ofWhenPadded() {
		RabbitUri uri = RabbitUri.of("  rabbit.example.com  ");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", null, null, null, null));
	}

	@Test
	void ofWhenAmqpScheme() {
		RabbitUri uri = RabbitUri.of("amqp://rabbit.example.com");
		assertThat(uri).isEqualTo(new RabbitUri(false, "rabbit.example.com", null, null, null, null));
	}

	@Test
	void ofWhenAmqpsScheme() {
		RabbitUri uri = RabbitUri.of("amqps://rabbit.example.com");
		assertThat(uri).isEqualTo(new RabbitUri(true, "rabbit.example.com", null, null, null, null));
	}

	@Test
	void ofWhenHasUsename() {
		RabbitUri uri = RabbitUri.of("user@rabbit.example.com");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", null, "user", null, null));
	}

	@Test
	void ofWhenHasUsenameAndPassword() {
		RabbitUri uri = RabbitUri.of("user:password@rabbit.example.com");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", null, "user", "password", null));
	}

	@Test
	void ofWhenHasDefaultVirtualHost() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com/");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", null, null, null, "/"));
	}

	@Test
	void ofWhenHasSpecifiedVirtualHost() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com/vhost");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", null, null, null, "vhost"));
	}

	@Test
	void ofWhenHasPort() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com:1234");
		assertThat(uri).isEqualTo(new RabbitUri(null, "rabbit.example.com", 1234, null, null, null));
	}

	@Test
	void ofWhenIpv6WithPort() {
		RabbitUri uri = RabbitUri.of("[aaaa:bbbb:cccc::d]:1234");
		assertThat(uri).isEqualTo(new RabbitUri(null, "[aaaa:bbbb:cccc::d]", 1234, null, null, null));
	}

	@Test
	void ofWhenIpv6WithoutPort() {
		RabbitUri uri = RabbitUri.of("[aaaa:bbbb:cccc::d]");
		assertThat(uri).isEqualTo(new RabbitUri(null, "[aaaa:bbbb:cccc::d]", null, null, null, null));
	}

	@Test
	void ofWhenPortIsNotNumberThrowsException() {
		assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> RabbitUri.of("example.com:0x00"));
	}

	@Test
	void ofWhenHostIsMissingThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> RabbitUri.of(":1234")).withMessage("Missing RabbitMQ host");
	}

	@Test
	void determineSecureWhenHasUserDefinedSecure() {
		RabbitUri uri = RabbitUri.of("amqps://rabbit.example.com");
		assertThat(uri.determineSecure(false)).isTrue();
		assertThat(uri.determineSecure(true)).isTrue();
		assertThat(uri.determineSecure(null)).isTrue();
	}

	@Test
	void determineSecureWhenHasUserDefinedInsecure() {
		RabbitUri uri = RabbitUri.of("amqp://rabbit.example.com");
		assertThat(uri.determineSecure(false)).isFalse();
		assertThat(uri.determineSecure(true)).isFalse();
		assertThat(uri.determineSecure(null)).isFalse();
	}

	@Test
	void determineSecureWhenHasNoUserDefinedSecure() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com");
		assertThat(uri.determineSecure(false)).isFalse();
		assertThat(uri.determineSecure(true)).isTrue();
		assertThat(uri.determineSecure(null)).isFalse();
	}

	@Test
	void determineUsernameWhenHasUserDefinedUsername() {
		RabbitUri uri = RabbitUri.of("user@rabbit.example.com");
		assertThat(uri.determineUsername("fallback")).isEqualTo("user");
		assertThat(uri.determineUsername(null)).isEqualTo("user");
	}

	@Test
	void determineUsernameWhenHasNoUserDefinedUsername() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com");
		assertThat(uri.determineUsername("fallback")).isEqualTo("fallback");
		assertThat(uri.determineUsername(null)).isNull();
	}

	@Test
	void determinePasswordWhenHasUserDefinedPassword() {
		RabbitUri uri = RabbitUri.of("user:password@rabbit.example.com");
		assertThat(uri.determinePassword("fallback")).isEqualTo("password");
		assertThat(uri.determinePassword(null)).isEqualTo("password");
	}

	@Test
	void determinePasswordWhenHasNoUserDefinedPassword() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com");
		assertThat(uri.determinePassword("fallback")).isEqualTo("fallback");
		assertThat(uri.determinePassword(null)).isNull();
	}

	@Test
	void determineOrDefaultPortWhenHasUserDefinedPortReturnsPort() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com:1234");
		assertThat(uri.determineOrDefaultPort(false)).isEqualTo(1234);
		assertThat(uri.determineOrDefaultPort(true)).isEqualTo(1234);
	}

	@Test
	void determineOrDefaultPortWhenHasNoUserDefinedPortAndUserDefinedSecureReturnsDefaultSecurePort() {
		RabbitUri uri = RabbitUri.of("amqps://rabbit.example.com");
		assertThat(uri.determineOrDefaultPort(false)).isEqualTo(5671);
		assertThat(uri.determineOrDefaultPort(true)).isEqualTo(5671);
	}

	@Test
	void determineOrDefaultPortWhenHasNoUserDefinedPortAndUserDefinedInsecureReturnsDefaultPort() {
		RabbitUri uri = RabbitUri.of("amqp://rabbit.example.com");
		assertThat(uri.determineOrDefaultPort(false)).isEqualTo(5672);
		assertThat(uri.determineOrDefaultPort(true)).isEqualTo(5672);
	}

	@Test
	void determineOrDefaultPortWhenHasNoUserDefinedPortAndNoUserDefinedSecureReturnsPortBasedOnParameter() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com");
		assertThat(uri.determineOrDefaultPort(false)).isEqualTo(5672);
		assertThat(uri.determineOrDefaultPort(true)).isEqualTo(5671);
	}

	@Test
	void determineVirtualHostWhenHasUserDefinedVirtualHost() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com/virtual");
		assertThat(uri.determineVirtualHost("fallback")).isEqualTo("virtual");
		assertThat(uri.determineVirtualHost(null)).isEqualTo("virtual");
	}

	@Test
	void determineVirtualHostWhenHasNoUserDefinedVirtualHost() {
		RabbitUri uri = RabbitUri.of("rabbit.example.com");
		assertThat(uri.determinePassword("fallback")).isEqualTo("fallback");
		assertThat(uri.determinePassword(null)).isNull();
	}

	@Test
	void ofDecodesEncodedUris() {
		RabbitUri uri = RabbitUri.of("amqp://us%3Aer:p%40ss@localhost:5672/my%2Fvhost");
		assertThat(uri).isEqualTo(new RabbitUri(false, "localhost", 5672, "us:er", "p@ss", "my/vhost"));
	}

	@Test
	void ofDecodesWithoutChangingPlus() {
		RabbitUri uri = RabbitUri.of("amqp://user:pass+word@localhost:5672");
		assertThat(uri).isEqualTo(new RabbitUri(false, "localhost", 5672, "user", "pass+word", null));
	}

}
