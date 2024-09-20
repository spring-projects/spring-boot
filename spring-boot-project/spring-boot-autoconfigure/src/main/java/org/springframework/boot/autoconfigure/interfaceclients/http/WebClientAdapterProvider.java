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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.interfaceclients.InterfaceClientsAdapter;
import org.springframework.boot.autoconfigure.interfaceclients.QualifiedBeanProvider;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

/**
 * {@link WebClient}-backed {@link HttpExchangeAdapterProvider} implementation.
 * <p>
 * Will attempt to use a {@link WebClient} or {@link WebClient.Builder} bean provided by
 * the user to create a {@link WebClientAdapter}. Beans qualified with a specific client
 * id or {@link InterfaceClientsAdapter#INTERFACE_CLIENTS_DEFAULT_QUALIFIER}) will be
 * used. If no user-provided bean is found, one with a default implementation is created.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
public class WebClientAdapterProvider implements HttpExchangeAdapterProvider {

	private static final Log logger = LogFactory.getLog(WebClientAdapterProvider.class);

	private final ObjectProvider<WebClient.Builder> builderProvider;

	private final ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider;

	public WebClientAdapterProvider(ObjectProvider<WebClient.Builder> builderProvider,
			ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
		this.builderProvider = builderProvider;
		this.propertiesProvider = propertiesProvider;
	}

	@Override
	public HttpExchangeAdapter get(ListableBeanFactory beanFactory, String clientId) {
		HttpInterfaceClientsProperties properties = this.propertiesProvider.getObject();
		String baseUrl = properties.getProperties(clientId).getBaseUrl();

		WebClient userProvidedWebClient = QualifiedBeanProvider.qualifiedBean(beanFactory, WebClient.class, clientId);
		if (userProvidedWebClient != null) {
			// If the user wants to set the baseUrl directly on the builder,
			// it should not be set in the properties.
			if (baseUrl != null) {
				userProvidedWebClient = userProvidedWebClient.mutate().baseUrl(baseUrl).build();
			}
			return WebClientAdapter.create(userProvidedWebClient);
		}

		WebClient.Builder userProvidedWebClientBuilder = QualifiedBeanProvider.qualifiedBean(beanFactory,
				WebClient.Builder.class, clientId);
		if (userProvidedWebClientBuilder != null) {
			// If the user wants to set the baseUrl directly on the builder,
			// it should not be set in the properties.
			if (baseUrl != null) {
				userProvidedWebClientBuilder.baseUrl(baseUrl);
			}
			return WebClientAdapter.create(userProvidedWebClientBuilder.build());
		}

		// create a WebClientAdapter bean with default implementation
		if (logger.isDebugEnabled()) {
			logger.debug("Creating WebClientAdapter for '" + clientId + "'");
		}
		WebClient webClient = this.builderProvider.getObject().baseUrl(baseUrl).build();
		return WebClientAdapter.create(webClient);
	}

}
