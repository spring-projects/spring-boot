/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * {@link WebTestClientBuilderCustomizer} for a typical Spring Boot application. Usually applied
 * automatically via {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient}, but may also be
 * used directly.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Roman Zaynetdinov
 * @since 2.0.0
 */
public class SpringBootWebTestClientBuilderCustomizer implements WebTestClientBuilderCustomizer {

	private final ApplicationContext context;

	/**
	 * Create a new {@link SpringBootWebTestClientBuilderCustomizer} instance.
	 * @param context the source application context
	 */
	public SpringBootWebTestClientBuilderCustomizer(ApplicationContext context) {
		Assert.notNull(context, "Context must not be null");
		this.context = context;
	}

	@Override
	public void customize(WebTestClient.Builder builder) {
		customizeWebTestClient(builder);
		customizeWebTestClientCodecs(builder);
	}

	private void customizeWebTestClient(WebTestClient.Builder builder) {
		Binder.get(this.context.getEnvironment())
				.bind("spring.test.webtestclient.timeout", Duration.class)
				.ifBound(builder::responseTimeout);
	}

	private void customizeWebTestClientCodecs(WebTestClient.Builder builder) {
		Collection<CodecCustomizer> customizers = this.context
				.getBeansOfType(CodecCustomizer.class).values();
		if (!CollectionUtils.isEmpty(customizers)) {
			builder.exchangeStrategies(ExchangeStrategies.builder()
					.codecs(this.applyCustomizers(customizers)).build());
		}
	}

	private Consumer<ClientCodecConfigurer> applyCustomizers(
			Collection<CodecCustomizer> customizers) {
		return (codecs) -> customizers
				.forEach((customizer) -> customizer.customize(codecs));
	}

}
