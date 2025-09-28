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

package org.springframework.boot.http.codec;

import java.util.Collection;
import java.util.function.Consumer;

import org.springframework.boot.test.web.reactive.client.WebTestClientBuilderCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient.Builder;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

/**
 * {@link WebTestClientBuilderCustomizer} to apply {@link CodecCustomizer} beans.
 *
 * @author Phillip Webb
 */
class CodecWebTestClientBuilderCustomizer implements WebTestClientBuilderCustomizer {

	private final ApplicationContext context;

	CodecWebTestClientBuilderCustomizer(ApplicationContext context) {
		this.context = context;
	}

	@Override
	public void customize(Builder builder) {
		Collection<CodecCustomizer> customizers = this.context.getBeansOfType(CodecCustomizer.class).values();
		if (!CollectionUtils.isEmpty(customizers)) {
			ExchangeStrategies strategies = ExchangeStrategies.builder().codecs(apply(customizers)).build();
			builder.exchangeStrategies(strategies);
		}
	}

	private Consumer<ClientCodecConfigurer> apply(Collection<CodecCustomizer> customizers) {
		return (codecs) -> customizers.forEach((customizer) -> customizer.customize(codecs));
	}

}
