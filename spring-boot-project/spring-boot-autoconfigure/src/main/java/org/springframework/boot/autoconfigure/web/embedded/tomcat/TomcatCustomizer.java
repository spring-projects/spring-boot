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

package org.springframework.boot.autoconfigure.web.embedded.tomcat;

import java.time.Duration;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Customization for Tomcat-specific features common
 * for both Servlet and Reactive servers.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public final class TomcatCustomizer {

	private TomcatCustomizer() {
	}

	public static void customizeTomcat(ServerProperties serverProperties,
			Environment environment, ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat tomcatProperties = serverProperties.getTomcat();
		if (tomcatProperties.getBasedir() != null) {
			factory.setBaseDirectory(tomcatProperties.getBasedir());
		}
		if (tomcatProperties.getBackgroundProcessorDelay() != null) {
			factory.setBackgroundProcessorDelay((int) tomcatProperties
					.getBackgroundProcessorDelay().getSeconds());
		}
		customizeRemoteIpValve(serverProperties, environment, factory);
		if (tomcatProperties.getMaxThreads() > 0) {
			customizeMaxThreads(factory, tomcatProperties.getMaxThreads());
		}
		if (tomcatProperties.getMinSpareThreads() > 0) {
			customizeMinThreads(factory, tomcatProperties.getMinSpareThreads());
		}
		int maxHttpHeaderSize = (serverProperties.getMaxHttpHeaderSize() > 0
				? serverProperties.getMaxHttpHeaderSize()
				: tomcatProperties.getMaxHttpHeaderSize());
		if (maxHttpHeaderSize > 0) {
			customizeMaxHttpHeaderSize(factory, maxHttpHeaderSize);
		}
		if (tomcatProperties.getMaxHttpPostSize() != 0) {
			customizeMaxHttpPostSize(factory, tomcatProperties.getMaxHttpPostSize());
		}
		if (tomcatProperties.getAccesslog().isEnabled()) {
			customizeAccessLog(tomcatProperties, factory);
		}
		if (tomcatProperties.getUriEncoding() != null) {
			factory.setUriEncoding(tomcatProperties.getUriEncoding());
		}
		if (serverProperties.getConnectionTimeout() != null) {
			customizeConnectionTimeout(factory,
					serverProperties.getConnectionTimeout());
		}
		if (tomcatProperties.getMaxConnections() > 0) {
			customizeMaxConnections(factory, tomcatProperties.getMaxConnections());
		}
		if (tomcatProperties.getAcceptCount() > 0) {
			customizeAcceptCount(factory, tomcatProperties.getAcceptCount());
		}
		customizeStaticResources(serverProperties.getTomcat().getResource(), factory);
	}

	private static void customizeAcceptCount(ConfigurableTomcatWebServerFactory factory,
			int acceptCount) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setAcceptCount(acceptCount);
			}
		});
	}

	private static void customizeMaxConnections(ConfigurableTomcatWebServerFactory factory,
			int maxConnections) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setMaxConnections(maxConnections);
			}
		});
	}

	private static void customizeConnectionTimeout(
			ConfigurableTomcatWebServerFactory factory, Duration connectionTimeout) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setConnectionTimeout((int) connectionTimeout.toMillis());
			}
		});
	}

	private static void customizeRemoteIpValve(ServerProperties properties,
			Environment environment, ConfigurableTomcatWebServerFactory factory) {
		String protocolHeader = properties.getTomcat().getProtocolHeader();
		String remoteIpHeader = properties.getTomcat().getRemoteIpHeader();
		// For back compatibility the valve is also enabled if protocol-header is set
		if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
				|| getOrDeduceUseForwardHeaders(properties, environment)) {
			RemoteIpValve valve = new RemoteIpValve();
			valve.setProtocolHeader(StringUtils.hasLength(protocolHeader)
					? protocolHeader : "X-Forwarded-Proto");
			if (StringUtils.hasLength(remoteIpHeader)) {
				valve.setRemoteIpHeader(remoteIpHeader);
			}
			// The internal proxies default to a white list of "safe" internal IP
			// addresses
			valve.setInternalProxies(properties.getTomcat().getInternalProxies());
			valve.setPortHeader(properties.getTomcat().getPortHeader());
			valve.setProtocolHeaderHttpsValue(
					properties.getTomcat().getProtocolHeaderHttpsValue());
			// ... so it's safe to add this valve by default.
			factory.addEngineValves(valve);
		}
	}

	private static boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

	@SuppressWarnings("rawtypes")
	private static void customizeMaxThreads(ConfigurableTomcatWebServerFactory factory,
			int maxThreads) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol protocol = (AbstractProtocol) handler;
				protocol.setMaxThreads(maxThreads);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private static void customizeMinThreads(ConfigurableTomcatWebServerFactory factory,
			int minSpareThreads) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol protocol = (AbstractProtocol) handler;
				protocol.setMinSpareThreads(minSpareThreads);
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private static void customizeMaxHttpHeaderSize(
			ConfigurableTomcatWebServerFactory factory, int maxHttpHeaderSize) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol) {
				AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
				protocol.setMaxHttpHeaderSize(maxHttpHeaderSize);
			}
		});
	}

	private static void customizeMaxHttpPostSize(
			ConfigurableTomcatWebServerFactory factory, int maxHttpPostSize) {
		factory.addConnectorCustomizers(
				(connector) -> connector.setMaxPostSize(maxHttpPostSize));
	}

	private static void customizeAccessLog(ServerProperties.Tomcat tomcatProperties,
			ConfigurableTomcatWebServerFactory factory) {

		AccessLogValve valve = new AccessLogValve();
		valve.setPattern(tomcatProperties.getAccesslog().getPattern());
		valve.setDirectory(tomcatProperties.getAccesslog().getDirectory());
		valve.setPrefix(tomcatProperties.getAccesslog().getPrefix());
		valve.setSuffix(tomcatProperties.getAccesslog().getSuffix());
		valve.setRenameOnRotate(tomcatProperties.getAccesslog().isRenameOnRotate());
		valve.setFileDateFormat(tomcatProperties.getAccesslog().getFileDateFormat());
		valve.setRequestAttributesEnabled(
				tomcatProperties.getAccesslog().isRequestAttributesEnabled());
		valve.setRotatable(tomcatProperties.getAccesslog().isRotate());
		valve.setBuffered(tomcatProperties.getAccesslog().isBuffered());
		factory.addEngineValves(valve);
	}

	private static void customizeStaticResources(ServerProperties.Tomcat.Resource resource,
			ConfigurableTomcatWebServerFactory factory) {
		if (resource.getCacheTtl() == null) {
			return;
		}
		factory.addContextCustomizers((context) -> {
			context.addLifecycleListener((event) -> {
				if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
					long ttl = resource.getCacheTtl().toMillis();
					context.getResources().setCacheTtl(ttl);
				}
			});
		});
	}
}
