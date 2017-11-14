/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.export.datadog;

import org.springframework.boot.actuate.autoconfigure.metrics.export.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Datadog metrics export.
 *
 * @author Jon Schneider
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.metrics.export.datadog")
public class DatadogProperties extends StepRegistryProperties {

	/**
	 * Datadog API key.
	 */
	private String apiKey;

	/**
	 * Tag that will be mapped to "host" when shipping metrics to Datadog. Can be
	 * omitted of host should be omitted on publishing.
	 */
	private String hostTag;

	/**
	 * URI to ship metrics to. If you need to publish metrics to an internal proxy
	 * en-route to Datadog, you can define the location of the proxy with this.
	 */
	private String uri;

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getHostTag() {
		return this.hostTag;
	}

	public void setHostKey(String hostKey) {
		this.hostTag = hostKey;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
}
