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

import io.getunleash.CustomHttpHeadersProvider;
import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.UnleashContextProvider;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.repository.ToggleBootstrapProvider;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import io.getunleash.util.UnleashScheduledExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.net.Proxy;
import java.util.List;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Unleash.<br>
 * For available configuration options see the
 * <a href="https://github.com/Unleash/unleash-client-java#configuration-options">Unleash Java client documentation</a>.
 *
 * @author Max Schwaab
 */
@AutoConfiguration
@Conditional(UnleashPropertiesOrUnleashConfig.class)
class UnleashAutoConfiguration {

  @AutoConfiguration
  @ConditionalOnUnleashRequiredProperties
  @EnableConfigurationProperties(UnleashProperties.class)
  static class EnableUnleashProperties {

    @Bean
    UnleashConfigBuilderCustomizer propertiesConfigBuilderCustomizer(final UnleashProperties properties) {
      return new UnleashPropertiesConfigBuilderCustomizer(properties);
    }

  }

  @Bean
  @ConditionalOnMissingBean({ Unleash.class, UnleashConfig.class })
  UnleashConfig unleashConfig(final List<UnleashConfigBuilderCustomizer> configBuilderCustomizers) {
    final UnleashConfig.Builder configBuilder = UnleashConfig.builder();

    configBuilderCustomizers.forEach(customizer -> customizer.customize(configBuilder));

    return configBuilder.build();
  }

  @Bean
  @ConditionalOnMissingBean(Unleash.class)
  Unleash unleash(final UnleashConfig unleashConfig) {
    return new DefaultUnleash(unleashConfig);
  }

  @Bean
  UnleashConfigBuilderCustomizer beanConfigBuilderCustomizer(
      @Autowired(required = false)
      final UnleashContextProvider contextProvider,
      @Autowired(required = false)
      final Strategy fallbackStrategy,
      @Autowired(required = false)
      final CustomHttpHeadersProvider httpHeadersProvider,
      @Autowired(required = false)
      final Proxy proxy,
      @Autowired(required = false)
      final UnleashScheduledExecutor scheduledExecutor,
      @Autowired(required = false)
      final UnleashSubscriber subscriber,
      @Autowired(required = false)
      final ToggleBootstrapProvider toggleBootstrapProvider) {
    return new UnleashBeanConfigBuilderCustomizer(
        contextProvider,
        fallbackStrategy,
        httpHeadersProvider,
        proxy,
        scheduledExecutor,
        subscriber,
        toggleBootstrapProvider
    );
  }

}
