/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AmqpRabbitProperties}.
 *
 * @author Stephane Nicoll
 */
class AmqpRabbitPropertiesTests {

	private final AmqpRabbitProperties properties = new AmqpRabbitProperties();

	@Test
	void customHost() {
		this.properties.setHost("rabbit.example.com");
		assertThat(this.properties.getHost()).isEqualTo("rabbit.example.com");
	}

	@Test
	void customPort() {
		this.properties.setPort(1234);
		assertThat(this.properties.getPort()).isEqualTo(1234);
	}

	@Test
	void customVirtualHost() {
		this.properties.setVirtualHost("alpha");
		assertThat(this.properties.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void virtualHostRetainsALeadingSlash() {
		this.properties.setVirtualHost("/alpha");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/alpha");
	}

	@Test
	void emptyVirtualHostIsCoercedToASlash() {
		this.properties.setVirtualHost("");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/");
	}

	@Test
	void customUsername() {
		this.properties.setUsername("user");
		assertThat(this.properties.getUsername()).isEqualTo("user");
	}

	@Test
	void customPassword() {
		this.properties.setPassword("secret");
		assertThat(this.properties.getPassword()).isEqualTo("secret");
	}

	@Test
	void customAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234/alpha");
		assertThat(this.properties.getAddress()).isEqualTo("user:secret@rabbit.example.com:1234/alpha");
	}

	@Test
	void setHostWhenPropertyContainsMultipleHostsThrowsException() {
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(() -> this.properties.setHost("my-rmq-host.net,my-rmq-host-2.net"))
			.withMessageContaining("spring.amqp.rabbitmq.host");
	}

	@Test
	void customSslBundle() {
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.properties.getSsl().getBundle()).isEqualTo("test-bundle");
	}

}
