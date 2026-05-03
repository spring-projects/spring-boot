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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;

/**
 * Invokes the customizations to a {@link ManagedChannelBuilder} based on the provided
 * beans.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class GrpcChannelBuilderCustomizers {

	private final List<GrpcChannelBuilderCustomizer<?>> customizers;

	GrpcChannelBuilderCustomizers(GrpcClientProperties grpcClientProperties,
			ObjectProvider<CompressorRegistry> compressorRegistry,
			ObjectProvider<DecompressorRegistry> decompressorRegistry,
			ObjectProvider<GrpcChannelBuilderCustomizer<?>> customizers,
			ObjectProvider<GrpcClientDefaultServiceConfigCustomizer> defaultServiceConfigCustomizers) {
		this(grpcClientProperties, compressorRegistry.getIfAvailable(), decompressorRegistry.getIfAvailable(),
				customizers.orderedStream().toList(), defaultServiceConfigCustomizers.orderedStream().toList());
	}

	GrpcChannelBuilderCustomizers(List<? extends GrpcChannelBuilderCustomizer<?>> customizers) {
		this(null, null, null, customizers, Collections.emptyList());
	}

	GrpcChannelBuilderCustomizers(@Nullable GrpcClientProperties grpcClientProperties,
			@Nullable CompressorRegistry compressorRegistry, @Nullable DecompressorRegistry decompressorRegistry,
			List<? extends GrpcChannelBuilderCustomizer<?>> customizers,
			List<? extends GrpcClientDefaultServiceConfigCustomizer> defaultServiceConfigCustomizers) {
		List<GrpcChannelBuilderCustomizer<?>> all = new ArrayList<>();
		addCustomizer(all, compressorRegistry, ManagedChannelBuilder::compressorRegistry);
		addCustomizer(all, decompressorRegistry, ManagedChannelBuilder::decompressorRegistry);
		if (grpcClientProperties != null) {
			all.add(new PropertiesGrpcChannelBuilderCustomizer<>(grpcClientProperties));
		}
		all.addAll(customizers);
		all.add(customizeDefaultServiceConfig(grpcClientProperties, defaultServiceConfigCustomizers));
		this.customizers = List.copyOf(all);
	}

	private static <B extends ManagedChannelBuilder<B>, T> void addCustomizer(
			List<GrpcChannelBuilderCustomizer<?>> customizers, @Nullable T bean, BiConsumer<B, T> action) {
		if (bean != null) {
			GrpcChannelBuilderCustomizer<B> customizer = (target, builder) -> action.accept(builder, bean);
			customizers.add(customizer);
		}
	}

	private <B extends ManagedChannelBuilder<B>> GrpcChannelBuilderCustomizer<B> customizeDefaultServiceConfig(
			@Nullable GrpcClientProperties properties,
			List<? extends GrpcClientDefaultServiceConfigCustomizer> customizers) {
		PropertiesGrpcClientDefaultServiceConfigCustomizer propertiesCustomizer = (properties != null)
				? new PropertiesGrpcClientDefaultServiceConfigCustomizer(properties) : null;
		return (target, builder) -> {
			Map<String, Object> defaultServiceConfig = new LinkedHashMap<>();
			if (propertiesCustomizer != null) {
				propertiesCustomizer.customize(target, defaultServiceConfig);
			}
			customizers.forEach((customizer) -> customizer.customize(target, defaultServiceConfig));
			if (!defaultServiceConfig.isEmpty()) {
				builder.defaultServiceConfig(defaultServiceConfig);
			}
		};
	}

	<T extends ManagedChannelBuilder<T>> List<GrpcChannelBuilderCustomizer<T>> forFactory() {
		return List.of(this::apply);
	}

	@SuppressWarnings("unchecked")
	<T extends ManagedChannelBuilder<?>> void apply(String target, T builder) {
		LambdaSafe.callbacks(GrpcChannelBuilderCustomizer.class, this.customizers, builder)
			.withLogger(GrpcChannelBuilderCustomizers.class)
			.invoke((customizer) -> customizer.customize(target, builder));
	}

}
