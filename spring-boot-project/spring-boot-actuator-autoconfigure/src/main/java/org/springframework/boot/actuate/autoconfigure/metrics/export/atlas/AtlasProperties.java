/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.atlas;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Atlas metrics
 * export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.atlas")
public class AtlasProperties {

	/**
	 * Step size (i.e. reporting frequency) to use.
	 */
	private Duration step = Duration.ofMinutes(1);

	/**
	 * Whether exporting of metrics to this backend is enabled.
	 */
	private boolean enabled = true;

	/**
	 * Connection timeout for requests to this backend.
	 */
	private Duration connectTimeout = Duration.ofSeconds(1);

	/**
	 * Read timeout for requests to this backend.
	 */
	private Duration readTimeout = Duration.ofSeconds(10);

	/**
	 * Number of threads to use with the metrics publishing scheduler.
	 */
	private Integer numThreads = 4;

	/**
	 * Number of measurements per request to use for this backend. If more measurements
	 * are found, then multiple requests will be made.
	 */
	private Integer batchSize = 10000;

	/**
	 * URI of the Atlas server.
	 */
	private String uri = "http://localhost:7101/api/v1/publish";

	/**
	 * Time to live for meters that do not have any activity. After this period the meter
	 * will be considered expired and will not get reported.
	 */
	private Duration meterTimeToLive = Duration.ofMinutes(15);

	/**
	 * Whether to enable streaming to Atlas LWC.
	 */
	private boolean lwcEnabled;

	/**
	 * Frequency for refreshing config settings from the LWC service.
	 */
	private Duration configRefreshFrequency = Duration.ofSeconds(10);

	/**
	 * Time to live for subscriptions from the LWC service.
	 */
	private Duration configTimeToLive = Duration.ofSeconds(150);

	/**
	 * URI for the Atlas LWC endpoint to retrieve current subscriptions.
	 */
	private String configUri = "http://localhost:7101/lwc/api/v1/expressions/local-dev";

	/**
	 * URI for the Atlas LWC endpoint to evaluate the data for a subscription.
	 */
	private String evalUri = "http://localhost:7101/lwc/api/v1/evaluate";

	public Duration getStep() {
		return this.step;
	}

	public void setStep(Duration step) {
		this.step = step;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public Integer getNumThreads() {
		return this.numThreads;
	}

	public void setNumThreads(Integer numThreads) {
		this.numThreads = numThreads;
	}

	public Integer getBatchSize() {
		return this.batchSize;
	}

	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Duration getMeterTimeToLive() {
		return this.meterTimeToLive;
	}

	public void setMeterTimeToLive(Duration meterTimeToLive) {
		this.meterTimeToLive = meterTimeToLive;
	}

	public boolean isLwcEnabled() {
		return this.lwcEnabled;
	}

	public void setLwcEnabled(boolean lwcEnabled) {
		this.lwcEnabled = lwcEnabled;
	}

	public Duration getConfigRefreshFrequency() {
		return this.configRefreshFrequency;
	}

	public void setConfigRefreshFrequency(Duration configRefreshFrequency) {
		this.configRefreshFrequency = configRefreshFrequency;
	}

	public Duration getConfigTimeToLive() {
		return this.configTimeToLive;
	}

	public void setConfigTimeToLive(Duration configTimeToLive) {
		this.configTimeToLive = configTimeToLive;
	}

	public String getConfigUri() {
		return this.configUri;
	}

	public void setConfigUri(String configUri) {
		this.configUri = configUri;
	}

	public String getEvalUri() {
		return this.evalUri;
	}

	public void setEvalUri(String evalUri) {
		this.evalUri = evalUri;
	}

}
