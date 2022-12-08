/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.unleash;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Unleash.
 *
 * @author Max Schwaab
 */
@Validated
@ConfigurationProperties(prefix = "spring.unleash")
class UnleashProperties {

	/**
	 * The name of the application as shown in the Unleash UI. Registered applications are listed on the Applications page.
	 */
	@NotBlank
	private String appName;

	/**
	 * The URL of the Unleash API.
	 */
	@NotBlank
	private String unleashApi;

	/**
	 * The api key to use for authenticating against the Unleash API.
	 */
	@NotBlank
	private String apiKey;

	/**
	 * A boolean indicating whether the client should disable sending usage metrics to the Unleash server.
	 */
	private boolean disableMetrics;

	/**
	 * A boolean indicating whether the client should poll the unleash api for updates to toggles.
	 */
	private boolean disablePolling;

	/**
	 * Enable support for using JVM properties for HTTP proxy authentication.
	 */
	private boolean enableProxyAuthenticationByJvmProperties;

	/**
	 * Whether the client should fetch toggle configuration synchronously (in a blocking manner).
	 */
	private boolean synchronousFetchOnInitialisation;

	/**
	 * The path to the file where local backups get stored.
	 */
	private String backUpFile;

	/**
	 * 	The name of the current environment.
	 */
	private String environment;

	/**
	 * A unique(-ish) identifier for your instance. Typically a hostname, pod id or something similar.
	 * Unleash uses this to separate metrics from the client SDKs with the same appName.
	 */
	private String instanceId;

	/**
	 * If provided, the client will only fetch toggles whose name starts with the provided value.
	 */
	private String namePrefix;

	/**
	 * If provided, the client will only fetch toggles from the specified project.
	 * (This can also be achieved with an API token).
	 */
	private String projectName;

	/**
	 * How often (in seconds) the client should check for toggle updates. Set to 0 if you want to only check once.
	 */
	private Long fetchTogglesInterval;

	/**
	 * Connect timeout for fetch toggles operation in seconds.
	 */
	private Long fetchTogglesConnectTimeoutSeconds;

	/**
	 * Read timeout for fetch toggles operation in seconds.
	 */
	private Long fetchTogglesReadTimeoutSeconds;

	/**
	 * How often (in seconds) the client should send metrics to the Unleash server.
	 * Ignored if you disable metrics with the disableMetrics method.
	 */
	private Long sendMetricsInterval;

	/**
	 * Connect timeout for send metrics operation in seconds.
	 */
	private Long sendMetricsConnectTimeoutSeconds;

	/**
	 * Read timeout for send metrics operation in seconds.
	 */
	private Long sendMetricsReadTimeoutSeconds;

	/**
	 * Add a custom HTTP header to the list of HTTP headers that will the client sends to the Unleash API.
	 * Each method call will add a new header.
	 * Note: in most cases, you'll need to use this method to provide an API token.<br>
	 * <br>
	 * Headers can be defined as a map in YAML.
	 */
	private Map<String, String> customHeaders = new HashMap<>();

	public String getAppName() {
		return appName;
	}

	public UnleashProperties setAppName(final String appName) {
		this.appName = appName;
		return this;
	}

	public String getUnleashApi() {
		return unleashApi;
	}

	public UnleashProperties setUnleashApi(final String unleashApi) {
		this.unleashApi = unleashApi;
		return this;
	}

	public String getApiKey() {
		return apiKey;
	}

	public UnleashProperties setApiKey(final String apiKey) {
		this.apiKey = apiKey;
		return this;
	}

	public boolean isDisableMetrics() {
		return disableMetrics;
	}

	public UnleashProperties setDisableMetrics(final boolean disableMetrics) {
		this.disableMetrics = disableMetrics;
		return this;
	}

	public boolean isDisablePolling() {
		return disablePolling;
	}

	public UnleashProperties setDisablePolling(final boolean disablePolling) {
		this.disablePolling = disablePolling;
		return this;
	}

	public boolean isEnableProxyAuthenticationByJvmProperties() {
		return enableProxyAuthenticationByJvmProperties;
	}

	public UnleashProperties setEnableProxyAuthenticationByJvmProperties(
			final boolean enableProxyAuthenticationByJvmProperties) {
		this.enableProxyAuthenticationByJvmProperties = enableProxyAuthenticationByJvmProperties;
		return this;
	}

	public boolean isSynchronousFetchOnInitialisation() {
		return synchronousFetchOnInitialisation;
	}

	public UnleashProperties setSynchronousFetchOnInitialisation(final boolean synchronousFetchOnInitialisation) {
		this.synchronousFetchOnInitialisation = synchronousFetchOnInitialisation;
		return this;
	}

	public String getBackUpFile() {
		return backUpFile;
	}

	public UnleashProperties setBackUpFile(final String backUpFile) {
		this.backUpFile = backUpFile;
		return this;
	}

	public String getEnvironment() {
		return environment;
	}

	public UnleashProperties setEnvironment(final String environment) {
		this.environment = environment;
		return this;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public UnleashProperties setInstanceId(final String instanceId) {
		this.instanceId = instanceId;
		return this;
	}

	public String getNamePrefix() {
		return namePrefix;
	}

	public UnleashProperties setNamePrefix(final String namePrefix) {
		this.namePrefix = namePrefix;
		return this;
	}

	public String getProjectName() {
		return projectName;
	}

	public UnleashProperties setProjectName(final String projectName) {
		this.projectName = projectName;
		return this;
	}

	public Long getFetchTogglesInterval() {
		return fetchTogglesInterval;
	}

	public UnleashProperties setFetchTogglesInterval(final Long fetchTogglesInterval) {
		this.fetchTogglesInterval = fetchTogglesInterval;
		return this;
	}

	public Long getFetchTogglesConnectTimeoutSeconds() {
		return fetchTogglesConnectTimeoutSeconds;
	}

	public UnleashProperties setFetchTogglesConnectTimeoutSeconds(final Long fetchTogglesConnectTimeoutSeconds) {
		this.fetchTogglesConnectTimeoutSeconds = fetchTogglesConnectTimeoutSeconds;
		return this;
	}

	public Long getFetchTogglesReadTimeoutSeconds() {
		return fetchTogglesReadTimeoutSeconds;
	}

	public UnleashProperties setFetchTogglesReadTimeoutSeconds(final Long fetchTogglesReadTimeoutSeconds) {
		this.fetchTogglesReadTimeoutSeconds = fetchTogglesReadTimeoutSeconds;
		return this;
	}

	public Long getSendMetricsInterval() {
		return sendMetricsInterval;
	}

	public UnleashProperties setSendMetricsInterval(final Long sendMetricsInterval) {
		this.sendMetricsInterval = sendMetricsInterval;
		return this;
	}

	public Long getSendMetricsConnectTimeoutSeconds() {
		return sendMetricsConnectTimeoutSeconds;
	}

	public UnleashProperties setSendMetricsConnectTimeoutSeconds(final Long sendMetricsConnectTimeoutSeconds) {
		this.sendMetricsConnectTimeoutSeconds = sendMetricsConnectTimeoutSeconds;
		return this;
	}

	public Long getSendMetricsReadTimeoutSeconds() {
		return sendMetricsReadTimeoutSeconds;
	}

	public UnleashProperties setSendMetricsReadTimeoutSeconds(final Long sendMetricsReadTimeoutSeconds) {
		this.sendMetricsReadTimeoutSeconds = sendMetricsReadTimeoutSeconds;
		return this;
	}

	public Map<String, String> getCustomHeaders() {
		return customHeaders;
	}

	public UnleashProperties setCustomHeaders(final Map<String, String> customHeaders) {
		this.customHeaders = customHeaders;
		return this;
	}

}
