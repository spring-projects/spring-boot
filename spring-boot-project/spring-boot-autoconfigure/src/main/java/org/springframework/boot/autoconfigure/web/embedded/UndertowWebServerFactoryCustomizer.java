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

import io.undertow.UndertowOptions;

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
 * @since 2.0.0
 */
public class UndertowWebServerFactoryCustomizer implements
		WebServerFactoryCustomizer<ConfigurableUndertowWebServerFactory>, Ordered {

	private final Environment environment;

	private final ServerProperties serverProperties;

	public UndertowWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties) {
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
		ServerProperties.Undertow.Accesslog accesslogProperties = undertowProperties
				.getAccesslog();
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		propertyMapper.from(undertowProperties::getBufferSize).whenNonNull()
				.asInt(DataSize::toBytes).to(factory::setBufferSize);
		propertyMapper.from(undertowProperties::getIoThreads).to(factory::setIoThreads);
		propertyMapper.from(undertowProperties::getWorkerThreads)
				.to(factory::setWorkerThreads);
		propertyMapper.from(undertowProperties::getDirectBuffers)
				.to(factory::setUseDirectBuffers);
		propertyMapper.from(accesslogProperties::isEnabled)
				.to(factory::setAccessLogEnabled);
		propertyMapper.from(accesslogProperties::getDir)
				.to(factory::setAccessLogDirectory);
		propertyMapper.from(accesslogProperties::getPattern)
				.to(factory::setAccessLogPattern);
		propertyMapper.from(accesslogProperties::getPrefix)
				.to(factory::setAccessLogPrefix);
		propertyMapper.from(accesslogProperties::getSuffix)
				.to(factory::setAccessLogSuffix);
		propertyMapper.from(accesslogProperties::isRotate)
				.to(factory::setAccessLogRotate);
		propertyMapper.from(this::getOrDeduceUseForwardHeaders)
				.to(factory::setUseForwardHeaders);
		propertyMapper.from(properties::getMaxHttpHeaderSize).whenNonNull()
				.asInt(DataSize::toBytes).when(this::isPositive)
				.to((maxHttpHeaderSize) -> customizeMaxHttpHeaderSize(factory,
						maxHttpHeaderSize));
		propertyMapper.from(undertowProperties::getMaxHttpPostSize)
				.asInt(DataSize::toBytes).when(this::isPositive)
				.to((maxHttpPostSize) -> customizeMaxHttpPostSize(factory,
						maxHttpPostSize));
		propertyMapper.from(properties::getConnectionTimeout)
				.to((connectionTimeout) -> customizeConnectionTimeout(factory,
						connectionTimeout));
		factory.addDeploymentInfoCustomizers((deploymentInfo) -> deploymentInfo
				.setEagerFilterInit(undertowProperties.isEagerFilterInit()));
	}

	private boolean isPositive(Number value) {
		return value.longValue() > 0;
	}

	private void customizeConnectionTimeout(ConfigurableUndertowWebServerFactory factory,
			Duration connectionTimeout) {
		factory.addBuilderCustomizers((builder) -> builder.setSocketOption(
				UndertowOptions.NO_REQUEST_TIMEOUT, (int) connectionTimeout.toMillis()));
	}

	private void customizeMaxHttpHeaderSize(ConfigurableUndertowWebServerFactory factory,
			int maxHttpHeaderSize) {
		factory.addBuilderCustomizers((builder) -> builder
				.setServerOption(UndertowOptions.MAX_HEADER_SIZE, maxHttpHeaderSize));
	}

	private void customizeMaxHttpPostSize(ConfigurableUndertowWebServerFactory factory,
			long maxHttpPostSize) {
		factory.addBuilderCustomizers((builder) -> builder
				.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, maxHttpPostSize));
	}

	private boolean getOrDeduceUseForwardHeaders() {
		if (this.serverProperties.isUseForwardHeaders() != null) {
			return this.serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(this.environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

}
