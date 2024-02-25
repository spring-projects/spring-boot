/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
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
@AutoConfiguration(after = { CodecsAutoConfiguration.class, ClientHttpConnectorAutoConfiguration.class })
@ConditionalOnClass(WebClient.class)
public class WebClientAutoConfiguration {

	/**
     * Creates a new instance of WebClient.Builder with prototype scope.
     * If there is no existing bean of type WebClient.Builder, this method will be used.
     * 
     * @param customizerProvider ObjectProvider of WebClientCustomizer to customize the WebClient.Builder
     * @return WebClient.Builder instance
     */
    @Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public WebClient.Builder webClientBuilder(ObjectProvider<WebClientCustomizer> customizerProvider) {
		WebClient.Builder builder = WebClient.builder();
		customizerProvider.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	/**
     * Creates an instance of AutoConfiguredWebClientSsl if WebClientSsl bean is missing and SslBundles bean is present.
     * 
     * @param clientHttpConnectorFactory The factory for creating the client HTTP connector.
     * @param sslBundles The SSL bundles for configuring the WebClientSsl.
     * @return An instance of AutoConfiguredWebClientSsl.
     */
    @Bean
	@ConditionalOnMissingBean(WebClientSsl.class)
	@ConditionalOnBean(SslBundles.class)
	AutoConfiguredWebClientSsl webClientSsl(ClientHttpConnectorFactory<?> clientHttpConnectorFactory,
			SslBundles sslBundles) {
		return new AutoConfiguredWebClientSsl(clientHttpConnectorFactory, sslBundles);
	}

	/**
     * WebClientCodecsConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(CodecCustomizer.class)
	protected static class WebClientCodecsConfiguration {

		/**
         * Creates a WebClientCodecCustomizer bean if no other bean of the same type is present.
         * The bean is ordered with a priority of 0.
         * 
         * @param codecCustomizers an ObjectProvider of CodecCustomizer beans
         * @return a WebClientCodecCustomizer bean with the provided CodecCustomizer beans
         */
        @Bean
		@ConditionalOnMissingBean
		@Order(0)
		public WebClientCodecCustomizer exchangeStrategiesCustomizer(ObjectProvider<CodecCustomizer> codecCustomizers) {
			return new WebClientCodecCustomizer(codecCustomizers.orderedStream().toList());
		}

	}

}
