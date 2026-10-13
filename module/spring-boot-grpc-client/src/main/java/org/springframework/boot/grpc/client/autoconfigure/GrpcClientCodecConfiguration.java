/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.client.autoconfigure;

import java.util.List;

import io.grpc.Codec;
import io.grpc.Compressor;
import io.grpc.CompressorRegistry;
import io.grpc.Decompressor;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannelBuilder;

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
class GrpcClientCodecConfiguration {

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
