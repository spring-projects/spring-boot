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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.reactive.DefaultReactiveWebServerCustomizer;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Configuration for reactive web endpoint infrastructure when a separate management
 * context with a web server running on a different port is required.
 *
 * @author Andy Wilkinson
 */
@EnableWebFlux
@ManagementContextConfiguration(ManagementContextType.CHILD)
@ConditionalOnWebApplication(type = Type.REACTIVE)
class ReactiveEndpointChildManagementContextConfiguration {

	@Bean
	public HttpHandler httpHandler(ApplicationContext applicationContext) {
		return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
	}

	@Bean
	public ManagementReactiveWebServerFactoryCustomizer webServerFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ManagementReactiveWebServerFactoryCustomizer(beanFactory);
	}

	static class ManagementReactiveWebServerFactoryCustomizer extends
			ManagementWebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory> {

		ManagementReactiveWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
			super(beanFactory, DefaultReactiveWebServerCustomizer.class);
		}

	}

}
