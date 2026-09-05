/*
 * Copyright 2012-present the original author or authors.
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
	void determinePortDefaultsTo5672() {
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void determinePortReturnsPortFromAddress() {
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	void determinePortDefaultsWhenAddressHasNoPort() {
		this.properties.setAddress("rabbit.example.com");
		assertThat(this.properties.determinePort()).isEqualTo(5672);
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
	void determineAddressUsesHostAndPortDefaults() {
		assertThat(this.properties.determineAddress()).isEqualTo("localhost:5672");
	}

	@Test
	void determineAddressUsesHostAndPortProperties() {
		this.properties.setHost("rabbit.example.com");
		this.properties.setPort(1234);
		assertThat(this.properties.determineAddress()).isEqualTo("rabbit.example.com:1234");
	}

	@Test
	void determineAddressUsesAddressProperty() {
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.properties.determineAddress()).isEqualTo("rabbit.example.com:1234");
	}

	@Test
	void determineAddressFromAmqpUrl() {
		this.properties.setAddress("amqp://user:secret@rabbit.example.com:1234/alpha");
		assertThat(this.properties.determineAddress()).isEqualTo("rabbit.example.com:1234");
		assertThat(this.properties.determineUsername()).isEqualTo("user");
		assertThat(this.properties.determinePassword()).isEqualTo("secret");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void determineAddressFromAmqpsUrl() {
		this.properties.setAddress("amqps://user:secret@rabbit.example.com:1234");
		assertThat(this.properties.determineAddress()).isEqualTo("rabbit.example.com:1234");
	}

	@Test
	void determineUsernameFromAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.properties.determineUsername()).isEqualTo("user");
	}

	@Test
	void determineUsernameReturnsPropertyWhenAddressHasNoUsername() {
		this.properties.setUsername("alice");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.properties.determineUsername()).isEqualTo("alice");
	}

	@Test
	void determineUsernameWithoutPassword() {
		this.properties.setAddress("user@rabbit.example.com:1234");
		assertThat(this.properties.determineUsername()).isEqualTo("user");
		assertThat(this.properties.determinePassword()).isEqualTo("guest");
	}

	@Test
	void determinePasswordFromAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.properties.determinePassword()).isEqualTo("secret");
	}

	@Test
	void determinePasswordReturnsPropertyWhenAddressHasNoPassword() {
		this.properties.setPassword("12345678");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.properties.determinePassword()).isEqualTo("12345678");
	}

	@Test
	void determineVirtualHostFromAddress() {
		this.properties.setAddress("rabbit.example.com:1234/alpha");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void determineVirtualHostReturnsPropertyWhenAddressHasNoVirtualHost() {
		this.properties.setVirtualHost("alpha");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void determineVirtualHostIsSlashWhenAddressHasTrailingSlash() {
		this.properties.setAddress("amqp://root:password@otherhost:1111/");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("/");
	}

	@Test
	void ipv6Address() {
		this.properties.setAddress("amqp://foo:bar@[aaaa:bbbb:cccc::d]:1234");
		assertThat(this.properties.determineAddress()).isEqualTo("[aaaa:bbbb:cccc::d]:1234");
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	void ipv6AddressDefaultPort() {
		this.properties.setAddress("amqp://foo:bar@[aaaa:bbbb:cccc::d]");
		assertThat(this.properties.determineAddress()).isEqualTo("[aaaa:bbbb:cccc::d]:5672");
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void hostPropertyMustBeSingleHost() {
		this.properties.setHost("my-rmq-host.net,my-rmq-host-2.net");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(this.properties::determineAddress)
			.withMessageContaining("spring.amqp.rabbitmq.host");
	}

	@Test
	void customSslBundle() {
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.properties.getSsl().getBundle()).isEqualTo("test-bundle");
	}

}
