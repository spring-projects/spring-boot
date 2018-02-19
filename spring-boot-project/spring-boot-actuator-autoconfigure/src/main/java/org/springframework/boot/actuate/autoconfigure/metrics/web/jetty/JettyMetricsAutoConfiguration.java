/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.metrics.web.jetty;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.core.instrument.binder.jetty.JettyStatisticsMetrics;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandlerContainer;
import org.eclipse.jetty.server.handler.StatisticsHandler;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JettyStatisticsMetrics}.
 *
 * @author Even Holthe
 * @since 2.1.0
 */
@ConditionalOnWebApplication
@ConditionalOnClass({ JettyStatisticsMetrics.class, Server.class })
public class JettyMetricsAutoConfiguration {
	private volatile StatisticsHandler statsHandler;

	@Bean
	@ConditionalOnMissingBean(JettyStatisticsMetrics.class)
	public JettyStatisticsMetrics jettyMetrics() {
		return new JettyStatisticsMetrics(this.statsHandler, Collections.emptyList());
	}

	@Bean
	@ConditionalOnWebApplication(type = Type.SERVLET)
	public WebServerFactoryCustomizer<JettyServletWebServerFactory> serverCapturingServletJettyCustomizer() {
		return this::statisticsCustomizer;
	}

	@Bean
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	public WebServerFactoryCustomizer<JettyReactiveWebServerFactory> serverCapturingReactiveJettyCustomizer() {
		return this::statisticsCustomizer;
	}

	private void extractExistingOrCreateStatisticsHandler(Server server) {
		final Handler existingHandler = server.getHandler();
		StatisticsHandler activeStatsHandler = null;

		if (existingHandler instanceof StatisticsHandler) {
			activeStatsHandler = (StatisticsHandler) existingHandler;
		}

		else if (existingHandler instanceof AbstractHandlerContainer) {
			AbstractHandlerContainer abstractHandler = (AbstractHandlerContainer) existingHandler;
			Handler[] existingHandlers = abstractHandler.getChildHandlers();

			final List<StatisticsHandler> existingStatsHandlers = Stream.of(existingHandlers)
					.filter(StatisticsHandler.class::isInstance)
					.map(StatisticsHandler.class::cast)
					.collect(Collectors.toList());

			if (!existingStatsHandlers.isEmpty()) {
				activeStatsHandler = existingStatsHandlers.get(0);
			}
		}

		if (activeStatsHandler == null) {
			activeStatsHandler = new StatisticsHandler();
			activeStatsHandler.setHandler(existingHandler);
			server.setHandler(activeStatsHandler);
		}

		this.statsHandler = activeStatsHandler;
	}

	private void statisticsCustomizer(ConfigurableJettyWebServerFactory jettyFactory) {
		jettyFactory.addServerCustomizers(this::extractExistingOrCreateStatisticsHandler);
	}
}
