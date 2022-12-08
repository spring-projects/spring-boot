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

import javax.annotation.Nullable;
import java.net.Proxy;

/**
 * A {@link UnleashConfigBuilderCustomizer} that applies non null
 * constructor args to a {@link UnleashConfig.Builder}.
 *
 * @author Max Schwaab
 */
class UnleashBeanConfigBuilderCustomizer implements UnleashConfigBuilderCustomizer {

  private final UnleashContextProvider contextProvider;
  private final Strategy fallbackStrategy;
  private final CustomHttpHeadersProvider httpHeadersProvider;
  private final Proxy proxy;
  private final UnleashScheduledExecutor scheduledExecutor;
  private final UnleashSubscriber subscriber;
  private final ToggleBootstrapProvider toggleBootstrapProvider;

  UnleashBeanConfigBuilderCustomizer(
      @Nullable final UnleashContextProvider contextProvider,
      @Nullable final Strategy fallbackStrategy,
      @Nullable final CustomHttpHeadersProvider httpHeadersProvider,
      @Nullable final Proxy proxy,
      @Nullable final UnleashScheduledExecutor scheduledExecutor,
      @Nullable final UnleashSubscriber subscriber,
      @Nullable final ToggleBootstrapProvider toggleBootstrapProvider) {
    this.contextProvider = contextProvider;
    this.fallbackStrategy = fallbackStrategy;
    this.httpHeadersProvider = httpHeadersProvider;
    this.proxy = proxy;
    this.scheduledExecutor = scheduledExecutor;
    this.subscriber = subscriber;
    this.toggleBootstrapProvider = toggleBootstrapProvider;
  }

  @Override
  public void customize(final UnleashConfig.Builder configBuilder) {
    if (fallbackStrategy != null) {
      configBuilder.fallbackStrategy(fallbackStrategy);
    }
    if (httpHeadersProvider != null) {
      configBuilder.customHttpHeadersProvider(httpHeadersProvider);
    }
    if (proxy != null) {
      configBuilder.proxy(proxy);
    }
    if (scheduledExecutor != null) {
      configBuilder.scheduledExecutor(scheduledExecutor);
    }
    if (subscriber != null) {
      configBuilder.subscriber(subscriber);
    }
    if (toggleBootstrapProvider != null) {
      configBuilder.toggleBootstrapProvider(toggleBootstrapProvider);
    }
    if (contextProvider != null) {
      configBuilder.unleashContextProvider(contextProvider);
    }
  }

}
