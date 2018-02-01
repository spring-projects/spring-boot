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

package org.springframework.boot.autoconfigure.web.embedded.jetty;

import java.time.Duration;

import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.core.env.Environment;

/**
 * Customization for Jetty-specific features common for both Servlet and Reactive servers.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public final class JettyCustomizer {

	private JettyCustomizer() {
	}

	public static void customizeJetty(ServerProperties serverProperties,
			Environment environment, ConfigurableJettyWebServerFactory factory) {
		ServerProperties.Jetty jettyProperties = serverProperties.getJetty();
		factory.setUseForwardHeaders(
				getOrDeduceUseForwardHeaders(serverProperties, environment));
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(jettyProperties::getAcceptors).whenNonNull()
				.to(factory::setAcceptors);
		propertyMapper.from(jettyProperties::getSelectors).whenNonNull()
				.to(factory::setSelectors);
		propertyMapper.from(serverProperties::getMaxHttpHeaderSize)
				.when(JettyCustomizer::isPositive)
				.to((maxHttpHeaderSize) -> customizeMaxHttpHeaderSize(factory,
						maxHttpHeaderSize));
		propertyMapper.from(jettyProperties::getMaxHttpPostSize)
				.when(JettyCustomizer::isPositive)
				.to((maxHttpPostSize) -> customizeMaxHttpPostSize(factory,
						maxHttpPostSize));
		propertyMapper.from(serverProperties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeConnectionTimeout(factory,
						connectionTimeout));
		propertyMapper.from(jettyProperties::getAccesslog)
				.when(ServerProperties.Jetty.Accesslog::isEnabled)
				.to((accesslog) -> customizeAccessLog(factory, accesslog));
	}

	private static boolean isPositive(Integer value) {
		return value > 0;
	}

	private static boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

	private static void customizeConnectionTimeout(
			ConfigurableJettyWebServerFactory factory, Duration connectionTimeout) {
		factory.addServerCustomizers((server) -> {
			for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
				if (connector instanceof AbstractConnector) {
					((AbstractConnector) connector)
							.setIdleTimeout(connectionTimeout.toMillis());
				}
			}
		});
	}

	private static void customizeMaxHttpHeaderSize(
			ConfigurableJettyWebServerFactory factory, int maxHttpHeaderSize) {
		factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				for (org.eclipse.jetty.server.Connector connector : server
						.getConnectors()) {
					for (ConnectionFactory connectionFactory : connector
							.getConnectionFactories()) {
						if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
							customize(
									(HttpConfiguration.ConnectionFactory) connectionFactory);
						}
					}
				}
			}

			private void customize(HttpConfiguration.ConnectionFactory factory) {
				HttpConfiguration configuration = factory.getHttpConfiguration();
				configuration.setRequestHeaderSize(maxHttpHeaderSize);
				configuration.setResponseHeaderSize(maxHttpHeaderSize);
			}

		});
	}

	private static void customizeMaxHttpPostSize(
			ConfigurableJettyWebServerFactory factory, int maxHttpPostSize) {
		factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				setHandlerMaxHttpPostSize(maxHttpPostSize, server.getHandlers());
			}

			private void setHandlerMaxHttpPostSize(int maxHttpPostSize,
					Handler... handlers) {
				for (Handler handler : handlers) {
					if (handler instanceof ContextHandler) {
						((ContextHandler) handler).setMaxFormContentSize(maxHttpPostSize);
					}
					else if (handler instanceof HandlerWrapper) {
						setHandlerMaxHttpPostSize(maxHttpPostSize,
								((HandlerWrapper) handler).getHandler());
					}
					else if (handler instanceof HandlerCollection) {
						setHandlerMaxHttpPostSize(maxHttpPostSize,
								((HandlerCollection) handler).getHandlers());
					}
				}
			}

		});
	}

	private static void customizeAccessLog(ConfigurableJettyWebServerFactory factory,
			ServerProperties.Jetty.Accesslog properties) {
		factory.addServerCustomizers((server) -> {
			NCSARequestLog log = new NCSARequestLog();
			if (properties.getFilename() != null) {
				log.setFilename(properties.getFilename());
			}
			if (properties.getFileDateFormat() != null) {
				log.setFilenameDateFormat(properties.getFileDateFormat());
			}
			log.setRetainDays(properties.getRetentionPeriod());
			log.setAppend(properties.isAppend());
			log.setExtended(properties.isExtendedFormat());
			if (properties.getDateFormat() != null) {
				log.setLogDateFormat(properties.getDateFormat());
			}
			if (properties.getLocale() != null) {
				log.setLogLocale(properties.getLocale());
			}
			if (properties.getTimeZone() != null) {
				log.setLogTimeZone(properties.getTimeZone().getID());
			}
			log.setLogCookies(properties.isLogCookies());
			log.setLogServer(properties.isLogServer());
			log.setLogLatency(properties.isLogLatency());
			server.setRequestLog(log);
		});
	}

}
