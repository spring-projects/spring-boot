/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.RequestLogWriter;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
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

	public JettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

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
			.to(customizeHttpConfigurations(factory, HttpConfiguration::setRequestHeaderSize));
		map.from(properties::getMaxHttpResponseHeaderSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to(customizeHttpConfigurations(factory, HttpConfiguration::setResponseHeaderSize));
		map.from(properties::getMaxHttpFormPostSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to(customizeServletContextHandler(factory, ServletContextHandler::setMaxFormContentSize));
		map.from(properties::getMaxFormKeys)
			.when(this::isPositive)
			.to(customizeServletContextHandler(factory, ServletContextHandler::setMaxFormKeys));
		map.from(properties::getConnectionIdleTimeout)
			.as(Duration::toMillis)
			.to(customizeAbstractConnectors(factory, AbstractConnector::setIdleTimeout));
		map.from(properties::getAccesslog)
			.when(ServerProperties.Jetty.Accesslog::isEnabled)
			.to((accesslog) -> customizeAccessLog(factory, accesslog));
	}

	private boolean isPositive(Integer value) {
		return value > 0;
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

	private <T> Consumer<T> customizeHttpConfigurations(ConfigurableJettyWebServerFactory factory,
			BiConsumer<HttpConfiguration, T> action) {
		return customizeConnectionFactories(factory, HttpConfiguration.ConnectionFactory.class,
				(connectionFactory, value) -> action.accept(connectionFactory.getHttpConfiguration(), value));
	}

	private <V, F> Consumer<V> customizeConnectionFactories(ConfigurableJettyWebServerFactory factory,
			Class<F> connectionFactoryType, BiConsumer<F, V> action) {
		return customizeConnectors(factory, Connector.class, (connector, value) -> {
			Stream<ConnectionFactory> connectionFactories = connector.getConnectionFactories().stream();
			forEach(connectionFactories, connectionFactoryType, action, value);
		});
	}

	private <V> Consumer<V> customizeAbstractConnectors(ConfigurableJettyWebServerFactory factory,
			BiConsumer<AbstractConnector, V> action) {
		return customizeConnectors(factory, AbstractConnector.class, action);
	}

	private <V, C> Consumer<V> customizeConnectors(ConfigurableJettyWebServerFactory factory, Class<C> connectorType,
			BiConsumer<C, V> action) {
		return (value) -> factory.addServerCustomizers((server) -> {
			Stream<Connector> connectors = Arrays.stream(server.getConnectors());
			forEach(connectors, connectorType, action, value);
		});
	}

	private <V> Consumer<V> customizeServletContextHandler(ConfigurableJettyWebServerFactory factory,
			BiConsumer<ServletContextHandler, V> action) {
		return customizeHandlers(factory, ServletContextHandler.class, action);
	}

	private <V, H> Consumer<V> customizeHandlers(ConfigurableJettyWebServerFactory factory, Class<H> handlerType,
			BiConsumer<H, V> action) {
		return (value) -> factory.addServerCustomizers((server) -> {
			List<Handler> handlers = server.getHandlers();
			forEachHandler(handlers, handlerType, action, value);
		});
	}

	@SuppressWarnings("unchecked")
	private <V, H> void forEachHandler(List<Handler> handlers, Class<H> handlerType, BiConsumer<H, V> action, V value) {
		for (Handler handler : handlers) {
			if (handlerType.isInstance(handler)) {
				action.accept((H) handler, value);
			}
			if (handler instanceof Handler.Wrapper wrapper) {
				forEachHandler(wrapper.getHandlers(), handlerType, action, value);
			}
			if (handler instanceof Handler.Collection collection) {
				forEachHandler(collection.getHandlers(), handlerType, action, value);
			}
		}
	}

	private <T, V> void forEach(Stream<?> elements, Class<T> type, BiConsumer<T, V> action, V value) {
		elements.filter(type::isInstance).map(type::cast).forEach((element) -> action.accept(element, value));
	}

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

	private String getLogFormat(ServerProperties.Jetty.Accesslog properties) {
		if (properties.getCustomFormat() != null) {
			return properties.getCustomFormat();
		}
		if (ServerProperties.Jetty.Accesslog.FORMAT.EXTENDED_NCSA.equals(properties.getFormat())) {
			return CustomRequestLog.EXTENDED_NCSA_FORMAT;
		}
		return CustomRequestLog.NCSA_FORMAT;
	}

}
