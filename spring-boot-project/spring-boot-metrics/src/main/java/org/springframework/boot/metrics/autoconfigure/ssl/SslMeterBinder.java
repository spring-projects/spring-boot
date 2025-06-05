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

package org.springframework.boot.metrics.autoconfigure.ssl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.MultiGauge.Row;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.TimeGauge;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.boot.info.SslInfo;
import org.springframework.boot.info.SslInfo.BundleInfo;
import org.springframework.boot.info.SslInfo.CertificateChainInfo;
import org.springframework.boot.info.SslInfo.CertificateInfo;
import org.springframework.boot.ssl.SslBundles;

/**
 * {@link MeterBinder} which registers the SSL chain expiry (soonest to expire certificate
 * in the chain) as a {@link TimeGauge}.
 *
 * @author Moritz Halbritter
 */
class SslMeterBinder implements MeterBinder {

	private static final String CHAIN_EXPIRY_METRIC_NAME = "ssl.chain.expiry";

	private final Clock clock;

	private final SslInfo sslInfo;

	private final BundleMetrics bundleMetrics = new BundleMetrics();

	SslMeterBinder(SslInfo sslInfo, SslBundles sslBundles) {
		this(sslInfo, sslBundles, Clock.systemDefaultZone());
	}

	SslMeterBinder(SslInfo sslInfo, SslBundles sslBundles, Clock clock) {
		this.clock = clock;
		this.sslInfo = sslInfo;
		sslBundles.addBundleRegisterHandler((bundleName, ignored) -> onBundleChange(bundleName));
		for (String bundleName : sslBundles.getBundleNames()) {
			sslBundles.addBundleUpdateHandler(bundleName, (ignored) -> onBundleChange(bundleName));
		}
	}

	private void onBundleChange(String bundleName) {
		BundleInfo bundle = this.sslInfo.getBundle(bundleName);
		this.bundleMetrics.updateBundle(bundle);
		for (MeterRegistry meterRegistry : this.bundleMetrics.getMeterRegistries()) {
			createOrUpdateBundleMetrics(meterRegistry, bundle);
		}
	}

	@Override
	public void bindTo(MeterRegistry meterRegistry) {
		for (BundleInfo bundle : this.sslInfo.getBundles()) {
			createOrUpdateBundleMetrics(meterRegistry, bundle);
		}
	}

	private void createOrUpdateBundleMetrics(MeterRegistry meterRegistry, BundleInfo bundle) {
		MultiGauge multiGauge = this.bundleMetrics.getGauge(bundle, meterRegistry);
		List<Row<CertificateInfo>> rows = new ArrayList<>();
		for (CertificateChainInfo chain : bundle.getCertificateChains()) {
			Row<CertificateInfo> row = createRowForChain(bundle, chain);
			if (row != null) {
				rows.add(row);
			}
		}
		multiGauge.register(rows, true);
	}

	private Row<CertificateInfo> createRowForChain(BundleInfo bundle, CertificateChainInfo chain) {
		CertificateInfo leastValidCertificate = chain.getCertificates()
			.stream()
			.min(Comparator.comparing(CertificateInfo::getValidityEnds))
			.orElse(null);
		if (leastValidCertificate == null) {
			return null;
		}
		Tags tags = Tags.of("chain", chain.getAlias(), "bundle", bundle.getName(), "certificate",
				leastValidCertificate.getSerialNumber());
		return Row.of(tags, leastValidCertificate, this::getChainExpiry);
	}

	private long getChainExpiry(CertificateInfo certificate) {
		Duration valid = Duration.between(Instant.now(this.clock), certificate.getValidityEnds());
		return valid.get(ChronoUnit.SECONDS);
	}

	/**
	 * Manages bundles and their metrics.
	 */
	private static final class BundleMetrics {

		private final Map<String, Gauges> gauges = new ConcurrentHashMap<>();

		/**
		 * Gets (or creates) a {@link MultiGauge} for the given bundle and meter registry.
		 * @param bundleInfo the bundle
		 * @param meterRegistry the meter registry
		 * @return the {@link MultiGauge}
		 */
		MultiGauge getGauge(BundleInfo bundleInfo, MeterRegistry meterRegistry) {
			Gauges gauges = this.gauges.computeIfAbsent(bundleInfo.getName(),
					(ignored) -> Gauges.emptyGauges(bundleInfo));
			return gauges.getGauge(meterRegistry);
		}

		/**
		 * Returns all meter registries.
		 * @return all meter registries
		 */
		Collection<MeterRegistry> getMeterRegistries() {
			Set<MeterRegistry> result = new HashSet<>();
			for (Gauges metrics : this.gauges.values()) {
				result.addAll(metrics.getMeterRegistries());
			}
			return result;
		}

		/**
		 * Updates the given bundle.
		 * @param bundle the updated bundle
		 */
		void updateBundle(BundleInfo bundle) {
			this.gauges.computeIfPresent(bundle.getName(), (key, oldValue) -> oldValue.withBundle(bundle));
		}

		/**
		 * Manages the {@link MultiGauge MultiGauges} associated to a bundle.
		 *
		 * @param bundle the bundle
		 * @param multiGauges mapping from meter registry to {@link MultiGauge}
		 */
		private record Gauges(BundleInfo bundle, Map<MeterRegistry, MultiGauge> multiGauges) {

			/**
			 * Gets (or creates) the {@link MultiGauge} for the given meter registry.
			 * @param meterRegistry the meter registry
			 * @return the {@link MultiGauge}
			 */
			MultiGauge getGauge(MeterRegistry meterRegistry) {
				return this.multiGauges.computeIfAbsent(meterRegistry, (ignored) -> createGauge(meterRegistry));
			}

			/**
			 * Returns a copy of this bundle with an updated {@link BundleInfo}.
			 * @param bundle the updated {@link BundleInfo}
			 * @return the copy of this bundle with an updated {@link BundleInfo}
			 */
			Gauges withBundle(BundleInfo bundle) {
				return new Gauges(bundle, this.multiGauges);
			}

			/**
			 * Returns all meter registries.
			 * @return all meter registries
			 */
			Set<MeterRegistry> getMeterRegistries() {
				return this.multiGauges.keySet();
			}

			private MultiGauge createGauge(MeterRegistry meterRegistry) {
				return MultiGauge.builder(CHAIN_EXPIRY_METRIC_NAME)
					.baseUnit("seconds")
					.description("SSL chain expiry")
					.register(meterRegistry);
			}

			/**
			 * Creates an instance with an empty gauge mapping.
			 * @param bundle the {@link BundleInfo} associated with the new instance
			 * @return the new instance
			 */
			static Gauges emptyGauges(BundleInfo bundle) {
				return new Gauges(bundle, new ConcurrentHashMap<>());
			}
		}

	}

}
