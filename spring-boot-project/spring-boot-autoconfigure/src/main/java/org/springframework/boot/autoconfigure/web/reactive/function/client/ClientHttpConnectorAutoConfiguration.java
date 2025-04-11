/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.List;

import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.http.client.reactive.ClientHttpConnectorBuilderCustomizer;
import org.springframework.boot.autoconfigure.reactor.netty.ReactorNettyConfigurations.ReactorResourceFactoryConfiguration;
import org.springframework.boot.http.client.reactive.ReactorClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Deprecated {@link EnableAutoConfiguration Auto-configuration} for
 * {@link ReactorNettyHttpClientMapper}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 * @since 2.1.0
 * @deprecated since 3.5.0 for removal in 4.0.0 in favor of
 * {@link org.springframework.boot.autoconfigure.http.client.reactive.ClientHttpConnectorAutoConfiguration}
 * and to align with the deprecation of {@link ReactorNettyHttpClientMapper}
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@Deprecated(since = "3.5.0", forRemoval = true)
public class ClientHttpConnectorAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HttpClient.class)
	@Import(ReactorResourceFactoryConfiguration.class)
	@SuppressWarnings("removal")
	static class ReactorNetty {

		@Bean
		@Order(0)
		ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> reactorNettyHttpClientMapperClientHttpConnectorBuilderCustomizer(
				ReactorResourceFactory reactorResourceFactory,
				ObjectProvider<ReactorNettyHttpClientMapper> mapperProvider) {
			return applyMappers(mapperProvider.orderedStream().toList());
		}

		private ClientHttpConnectorBuilderCustomizer<ReactorClientHttpConnectorBuilder> applyMappers(
				List<ReactorNettyHttpClientMapper> mappers) {
			return (builder) -> {
				for (ReactorNettyHttpClientMapper mapper : mappers) {
					builder = builder.withHttpClientCustomizer(mapper::configure);
				}
				return builder;
			};
		}

	}

}
