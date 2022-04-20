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

package org.springframework.boot.actuate.autoconfigure.metrics.web.jetty;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics;
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics;
import io.micrometer.core.instrument.binder.jetty.JettySslHandshakeMetrics;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.jetty.JettyConnectionMetricsBinder;
import org.springframework.boot.actuate.metrics.web.jetty.JettyServerThreadPoolMetricsBinder;
import org.springframework.boot.actuate.metrics.web.jetty.JettySslHandshakeMetricsBinder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Jetty metrics.
 *
 * @author Andy Wilkinson
 * @author Chris Bono
 * @since 2.1.0
 */
@AutoConfiguration(after = CompositeMeterRegistryAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnClass({ JettyServerThreadPoolMetrics.class, Server.class })
@ConditionalOnBean(MeterRegistry.class)
public class JettyMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ JettyServerThreadPoolMetrics.class, JettyServerThreadPoolMetricsBinder.class })
	public JettyServerThreadPoolMetricsBinder jettyServerThreadPoolMetricsBinder(MeterRegistry meterRegistry) {
		return new JettyServerThreadPoolMetricsBinder(meterRegistry);
	}

	@Bean
	@ConditionalOnMissingBean({ JettyConnectionMetrics.class, JettyConnectionMetricsBinder.class })
	public JettyConnectionMetricsBinder jettyConnectionMetricsBinder(MeterRegistry meterRegistry) {
		return new JettyConnectionMetricsBinder(meterRegistry);
	}

	@Bean
	@ConditionalOnMissingBean({ JettySslHandshakeMetrics.class, JettySslHandshakeMetricsBinder.class })
	@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
	public JettySslHandshakeMetricsBinder jettySslHandshakeMetricsBinder(MeterRegistry meterRegistry) {
		return new JettySslHandshakeMetricsBinder(meterRegistry);
	}

}
