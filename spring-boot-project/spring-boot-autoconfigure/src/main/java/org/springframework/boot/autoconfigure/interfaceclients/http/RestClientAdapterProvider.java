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
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpExchangeAdapter;

/**
 * {@link RestClient}-backed {@link HttpExchangeAdapterProvider} implementation.
 * <p>
 * Will attempt to use a {@link RestClient} or {@link RestClient.Builder} bean provided by
 * the user to create a {@link RestClientAdapter}. Beans qualified with a specific client
 * id or {@link InterfaceClientsAdapter#INTERFACE_CLIENTS_DEFAULT_QUALIFIER}) will be
 * used. If no user-provided bean is found, one with a default implementation is created.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.4.0
 */
public class RestClientAdapterProvider implements HttpExchangeAdapterProvider {

	private static final Log logger = LogFactory.getLog(RestClientAdapterProvider.class);

	private final ObjectProvider<RestClient.Builder> restClientBuilderProvider;

	private final ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider;

	public RestClientAdapterProvider(ObjectProvider<RestClient.Builder> restClientBuilderProvider,
			ObjectProvider<HttpInterfaceClientsProperties> propertiesProvider) {
		this.restClientBuilderProvider = restClientBuilderProvider;
		this.propertiesProvider = propertiesProvider;
	}

	@Override
	public HttpExchangeAdapter get(ListableBeanFactory beanFactory, String clientId) {
		HttpInterfaceClientsProperties properties = this.propertiesProvider.getObject();
		String baseUrl = properties.getProperties(clientId).getBaseUrl();

		RestClient userProvidedRestClient = QualifiedBeanProvider.qualifiedBean(beanFactory, RestClient.class,
				clientId);
		if (userProvidedRestClient != null) {
			// If the user wants to set the baseUrl directly on the builder,
			// it should not be set in the properties.
			if (baseUrl != null) {
				userProvidedRestClient = userProvidedRestClient.mutate().baseUrl(baseUrl).build();
			}
			return RestClientAdapter.create(userProvidedRestClient);
		}

		RestClient.Builder userProvidedRestClientBuilder = QualifiedBeanProvider.qualifiedBean(beanFactory,
				RestClient.Builder.class, clientId);
		if (userProvidedRestClientBuilder != null) {
			// If the user wants to set the baseUrl directly on the builder,
			// it should not be set in the properties.
			if (baseUrl != null) {
				userProvidedRestClientBuilder.baseUrl(baseUrl);
			}
			return RestClientAdapter.create(userProvidedRestClientBuilder.build());
		}
		// create a RestClientAdapter bean with default implementation
		if (logger.isDebugEnabled()) {
			logger.debug("Creating RestClientAdapter for '" + clientId + "'");
		}
		RestClient restClient = this.restClientBuilderProvider.getObject().baseUrl(baseUrl).build();
		return RestClientAdapter.create(restClient);
	}

}
