/*
 * Copyright 2012-2019 the original author or authors.
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
@ConfigurationProperties(prefix = "management.metrics.export.appoptics")
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
	 * Number of measurements per request to use for this backend. If more measurements
	 * are found, then multiple requests will be made.
	 */
	private Integer batchSize = 500;

	/**
	 * Connection timeout for requests to this backend.
	 */
	private Duration connectTimeout = Duration.ofSeconds(5);

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getApiToken() {
		return this.apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	public String getHostTag() {
		return this.hostTag;
	}

	public void setHostTag(String hostTag) {
		this.hostTag = hostTag;
	}

	@Override
	public Integer getBatchSize() {
		return this.batchSize;
	}

	@Override
	public void setBatchSize(Integer batchSize) {
		this.batchSize = batchSize;
	}

	@Override
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	@Override
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
