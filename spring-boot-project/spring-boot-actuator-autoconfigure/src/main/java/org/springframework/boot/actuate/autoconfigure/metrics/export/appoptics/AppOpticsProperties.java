/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.appoptics;

import java.time.Duration;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring AppOptics
 * metrics export.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.appoptics.metrics.export")
public class AppOpticsProperties extends StepRegistryProperties {

	/**
	 * URI to ship metrics to.
	 */
	private String uri = "https://api.appoptics.com/v1/measurements";

	/**
	 * AppOptics API token.
	 */
	private String apiToken;

	/**
	 * Tag that will be mapped to "@host" when shipping metrics to AppOptics.
	 */
	private String hostTag = "instance";

	/**
	 * Whether to ship a floored time, useful when sending measurements from multiple
	 * hosts to align them on a given time boundary.
	 */
	private boolean floorTimes;

	/**
	 * Number of measurements per request to use for this backend. If more measurements
	 * are found, then multiple requests will be made.
	 */
	private Integer batchSize = 500;

	/**
	 * Connection timeout for requests to this backend.
	 */
	private Duration connectTimeout = Duration.ofSeconds(5);

	/**
     * Returns the URI of the AppOpticsProperties object.
     *
     * @return the URI of the AppOpticsProperties object
     */
    public String getUri() {
		return this.uri;
	}

	/**
     * Sets the URI for the AppOpticsProperties.
     * 
     * @param uri the URI to be set
     */
    public void setUri(String uri) {
		this.uri = uri;
	}

	/**
     * Returns the API token.
     *
     * @return the API token
     */
    public String getApiToken() {
		return this.apiToken;
	}

	/**
     * Sets the API token for authentication.
     * 
     * @param apiToken the API token to be set
     */
    public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	/**
     * Returns the host tag associated with the AppOpticsProperties object.
     *
     * @return the host tag
     */
    public String getHostTag() {
		return this.hostTag;
	}

	/**
     * Sets the host tag for the AppOpticsProperties.
     * 
     * @param hostTag the host tag to be set
     */
    public void setHostTag(String hostTag) {
		this.hostTag = hostTag;
	}

	/**
     * Returns the value indicating whether the floor times option is enabled.
     * 
     * @return true if the floor times option is enabled, false otherwise
     */
    public boolean isFloorTimes() {
		return this.floorTimes;
	}

	/**
     * Sets the value of the floorTimes property.
     * 
     * @param floorTimes the new value for the floorTimes property
     */
    public void setFloorTimes(boolean floorTimes) {
		this.floorTimes = floorTimes;
	}

	/**
     * Returns the batch size for processing data.
     *
     * @return the batch size for processing data
     */
    @Override
	public Integer getBatchSize() {
		return this.batchSize;
	}

	/**
     * Sets the batch size for sending data to AppOptics.
     * 
     * @param batchSize the batch size to set
     */
    @Override
	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	/**
     * Returns the connect timeout duration.
     *
     * @return the connect timeout duration
     */
    @Override
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	/**
     * Sets the connection timeout for the AppOpticsProperties.
     * 
     * @param connectTimeout the duration of the connection timeout
     */
    @Override
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
