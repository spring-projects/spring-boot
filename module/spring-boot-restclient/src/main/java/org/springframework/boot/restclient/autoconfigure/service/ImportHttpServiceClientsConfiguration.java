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

package org.springframework.boot.restclient.autoconfigure.service;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.http.client.autoconfigure.service.ConditionalOnMissingHttpServiceProxyBean;
import org.springframework.boot.restclient.autoconfigure.service.ImportHttpServiceClientsConfiguration.ImportHttpServiceClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.AbstractClientHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceClient;

/**
 * {@link Configuration @Configuration} to import {@link ImportHttpServiceClients} when no
 * user-defined HTTP service client beans are found.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingHttpServiceProxyBean
@Import(ImportHttpServiceClients.class)
class ImportHttpServiceClientsConfiguration {

	/**
	 * {@link AbstractClientHttpServiceRegistrar} to import
	 * {@link HttpServiceClient @HttpServiceClient} annotated classes from
	 * {@link AutoConfigurationPackages}.
	 */
	static class ImportHttpServiceClients extends AbstractClientHttpServiceRegistrar {

		private final BeanFactory beanFactory;

		ImportHttpServiceClients(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata importingClassMetadata) {
			if (AutoConfigurationPackages.has(this.beanFactory)) {
				findAndRegisterHttpServiceClients(registry, AutoConfigurationPackages.get(this.beanFactory));
			}
		}

	}

}
