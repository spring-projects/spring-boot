/*
 * Copyright 2012-present the original author or authors.
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

	private DefaultSslBundleRegistry sslBundles;

	private PropertiesAmqpRabbitConnectionDetails details;

	@BeforeEach
	void setUp() {
		this.properties = new AmqpRabbitProperties();
		this.sslBundles = new DefaultSslBundleRegistry();
		this.details = new PropertiesAmqpRabbitConnectionDetails(this.properties, this.sslBundles);
	}

	@Test
	void getAddressUsesDefaultHostAndPort() {
		Address address = this.details.getAddress();
		assertThat(address.host()).isEqualTo("localhost");
		assertThat(address.port()).isEqualTo(5672);
	}

	@Test
	void getAddressUsesHostAndPortProperties() {
		this.properties.setHost("rabbit.example.com");
		this.properties.setPort(1234);
		Address address = this.details.getAddress();
		assertThat(address.host()).isEqualTo("rabbit.example.com");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getAddressUsesAddressProperty() {
		this.properties.setAddress("rabbit.example.com:1234");
		Address address = this.details.getAddress();
		assertThat(address.host()).isEqualTo("rabbit.example.com");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getAddressFromAmqpUrl() {
		this.properties.setAddress("amqp://user:secret@rabbit.example.com:1234/alpha");
		Address address = this.details.getAddress();
		assertThat(address.host()).isEqualTo("rabbit.example.com");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getAddressWithIpv6() {
		this.properties.setAddress("amqp://foo:bar@[aaaa:bbbb:cccc::d]:1234");
		Address address = this.details.getAddress();
		assertThat(address.host()).isEqualTo("[aaaa:bbbb:cccc::d]");
		assertThat(address.port()).isEqualTo(1234);
	}

	@Test
	void getUsernameReturnsNullByDefault() {
		assertThat(this.details.getUsername()).isNull();
	}

	@Test
	void getUsernameUsesProperty() {
		this.properties.setUsername("alice");
		assertThat(this.details.getUsername()).isEqualTo("alice");
	}

	@Test
	void getUsernameFromAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.details.getUsername()).isEqualTo("user");
	}

	@Test
	void getPasswordReturnsNullByDefault() {
		assertThat(this.details.getPassword()).isNull();
	}

	@Test
	void getPasswordUsesProperty() {
		this.properties.setPassword("secret");
		assertThat(this.details.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordFromAddress() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.details.getPassword()).isEqualTo("secret");
	}

	@Test
	void getVirtualHostDefaultsToNull() {
		assertThat(this.details.getVirtualHost()).isNull();
	}

	@Test
	void getVirtualHostUsesProperty() {
		this.properties.setVirtualHost("alpha");
		assertThat(this.details.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void getVirtualHostFromAddress() {
		this.properties.setAddress("rabbit.example.com:1234/alpha");
		assertThat(this.details.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void getSslBundleDefaultsToNull() {
		assertThat(this.details.getSslBundle()).isNull();
	}

	@Test
	void getSslBundleUsesBundle() {
		SslBundle bundle = mock(SslBundle.class);
		this.sslBundles.registerBundle("test-bundle", bundle);
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.details.getSslBundle()).isSameAs(bundle);
	}

	@Test
	void getSslBundleWithNoSslBundlesThrowsException() {
		AmqpRabbitConnectionDetails details = new PropertiesAmqpRabbitConnectionDetails(this.properties, null);
		this.properties.getSsl().setBundle("test-bundle");
		assertThatIllegalArgumentException().isThrownBy(details::getSslBundle)
			.withMessageContaining("SSL bundle name has been set but no SSL bundles found in context");
	}

	@Test
	void getAddressWhenNoNothingConfiguredUsesLocalhostAndDefaultPort() {
		assertThat(this.details.getAddress()).isEqualTo(new Address("localhost", Address.DEFAULT_PORT));
	}

	@Test
	void getAddressWhenHasSslBundleUsesLocalhostAndSecurePort() {
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.details.getAddress()).isEqualTo(new Address("localhost", Address.DEFAULT_SECURE_PORT));
	}

	@Test
	void getAddressWhenHasSslBundleAndInsecureAddressUsesInsecurePort() {
		this.properties.getSsl().setBundle("test-bundle");
		this.properties.setAddress("amqp://localhost");
		assertThat(this.details.getAddress().port()).isEqualTo(Address.DEFAULT_PORT);
	}

	@Test
	void getAddressWhenHasNoSslBundleAndSecureAddressUsesSecurePort() {
		this.properties.setAddress("amqps://localhost");
		assertThat(this.details.getAddress().port()).isEqualTo(Address.DEFAULT_SECURE_PORT);
	}

	@Test
	void getAddressWhenHasSslBundleAndPortUsesPort() {
		this.properties.getSsl().setBundle("test-bundle");
		this.properties.setPort(1234);
		assertThat(this.details.getAddress().port()).isEqualTo(1234);
	}

	@Test
	void getAddressWhenHasSslBundleAndAddressWithPortUsesPort() {
		this.properties.getSsl().setBundle("test-bundle");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.details.getAddress().port()).isEqualTo(1234);
	}

	@Test
	void getAddressWhenHasNoSslBundleAndAddressWithPortUsesAddress() {
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
	}

	@Test
	void getAddressWhenHasNoSslBundleAndAddressWithoutPortUsesDefaultPort() {
		this.properties.setAddress("rabbit.example.com");
		assertThat(this.details.getAddress().port()).isEqualTo(Address.DEFAULT_PORT);
	}

	@Test
	void getAddressWhenHasHostAndPortUsesHostAndPort() {
		this.properties.setHost("rabbit.example.com");
		this.properties.setPort(1234);
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
	}

	@Test
	void whenHasAddressWithUsernamePasswordHostPortAndVirtualhost() {
		this.properties.setAddress("amqp://user:secret@rabbit.example.com:1234/alpha");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
		assertThat(this.details.getUsername()).isEqualTo("user");
		assertThat(this.details.getPassword()).isEqualTo("secret");
		assertThat(this.details.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void whenHasAddressWithUsernamePasswordHostAndPort() {
		this.properties.setAddress("amqps://user:secret@rabbit.example.com:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
		assertThat(this.details.getUsername()).isEqualTo("user");
		assertThat(this.details.getPassword()).isEqualTo("secret");
		assertThat(this.details.getVirtualHost()).isNull();
	}

	@Test
	void whenHasAddressWithNoScheme() {
		this.properties.setAddress("user:secret@rabbit.example.com:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
		assertThat(this.details.getUsername()).isEqualTo("user");
		assertThat(this.details.getPassword()).isEqualTo("secret");
		assertThat(this.details.getVirtualHost()).isNull();
	}

	@Test
	void whenHasAddressWithNoUsernameAndUsernameProperty() {
		this.properties.setUsername("alice");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
		assertThat(this.details.getUsername()).isEqualTo("alice");
		assertThat(this.details.getPassword()).isNull();
		assertThat(this.details.getVirtualHost()).isNull();
	}

	@Test
	void whenHasAddressWithUsernameAndNoPassword() {
		this.properties.setAddress("user@rabbit.example.com:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
		assertThat(this.details.getUsername()).isEqualTo("user");
		assertThat(this.details.getPassword()).isNull();
		assertThat(this.details.getVirtualHost()).isNull();
	}

	@Test
	void whenHasAddressWithNoPasswordAndPasswordProperty() {
		this.properties.setPassword("12345678");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("rabbit.example.com", 1234));
		assertThat(this.details.getUsername()).isNull();
		assertThat(this.details.getPassword()).isEqualTo("12345678");
		assertThat(this.details.getVirtualHost()).isNull();
	}

	@Test
	void getVirtualHostWhenAddressPropertyIncludesVirtualHost() {
		this.properties.setAddress("rabbit.example.com:1234/alpha");
		assertThat(this.details.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void getVirtualHostWhenHasVirtualHostPropertyAndAddressHasNoVirtualHost() {
		this.properties.setVirtualHost("alpha");
		this.properties.setAddress("rabbit.example.com:1234");
		assertThat(this.details.getVirtualHost()).isEqualTo("alpha");
	}

	@Test
	void getVirtualHostWhenHasVirtualHostPropertyAndAddressWithVirtualHost() {
		this.properties.setVirtualHost("alpha");
		this.properties.setAddress("rabbit.example.com:1234/beta");
		assertThat(this.details.getVirtualHost()).isEqualTo("beta");
	}

	@Test
	void getVirtualHostWhenHasAddressWithTrailingSlash() {
		this.properties.setAddress("amqp://root:password@otherhost:1111/");
		assertThat(this.details.getVirtualHost()).isEqualTo("/");
	}

	@Test
	void getAddressWhenAddressIsIpv6Address() {
		this.properties.setAddress("amqp://foo:bar@[aaaa:bbbb:cccc::d]:1234");
		assertThat(this.details.getAddress()).isEqualTo(new Address("[aaaa:bbbb:cccc::d]", 1234));
	}

	@Test
	void getAddressWhenAddressIsIpv6AddressWithoutPort() {
		this.properties.setAddress("amqp://foo:bar@[aaaa:bbbb:cccc::d]");
		assertThat(this.details.getAddress()).isEqualTo(new Address("[aaaa:bbbb:cccc::d]", Address.DEFAULT_PORT));
	}

}
