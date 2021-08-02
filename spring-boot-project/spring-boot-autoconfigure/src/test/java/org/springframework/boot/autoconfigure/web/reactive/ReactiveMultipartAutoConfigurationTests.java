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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveMultipartAutoConfiguration}.
 *
 * @author Chris Bono
 */
class ReactiveMultipartAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveMultipartAutoConfiguration.class));

	private static final Path DEFAULT_FILE_STORAGE_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"),
			"spring-multipart");

	@Test
	void shouldNotProvideCustomizerForNonReactiveApp() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ReactiveMultipartAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CodecCustomizer.class));
	}

	@Test
	void shouldNotProvideCustomizerWhenWebFluxNotAvailable() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(WebFluxConfigurer.class))
				.run((context) -> assertThat(context).doesNotHaveBean(CodecCustomizer.class));
	}

	@Test
	void shouldConfigureMultipartProperties() {
		this.contextRunner.withPropertyValues("spring.webflux.multipart.streaming:true",
				"spring.webflux.multipart.max-in-memory-size=1GB", "spring.webflux.multipart.max-headers-size=16KB",
				"spring.webflux.multipart.max-disk-usage-per-part=100MB", "spring.webflux.multipart.max-parts=7",
				"spring.webflux.multipart.headers-charset:UTF_16").run((context) -> {
					CodecCustomizer customizer = context.getBean(CodecCustomizer.class);
					DefaultServerCodecConfigurer configurer = new DefaultServerCodecConfigurer();
					customizer.customize(configurer);
					DefaultPartHttpMessageReader partReader = getPartReader(configurer);
					assertThat(partReader).hasFieldOrPropertyWithValue("streaming", true);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxParts", 7);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxHeadersSize",
							Math.toIntExact(DataSize.ofKilobytes(16).toBytes()));
					assertThat(partReader).hasFieldOrPropertyWithValue("headersCharset", StandardCharsets.UTF_16);
					assertThat(partReader).hasFieldOrPropertyWithValue("maxInMemorySize",
							Math.toIntExact(DataSize.ofGigabytes(1).toBytes()));
					assertThat(partReader).hasFieldOrPropertyWithValue("maxDiskUsagePerPart",
							DataSize.ofMegabytes(100).toBytes());
				});
	}

	private DefaultPartHttpMessageReader getPartReader(DefaultServerCodecConfigurer codecConfigurer) {
		return codecConfigurer.getReaders().stream().filter(DefaultPartHttpMessageReader.class::isInstance)
				.map(DefaultPartHttpMessageReader.class::cast).findFirst()
				.orElseThrow(() -> new IllegalStateException("Could not find DefaultPartHttpMessageReader"));
	}

}
