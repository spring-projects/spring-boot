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
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link UnleashConfigBuilderCustomizer} that applies properties from a
 * {@link UnleashProperties} to a {@link UnleashConfig.Builder}.
 *
 * @author Max Schwaab
 */
class UnleashPropertiesConfigBuilderCustomizer implements UnleashConfigBuilderCustomizer {

  private final UnleashProperties properties;

  UnleashPropertiesConfigBuilderCustomizer(final UnleashProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(final UnleashConfig.Builder configBuilder) {
    configBuilder.appName(properties.getAppName())
        .unleashAPI(properties.getApiUrl())
        .apiKey(properties.getApiClientSecret())
        .namePrefix(properties.getNamePrefix())
        .projectName(properties.getProjectName())
        .synchronousFetchOnInitialisation(properties.isSynchronousFetchOnInitialisation());

    if (properties.isDisableMetrics()) {
      configBuilder.disableMetrics();
    }
    if (properties.isEnableProxyAuthenticationByJvmProperties()) {
      configBuilder.enableProxyAuthenticationByJvmProperties();
    }
    if (StringUtils.isNotBlank(properties.getEnvironment())) {
      configBuilder.environment(properties.getEnvironment());
    }
    if (StringUtils.isNotBlank(properties.getInstanceId())) {
      configBuilder.instanceId(properties.getInstanceId());
    }
    if (StringUtils.isNotBlank(properties.getBackUpFile())) {
      configBuilder.backupFile(properties.getBackUpFile());
    }
    if (properties.getFetchTogglesInterval() != null) {
      configBuilder.fetchTogglesInterval(properties.getFetchTogglesInterval());
    }
    if (properties.getSendMetricsInterval() != null) {
      configBuilder.sendMetricsInterval(properties.getSendMetricsInterval());
    }

    properties.getCustomHeaders().forEach(configBuilder::customHttpHeader);
  }

}
