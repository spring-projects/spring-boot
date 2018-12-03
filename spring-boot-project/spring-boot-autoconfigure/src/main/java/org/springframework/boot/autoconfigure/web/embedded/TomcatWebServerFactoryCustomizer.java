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

package org.springframework.boot.autoconfigure.web.embedded;

import java.time.Duration;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeStacktrace;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Tomcat-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Chentao Qu
 * @since 2.0.0
 */
public class TomcatWebServerFactoryCustomizer implements
		WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public TomcatWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties properties = this.serverProperties;
		ServerProperties.Tomcat tomcatProperties = properties.getTomcat();
		PropertyMapper propertyMapper = PropertyMapper.get();
		propertyMapper.from(tomcatProperties::getBasedir).whenNonNull()
				.to(factory::setBaseDirectory);
		propertyMapper.from(tomcatProperties::getBackgroundProcessorDelay).whenNonNull()
				.as(Duration::getSeconds).as(Long::intValue)
				.to(factory::setBackgroundProcessorDelay);
		customizeRemoteIpValve(factory);
		propertyMapper.from(tomcatProperties::getMaxThreads).when(this::isPositive)
				.to((maxThreads) -> customizeMaxThreads(factory,
						tomcatProperties.getMaxThreads()));
		propertyMapper.from(tomcatProperties::getMinSpareThreads).when(this::isPositive)
				.to((minSpareThreads) -> customizeMinThreads(factory, minSpareThreads));
		propertyMapper.from(this::determineMaxHttpHeaderSize).whenNonNull()
				.asInt(DataSize::toBytes).when(this::isPositive)
				.to((maxHttpHeaderSize) -> customizeMaxHttpHeaderSize(factory,
						maxHttpHeaderSize));
		propertyMapper.from(tomcatProperties::getMaxSwallowSize).whenNonNull()
				.asInt(DataSize::toBytes)
				.to((maxSwallowSize) -> customizeMaxSwallowSize(factory, maxSwallowSize));
		propertyMapper.from(tomcatProperties::getMaxHttpPostSize).asInt(DataSize::toBytes)
				.when((maxHttpPostSize) -> maxHttpPostSize != 0)
				.to((maxHttpPostSize) -> customizeMaxHttpPostSize(factory,
						maxHttpPostSize));
		propertyMapper.from(tomcatProperties::getAccesslog)
				.when(ServerProperties.Tomcat.Accesslog::isEnabled)
				.to((enabled) -> customizeAccessLog(factory));
		propertyMapper.from(tomcatProperties::getUriEncoding).whenNonNull()
				.to(factory::setUriEncoding);
		propertyMapper.from(properties::getConnectionTimeout).whenNonNull()
				.to((connectionTimeout) -> customizeConnectionTimeout(factory,
						connectionTimeout));
		propertyMapper.from(tomcatProperties::getMaxConnections).when(this::isPositive)
				.to((maxConnections) -> customizeMaxConnections(factory, maxConnections));
		propertyMapper.from(tomcatProperties::getAcceptCount).when(this::isPositive)
				.to((acceptCount) -> customizeAcceptCount(factory, acceptCount));
		propertyMapper.from(tomcatProperties::getProcessorCache).when(this::isPositive)
				.to((processorCache) -> customizeProcessorCache(factory, processorCache));
		customizeStaticResources(factory);
		customizeErrorReportValve(properties.getError(), factory);
	}

	private boolean isPositive(int value) {
		return value > 0;
	}

	@SuppressWarnings("deprecation")
	private DataSize determineMaxHttpHeaderSize() {
		return (this.serverProperties.getTomcat().getMaxHttpHeaderSize().toBytes() > 0)
				? this.serverProperties.getTomcat().getMaxHttpHeaderSize()
				: this.serverProperties.getMaxHttpHeaderSize();
	}

	private void customizeAcceptCount(ConfigurableTomcatWebServerFactory factory,
			int acceptCount) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setAcceptCount(acceptCount);
			}
		});
	}

	private void customizeProcessorCache(ConfigurableTomcatWebServerFactory factory,
			int processorCache) {
		factory.addConnectorCustomizers((
				connector) -> ((AbstractHttp11Protocol<?>) connector.getProtocolHandler())
						.setProcessorCache(processorCache));
	}

	private void customizeMaxConnections(ConfigurableTomcatWebServerFactory factory,
			int maxConnections) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setMaxConnections(maxConnections);
			}
		});
	}

	private void customizeConnectionTimeout(ConfigurableTomcatWebServerFactory factory,
			Duration connectionTimeout) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractProtocol) {
				AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
				protocol.setConnectionTimeout((int) connectionTimeout.toMillis());
			}
		});
	}

	private void customizeRemoteIpValve(ConfigurableTomcatWebServerFactory factory) {
		Tomcat tomcatProperties = this.serverProperties.getTomcat();
		String protocolHeader = tomcatProperties.getProtocolHeader();
		String remoteIpHeader = tomcatProperties.getRemoteIpHeader();
		// For back compatibility the valve is also enabled if protocol-header is set
		if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
				|| getOrDeduceUseForwardHeaders()) {
			RemoteIpValve valve = new RemoteIpValve();
			valve.setProtocolHeader(StringUtils.hasLength(protocolHeader) ? protocolHeader
					: "X-Forwarded-Proto");
			if (StringUtils.hasLength(remoteIpHeader)) {
				valve.setRemoteIpHeader(remoteIpHeader);
			}
			// The internal proxies default to a white list of "safe" internal IP
			// addresses
			valve.setInternalProxies(tomcatProperties.getInternalProxies());
			valve.setPortHeader(tomcatProperties.getPortHeader());
			valve.setProtocolHeaderHttpsValue(
					tomcatProperties.getProtocolHeaderHttpsValue());
			// ... so it's safe to add this valve by default.
			factory.addEngineValves(valve);
		}
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.isUseForwardHeaders() != null) {
			return this.serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(this.environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxThreads(ConfigurableTomcatWebServerFactory factory,
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
	private void customizeMinThreads(ConfigurableTomcatWebServerFactory factory,
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
	private void customizeMaxHttpHeaderSize(ConfigurableTomcatWebServerFactory factory,
			int maxHttpHeaderSize) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol) {
				AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
				protocol.setMaxHttpHeaderSize(maxHttpHeaderSize);
			}
		});
	}

	private void customizeMaxSwallowSize(ConfigurableTomcatWebServerFactory factory,
			int maxSwallowSize) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol) {
				AbstractHttp11Protocol<?> protocol = (AbstractHttp11Protocol<?>) handler;
				protocol.setMaxSwallowSize(maxSwallowSize);
			}
		});
	}

	private void customizeMaxHttpPostSize(ConfigurableTomcatWebServerFactory factory,
			int maxHttpPostSize) {
		factory.addConnectorCustomizers(
				(connector) -> connector.setMaxPostSize(maxHttpPostSize));
	}

	private void customizeAccessLog(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
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

	private void customizeStaticResources(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat.Resource resource = this.serverProperties.getTomcat()
				.getResource();
		factory.addContextCustomizers((context) -> {
			context.addLifecycleListener((event) -> {
				if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
					context.getResources().setCachingAllowed(resource.isAllowCaching());
					if (resource.getCacheTtl() != null) {
						long ttl = resource.getCacheTtl().toMillis();
						context.getResources().setCacheTtl(ttl);
					}
				}
			});
		});
	}

	private void customizeErrorReportValve(ErrorProperties error,
			ConfigurableTomcatWebServerFactory factory) {
		if (error.getIncludeStacktrace() == IncludeStacktrace.NEVER) {
			factory.addContextCustomizers((context) -> {
				ErrorReportValve valve = new ErrorReportValve();
				valve.setShowServerInfo(false);
				valve.setShowReport(false);
				context.getParent().getPipeline().addValve(valve);
			});
		}
	}

}
