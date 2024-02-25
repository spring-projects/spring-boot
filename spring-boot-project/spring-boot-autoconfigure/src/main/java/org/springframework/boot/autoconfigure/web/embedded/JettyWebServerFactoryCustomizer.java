/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.List;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Jetty-specific features common for both Servlet and Reactive servers.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @author HaiTao Zhang
 * @author Rafiullah Hamedy
 * @author Florian Storz
 * @author Michael Weidmann
 * @since 2.0.0
 */
public class JettyWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory>, Ordered {

	static final int ORDER = 0;

	private final Environment environment;

	private final ServerProperties serverProperties;

	/**
	 * Constructs a new JettyWebServerFactoryCustomizer with the specified environment and
	 * server properties.
	 * @param environment the environment used for configuration
	 * @param serverProperties the server properties used for configuration
	 */
	public JettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	/**
	 * Returns the order value for this customizer.
	 * @return the order value
	 */
	@Override
	public int getOrder() {
		return ORDER;
	}

	/**
	 * Customize the Jetty web server factory.
	 * @param factory the configurable Jetty web server factory
	 */
	@Override
	public void customize(ConfigurableJettyWebServerFactory factory) {
		ServerProperties.Jetty properties = this.serverProperties.getJetty();
		factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());
		ServerProperties.Jetty.Threads threadProperties = properties.getThreads();
		factory.setThreadPool(JettyThreadPool.create(properties.getThreads()));
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getMaxConnections).to(factory::setMaxConnections);
		map.from(threadProperties::getAcceptors).to(factory::setAcceptors);
		map.from(threadProperties::getSelectors).to(factory::setSelectors);
		map.from(this.serverProperties::getMaxHttpRequestHeaderSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to((maxHttpRequestHeaderSize) -> factory
				.addServerCustomizers(new MaxHttpRequestHeaderSizeCustomizer(maxHttpRequestHeaderSize)));
		map.from(properties::getMaxHttpResponseHeaderSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to((maxHttpResponseHeaderSize) -> factory
				.addServerCustomizers(new MaxHttpResponseHeaderSizeCustomizer(maxHttpResponseHeaderSize)));
		map.from(properties::getMaxHttpFormPostSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to((maxHttpFormPostSize) -> customizeMaxHttpFormPostSize(factory, maxHttpFormPostSize));
		map.from(properties::getConnectionIdleTimeout).to((idleTimeout) -> customizeIdleTimeout(factory, idleTimeout));
		map.from(properties::getAccesslog)
			.when(ServerProperties.Jetty.Accesslog::isEnabled)
			.to((accesslog) -> customizeAccessLog(factory, accesslog));
	}

	/**
	 * Checks if the given value is positive.
	 * @param value the value to be checked
	 * @return true if the value is positive, false otherwise
	 */
	private boolean isPositive(Integer value) {
		return value > 0;
	}

	/**
	 * Returns a boolean value indicating whether to use forward headers or deduce it
	 * based on the active cloud platform.
	 * @return {@code true} if forward headers should be used, {@code false} otherwise
	 */
	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	/**
	 * Customizes the idle timeout for the Jetty web server factory.
	 * @param factory the configurable Jetty web server factory
	 * @param connectionTimeout the duration of the idle timeout
	 */
	private void customizeIdleTimeout(ConfigurableJettyWebServerFactory factory, Duration connectionTimeout) {
		factory.addServerCustomizers((server) -> {
			for (org.eclipse.jetty.server.Connector connector : server.getConnectors()) {
				if (connector instanceof AbstractConnector abstractConnector) {
					abstractConnector.setIdleTimeout(connectionTimeout.toMillis());
				}
			}
		});
	}

	/**
	 * Customizes the maximum HTTP form post size for the given Jetty web server factory.
	 * @param factory The configurable Jetty web server factory to customize.
	 * @param maxHttpFormPostSize The maximum HTTP form post size to set.
	 */
	private void customizeMaxHttpFormPostSize(ConfigurableJettyWebServerFactory factory, int maxHttpFormPostSize) {
		factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				setHandlerMaxHttpFormPostSize(server.getHandlers());
			}

			private void setHandlerMaxHttpFormPostSize(List<Handler> handlers) {
				for (Handler handler : handlers) {
					setHandlerMaxHttpFormPostSize(handler);
				}
			}

			private void setHandlerMaxHttpFormPostSize(Handler handler) {
				if (handler instanceof ServletContextHandler contextHandler) {
					contextHandler.setMaxFormContentSize(maxHttpFormPostSize);
				}
				else if (handler instanceof Handler.Wrapper wrapper) {
					setHandlerMaxHttpFormPostSize(wrapper.getHandler());
				}
				else if (handler instanceof Handler.Collection collection) {
					setHandlerMaxHttpFormPostSize(collection.getHandlers());
				}
			}

		});
	}

	/**
	 * Customizes the access log configuration for the Jetty web server factory.
	 * @param factory the configurable Jetty web server factory
	 * @param properties the access log properties
	 */
	private void customizeAccessLog(ConfigurableJettyWebServerFactory factory,
			ServerProperties.Jetty.Accesslog properties) {
		factory.addServerCustomizers((server) -> {
			RequestLogWriter logWriter = new RequestLogWriter();
			String format = getLogFormat(properties);
			CustomRequestLog log = new CustomRequestLog(logWriter, format);
			if (!CollectionUtils.isEmpty(properties.getIgnorePaths())) {
				log.setIgnorePaths(properties.getIgnorePaths().toArray(new String[0]));
			}
			if (properties.getFilename() != null) {
				logWriter.setFilename(properties.getFilename());
			}
			if (properties.getFileDateFormat() != null) {
				logWriter.setFilenameDateFormat(properties.getFileDateFormat());
			}
			logWriter.setRetainDays(properties.getRetentionPeriod());
			logWriter.setAppend(properties.isAppend());
			server.setRequestLog(log);
		});
	}

	/**
	 * Returns the log format for the Jetty access log based on the provided properties.
	 * If a custom format is specified in the properties, it will be returned. If the
	 * format is set to EXTENDED_NCSA, the extended NCSA format will be returned.
	 * Otherwise, the default NCSA format will be returned.
	 * @param properties The Jetty access log properties.
	 * @return The log format for the Jetty access log.
	 */
	private String getLogFormat(ServerProperties.Jetty.Accesslog properties) {
		if (properties.getCustomFormat() != null) {
			return properties.getCustomFormat();
		}
		else if (ServerProperties.Jetty.Accesslog.FORMAT.EXTENDED_NCSA.equals(properties.getFormat())) {
			return CustomRequestLog.EXTENDED_NCSA_FORMAT;
		}
		return CustomRequestLog.NCSA_FORMAT;
	}

	/**
	 * MaxHttpRequestHeaderSizeCustomizer class.
	 */
	private static class MaxHttpRequestHeaderSizeCustomizer implements JettyServerCustomizer {

		private final int maxRequestHeaderSize;

		/**
		 * Constructs a new MaxHttpRequestHeaderSizeCustomizer with the specified maximum
		 * request header size.
		 * @param maxRequestHeaderSize the maximum size of the request header
		 */
		MaxHttpRequestHeaderSizeCustomizer(int maxRequestHeaderSize) {
			this.maxRequestHeaderSize = maxRequestHeaderSize;
		}

		/**
		 * Customizes the server by setting the maximum request header size for each
		 * connector.
		 * @param server the server to be customized
		 */
		@Override
		public void customize(Server server) {
			Arrays.stream(server.getConnectors()).forEach(this::customize);
		}

		/**
		 * Customizes the given Jetty server connector by customizing each connection
		 * factory.
		 * @param connector the Jetty server connector to be customized
		 */
		private void customize(org.eclipse.jetty.server.Connector connector) {
			connector.getConnectionFactories().forEach(this::customize);
		}

		/**
		 * Customizes the given ConnectionFactory by setting the maximum request header
		 * size.
		 * @param factory the ConnectionFactory to be customized
		 */
		private void customize(ConnectionFactory factory) {
			if (factory instanceof HttpConfiguration.ConnectionFactory) {
				((HttpConfiguration.ConnectionFactory) factory).getHttpConfiguration()
					.setRequestHeaderSize(this.maxRequestHeaderSize);
			}
		}

	}

	/**
	 * MaxHttpResponseHeaderSizeCustomizer class.
	 */
	private static class MaxHttpResponseHeaderSizeCustomizer implements JettyServerCustomizer {

		private final int maxResponseHeaderSize;

		/**
		 * Constructs a new MaxHttpResponseHeaderSizeCustomizer with the specified maximum
		 * response header size.
		 * @param maxResponseHeaderSize the maximum size of the response header
		 */
		MaxHttpResponseHeaderSizeCustomizer(int maxResponseHeaderSize) {
			this.maxResponseHeaderSize = maxResponseHeaderSize;
		}

		/**
		 * Customizes the server by setting the maximum response header size for each
		 * connector.
		 * @param server the server to be customized
		 */
		@Override
		public void customize(Server server) {
			Arrays.stream(server.getConnectors()).forEach(this::customize);
		}

		/**
		 * Customizes the given Jetty server connector by customizing each connection
		 * factory.
		 * @param connector the Jetty server connector to be customized
		 */
		private void customize(org.eclipse.jetty.server.Connector connector) {
			connector.getConnectionFactories().forEach(this::customize);
		}

		/**
		 * Customizes the given ConnectionFactory by setting the maximum response header
		 * size.
		 * @param factory the ConnectionFactory to customize
		 */
		private void customize(ConnectionFactory factory) {
			if (factory instanceof HttpConfiguration.ConnectionFactory httpConnectionFactory) {
				httpConnectionFactory.getHttpConfiguration().setResponseHeaderSize(this.maxResponseHeaderSize);
			}
		}

	}

}
