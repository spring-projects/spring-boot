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

import io.grpc.Codec;
import io.grpc.Compressor;
import io.grpc.CompressorRegistry;
import io.grpc.Decompressor;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The configuration that contains all codec related beans for clients.
 *
 * @author Andrei Lisa
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Codec.class)
class GrpcCodecConfiguration {

	/**
	 * The compressor registry that is set on the
	 * {@link ManagedChannelBuilder#compressorRegistry(CompressorRegistry) channel
	 * builder}.
	 * @param compressors the compressors to use on the registry
	 * @return a new {@link CompressorRegistry#newEmptyInstance() registry} with the
	 * specified compressors or the {@link CompressorRegistry#getDefaultInstance() default
	 * registry} if no custom compressors are available in the application context.
	 */
	@Bean
	@ConditionalOnMissingBean
	CompressorRegistry compressorRegistry(ObjectProvider<Compressor> compressors) {
		if (compressors.stream().count() == 0) {
			return CompressorRegistry.getDefaultInstance();
		}
		CompressorRegistry registry = CompressorRegistry.newEmptyInstance();
		compressors.orderedStream().forEachOrdered(registry::register);
		return registry;
	}

	/**
	 * The decompressor registry that is set on the
	 * {@link ManagedChannelBuilder#decompressorRegistry(DecompressorRegistry) channel
	 * builder}.
	 * @param decompressors the decompressors to use on the registry
	 * @return a new {@link DecompressorRegistry#emptyInstance() registry} with the
	 * specified decompressors or the {@link DecompressorRegistry#getDefaultInstance()
	 * default registry} if no custom decompressors are available in the application
	 * context.
	 */
	@Bean
	@ConditionalOnMissingBean
	DecompressorRegistry decompressorRegistry(ObjectProvider<Decompressor> decompressors) {
		if (decompressors.stream().count() == 0) {
			return DecompressorRegistry.getDefaultInstance();
		}
		DecompressorRegistry registry = DecompressorRegistry.emptyInstance();
		for (Decompressor decompressor : decompressors.orderedStream().toList()) {
			registry = registry.with(decompressor, false);
		}
		return registry;
	}

}
