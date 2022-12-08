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

  @NotBlank
  private String appName;
  @NotBlank
  private String apiUrl;
  @NotBlank
  private String apiClientSecret;
  private boolean disableMetrics;
  private boolean enableProxyAuthenticationByJvmProperties;
  private boolean synchronousFetchOnInitialisation;
  private String backUpFile;
  private String environment;
  private String instanceId;
  private String namePrefix;
  private String projectName;
  private Long fetchTogglesInterval;
  private Long sendMetricsInterval;
  private Map<String, String> customHeaders = new HashMap<>();

  public String getAppName() {
    return appName;
  }

  public UnleashProperties setAppName(final String appName) {
    this.appName = appName;
    return this;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public UnleashProperties setApiUrl(final String apiUrl) {
    this.apiUrl = apiUrl;
    return this;
  }

  public String getApiClientSecret() {
    return apiClientSecret;
  }

  public UnleashProperties setApiClientSecret(final String apiClientSecret) {
    this.apiClientSecret = apiClientSecret;
    return this;
  }

  public boolean isDisableMetrics() {
    return disableMetrics;
  }

  public UnleashProperties setDisableMetrics(final boolean disableMetrics) {
    this.disableMetrics = disableMetrics;
    return this;
  }

  public boolean isEnableProxyAuthenticationByJvmProperties() {
    return enableProxyAuthenticationByJvmProperties;
  }

  public UnleashProperties setEnableProxyAuthenticationByJvmProperties(final boolean enableProxyAuthenticationByJvmProperties) {
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

  public Long getSendMetricsInterval() {
    return sendMetricsInterval;
  }

  public UnleashProperties setSendMetricsInterval(final Long sendMetricsInterval) {
    this.sendMetricsInterval = sendMetricsInterval;
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
