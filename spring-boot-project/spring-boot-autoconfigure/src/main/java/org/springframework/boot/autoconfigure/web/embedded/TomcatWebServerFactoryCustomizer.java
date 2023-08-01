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
import java.util.List;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties.IncludeAttribute;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat.Accesslog;
import org.springframework.boot.autoconfigure.web.ServerProperties.Tomcat.Remoteip;
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
 * @author Andrew McGhie
 * @author Dirk Deyne
 * @author Rafiullah Hamedy
 * @author Victor Mandujano
 * @author Parviz Rozikov
 * @author Florian Storz
 * @author Michael Weidmann
 * @since 2.0.0
 */
public class TomcatWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

	static final int order = 0;

	private final Environment environment;

	private final ServerProperties serverProperties;

	public TomcatWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	@SuppressWarnings("removal")
	public void customize(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat properties = this.serverProperties.getTomcat();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(properties::getBasedir).to(factory::setBaseDirectory);
		map.from(properties::getBackgroundProcessorDelay)
			.as(Duration::getSeconds)
			.as(Long::intValue)
			.to(factory::setBackgroundProcessorDelay);
		customizeRemoteIpValve(factory);
		ServerProperties.Tomcat.Threads threadProperties = properties.getThreads();
		map.from(threadProperties::getMax)
			.when(this::isPositive)
			.to((maxThreads) -> customizeMaxThreads(factory, threadProperties.getMax()));
		map.from(threadProperties::getMinSpare)
			.when(this::isPositive)
			.to((minSpareThreads) -> customizeMinThreads(factory, minSpareThreads));
		map.from(this.serverProperties.getMaxHttpRequestHeaderSize())
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to((maxHttpRequestHeaderSize) -> customizeMaxHttpRequestHeaderSize(factory, maxHttpRequestHeaderSize));
		map.from(properties::getMaxHttpResponseHeaderSize)
			.asInt(DataSize::toBytes)
			.when(this::isPositive)
			.to((maxHttpResponseHeaderSize) -> customizeMaxHttpResponseHeaderSize(factory, maxHttpResponseHeaderSize));
		map.from(properties::getMaxSwallowSize)
			.asInt(DataSize::toBytes)
			.to((maxSwallowSize) -> customizeMaxSwallowSize(factory, maxSwallowSize));
		map.from(properties::getMaxHttpFormPostSize)
			.asInt(DataSize::toBytes)
			.when((maxHttpFormPostSize) -> maxHttpFormPostSize != 0)
			.to((maxHttpFormPostSize) -> customizeMaxHttpFormPostSize(factory, maxHttpFormPostSize));
		map.from(properties::getAccesslog)
			.when(ServerProperties.Tomcat.Accesslog::isEnabled)
			.to((enabled) -> customizeAccessLog(factory));
		map.from(properties::getUriEncoding).to(factory::setUriEncoding);
		map.from(properties::getConnectionTimeout)
			.to((connectionTimeout) -> customizeConnectionTimeout(factory, connectionTimeout));
		map.from(properties::getMaxConnections)
			.when(this::isPositive)
			.to((maxConnections) -> customizeMaxConnections(factory, maxConnections));
		map.from(properties::getAcceptCount)
			.when(this::isPositive)
			.to((acceptCount) -> customizeAcceptCount(factory, acceptCount));
		map.from(properties::getProcessorCache)
			.to((processorCache) -> customizeProcessorCache(factory, processorCache));
		map.from(properties::getKeepAliveTimeout)
			.to((keepAliveTimeout) -> customizeKeepAliveTimeout(factory, keepAliveTimeout));
		map.from(properties::getMaxKeepAliveRequests)
			.to((maxKeepAliveRequests) -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));
		map.from(properties::getRelaxedPathChars)
			.as(this::joinCharacters)
			.whenHasText()
			.to((relaxedChars) -> customizeRelaxedPathChars(factory, relaxedChars));
		map.from(properties::getRelaxedQueryChars)
			.as(this::joinCharacters)
			.whenHasText()
			.to((relaxedChars) -> customizeRelaxedQueryChars(factory, relaxedChars));
		map.from(properties::isRejectIllegalHeader)
			.to((rejectIllegalHeader) -> customizeRejectIllegalHeader(factory, rejectIllegalHeader));
		customizeStaticResources(factory);
		customizeErrorReportValve(this.serverProperties.getError(), factory);
	}

	private boolean isPositive(int value) {
		return value > 0;
	}

	@SuppressWarnings("rawtypes")
	private void customizeAcceptCount(ConfigurableTomcatWebServerFactory factory, int acceptCount) {
		customizeHandler(factory, acceptCount, AbstractProtocol.class, AbstractProtocol::setAcceptCount);
	}

	@SuppressWarnings("rawtypes")
	private void customizeProcessorCache(ConfigurableTomcatWebServerFactory factory, int processorCache) {
		customizeHandler(factory, processorCache, AbstractProtocol.class, AbstractProtocol::setProcessorCache);
	}

	private void customizeKeepAliveTimeout(ConfigurableTomcatWebServerFactory factory, Duration keepAliveTimeout) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			for (UpgradeProtocol upgradeProtocol : handler.findUpgradeProtocols()) {
				if (upgradeProtocol instanceof Http2Protocol protocol) {
					protocol.setKeepAliveTimeout(keepAliveTimeout.toMillis());
				}
			}
			if (handler instanceof AbstractProtocol<?> protocol) {
				protocol.setKeepAliveTimeout((int) keepAliveTimeout.toMillis());
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxKeepAliveRequests(ConfigurableTomcatWebServerFactory factory, int maxKeepAliveRequests) {
		customizeHandler(factory, maxKeepAliveRequests, AbstractHttp11Protocol.class,
				AbstractHttp11Protocol::setMaxKeepAliveRequests);
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxConnections(ConfigurableTomcatWebServerFactory factory, int maxConnections) {
		customizeHandler(factory, maxConnections, AbstractProtocol.class, AbstractProtocol::setMaxConnections);
	}

	@SuppressWarnings("rawtypes")
	private void customizeConnectionTimeout(ConfigurableTomcatWebServerFactory factory, Duration connectionTimeout) {
		customizeHandler(factory, (int) connectionTimeout.toMillis(), AbstractProtocol.class,
				AbstractProtocol::setConnectionTimeout);
	}

	private void customizeRelaxedPathChars(ConfigurableTomcatWebServerFactory factory, String relaxedChars) {
		factory.addConnectorCustomizers((connector) -> connector.setProperty("relaxedPathChars", relaxedChars));
	}

	private void customizeRelaxedQueryChars(ConfigurableTomcatWebServerFactory factory, String relaxedChars) {
		factory.addConnectorCustomizers((connector) -> connector.setProperty("relaxedQueryChars", relaxedChars));
	}

	@SuppressWarnings("deprecation")
	private void customizeRejectIllegalHeader(ConfigurableTomcatWebServerFactory factory, boolean rejectIllegalHeader) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol<?> protocol) {
				protocol.setRejectIllegalHeader(rejectIllegalHeader);
			}
		});
	}

	private String joinCharacters(List<Character> content) {
		return content.stream().map(String::valueOf).collect(Collectors.joining());
	}

	private void customizeRemoteIpValve(ConfigurableTomcatWebServerFactory factory) {
		Remoteip remoteIpProperties = this.serverProperties.getTomcat().getRemoteip();
		String protocolHeader = remoteIpProperties.getProtocolHeader();
		String remoteIpHeader = remoteIpProperties.getRemoteIpHeader();
		// For back compatibility the valve is also enabled if protocol-header is set
		if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
				|| getOrDeduceUseForwardHeaders()) {
			RemoteIpValve valve = new RemoteIpValve();
			valve.setProtocolHeader(StringUtils.hasLength(protocolHeader) ? protocolHeader : "X-Forwarded-Proto");
			if (StringUtils.hasLength(remoteIpHeader)) {
				valve.setRemoteIpHeader(remoteIpHeader);
			}
			valve.setTrustedProxies(remoteIpProperties.getTrustedProxies());
			// The internal proxies default to a list of "safe" internal IP addresses
			valve.setInternalProxies(remoteIpProperties.getInternalProxies());
			try {
				valve.setHostHeader(remoteIpProperties.getHostHeader());
			}
			catch (NoSuchMethodError ex) {
				// Avoid failure with war deployments to Tomcat 8.5 before 8.5.44 and
				// Tomcat 9 before 9.0.23
			}
			valve.setPortHeader(remoteIpProperties.getPortHeader());
			valve.setProtocolHeaderHttpsValue(remoteIpProperties.getProtocolHeaderHttpsValue());
			// ... so it's safe to add this valve by default.
			factory.addEngineValves(valve);
		}
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy() == null) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy() == ServerProperties.ForwardHeadersStrategy.NATIVE;
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxThreads(ConfigurableTomcatWebServerFactory factory, int maxThreads) {
		customizeHandler(factory, maxThreads, AbstractProtocol.class, AbstractProtocol::setMaxThreads);
	}

	@SuppressWarnings("rawtypes")
	private void customizeMinThreads(ConfigurableTomcatWebServerFactory factory, int minSpareThreads) {
		customizeHandler(factory, minSpareThreads, AbstractProtocol.class, AbstractProtocol::setMinSpareThreads);
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxHttpRequestHeaderSize(ConfigurableTomcatWebServerFactory factory,
			int maxHttpRequestHeaderSize) {
		customizeHandler(factory, maxHttpRequestHeaderSize, AbstractHttp11Protocol.class,
				AbstractHttp11Protocol::setMaxHttpRequestHeaderSize);
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxHttpResponseHeaderSize(ConfigurableTomcatWebServerFactory factory,
			int maxHttpResponseHeaderSize) {
		customizeHandler(factory, maxHttpResponseHeaderSize, AbstractHttp11Protocol.class,
				AbstractHttp11Protocol::setMaxHttpResponseHeaderSize);
	}

	@SuppressWarnings("rawtypes")
	private void customizeMaxSwallowSize(ConfigurableTomcatWebServerFactory factory, int maxSwallowSize) {
		customizeHandler(factory, maxSwallowSize, AbstractHttp11Protocol.class,
				AbstractHttp11Protocol::setMaxSwallowSize);
	}

	private <T extends ProtocolHandler> void customizeHandler(ConfigurableTomcatWebServerFactory factory, int value,
			Class<T> type, ObjIntConsumer<T> consumer) {
		factory.addConnectorCustomizers((connector) -> {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (type.isAssignableFrom(handler.getClass())) {
				consumer.accept(type.cast(handler), value);
			}
		});
	}

	private void customizeMaxHttpFormPostSize(ConfigurableTomcatWebServerFactory factory, int maxHttpFormPostSize) {
		factory.addConnectorCustomizers((connector) -> connector.setMaxPostSize(maxHttpFormPostSize));
	}

	private void customizeAccessLog(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
		AccessLogValve valve = new AccessLogValve();
		PropertyMapper map = PropertyMapper.get();
		Accesslog accessLogConfig = tomcatProperties.getAccesslog();
		map.from(accessLogConfig.getConditionIf()).to(valve::setConditionIf);
		map.from(accessLogConfig.getConditionUnless()).to(valve::setConditionUnless);
		map.from(accessLogConfig.getPattern()).to(valve::setPattern);
		map.from(accessLogConfig.getDirectory()).to(valve::setDirectory);
		map.from(accessLogConfig.getPrefix()).to(valve::setPrefix);
		map.from(accessLogConfig.getSuffix()).to(valve::setSuffix);
		map.from(accessLogConfig.getEncoding()).whenHasText().to(valve::setEncoding);
		map.from(accessLogConfig.getLocale()).whenHasText().to(valve::setLocale);
		map.from(accessLogConfig.isCheckExists()).to(valve::setCheckExists);
		map.from(accessLogConfig.isRotate()).to(valve::setRotatable);
		map.from(accessLogConfig.isRenameOnRotate()).to(valve::setRenameOnRotate);
		map.from(accessLogConfig.getMaxDays()).to(valve::setMaxDays);
		map.from(accessLogConfig.getFileDateFormat()).to(valve::setFileDateFormat);
		map.from(accessLogConfig.isIpv6Canonical()).to(valve::setIpv6Canonical);
		map.from(accessLogConfig.isRequestAttributesEnabled()).to(valve::setRequestAttributesEnabled);
		map.from(accessLogConfig.isBuffered()).to(valve::setBuffered);
		factory.addEngineValves(valve);
	}

	private void customizeStaticResources(ConfigurableTomcatWebServerFactory factory) {
		ServerProperties.Tomcat.Resource resource = this.serverProperties.getTomcat().getResource();
		factory.addContextCustomizers((context) -> context.addLifecycleListener((event) -> {
			if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
				context.getResources().setCachingAllowed(resource.isAllowCaching());
				if (resource.getCacheTtl() != null) {
					long ttl = resource.getCacheTtl().toMillis();
					context.getResources().setCacheTtl(ttl);
				}
			}
		}));
	}

	private void customizeErrorReportValve(ErrorProperties error, ConfigurableTomcatWebServerFactory factory) {
		if (error.getIncludeStacktrace() == IncludeAttribute.NEVER) {
			factory.addContextCustomizers((context) -> {
				ErrorReportValve valve = new ErrorReportValve();
				valve.setShowServerInfo(false);
				valve.setShowReport(false);
				context.getParent().getPipeline().addValve(valve);
			});
		}
	}

}
