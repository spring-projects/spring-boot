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

import io.grpc.ManagedChannelBuilder;

import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.VirtualTargets;

/**
 * Callback interface that can be used to customize the default service config of the gRPC
 * channel.
 * <p>
 * This customizer should be used instead of calling
 * {@link ManagedChannelBuilder#defaultServiceConfig(Map)} from a
 * {@link GrpcChannelBuilderCustomizer} since it allows multiple customizers to update the
 * same default service config, rather than having a "last wins" outcome.
 *
 * @author Phillip Webb
 * @since 4.1.0
 * @see GrpcChannelBuilderCustomizer
 */
@FunctionalInterface
public interface GrpcClientDefaultServiceConfigCustomizer {

	/**
	 * Customize the given default service config.
	 * @param target the target (which may be a {@link VirtualTargets virtual target}).
	 * @param defaultServiceConfig the default service config to customize
	 */
	void customize(String target, Map<String, Object> defaultServiceConfig);

}
