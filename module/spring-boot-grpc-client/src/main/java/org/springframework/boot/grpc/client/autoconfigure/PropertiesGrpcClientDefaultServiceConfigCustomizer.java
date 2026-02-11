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

import java.util.Map;

import org.springframework.boot.grpc.client.autoconfigure.GrpcClientProperties.Channel;

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
		if (channel != null && channel.getHealth().isEnabled()) {
			String serviceName = channel.getHealth().getServiceName();
			Map<String, String> healthCheckConfig = Map.of("serviceName", (serviceName != null) ? serviceName : "");
			defaultServiceConfig.put("healthCheckConfig", healthCheckConfig);
		}
	}

}
