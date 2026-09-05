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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.amqp.rabbitmq.autoconfigure.AmqpRabbitConnectionDetails.Address;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertiesAmqpRabbitConnectionDetails}.
 *
 * @author Stephane Nicoll
 */
class PropertiesAmqpRabbitConnectionDetailsTests {

	private AmqpRabbitProperties properties;

	private DefaultSslBundleRegistry sslBundleRegistry;

	private PropertiesAmqpRabbitConnectionDetails connectionDetails;

	@BeforeEach
	void setUp() {
		this.properties = new AmqpRabbitProperties();
		this.sslBundleRegistry = new DefaultSslBundleRegistry();
		this.connectionDetails = new PropertiesAmqpRabbitConnectionDetails(this.properties, this.sslBundleRegistry);
	}

	@Test
	void getAddressUsesDefaultHostAndPort() {
		Address address = this.connectionDetails.getAddress();
		assertThat(address.host()).isEqualTo("localhost");
		assertThat(address.port()).isEqualTo(5672);
	}

	@Test
	void getAddressUsesHostAndPortProperties() {
		this.properties.setHost("rabbit.example.com");
		this.properties.setPort(1234);
		Address address = this.connectionDetails.getAddress();
		assertThat(address.host()).isEqualTo("rabbit.example.com");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getAddressUsesAddressProperty() {
		this.properties.setAddress("rabbit.example.com:1234");
		Address address = this.connectionDetails.getAddress();
		assertThat(address.host()).isEqualTo("rabbit.example.com");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getAddressFromAmqpUrl() {
		this.properties.setAddress("amqp://user:secret@rabbit.example.com:1234/alpha");
		Address address = this.connectionDetails.getAddress();
		assertThat(address.host()).isEqualTo("rabbit.example.com");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getAddressWithIpv6() {
		this.properties.setAddress("amqp://foo:bar@[aaaa:bbbb:cccc::d]:1234");
		Address address = this.connectionDetails.getAddress();
		assertThat(address.host()).isEqualTo("[aaaa:bbbb:cccc::d]");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getUsernameReturnsDefaultGuest() {
		assertThat(this.connectionDetails.getUsername()).isEqualTo("guest");
	}

	@Test
	void getUsernameUsesProperty() {
		this.properties.setUsername("alice");
		assertThat(this.connectionDetails.getUsername()).isEqualTo("alice");
	}

	@Test
	void getUsernameFromAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.connectionDetails.getUsername()).isEqualTo("user");
	}

	@Test
	void getPasswordReturnsDefaultGuest() {
		assertThat(this.connectionDetails.getPassword()).isEqualTo("guest");
	}

	@Test
	void getPasswordUsesProperty() {
		this.properties.setPassword("secret");
		assertThat(this.connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordFromAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.connectionDetails.getPassword()).isEqualTo("secret");
	}

	@Test
	void getVirtualHostDefaultsToNull() {
		assertThat(this.connectionDetails.getVirtualHost()).isNull();
	}

	@Test
	void getVirtualHostUsesProperty() {
		this.properties.setVirtualHost("alpha");
		assertThat(this.connectionDetails.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void getVirtualHostFromAddress() {
		this.properties.setAddress("rabbit.example.com:1234/alpha");
		assertThat(this.connectionDetails.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void getSslBundleDefaultsToNull() {
		assertThat(this.connectionDetails.getSslBundle()).isNull();
	}

	@Test
	void getSslBundleUsesBundle() {
		SslBundle bundle = mock(SslBundle.class);
		this.sslBundleRegistry.registerBundle("test-bundle", bundle);
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.connectionDetails.getSslBundle()).isSameAs(bundle);
	}

	@Test
	void getSslBundleWithNoSslBundlesThrowsException() {
		PropertiesAmqpRabbitConnectionDetails details = new PropertiesAmqpRabbitConnectionDetails(this.properties,
				null);
		this.properties.getSsl().setBundle("test-bundle");
		assertThatIllegalArgumentException().isThrownBy(details::getSslBundle)
			.withMessageContaining("SSL bundle name has been set but no SSL bundles found in context");
	}

}
