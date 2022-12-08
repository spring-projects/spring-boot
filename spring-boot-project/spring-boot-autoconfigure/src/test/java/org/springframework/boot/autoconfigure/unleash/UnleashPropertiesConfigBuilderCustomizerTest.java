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

import io.getunleash.util.UnleashConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link UnleashPropertiesConfigBuilderCustomizer}.
 *
 * @author Max Schwaab
 */
@DisplayName("UnleashPropertiesConfigBuilderCustomizer")
class UnleashPropertiesConfigBuilderCustomizerTest {

  private UnleashProperties properties;
  private UnleashConfigBuilderCustomizer customizer;
  private UnleashConfig.Builder configBuilder;

  @BeforeEach
  void setUp() {
    properties = new UnleashProperties()
         .setAppName("TestApp")
         .setApiUrl("http://unleash.com")
         .setApiClientSecret("c13n753cr37");
    customizer = new UnleashPropertiesConfigBuilderCustomizer(properties);
    configBuilder = UnleashConfig.builder();
  }

  @Test
  void shouldCustomizeAppName() {
    final String appName = "MyAppName";

    properties.setAppName(appName);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getAppName()).isEqualTo(appName);
  }

  @Test
  void shouldCustomizeApiUrl() {
    final String apiUrl = "http://my.unleash.com";

    properties.setApiUrl(apiUrl);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getUnleashAPI()).isEqualTo(URI.create(apiUrl));
  }

  @Test
  void shouldCustomizeApiClientSecret() {
    final String apiClientSecret = "MyApi";

    properties.setApiClientSecret(apiClientSecret);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getCustomHttpHeaders()).containsExactly(entry("Authorization", apiClientSecret));
  }

  @Test
  void shouldCustomizeDisableMetrcis() {
    properties.setDisableMetrics(true);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().isDisableMetrics()).isTrue();
  }

  @Test
  void shouldCustomizeEnableProxyAuthenticationByJvmProperties() {
    properties.setEnableProxyAuthenticationByJvmProperties(true);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().isProxyAuthenticationByJvmProperties()).isTrue();
  }

  @Test
  void shouldCustomizeSynchronousFetchOnInitialisation() {
    properties.setSynchronousFetchOnInitialisation(true);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().isSynchronousFetchOnInitialisation()).isTrue();
  }

  @Test
  void shouldCustomizeBackupFile() {
    final String backupFile = "myBackupFile";

    properties.setBackUpFile(backupFile);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getBackupFile()).isEqualTo(backupFile);
  }

  @Test
  void shouldCustomizeEnvironement() {
    final String environment = "myEnv";

    properties.setEnvironment(environment);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getEnvironment()).isEqualTo(environment);
  }

  @Test
  void shouldCustomizeInstanceId() {
    final String instanceId = "myInstance";

    properties.setInstanceId(instanceId);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getInstanceId()).isEqualTo(instanceId);
  }

  @Test
  void shouldCustomizeNamePrefix() {
    final String namePrefix = "myPrefix";

    properties.setNamePrefix(namePrefix);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getNamePrefix()).isEqualTo(namePrefix);
  }

  @Test
  void shouldCustomizeProjectName() {
    final String projectName = "myProjectName";

    properties.setProjectName(projectName);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getProjectName()).isEqualTo(projectName);
  }

  @Test
  void shouldCustomizeFetchTogglesInterval() {
    final long fetchTogglesInterval = 100;

    properties.setFetchTogglesInterval(fetchTogglesInterval);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getFetchTogglesInterval()).isEqualTo(fetchTogglesInterval);
  }

  @Test
  void shouldCustomizeSendMetricsInterval() {
    final long sendMetricsInterval = 100;

    properties.setSendMetricsInterval(sendMetricsInterval);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getSendMetricsInterval()).isEqualTo(sendMetricsInterval);
  }

  @Test
  void shouldCustomizeCustomHeaders() {
    final Map<String, String> customHeaders = Map.of(
        "X-Wayfair", "WFN",
        "Accept", "application/json"
    );

    properties.setCustomHeaders(customHeaders);
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getCustomHttpHeaders()).contains(
        entry("X-Wayfair", "WFN"),
        entry("Accept", "application/json")
    );
  }

}