/*
 * Copyright 2012-2020 the original author or authors.
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
import org.springframework.boot.autoconfigure.web.embedded.JettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.UndertowWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.reactive.TomcatReactiveWebServerFactoryCustomizer;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
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
 * @since 2.0.0
 */
@EnableWebFlux
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public class ReactiveManagementChildContextConfiguration {

	@Bean
	public ReactiveManagementWebServerFactoryCustomizer reactiveManagementWebServerFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ReactiveManagementWebServerFactoryCustomizer(beanFactory);
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

	static class ReactiveManagementWebServerFactoryCustomizer
			extends ManagementWebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory> {

		ReactiveManagementWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
			super(beanFactory, ReactiveWebServerFactoryCustomizer.class, TomcatWebServerFactoryCustomizer.class,
					TomcatReactiveWebServerFactoryCustomizer.class, JettyWebServerFactoryCustomizer.class,
					UndertowWebServerFactoryCustomizer.class, NettyWebServerFactoryCustomizer.class);
		}

	}

}
