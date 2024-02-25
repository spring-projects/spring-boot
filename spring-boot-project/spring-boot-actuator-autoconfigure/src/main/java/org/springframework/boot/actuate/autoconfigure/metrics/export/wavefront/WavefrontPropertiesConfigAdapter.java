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

import com.wavefront.sdk.common.clients.service.token.TokenService.Type;
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

	/**
     * Constructs a new WavefrontPropertiesConfigAdapter with the given WavefrontProperties.
     * 
     * @param properties the WavefrontProperties object containing the configuration properties
     */
    public WavefrontPropertiesConfigAdapter(WavefrontProperties properties) {
		super(properties.getMetrics().getExport());
		this.properties = properties;
	}

	/**
     * Returns the prefix for the Wavefront metrics export configuration properties.
     *
     * @return the prefix for the Wavefront metrics export configuration properties
     */
    @Override
	public String prefix() {
		return "management.wavefront.metrics.export";
	}

	/**
     * Returns the URI of the effective properties.
     *
     * @return the URI of the effective properties
     */
    @Override
	public String uri() {
		return this.properties.getEffectiveUri().toString();
	}

	/**
     * Returns the source of the Wavefront properties configuration.
     * 
     * @return the source of the Wavefront properties configuration
     */
    @Override
	public String source() {
		return this.properties.getSourceOrDefault();
	}

	/**
     * Returns the batch size for sending data.
     *
     * @return the batch size for sending data
     */
    @Override
	public int batchSize() {
		return this.properties.getSender().getBatchSize();
	}

	/**
     * Returns the API token from the properties configuration.
     *
     * @return the API token
     * @throws IllegalStateException if the API token is not found in the properties configuration
     */
    @Override
	public String apiToken() {
		return this.properties.getApiTokenOrThrow();
	}

	/**
     * Returns the global prefix for the Wavefront configuration.
     * 
     * @return the global prefix
     */
    @Override
	public String globalPrefix() {
		return get(Export::getGlobalPrefix, WavefrontConfig.super::globalPrefix);
	}

	/**
     * Returns the value of the 'reportMinuteDistribution' property.
     *
     * @return the value of the 'reportMinuteDistribution' property
     */
    @Override
	public boolean reportMinuteDistribution() {
		return get(Export::isReportMinuteDistribution, WavefrontConfig.super::reportMinuteDistribution);
	}

	/**
     * Returns the value of the reportHourDistribution property.
     *
     * @return {@code true} if the reportHourDistribution property is enabled, {@code false} otherwise.
     */
    @Override
	public boolean reportHourDistribution() {
		return get(Export::isReportHourDistribution, WavefrontConfig.super::reportHourDistribution);
	}

	/**
     * Returns the value of the 'reportDayDistribution' property.
     *
     * @return the value of the 'reportDayDistribution' property
     */
    @Override
	public boolean reportDayDistribution() {
		return get(Export::isReportDayDistribution, WavefrontConfig.super::reportDayDistribution);
	}

	/**
     * Returns the API token type for the Wavefront API.
     *
     * @return the API token type
     */
    @Override
	public Type apiTokenType() {
		return this.properties.getWavefrontApiTokenType();
	}

}
