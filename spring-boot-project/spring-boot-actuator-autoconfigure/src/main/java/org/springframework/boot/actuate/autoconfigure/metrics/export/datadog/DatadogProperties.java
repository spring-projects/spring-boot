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
@ConfigurationProperties(prefix = "management.datadog.metrics.export")
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
	 * URI to ship metrics to. Set this if you need to publish metrics to a Datadog site
	 * other than US, or to an internal proxy en-route to Datadog.
	 */
	private String uri = "https://api.datadoghq.com";

	/**
     * Returns the API key.
     *
     * @return the API key
     */
    public String getApiKey() {
		return this.apiKey;
	}

	/**
     * Sets the API key for accessing the Datadog API.
     * 
     * @param apiKey the API key to be set
     */
    public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
     * Returns the application key.
     *
     * @return the application key
     */
    public String getApplicationKey() {
		return this.applicationKey;
	}

	/**
     * Sets the application key for the Datadog integration.
     * 
     * @param applicationKey the application key to be set
     */
    public void setApplicationKey(String applicationKey) {
		this.applicationKey = applicationKey;
	}

	/**
     * Returns the value indicating whether descriptions are enabled or not.
     * 
     * @return true if descriptions are enabled, false otherwise
     */
    public boolean isDescriptions() {
		return this.descriptions;
	}

	/**
     * Sets the flag indicating whether descriptions should be included.
     * 
     * @param descriptions the flag indicating whether descriptions should be included
     */
    public void setDescriptions(boolean descriptions) {
		this.descriptions = descriptions;
	}

	/**
     * Returns the host tag associated with the DatadogProperties object.
     * 
     * @return the host tag
     */
    public String getHostTag() {
		return this.hostTag;
	}

	/**
     * Sets the host tag for the DatadogProperties.
     * 
     * @param hostTag the host tag to be set
     */
    public void setHostTag(String hostTag) {
		this.hostTag = hostTag;
	}

	/**
     * Returns the URI of the DatadogProperties object.
     *
     * @return the URI of the DatadogProperties object
     */
    public String getUri() {
		return this.uri;
	}

	/**
     * Sets the URI for the DatadogProperties class.
     * 
     * @param uri the URI to be set
     */
    public void setUri(String uri) {
		this.uri = uri;
	}

}
