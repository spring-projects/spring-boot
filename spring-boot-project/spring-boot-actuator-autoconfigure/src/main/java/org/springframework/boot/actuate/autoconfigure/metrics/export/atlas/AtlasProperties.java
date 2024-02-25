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
@ConfigurationProperties(prefix = "management.atlas.metrics.export")
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
	 * Step size (reporting frequency) to use for streaming to Atlas LWC. This is the
	 * highest supported resolution for getting an on-demand stream of the data. It must
	 * be less than or equal to management.metrics.export.atlas.step and
	 * management.metrics.export.atlas.step should be an even multiple of this value.
	 */
	private Duration lwcStep = Duration.ofSeconds(5);

	/**
	 * Whether expressions with the same step size as Atlas publishing should be ignored
	 * for streaming. Used for cases where data being published to Atlas is also sent into
	 * streaming from the backend.
	 */
	private boolean lwcIgnorePublishStep = true;

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

	/**
	 * Returns the step duration.
	 * @return the step duration
	 */
	public Duration getStep() {
		return this.step;
	}

	/**
	 * Sets the step duration for the AtlasProperties.
	 * @param step the step duration to be set
	 */
	public void setStep(Duration step) {
		this.step = step;
	}

	/**
	 * Returns the current status of the enabled flag.
	 * @return true if the enabled flag is set to true, false otherwise.
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Sets the enabled status of the AtlasProperties.
	 * @param enabled the enabled status to be set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the connect timeout duration.
	 * @return the connect timeout duration
	 */
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	/**
	 * Sets the connection timeout for the AtlasProperties class.
	 * @param connectTimeout the duration of the connection timeout
	 */
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * Returns the read timeout duration.
	 * @return the read timeout duration
	 */
	public Duration getReadTimeout() {
		return this.readTimeout;
	}

	/**
	 * Sets the read timeout for the connection.
	 * @param readTimeout the duration of the read timeout
	 */
	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	/**
	 * Returns the number of threads.
	 * @return the number of threads
	 */
	public Integer getNumThreads() {
		return this.numThreads;
	}

	/**
	 * Sets the number of threads to be used.
	 * @param numThreads the number of threads to be set
	 */
	public void setNumThreads(Integer numThreads) {
		this.numThreads = numThreads;
	}

	/**
	 * Returns the batch size.
	 * @return the batch size
	 */
	public Integer getBatchSize() {
		return this.batchSize;
	}

	/**
	 * Sets the batch size for processing data.
	 * @param batchSize the batch size to be set
	 */
	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Returns the URI of the AtlasProperties object.
	 * @return the URI of the AtlasProperties object
	 */
	public String getUri() {
		return this.uri;
	}

	/**
	 * Sets the URI for the AtlasProperties class.
	 * @param uri the URI to be set
	 */
	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Returns the time to live for the meter in the AtlasProperties class.
	 * @return the time to live for the meter
	 */
	public Duration getMeterTimeToLive() {
		return this.meterTimeToLive;
	}

	/**
	 * Sets the time to live for the meter.
	 * @param meterTimeToLive the time to live for the meter
	 */
	public void setMeterTimeToLive(Duration meterTimeToLive) {
		this.meterTimeToLive = meterTimeToLive;
	}

	/**
	 * Returns a boolean value indicating whether LWC (Lightning Web Components) is
	 * enabled.
	 * @return true if LWC is enabled, false otherwise
	 */
	public boolean isLwcEnabled() {
		return this.lwcEnabled;
	}

	/**
	 * Sets the LWC enabled flag.
	 * @param lwcEnabled the value indicating whether LWC is enabled or not
	 */
	public void setLwcEnabled(boolean lwcEnabled) {
		this.lwcEnabled = lwcEnabled;
	}

	/**
	 * Returns the LWC step duration.
	 * @return the LWC step duration
	 */
	public Duration getLwcStep() {
		return this.lwcStep;
	}

	/**
	 * Sets the duration of the LWC step.
	 * @param lwcStep the duration of the LWC step to be set
	 */
	public void setLwcStep(Duration lwcStep) {
		this.lwcStep = lwcStep;
	}

	/**
	 * Returns the value of the lwcIgnorePublishStep property.
	 * @return true if the lwcIgnorePublishStep property is set to true, false otherwise.
	 */
	public boolean isLwcIgnorePublishStep() {
		return this.lwcIgnorePublishStep;
	}

	/**
	 * Sets the flag to ignore the publish step for LWC.
	 * @param lwcIgnorePublishStep the flag indicating whether to ignore the publish step
	 * for LWC
	 */
	public void setLwcIgnorePublishStep(boolean lwcIgnorePublishStep) {
		this.lwcIgnorePublishStep = lwcIgnorePublishStep;
	}

	/**
	 * Returns the refresh frequency of the configuration.
	 * @return the refresh frequency of the configuration
	 */
	public Duration getConfigRefreshFrequency() {
		return this.configRefreshFrequency;
	}

	/**
	 * Sets the refresh frequency for the configuration.
	 * @param configRefreshFrequency the refresh frequency for the configuration
	 */
	public void setConfigRefreshFrequency(Duration configRefreshFrequency) {
		this.configRefreshFrequency = configRefreshFrequency;
	}

	/**
	 * Returns the time to live for the configuration.
	 * @return the time to live for the configuration
	 */
	public Duration getConfigTimeToLive() {
		return this.configTimeToLive;
	}

	/**
	 * Sets the time to live for the configuration.
	 * @param configTimeToLive the time to live for the configuration
	 */
	public void setConfigTimeToLive(Duration configTimeToLive) {
		this.configTimeToLive = configTimeToLive;
	}

	/**
	 * Returns the configuration URI.
	 * @return the configuration URI
	 */
	public String getConfigUri() {
		return this.configUri;
	}

	/**
	 * Sets the configuration URI for AtlasProperties.
	 * @param configUri the configuration URI to be set
	 */
	public void setConfigUri(String configUri) {
		this.configUri = configUri;
	}

	/**
	 * Returns the evaluation URI.
	 * @return the evaluation URI
	 */
	public String getEvalUri() {
		return this.evalUri;
	}

	/**
	 * Sets the evaluation URI for the AtlasProperties class.
	 * @param evalUri the evaluation URI to be set
	 */
	public void setEvalUri(String evalUri) {
		this.evalUri = evalUri;
	}

}
