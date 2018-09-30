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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link WebClient}.
 * <p>
 * This will produce a
 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
 * WebClient.Builder} bean with the {@code prototype} scope, meaning each injection point
 * will receive a newly cloned instance of the builder.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@AutoConfigureAfter({ CodecsAutoConfiguration.class,
		ClientHttpConnectorAutoConfiguration.class })
public class WebClientAutoConfiguration {

	private final WebClient.Builder webClientBuilder;

	public WebClientAutoConfiguration(
			ObjectProvider<WebClientCustomizer> customizerProvider) {
		this.webClientBuilder = WebClient.builder();
		customizerProvider.orderedStream()
				.forEach((customizer) -> customizer.customize(this.webClientBuilder));
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public WebClient.Builder webClientBuilder() {
		return this.webClientBuilder.clone();
	}

	@Configuration
	@ConditionalOnBean(CodecCustomizer.class)
	protected static class WebClientCodecsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@Order(0)
		public WebClientCodecCustomizer exchangeStrategiesCustomizer(
				List<CodecCustomizer> codecCustomizers) {
			return new WebClientCodecCustomizer(codecCustomizers);
		}

	}

}
