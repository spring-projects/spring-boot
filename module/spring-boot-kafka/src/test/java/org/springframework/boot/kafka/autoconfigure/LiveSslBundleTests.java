/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LiveSslBundle}.
 *
 * @author Henning Kvinnesland
 */
class LiveSslBundleTests {

	private final SslBundle initial = mock(SslBundle.class);

	private final SslBundle updated = mock(SslBundle.class);

	private DefaultSslBundleRegistry registry;

	private LiveSslBundle bundle;

	@BeforeEach
	void setUp() {
		this.registry = new DefaultSslBundleRegistry("test", this.initial);
		this.bundle = new LiveSslBundle(this.registry, "test");
	}

	@Test
	void delegatesToRegisteredBundle() {
		SslStoreBundle stores = mock(SslStoreBundle.class);
		SslBundleKey key = mock(SslBundleKey.class);
		SslOptions options = mock(SslOptions.class);
		SslManagerBundle managers = mock(SslManagerBundle.class);
		SSLContext sslContext = mock(SSLContext.class);
		given(this.initial.getStores()).willReturn(stores);
		given(this.initial.getKey()).willReturn(key);
		given(this.initial.getOptions()).willReturn(options);
		given(this.initial.getProtocol()).willReturn("initial");
		given(this.initial.getManagers()).willReturn(managers);
		given(this.initial.createSslContext()).willReturn(sslContext);
		assertThat(this.bundle.getStores()).isSameAs(stores);
		assertThat(this.bundle.getKey()).isSameAs(key);
		assertThat(this.bundle.getOptions()).isSameAs(options);
		assertThat(this.bundle.getProtocol()).isEqualTo("initial");
		assertThat(this.bundle.getManagers()).isSameAs(managers);
		assertThat(this.bundle.createSslContext()).isSameAs(sslContext);
	}

	@Test
	void followsUpdatesToRegisteredBundle() {
		SslStoreBundle stores = mock(SslStoreBundle.class);
		SslBundleKey key = mock(SslBundleKey.class);
		SslOptions options = mock(SslOptions.class);
		SslManagerBundle managers = mock(SslManagerBundle.class);
		SSLContext sslContext = mock(SSLContext.class);
		given(this.initial.getProtocol()).willReturn("initial");
		given(this.updated.getStores()).willReturn(stores);
		given(this.updated.getKey()).willReturn(key);
		given(this.updated.getOptions()).willReturn(options);
		given(this.updated.getProtocol()).willReturn("updated");
		given(this.updated.getManagers()).willReturn(managers);
		given(this.updated.createSslContext()).willReturn(sslContext);
		assertThat(this.bundle.getProtocol()).isEqualTo("initial");
		this.registry.updateBundle("test", this.updated);
		assertThat(this.bundle.getStores()).isSameAs(stores);
		assertThat(this.bundle.getKey()).isSameAs(key);
		assertThat(this.bundle.getOptions()).isSameAs(options);
		assertThat(this.bundle.getProtocol()).isEqualTo("updated");
		assertThat(this.bundle.getManagers()).isSameAs(managers);
		assertThat(this.bundle.createSslContext()).isSameAs(sslContext);
	}

	@Test
	void createSslContextResolvesRegisteredBundleOnce() {
		SSLContext first = mock(SSLContext.class);
		SSLContext second = mock(SSLContext.class);
		given(this.initial.createSslContext()).willReturn(first);
		given(this.updated.createSslContext()).willReturn(second);
		assertThat(this.bundle.createSslContext()).isSameAs(first);
		this.registry.updateBundle("test", this.updated);
		assertThat(this.bundle.createSslContext()).isSameAs(second);
	}

	@Test
	void bundlesForTheSameNameAreEqual() {
		assertThat(new LiveSslBundle(this.registry, "test")).isEqualTo(this.bundle)
			.hasSameHashCodeAs(this.bundle)
			.isNotEqualTo(new LiveSslBundle(this.registry, "other"));
	}

	@Test
	void toStringUsesRegisteredBundle() {
		given(this.initial.toString()).willReturn("initial-bundle");
		assertThat(this.bundle).hasToString("initial-bundle");
	}

	@Test
	void unknownBundleNameThrowsOnAccess() {
		LiveSslBundle missing = new LiveSslBundle(this.registry, "missing");
		assertThatExceptionOfType(NoSuchSslBundleException.class).isThrownBy(missing::getStores);
	}

}
