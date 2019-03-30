/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import java.time.Duration;
import java.util.Arrays;

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
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Jetty-specific features common for both Servlet and Reactive servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.0.0
 */
public class JettyWebServerFactoryCustomizer implements
		WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public JettyWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		ServerProperties properties = this.serverProperties;
		ServerProperties.Jetty jettyProperties = properties.getJetty();
		factory.setUseForwardHeaders(
				getOrDeduceUseForwardHeaders(properties, this.environment));
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(jettyProperties::getAcceptors).whenNonNull()
				.to(factory::setAcceptors);
		propertyMapper.from(jettyProperties::getSelectors).whenNonNull()
				.to(factory::setSelectors);
		propertyMapper.from(properties::getMaxHttpHeaderSize).whenNonNull()
				.asInt(DataSize::toBytes).when(this::isPositive)
				.to((maxHttpHeaderSize) -> factory.addServerCustomizers(
						new MaxHttpHeaderSizeCustomizer(maxHttpHeaderSize)));
		propertyMapper.from(jettyProperties::getMaxHttpPostSize).asInt(DataSize::toBytes)
				.when(this::isPositive)
				.to((maxHttpPostSize) -> customizeMaxHttpPostSize(factory,
						maxHttpPostSize));
		propertyMapper.from(properties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeConnectionTimeout(factory,
						connectionTimeout));
		propertyMapper.from(jettyProperties::getAccesslog)
				.when(ServerProperties.Jetty.Accesslog::isEnabled)
				.to((accesslog) -> customizeAccessLog(factory, accesslog));
	}

	private boolean isPositive(Integer value) {
		return value > 0;
	}

	private boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

	private void customizeConnectionTimeout(ConfigurableJettyWebServerFactory factory,
			Duration connectionTimeout) {
		factory.addServerCustomizers((server) -> {
			for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
				if (connector instanceof AbstractConnector) {
					((AbstractConnector) connector)
							.setIdleTimeout(connectionTimeout.toMillis());
				}
			}
		});
	}

	private void customizeMaxHttpPostSize(ConfigurableJettyWebServerFactory factory,
			int maxHttpPostSize) {
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

	private void customizeAccessLog(ConfigurableJettyWebServerFactory factory,
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

	private static class MaxHttpHeaderSizeCustomizer implements JettyServerCustomizer {

		private final int maxHttpHeaderSize;

		MaxHttpHeaderSizeCustomizer(int maxHttpHeaderSize) {
			this.maxHttpHeaderSize = maxHttpHeaderSize;
		}

		@Override
		public void customize(Server server) {
			Arrays.stream(server.getConnectors()).forEach(this::customize);
		}

		private void customize(org.eclipse.jetty.server.Connector connector) {
			connector.getConnectionFactories().forEach(this::customize);
		}

		private void customize(ConnectionFactory factory) {
			if (factory instanceof HttpConfiguration.ConnectionFactory) {
				((HttpConfiguration.ConnectionFactory) factory).getHttpConfiguration()
						.setRequestHeaderSize(this.maxHttpHeaderSize);
			}
		}

	}

}
