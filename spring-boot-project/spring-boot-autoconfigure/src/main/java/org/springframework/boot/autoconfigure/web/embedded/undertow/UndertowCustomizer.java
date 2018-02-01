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

package org.springframework.boot.autoconfigure.web.embedded.undertow;

import java.time.Duration;

import io.undertow.UndertowOptions;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.core.env.Environment;

/**
 * Customization for Undertow-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 * @author Yulin Qin
 * @author Stephane Nicoll
 */
public final class UndertowCustomizer {

	private UndertowCustomizer() {
	}

	public static void customizeUndertow(ServerProperties serverProperties,
			Environment environment, ConfigurableUndertowWebServerFactory factory) {
		ServerProperties.Undertow undertowProperties = serverProperties.getUndertow();
		ServerProperties.Undertow.Accesslog accesslogProperties = undertowProperties
				.getAccesslog();
		PropertyMapper propertyMapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
		propertyMapper.from(undertowProperties::getBufferSize).to(factory::setBufferSize);
		propertyMapper.from(undertowProperties::getIoThreads).to(factory::setIoThreads);
		propertyMapper.from(undertowProperties::getWorkerThreads)
				.to(factory::setWorkerThreads);
		propertyMapper.from(undertowProperties::getDirectBuffers)
				.to(factory::setUseDirectBuffers);
		propertyMapper.from(accesslogProperties::getEnabled)
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
		propertyMapper
				.from(() -> getOrDeduceUseForwardHeaders(serverProperties, environment))
				.to(factory::setUseForwardHeaders);
		propertyMapper.from(serverProperties::getMaxHttpHeaderSize)
				.when(UndertowCustomizer::isPositive)
				.to((maxHttpHeaderSize) -> customizeMaxHttpHeaderSize(factory,
						maxHttpHeaderSize));
		propertyMapper.from(undertowProperties::getMaxHttpPostSize)
				.when(UndertowCustomizer::isPositive)
				.to((maxHttpPostSize) -> customizeMaxHttpPostSize(factory,
						maxHttpPostSize));
		propertyMapper.from(serverProperties::getConnectionTimeout)
				.to((connectionTimeout) -> customizeConnectionTimeout(factory,
						connectionTimeout));
		factory.addDeploymentInfoCustomizers((deploymentInfo) -> deploymentInfo
				.setEagerFilterInit(undertowProperties.isEagerFilterInit()));
	}

	private static boolean isPositive(Number value) {
		return value.longValue() > 0;
	}

	private static void customizeConnectionTimeout(
			ConfigurableUndertowWebServerFactory factory, Duration connectionTimeout) {
		factory.addBuilderCustomizers((builder) -> builder.setSocketOption(
				UndertowOptions.NO_REQUEST_TIMEOUT, (int) connectionTimeout.toMillis()));
	}

	private static void customizeMaxHttpHeaderSize(
			ConfigurableUndertowWebServerFactory factory, int maxHttpHeaderSize) {
		factory.addBuilderCustomizers((builder) -> builder
				.setServerOption(UndertowOptions.MAX_HEADER_SIZE, maxHttpHeaderSize));
	}

	private static void customizeMaxHttpPostSize(
			ConfigurableUndertowWebServerFactory factory, long maxHttpPostSize) {
		factory.addBuilderCustomizers((builder) -> builder
				.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, maxHttpPostSize));
	}

	private static boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return platform != null && platform.isUsingForwardHeaders();
	}

}
