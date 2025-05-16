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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for reactive web
 * infrastructure when a separate management context with a web server running on a
 * different port is required.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
@EnableWebFlux
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public class ReactiveManagementChildContextConfiguration {

	@Bean
	public ManagementWebServerFactoryCustomizer<ConfigurableWebServerFactory> reactiveManagementWebServerFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ManagementWebServerFactoryCustomizer<>(beanFactory);
	}

	@Bean
	public HttpHandler httpHandler(ApplicationContext applicationContext, ManagementServerProperties properties) {
		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		if (StringUtils.hasText(properties.getBasePath())) {
			Map<String, HttpHandler> handlersMap = Collections.singletonMap(properties.getBasePath(), httpHandler);
			return new ContextPathCompositeHandler(handlersMap);
		}
		return httpHandler;
	}

}
