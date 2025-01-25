/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.ssl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.info.SslInfo;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslMeterBinder}.
 *
 * @author Moritz Halbritter
 */
class SslMeterBinderTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-10-21T13:51:40Z"), ZoneId.of("UTC"));

	@Test
	void shouldRegisterChainMetrics() {
		MeterRegistry meterRegistry = bindToRegistry();
		assertThat(meterRegistry.get("ssl.chains").tag("status", "valid").gauge().value()).isEqualTo(3.0);
		assertThat(meterRegistry.get("ssl.chains").tag("status", "expired").gauge().value()).isEqualTo(1.0);
		assertThat(meterRegistry.get("ssl.chains").tag("status", "not-yet-valid").gauge().value()).isEqualTo(1.0);
		assertThat(meterRegistry.get("ssl.chains").tag("status", "will-expire-soon").gauge().value()).isEqualTo(0.0);
	}

	@Test
	void shouldRegisterChainExpiryMetrics() {
		MeterRegistry meterRegistry = bindToRegistry();
		assertThat(Duration.ofSeconds(findExpiryGauge(meterRegistry, "ca", "419224ce190242b2c44069dd3c560192b3b669f3")))
			.hasDays(1095);
		assertThat(Duration
			.ofSeconds(findExpiryGauge(meterRegistry, "intermediary", "60f79365fc46bf69149754d377680192b3b6bcf5")))
			.hasDays(730);
		assertThat(Duration
			.ofSeconds(findExpiryGauge(meterRegistry, "server", "504c45129526ac050abb11459b1f0192b3b70fe9")))
			.hasDays(365);
		assertThat(Duration
			.ofSeconds(findExpiryGauge(meterRegistry, "expired", "562bc5dcf4f26bb179abb13068180192b3bb53dc")))
			.hasDays(-386);
		assertThat(Duration
			.ofSeconds(findExpiryGauge(meterRegistry, "not-yet-valid", "7df79335f274e2cfa7467fd5f9ce0192b3bcf4aa")))
			.hasDays(36889);
	}

	private static long findExpiryGauge(MeterRegistry meterRegistry, String chain, String certificateSerialNumber) {
		return (long) meterRegistry.get("ssl.chain.expiry")
			.tag("bundle", "test-0")
			.tag("chain", chain)
			.tag("certificate", certificateSerialNumber)
			.gauge()
			.value();
	}

	private SimpleMeterRegistry bindToRegistry() {
		SslBundles sslBundles = createSslBundles("classpath:certificates/chains.p12");
		SslInfo sslInfo = createSslInfo(sslBundles);
		SslMeterBinder binder = new SslMeterBinder(sslInfo, sslBundles, CLOCK);
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		binder.bindTo(meterRegistry);
		return meterRegistry;
	}

	private SslBundles createSslBundles(String... locations) {
		DefaultSslBundleRegistry sslBundleRegistry = new DefaultSslBundleRegistry();
		for (int i = 0; i < locations.length; i++) {
			JksSslStoreDetails keyStoreDetails = JksSslStoreDetails.forLocation(locations[i]).withPassword("secret");
			SslStoreBundle sslStoreBundle = new JksSslStoreBundle(keyStoreDetails, null);
			sslBundleRegistry.registerBundle("test-%d".formatted(i), SslBundle.of(sslStoreBundle));
		}
		return sslBundleRegistry;
	}

	private SslInfo createSslInfo(SslBundles sslBundles) {
		return new SslInfo(sslBundles, Duration.ofDays(7), CLOCK);
	}

}
