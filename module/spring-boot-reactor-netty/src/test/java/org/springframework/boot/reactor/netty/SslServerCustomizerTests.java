/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.reactor.netty;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.netty.tcp.SslProvider;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.web.server.Ssl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Daeho Kwon
 */
class SslServerCustomizerTests {

	@Test
	@WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
	void getSslProviderReturnsMappedProviderForKnownServerName() {
		SslBundle defaultBundle = createBundle("1.key", "1.crt");
		SslBundle mappedBundle = createBundle("2.key", "2.crt");
		SslServerCustomizer customizer = new SslServerCustomizer(null, Ssl.ClientAuth.NONE, defaultBundle,
				Map.of("mapped.example", mappedBundle));
		SslProvider mapped = customizer.getSslProvider("mapped.example");
		assertThat(mapped).isNotNull().isNotSameAs(customizer.getSslProvider(null));
	}

	@Test
	@WithPackageResources({ "1.key", "1.crt", "2.key", "2.crt" })
	void getSslProviderFallsBackToDefaultWhenServerNameIsUnmapped() {
		SslBundle defaultBundle = createBundle("1.key", "1.crt");
		SslBundle mappedBundle = createBundle("2.key", "2.crt");
		SslServerCustomizer customizer = new SslServerCustomizer(null, Ssl.ClientAuth.NONE, defaultBundle,
				Map.of("mapped.example", mappedBundle));
		assertThat(customizer.getSslProvider("unmapped.example")).isSameAs(customizer.getSslProvider(null));
	}

	@Test
	@WithPackageResources({ "1.key", "1.crt" })
	void getSslProviderReturnsDefaultWhenServerNameIsNull() {
		SslBundle defaultBundle = createBundle("1.key", "1.crt");
		SslServerCustomizer customizer = new SslServerCustomizer(null, Ssl.ClientAuth.NONE, defaultBundle,
				Collections.emptyMap());
		assertThat(customizer.getSslProvider(null)).isNotNull();
	}

	private static SslBundle createBundle(String key, String certificate) {
		return SslBundle.of(new PemSslStoreBundle(
				new PemSslStoreDetails(null, "classpath:" + certificate, "classpath:" + key), null));
	}

}
