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

package org.springframework.boot.actuate.autoconfigure.web.exchanges;

import org.springframework.boot.actuate.web.exchanges.HttpExchange;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.reactive.HttpExchangesWebFilter;
import org.springframework.boot.actuate.web.exchanges.servlet.HttpExchangesFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} to record {@link HttpExchange HTTP
 * exchanges}.
 *
 * @author Dave Syer
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "management.httpexchanges.recording", name = "enabled", matchIfMissing = true)
@ConditionalOnBean(HttpExchangeRepository.class)
@EnableConfigurationProperties(HttpExchangesProperties.class)
public class HttpExchangesAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletHttpExchangesConfiguration {

		@Bean
		@ConditionalOnMissingBean
		HttpExchangesFilter httpExchangesFilter(HttpExchangeRepository repository, HttpExchangesProperties properties) {
			return new HttpExchangesFilter(repository, properties.getRecording().getInclude());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveHttpExchangesConfiguration {

		@Bean
		@ConditionalOnMissingBean
		HttpExchangesWebFilter httpExchangesWebFilter(HttpExchangeRepository repository,
				HttpExchangesProperties properties) {
			return new HttpExchangesWebFilter(repository, properties.getRecording().getInclude());
		}

	}

}
