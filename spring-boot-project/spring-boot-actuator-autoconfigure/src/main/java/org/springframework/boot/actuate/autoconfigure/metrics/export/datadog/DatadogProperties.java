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

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring Datadog
 * metrics export.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.datadog")
public class DatadogProperties extends StepRegistryProperties {

	/**
	 * Datadog API key.
	 */
	private String apiKey;

	/**
	 * Datadog application key. Not strictly required, but improves the Datadog experience
	 * by sending meter descriptions, types, and base units to Datadog.
	 */
	private String applicationKey;

	/**
	 * Whether to publish descriptions metadata to Datadog. Turn this off to minimize the
	 * amount of metadata sent.
	 */
	private boolean descriptions = true;

	/**
	 * Tag that will be mapped to "host" when shipping metrics to Datadog.
	 */
	private String hostTag = "instance";

	/**
	 * URI to ship metrics to. If you need to publish metrics to an internal proxy
	 * en-route to Datadog, you can define the location of the proxy with this.
	 */
	private String uri = "https://app.datadoghq.com";

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApplicationKey() {
		return this.applicationKey;
	}

	public void setApplicationKey(String applicationKey) {
		this.applicationKey = applicationKey;
	}

	public boolean isDescriptions() {
		return this.descriptions;
	}

	public void setDescriptions(boolean descriptions) {
		this.descriptions = descriptions;
	}

	public String getHostTag() {
		return this.hostTag;
	}

	public void setHostTag(String hostTag) {
		this.hostTag = hostTag;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
