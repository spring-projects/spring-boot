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

import java.util.Collection;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto-configuration for {@link WebTestClient}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ WebClient.class, WebTestClient.class })
@AutoConfigureAfter(CodecsAutoConfiguration.class)
public class WebTestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public WebTestClient webTestClient(ApplicationContext applicationContext) {
		WebTestClient.Builder builder = WebTestClient
				.bindToApplicationContext(applicationContext).configureClient();
		customizeWebTestClientCodecs(builder, applicationContext);
		return builder.build();
	}

	private void customizeWebTestClientCodecs(WebTestClient.Builder builder,
			ApplicationContext applicationContext) {
		Collection<CodecCustomizer> customizers = applicationContext
				.getBeansOfType(CodecCustomizer.class).values();
		if (!CollectionUtils.isEmpty(customizers)) {
			builder.exchangeStrategies(ExchangeStrategies.builder()
					.codecs(applyCustomizers(customizers)).build());
		}
	}

	private Consumer<ClientCodecConfigurer> applyCustomizers(
			Collection<CodecCustomizer> customizers) {
		return (codecs) -> customizers
				.forEach((customizer) -> customizer.customize(codecs));
	}

}
