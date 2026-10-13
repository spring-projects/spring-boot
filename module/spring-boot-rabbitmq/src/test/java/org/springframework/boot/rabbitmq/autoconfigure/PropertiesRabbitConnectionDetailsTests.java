/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.rabbitmq.autoconfigure.RabbitConnectionDetails.Address;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PropertiesRabbitConnectionDetails}.
 *
 * @author Jonas Fügedi
 */
class PropertiesRabbitConnectionDetailsTests {

	private static final int DEFAULT_PORT = 5672;

	private DefaultSslBundleRegistry sslBundleRegistry;

	private RabbitProperties properties;

	private PropertiesRabbitConnectionDetails propertiesRabbitConnectionDetails;

	@BeforeEach
	void setUp() {
		this.properties = new RabbitProperties();
		this.sslBundleRegistry = new DefaultSslBundleRegistry();
		this.propertiesRabbitConnectionDetails = new PropertiesRabbitConnectionDetails(this.properties,
				this.sslBundleRegistry);
	}

	@Test
	void getAddresses() {
		this.properties.setAddresses(List.of("localhost", "localhost:1234", "[::1]", "[::1]:32863"));
		List<Address> addresses = this.propertiesRabbitConnectionDetails.getAddresses();
		assertThat(addresses.size()).isEqualTo(4);
		assertThat(addresses.get(0).host()).isEqualTo("localhost");
		assertThat(addresses.get(0).port()).isEqualTo(DEFAULT_PORT);
		assertThat(addresses.get(1).host()).isEqualTo("localhost");
		assertThat(addresses.get(1).port()).isEqualTo(1234);
		assertThat(addresses.get(2).host()).isEqualTo("[::1]");
		assertThat(addresses.get(2).port()).isEqualTo(DEFAULT_PORT);
		assertThat(addresses.get(3).host()).isEqualTo("[::1]");
		assertThat(addresses.get(3).port()).isEqualTo(32863);
	}

	@Test
	void shouldReturnSslBundle() {
		SslBundle bundle1 = mock(SslBundle.class);
		this.sslBundleRegistry.registerBundle("bundle-1", bundle1);
		this.properties.getSsl().setBundle("bundle-1");
		SslBundle sslBundle = this.propertiesRabbitConnectionDetails.getSslBundle();
		assertThat(sslBundle).isSameAs(bundle1);
	}

	@Test
	void shouldReturnNullIfSslIsEnabledButBundleNotSet() {
		this.properties.getSsl().setEnabled(true);
		SslBundle sslBundle = this.propertiesRabbitConnectionDetails.getSslBundle();
		assertThat(sslBundle).isNull();
	}

	@Test
	void shouldReturnNullIfSslIsNotEnabled() {
		this.properties.getSsl().setEnabled(false);
		SslBundle sslBundle = this.propertiesRabbitConnectionDetails.getSslBundle();
		assertThat(sslBundle).isNull();
	}

}
