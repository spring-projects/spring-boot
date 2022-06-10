/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.DefaultTracingObservationHandler;
import io.micrometer.tracing.handler.HttpClientTracingObservationHandler;
import io.micrometer.tracing.handler.HttpServerTracingObservationHandler;
import io.micrometer.tracing.http.HttpClientHandler;
import io.micrometer.tracing.http.HttpServerHandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Micrometer Tracing API.
 *
 * @author Moritz Halbritter
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnClass(Tracer.class)
@ConditionalOnEnabledTracing
public class MicrometerTracingAutoConfiguration {

	/**
	 * {@code @Order} value of {@link #defaultTracingObservationHandler(Tracer)}.
	 */
	public static final int DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER = Ordered.LOWEST_PRECEDENCE - 1000;

	/**
	 * {@code @Order} value of
	 * {@link #httpServerTracingObservationHandler(Tracer, HttpServerHandler)}.
	 */
	public static final int HTTP_SERVER_TRACING_OBSERVATION_HANDLER_ORDER = 1000;

	/**
	 * {@code @Order} value of
	 * {@link #httpClientTracingObservationHandler(Tracer, HttpClientHandler)}.
	 */
	public static final int HTTP_CLIENT_TRACING_OBSERVATION_HANDLER_ORDER = 2000;

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(Tracer.class)
	@Order(DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER)
	public DefaultTracingObservationHandler defaultTracingObservationHandler(Tracer tracer) {
		return new DefaultTracingObservationHandler(tracer);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean({ Tracer.class, HttpServerHandler.class })
	@Order(HTTP_SERVER_TRACING_OBSERVATION_HANDLER_ORDER)
	public HttpServerTracingObservationHandler httpServerTracingObservationHandler(Tracer tracer,
			HttpServerHandler httpServerHandler) {
		return new HttpServerTracingObservationHandler(tracer, httpServerHandler);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean({ Tracer.class, HttpClientHandler.class })
	@Order(HTTP_CLIENT_TRACING_OBSERVATION_HANDLER_ORDER)
	public HttpClientTracingObservationHandler httpClientTracingObservationHandler(Tracer tracer,
			HttpClientHandler httpClientHandler) {
		return new HttpClientTracingObservationHandler(tracer, httpClientHandler);
	}

}
