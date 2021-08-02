/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveMultipartAutoConfiguration}.
 *
 * @author Chris Bono
 */
class ReactiveMultipartAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveMultipartAutoConfiguration.class));

	private static final boolean DEFAULT_STREAMING = false;

	private static final int DEFAULT_MAX_IN_MEMORY_SIZE = 256 * 1024;

	private static final int DEFAULT_MAX_PARTS = -1;

	private static final int DEFAULT_MAX_HEADERS_SIZE = 8 * 1024;

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final long DEFAULT_MAX_DISK_USAGE_PER_PART = -1;

	private static final Path DEFAULT_FILE_STORAGE_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
			"spring-multipart");

	@Test
	void shouldNotProvideCustomizerForNonReactiveApp() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ReactiveMultipartAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CodecCustomizer.class));
	}

	@Test
	void shouldNotProvideCustomizerWhenDefaultPartHttpMessageReaderNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(DefaultPartHttpMessageReader.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CodecCustomizer.class));
	}

	@Test
	void shouldNotProvideCustomizerWhenCodecConfigurerNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CodecConfigurer.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CodecCustomizer.class));
	}

	@Test
	void customizerSetsAppropriatePropsWhenStreamingEnabled() {
		this.contextRunner.withPropertyValues("spring.webflux.multipart.streaming:true",
				"spring.webflux.multipart.max-in-memory-size=1GB", "spring.webflux.multipart.max-headers-size=512MB",
				"spring.webflux.multipart.max-disk-usage-per-part=100MB", "spring.webflux.multipart.max-parts=7",
				"spring.webflux.multipart.file-storage-directory:.", "spring.webflux.multipart.headers-charset:UTF_16")
				.run((context) -> {
					CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
					DefaultServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();
					customizer.customize(configurer);
					DefaultPartHttpMessageReader partReader = getPartReader(configurer);

					// always set
					assertThat(partReader).hasFieldOrPropertyWithValue("streaming", true);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxParts", 7);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxHeadersSize",
							toIntBytes(DataSize.ofMegabytes(512)));
					assertThat(partReader).hasFieldOrPropertyWithValue("headersCharset", StandardCharsets.UTF_16);

					// never set when streaming
					assertThat(partReader).hasFieldOrPropertyWithValue("maxInMemorySize", DEFAULT_MAX_IN_MEMORY_SIZE);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart",
							DEFAULT_MAX_DISK_USAGE_PER_PART);
					assertThat(partReader).extracting("fileStorageDirectory")
							.asInstanceOf(InstanceOfAssertFactories.type(Mono.class))
							.returns(DEFAULT_FILE_STORAGE_DIRECTORY, Mono<Path>::block);
				});
	}

	@Test
	void customizerSetsAppropriatePropsWhenStreamingDisabledWithUnlimitedMemorySize() {
		this.contextRunner.withPropertyValues("spring.webflux.multipart.streaming:false",
				"spring.webflux.multipart.max-in-memory-size=-1", "spring.webflux.multipart.max-headers-size=512MB",
				"spring.webflux.multipart.max-disk-usage-per-part=100MB", "spring.webflux.multipart.max-parts=7",
				"spring.webflux.multipart.file-storage-directory:.", "spring.webflux.multipart.headers-charset:UTF_16")
				.run((context) -> {

					CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
					DefaultServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();
					customizer.customize(configurer);
					DefaultPartHttpMessageReader partReader = getPartReader(configurer);

					// always set
					assertThat(partReader).hasFieldOrPropertyWithValue("streaming", false);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxParts", 7);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxHeadersSize",
							toIntBytes(DataSize.ofMegabytes(512)));
					assertThat(partReader).hasFieldOrPropertyWithValue("headersCharset", StandardCharsets.UTF_16);

					// set when not streaming w/ unlimited memory size
					assertThat(partReader).hasFieldOrPropertyWithValue("maxInMemorySize", -1);

					// never set when not streaming w/ unlimited memory size
					assertThat(partReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart",
							DEFAULT_MAX_DISK_USAGE_PER_PART);
					assertThat(partReader).extracting("fileStorageDirectory")
							.asInstanceOf(InstanceOfAssertFactories.type(Mono.class))
							.returns(DEFAULT_FILE_STORAGE_DIRECTORY, Mono<Path>::block);
				});
	}

	@Test
	void customizerSetsAppropriatePropsWhenStreamingDisabledWithLimitedMemorySize() {
		this.contextRunner.withPropertyValues("spring.webflux.multipart.streaming:false",
				"spring.webflux.multipart.max-in-memory-size=1GB", "spring.webflux.multipart.max-headers-size=512MB",
				"spring.webflux.multipart.max-disk-usage-per-part=100MB", "spring.webflux.multipart.max-parts=7",
				"spring.webflux.multipart.file-storage-directory:.", "spring.webflux.multipart.headers-charset:UTF_16")
				.run((context) -> {

					CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
					DefaultServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();
					customizer.customize(configurer);
					DefaultPartHttpMessageReader partReader = getPartReader(configurer);

					// always set
					assertThat(partReader).hasFieldOrPropertyWithValue("streaming", false);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxParts", 7);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxHeadersSize",
							toIntBytes(DataSize.ofMegabytes(512)));
					assertThat(partReader).hasFieldOrPropertyWithValue("headersCharset", StandardCharsets.UTF_16);

					// set when not streaming w/ limited memory size
					assertThat(partReader).hasFieldOrPropertyWithValue("maxInMemorySize",
							toIntBytes(DataSize.ofGigabytes(1)));
					assertThat(partReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart",
							DataSize.ofMegabytes(100).toBytes());
					assertThat(partReader).extracting("fileStorageDirectory")
							.asInstanceOf(InstanceOfAssertFactories.type(Mono.class))
							.returns(Paths.get("."), Mono<Path>::block);
				});
	}

	@Test
	void customizerSetsDefaultPropsWhenNoPropertiesAreSet() {
		this.contextRunner.run((context) -> {
			CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
			DefaultServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();
			customizer.customize(configurer);
			DefaultPartHttpMessageReader partReader = getPartReader(configurer);
			assertThat(partReader).hasFieldOrPropertyWithValue("streaming", DEFAULT_STREAMING);
			assertThat(partReader).hasFieldOrPropertyWithValue("maxParts", DEFAULT_MAX_PARTS);
			assertThat(partReader).hasFieldOrPropertyWithValue("maxHeadersSize", DEFAULT_MAX_HEADERS_SIZE);
			assertThat(partReader).hasFieldOrPropertyWithValue("headersCharset", DEFAULT_CHARSET);
			assertThat(partReader).hasFieldOrPropertyWithValue("maxInMemorySize", DEFAULT_MAX_IN_MEMORY_SIZE);
			assertThat(partReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart", DEFAULT_MAX_DISK_USAGE_PER_PART);
			assertThat(partReader).extracting("fileStorageDirectory")
					.asInstanceOf(InstanceOfAssertFactories.type(Mono.class))
					.returns(DEFAULT_FILE_STORAGE_DIRECTORY, Mono<Path>::block);
		});
	}

	@Test
	void customizerSetsDefaultPropsWhenDefaultsAreExplicitlySet() {
		this.contextRunner.withPropertyValues("spring.webflux.multipart.streaming:false",
				"spring.webflux.multipart.max-in-memory-size=256KB", "spring.webflux.multipart.max-headers-size=8KB",
				"spring.webflux.multipart.max-disk-usage-per-part=-1", "spring.webflux.multipart.max-parts=-1",
				"spring.webflux.multipart.file-storage-directory:" + DEFAULT_FILE_STORAGE_DIRECTORY,
				"spring.webflux.multipart.headers-charset:" + DEFAULT_CHARSET).run((context) -> {
					CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
					DefaultServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();
					customizer.customize(configurer);
					DefaultPartHttpMessageReader partReader = getPartReader(configurer);
					assertThat(partReader).hasFieldOrPropertyWithValue("streaming", DEFAULT_STREAMING);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxParts", DEFAULT_MAX_PARTS);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxHeadersSize", DEFAULT_MAX_HEADERS_SIZE);
					assertThat(partReader).hasFieldOrPropertyWithValue("headersCharset", DEFAULT_CHARSET);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxInMemorySize", DEFAULT_MAX_IN_MEMORY_SIZE);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart",
							DEFAULT_MAX_DISK_USAGE_PER_PART);
					assertThat(partReader).extracting("fileStorageDirectory")
							.asInstanceOf(InstanceOfAssertFactories.type(Mono.class))
							.returns(DEFAULT_FILE_STORAGE_DIRECTORY, Mono<Path>::block);
				});
	}

	@Test
	void customizerBeanShouldHaveOrderOne() {
		this.contextRunner.run((context) -> {
			Method customizerMethod = ReflectionUtils.findMethod(ReactiveMultipartAutoConfiguration.class,
					"defaultPartHttpMessageReaderCustomizer", ReactiveMultipartProperties.class);
			Integer order = new TestAnnotationAwareOrderComparator().findOrder(customizerMethod);
			assertThat(order).isEqualTo(1);
		});
	}

	private DefaultPartHttpMessageReader getPartReader(DefaultServerCodecConfigurer codecConfigurer) {
		return codecConfigurer.getReaders().stream().filter(DefaultPartHttpMessageReader.class::isInstance)
				.map(DefaultPartHttpMessageReader.class::cast).findFirst()
				.orElseThrow(() -> new IllegalStateException("Could not find DefaultPartHttpMessageReader"));
	}

	private Integer toIntBytes(DataSize size) {
		return (size != null) ? (int) size.toBytes() : null;
	}

	static class TestAnnotationAwareOrderComparator extends AnnotationAwareOrderComparator {

		@Override
		public Integer findOrder(Object obj) {
			return super.findOrder(obj);
		}

	}

}
