/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link PropertiesKafkaConnectionDetails}.
 *
 * @author Henning Kvinnesland
 */
class PropertiesKafkaConnectionDetailsTests {

	private KafkaProperties properties;

	private DefaultSslBundleRegistry sslBundleRegistry;

	private PropertiesKafkaConnectionDetails connectionDetails;

	@BeforeEach
	void setUp() {
		this.properties = new KafkaProperties();
		this.sslBundleRegistry = new DefaultSslBundleRegistry();
		this.connectionDetails = new PropertiesKafkaConnectionDetails(this.properties, this.sslBundleRegistry);
	}

	@Test
	void getSslBundleWhenNoBundleConfiguredReturnsNull() {
		assertThat(this.connectionDetails.getSslBundle()).isNull();
	}

	@Test
	void getSslBundleResolvesConfiguredBundle() {
		SslBundle bundle = mock(SslBundle.class);
		given(bundle.getProtocol()).willReturn("bundle-1");
		this.sslBundleRegistry.registerBundle("bundle-1", bundle);
		this.properties.getSsl().setBundle("bundle-1");
		SslBundle sslBundle = this.connectionDetails.getSslBundle();
		assertThat(sslBundle).isNotNull();
		assertThat(sslBundle.getProtocol()).isEqualTo("bundle-1");
	}

	@Test
	void getSslBundleWhenBundleIsUnknownFailsImmediately() {
		this.properties.getSsl().setBundle("missing");
		assertThatExceptionOfType(NoSuchSslBundleException.class)
			.isThrownBy(() -> this.connectionDetails.getSslBundle())
			.satisfies((ex) -> assertThat(ex.getBundleName()).isEqualTo("missing"));
	}

	@Test
	void getSslBundleReturnsSameBundleOnEachCall() {
		this.sslBundleRegistry.registerBundle("bundle-1", mock(SslBundle.class));
		this.properties.getSsl().setBundle("bundle-1");
		assertThat(this.connectionDetails.getSslBundle()).isSameAs(this.connectionDetails.getSslBundle());
	}

	@Test
	void getSslBundleFollowsRegistryUpdates() {
		SslBundle initial = mock(SslBundle.class);
		SslBundle updated = mock(SslBundle.class);
		given(initial.getProtocol()).willReturn("initial");
		given(updated.getProtocol()).willReturn("updated");
		this.sslBundleRegistry.registerBundle("bundle-1", initial);
		this.properties.getSsl().setBundle("bundle-1");
		SslBundle captured = this.connectionDetails.getSslBundle();
		assertThat(captured).isNotNull();
		assertThat(captured.getProtocol()).isEqualTo("initial");
		this.sslBundleRegistry.updateBundle("bundle-1", updated);
		assertThat(captured.getProtocol()).isEqualTo("updated");
	}

	@Test
	void componentSpecificBundlesAreResolvedIndependently() {
		SslBundle consumerBundle = mock(SslBundle.class);
		SslBundle producerBundle = mock(SslBundle.class);
		given(consumerBundle.getProtocol()).willReturn("consumer-bundle");
		given(producerBundle.getProtocol()).willReturn("producer-bundle");
		this.sslBundleRegistry.registerBundle("consumer-bundle", consumerBundle);
		this.sslBundleRegistry.registerBundle("producer-bundle", producerBundle);
		this.properties.getConsumer().getSsl().setBundle("consumer-bundle");
		this.properties.getProducer().getSsl().setBundle("producer-bundle");
		SslBundle consumer = this.connectionDetails.getConsumer().getSslBundle();
		SslBundle producer = this.connectionDetails.getProducer().getSslBundle();
		assertThat(consumer).isNotNull();
		assertThat(producer).isNotNull();
		assertThat(consumer.getProtocol()).isEqualTo("consumer-bundle");
		assertThat(producer.getProtocol()).isEqualTo("producer-bundle");
	}

	@Test
	void getSslBundleRegistersASingleUpdateHandlerPerBundle() {
		SslBundles sslBundles = mock(SslBundles.class);
		given(sslBundles.getBundle("bundle-1")).willReturn(mock(SslBundle.class));
		PropertiesKafkaConnectionDetails details = new PropertiesKafkaConnectionDetails(this.properties, sslBundles);
		this.properties.getSsl().setBundle("bundle-1");
		details.getSslBundle();
		details.getSslBundle();
		details.getConsumer();
		then(sslBundles).should(times(1)).addBundleUpdateHandler(eq("bundle-1"), any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void registeredUpdateHandlerAcceptsUpdates() {
		SslBundles sslBundles = mock(SslBundles.class);
		given(sslBundles.getBundle("bundle-1")).willReturn(mock(SslBundle.class));
		PropertiesKafkaConnectionDetails details = new PropertiesKafkaConnectionDetails(this.properties, sslBundles);
		this.properties.getSsl().setBundle("bundle-1");
		details.getSslBundle();
		ArgumentCaptor<Consumer<SslBundle>> handler = ArgumentCaptor.forClass(Consumer.class);
		then(sslBundles).should().addBundleUpdateHandler(eq("bundle-1"), handler.capture());
		assertThatNoException().isThrownBy(() -> handler.getValue().accept(mock(SslBundle.class)));
	}

}
