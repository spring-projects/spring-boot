/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.trace.http;

import org.springframework.boot.actuate.trace.http.HttpExchangeTracer;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.actuate.web.trace.reactive.HttpTraceWebFilter;
import org.springframework.boot.actuate.web.trace.servlet.HttpTraceFilter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for HTTP tracing.
 *
 * @author Dave Syer
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "management.trace.http", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(HttpTraceProperties.class)
public class HttpTraceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HttpTraceRepository.class)
	public InMemoryHttpTraceRepository traceRepository() {
		return new InMemoryHttpTraceRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public HttpExchangeTracer httpExchangeTracer(HttpTraceProperties traceProperties) {
		return new HttpExchangeTracer(traceProperties.getInclude());
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletTraceFilterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public HttpTraceFilter httpTraceFilter(HttpTraceRepository repository,
				HttpExchangeTracer tracer) {
			return new HttpTraceFilter(repository, tracer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveTraceFilterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public HttpTraceWebFilter httpTraceWebFilter(HttpTraceRepository repository,
				HttpExchangeTracer tracer, HttpTraceProperties traceProperties) {
			return new HttpTraceWebFilter(repository, tracer,
					traceProperties.getInclude());
		}

	}

}
