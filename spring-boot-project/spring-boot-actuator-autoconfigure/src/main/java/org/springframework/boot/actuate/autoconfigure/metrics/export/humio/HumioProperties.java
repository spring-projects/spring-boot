/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.humio;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties} for configuring Humio metrics export.
 *
 * @author Andy Wilkinson
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.humio")
public class HumioProperties extends StepRegistryProperties {

	/**
	 * Humio API token.
	 */
	private String apiToken;

	/**
	 * Connection timeout for requests to this backend.
	 */
	private Duration connectTimeout = Duration.ofSeconds(5);

	/**
	 * Name of the repository to publish metrics to.
	 */
	private String repository = "sandbox";

	/**
	 * Humio tags describing the data source in which metrics will be stored. Humio tags
	 * are a distinct concept from Micrometer's tags. Micrometer's tags are used to divide
	 * metrics along dimensional boundaries.
	 */
	private Map<String, String> tags = new HashMap<>();

	/**
	 * URI to ship metrics to. If you need to publish metrics to an internal proxy
	 * en-route to Humio, you can define the location of the proxy with this.
	 */
	private String uri = "https://cloud.humio.com";

	public String getApiToken() {
		return this.apiToken;
	}

	public void setApiToken(String apiToken) {
		this.apiToken = apiToken;
	}

	@Override
	public Duration getConnectTimeout() {
		return this.connectTimeout;
	}

	@Override
	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public String getRepository() {
		return this.repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public Map<String, String> getTags() {
		return this.tags;
	}

	public void setTags(Map<String, String> tags) {
		this.tags = tags;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
