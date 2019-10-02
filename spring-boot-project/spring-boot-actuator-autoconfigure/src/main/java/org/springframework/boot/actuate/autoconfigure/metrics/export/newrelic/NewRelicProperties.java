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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring New Relic
 * metrics export.
 *
 * @author Jon Schneider
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Neil Powell
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "management.metrics.export.newrelic")
public class NewRelicProperties extends StepRegistryProperties {

	/**
	 * When this is {@code false}, the New Relic eventType value will be set to
	 * {@link #eventType()}. Otherwise, the meter name will be used. Defaults to
	 * {@code false}.
	 */
	private boolean meterNameEventTypeEnabled;

	/**
	 * This configuration property will only be used if
	 * {@link #meterNameEventTypeEnabled()} is {@code false}. Default value is
	 * {@code SpringBootSample}.
	 */
	private String eventType = "SpringBootSample";

	/**
	 * New Relic API key.
	 */
	private String apiKey;

	/**
	 * New Relic account ID.
	 */
	private String accountId;

	/**
	 * URI to ship metrics to.
	 */
	private String uri = "https://insights-collector.newrelic.com";

	public boolean isMeterNameEventTypeEnabled() {
		return this.meterNameEventTypeEnabled;
	}

	public void setMeterNameEventTypeEnabled(boolean meterNameEventTypeEnabled) {
		this.meterNameEventTypeEnabled = meterNameEventTypeEnabled;
	}

	public String getEventType() {
		return this.eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getApiKey() {
		return this.apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getAccountId() {
		return this.accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
