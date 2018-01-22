/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.trace;

import org.springframework.boot.actuate.web.trace.HttpExchangeTracer;
import org.springframework.boot.actuate.web.trace.HttpTraceRepository;
import org.springframework.boot.actuate.web.trace.InMemoryHttpTraceRepository;
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
@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "management.trace", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TraceProperties.class)
public class TraceAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(HttpTraceRepository.class)
	public InMemoryHttpTraceRepository traceRepository() {
		return new InMemoryHttpTraceRepository();
	}

	@Bean
	@ConditionalOnMissingBean
	public HttpExchangeTracer httpExchangeTracer(TraceProperties traceProperties) {
		return new HttpExchangeTracer(traceProperties.getInclude());
	}

	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletTraceFilterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public HttpTraceFilter httpTraceFilter(HttpTraceRepository repository,
				HttpExchangeTracer tracer) {
			return new HttpTraceFilter(repository, tracer);
		}

	}

	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveTraceFilterConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public HttpTraceWebFilter httpTraceWebFilter(HttpTraceRepository repository,
				HttpExchangeTracer tracer, TraceProperties traceProperties) {
			return new HttpTraceWebFilter(repository, tracer,
					traceProperties.getInclude());
		}

	}

}
