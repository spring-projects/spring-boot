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

package org.springframework.boot.autoconfigure.web.reactive;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.session.WebSessionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpHandler}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass({ DispatcherHandler.class, HttpHandler.class })
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnMissingBean(HttpHandler.class)
@AutoConfigureAfter({ WebFluxAutoConfiguration.class })
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpHandlerAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(RouterFunction.class)
	public static class AnnotationConfig {

		private ApplicationContext applicationContext;

		public AnnotationConfig(ApplicationContext applicationContext) {
			this.applicationContext = applicationContext;
		}

		@Bean
		public HttpHandler httpHandler() {
			return WebHttpHandlerBuilder.applicationContext(this.applicationContext).build();
		}

	}

	@Configuration
	@ConditionalOnBean(RouterFunction.class)
	public static class FunctionalConfig {

		private final List<WebFilter> webFilters;

		private final WebSessionManager webSessionManager;

		private HandlerStrategies.Builder handlerStrategiesBuilder;

		private final List<ViewResolver> viewResolvers;

		public FunctionalConfig(ObjectProvider<List<WebFilter>> webFilters,
			ObjectProvider<WebSessionManager> webSessionManager,
			ObjectProvider<HandlerStrategies.Builder> handlerStrategiesBuilder,
			ObjectProvider<List<ViewResolver>> viewResolvers) {
			this.webFilters = webFilters.getIfAvailable();
			if (this.webFilters != null) {
				AnnotationAwareOrderComparator.sort(this.webFilters);
			}
			this.webSessionManager = webSessionManager.getIfAvailable();
			this.handlerStrategiesBuilder = handlerStrategiesBuilder.getIfAvailable();
			this.viewResolvers = viewResolvers.getIfAvailable();
		}

		@Bean
		public <T extends ServerResponse> HttpHandler httpHandler(List<RouterFunction<T>> routerFunctions) {
			routerFunctions.sort(new AnnotationAwareOrderComparator());
			RouterFunction<T> routerFunction = routerFunctions.stream()
				.reduce(RouterFunction::and).get();
			if (this.handlerStrategiesBuilder == null) {
				this.handlerStrategiesBuilder = HandlerStrategies.builder();
			}
			if (this.viewResolvers != null) {
				this.viewResolvers.forEach(this.handlerStrategiesBuilder::viewResolver);
			}
			WebHandler webHandler = RouterFunctions.toHttpHandler(routerFunction,
				this.handlerStrategiesBuilder.build());
			WebHttpHandlerBuilder builder = WebHttpHandlerBuilder.webHandler(webHandler)
				.sessionManager(this.webSessionManager);
			builder.filters(this.webFilters);
			return builder.build();
		}

	}

}
