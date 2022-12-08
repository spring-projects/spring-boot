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
import io.getunleash.UnleashContextProvider;
import io.getunleash.event.UnleashSubscriber;
import io.getunleash.repository.ToggleBootstrapProvider;
import io.getunleash.strategy.Strategy;
import io.getunleash.util.UnleashConfig;
import io.getunleash.util.UnleashScheduledExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.Proxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UnleashBeanConfigBuilderCustomizer}.
 *
 * @author Max Schwaab
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnleashBeanConfigBuilderCustomizer")
class UnleashBeanConfigBuilderCustomizerTest {

  @Mock
  private UnleashContextProvider contextProviderMock;
  @Mock
  private Strategy fallbackStrategyMock;
  @Mock
  private CustomHttpHeadersProvider httpHeadersProviderMock;
  @Mock
  private Proxy proxyMock;
  @Mock
  private UnleashScheduledExecutor scheduledExecutorMock;
  @Mock
  private UnleashSubscriber subscriberMock;
  @Mock
  private ToggleBootstrapProvider toggleBootstrapProviderMock;

  private UnleashBeanConfigBuilderCustomizer customizer;
  private UnleashConfig.Builder configBuilder;

  @BeforeEach
  void setUp() {
    customizer = new UnleashBeanConfigBuilderCustomizer(
        contextProviderMock,
        fallbackStrategyMock,
        httpHeadersProviderMock,
        proxyMock,
        scheduledExecutorMock,
        subscriberMock,
        toggleBootstrapProviderMock
    );
    configBuilder = UnleashConfig.builder()
        .appName("TestApp")
        .unleashAPI("http://unleash.com")
        .apiKey("c13n753cr37");
  }

  @Test
  void shouldCustomizeContextProvider() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getContextProvider()).isEqualTo(contextProviderMock);
  }

  @Test
  void shouldCustomizeFallbackStrategy() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getFallbackStrategy()).isEqualTo(fallbackStrategyMock);
  }

  @Test
  void shouldCustomizeHttpHeadersProvider() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getCustomHttpHeadersProvider()).isEqualTo(httpHeadersProviderMock);
  }

  @Test
  void shouldCustomizeProxy() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getProxy()).isEqualTo(proxyMock);
  }

  @Test
  void shouldCustomizeSchedulerExecutor() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getScheduledExecutor()).isEqualTo(scheduledExecutorMock);
  }

  @Test
  void shouldCustomizeSubscriber() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getSubscriber()).isEqualTo(subscriberMock);
  }

  @Test
  void shouldCustomizeToggleBootstrapProvider() {
    customizer.customize(configBuilder);

    assertThat(configBuilder.build().getToggleBootstrapProvider()).isEqualTo(toggleBootstrapProviderMock);
  }

}