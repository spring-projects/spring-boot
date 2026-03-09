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

import java.util.List;

import io.grpc.Compressor;
import io.grpc.CompressorRegistry;
import io.grpc.Decompressor;
import io.grpc.DecompressorRegistry;
import io.grpc.ServerBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The configuration that contains all gRPC codec related beans.
 *
 * @author Andrei Lisa
 */
@Configuration(proxyBeanMethods = false)
class GrpcServerCodecConfiguration {

	/**
	 * The compressor registry that is set on the
	 * {@link ServerBuilder#compressorRegistry(CompressorRegistry) server builder} .
	 * @param compressors the compressors to use on the registry
	 * @return a new {@link CompressorRegistry#newEmptyInstance() registry} with the
	 * specified compressors or the {@link CompressorRegistry#getDefaultInstance() default
	 * registry} if no custom compressors are available in the application context.
	 */
	@Bean
	@ConditionalOnMissingBean
	CompressorRegistry grpcCompressorRegistry(List<Compressor> compressors) {
		if (compressors.isEmpty()) {
			return CompressorRegistry.getDefaultInstance();
		}
		CompressorRegistry registry = CompressorRegistry.newEmptyInstance();
		compressors.forEach(registry::register);
		return registry;
	}

	/**
	 * The decompressor registry that is set on the
	 * {@link ServerBuilder#decompressorRegistry(DecompressorRegistry) server builder}.
	 * @param decompressors the decompressors to use on the registry
	 * @return a new {@link DecompressorRegistry#emptyInstance() registry} with the
	 * specified decompressors or the {@link DecompressorRegistry#getDefaultInstance()
	 * default registry} if no custom decompressors are available in the application
	 * context.
	 */
	@Bean
	@ConditionalOnMissingBean
	DecompressorRegistry grpcDecompressorRegistry(List<Decompressor> decompressors) {
		if (decompressors.isEmpty()) {
			return DecompressorRegistry.getDefaultInstance();
		}
		DecompressorRegistry registry = DecompressorRegistry.emptyInstance();
		for (Decompressor decompressor : decompressors) {
			registry = registry.with(decompressor, false);
		}
		return registry;
	}

}
