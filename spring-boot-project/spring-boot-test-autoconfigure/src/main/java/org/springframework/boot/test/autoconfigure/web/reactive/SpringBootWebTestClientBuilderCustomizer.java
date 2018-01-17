/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.autoconfigure.web.reactive;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Consumer;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient.Builder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

/**
 * {@link WebTestClientBuilderCustomizer} for a typical Spring Boot application. Usually
 * applied automatically via
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient}, but may also be used
 * directly.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class SpringBootWebTestClientBuilderCustomizer
		implements WebTestClientBuilderCustomizer {

	private final Collection<CodecCustomizer> codecCustomizers;

	private Duration timeout;

	/**
	 * Create a new {@code SpringBootWebTestClientBuilderCustomizer} that will configure
	 * the builder's codecs using the given {@code codecCustomizers}.
	 * @param codecCustomizers the codec customizers
	 */
	public SpringBootWebTestClientBuilderCustomizer(
			Collection<CodecCustomizer> codecCustomizers) {
		this.codecCustomizers = codecCustomizers;
	}

	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	@Override
	public void customize(Builder builder) {
		if (this.timeout != null) {
			builder.responseTimeout(this.timeout);
		}
		customizeWebTestClientCodecs(builder);
	}

	private void customizeWebTestClientCodecs(WebTestClient.Builder builder) {
		if (!CollectionUtils.isEmpty(this.codecCustomizers)) {
			builder.exchangeStrategies(ExchangeStrategies.builder()
					.codecs(applyCustomizers(this.codecCustomizers)).build());
		}
	}

	private Consumer<ClientCodecConfigurer> applyCustomizers(
			Collection<CodecCustomizer> customizers) {
		return (codecs) -> customizers
				.forEach((customizer) -> customizer.customize(codecs));
	}

}
