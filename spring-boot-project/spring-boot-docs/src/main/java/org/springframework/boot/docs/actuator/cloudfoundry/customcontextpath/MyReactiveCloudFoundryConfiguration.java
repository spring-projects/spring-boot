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

package org.springframework.boot.docs.actuator.cloudfoundry.customcontextpath;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.web.reactive.WebFluxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebFluxProperties.class)
public class MyReactiveCloudFoundryConfiguration {

	@Bean
	public HttpHandler httpHandler(ApplicationContext applicationContext, WebFluxProperties properties) {
		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		return new CloudFoundryHttpHandler(properties.getBasePath(), httpHandler);
	}

	private static final class CloudFoundryHttpHandler implements HttpHandler {

		private final HttpHandler delegate;

		private final ContextPathCompositeHandler contextPathDelegate;

		private CloudFoundryHttpHandler(String basePath, HttpHandler delegate) {
			this.delegate = delegate;
			this.contextPathDelegate = new ContextPathCompositeHandler(Map.of(basePath, delegate));
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			// Remove underlying context path first (e.g. Servlet container)
			String path = request.getPath().pathWithinApplication().value();
			if (path.startsWith("/cloudfoundryapplication")) {
				return this.delegate.handle(request, response);
			}
			else {
				return this.contextPathDelegate.handle(request, response);
			}
		}

	}

}
