/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;
import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel.Health;
import org.springframework.util.Assert;

/**
 * {@link GrpcClientDefaultServiceConfigCustomizer} to apply {@link GrpcClientProperties}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @param properties the client properties
 */
record PropertiesGrpcClientDefaultServiceConfigCustomizer(
		GrpcClientProperties properties) implements GrpcClientDefaultServiceConfigCustomizer {

	@Override
	public void customize(String target, Map<String, Object> defaultServiceConfig) {
		Channel channel = this.properties.getChannel().get(target);
		channel = (channel != null) ? channel : this.properties.getChannel().get("default");
		if (channel == null) {
			return;
		}
		applyServiceConfig(channel.getServiceConfig(), defaultServiceConfig);
		applyHealth(channel.getHealth(), defaultServiceConfig);
	}

	private void applyServiceConfig(@Nullable ServiceConfig serviceConfig, Map<String, Object> defaultServiceConfig) {
		if (serviceConfig != null) {
			serviceConfig.applyTo(defaultServiceConfig);
		}
	}

	private void applyHealth(Health health, Map<String, Object> defaultServiceConfig) {
		if (!health.isEnabled()) {
			return;
		}
		String serviceName = (health.getServiceName() != null) ? health.getServiceName() : "";
		Map<String, Object> healthCheckConfig = cloneOrCreateHealthCheckConfig(defaultServiceConfig);
		String existingServiceName = (String) healthCheckConfig.get(ServiceConfig.HEALTH_CHECK_SERVICE_NAME_KEY);
		Assert.state(existingServiceName == null || serviceName.equals(existingServiceName),
				() -> "Unable to change health check config service name from '%s' to '%s'"
					.formatted(existingServiceName, serviceName));
		healthCheckConfig.put(ServiceConfig.HEALTH_CHECK_SERVICE_NAME_KEY, serviceName);
		defaultServiceConfig.put(ServiceConfig.HEALTH_CHECK_CONFIG_KEY, healthCheckConfig);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> cloneOrCreateHealthCheckConfig(Map<String, Object> defaultServiceConfig) {
		Map<String, Object> healthCheckConfig = (Map<String, Object>) defaultServiceConfig
			.get(ServiceConfig.HEALTH_CHECK_CONFIG_KEY);
		return new LinkedHashMap<>((healthCheckConfig != null) ? healthCheckConfig : Collections.emptyMap());
	}

}
