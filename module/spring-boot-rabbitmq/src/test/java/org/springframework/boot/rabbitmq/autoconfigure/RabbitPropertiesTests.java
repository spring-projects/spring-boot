/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.rabbitmq.autoconfigure;

import java.util.List;
import java.util.Map;

import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link RabbitProperties}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Rafael Carvalho
 * @author Scott Frederick
 * @author Jay Choi
 */
class RabbitPropertiesTests {

	private final RabbitProperties properties = new RabbitProperties();

	@Test
	void hostDefaultsToLocalhost() {
		assertThat(this.properties.getHost()).isEqualTo("localhost");
	}

	@Test
	void customHost() {
		this.properties.setHost("rabbit.example.com");
		assertThat(this.properties.getHost()).isEqualTo("rabbit.example.com");
	}

	@Test
	@SuppressWarnings("removal")
	void hostIsDeterminedFromFirstAddress() {
		this.properties.setAddresses(List.of("rabbit1.example.com:1234", "rabbit2.example.com:2345"));
		assertThat(this.properties.determineHost()).isEqualTo("rabbit1.example.com");
	}

	@Test
	@SuppressWarnings("removal")
	void determineHostReturnsHostPropertyWhenNoAddresses() {
		this.properties.setHost("rabbit.example.com");
		assertThat(this.properties.determineHost()).isEqualTo("rabbit.example.com");
	}

	@Test
	void portDefaultsToNull() {
		assertThat(this.properties.getPort()).isNull();
	}

	@Test
	void customPort() {
		this.properties.setPort(1234);
		assertThat(this.properties.getPort()).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsPortOfFirstAddress() {
		this.properties.setAddresses(List.of("rabbit1.example.com:1234", "rabbit2.example.com:2345"));
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsDefaultPortWhenNoAddresses() {
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortWithSslReturnsDefaultSslPortWhenNoAddresses() {
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortWithSslBundleReturnsDefaultSslPortWhenNoAddresses() {
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsPortPropertyWhenSslBundleIsSet() {
		this.properties.getSsl().setBundle("test-bundle");
		this.properties.setPort(1234);
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsPortPropertyWhenNoAddresses() {
		this.properties.setPort(1234);
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsDefaultAmqpPortWhenFirstAddressHasNoExplicitPort() {
		this.properties.setPort(1234);
		this.properties.setAddresses(List.of("rabbit1.example.com", "rabbit2.example.com:2345"));
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortUsingAmqpReturnsPortOfFirstAddress() {
		this.properties.setAddresses(List.of("amqp://root:password@otherhost", "amqps://root:password2@otherhost2"));
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortUsingAmqpsReturnsPortOfFirstAddress() {
		this.properties.setAddresses(List.of("amqps://root:password@otherhost", "amqp://root:password2@otherhost2"));
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsDefaultAmqpsPortWhenFirstAddressHasNoExplicitPortButSslEnabled() {
		this.properties.getSsl().setEnabled(true);
		this.properties.setPort(1234);
		this.properties.setAddresses(List.of("rabbit1.example.com", "rabbit2.example.com:2345"));
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortReturnsDefaultAmqpsPortWhenFirstAddressHasNoExplicitPortButSslBundleIsSet() {
		this.properties.getSsl().setBundle("test-bundle");
		this.properties.setAddresses(List.of("rabbit1.example.com", "rabbit2.example.com:2345"));
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortUsesAmqpAddressSchemeWhenFirstAddressHasNoExplicitPortAndSslEnabled() {
		this.properties.getSsl().setEnabled(true);
		this.properties.setAddresses(List.of("amqp://root:password@rabbit1.example.com"));
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortUsesAmqpAddressSchemeWhenFirstAddressHasNoExplicitPortAndSslBundleIsSet() {
		this.properties.getSsl().setBundle("test-bundle");
		this.properties.setAddresses(List.of("amqp://root:password@rabbit1.example.com"));
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	void virtualHostDefaultsToNull() {
		assertThat(this.properties.getVirtualHost()).isNull();
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
	@SuppressWarnings("removal")
	void determineVirtualHostReturnsVirtualHostOfFirstAddress() {
		this.properties.setAddresses(List.of("rabbit1.example.com:1234/alpha", "rabbit2.example.com:2345/bravo"));
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	@SuppressWarnings("removal")
	void determineVirtualHostReturnsPropertyWhenNoAddresses() {
		this.properties.setVirtualHost("alpha");
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	@SuppressWarnings("removal")
	void determineVirtualHostReturnsPropertyWhenFirstAddressHasNoVirtualHost() {
		this.properties.setVirtualHost("alpha");
		this.properties.setAddresses(List.of("rabbit1.example.com:1234", "rabbit2.example.com:2345/bravo"));
		assertThat(this.properties.determineVirtualHost()).isEqualTo("alpha");
	}

	@Test
	@SuppressWarnings("removal")
	void determineVirtualHostIsSlashWhenAddressHasTrailingSlash() {
		this.properties.setAddresses(List.of("amqp://root:password@otherhost:1111/"));
		assertThat(this.properties.determineVirtualHost()).isEqualTo("/");
	}

	@Test
	void emptyVirtualHostIsCoercedToASlash() {
		this.properties.setVirtualHost("");
		assertThat(this.properties.getVirtualHost()).isEqualTo("/");
	}

	@Test
	void usernameDefaultsToGuest() {
		assertThat(this.properties.getUsername()).isEqualTo("guest");
	}

	@Test
	void customUsername() {
		this.properties.setUsername("user");
		assertThat(this.properties.getUsername()).isEqualTo("user");
	}

	@Test
	@SuppressWarnings("removal")
	void determineUsernameReturnsUsernameOfFirstAddress() {
		this.properties
			.setAddresses(List.of("user:secret@rabbit1.example.com:1234/alpha", "rabbit2.example.com:2345/bravo"));
		assertThat(this.properties.determineUsername()).isEqualTo("user");
	}

	@Test
	@SuppressWarnings("removal")
	void determineUsernameReturnsPropertyWhenNoAddresses() {
		this.properties.setUsername("alice");
		assertThat(this.properties.determineUsername()).isEqualTo("alice");
	}

	@Test
	@SuppressWarnings("removal")
	void determineUsernameReturnsPropertyWhenFirstAddressHasNoUsername() {
		this.properties.setUsername("alice");
		this.properties
			.setAddresses(List.of("rabbit1.example.com:1234/alpha", "user:secret@rabbit2.example.com:2345/bravo"));
		assertThat(this.properties.determineUsername()).isEqualTo("alice");
	}

	@Test
	void passwordDefaultsToGuest() {
		assertThat(this.properties.getPassword()).isEqualTo("guest");
	}

	@Test
	void customPassword() {
		this.properties.setPassword("secret");
		assertThat(this.properties.getPassword()).isEqualTo("secret");
	}

	@Test
	@SuppressWarnings("removal")
	void determinePasswordReturnsPasswordOfFirstAddress() {
		this.properties
			.setAddresses(List.of("user:secret@rabbit1.example.com:1234/alpha", "rabbit2.example.com:2345/bravo"));
		assertThat(this.properties.determinePassword()).isEqualTo("secret");
	}

	@Test
	@SuppressWarnings("removal")
	void determinePasswordReturnsPropertyWhenNoAddresses() {
		this.properties.setPassword("secret");
		assertThat(this.properties.determinePassword()).isEqualTo("secret");
	}

	@Test
	@SuppressWarnings("removal")
	void determinePasswordReturnsPropertyWhenFirstAddressHasNoPassword() {
		this.properties.setPassword("12345678");
		this.properties
			.setAddresses(List.of("rabbit1.example.com:1234/alpha", "user:secret@rabbit2.example.com:2345/bravo"));
		assertThat(this.properties.determinePassword()).isEqualTo("12345678");
	}

	@Test
	void addressesDefaultsToNull() {
		assertThat(this.properties.getAddresses()).isNull();
	}

	@Test
	void customAddresses() {
		this.properties.setAddresses(List.of("user:secret@rabbit1.example.com:1234/alpha", "rabbit2.example.com"));
		assertThat(this.properties.getAddresses()).containsExactly("user:secret@rabbit1.example.com:1234/alpha",
				"rabbit2.example.com");
	}

	@Test
	@SuppressWarnings("removal")
	void ipv6Address() {
		this.properties.setAddresses(List.of("amqp://foo:bar@[aaaa:bbbb:cccc::d]:1234"));
		assertThat(this.properties.determineHost()).isEqualTo("[aaaa:bbbb:cccc::d]");
		assertThat(this.properties.determinePort()).isEqualTo(1234);
	}

	@Test
	@SuppressWarnings("removal")
	void ipv6AddressDefaultPort() {
		this.properties.setAddresses(List.of("amqp://foo:bar@[aaaa:bbbb:cccc::d]"));
		assertThat(this.properties.determineHost()).isEqualTo("[aaaa:bbbb:cccc::d]");
		assertThat(this.properties.determinePort()).isEqualTo(5672);
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesReturnsAddressesWithJustHostAndPort() {
		this.properties.setAddresses(List.of("user:secret@rabbit1.example.com:1234/alpha", "rabbit2.example.com"));
		assertThat(this.properties.determineAddresses()).containsExactly("rabbit1.example.com:1234",
				"rabbit2.example.com:5672");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesDefaultWhenNoAddressesSet() {
		assertThat(this.properties.determineAddresses()).containsExactly("localhost:5672");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesWithSslUsesDefaultWhenNoAddressesSet() {
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determineAddresses()).containsExactly("localhost:5671");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesWithSslBundleUsesDefaultWhenNoAddressesSet() {
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.properties.determineAddresses()).containsExactly("localhost:5671");
	}

	@Test
	@SuppressWarnings("removal")
	void determinePortUsesDefaultSslPortWhenAddressesAreSetBeforeSslBundle() {
		this.properties.setAddresses(List.of("rabbit1.example.com"));
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.properties.determinePort()).isEqualTo(5671);
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesDefaultSslPortWhenAddressesAreSetBeforeSslBundle() {
		this.properties.setAddresses(List.of("rabbit1.example.com", "rabbit2.example.com:2345"));
		this.properties.getSsl().setBundle("test-bundle");
		assertThat(this.properties.determineAddresses()).containsExactly("rabbit1.example.com:5671",
				"rabbit2.example.com:2345");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesDefaultSslPortWhenAddressesAreSetBeforeSslIsEnabled() {
		this.properties.setAddresses(List.of("rabbit1.example.com"));
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determineAddresses()).containsExactly("rabbit1.example.com:5671");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesExplicitPortWhenAddressesAreSetBeforeSslIsEnabled() {
		this.properties.setAddresses(List.of("rabbit1.example.com:1234"));
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determineAddresses()).containsExactly("rabbit1.example.com:1234");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesAmqpSchemeWhenAddressesAreSetBeforeSslIsEnabled() {
		this.properties.setAddresses(List.of("amqp://rabbit1.example.com", "amqps://rabbit2.example.com"));
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.determineAddresses()).containsExactly("rabbit1.example.com:5672",
				"rabbit2.example.com:5671");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesDefaultSslPortWhenBoundFromProperties() {
		RabbitProperties properties = bind(Map.of("spring.rabbitmq.addresses", "rabbit1.example.com",
				"spring.rabbitmq.ssl.bundle", "test-bundle"));
		assertThat(properties.getSsl().determineEnabled()).isTrue();
		assertThat(properties.determineAddresses()).containsExactly("rabbit1.example.com:5671");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesHostAndPortPropertiesWhenNoAddressesSet() {
		this.properties.setHost("rabbit.example.com");
		this.properties.setPort(1234);
		assertThat(this.properties.determineAddresses()).containsExactly("rabbit.example.com:1234");
	}

	@Test
	@SuppressWarnings("removal")
	void determineAddressesUsesIpv6HostAndPortPropertiesWhenNoAddressesSet() {
		this.properties.setHost("[::1]");
		this.properties.setPort(32863);
		assertThat(this.properties.determineAddresses()).containsExactly("[::1]:32863");
	}

	@Test
	@SuppressWarnings("removal")
	void determineSslUsingAmqpsReturnsStateOfFirstAddress() {
		this.properties.setAddresses(List.of("amqps://root:password@otherhost", "amqp://root:password2@otherhost2"));
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	@SuppressWarnings("removal")
	void sslDetermineEnabledIsTrueWhenAddressHasNoProtocolAndSslIsEnabled() {
		this.properties.getSsl().setEnabled(true);
		this.properties.setAddresses(List.of("root:password@otherhost"));
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	@SuppressWarnings("removal")
	void sslDetermineEnabledIsFalseWhenAddressHasNoProtocolAndSslIsDisabled() {
		this.properties.getSsl().setEnabled(false);
		this.properties.setAddresses(List.of("root:password@otherhost"));
		assertThat(this.properties.getSsl().determineEnabled()).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void determineSslUsingAmqpReturnsStateOfFirstAddress() {
		this.properties.setAddresses(List.of("amqp://root:password@otherhost", "amqps://root:password2@otherhost2"));
		assertThat(this.properties.getSsl().determineEnabled()).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void determineSslReturnFlagPropertyWhenNoAddresses() {
		this.properties.getSsl().setEnabled(true);
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	@SuppressWarnings("removal")
	void determineSslEnabledIsTrueWhenBundleIsSetAndNoAddresses() {
		this.properties.getSsl().setBundle("test");
		assertThat(this.properties.getSsl().determineEnabled()).isTrue();
	}

	@Test
	@SuppressWarnings("removal")
	void determineSslEnabledIsFalseWhenBundleIsEmpty() {
		this.properties.getSsl().setBundle("");
		assertThat(this.properties.getSsl().determineEnabled()).isFalse();
	}

	@Test
	void propertiesUseConsistentDefaultValues() {
		ConnectionFactory connectionFactory = new ConnectionFactory();
		assertThat(connectionFactory).hasFieldOrPropertyWithValue("maxInboundMessageBodySize",
				(int) this.properties.getMaxInboundMessageBodySize().toBytes());
	}

	@Test
	void simpleContainerUseConsistentDefaultValues() {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		SimpleMessageListenerContainer container = factory.createListenerContainer();
		RabbitProperties.SimpleContainer simple = this.properties.getListener().getSimple();
		assertThat(simple.isAutoStartup()).isEqualTo(container.isAutoStartup());
		assertThat(container).hasFieldOrPropertyWithValue("missingQueuesFatal", simple.isMissingQueuesFatal());
		assertThat(container).hasFieldOrPropertyWithValue("deBatchingEnabled", simple.isDeBatchingEnabled());
		assertThat(container).hasFieldOrPropertyWithValue("consumerBatchEnabled", simple.isConsumerBatchEnabled());
		assertThat(container).hasFieldOrPropertyWithValue("forceStop", simple.isForceStop());
	}

	@Test
	void directContainerUseConsistentDefaultValues() {
		DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
		DirectMessageListenerContainer container = factory.createListenerContainer();
		RabbitProperties.DirectContainer direct = this.properties.getListener().getDirect();
		assertThat(direct.isAutoStartup()).isEqualTo(container.isAutoStartup());
		assertThat(container).hasFieldOrPropertyWithValue("missingQueuesFatal", direct.isMissingQueuesFatal());
		assertThat(container).hasFieldOrPropertyWithValue("deBatchingEnabled", direct.isDeBatchingEnabled());
		assertThat(container).hasFieldOrPropertyWithValue("forceStop", direct.isForceStop());
	}

	@Test
	@SuppressWarnings("removal")
	void determineUsernameWithoutPassword() {
		this.properties.setAddresses(List.of("user@rabbit1.example.com:1234/alpha"));
		assertThat(this.properties.determineUsername()).isEqualTo("user");
		assertThat(this.properties.determinePassword()).isEqualTo("guest");
	}

	@Test
	@SuppressWarnings("removal")
	void hostPropertyMustBeSingleHost() {
		this.properties.setHost("my-rmq-host.net,my-rmq-host-2.net");
		assertThat(this.properties.getHost()).isEqualTo("my-rmq-host.net,my-rmq-host-2.net");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(this.properties::determineAddresses)
			.satisfies((ex) -> {
				assertThat(ex.getName()).isEqualTo("spring.rabbitmq.host");
				assertThat(ex.getValue()).isEqualTo("my-rmq-host.net,my-rmq-host-2.net");
				assertThat(ex.getReason()).isEqualTo("Invalid character ','. Value must be a single host. "
						+ "For multiple hosts, use property 'spring.rabbitmq.addresses' instead.");
			});
	}

	@Test
	@SuppressWarnings("removal")
	void streamSslIsDisabledByDefault() {
		assertThat(this.properties.getStream().getSsl().determineEnabled()).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void streamSslIsEnabledWhenEnabledIsTrue() {
		this.properties.getStream().getSsl().setEnabled(true);
		assertThat(this.properties.getStream().getSsl().determineEnabled()).isTrue();
	}

	@Test
	@SuppressWarnings("removal")
	void streamSslIsEnabledWhenBundleIsSet() {
		this.properties.getStream().getSsl().setBundle("test-bundle");
		assertThat(this.properties.getStream().getSsl().determineEnabled()).isTrue();
	}

	@Test
	@SuppressWarnings("removal")
	void streamSslIsDisabledWhenBundleIsEmpty() {
		this.properties.getStream().getSsl().setBundle("");
		assertThat(this.properties.getStream().getSsl().determineEnabled()).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void streamSslIsDisabledWhenEnabledIsFalseAndBundleIsNotSet() {
		this.properties.getStream().getSsl().setEnabled(false);
		assertThat(this.properties.getStream().getSsl().determineEnabled()).isFalse();
	}

	@Test
	@SuppressWarnings("removal")
	void streamSslIsEnabledWhenBundleIsSetButEnabledIsFalse() {
		this.properties.getStream().getSsl().setBundle("test-bundle");
		this.properties.getStream().getSsl().setEnabled(false);
		assertThat(this.properties.getStream().getSsl().determineEnabled()).isTrue();
	}

	private RabbitProperties bind(Map<String, String> properties) {
		return new Binder(new MapConfigurationPropertySource(properties))
			.bind("spring.rabbitmq", Bindable.of(RabbitProperties.class))
			.get();
	}

}
