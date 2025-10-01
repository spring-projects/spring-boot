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
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcCodecConfiguration}.
 *
 * @author Andrei Lisa
 */
class GrpcCodecConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(GrpcCodecConfiguration.class));

	@Test
	void whenCodecNotOnClasspathThenAutoconfigurationSkipped() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Codec.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcCodecConfiguration.class));
	}

	@Test
	void whenHasCustomCompressorRegistryDoesNotAutoConfigureBean() {
		CompressorRegistry customRegistry = mock();
		this.contextRunner.withBean("customCompressorRegistry", CompressorRegistry.class, () -> customRegistry)
			.run((context) -> assertThat(context).getBean(CompressorRegistry.class).isSameAs(customRegistry));
	}

	@Test
	void compressorRegistryAutoConfiguredAsExpected() {
		this.contextRunner.run((context) -> assertThat(context).getBean(CompressorRegistry.class)
			.isSameAs(CompressorRegistry.getDefaultInstance()));
	}

	@Test
	void whenCustomCompressorsThenCompressorRegistryIsNewInstance() {
		Compressor compressor = mock();
		given(compressor.getMessageEncoding()).willReturn("foo");
		this.contextRunner.withBean(Compressor.class, () -> compressor).run((context) -> {
			assertThat(context).hasSingleBean(CompressorRegistry.class);
			CompressorRegistry registry = context.getBean(CompressorRegistry.class);
			assertThat(registry).isNotSameAs(CompressorRegistry.getDefaultInstance());
			assertThat(registry.lookupCompressor("foo")).isSameAs(compressor);
		});
	}

	@Test
	void whenHasCustomDecompressorRegistryDoesNotAutoConfigureBean() {
		DecompressorRegistry customRegistry = mock();
		this.contextRunner.withBean("customDecompressorRegistry", DecompressorRegistry.class, () -> customRegistry)
			.run((context) -> assertThat(context).getBean(DecompressorRegistry.class).isSameAs(customRegistry));
	}

	@Test
	void decompressorRegistryAutoConfiguredAsExpected() {
		this.contextRunner.run((context) -> assertThat(context).getBean(DecompressorRegistry.class)
			.isSameAs(DecompressorRegistry.getDefaultInstance()));
	}

	@Test
	void whenCustomDecompressorsThenDecompressorRegistryIsNewInstance() {
		Decompressor decompressor = mock();
		given(decompressor.getMessageEncoding()).willReturn("foo");
		this.contextRunner.withBean(Decompressor.class, () -> decompressor).run((context) -> {
			assertThat(context).hasSingleBean(DecompressorRegistry.class);
			DecompressorRegistry registry = context.getBean(DecompressorRegistry.class);
			assertThat(registry).isNotSameAs(DecompressorRegistry.getDefaultInstance());
			assertThat(registry.lookupDecompressor("foo")).isSameAs(decompressor);
		});
	}

}
