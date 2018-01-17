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
import org.springframework.boot.web.embedded.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.core.env.Environment;

/**
 * Customization for Undertow-specific features common for both Servlet and Reactive
 * servers.
 *
 * @author Brian Clozel
 */
public final class UndertowCustomizer {

	private UndertowCustomizer() {
	}

	public static void customizeUndertow(ServerProperties serverProperties,
			Environment environment, ConfigurableUndertowWebServerFactory factory) {

		ServerProperties.Undertow undertowProperties = serverProperties.getUndertow();
		ServerProperties.Undertow.Accesslog accesslogProperties = undertowProperties
				.getAccesslog();
		if (undertowProperties.getBufferSize() != null) {
			factory.setBufferSize(undertowProperties.getBufferSize());
		}
		if (undertowProperties.getIoThreads() != null) {
			factory.setIoThreads(undertowProperties.getIoThreads());
		}
		if (undertowProperties.getWorkerThreads() != null) {
			factory.setWorkerThreads(undertowProperties.getWorkerThreads());
		}
		if (undertowProperties.getDirectBuffers() != null) {
			factory.setUseDirectBuffers(undertowProperties.getDirectBuffers());
		}
		if (undertowProperties.getAccesslog().getEnabled() != null) {
			factory.setAccessLogEnabled(accesslogProperties.getEnabled());
		}
		factory.setAccessLogDirectory(accesslogProperties.getDir());
		factory.setAccessLogPattern(accesslogProperties.getPattern());
		factory.setAccessLogPrefix(accesslogProperties.getPrefix());
		factory.setAccessLogSuffix(accesslogProperties.getSuffix());
		factory.setAccessLogRotate(accesslogProperties.isRotate());
		factory.setUseForwardHeaders(
				getOrDeduceUseForwardHeaders(serverProperties, environment));
		if (serverProperties.getMaxHttpHeaderSize() > 0) {
			customizeMaxHttpHeaderSize(factory, serverProperties.getMaxHttpHeaderSize());
		}
		if (undertowProperties.getMaxHttpPostSize() > 0) {
			customizeMaxHttpPostSize(factory, undertowProperties.getMaxHttpPostSize());
		}
		if (serverProperties.getConnectionTimeout() != null) {
			customizeConnectionTimeout(factory, serverProperties.getConnectionTimeout());
		}
		factory.addDeploymentInfoCustomizers((deploymentInfo) -> deploymentInfo
				.setEagerFilterInit(undertowProperties.isEagerFilterInit()));
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
