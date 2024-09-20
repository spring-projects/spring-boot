/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.interfaceclients.http;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.client.NotReactiveWebApplicationCondition;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.client.support.RestTemplateAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP Interface Clients.
 * <p>
 * This will result in the creation of Interface Client beans from
 * {@link HttpClient}-annotated interfaces.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
@AutoConfiguration(after = { RestTemplateAutoConfiguration.class, RestClientAutoConfiguration.class,
		WebClientAutoConfiguration.class })
@Import(HttpInterfaceClientsImportRegistrar.class)
@EnableConfigurationProperties(HttpInterfaceClientsProperties.class)
@ConditionalOnProperty(value = "spring.interfaceclients.enabled", havingValue = "true")
public class HttpInterfaceClientsAutoConfiguration {

	@Bean
	HttpInterfaceClientsAdapter httpInterfaceClientAdapter(HttpExchangeAdapterProvider adapterProvider) {
		return new HttpInterfaceClientsAdapter(adapterProvider);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ RestClient.class, RestClientAdapter.class, HttpServiceProxyFactory.class })
	@Conditional(NotReactiveWebApplicationCondition.class)
	@ConditionalOnProperty(value = "spring.interfaceclients.http.resttemplate.enabled", havingValue = "false",
			matchIfMissing = true)
	protected static class RestClientInterfaceClientsConfiguration {

		@Bean
		@ConditionalOnBean(RestClient.Builder.class)
		@ConditionalOnMissingBean
		HttpExchangeAdapterProvider restClientAdapterProvider(
				ObjectProvider<RestClient.Builder> restClientBuilderProvider,
				ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
			return new RestClientAdapterProvider(restClientBuilderProvider, propertiesProvider);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ RestTemplate.class, RestTemplateAdapter.class, HttpServiceProxyFactory.class })
	@Conditional(NotReactiveWebApplicationCondition.class)
	@ConditionalOnProperty(value = "spring.interfaceclients.http.resttemplate.enabled", havingValue = "true")
	protected static class RestTemplateInterfaceClientsConfiguration {

		@Bean
		@ConditionalOnBean(RestTemplateBuilder.class)
		@ConditionalOnMissingBean
		HttpExchangeAdapterProvider restTemplateAdapterProvider(
				ObjectProvider<RestTemplateBuilder> restTemplateBuilderProvider,
				ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
			return new RestTemplateAdapterProvider(restTemplateBuilderProvider, propertiesProvider);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ WebClient.class, WebClientAdapter.class, HttpServiceProxyFactory.class })
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	protected static class WebClientInterfaceClientsConfiguration {

		@Bean
		@ConditionalOnBean(WebClient.Builder.class)
		@ConditionalOnMissingBean
		HttpExchangeAdapterProvider webClientAdapterProvider(ObjectProvider<WebClient.Builder> webClientBuilderProvider,
				ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
			return new WebClientAdapterProvider(webClientBuilderProvider, propertiesProvider);
		}

	}

}
