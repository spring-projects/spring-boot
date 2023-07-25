/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.wavefront;

import io.micrometer.wavefront.WavefrontConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.PushRegistryPropertiesConfigAdapter;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties.Metrics.Export;

/**
 * Adapter to convert {@link WavefrontProperties} to a {@link WavefrontConfig}.
 *
 * @author Jon Schneider
 * @author Moritz Halbritter
 * @since 2.0.0
 */
public class WavefrontPropertiesConfigAdapter
		extends PushRegistryPropertiesConfigAdapter<WavefrontProperties.Metrics.Export> implements WavefrontConfig {

	private final WavefrontProperties properties;

	public WavefrontPropertiesConfigAdapter(WavefrontProperties properties) {
		super(properties.getMetrics().getExport());
		this.properties = properties;
	}

	@Override
	public String prefix() {
		return "management.wavefront.metrics.export";
	}

	@Override
	public String uri() {
		return this.properties.getEffectiveUri().toString();
	}

	@Override
	public String source() {
		return this.properties.getSourceOrDefault();
	}

	@Override
	public int batchSize() {
		return this.properties.getSender().getBatchSize();
	}

	@Override
	public String apiToken() {
		return this.properties.getApiTokenOrThrow();
	}

	@Override
	public String globalPrefix() {
		return get(Export::getGlobalPrefix, WavefrontConfig.super::globalPrefix);
	}

	@Override
	public boolean reportMinuteDistribution() {
		return get(Export::isReportMinuteDistribution, WavefrontConfig.super::reportMinuteDistribution);
	}

	@Override
	public boolean reportHourDistribution() {
		return get(Export::isReportHourDistribution, WavefrontConfig.super::reportHourDistribution);
	}

	@Override
	public boolean reportDayDistribution() {
		return get(Export::isReportDayDistribution, WavefrontConfig.super::reportDayDistribution);
	}

}
