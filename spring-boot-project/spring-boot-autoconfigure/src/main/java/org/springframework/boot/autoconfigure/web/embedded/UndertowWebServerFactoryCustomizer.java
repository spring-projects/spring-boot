/*
 * Copyright 2012-2019 the original author or authors.
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

import io.undertow.UndertowOptions;
import org.xnio.Option;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;

/**
 * Customization for Undertow-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Arstiom Yudovin
 * @author Rafiullah Hamedy
 * @since 2.0.0
 */
public class UndertowWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public UndertowWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
		this.environment = environment;
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void customize(ConfigurableUndertowWebServerFactory factory) {
		ServerProperties properties = this.serverProperties;
		ServerProperties.Undertow undertowProperties = properties.getUndertow();
		ServerProperties.Undertow.Accesslog accesslogProperties = undertowProperties.getAccesslog();
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		propertyMapper.from(undertowProperties::getBufferSize).whenNonNull().asInt(DataSize::toBytes)
				.to(factory::setBufferSize);
		propertyMapper.from(undertowProperties::getIoThreads).to(factory::setIoThreads);
		propertyMapper.from(undertowProperties::getWorkerThreads).to(factory::setWorkerThreads);
		propertyMapper.from(undertowProperties::getDirectBuffers).to(factory::setUseDirectBuffers);
		propertyMapper.from(accesslogProperties::isEnabled).to(factory::setAccessLogEnabled);
		propertyMapper.from(accesslogProperties::getDir).to(factory::setAccessLogDirectory);
		propertyMapper.from(accesslogProperties::getPattern).to(factory::setAccessLogPattern);
		propertyMapper.from(accesslogProperties::getPrefix).to(factory::setAccessLogPrefix);
		propertyMapper.from(accesslogProperties::getSuffix).to(factory::setAccessLogSuffix);
		propertyMapper.from(accesslogProperties::isRotate).to(factory::setAccessLogRotate);
		propertyMapper.from(this::getOrDeduceUseForwardHeaders).to(factory::setUseForwardHeaders);

		propertyMapper.from(properties::getMaxHttpHeaderSize).whenNonNull().asInt(DataSize::toBytes)
				.when(this::isPositive).to((maxHttpHeaderSize) -> customizeServerOption(factory,
						UndertowOptions.MAX_HEADER_SIZE, maxHttpHeaderSize));

		propertyMapper.from(undertowProperties::getMaxHttpPostSize).as(DataSize::toBytes).when(this::isPositive).to(
				(maxHttpPostSize) -> customizeServerOption(factory, UndertowOptions.MAX_ENTITY_SIZE, maxHttpPostSize));

		propertyMapper.from(properties::getConnectionTimeout).to((connectionTimeout) -> customizeServerOption(factory,
				UndertowOptions.NO_REQUEST_TIMEOUT, (int) connectionTimeout.toMillis()));

		propertyMapper.from(undertowProperties::getMaxParameters)
				.to((maxParameters) -> customizeServerOption(factory, UndertowOptions.MAX_PARAMETERS, maxParameters));

		propertyMapper.from(undertowProperties::getMaxHeaders)
				.to((maxHeaders) -> customizeServerOption(factory, UndertowOptions.MAX_HEADERS, maxHeaders));

		propertyMapper.from(undertowProperties::getMaxCookies)
				.to((maxCookies) -> customizeServerOption(factory, UndertowOptions.MAX_COOKIES, maxCookies));

		propertyMapper.from(undertowProperties::isAllowEncodedSlash)
				.to((allowEncodedSlash) -> customizeServerOption(factory, UndertowOptions.ALLOW_ENCODED_SLASH,
						allowEncodedSlash));

		propertyMapper.from(undertowProperties::isDecodeUrl)
				.to((isDecodeUrl) -> customizeServerOption(factory, UndertowOptions.DECODE_URL, isDecodeUrl));

		propertyMapper.from(undertowProperties::getUrlCharset)
				.to((urlCharset) -> customizeServerOption(factory, UndertowOptions.URL_CHARSET, urlCharset.name()));

		propertyMapper.from(undertowProperties::isAlwaysSetKeepAlive)
				.to((alwaysSetKeepAlive) -> customizeServerOption(factory, UndertowOptions.ALWAYS_SET_KEEP_ALIVE,
						alwaysSetKeepAlive));

		factory.addDeploymentInfoCustomizers(
				(deploymentInfo) -> deploymentInfo.setEagerFilterInit(undertowProperties.isEagerFilterInit()));
	}

	private boolean isPositive(Number value) {
		return value.longValue() > 0;
	}

	private <T> void customizeServerOption(ConfigurableUndertowWebServerFactory factory, Option<T> option, T value) {
		factory.addBuilderCustomizers((builder) -> builder.setServerOption(option, value));
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NONE)) {
			CloudPlatform platform = CloudPlatform.getActive(this.environment);
			return platform != null && platform.isUsingForwardHeaders();
		}
		return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
	}

}
