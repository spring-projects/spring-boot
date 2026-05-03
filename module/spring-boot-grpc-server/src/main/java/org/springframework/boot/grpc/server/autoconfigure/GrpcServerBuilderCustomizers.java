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

package org.springframework.boot.grpc.server.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ServerBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.grpc.server.ServerBuilderCustomizer;

/**
 * Invokes the customizations to a {@link ServerBuilder} based on the provided beans.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class GrpcServerBuilderCustomizers {

	private final List<ServerBuilderCustomizer<?>> customizers;

	GrpcServerBuilderCustomizers(GrpcServerProperties grpcServerProperties,
			ObjectProvider<CompressorRegistry> compressorRegistry,
			ObjectProvider<DecompressorRegistry> decompressorRegistry,
			ObjectProvider<GrpcServerExecutorProvider> executorProvider,
			ObjectProvider<ServerBuilderCustomizer<?>> customizers) {
		this(grpcServerProperties, compressorRegistry.getIfAvailable(), decompressorRegistry.getIfAvailable(),
				executorProvider.getIfAvailable(), customizers.orderedStream().toList());
	}

	GrpcServerBuilderCustomizers(List<? extends ServerBuilderCustomizer<?>> customizers) {
		this(null, null, null, null, customizers);
	}

	GrpcServerBuilderCustomizers(@Nullable GrpcServerProperties grpcServerProperties,
			@Nullable CompressorRegistry compressorRegistry, @Nullable DecompressorRegistry decompressorRegistry,
			@Nullable GrpcServerExecutorProvider executorProvider,
			List<? extends ServerBuilderCustomizer<?>> customizers) {
		List<ServerBuilderCustomizer<?>> all = new ArrayList<>();
		addCustomizer(all, compressorRegistry, ServerBuilder::compressorRegistry);
		addCustomizer(all, decompressorRegistry, ServerBuilder::decompressorRegistry);
		addCustomizer(all, executorProvider, (builder, bean) -> builder.executor(bean.getExecutor()));
		if (grpcServerProperties != null) {
			all.add(new PropertiesServerBuilderCustomizer<>(grpcServerProperties));
		}
		all.addAll(customizers);
		this.customizers = List.copyOf(all);
	}

	private static <B extends ServerBuilder<B>, T> void addCustomizer(List<ServerBuilderCustomizer<?>> customizers,
			@Nullable T bean, BiConsumer<B, T> action) {
		if (bean != null) {
			ServerBuilderCustomizer<B> customizer = (builder) -> action.accept(builder, bean);
			customizers.add(customizer);
		}
	}

	<T extends ServerBuilder<T>> List<ServerBuilderCustomizer<T>> forFactory() {
		return List.of(this::apply);
	}

	@SuppressWarnings("unchecked")
	<T extends ServerBuilder<?>> void apply(T builder) {
		LambdaSafe.callbacks(ServerBuilderCustomizer.class, this.customizers, builder)
			.withLogger(GrpcServerBuilderCustomizers.class)
			.invoke((customizer) -> customizer.customize(builder));
	}

}
